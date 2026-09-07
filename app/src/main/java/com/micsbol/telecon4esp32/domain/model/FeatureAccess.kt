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

/**
 * Feature gate that matches Catalog / Home coin mode.
 *
 * When [requiresCoinEntry] is true (free tier or debug coin testing), only an active
 * wallet grant unlocks the feature — Premium DEBUG_OVERRIDE does not auto-enable it.
 * Otherwise a real Premium entitlement is enough.
 */
fun hasFeatureAccess(
    entitlement: Entitlement,
    feature: PremiumFeature,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    nowEpochMs: Long = System.currentTimeMillis(),
): Boolean {
    if (!requiresCoinEntry && entitlement.has(feature)) return true
    if (!requiresCoinEntry) return false
    val grant = wallet.grants[feature] ?: return false
    return grant.isActive(nowEpochMs)
}

fun Entitlement.usesCoinEconomy(): Boolean = this is Entitlement.Free

/**
 * Control Panel camera center view. Independent of Advanced protocol and radar.
 */
fun Entitlement.canUseControlPanelCenterExtras(
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = hasFeatureAccess(
    entitlement = this,
    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
    wallet = wallet,
    requiresCoinEntry = requiresCoinEntry,
)

/** Control Panel radar center view. Independent of Advanced protocol and camera. */
fun Entitlement.canUseControlPanelRadar(
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = hasFeatureAccess(
    entitlement = this,
    feature = PremiumFeature.CONTROL_PANEL_RADAR,
    wallet = wallet,
    requiresCoinEntry = requiresCoinEntry,
)

/** Control Panel stick XY graph. Independent of camera, radar, and Advanced. */
fun Entitlement.canUseControlPanelStick(
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = hasFeatureAccess(
    entitlement = this,
    feature = PremiumFeature.CONTROL_PANEL_STICK,
    wallet = wallet,
    requiresCoinEntry = requiresCoinEntry,
)

/** Control Panel session CSV recording. Independent of camera, radar, and Advanced. */
fun Entitlement.canUseControlPanelSessionCsv(
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = hasFeatureAccess(
    entitlement = this,
    feature = PremiumFeature.CONTROL_PANEL_SESSION_CSV,
    wallet = wallet,
    requiresCoinEntry = requiresCoinEntry,
)
