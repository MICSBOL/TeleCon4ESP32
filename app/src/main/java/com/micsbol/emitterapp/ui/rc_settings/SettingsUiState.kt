package com.micsbol.emitterapp.ui.rc_settings

import com.micsbol.emitterapp.domain.model.UserSettings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val settings: UserSettings) : SettingsUiState    data class Error(val message: String) : SettingsUiState
}
