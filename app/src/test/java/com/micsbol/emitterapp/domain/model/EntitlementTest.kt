package com.micsbol.emitterapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementTest {

    @Test
    fun freeUser_shouldShowAds() {
        assertTrue(Entitlement.Free.shouldShowAds())
    }

    @Test
    fun premiumUser_shouldNotShowAds() {
        assertFalse(
            Entitlement.Premium(PremiumSource.PURCHASE).shouldShowAds(),
        )
    }

    @Test
    fun premiumUser_hasAllPremiumFeatures() {
        assertTrue(Entitlement.Premium(PremiumSource.PURCHASE).has(PremiumFeature.AD_FREE))
    }

    @Test
    fun freeUser_hasNoPremiumFeatures() {
        assertFalse(Entitlement.Free.has(PremiumFeature.AD_FREE))
    }
}
