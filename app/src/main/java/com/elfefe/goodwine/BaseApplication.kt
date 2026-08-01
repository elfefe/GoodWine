package com.elfefe.goodwine

import android.app.Application
import android.util.Log
import com.elfefe.goodwine.mvvm.Mediator
import com.elfefe.goodwine.utils.resString
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class BaseApplication : Application() {
    lateinit var mediator: Mediator
        private set

    /**
     * Faux quand aucun client token Facebook n'est fourni : la connexion Facebook est alors
     * indisponible, mais le reste de l'app fonctionne.
     *
     * Depuis sa version 13, le SDK Facebook **lève** dans `sdkInitialize` si le client token
     * manque, là où la version 12 d'origine se contentait d'un avertissement. Sans cette
     * garde, l'app ne démarre plus du tout.
     */
    var facebookAvailable: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        mediator = Mediator()

        initFacebook()
    }

    private fun initFacebook() {
        val clientToken = resString(R.string.facebook_client_token)
        if (clientToken.isBlank()) {
            Log.i(TAG, "Facebook désactivé : aucun client token fourni.")
            return
        }
        facebookAvailable = runCatching {
            FacebookSdk.setApplicationId(resString(R.string.facebook_app_id))
            FacebookSdk.setClientToken(clientToken)
            FacebookSdk.sdkInitialize(this)
            AppEventsLogger.activateApp(this)
            true
        }.getOrElse {
            Log.w(TAG, "Initialisation du SDK Facebook impossible", it)
            false
        }
    }

    companion object {
        private const val TAG = "BaseApplication"

        lateinit var instance: BaseApplication
            private set
    }
}
