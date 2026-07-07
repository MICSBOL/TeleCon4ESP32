package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinUnlockOptionTest {

    @Test
    fun allOptions_areMultiplesOfSeven() {
        CoinUnlockOption.entries.forEach { option ->
            assertEquals(0, option.coinCost % CoinEconomy.COIN_UNIT)
        }
    }

    @Test
    fun pricing_matchesEconomyTable() {
        assertEquals(7, CoinUnlockOption.ONE_USE.coinCost)
        assertEquals(21, CoinUnlockOption.HOURS_24.coinCost)
        assertEquals(49, CoinUnlockOption.DAYS_3.coinCost)
        assertEquals(77, CoinUnlockOption.WEEK.coinCost)
    }

    @Test
    fun oneUse_isSessionOnly() {
        assertEquals(null, CoinUnlockOption.ONE_USE.durationMillis())
    }
}
