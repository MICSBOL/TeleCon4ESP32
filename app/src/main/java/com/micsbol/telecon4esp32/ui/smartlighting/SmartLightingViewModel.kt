package com.micsbol.telecon4esp32.ui.smartlighting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.lt.LtPacketEncoder
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartLightingViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.SMART_LIGHTING.protocolPrefix()

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.SMART_LIGHTING)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_LIGHTING),
        )

    private val entitlement: StateFlow<Entitlement> = observeEntitlement()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Entitlement.Free,
        )

    val protocolMode: StateFlow<BluetoothProtocolMode> = combine(
        storedProtocolMode,
        entitlement,
    ) { stored, access ->
        access.effectiveProtocolMode(ApplicationId.SMART_LIGHTING, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_LIGHTING),
    )

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
        sendSet(mapOf(settingId to if (enabled) 1 else 0))
    }

    fun onDeviceToggle(deviceId: String) {
        val index = _uiState.value.connectedDevices.indexOfFirst { it.id == deviceId }
        val device = _uiState.value.connectedDevices.getOrNull(index) ?: return
        val nextOn = !device.isOn
        _uiState.update { state ->
            state.copy(
                connectedDevices = state.connectedDevices.map { candidate ->
                    if (candidate.id == deviceId) candidate.copy(isOn = nextOn) else candidate
                },
            )
        }
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(
                        SimpleProtocolEncoder.buildSetLine(
                            appPrefix,
                            mapOf("zone" to deviceId, "state" to if (nextOn) 1 else 0),
                        ),
                    )
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(
                        LtPacketEncoder.buildSetPacket(
                            mapOf(
                                "zone_id" to index.coerceAtLeast(0),
                                "state" to if (nextOn) 1 else 0,
                            ),
                        ),
                    )
            }
        }
    }

    fun onToggleAllDevices() {
        val turnOn = !_uiState.value.allDevicesOn
        _uiState.update { state ->
            state.copy(
                connectedDevices = state.connectedDevices.map { device ->
                    device.copy(isOn = turnOn)
                },
            )
        }
        sendSet(mapOf("all" to if (turnOn) 1 else 0))
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(LtPacketEncoder.buildSetPacket(pairs))
            }
        }
    }
}
