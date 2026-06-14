package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveEntitlementUseCase @Inject constructor(
    private val repository: IEntitlementRepository,
) {
    operator fun invoke(): StateFlow<Entitlement> = repository.entitlement
}
