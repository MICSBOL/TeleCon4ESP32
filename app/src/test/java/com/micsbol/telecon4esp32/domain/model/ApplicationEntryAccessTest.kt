package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationEntryAccessTest {

    @Test
    fun freeApp_alwaysHasAccess() {
        assertTrue(
            ApplicationId.CONTROL_PANEL.hasEntryAccess(
                entitlement = Entitlement.Free,
                wallet = CoinWalletState.Empty,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun deferredApp_neverHasEntryAccessWhileUnshipped() {
        assertFalse(ApplicationId.GREENHOUSE.isShipped())
        assertFalse(
            ApplicationId.GREENHOUSE.hasEntryAccess(
                entitlement = Entitlement.Premium(PremiumSource.PURCHASE),
                wallet = CoinWalletState.Empty,
                requiresCoinEntry = false,
            ),
        )
    }

    @Test
    fun proApp_expiredGrant_noAccessInCoinMode() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.RC_VEHICLE_PRO to FeatureGrant(
                    feature = PremiumFeature.RC_VEHICLE_PRO,
                    option = CoinUnlockOption.HOURS_4,
                    expiresAtEpochMs = 1_000L,
                ),
            ),
        )
        assertFalse(
            ApplicationId.RC_VEHICLE_PRO.hasEntryAccess(
                entitlement = Entitlement.Free,
                wallet = wallet,
                requiresCoinEntry = true,
                nowEpochMs = 2_000L,
            ),
        )
    }

    @Test
    fun resolveHomeFeatured_fallsBackToControlPanelWhenExpired() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.GREENHOUSE to FeatureGrant(
                    feature = PremiumFeature.GREENHOUSE,
                    option = CoinUnlockOption.HOURS_4,
                    expiresAtEpochMs = 1_000L,
                ),
            ),
        )
        val featured = resolveHomeFeaturedApplication(
            activeSessionApplicationId = null,
            lastApplicationId = ApplicationId.GREENHOUSE,
            entitlement = Entitlement.Free,
            wallet = wallet,
            requiresCoinEntry = true,
            nowEpochMs = 5_000L,
        )
        assertEquals(ApplicationId.CONTROL_PANEL, featured)
    }

    @Test
    fun resolveHomeFeatured_ignoresOneUseSessionGrant() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.RC_VEHICLE_PRO to FeatureGrant(
                    feature = PremiumFeature.RC_VEHICLE_PRO,
                    option = CoinUnlockOption.ONE_USE,
                    expiresAtEpochMs = null,
                ),
            ),
        )
        val featured = resolveHomeFeaturedApplication(
            activeSessionApplicationId = ApplicationId.RC_VEHICLE_PRO,
            lastApplicationId = ApplicationId.RC_VEHICLE_PRO,
            entitlement = Entitlement.Free,
            wallet = wallet,
            requiresCoinEntry = true,
            nowEpochMs = 5_000L,
        )
        assertEquals(ApplicationId.CONTROL_PANEL, featured)
    }

    @Test
    fun hasEntryAccess_excludesSessionGrantWhenRequested() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.RC_VEHICLE_PRO to FeatureGrant(
                    feature = PremiumFeature.RC_VEHICLE_PRO,
                    option = CoinUnlockOption.ONE_USE,
                    expiresAtEpochMs = null,
                ),
            ),
        )
        assertTrue(
            ApplicationId.RC_VEHICLE_PRO.hasEntryAccess(
                entitlement = Entitlement.Free,
                wallet = wallet,
                requiresCoinEntry = true,
                includeSessionGrants = true,
            ),
        )
        assertFalse(
            ApplicationId.RC_VEHICLE_PRO.hasEntryAccess(
                entitlement = Entitlement.Free,
                wallet = wallet,
                requiresCoinEntry = true,
                includeSessionGrants = false,
            ),
        )
    }

    @Test
    fun catalogHiddenWhileFewAppsShipped() {
        assertEquals(2, shippedApplicationCount())
        assertFalse(isApplicationCatalogVisible())
    }
}
