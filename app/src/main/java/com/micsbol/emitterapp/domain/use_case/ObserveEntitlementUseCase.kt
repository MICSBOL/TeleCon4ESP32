package com.micsbol.emitterapp.domain.use_case

import com.micsbol.emitterapp.domain.model.Entitlement
import com.micsbol.emitterapp.domain.repository.IEntitlementRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveEntitlementUseCase @Inject constructor(
    private val repository: IEntitlementRepository,
) {
    operator fun invoke(): StateFlow<Entitlement> = repository.entitlement
}
