package com.elfefe.goodwine

import android.app.Application
import com.elfefe.goodwine.mvvm.repository.CameraRepository
import com.elfefe.goodwine.mvvm.repository.FirebaseRepository
import com.elfefe.goodwine.mvvm.Mediator
import com.elfefe.goodwine.mvvm.repository.OltpRepository
import com.elfefe.goodwine.utils.resString
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class BaseApplication: Application() {
    lateinit var cameraRepository: CameraRepository
    lateinit var oltpRepository: OltpRepository
    lateinit var firebaseRepository: FirebaseRepository
    lateinit var mediator: Mediator

    override fun onCreate() {
        super.onCreate()
        instance = this

        mediator = Mediator()

        FacebookSdk.setApplicationId(resString(R.string.facebook_app_id))
        FacebookSdk.sdkInitialize(this)
        AppEventsLogger.activateApp(this)
    }

    companion object {
        lateinit var instance: BaseApplication
    }
}