package com.elfefe.goodwine.mvvm.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.elfefe.goodwine.utils.TutorialItem

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

    private val _permittedLivedata = MutableLiveData<Boolean>()
    val permittedLivedata: LiveData<Boolean>
        get() = _permittedLivedata

    private val _descriptionItemLivedata = MutableLiveData<TutorialItem>()
    val descriptionItemLivedata: LiveData<TutorialItem>
        get() = _descriptionItemLivedata

    fun setPermitted(value: Boolean) {
        _permittedLivedata.value = value
    }

    fun setBottle(value: Boolean) {
        _addBottleLivedata.value = value
    }

    fun setKeyboard(visible: Boolean) {
        _keyboardLivedata.value = visible
    }

    fun setScreenSize(size: IntSize) {
        _screenSizeLivedata.value = size
    }

    fun setDescriptionItem(item: TutorialItem) {
        _descriptionItemLivedata.value = item
    }
}