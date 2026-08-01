package com.elfefe.goodwine.mvvm.repository

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.BuildConfig
import com.elfefe.goodwine.R
import com.elfefe.goodwine.mvvm.BottleSync
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.utils.enums.Connection
import com.elfefe.goodwine.utils.resString
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.TimeUnit


class FirebaseRepository {

    /**
     * Firebase n'est branché que si `google-services.json` était présent à la compilation.
     * Sans lui, [FirebaseApp.initializeApp] renvoie null et tout appel suivant lève : l'app
     * plantait au démarrage. On constate l'absence une fois pour toutes, et chaque fonction
     * rend la main en signalant l'indisponibilité au lieu de faire tomber l'app.
     */
    val available: Boolean = when {
        !BuildConfig.FIREBASE_ENABLED -> false
        else -> runCatching { FirebaseApp.initializeApp(BaseApplication.instance) != null }
            .getOrElse {
                Log.w(TAG, "Firebase indisponible", it)
                false
            }
    }

    private val auth: FirebaseAuth? = if (available) FirebaseAuth.getInstance() else null
    private val store: FirebaseFirestore? = if (available) FirebaseFirestore.getInstance() else null
    private val storage: FirebaseStorage? = if (available) FirebaseStorage.getInstance() else null

    val user: FirebaseUser?
        get() = auth?.currentUser

    val googleIntent: Intent?
        get() = if (!available) null else GoogleSignIn
            .getClient(
                BaseApplication.instance,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(resString(R.string.client_id))
                    .requestEmail()
                    .build()
            )
            .signInIntent

    // Créé à la première connexion Facebook. Il était déclaré lateinit et déréférencé sans
    // garde par onFacebookResult : l'app tombait si l'utilisateur ne passait pas par Facebook.
    private var facebookCallback: CallbackManager? = null

    private val _connectionFlow = MutableStateFlow<Connection?>(null)
    val connectionFlow: StateFlow<Connection?>
        get() = _connectionFlow

    private val _bottleFlow = MutableStateFlow<List<Bottle>?>(null)
    val bottleFlow: StateFlow<List<Bottle>?>
        get() = _bottleFlow

    private val bottleCollection: CollectionReference?
        get() {
            val owner = user?.email ?: user?.uid ?: return null
            return store
                ?.collection("Database")
                ?.document(owner)
                ?.collection("Bottle")
        }

    /** Signale l'indisponibilité par le flux de connexion et renvoie true si l'on doit renoncer. */
    private fun unavailable(): Boolean {
        if (!available) _connectionFlow.value = Connection.Failure(
            IllegalStateException(resString(R.string.firebase_unavailable))
        )
        return !available
    }

    private fun complete(task: com.google.android.gms.tasks.Task<*>, fallback: Int) {
        _connectionFlow.value = if (task.isSuccessful) Connection.Success
        else Connection.Failure(task.exception ?: Exception(resString(fallback)))
    }

    fun checkConnection() {
        if (!available) return
        if (AccessToken.isCurrentAccessTokenActive())
            AccessToken.getCurrentAccessToken()?.run {
                connectCredential(FacebookAuthProvider.getCredential(token))
            }
    }

    fun createAccount(email: String, password: String) {
        if (unavailable()) return
        auth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { complete(it, R.string.connection_create_account_failure) }
    }

    fun connectAccount(email: String, password: String) {
        if (unavailable()) return
        auth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { complete(it, R.string.connection_connect_account_failure) }
    }

    fun connectAnonymous() {
        if (unavailable()) return
        auth!!.signInAnonymously()
            .addOnCompleteListener { complete(it, R.string.connection_connect_account_failure) }
    }

    fun connectFacebook(activity: ComponentActivity) {
        if (unavailable()) return
        if (AccessToken.isCurrentAccessTokenActive())
            AccessToken.getCurrentAccessToken()?.run {
                linkUser(FacebookAuthProvider.getCredential(token))
            }
        else
            LoginManager.getInstance().apply {
                val callback = CallbackManager.Factory.create()
                facebookCallback = callback
                registerCallback(callback, object : FacebookCallback<LoginResult> {
                    override fun onCancel() {}

                    override fun onError(error: FacebookException) {
                        _connectionFlow.value = Connection.Failure(error)
                    }

                    override fun onSuccess(result: LoginResult) {
                        result.authenticationToken?.run {
                            linkUser(FacebookAuthProvider.getCredential(token))
                        }
                    }
                })
                logIn(activity, callback, mutableListOf("email", "public_profile"))
            }
    }

    fun onFacebookResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookCallback?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * L'implémentation d'origine lisait le numéro par `TelephonyManager.line1Number`, qui ne
     * renvoie plus rien sur la plupart des appareils depuis Android 10 et exigeait
     * READ_PHONE_NUMBERS + READ_PHONE_STATE. Le numéro est désormais fourni par l'appelant.
     */
    fun connectPhone(
        activity: ComponentActivity,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        if (!available) {
            onFailure(IllegalStateException(resString(R.string.firebase_unavailable)))
            return
        }
        if (phoneNumber.isBlank()) {
            onFailure(IllegalArgumentException(resString(R.string.connection_phone_empty)))
            return
        }
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder(auth!!)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) = onSuccess()
                    override fun onVerificationFailed(p0: FirebaseException) = onFailure(p0)
                })
                .build()
        )
    }

    private fun linkUser(credential: AuthCredential) {
        user
            ?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) _connectionFlow.value = Connection.Success
                else connectCredential(credential)
            } ?: connectCredential(credential)
    }

    private fun connectCredential(credential: AuthCredential) {
        if (unavailable()) return
        auth!!.signInWithCredential(credential)
            .addOnCompleteListener { complete(it, R.string.connection_connect_account_failure) }
    }

    /**
     * Récupère du serveur les bouteilles absentes du téléphone.
     *
     * Deux corrections par rapport à l'origine : `whereNotIn` est refusé par Firestore sur une
     * liste vide — cas d'une première installation, qui faisait échouer la synchronisation
     * silencieusement — et un identifiant de document non numérique faisait lever `toInt()`.
     */
    fun syncData(bottleIds: List<Int>) {
        val collection = bottleCollection ?: return
        val query =
            if (BottleSync.canFilterServerSide(bottleIds))
                collection.whereNotIn(FieldPath.documentId(), bottleIds.map { it.toString() })
            else collection
        query
            .get()
            .addOnSuccessListener { snapshot ->
                val remotes = snapshot.documents.map {
                    BottleSync.RemoteBottle(
                        id = it.id,
                        date = it.getLong("Date"),
                        picture = it.getString("Picture"),
                        description = it.getString("Description"),
                        rating = it.getDouble("Rating")
                    )
                }
                _bottleFlow.value = BottleSync.missingBottles(remotes, bottleIds)
            }
            .addOnFailureListener { Log.w(TAG, "Synchronisation impossible", it) }
    }

    /**
     * Envoie les fiches, et **la photo avec** : c'est ce qui manquait. `Bottle.picture` porte un
     * chemin du stockage interne, qui ne veut rien dire sur un autre appareil. La photo part
     * d'abord dans Storage, et c'est son URL qui est enregistrée dans Firestore.
     */
    fun sendBottles(bottles: List<Bottle>) {
        val collection = bottleCollection ?: return
        bottles.forEach { bottle ->
            uploadPicture(bottle) { pictureUrl ->
                collection
                    .document(bottle.id.toString())
                    .set(
                        mapOf(
                            "Date" to bottle.date,
                            "Picture" to pictureUrl,
                            "Description" to bottle.description,
                            "Rating" to bottle.rating
                        )
                    )
                    .addOnFailureListener { Log.w(TAG, "Envoi de la bouteille ${bottle.id} échoué", it) }
            }
        }
    }

    fun deleteBottle(bottle: Bottle) {
        bottleCollection
            ?.document(bottle.id.toString())
            ?.delete()
            ?.addOnFailureListener { Log.w(TAG, "Suppression de la bouteille ${bottle.id} échouée", it) }
        pictureReference(bottle)?.delete()
    }

    private fun pictureReference(bottle: Bottle): StorageReference? {
        val owner = user?.uid ?: return null
        return storage?.reference?.child(BottleSync.picturePath(owner, bottle.id))
    }

    /** Téléverse la photo et rend son URL ; rend le chemin d'origine si l'envoi n'est pas possible. */
    private fun uploadPicture(bottle: Bottle, onReady: (String) -> Unit) {
        if (!BottleSync.needsUpload(bottle.picture)) {
            onReady(bottle.picture)
            return
        }
        val local = File(bottle.picture)
        val reference = pictureReference(bottle)
        if (reference == null || !local.exists()) {
            onReady(bottle.picture)
            return
        }
        reference
            .putFile(Uri.fromFile(local))
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                reference.downloadUrl
            }
            .addOnSuccessListener { onReady(it.toString()) }
            .addOnFailureListener {
                Log.w(TAG, "Photo de la bouteille ${bottle.id} non envoyée", it)
                onReady(bottle.picture)
            }
    }

    companion object {
        private const val TAG = "FirebaseRepository"

        fun FirebaseRepository.connectGoogle(
            activity: ComponentActivity,
            onSuccess: (FirebaseUser) -> Unit = { },
            onFailure: (Exception?) -> Unit = { }
        ): () -> Unit {
            val register = activity
                .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        auth?.signInWithCredential(
                            GoogleAuthProvider.getCredential(account.idToken, null)
                        )?.addOnCompleteListener { result ->
                            if (result.isSuccessful) result.result.user?.let(onSuccess)
                            else onFailure(result.exception)
                        } ?: onFailure(IllegalStateException(resString(R.string.firebase_unavailable)))
                    } catch (e: ApiException) {
                        Log.w(TAG, "Connexion Google échouée", e)
                        onFailure(e)
                    }
                }
            return {
                val intent = googleIntent
                when {
                    user != null -> Unit
                    intent == null -> onFailure(
                        IllegalStateException(resString(R.string.firebase_unavailable))
                    )
                    else -> register.launch(intent)
                }
            }
        }
    }
}
