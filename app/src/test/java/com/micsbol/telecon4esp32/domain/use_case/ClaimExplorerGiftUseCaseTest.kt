package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.repository.IExplorerGiftRepository
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClaimExplorerGiftUseCaseTest {

    @Test
    fun claim_grantsTwelveHoursAndMarksClaimed() = runBlocking {
        val gift = FakeExplorerGiftRepository(available = true)
        val wallet = FakeWalletRepository()
        val useCase = ClaimExplorerGiftUseCase(gift, wallet)

        val result = useCase(PremiumFeature.RC_VEHICLE_PRO)

        assertEquals(WalletUnlockResult.Success, result)
        assertFalse(gift.isAvailable.value)
        assertEquals(CoinUnlockOption.HOURS_4, wallet.lastGrantedOption)
    }

    @Test
    fun claim_whenAlreadyUsed_returnsAlreadyUnlocked() = runBlocking {
        val gift = FakeExplorerGiftRepository(available = false)
        val wallet = FakeWalletRepository()
        val useCase = ClaimExplorerGiftUseCase(gift, wallet)

        val result = useCase(PremiumFeature.RC_VEHICLE_PRO)

        assertEquals(WalletUnlockResult.AlreadyUnlocked, result)
        assertEquals(null, wallet.lastGrantedOption)
    }

    private class FakeExplorerGiftRepository(
        available: Boolean,
    ) : IExplorerGiftRepository {
        override val isAvailable = MutableStateFlow(available)
        override suspend fun markClaimed(nowEpochMs: Long) {
            isAvailable.value = false
        }
    }

    private class FakeWalletRepository : IWalletRepository {
        override val wallet: StateFlow<CoinWalletState> = MutableStateFlow(CoinWalletState.Empty)
        var lastGrantedOption: CoinUnlockOption? = null

        override suspend fun addCoins(amount: Int) = Unit

        override suspend fun unlockFeature(
            feature: PremiumFeature,
            option: CoinUnlockOption,
            nowEpochMs: Long,
        ): WalletUnlockResult = error("unused")

        override suspend fun grantTimedAccess(
            feature: PremiumFeature,
            option: CoinUnlockOption,
            nowEpochMs: Long,
        ): WalletUnlockResult {
            lastGrantedOption = option
            return WalletUnlockResult.Success
        }

        override fun clearSessionGrant(feature: PremiumFeature) = Unit

        override suspend fun pruneExpiredGrants(nowEpochMs: Long) = Unit
    }
}
