package com.elfefe.goodwine.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.oltp.parcelable.Bottle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OltpViewmodel : ViewModel() {
    private val repository = BaseApplication.instance.mediator

    private val _bottlesLivedata = MutableLiveData<List<Bottle>>()
    val bottlesLivedata: LiveData<List<Bottle>>
        get() = _bottlesLivedata

    init {
        repository
            .bottleFlow
            .onEach {
                _bottlesLivedata.postValue(it)
            }
            .launchIn(viewModelScope)
    }

    fun updateBottlesRatingOrder(asc: Boolean) = repository.updateBottlesRatingOrder(asc)
    fun updateBottlesDateOrder(asc: Boolean) = repository.updateBottlesDateOrder(asc)

    fun saveBottle(bottle: Bottle) = repository.saveBottle(bottle)
}