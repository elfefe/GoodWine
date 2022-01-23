package com.elfefe.goodwine.mvvm

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.elfefe.goodwine.mvvm.repository.CameraRepository
import com.elfefe.goodwine.mvvm.repository.FirebaseRepository
import com.elfefe.goodwine.mvvm.repository.OltpRepository
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.utils.enums.Connection
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

class Mediator {
    private val cameraRepository = CameraRepository()
    private val firebaseRepository = FirebaseRepository()
    private val oltpRepository = OltpRepository()

    val captureFlow: StateFlow<Bitmap?>
        get() = cameraRepository.captureFlow

    val user: FirebaseUser?
        get() = firebaseRepository.user

    val connectionFlow: StateFlow<Connection?>
        get() = firebaseRepository.connectionFlow

    val bottleFlow: StateFlow<List<Bottle>>
        get() = oltpRepository.bottleFlow

    fun startCapture(lifecycleOwner: LifecycleOwner) = cameraRepository.startCamera(lifecycleOwner)
    fun stopCapture() = cameraRepository.stopCamera()
    @androidx.camera.core.ExperimentalGetImage
    fun takePicture() = cameraRepository.takePicture()
    fun addPreviewView(previewView: PreviewView) = cameraRepository.addPreviewView(previewView)

    fun checkConnection() = firebaseRepository.checkConnection()
    fun connectAnonymoulsy() = firebaseRepository.connectAnonymous()
    fun connectFacebook(activity: ComponentActivity) = firebaseRepository.connectFacebook(activity)
    fun onFacebookResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {
        firebaseRepository.onFacebookResult(requestCode, resultCode, data)
    }
    fun connectPhone(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) = firebaseRepository.connectPhone(activity, onSuccess, onFailure)

    fun updateBottlesRatingOrder(asc: Boolean) = oltpRepository.updateBottlesRatingOrder(asc)
    fun updateBottlesDateOrder(asc: Boolean) = oltpRepository.updateBottlesDateOrder(asc)
    fun saveBottle(bottle: Bottle) = oltpRepository.saveBottle(bottle)
}