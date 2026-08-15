package com.micsbol.telecon4esp32.domain.model

/**
 * Coin spend tiers for unlocking a single [PremiumFeature].
 *
 * All costs are multiples of 7 — see [coinCost].
 */
enum class CoinUnlockOption {
    ONE_USE,
    /** Explorer-gift easter egg only — not sold for coins. */
    HOURS_4,
    HOURS_24,
    DAYS_3,
    WEEK,
    ;

    val coinCost: Int
        get() = when (this) {
            ONE_USE -> 7
            HOURS_4 -> 0
            HOURS_24 -> 21
            DAYS_3 -> 49
            WEEK -> 77
        }

    /** Options shown in coin unlock / pricing UI. */
    val isCoinPurchasable: Boolean
        get() = coinCost > 0

    /** `null` means access lasts until the user leaves the pro application. */
    fun durationMillis(): Long? = when (this) {
        ONE_USE -> null
        HOURS_4 -> 4L * 60 * 60 * 1000
        HOURS_24 -> 24L * 60 * 60 * 1000
        DAYS_3 -> 3L * 24 * 60 * 60 * 1000
        WEEK -> 7L * 24 * 60 * 60 * 1000
    }

    val usesEquivalent: Int
        get() = if (coinCost == 0) 0 else coinCost / CoinEconomy.COIN_UNIT

    companion object {
        fun fromStoredName(name: String): CoinUnlockOption? {
            val normalized = when (name) {
                "HOURS_12" -> "HOURS_4" // legacy explorer-gift grants
                else -> name
            }
            return runCatching { valueOf(normalized) }.getOrNull()
        }
    }
}
