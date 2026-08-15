package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.gh.GhPacketEncoder
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
import com.micsbol.telecon4esp32.domain.model.GhCameraGimbal
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
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
class GreenhouseCameraViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
    private val remoteController: RemoteController,
    getApplicationBoard: GetApplicationBoardUseCase,
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
) : ViewModel() {

    private val appPrefix = ApplicationId.GREENHOUSE.protocolPrefix()
    private val runningOnEmulator = GreenhouseEmulatorSupport.isEmulator()
    private val cameraSession = Esp32CameraLinkSession(
        repository = cameraStreamRepository,
        skipOnEmulator = runningOnEmulator,
    )

    private val board: StateFlow<Esp32Board> = getApplicationBoard(ApplicationId.GREENHOUSE)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(ApplicationId.GREENHOUSE),
        )

    private val transport: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(ApplicationId.GREENHOUSE)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothTransportType.CLASSIC,
            )

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

    private val protocolMode: StateFlow<BluetoothProtocolMode> = combine(
        storedProtocolMode,
        entitlement,
    ) { stored, access ->
        access.effectiveProtocolMode(ApplicationId.GREENHOUSE, stored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.GREENHOUSE),
    )

    val cameraLinkProfile: StateFlow<CameraLinkProfile> = combine(board, transport) { selectedBoard, selectedTransport ->
        resolveCameraLinkProfile(ApplicationId.GREENHOUSE, selectedBoard, selectedTransport)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraLinkProfile.CONTROL_ONLY,
    )

    private val _uiState = MutableStateFlow(
        GreenhouseCameraUiState(
            // Emulators cannot reach ESP32-CAM and stream polling can kill low-RAM AVDs.
            isEmulatorPreview = runningOnEmulator,
            camPanPercent = GhCameraGimbal.CENTER,
            camTiltPercent = GhCameraGimbal.CENTER,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cameraLinkProfile.collect { profile ->
                cameraSession.setProfile(profile)
                _uiState.update { it.copy(cameraLinkProfile = profile) }
            }
        }

        if (!runningOnEmulator) {
            combine(
                cameraStreamRepository.streamState,
                cameraLinkProfile,
            ) { cameraState, profile ->
                cameraState to profile
            }.onEach { (cameraState, profile) ->
                val streaming = profile.shouldStartCameraStream
                _uiState.update {
                    it.copy(
                        cameraState = if (streaming) cameraState else CameraStreamState.Idle,
                        isCameraOnline = streaming && cameraState is CameraStreamState.Frame,
                    )
                }
            }.launchIn(viewModelScope)
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
        cameraSession.onVisible()
    }

    fun onScreenHidden() {
        cameraSession.onHidden()
        _uiState.update {
            it.copy(cameraState = CameraStreamState.Idle, isCameraOnline = false)
        }
    }

    /** SoftAP-only profile: auto-open TCP once SoftAP HTTP video proves reachability. */
    fun shouldAutoConnectSoftApControl(): Boolean {
        val state = _uiState.value
        return state.cameraLinkProfile.autoConnectSoftApControlWhenCameraOnline &&
            state.isCameraOnline &&
            !state.isBluetoothOnline
    }

    /** Live pad drag: update UI and send only when the integer angles change. */
    fun setCamGimbal(panPercent: Int, tiltPercent: Int) {
        val pan = GhCameraGimbal.clamp(panPercent)
        val tilt = GhCameraGimbal.clamp(tiltPercent)
        val current = _uiState.value
        if (current.camPanPercent == pan && current.camTiltPercent == tilt) return
        _uiState.update { it.copy(camPanPercent = pan, camTiltPercent = tilt) }
        sendSet(mapOf("cam_pan" to pan, "cam_tilt" to tilt))
    }

    fun commitCamGimbal() {
        val state = _uiState.value
        sendSet(
            mapOf(
                "cam_pan" to state.camPanPercent,
                "cam_tilt" to state.camTiltPercent,
            ),
        )
    }

    fun centerCamGimbal() {
        val pan = GhCameraGimbal.CENTER
        val tilt = GhCameraGimbal.CENTER
        _uiState.update { it.copy(camPanPercent = pan, camTiltPercent = tilt) }
        sendSet(mapOf("cam_pan" to pan, "cam_tilt" to tilt))
    }

    override fun onCleared() {
        cameraSession.stop()
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
                camPanPercent = values["cam_pan"]?.toIntOrNull()?.let(GhCameraGimbal::clamp)
                    ?: current.camPanPercent,
                camTiltPercent = values["cam_tilt"]?.toIntOrNull()?.let(GhCameraGimbal::clamp)
                    ?: current.camTiltPercent,
            )
        }
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
