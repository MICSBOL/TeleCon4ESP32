package com.micsbol.telecon4esp32.data.repository

import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumSource
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stand-in repository until [Play Billing][com.android.billingclient.api.BillingClient] is wired.
 *
 * Debug builds report [Entitlement.Premium] for local testing. Release builds stay
 * [Entitlement.Free] until a Play-backed implementation replaces this class.
 */
@Singleton
class InMemoryEntitlementRepository @Inject constructor() : IEntitlementRepository {

    private val _entitlement = MutableStateFlow(
        if (BuildConfig.DEBUG) {
            Entitlement.Premium(PremiumSource.DEBUG_OVERRIDE)
        } else {
            Entitlement.Free
        },
    )

    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    override suspend fun refresh() {
        // No Play connection yet — entitlement unchanged on refresh.
    }
}
