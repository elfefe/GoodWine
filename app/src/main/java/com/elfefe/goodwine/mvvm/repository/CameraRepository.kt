package com.elfefe.goodwine.mvvm.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.utils.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class CameraRepository {
    private val cameraProviderFuture = ProcessCameraProvider
        .getInstance(BaseApplication.instance)

    private var previewView: PreviewView? = null
    private var provider: ProcessCameraProvider? = null
    private var capture: ImageCapture? = null

    private val _captureFlow: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val captureFlow: StateFlow<Bitmap?>
        get() = _captureFlow

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        if (previewView == null) {
            // Échouait en silence quand l'appel précédait l'affichage de la vue.
            Log.w(javaClass.simpleName, "Aucune PreviewView enregistrée : caméra non démarrée")
            return
        }
        previewView?.let {
            cameraProviderFuture.addListener({
                provider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { preview ->
                        preview.setSurfaceProvider(it.surfaceProvider)
                    }
                capture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    provider?.unbindAll()
                    provider?.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, capture
                    )
                } catch (exc: Exception) {
                    Log.e(javaClass.simpleName, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(it.context))
        }
    }

    fun stopCamera() {
        provider?.unbindAll()
    }

    fun addPreviewView(previewView: PreviewView) {
        this.previewView = previewView
    }

    @androidx.camera.core.ExperimentalGetImage
    fun takePicture() {
        previewView?.let {
            capture?.takePicture(
                ContextCompat.getMainExecutor(it.context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        image.image?.let {
                            _captureFlow.value = it.toBitmap()
                        }
                    }
                })
        }
    }
}