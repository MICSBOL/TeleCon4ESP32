package com.micsbol.telecon4esp32.ui.rc_settings

import com.micsbol.telecon4esp32.domain.model.UserSettings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val settings: UserSettings) : SettingsUiState    data class Error(val message: String) : SettingsUiState
}
