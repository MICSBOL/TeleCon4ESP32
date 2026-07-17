package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.gh.GhPacketEncoder
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.canUseConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationTransportTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GreenhouseViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val saveApplicationProtocolMode: SaveApplicationProtocolModeUseCase,
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    private val saveApplicationTransportType: SaveApplicationTransportTypeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.GREENHOUSE.protocolPrefix()

    private val storedProtocolMode: StateFlow<BluetoothProtocolMode> =
        getApplicationProtocolMode(ApplicationId.GREENHOUSE)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.GREENHOUSE),
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
        access.effectiveProtocolMode(ApplicationId.GREENHOUSE, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.GREENHOUSE),
    )

    fun onConnectionModeChanged(mode: BluetoothConnectionMode) {
        if (!entitlement.value.canUseConnectionMode(ApplicationId.GREENHOUSE, mode)) return
        viewModelScope.launch {
            saveApplicationTransportType(ApplicationId.GREENHOUSE, mode.transport)
            saveApplicationProtocolMode(ApplicationId.GREENHOUSE, mode.protocolMode)
        }
    }

    val transportType: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(ApplicationId.GREENHOUSE)
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
        access.effectiveConnectionMode(ApplicationId.GREENHOUSE, transport, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothConnectionMode.from(
            BluetoothTransportType.CLASSIC,
            BluetoothProtocolMode.defaultFor(ApplicationId.GREENHOUSE),
        ),
    )

    private val _uiState = MutableStateFlow(GreenhouseUiState())
    val uiState = _uiState.asStateFlow()

    init {
        remoteController.isConnected
            .onEach { connected ->
                _uiState.update { current ->
                    current.copy(isOnline = connected)
                }
            }
            .launchIn(viewModelScope)

        remoteController.messages
            .onEach { message ->
                if (message.app == appPrefix && message.type == "DATA") {
                    applyTelemetry(message.values)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                refreshRelativeTimestamps()
            }
        }
    }

    fun toggleFan() {
        val fanOn = !_uiState.value.fanOn
        _uiState.update { it.copy(fanOn = fanOn) }
        sendSet(mapOf("fan" to if (fanOn) 1 else 0))
    }

    fun toggleHeater() {
        val heaterOn = !_uiState.value.heaterOn
        _uiState.update { it.copy(heaterOn = heaterOn) }
        sendSet(mapOf("heater" to if (heaterOn) 1 else 0))
    }

    fun togglePump() {
        val pumpOn = !_uiState.value.pumpOn
        _uiState.update { it.copy(pumpOn = pumpOn) }
        sendSet(mapOf("pump" to if (pumpOn) 1 else 0))
    }

    fun toggleLights() {
        val lightsOn = !_uiState.value.lightsOn
        _uiState.update { it.copy(lightsOn = lightsOn) }
        sendSet(mapOf("lights" to if (lightsOn) 1 else 0))
    }

    fun toggleAutoMode() {
        val autoOn = !_uiState.value.isAutoMode
        _uiState.update { it.copy(isAutoMode = autoOn) }
        sendSet(mapOf("auto" to if (autoOn) 1 else 0))
    }

    fun setVentOpenPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _uiState.update { it.copy(ventOpenPercent = clamped) }
        sendSet(mapOf("vent" to clamped))
    }

    fun commitVentOpen() {
        sendSet(mapOf("vent" to _uiState.value.ventOpenPercent))
    }

    fun setTargetTempC(tempC: Int) {
        val clamped = tempC.coerceIn(10, 40)
        _uiState.update { it.copy(targetTempC = clamped) }
        sendSet(mapOf("target_temp" to clamped))
    }

    fun setTargetHumidityPercent(humidityPercent: Int) {
        val clamped = humidityPercent.coerceIn(30, 95)
        _uiState.update { it.copy(targetHumidityPercent = clamped) }
        sendSet(mapOf("target_hum" to clamped))
    }

    fun updateTargetTempLocal(tempC: Int) {
        _uiState.update { it.copy(targetTempC = tempC.coerceIn(10, 40)) }
    }

    fun updateTargetHumidityLocal(humidityPercent: Int) {
        _uiState.update { it.copy(targetHumidityPercent = humidityPercent.coerceIn(30, 95)) }
    }

    fun updateVentOpenLocal(percent: Int) {
        _uiState.update { it.copy(ventOpenPercent = percent.coerceIn(0, 100)) }
    }

    fun commitTargetClimate() {
        val state = _uiState.value
        sendSet(
            mapOf(
                "target_temp" to state.targetTempC,
                "target_hum" to state.targetHumidityPercent,
            ),
        )
    }

    private fun applyTelemetry(values: Map<String, String>) {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            val temperatureC = values["temp"]?.toFloatOrNull() ?: current.temperatureC
            val tempOut = values["temp_out"]?.toFloatOrNull()
            val deltaTempC = values["delta"]?.toFloatOrNull()
                ?: tempOut?.let { temperatureC - it }
                ?: current.deltaTempC
            val chartData = parseChartHistory(values, current.chartData)
            val lastIrrMinutes = values["last_irr"]?.toIntOrNull()
            val hasCamera = values["cam"]?.toBooleanLike() ?: current.hasCamera

            current.copy(
                isOnline = true,
                lastTelemetryAtMs = now,
                updatedAgo = formatDurationAgo(now, now),
                deviceId = values["device"] ?: current.deviceId,
                temperatureC = temperatureC,
                humidityPercent = values["hum"]?.toIntOrNull() ?: current.humidityPercent,
                vpdKpa = values["vpd"]?.toFloatOrNull() ?: current.vpdKpa,
                isStable = values["stable"]?.toBooleanLike() ?: current.isStable,
                soilPercent = values["soil"]?.toIntOrNull() ?: current.soilPercent,
                soilTargetMin = values["soil_min"]?.toIntOrNull() ?: current.soilTargetMin,
                soilTargetMax = values["soil_max"]?.toIntOrNull() ?: current.soilTargetMax,
                lightLux = values["light"]?.toFloatOrNull() ?: current.lightLux,
                lightsOn = values["lights"]?.toBooleanLike() ?: current.lightsOn,
                deltaTempC = deltaTempC,
                ventOpenPercent = values["vent"]?.toIntOrNull() ?: current.ventOpenPercent,
                tankPercent = values["tank"]?.toIntOrNull() ?: current.tankPercent,
                lastIrrigationAgo = lastIrrMinutes?.let(::formatDurationFromMinutes)
                    ?: current.lastIrrigationAgo,
                chartData = chartData,
                fanOn = values["fan"]?.toBooleanLike() ?: current.fanOn,
                heaterOn = values["heater"]?.toBooleanLike() ?: current.heaterOn,
                pumpOn = values["pump"]?.toBooleanLike() ?: current.pumpOn,
                isAutoMode = values["auto"]?.toBooleanLike() ?: current.isAutoMode,
                targetTempC = values["target_temp"]?.toIntOrNull() ?: current.targetTempC,
                targetHumidityPercent = values["target_hum"]?.toIntOrNull()
                    ?: current.targetHumidityPercent,
                hasCamera = hasCamera,
            )
        }
    }

    private fun refreshRelativeTimestamps() {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            current.copy(
                updatedAgo = formatDurationAgo(now, current.lastTelemetryAtMs),
            )
        }
    }

    private fun parseChartHistory(
        values: Map<String, String>,
        current: EnvironmentalChartData,
    ): EnvironmentalChartData {
        val tempSeries = values["hist_temp"]?.parseFloatSeries()
        val humiditySeries = values["hist_hum"]?.parseFloatSeries()
        val vpdSeries = values["hist_vpd"]?.parseFloatSeries()
        if (tempSeries == null && humiditySeries == null && vpdSeries == null) {
            return current
        }
        return current.copy(
            tempSeries = tempSeries ?: current.tempSeries,
            humiditySeries = humiditySeries ?: current.humiditySeries,
            vpdSeries = vpdSeries ?: current.vpdSeries,
        )
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE -> {
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
                }
                BluetoothProtocolMode.ADVANCED -> {
                    remoteController.sendData(GhPacketEncoder.buildSetPacket(pairs))
                }
            }
        }
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}

private fun String.parseFloatSeries(): List<Float>? =
    split("|")
        .mapNotNull { it.trim().toFloatOrNull() }
        .takeIf { it.isNotEmpty() }
