package com.micsbol.telecon4esp32.ui.smartdoorlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.dl.DlPacketEncoder
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraLinkSession
import com.micsbol.telecon4esp32.domain.camera.autoConnectSoftApControlWhenCameraOnline
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class SmartDoorLockViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
    private val remoteController: RemoteController,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    getApplicationBoard: GetApplicationBoardUseCase,
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.SMART_DOOR_LOCK.protocolPrefix()
    private val cameraSession = Esp32CameraLinkSession(repository = cameraStreamRepository)

    private val storedProtocolMode = getApplicationProtocolMode(ApplicationId.SMART_DOOR_LOCK)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_DOOR_LOCK),
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
        access.effectiveProtocolMode(ApplicationId.SMART_DOOR_LOCK, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.SMART_DOOR_LOCK),
    )

    private val board: StateFlow<Esp32Board> = getApplicationBoard(ApplicationId.SMART_DOOR_LOCK)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(ApplicationId.SMART_DOOR_LOCK),
        )

    private val transport: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(ApplicationId.SMART_DOOR_LOCK)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothTransportType.CLASSIC,
            )

    val cameraLinkProfile: StateFlow<CameraLinkProfile> = combine(board, transport) { selectedBoard, selectedTransport ->
        resolveCameraLinkProfile(ApplicationId.SMART_DOOR_LOCK, selectedBoard, selectedTransport)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraLinkProfile.CONTROL_ONLY,
    )

    private val _uiState = MutableStateFlow(SmartDoorLockUiState())
    val uiState: StateFlow<SmartDoorLockUiState> = _uiState.asStateFlow()

    private var callTimerJob: Job? = null
    private var relayPulseJob: Job? = null

    init {
        viewModelScope.launch {
            cameraLinkProfile.collect { profile ->
                cameraSession.setProfile(profile)
                _uiState.update { it.copy(cameraLinkProfile = profile) }
            }
        }

        viewModelScope.launch {
            combine(
                cameraStreamRepository.streamState,
                cameraLinkProfile,
                remoteController.isConnected,
            ) { cameraState, profile, connected ->
                Triple(cameraState, profile, connected)
            }.collect { (cameraState, profile, connected) ->
                val streaming = profile.shouldStartCameraStream && _uiState.value.isCameraEnabled
                _uiState.update { current ->
                    current.copy(
                        cameraLinkProfile = profile,
                        cameraState = if (streaming) cameraState else CameraStreamState.Idle,
                        isCameraOnline = streaming && cameraState is CameraStreamState.Frame,
                        isEsp32Online = (streaming && (
                            cameraState is CameraStreamState.Frame ||
                                cameraState is CameraStreamState.Connecting
                            )) || connected,
                    )
                }
            }
        }

        remoteController.messages
            .onEach { message ->
                if (message.app == appPrefix && message.type == "DATA") {
                    applyTelemetry(message.values)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onScreenVisible() {
        _uiState.update { it.copy(isCallActive = true) }
        startCallTimer()
        cameraSession.setCameraEnabled(_uiState.value.isCameraEnabled)
        cameraSession.onVisible()
    }

    fun onScreenHidden() {
        stopCallTimer()
        relayPulseJob?.cancel()
        cameraSession.onHidden()
        sendSet(mapOf("call" to 0))
        _uiState.update {
            it.copy(
                isCallActive = false,
                callDurationSeconds = 0,
                isRelayPulseActive = false,
                cameraState = CameraStreamState.Idle,
                isCameraOnline = false,
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
        sendSet(mapOf("unlock" to 1))
    }

    fun onSendLockSignal() {
        _uiState.update {
            it.copy(
                doorLockState = DoorLockState.LOCKED,
                relayPinState = RelayPinState.HIGH,
                lastSignalMessage = "lock_sent",
            )
        }
        sendSet(mapOf("lock" to 1))
    }

    fun onTriggerRelayPulse() {
        if (_uiState.value.isRelayPulseActive) return

        relayPulseJob?.cancel()
        sendSet(mapOf("pulse" to 1))
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
        val enabled = !_uiState.value.isMicEnabled
        _uiState.update { it.copy(isMicEnabled = enabled) }
        sendSet(mapOf("mic" to if (enabled) 1 else 0))
    }

    fun onToggleSpeaker() {
        val enabled = !_uiState.value.isSpeakerEnabled
        _uiState.update { it.copy(isSpeakerEnabled = enabled) }
        sendSet(mapOf("spk" to if (enabled) 1 else 0))
    }

    fun onToggleCamera() {
        val enableCamera = !_uiState.value.isCameraEnabled
        _uiState.update { it.copy(isCameraEnabled = enableCamera) }
        sendSet(mapOf("cam" to if (enableCamera) 1 else 0))
        cameraSession.setCameraEnabled(enableCamera)
        if (!enableCamera) {
            _uiState.update { it.copy(cameraState = CameraStreamState.Idle, isCameraOnline = false) }
        }
    }

    fun onEndCall() {
        onScreenHidden()
    }

    fun clearLastSignalMessage() {
        _uiState.update { it.copy(lastSignalMessage = null) }
    }

    fun shouldAutoConnectSoftApControl(): Boolean {
        val state = _uiState.value
        return state.cameraLinkProfile.autoConnectSoftApControlWhenCameraOnline &&
            state.isCameraOnline &&
            !remoteController.isConnected.value
    }

    override fun onCleared() {
        cameraSession.stop()
        super.onCleared()
    }

    private fun applyTelemetry(values: Map<String, String>) {
        _uiState.update { current ->
            val camEnabled = values["cam"]?.toBooleanLike() ?: current.isCameraEnabled
            current.copy(
                isEsp32Online = true,
                doorLockState = when (values["lock"]) {
                    "0" -> DoorLockState.UNLOCKED
                    "1" -> DoorLockState.LOCKED
                    else -> current.doorLockState
                },
                relayPinState = when (values["relay"]?.uppercase()) {
                    "LOW" -> RelayPinState.LOW
                    "HIGH" -> RelayPinState.HIGH
                    else -> current.relayPinState
                },
                isMicEnabled = values["mic"]?.toBooleanLike() ?: current.isMicEnabled,
                isSpeakerEnabled = values["spk"]?.toBooleanLike() ?: current.isSpeakerEnabled,
                isCameraEnabled = camEnabled,
            )
        }
        values["cam"]?.toBooleanLike()?.let { cameraSession.setCameraEnabled(it) }
    }

    private fun sendSet(pairs: Map<String, Any>) {
        viewModelScope.launch {
            if (!remoteController.isConnected.value) return@launch
            when (protocolMode.value) {
                BluetoothProtocolMode.SIMPLE ->
                    remoteController.sendLine(SimpleProtocolEncoder.buildSetLine(appPrefix, pairs))
                BluetoothProtocolMode.ADVANCED ->
                    remoteController.sendData(DlPacketEncoder.buildSetPacket(pairs))
            }
        }
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

    companion object {
        private const val RELAY_PULSE_MS = 2_000L
    }
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    else -> false
}
