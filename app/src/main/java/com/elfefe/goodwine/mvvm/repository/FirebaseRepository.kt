package com.elfefe.goodwine.mvvm.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.R
import com.elfefe.goodwine.utils.enums.Connection
import com.elfefe.goodwine.utils.resString
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _connectionFlow = MutableStateFlow<Connection?>(null)
    val connectionFlow: StateFlow<Connection?>
        get() = _connectionFlow

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

    fun connectCredential(credential: AuthCredential) {
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

    fun connectFacebook(
        activity: ComponentActivity
    ) {
        LoginManager.getInstance().apply {
            val callback = CallbackManager.Factory.create()
            registerCallback(callback, object : FacebookCallback<LoginResult> {
                override fun onCancel() { }
                override fun onError(error: FacebookException) { }
                override fun onSuccess(result: LoginResult) {
                    result.authenticationToken?.run {
                        connectCredential(FacebookAuthProvider.getCredential(token))
                    }
                }
            })
            logIn(activity, callback, mutableListOf("email"))
        }
    }

    @SuppressLint("MissingPermission")
    fun connectPhone(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        val telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder(auth)
                .setPhoneNumber(telephonyManager.line1Number)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(activity)                 // Activity (for callback binding)
                .setCallbacks(object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) { onSuccess() }
                    override fun onVerificationFailed(p0: FirebaseException) { onFailure(p0) }
                    override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(p0, p1)
                    }
                })
                .build()
        )
    }

    fun syncData(bottleIds: List<Long>) {
        store
            .collection("Bottles")
            .addSnapshotListener { value, error ->
                println("BOTTLE ${value?.documents?.map { it.id }?.toList()}")
                error?.printStackTrace()
            }
    }

    companion object {
        fun connectGoogle(
            activity: ComponentActivity,
            onSuccess: (FirebaseUser) -> Unit = { },
            onFailure: (Exception?) -> Unit = { }
        ): () -> Unit {
            val firebase = BaseApplication.instance.firebaseRepository
            val register = activity
                .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == 0) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            firebase.auth.signInWithCredential(
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
            return { firebase.user ?: register.launch(firebase.googleIntent) }
        }
    }
}