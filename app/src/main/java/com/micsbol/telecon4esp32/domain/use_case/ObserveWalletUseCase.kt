package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveWalletUseCase @Inject constructor(
    private val repository: IWalletRepository,
) {
    operator fun invoke(): StateFlow<CoinWalletState> = repository.wallet
}
