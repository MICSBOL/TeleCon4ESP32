package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CoinUnlockOptionTest {

    @Test
    fun rcVehicleTable_purchasableOptions_areMultiplesOfSeven() {
        CoinUnlockOption.entries.filter { it.isCoinPurchasable }.forEach { option ->
            assertEquals(0, option.coinCost(CoinPriceTable.RC_VEHICLE) % CoinEconomy.COIN_UNIT)
        }
    }

    @Test
    fun rcVehicleTable_usesOriginalPricing() {
        assertEquals(7, CoinUnlockOption.ONE_USE.coinCost(PremiumFeature.RC_VEHICLE_PRO))
        assertEquals(0, CoinUnlockOption.HOURS_4.coinCost(PremiumFeature.RC_VEHICLE_PRO))
        assertEquals(21, CoinUnlockOption.HOURS_24.coinCost(PremiumFeature.RC_VEHICLE_PRO))
        assertEquals(49, CoinUnlockOption.DAYS_3.coinCost(PremiumFeature.RC_VEHICLE_PRO))
        assertEquals(77, CoinUnlockOption.WEEK.coinCost(PremiumFeature.RC_VEHICLE_PRO))
    }

    @Test
    fun standardTable_oneUseIsThreeCoins() {
        assertEquals(3, CoinUnlockOption.ONE_USE.coinCost(PremiumFeature.CONTROL_PANEL_STICK))
        assertEquals(3, CoinUnlockOption.ONE_USE.coinCost(PremiumFeature.CONTROL_PANEL_SESSION_CSV))
        assertEquals(3, CoinUnlockOption.ONE_USE.coinCost(PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS))
        assertEquals(7, CoinUnlockOption.HOURS_24.coinCost(PremiumFeature.CONTROL_PANEL_STICK))
        assertEquals(14, CoinUnlockOption.DAYS_3.coinCost(PremiumFeature.CONTROL_PANEL_STICK))
        assertEquals(21, CoinUnlockOption.WEEK.coinCost(PremiumFeature.CONTROL_PANEL_STICK))
    }

    @Test
    fun hours4_isFourHours() {
        assertEquals(4L * 60 * 60 * 1000, CoinUnlockOption.HOURS_4.durationMillis())
        assertFalse(CoinUnlockOption.HOURS_4.isCoinPurchasable)
    }

    @Test
    fun fromStoredName_mapsLegacyHours12() {
        assertEquals(CoinUnlockOption.HOURS_4, CoinUnlockOption.fromStoredName("HOURS_12"))
        assertEquals(CoinUnlockOption.HOURS_4, CoinUnlockOption.fromStoredName("HOURS_4"))
    }

    @Test
    fun oneUse_isSessionOnly() {
        assertEquals(null, CoinUnlockOption.ONE_USE.durationMillis())
    }
}
