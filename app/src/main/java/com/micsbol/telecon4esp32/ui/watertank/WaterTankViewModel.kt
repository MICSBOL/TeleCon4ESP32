package com.micsbol.telecon4esp32.ui.watertank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.wt.WtPacketEncoder
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterTankViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.WATER_TANK.protocolPrefix()

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.WATER_TANK)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.WATER_TANK),
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
        access.effectiveProtocolMode(ApplicationId.WATER_TANK, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.WATER_TANK),
    )

    private val _uiState = MutableStateFlow(WaterTankUiState())
    val uiState = _uiState.asStateFlow()

    init {
        remoteController.messages
            .onEach { message ->
                if (message.app == appPrefix && message.type == "DATA") {
                    applyTelemetry(message.values)
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectChartPeriod(period: TankChartPeriod) {
        _uiState.update {
            it.copy(
                chartPeriod = period,
                chartData = TankLevelChartData.mock(period),
            )
        }
    }

    fun togglePump() {
        val pumpOn = !_uiState.value.pumpOn
        _uiState.update { it.copy(pumpOn = pumpOn) }
        sendSet(mapOf("pump" to if (pumpOn) 1 else 0))
    }

    private fun applyTelemetry(values: Map<String, String>) {
        _uiState.update { current ->
            current.copy(
                isOnline = true,
                isConnected = true,
                levelPercent = values["level"]?.toIntOrNull() ?: current.levelPercent,
                capacityLiters = values["cap"]?.toIntOrNull() ?: current.capacityLiters,
                pumpOn = values["pump"]?.toBooleanLike() ?: current.pumpOn,
                tankStatus = values["status"]?.toIntOrNull()?.toTankStatus() ?: current.tankStatus,
            )
        }
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(WtPacketEncoder.buildSetPacket(pairs))
            }
        }
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}

private fun Int.toTankStatus(): TankStatus = when (this) {
    1 -> TankStatus.LOW
    2 -> TankStatus.CRITICAL
    else -> TankStatus.NORMAL
}
