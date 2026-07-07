package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.CoinEconomy
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import javax.inject.Inject

class GrantCoinsFromRewardedAdUseCase @Inject constructor(
    private val repository: IWalletRepository,
) {
    suspend operator fun invoke() {
        repository.addCoins(CoinEconomy.COINS_PER_REWARDED_AD)
    }
}
