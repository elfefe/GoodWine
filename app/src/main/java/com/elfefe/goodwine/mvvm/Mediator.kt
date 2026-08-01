package com.elfefe.goodwine.mvvm

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

    /** Faux quand le build n'a pas de configuration Firebase : l'interface s'y adapte. */
    val cloudAvailable: Boolean
        get() = firebaseRepository.available

    val connectionFlow: StateFlow<Connection?>
        get() = firebaseRepository.connectionFlow

    val bottleFlow: StateFlow<List<Bottle>>
        get() = oltpRepository.bottleFlow

    val remoteBottleFlow: StateFlow<List<Bottle>?>
        get() = firebaseRepository.bottleFlow

    fun startCapture(lifecycleOwner: LifecycleOwner) = cameraRepository.startCamera(lifecycleOwner)
    fun stopCapture() = cameraRepository.stopCamera()

    @androidx.camera.core.ExperimentalGetImage
    fun takePicture() = cameraRepository.takePicture()
    fun addPreviewView(previewView: PreviewView) = cameraRepository.addPreviewView(previewView)

    fun checkConnection() = firebaseRepository.checkConnection()
    fun connectAnonymously() = firebaseRepository.connectAnonymous()
    fun connectFacebook(activity: ComponentActivity) = firebaseRepository.connectFacebook(activity)

    fun onFacebookResult(requestCode: Int, resultCode: Int, data: Intent?) =
        firebaseRepository.onFacebookResult(requestCode, resultCode, data)

    fun connectPhone(
        activity: ComponentActivity,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (Exception?) -> Unit
    ) = firebaseRepository.connectPhone(activity, phoneNumber, onSuccess, onFailure)

    fun updateBottlesRatingOrder(asc: Boolean) = oltpRepository.updateBottlesRatingOrder(asc)
    fun updateBottlesDateOrder(asc: Boolean) = oltpRepository.updateBottlesDateOrder(asc)
    fun saveBottle(bottle: Bottle) = oltpRepository.saveBottle(bottle)

    /** Supprime la bouteille localement, et dans le cloud si celui-ci est disponible. */
    fun deleteBottle(bottle: Bottle) {
        oltpRepository.deleteBottle(bottle)
        firebaseRepository.deleteBottle(bottle)
    }

    /** Rapatrie ce que le téléphone n'a pas, puis pousse l'état local. */
    fun syncBottles() {
        val local = oltpRepository.bottleFlow.value
        firebaseRepository.syncData(local.map { it.id })
        firebaseRepository.sendBottles(local)
    }

    fun saveRemoteBottles(bottles: List<Bottle>) = bottles.forEach(oltpRepository::saveBottle)
}
