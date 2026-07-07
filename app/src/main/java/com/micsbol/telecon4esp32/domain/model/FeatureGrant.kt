package com.micsbol.telecon4esp32.domain.model

/**
 * Temporary access to a [PremiumFeature] purchased with coins.
 *
 * @param expiresAtEpochMs When access ends; `null` means session-only until cleared.
 */
data class FeatureGrant(
    val feature: PremiumFeature,
    val option: CoinUnlockOption,
    val expiresAtEpochMs: Long?,
) {
    val isSessionOnly: Boolean get() = expiresAtEpochMs == null

    fun isActive(nowEpochMs: Long): Boolean =
        expiresAtEpochMs == null || expiresAtEpochMs > nowEpochMs
}
