package com.micsbol.telecon4esp32.ui.rc_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSettingsUseCases
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getUserSettings: GetUserSettingsUseCase,
    private val saveSettings: SaveSettingsUseCases
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        getUserSettings()
            .map<UserSettings, SettingsUiState> { settings ->
                SettingsUiState.Success(settings)
            }
            .catch { throwable ->
                emit(SettingsUiState.Error(throwable.message ?: "An unexpected error occurred"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsUiState.Loading
            )
    fun onLeftStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            saveSettings.saveLeftStickMode(newMode)
        }
    }

    fun onRightStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            saveSettings.saveRightStickMode(newMode)
        }
    }

    fun onSwitchInitialStateChange(index: Int, isOn: Boolean){
        viewModelScope.launch {
            saveSettings.saveSwitchState(index, isOn)
        }
    }

    fun onLeftKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            saveSettings.saveLeftKnobValue(value)
        }
    }

    fun onRightKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            saveSettings.saveRightKnobValue(value)
        }
    }
}