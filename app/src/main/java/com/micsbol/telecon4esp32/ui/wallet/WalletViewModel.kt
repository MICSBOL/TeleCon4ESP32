package com.micsbol.telecon4esp32.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.repository.IExplorerGiftRepository
import com.micsbol.telecon4esp32.domain.use_case.ClaimExplorerGiftUseCase
import com.micsbol.telecon4esp32.domain.use_case.ClearSessionGrantUseCase
import com.micsbol.telecon4esp32.domain.use_case.GrantCoinsFromRewardedAdUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveWalletUseCase
import com.micsbol.telecon4esp32.domain.use_case.PruneExpiredGrantsUseCase
import com.micsbol.telecon4esp32.domain.use_case.UnlockFeatureWithCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    observeWallet: ObserveWalletUseCase,
    explorerGiftRepository: IExplorerGiftRepository,
    private val unlockFeatureWithCoins: UnlockFeatureWithCoinsUseCase,
    private val claimExplorerGiftUseCase: ClaimExplorerGiftUseCase,
    private val grantCoinsFromRewardedAd: GrantCoinsFromRewardedAdUseCase,
    private val clearSessionGrant: ClearSessionGrantUseCase,
    private val pruneExpiredGrants: PruneExpiredGrantsUseCase,
) : ViewModel() {

    val wallet: StateFlow<CoinWalletState> = observeWallet()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CoinWalletState.Empty,
        )

    val explorerGiftAvailable: StateFlow<Boolean> = explorerGiftRepository.isAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = explorerGiftRepository.isAvailable.value,
        )

    init {
        viewModelScope.launch {
            pruneExpiredGrants()
        }
    }

    fun unlockFeature(
        feature: PremiumFeature,
        option: CoinUnlockOption,
        onResult: (WalletUnlockResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = unlockFeatureWithCoins(feature, option)
            onResult(result)
        }
    }

    fun claimExplorerGift(
        feature: PremiumFeature,
        onResult: (WalletUnlockResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(claimExplorerGiftUseCase(feature))
        }
    }

    fun grantRewardedAdCoins() {
        viewModelScope.launch {
            grantCoinsFromRewardedAd()
        }
    }

    fun onProAppFeatureChanged(previousFeature: PremiumFeature?) {
        if (previousFeature == null) return
        // Synchronous so Home/Catalog recompose without a lingering one-use grant.
        clearSessionGrant(previousFeature)
    }
}
