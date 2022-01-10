package com.elfefe.goodwine.mvvm.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UiViewmodel: ViewModel() {
    private val _screenSizeLivedata = MutableLiveData<IntSize>()
    val screenSizeLivedata: LiveData<IntSize>
        get() = _screenSizeLivedata

    private val _addBottleLivedata = MutableLiveData<Boolean>()
    val addBottleLivedata: LiveData<Boolean>
        get() = _addBottleLivedata

    private val _keyboardLivedata = MutableLiveData<Boolean>()
    val keyboardLivedata: LiveData<Boolean>
        get() = _keyboardLivedata

    fun setBottle(value: Boolean) {
        _addBottleLivedata.value = value
    }

    fun setKeyboard(visible: Boolean) {
        _keyboardLivedata.value = visible
    }

    fun setScreenSize(size: IntSize) {
        _screenSizeLivedata.value = size
    }
}