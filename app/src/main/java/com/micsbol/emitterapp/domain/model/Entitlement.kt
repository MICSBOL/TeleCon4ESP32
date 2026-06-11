package com.micsbol.emitterapp.domain.model

/**
 * User access level for monetized capabilities.
 *
 * UI and ad policy depend on this type instead of querying Play Billing directly.
 */
sealed interface Entitlement {

    data object Free : Entitlement

    data class Premium(val source: PremiumSource) : Entitlement
}

/** Free users see ads; premium users do not. */
fun Entitlement.shouldShowAds(): Boolean = this is Entitlement.Free

/**
 * v1: a single premium SKU unlocks every [PremiumFeature].
 * Split per-feature checks here when multiple products exist.
 */
fun Entitlement.has(feature: PremiumFeature): Boolean = when (this) {
    is Entitlement.Premium -> true
    is Entitlement.Free -> false
}
