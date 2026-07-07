package com.micsbol.telecon4esp32

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.micsbol.telecon4esp32.ui.ads.InterstitialAdManager
import com.micsbol.telecon4esp32.ui.ads.RewardedAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TeleCon4Esp32App : Application() {

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        interstitialAdManager.preload()
        rewardedAdManager.preload()
    }
}
