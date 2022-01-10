package com.elfefe.goodwine

import android.app.Application
import com.elfefe.goodwine.mvvm.repository.CameraRepository
import com.elfefe.goodwine.mvvm.repository.FirebaseRepository
import com.elfefe.goodwine.mvvm.repository.OltpRepository

class BaseApplication: Application() {
    lateinit var cameraRepository: CameraRepository
    lateinit var oltpRepository: OltpRepository
    lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        cameraRepository = CameraRepository()
        oltpRepository = OltpRepository()
        firebaseRepository = FirebaseRepository()
    }

    companion object {
        lateinit var instance: BaseApplication
    }
}