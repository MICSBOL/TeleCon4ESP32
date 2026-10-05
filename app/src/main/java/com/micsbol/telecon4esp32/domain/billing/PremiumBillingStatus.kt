package com.micsbol.telecon4esp32.domain.billing

/**
 * Play Billing state shown on the upgrade screen.
 */
enum class PremiumBillingStatus {
    Connecting,
    Ready,
    Unavailable,
    ProductUnavailable,
    Pending,
    Restored,
    NoPurchaseFound,
    Failed,
}
