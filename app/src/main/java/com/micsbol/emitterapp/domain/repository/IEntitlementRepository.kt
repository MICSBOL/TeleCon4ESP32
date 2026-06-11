package com.micsbol.emitterapp.domain.repository

import com.micsbol.emitterapp.domain.model.Entitlement
import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for whether the user has premium access.
 *
 * Implementations map Play Billing (or a dev stand-in) to [Entitlement].
 */
interface IEntitlementRepository {

    val entitlement: StateFlow<Entitlement>

    /** Re-query purchase state from Google Play. No-op until billing is wired. */
    suspend fun refresh()
}
