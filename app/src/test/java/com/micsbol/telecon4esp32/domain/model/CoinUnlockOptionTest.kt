package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CoinUnlockOptionTest {

    @Test
    fun allPurchasableOptions_areMultiplesOfSeven() {
        CoinUnlockOption.entries.filter { it.isCoinPurchasable }.forEach { option ->
            assertEquals(0, option.coinCost % CoinEconomy.COIN_UNIT)
        }
    }

    @Test
    fun pricing_matchesEconomyTable() {
        assertEquals(7, CoinUnlockOption.ONE_USE.coinCost)
        assertEquals(0, CoinUnlockOption.HOURS_4.coinCost)
        assertEquals(21, CoinUnlockOption.HOURS_24.coinCost)
        assertEquals(49, CoinUnlockOption.DAYS_3.coinCost)
        assertEquals(77, CoinUnlockOption.WEEK.coinCost)
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
