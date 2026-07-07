package com.micsbol.telecon4esp32.domain.model

/**
 * Global coin economy constants.
 */
object CoinEconomy {
    const val COIN_UNIT = 7
    const val COINS_PER_REWARDED_AD = 3

    /** Starting balance seeded once in debug builds when the wallet is empty. */
    const val DEBUG_STARTING_BALANCE = 500
}
