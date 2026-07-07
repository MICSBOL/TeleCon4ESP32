package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessTest {

    @Test
    fun premiumUser_hasAccessWithoutGrants() {
        assertTrue(
            hasPremiumAccess(
                Entitlement.Premium(PremiumSource.PURCHASE),
                PremiumFeature.GREENHOUSE,
                CoinWalletState.Empty,
            ),
        )
    }

    @Test
    fun freeUser_withTimedGrant_hasAccess() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.GREENHOUSE to FeatureGrant(
                    feature = PremiumFeature.GREENHOUSE,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 9_999_999_999L,
                ),
            ),
        )
        assertTrue(
            hasPremiumAccess(Entitlement.Free, PremiumFeature.GREENHOUSE, wallet, nowEpochMs = 1L),
        )
    }

    @Test
    fun freeUser_withExpiredGrant_hasNoAccess() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.GREENHOUSE to FeatureGrant(
                    feature = PremiumFeature.GREENHOUSE,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 100L,
                ),
            ),
        )
        assertFalse(
            hasPremiumAccess(Entitlement.Free, PremiumFeature.GREENHOUSE, wallet, nowEpochMs = 200L),
        )
    }

    @Test
    fun freeUser_withSessionGrant_hasAccess() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.WATER_TANK to FeatureGrant(
                    feature = PremiumFeature.WATER_TANK,
                    option = CoinUnlockOption.ONE_USE,
                    expiresAtEpochMs = null,
                ),
            ),
        )
        assertTrue(
            hasPremiumAccess(Entitlement.Free, PremiumFeature.WATER_TANK, wallet),
        )
    }
}
