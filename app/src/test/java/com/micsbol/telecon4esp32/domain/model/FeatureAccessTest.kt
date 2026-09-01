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
                    expiresAtEpochMs = 4_000_000_000_000L,
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

    @Test
    fun coinMode_debugPremium_doesNotUnlockWithoutGrant() {
        assertFalse(
            hasFeatureAccess(
                entitlement = Entitlement.Premium(PremiumSource.DEBUG_OVERRIDE),
                feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                wallet = CoinWalletState.Empty,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun coinMode_withGrant_unlocksFeature() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            hasFeatureAccess(
                entitlement = Entitlement.Premium(PremiumSource.DEBUG_OVERRIDE),
                feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                wallet = wallet,
                requiresCoinEntry = true,
                nowEpochMs = 1L,
            ),
        )
    }

    @Test
    fun purchasePremium_withoutCoinMode_unlocksFeature() {
        assertTrue(
            hasFeatureAccess(
                entitlement = Entitlement.Premium(PremiumSource.PURCHASE),
                feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                wallet = CoinWalletState.Empty,
                requiresCoinEntry = false,
            ),
        )
    }

    @Test
    fun centerExtrasGrant_doesNotUnlockAdvancedProtocol() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.GREENHOUSE,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun cameraGrant_doesNotUnlockRadar() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelRadar(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun radarGrant_doesNotUnlockCamera() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_RADAR to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_RADAR,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelRadar(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun advancedProtocolGrant_doesNotUnlockControlPanelCenterExtras() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.ADVANCED_PROTOCOL to FeatureGrant(
                    feature = PremiumFeature.ADVANCED_PROTOCOL,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertTrue(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun freeUser_withoutGrants_cannotUseControlPanelAdvancedBundle() {
        assertFalse(
            Entitlement.Free.canUseControlPanelCenterExtras(requiresCoinEntry = true),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelRadar(requiresCoinEntry = true),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelSessionCsv(requiresCoinEntry = true),
        )
        assertFalse(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun sessionCsvGrant_doesNotUnlockCameraRadarOrAdvanced() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_SESSION_CSV to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_SESSION_CSV,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelSessionCsv(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelRadar(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun cameraGrant_doesNotUnlockSessionCsv() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelSessionCsv(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }
}
