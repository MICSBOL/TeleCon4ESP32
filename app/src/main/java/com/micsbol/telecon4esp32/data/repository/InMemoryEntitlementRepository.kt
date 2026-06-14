package com.micsbol.telecon4esp32.data.repository

import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stand-in repository until [Play Billing][com.android.billingclient.api.BillingClient] is wired.
 *
 * Always reports [Entitlement.Free]. Replace the Hilt binding with a Play-backed
 * implementation when purchases are ready.
 */
@Singleton
class InMemoryEntitlementRepository @Inject constructor() : IEntitlementRepository {

    private val _entitlement = MutableStateFlow<Entitlement>(Entitlement.Free)

    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    override suspend fun refresh() {
        // No Play connection yet — entitlement stays Free.
    }
}
