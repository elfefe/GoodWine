package com.elfefe.goodwine.mvvm.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.R
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.utils.enums.Connection
import com.elfefe.goodwine.utils.resString
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class FirebaseRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    val app = FirebaseApp.initializeApp(BaseApplication.instance)

    val auth = FirebaseAuth.getInstance()
    private val store = FirebaseFirestore.getInstance()

    val user: FirebaseUser?
        get() = auth.currentUser

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(resString(R.string.client_id))
        .requestEmail()
        .build()

    val googleIntent = GoogleSignIn.getClient(BaseApplication.instance, gso).signInIntent

    private lateinit var facebookCallback: CallbackManager

    private val _connectionFlow = MutableStateFlow<Connection?>(null)
    val connectionFlow: StateFlow<Connection?>
        get() = _connectionFlow

    private val _bottleFlow = MutableStateFlow<List<Bottle>?>(null)
    val bottleFlow: StateFlow<List<Bottle>?>
        get() = _bottleFlow

    private val bottleCollection: CollectionReference
        get() = store
            .collection("Database")
            .document(user?.email ?: "")
            .collection("Bottle")

    fun checkConnection() {
        if (AccessToken.isCurrentAccessTokenActive())
            AccessToken.getCurrentAccessToken()?.run {
                connectCredential(FacebookAuthProvider.getCredential(token))
            }
    }

    fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_create_account_failure))
                )
            }
    }

    fun connectAccount(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_connect_account_failure))
                )
            }
    }

    fun connectAnonymous() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_connect_account_failure))
                )
            }
    }

    fun connectFacebook(
        activity: ComponentActivity
    ) {
        if (AccessToken.isCurrentAccessTokenActive())
            AccessToken.getCurrentAccessToken()?.run {
                linkUser(FacebookAuthProvider.getCredential(token))
            }
        else
            LoginManager.getInstance().apply {
                facebookCallback = CallbackManager.Factory.create()
                registerCallback(facebookCallback, object : FacebookCallback<LoginResult> {
                    override fun onCancel() {}
                    override fun onError(error: FacebookException) {}
                    override fun onSuccess(result: LoginResult) {
                        result.authenticationToken?.run {
                            linkUser(FacebookAuthProvider.getCredential(token))
                        }
                    }
                })
                logIn(activity, facebookCallback, mutableListOf("email", "public_profile"))
            }
    }

    fun onFacebookResult(requestCode: Int, resultCode: Int, data: Intent?) =
        facebookCallback.onActivityResult(requestCode, resultCode, data)

    @SuppressLint("MissingPermission")
    fun connectPhone(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        val telephonyManager =
            activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder(auth)
                .setPhoneNumber(telephonyManager.line1Number)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(activity)                 // Activity (for callback binding)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        onSuccess()
                    }

                    override fun onVerificationFailed(p0: FirebaseException) {
                        onFailure(p0)
                    }

                    override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(p0, p1)
                    }
                })
                .build()
        )
    }

    private fun linkUser(credential: AuthCredential) {
        user
            ?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _connectionFlow.value = Connection.Success
                } else {
                    connectCredential(credential)
                }
            } ?: connectCredential(credential)
    }

    private fun connectCredential(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_connect_account_failure))
                )
            }
    }

    fun syncData(bottleIds: List<Int>) {
        store
            .collection("Database")
            .document(user?.email ?: "")
            .collection("Bottle")
            .whereNotIn(FieldPath.documentId(), bottleIds.map { it.toString() })
            .get()
            .addOnSuccessListener { snapshot ->
                _bottleFlow.value =
                    snapshot.documents.map {
                        Bottle(
                            it.id.toInt(),
                            it.get("Date") as Long,
                            it.get("Picture").toString(),
                            it.get("Description").toString(),
                            (it.get("Rating") as Double).toFloat(),
                        )
                    }.toList()
            }
    }

    fun sendBottles(bottles: List<Bottle>) {
        bottles.forEach { bottle ->
            bottleCollection
                .document(bottle.id.toString())
                .set(
                    mapOf(
                        "Date" to bottle.date,
                        "Picture" to bottle.picture,
                        "Description" to bottle.description,
                        "Rating" to bottle.rating
                    )
                )
        }
    }

    companion object {
        fun FirebaseRepository.connectGoogle(
            activity: ComponentActivity,
            onSuccess: (FirebaseUser) -> Unit = { },
            onFailure: (Exception?) -> Unit = { }
        ): () -> Unit {
            val register = activity
                .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == 0) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            auth.signInWithCredential(
                                GoogleAuthProvider.getCredential(
                                    account.idToken,
                                    null
                                )
                            ).addOnCompleteListener { result ->
                                if (result.isSuccessful) result.result.user?.let(onSuccess)
                                else onFailure(result.exception)
                            }
                        } catch (e: ApiException) {
                            println("Exception ${e.message}")
                            onFailure(e)
                        }
                    }
                }
            return { user ?: register.launch(googleIntent) }
        }
    }
}