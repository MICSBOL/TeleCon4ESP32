package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.ui.bluetooth.RcControlState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class RcVehicleProViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
    private val remoteController: RemoteController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RcVehicleProUiState())
    val uiState: StateFlow<RcVehicleProUiState> = _uiState.asStateFlow()

    private val cameraBaseUrl = Esp32CameraDefaults.DEFAULT_BASE_URL

    init {
        viewModelScope.launch {
            combine(
                cameraStreamRepository.streamState,
                remoteController.isConnected,
                remoteController.telemetryState,
            ) { cameraState, connected, telemetry ->
                Triple(cameraState, connected, telemetry)
            }.collect { (cameraState, connected, telemetry) ->
                _uiState.update { current ->
                    current.copy(
                        cameraState = cameraState,
                        isBluetoothConnected = connected,
                        isCameraOnline = cameraState is CameraStreamState.Frame,
                        batteryPercent = telemetry.indicatorState.batteryLevel.coerceIn(0, 100),
                        motorTempCelsius = mapAnalogToMotorTemp(telemetry.indicatorState.analogValue),
                    )
                }
            }
        }
    }

    fun onScreenVisible() {
        cameraStreamRepository.startStream(cameraBaseUrl)
    }

    fun onScreenHidden() {
        cameraStreamRepository.stopStream()
    }

    fun updateDriveMetrics(rcControlState: RcControlState) {
        val throttle = rcControlState.leftStickPosition.second
        val speed = abs(throttle) * MAX_SPEED_KMH
        _uiState.update { it.copy(speedKmh = speed) }
    }

    fun onEmergencyStop() {
        _uiState.update { it.copy(speedKmh = 0f) }
    }

    fun onToggleRecording() {
        _uiState.update { it.copy(isRecording = !it.isRecording) }
    }

    fun onToggleLights() {
        _uiState.update { it.copy(lightsOn = !it.lightsOn) }
    }

    fun onCapturePhoto() {
        // Snapshot is already polled from /capture; hook for future save-to-gallery action.
    }

    private fun mapAnalogToMotorTemp(analogValue: Int): Int {
        return (35 + (analogValue.coerceIn(0, 100) * 0.25f)).roundToInt()
    }

    private companion object {
        const val MAX_SPEED_KMH = 40f
    }
}
