package com.micsbol.emitterapp.ui.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAtMs = 0L
    private var sessionShowCount = 0

    fun preload() {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdPolicy.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows an interstitial when policy allows, then runs [onComplete].
     * Navigation must not be blocked when the ad is unavailable.
     */
    fun tryShow(
        activity: Activity,
        trigger: InterstitialTrigger,
        onComplete: () -> Unit,
    ) {
        if (!AdPolicy.shouldShowInterstitial(trigger)) {
            onComplete()
            return
        }
        if (!canShowNow()) {
            onComplete()
            preload()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onComplete()
            preload()
            return
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                recordShown()
                preload()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial show failed: ${error.message}")
                preload()
                onComplete()
            }
        }
        ad.show(activity)
    }

    private fun canShowNow(): Boolean {
        if (sessionShowCount >= AdPolicy.MAX_INTERSTITIALS_PER_SESSION) return false
        if (lastShownAtMs == 0L) return true
        val elapsed = SystemClock.elapsedRealtime() - lastShownAtMs
        return elapsed >= AdPolicy.MIN_INTERSTITIAL_INTERVAL_MS
    }

    private fun recordShown() {
        lastShownAtMs = SystemClock.elapsedRealtime()
        sessionShowCount++
    }

    companion object {
        private const val TAG = "InterstitialAdManager"
    }
}
