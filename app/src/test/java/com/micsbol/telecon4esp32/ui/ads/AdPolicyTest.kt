package com.micsbol.telecon4esp32.ui.ads

import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumSource
import com.micsbol.telecon4esp32.ui.navigation.Screen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test
    fun freeUser_hasBannerOnHome() {
        assertTrue(AdPolicy.hasBanner(Screen.Home.route, Entitlement.Free))
    }

    @Test
    fun premiumUser_hasNoBannerOnHome() {
        assertFalse(
            AdPolicy.hasBanner(
                Screen.Home.route,
                Entitlement.Premium(PremiumSource.PURCHASE),
            ),
        )
    }

    @Test
    fun freeUser_showsInterstitialOnRcExit() {
        assertTrue(
            AdPolicy.shouldShowInterstitial(InterstitialTrigger.RC_EXIT, Entitlement.Free),
        )
    }

    @Test
    fun premiumUser_skipsInterstitialOnRcExit() {
        assertFalse(
            AdPolicy.shouldShowInterstitial(
                InterstitialTrigger.RC_EXIT,
                Entitlement.Premium(PremiumSource.PURCHASE),
            ),
        )
    }
}
