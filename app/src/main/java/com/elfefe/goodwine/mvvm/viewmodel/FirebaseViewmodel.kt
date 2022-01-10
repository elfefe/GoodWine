package com.elfefe.goodwine.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.utils.enums.Connection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FirebaseViewmodel: ViewModel() {
    private val repository = BaseApplication.instance.firebaseRepository

    private val _connectionLivedata = MutableLiveData<Connection>()
    val connectionLivedata: LiveData<Connection>
        get() = _connectionLivedata

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

    fun setConnection(state: Connection) {
        _connectionLivedata.value = state
    }
}