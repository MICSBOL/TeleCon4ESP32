package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import javax.inject.Inject

class RefreshEntitlementUseCase @Inject constructor(
    private val repository: IEntitlementRepository,
) {
    suspend operator fun invoke() {
        repository.refresh()
    }
}
