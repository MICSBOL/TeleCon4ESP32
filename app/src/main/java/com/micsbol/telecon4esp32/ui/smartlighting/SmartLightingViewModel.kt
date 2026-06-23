package com.micsbol.telecon4esp32.ui.smartlighting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SmartLightingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SmartLightingUiState())
    val uiState = _uiState.asStateFlow()

    fun onSettingToggle(settingId: String, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                settings = state.settings.map { setting ->
                    if (setting.id == settingId) setting.copy(isEnabled = enabled) else setting
                },
            )
        }
    }

    fun onDeviceToggle(deviceId: String) {
        _uiState.update { state ->
            state.copy(
                connectedDevices = state.connectedDevices.map { device ->
                    if (device.id == deviceId) device.copy(isOn = !device.isOn) else device
                },
            )
        }
    }

    fun onToggleAllDevices() {
        _uiState.update { state ->
            val turnOn = !state.allDevicesOn
            state.copy(
                connectedDevices = state.connectedDevices.map { device ->
                    device.copy(isOn = turnOn)
                },
            )
        }
    }
}
