package com.elfefe.goodwine.mvvm.viewmodel

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

class FirebaseViewmodel : ViewModel() {
    private val repository = BaseApplication.instance.mediator

    private val _connectionLivedata = MutableLiveData<Connection>()
    val connectionLivedata: LiveData<Connection>
        get() = _connectionLivedata

    val user: FirebaseUser?
        get() = repository.user

    /** Faux quand le build n'a pas de configuration Firebase. */
    val cloudAvailable: Boolean
        get() = repository.cloudAvailable

    init {
        repository
            .connectionFlow
            .onEach { it?.let(_connectionLivedata::postValue) }
            .launchIn(viewModelScope)

        // Ce que le serveur renvoie est réinjecté en base locale : sans cela, syncData()
        // remplissait un flux que personne ne lisait.
        repository
            .remoteBottleFlow
            .onEach { bottles -> bottles?.let(repository::saveRemoteBottles) }
            .launchIn(viewModelScope)
    }

    fun connect() {
        if (!cloudAvailable) return
        if (repository.user?.isEmailVerified != true) repository.checkConnection()
    }

    fun connectAnonymously() = repository.connectAnonymously()

    fun connectFacebook(activity: ComponentActivity) = repository.connectFacebook(activity)

    fun onResult(requestCode: Int, resultCode: Int, data: Intent?) =
        repository.onFacebookResult(requestCode, resultCode, data)

    fun connectPhone(
        activity: ComponentActivity,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) = repository.connectPhone(activity, phoneNumber, onSuccess, onFailure)

    /** Était vide. Rapatrie ce qui manque au téléphone, puis pousse l'état local. */
    fun syncBottles() {
        if (!cloudAvailable) return
        repository.syncBottles()
    }
}
