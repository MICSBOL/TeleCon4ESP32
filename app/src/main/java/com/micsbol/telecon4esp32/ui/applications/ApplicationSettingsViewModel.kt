package com.micsbol.telecon4esp32.ui.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.canUseConnectionMode
import com.micsbol.telecon4esp32.domain.model.coerceConnectionModeForBoard
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.isConnectionModeAvailable
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApPerformancePresetUseCase
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
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    getApplicationConnectionMode: GetApplicationConnectionModeUseCase,
    private val saveApplicationConnectionMode: SaveApplicationConnectionModeUseCase,
    getApplicationBoard: GetApplicationBoardUseCase,
    private val saveApplicationBoard: SaveApplicationBoardUseCase,
    getSoftApPerformancePreset: GetSoftApPerformancePresetUseCase,
    private val saveSoftApPerformancePreset: SaveSoftApPerformancePresetUseCase,
    getSoftApHudProcessingRate: GetSoftApHudProcessingRateUseCase,
    private val saveSoftApHudProcessingRate: SaveSoftApHudProcessingRateUseCase,
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

    private val storedConnectionMode: StateFlow<BluetoothConnectionMode?> =
        getApplicationConnectionMode(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
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

    val board: StateFlow<Esp32Board> = getApplicationBoard(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(applicationId),
        )

    val softApPerformancePreset: StateFlow<SoftApPerformancePreset> =
        getSoftApPerformancePreset(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApPerformancePreset.DEFAULT,
            )

    val softApHudProcessingRate: StateFlow<SoftApHudProcessingRate> =
        getSoftApHudProcessingRate(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApHudProcessingRate.DEFAULT,
            )

    val connectionMode: StateFlow<BluetoothConnectionMode> = combine(
        transportType,
        storedProtocolMode,
        storedConnectionMode,
        entitlement,
        board,
    ) { transport, storedProtocol, storedMode, access, selectedBoard ->
        val candidate = storedMode
            ?: access.effectiveConnectionMode(
                applicationId,
                transport,
                storedProtocol,
                selectedBoard,
            )
        access.coerceConnectionModeForBoard(applicationId, selectedBoard, candidate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothConnectionMode.from(
            BluetoothTransportType.CLASSIC,
            BluetoothProtocolMode.defaultFor(applicationId),
        ),
    )

    fun onConnectionModeChanged(mode: BluetoothConnectionMode) {
        if (!entitlement.value.canUseConnectionMode(applicationId, mode)) return
        if (!applicationId.isConnectionModeAvailable(board.value, mode)) return
        viewModelScope.launch {
            saveApplicationConnectionMode(applicationId, mode)
        }
    }

    fun onBoardChanged(board: Esp32Board) {
        viewModelScope.launch {
            saveApplicationBoard(applicationId, board)
            val current = storedConnectionMode.value
                ?: entitlement.value.effectiveConnectionMode(
                    applicationId,
                    transportType.value,
                    storedProtocolMode.value,
                    board,
                )
            val coerced = entitlement.value.coerceConnectionModeForBoard(
                applicationId,
                board,
                current,
            )
            if (coerced != current || storedConnectionMode.value == null) {
                saveApplicationConnectionMode(applicationId, coerced)
            }
        }
    }

    fun onSoftApPerformancePresetChanged(preset: SoftApPerformancePreset) {
        viewModelScope.launch {
            saveSoftApPerformancePreset(applicationId, preset)
        }
    }

    fun onSoftApHudProcessingRateChanged(rate: SoftApHudProcessingRate) {
        viewModelScope.launch {
            saveSoftApHudProcessingRate(applicationId, rate)
        }
    }
}
