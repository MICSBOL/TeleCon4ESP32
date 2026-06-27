package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GreenhouseViewModel @Inject constructor(
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.GREENHOUSE.protocolPrefix()

    private val protocolMode = getApplicationProtocolMode(ApplicationId.GREENHOUSE)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.GREENHOUSE),
        )

    private val _uiState = MutableStateFlow(GreenhouseUiState())
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

    private fun applyTelemetry(values: Map<String, String>) {
        _uiState.update { current ->
            current.copy(
                isOnline = true,
                temperatureC = values["temp"]?.toFloatOrNull() ?: current.temperatureC,
                humidityPercent = values["hum"]?.toIntOrNull() ?: current.humidityPercent,
                vpdKpa = values["vpd"]?.toFloatOrNull() ?: current.vpdKpa,
                soilPercent = values["soil"]?.toIntOrNull() ?: current.soilPercent,
                lightLux = values["light"]?.toFloatOrNull() ?: current.lightLux,
                fanOn = values["fan"]?.toBooleanLike() ?: current.fanOn,
                heaterOn = values["heater"]?.toBooleanLike() ?: current.heaterOn,
                pumpOn = values["pump"]?.toBooleanLike() ?: current.pumpOn,
            )
        }
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            if (protocolMode.value != BluetoothProtocolMode.SIMPLE) return@launch
            remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
        }
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}
