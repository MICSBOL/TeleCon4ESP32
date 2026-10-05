package com.micsbol.telecon4esp32.domain.repository

import android.app.Activity
import com.micsbol.telecon4esp32.domain.billing.PremiumBillingStatus
import com.micsbol.telecon4esp32.domain.model.Entitlement
import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for whether the user has premium access.
 *
 * Implementations map Play Billing to [Entitlement].
 */
interface IEntitlementRepository {

    val entitlement: StateFlow<Entitlement>

    /** True when Play returned product details for the Pro one-time product. */
    val premiumProductAvailable: StateFlow<Boolean>

    val billingStatus: StateFlow<PremiumBillingStatus>

    /** Re-query purchase state from Google Play. */
    suspend fun refresh()

    /** Starts the Play purchase sheet for Pro. */
    fun purchasePremium(activity: Activity)

    /** Re-reads owned purchases and reports restore success or absence. */
    fun restorePurchases()
}
