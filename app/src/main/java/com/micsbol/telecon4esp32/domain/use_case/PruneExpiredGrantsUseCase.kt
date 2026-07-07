package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import javax.inject.Inject

class PruneExpiredGrantsUseCase @Inject constructor(
    private val repository: IWalletRepository,
) {
    suspend operator fun invoke() {
        repository.pruneExpiredGrants()
    }
}
