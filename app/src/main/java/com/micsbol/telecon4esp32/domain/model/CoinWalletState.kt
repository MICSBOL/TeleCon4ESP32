package com.micsbol.telecon4esp32.domain.model

/**
 * Persisted balance plus active per-feature unlock grants.
 */
data class CoinWalletState(
    val balance: Int = 0,
    val grants: Map<PremiumFeature, FeatureGrant> = emptyMap(),
) {
    companion object {
        val Empty = CoinWalletState()
    }
}
