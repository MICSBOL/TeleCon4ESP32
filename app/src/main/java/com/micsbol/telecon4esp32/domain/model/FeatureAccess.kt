package com.micsbol.telecon4esp32.domain.model

/**
 * Resolves whether a user can use a [PremiumFeature] via subscription or coin grants.
 */
fun hasPremiumAccess(
    entitlement: Entitlement,
    feature: PremiumFeature,
    wallet: CoinWalletState,
    nowEpochMs: Long = System.currentTimeMillis(),
): Boolean {
    if (entitlement.has(feature)) return true
    val grant = wallet.grants[feature] ?: return false
    return grant.isActive(nowEpochMs)
}

fun Entitlement.usesCoinEconomy(): Boolean = this is Entitlement.Free
