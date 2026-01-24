package com.example.emitterapp.ui.rc_settings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val settings: SettingsState) : SettingsUiState    data class Error(val message: String) : SettingsUiState
}
