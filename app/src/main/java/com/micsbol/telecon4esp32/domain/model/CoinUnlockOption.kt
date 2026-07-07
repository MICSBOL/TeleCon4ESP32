package com.micsbol.telecon4esp32.domain.model

/**
 * Coin spend tiers for unlocking a single [PremiumFeature].
 *
 * All costs are multiples of 7 — see [coinCost].
 */
enum class CoinUnlockOption {
    ONE_USE,
    HOURS_24,
    DAYS_3,
    WEEK,
    ;

    val coinCost: Int
        get() = when (this) {
            ONE_USE -> 7
            HOURS_24 -> 21
            DAYS_3 -> 49
            WEEK -> 77
        }

    /** `null` means access lasts until the user leaves the pro application. */
    fun durationMillis(): Long? = when (this) {
        ONE_USE -> null
        HOURS_24 -> 24L * 60 * 60 * 1000
        DAYS_3 -> 3L * 24 * 60 * 60 * 1000
        WEEK -> 7L * 24 * 60 * 60 * 1000
    }

    val usesEquivalent: Int
        get() = coinCost / CoinEconomy.COIN_UNIT
}
