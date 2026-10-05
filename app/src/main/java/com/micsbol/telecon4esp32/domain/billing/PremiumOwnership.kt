package com.micsbol.telecon4esp32.domain.billing

/**
 * A one-time Play purchase grants Pro only after Google reports it as purchased.
 * Pending purchases stay locked until Play completes them.
 */
fun grantsPremium(productIds: List<String>, purchased: Boolean): Boolean =
    purchased && BillingConstants.PREMIUM_PRODUCT_ID in productIds

/** Closed-test builds keep Pro when Play has no purchase for this account. */
fun shouldRevokeMissingPremium(
    revokeIfMissing: Boolean,
    pending: Boolean,
    closedTestUnlock: Boolean,
): Boolean = revokeIfMissing && !pending && !closedTestUnlock
