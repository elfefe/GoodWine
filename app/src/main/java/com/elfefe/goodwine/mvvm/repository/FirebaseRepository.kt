package com.elfefe.goodwine.mvvm.repository

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.R
import com.elfefe.goodwine.utils.enums.Connection
import com.elfefe.goodwine.utils.resString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.lang.Exception

class FirebaseRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

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

    fun createAccount(email: String, password: String, onSuccess: (FirebaseUser) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user?.let(onSuccess)
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_create_account_failure))
                )
            }
    }

    fun connectAccount(email: String, password: String, onSuccess: (FirebaseUser) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user?.let(onSuccess)
                    _connectionFlow.value = Connection.Success
                } else _connectionFlow.value = Connection.Failure(
                    task.exception
                        ?: Exception(resString(R.string.connection_connect_account_failure))
                )
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
                    println("ResultCode ${it.resultCode}")
                    if (it.resultCode == 0) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            println("Task ${account.idToken}")
                            firebase.auth.signInWithCredential(
                                GoogleAuthProvider.getCredential(
                                    account.idToken,
                                    null
                                )
                            ).addOnCompleteListener { result ->
                                println("Result ${result.isSuccessful} ${result.result.user}")
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