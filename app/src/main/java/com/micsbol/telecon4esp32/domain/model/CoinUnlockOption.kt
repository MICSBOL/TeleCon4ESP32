package com.micsbol.telecon4esp32.domain.model

/**
 * Which coin price list a [PremiumFeature] uses.
 *
 * [RC_VEHICLE] is the original catalog table (7 / 21 / 49 / 77).
 * [STANDARD] is the in-app feature table (3 / 7 / 14 / 21), with one-use at one rewarded ad.
 */
enum class CoinPriceTable {
    RC_VEHICLE,
    STANDARD,
}

fun PremiumFeature.coinPriceTable(): CoinPriceTable =
    if (this == PremiumFeature.RC_VEHICLE_PRO) {
        CoinPriceTable.RC_VEHICLE
    } else {
        CoinPriceTable.STANDARD
    }

/**
 * Coin spend tiers for unlocking a single [PremiumFeature].
 */
enum class CoinUnlockOption {
    ONE_USE,
    /** Explorer-gift easter egg only — not sold for coins. */
    HOURS_4,
    HOURS_24,
    DAYS_3,
    WEEK,
    ;

    fun coinCost(feature: PremiumFeature): Int = coinCost(feature.coinPriceTable())

    fun coinCost(table: CoinPriceTable): Int = when (table) {
        CoinPriceTable.RC_VEHICLE -> when (this) {
            ONE_USE -> 7
            HOURS_4 -> 0
            HOURS_24 -> 21
            DAYS_3 -> 49
            WEEK -> 77
        }
        CoinPriceTable.STANDARD -> when (this) {
            ONE_USE -> 3
            HOURS_4 -> 0
            HOURS_24 -> 7
            DAYS_3 -> 14
            WEEK -> 21
        }
    }

    /** Options shown in coin unlock / pricing UI. */
    val isCoinPurchasable: Boolean
        get() = this != HOURS_4

    /** `null` means access lasts until the user leaves the pro application. */
    fun durationMillis(): Long? = when (this) {
        ONE_USE -> null
        HOURS_4 -> 4L * 60 * 60 * 1000
        HOURS_24 -> 24L * 60 * 60 * 1000
        DAYS_3 -> 3L * 24 * 60 * 60 * 1000
        WEEK -> 7L * 24 * 60 * 60 * 1000
    }

    fun usesEquivalent(feature: PremiumFeature): Int = usesEquivalent(feature.coinPriceTable())

    fun usesEquivalent(table: CoinPriceTable): Int {
        val cost = coinCost(table)
        val oneUse = ONE_USE.coinCost(table)
        if (cost <= 0 || oneUse <= 0) return 0
        return cost / oneUse
    }

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
