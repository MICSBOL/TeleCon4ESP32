package com.micsbol.emitterapp

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.micsbol.emitterapp.ui.ads.InterstitialAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EmitterApp : Application() {

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        interstitialAdManager.preload()
    }
}