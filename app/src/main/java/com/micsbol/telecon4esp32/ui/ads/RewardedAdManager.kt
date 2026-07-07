package com.micsbol.telecon4esp32.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import com.micsbol.telecon4esp32.domain.use_case.GrantCoinsFromRewardedAdUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlementRepository: IEntitlementRepository,
    private val grantCoinsFromRewardedAd: GrantCoinsFromRewardedAdUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload() {
        if (!entitlementRepository.entitlement.value.usesCoinEconomy()) return
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            AdPolicy.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    Log.w(TAG, "Rewarded load failed: ${error.message}")
                }
            },
        )
    }

    fun tryShow(
        activity: Activity,
        onComplete: (rewardGranted: Boolean) -> Unit,
    ) {
        if (!entitlementRepository.entitlement.value.usesCoinEconomy()) {
            onComplete(false)
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            onComplete(false)
            preload()
            return
        }

        rewardedAd = null
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                onComplete(rewardGranted)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded show failed: ${error.message}")
                preload()
                onComplete(false)
            }
        }
        ad.show(activity) {
            rewardGranted = true
            scope.launch {
                grantCoinsFromRewardedAd()
            }
        }
    }

    companion object {
        private const val TAG = "RewardedAdManager"
    }
}
