package com.micsbol.telecon4esp32.ui.smartdoorlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartDoorLockViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartDoorLockUiState())
    val uiState: StateFlow<SmartDoorLockUiState> = _uiState.asStateFlow()

    private val cameraBaseUrl = Esp32CameraDefaults.DEFAULT_BASE_URL
    private var callTimerJob: Job? = null
    private var relayPulseJob: Job? = null

    init {
        viewModelScope.launch {
            cameraStreamRepository.streamState.collect { cameraState ->
                _uiState.update { current ->
                    current.copy(
                        cameraState = if (current.isCameraEnabled) {
                            cameraState
                        } else {
                            CameraStreamState.Idle
                        },
                        isEsp32Online = cameraState is CameraStreamState.Frame ||
                            cameraState is CameraStreamState.Connecting,
                    )
                }
            }
        }
    }

    fun onScreenVisible() {
        _uiState.update { it.copy(isCallActive = true) }
        startCallTimer()
        if (_uiState.value.isCameraEnabled) {
            cameraStreamRepository.startStream(cameraBaseUrl)
        }
    }

    fun onScreenHidden() {
        stopCallTimer()
        relayPulseJob?.cancel()
        cameraStreamRepository.stopStream()
        _uiState.update {
            it.copy(
                isCallActive = false,
                callDurationSeconds = 0,
                isRelayPulseActive = false,
            )
        }
    }

    fun onSendUnlockSignal() {
        _uiState.update {
            it.copy(
                doorLockState = DoorLockState.UNLOCKED,
                relayPinState = RelayPinState.LOW,
                lastSignalMessage = "unlock_sent",
            )
        }
    }

    fun onSendLockSignal() {
        _uiState.update {
            it.copy(
                doorLockState = DoorLockState.LOCKED,
                relayPinState = RelayPinState.HIGH,
                lastSignalMessage = "lock_sent",
            )
        }
    }

    fun onTriggerRelayPulse() {
        if (_uiState.value.isRelayPulseActive) return

        relayPulseJob?.cancel()
        relayPulseJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRelayPulseActive = true,
                    relayPinState = RelayPinState.LOW,
                    doorLockState = DoorLockState.UNLOCKED,
                    lastSignalMessage = "relay_pulse",
                )
            }
            delay(RELAY_PULSE_MS)
            if (isActive) {
                _uiState.update {
                    it.copy(
                        isRelayPulseActive = false,
                        relayPinState = RelayPinState.HIGH,
                        doorLockState = DoorLockState.LOCKED,
                    )
                }
            }
        }
    }

    fun onToggleMic() {
        _uiState.update { it.copy(isMicEnabled = !it.isMicEnabled) }
    }

    fun onToggleSpeaker() {
        _uiState.update { it.copy(isSpeakerEnabled = !it.isSpeakerEnabled) }
    }

    fun onToggleCamera() {
        val enableCamera = !_uiState.value.isCameraEnabled
        _uiState.update { it.copy(isCameraEnabled = enableCamera) }
        if (enableCamera && _uiState.value.isCallActive) {
            cameraStreamRepository.startStream(cameraBaseUrl)
        } else {
            cameraStreamRepository.stopStream()
            _uiState.update { it.copy(cameraState = CameraStreamState.Idle) }
        }
    }

    fun onEndCall() {
        onScreenHidden()
    }

    fun clearLastSignalMessage() {
        _uiState.update { it.copy(lastSignalMessage = null) }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private companion object {
        const val RELAY_PULSE_MS = 2_000L
    }
}
