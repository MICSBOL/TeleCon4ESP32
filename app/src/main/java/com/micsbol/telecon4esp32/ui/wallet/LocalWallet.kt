package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.runtime.compositionLocalOf
import com.micsbol.telecon4esp32.domain.model.CoinWalletState

val LocalWallet = compositionLocalOf { CoinWalletState.Empty }
