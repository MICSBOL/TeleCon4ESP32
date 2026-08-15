package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.ExplorerGiftPolicy
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.repository.IExplorerGiftRepository
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import javax.inject.Inject

/**
 * 4-hour Pro access after the explorer sparkle easter egg (rewarded ad).
 * Cooldown between claims is governed by [ExplorerGiftPolicy] (3 days).
 */
class ClaimExplorerGiftUseCase @Inject constructor(
    private val explorerGiftRepository: IExplorerGiftRepository,
    private val walletRepository: IWalletRepository,
) {
    suspend operator fun invoke(feature: PremiumFeature): WalletUnlockResult {
        if (!explorerGiftRepository.isAvailable.value) {
            return WalletUnlockResult.AlreadyUnlocked
        }
        val result = walletRepository.grantTimedAccess(feature, CoinUnlockOption.HOURS_4)
        if (result == WalletUnlockResult.Success) {
            explorerGiftRepository.markClaimed()
        }
        return result
    }
}
