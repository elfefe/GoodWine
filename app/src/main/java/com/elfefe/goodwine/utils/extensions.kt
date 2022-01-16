package com.elfefe.goodwine.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.input.InputManager
import android.media.Image
import android.os.Environment
import android.view.inputmethod.InputMethodManager
import com.elfefe.goodwine.BaseApplication
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*


val app = BaseApplication.instance

fun resString(id: Int) = BaseApplication.instance.getString(id)

val timestamp: Long
    get() = Date().time

val prefs = app.getSharedPreferences("Default", Context.MODE_PRIVATE)

const val FIRST_USE_TAG = "First use tag"

fun Image.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun saveImage(image: Bitmap, name: String): String {
    val dir = ContextWrapper(BaseApplication.instance).filesDir
    val picture = File(dir, name)
    try {
        val fos = FileOutputStream(picture)
        image.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return picture.absolutePath
}