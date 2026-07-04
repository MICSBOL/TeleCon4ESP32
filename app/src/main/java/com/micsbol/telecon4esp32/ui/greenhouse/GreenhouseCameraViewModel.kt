package com.micsbol.telecon4esp32.ui.greenhouse

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class GreenhouseCameraViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
    remoteController: RemoteController,
) : ViewModel() {

    private val appPrefix = ApplicationId.GREENHOUSE.protocolPrefix()
    private val cameraBaseUrl = Esp32CameraDefaults.DEFAULT_BASE_URL
    private val runningOnEmulator = isEmulatorDevice()

    private val _uiState = MutableStateFlow(
        GreenhouseCameraUiState(
            // Emulators cannot reach ESP32-CAM and stream polling can kill low-RAM AVDs.
            isEmulatorPreview = runningOnEmulator,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (!runningOnEmulator) {
            cameraStreamRepository.streamState
                .onEach { cameraState ->
                    _uiState.update { it.copy(cameraState = cameraState) }
                }
                .launchIn(viewModelScope)
        }

        remoteController.isConnected
            .onEach { connected ->
                _uiState.update { it.copy(isBluetoothOnline = connected) }
            }
            .launchIn(viewModelScope)

        remoteController.messages
            .onEach { message ->
                if (message.app == appPrefix && message.type == "DATA") {
                    applyTelemetry(message.values)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onScreenVisible() {
        if (runningOnEmulator) return
        cameraStreamRepository.startStream(cameraBaseUrl)
    }

    fun onScreenHidden() {
        if (runningOnEmulator) return
        cameraStreamRepository.stopStream()
        _uiState.update { it.copy(cameraState = CameraStreamState.Idle) }
    }

    override fun onCleared() {
        if (!runningOnEmulator) {
            cameraStreamRepository.stopStream()
        }
        super.onCleared()
    }

    private fun applyTelemetry(values: Map<String, String>) {
        _uiState.update { current ->
            current.copy(
                isBluetoothOnline = true,
                temperatureC = values["temp"]?.toFloatOrNull() ?: current.temperatureC,
                humidityPercent = values["hum"]?.toIntOrNull() ?: current.humidityPercent,
                vpdKpa = values["vpd"]?.toFloatOrNull() ?: current.vpdKpa,
                soilPercent = values["soil"]?.toIntOrNull() ?: current.soilPercent,
                tankPercent = values["tank"]?.toIntOrNull() ?: current.tankPercent,
                isAutoMode = values["auto"]?.toBooleanLike() ?: current.isAutoMode,
                isStable = values["stable"]?.toBooleanLike() ?: current.isStable,
                fanOn = values["fan"]?.toBooleanLike() ?: current.fanOn,
                lightsOn = values["lights"]?.toBooleanLike() ?: current.lightsOn,
            )
        }
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}

private fun isEmulatorDevice(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.PRODUCT.contains("sdk_gphone") ||
        Build.PRODUCT.contains("sdk") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu")
}
