package com.micsbol.emitterapp.ui.ads

import com.micsbol.emitterapp.domain.model.Entitlement
import com.micsbol.emitterapp.domain.model.shouldShowAds
import com.micsbol.emitterapp.ui.navigation.Screen

/**
 * Central ad placement rules for EmitterApp.
 *
 * Recurring sessions: Home banner + interstitial when leaving RC.
 * Rare visits (1–3×): interstitial when leaving Codes only.
 * Premium users never see ads — see [Entitlement.shouldShowAds].
 */
object AdPolicy {
    /** Google sample ad unit — replace before Play release. */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    /** Google sample ad unit — replace before Play release. */
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    const val MIN_INTERSTITIAL_INTERVAL_MS = 4 * 60 * 1000L
    const val MAX_INTERSTITIALS_PER_SESSION = 3

    private val bannerRoutes = setOf(
        Screen.Home.route,
        Screen.Bluetooth.route,
    )

    fun hasBanner(route: String, entitlement: Entitlement): Boolean =
        entitlement.shouldShowAds() && route in bannerRoutes

    fun shouldShowInterstitial(trigger: InterstitialTrigger, entitlement: Entitlement): Boolean =
        entitlement.shouldShowAds() && when (trigger) {
            InterstitialTrigger.CODES_EXIT,
            InterstitialTrigger.RC_EXIT -> true
        }
}

enum class InterstitialTrigger {
    CODES_EXIT,
    RC_EXIT,
}
