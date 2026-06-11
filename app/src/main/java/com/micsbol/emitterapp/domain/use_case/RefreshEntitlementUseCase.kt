package com.micsbol.emitterapp.domain.use_case

import com.micsbol.emitterapp.domain.repository.IEntitlementRepository
import javax.inject.Inject

class RefreshEntitlementUseCase @Inject constructor(
    private val repository: IEntitlementRepository,
) {
    suspend operator fun invoke() {
        repository.refresh()
    }
}
