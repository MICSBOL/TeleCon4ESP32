package com.micsbol.telecon4esp32.domain.model

/**
 * Whether the user can enter [applicationId] from Catalog / Home.
 *
 * When [requiresCoinEntry] is true (free tier or debug coin mode), Pro apps need an
 * active coin/gift grant. Otherwise a Premium entitlement is enough.
 *
 * @param includeSessionGrants One-use grants live only while inside that Pro module.
 *   Pass `false` on Home so leaving the module consumes one-use access.
 */
fun ApplicationId.hasEntryAccess(
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    nowEpochMs: Long = System.currentTimeMillis(),
    includeSessionGrants: Boolean = true,
): Boolean {
    if (!isShipped()) return false
    if (isFree()) return true
    val feature = premiumFeature() ?: return false
    if (!requiresCoinEntry && entitlement.has(feature)) return true
    if (!requiresCoinEntry) return false
    val grant = wallet.grants[feature] ?: return false
    if (grant.isSessionOnly && !includeSessionGrants) return false
    return grant.isActive(nowEpochMs)
}

/**
 * Home hologram app: last usable timed/subscription app, else Control Panel.
 * Ignores one-use session grants — those end when the user leaves the Pro module.
 */
fun resolveHomeFeaturedApplication(
    activeSessionApplicationId: ApplicationId?,
    lastApplicationId: ApplicationId?,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    nowEpochMs: Long = System.currentTimeMillis(),
): ApplicationId {
    val candidates = listOfNotNull(activeSessionApplicationId, lastApplicationId).distinct()
    for (candidate in candidates) {
        if (
            candidate.hasEntryAccess(
                entitlement = entitlement,
                wallet = wallet,
                requiresCoinEntry = requiresCoinEntry,
                nowEpochMs = nowEpochMs,
                includeSessionGrants = false,
            )
        ) {
            return candidate
        }
    }
    return ApplicationId.CONTROL_PANEL
}
