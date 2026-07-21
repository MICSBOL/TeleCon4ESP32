package com.micsbol.telecon4esp32.ui.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.canUseConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationTransportTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val saveApplicationProtocolMode: SaveApplicationProtocolModeUseCase,
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    private val saveApplicationTransportType: SaveApplicationTransportTypeUseCase,
    getApplicationBoard: GetApplicationBoardUseCase,
    private val saveApplicationBoard: SaveApplicationBoardUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    val applicationId: ApplicationId = savedStateHandle.get<String>("applicationId")
        ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
        ?: ApplicationId.CONTROL_PANEL

    private val storedProtocolMode: StateFlow<BluetoothProtocolMode> =
        getApplicationProtocolMode(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothProtocolMode.defaultFor(applicationId),
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
        access.effectiveProtocolMode(applicationId, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(applicationId),
    )

    val transportType: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothTransportType.CLASSIC,
            )

    val connectionMode: StateFlow<BluetoothConnectionMode> = combine(
        transportType,
        storedProtocolMode,
        entitlement,
    ) { transport, stored, access ->
        access.effectiveConnectionMode(applicationId, transport, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothConnectionMode.from(
            BluetoothTransportType.CLASSIC,
            BluetoothProtocolMode.defaultFor(applicationId),
        ),
    )

    val board: StateFlow<Esp32Board> = getApplicationBoard(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(applicationId),
        )

    fun onConnectionModeChanged(mode: BluetoothConnectionMode) {
        if (!entitlement.value.canUseConnectionMode(applicationId, mode)) return
        viewModelScope.launch {
            saveApplicationTransportType(applicationId, mode.transport)
            saveApplicationProtocolMode(applicationId, mode.protocolMode)
        }
    }

    fun onBoardChanged(board: Esp32Board) {
        viewModelScope.launch {
            saveApplicationBoard(applicationId, board)
        }
    }
}
