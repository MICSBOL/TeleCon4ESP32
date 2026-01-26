package com.example.emitterapp.ui.rc_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.data.repository.SettingsRepository
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: Flow<SettingsState> = settingsRepository.settingsFlow
    fun onLeftStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            settingsRepository.saveLeftStickMode(newMode)
        }
    }

    fun onRightStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            settingsRepository.saveRightStickMode(newMode)
        }
    }

    fun onSwitchInitialStateChange(index: Int, inOn: Boolean){
        viewModelScope.launch {
            settingsRepository.saveSwitchState(index, inOn)
        }
    }

    fun onLeftKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            settingsRepository.saveLeftKnobValue(value)
        }
    }

    fun onRightKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            settingsRepository.saveRightKnobValue(value)
        }
    }
}