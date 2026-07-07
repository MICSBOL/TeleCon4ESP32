package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import kotlinx.coroutines.flow.StateFlow

interface IWalletRepository {

    val wallet: StateFlow<CoinWalletState>

    suspend fun addCoins(amount: Int)

    suspend fun unlockFeature(
        feature: PremiumFeature,
        option: CoinUnlockOption,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): WalletUnlockResult

    suspend fun clearSessionGrant(feature: PremiumFeature)

    suspend fun pruneExpiredGrants(nowEpochMs: Long = System.currentTimeMillis())
}
