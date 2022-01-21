package com.elfefe.goodwine.mvvm.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.utils.enums.Connection
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FirebaseViewmodel: ViewModel() {
    private val repository = BaseApplication.instance.firebaseRepository

    private val _connectionLivedata = MutableLiveData<Connection>()
    val connectionLivedata: LiveData<Connection>
        get() = _connectionLivedata

    val user: FirebaseUser?
        get() = repository.user

    init {
        repository
            .connectionFlow
            .onEach {
                it?.let { connection ->
                    _connectionLivedata.postValue(connection)
                }
            }
            .launchIn(viewModelScope)
    }

    fun connect() {
        if (repository.user?.isEmailVerified != true) repository.checkConnection()
    }

    fun connectAnonymoulsy() = repository.connectAnonymous()

    fun connectFacebook(activity: ComponentActivity) = repository.connectFacebook(activity)

    fun onResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {
        repository.onFacebookResult(requestCode, resultCode, data)
    }

    fun connectPhone(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) = repository.connectPhone(activity, onSuccess, onFailure)

    fun syncBottles() = repository.syncData(listOf())
}