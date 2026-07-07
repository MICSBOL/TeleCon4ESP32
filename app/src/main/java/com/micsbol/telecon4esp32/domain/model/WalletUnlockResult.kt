package com.micsbol.telecon4esp32.domain.model

sealed interface WalletUnlockResult {
    data object Success : WalletUnlockResult
    data object AlreadyUnlocked : WalletUnlockResult
    data object InsufficientBalance : WalletUnlockResult
}
