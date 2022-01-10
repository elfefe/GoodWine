package com.elfefe.goodwine.mvvm.viewmodel

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.*
import com.elfefe.goodwine.BaseApplication
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CameraViewmodel: ViewModel() {
    private val repository = BaseApplication.instance.cameraRepository

    private val _pictureLivedata = MutableLiveData<Bitmap>()
    val pictureLivedata: LiveData<Bitmap>
        get() = _pictureLivedata

    fun addPreviewView(previewView: PreviewView) = repository.addPreviewView(previewView)
    fun startCamera(lifecycleOwner: LifecycleOwner) = repository.startCamera(lifecycleOwner)
    fun stopCamera() = repository.stopCamera()

    @androidx.camera.core.ExperimentalGetImage
    fun takePicture(): LiveData<Bitmap> = _pictureLivedata.apply {
        repository
            .captureFlow
            .onEach {
                it?.let { bitmap ->
                    postValue(bitmap)
                }
            }
            .launchIn(viewModelScope)
        repository.takePicture()
    }

    fun setPicture(image: Bitmap) {
        _pictureLivedata.value = image
    }
}