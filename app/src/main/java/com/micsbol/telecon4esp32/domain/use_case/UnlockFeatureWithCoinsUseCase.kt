package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import javax.inject.Inject

class UnlockFeatureWithCoinsUseCase @Inject constructor(
    private val repository: IWalletRepository,
) {
    suspend operator fun invoke(
        feature: PremiumFeature,
        option: CoinUnlockOption,
    ): WalletUnlockResult = repository.unlockFeature(feature, option)
}
