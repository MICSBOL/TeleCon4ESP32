package com.micsbol.telecon4esp32.ui.smarthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.sh.ShPacketEncoder
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
class SmartHomeViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.SMART_HOME.protocolPrefix()

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.SMART_HOME)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_HOME),
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
        access.effectiveProtocolMode(ApplicationId.SMART_HOME, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_HOME),
    )

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleRoomLight(roomId: String) {
        val roomIndex = _uiState.value.rooms.indexOfFirst { it.id == roomId }
        val room = _uiState.value.rooms.getOrNull(roomIndex) ?: return
        val light = room.devices.firstOrNull { it.isLight } ?: return
        val nextOn = !light.isOn

        _uiState.update { state ->
            state.copy(
                rooms = state.rooms.map { candidate ->
                    if (candidate.id == roomId) candidate.withToggledLight() else candidate
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
                            mapOf(
                                "room" to roomId,
                                "device" to light.id,
                                "state" to if (nextOn) 1 else 0,
                            ),
                        ),
                    )
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(
                        ShPacketEncoder.buildSetPacket(
                            mapOf(
                                "room_id" to roomIndex.coerceAtLeast(0),
                                "device_id" to 1,
                                "state" to if (nextOn) 1 else 0,
                            ),
                        ),
                    )
            }
        }
    }
}
