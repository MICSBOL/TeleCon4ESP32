package com.micsbol.telecon4esp32.ui.control_panel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.data.camera.SoftApNetworkResolver
import com.micsbol.telecon4esp32.data.wifi.SoftApWifiSession
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.CameraVideoSoftApTarget
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraLinkSession
import com.micsbol.telecon4esp32.domain.camera.HudPreviewOptions
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.camera.cameraVideoSoftApConflictsWithControlLink
import com.micsbol.telecon4esp32.domain.camera.isKnownSoftApLaggyClient
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveCameraVideoSoftApTarget
import com.micsbol.telecon4esp32.domain.camera.resolveSessionCameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveSoftApHudPreviewOptions
import com.micsbol.telecon4esp32.domain.camera.shouldPreferCapturePollingForLowLatency
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.coerceForApplication
import com.micsbol.telecon4esp32.domain.model.resolveCameraHardwareRole
import com.micsbol.telecon4esp32.domain.use_case.ApplyCameraHardwareRoleUseCase
import com.micsbol.telecon4esp32.domain.use_case.ApplySoftApCamConfigUseCase
import com.micsbol.telecon4esp32.domain.use_case.EnsureSoftApStreamQualityDefaultsUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetControlPanelCenterModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUseSoftApCameraUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveControlPanelCenterModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApPerformancePresetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlPanelCameraStreamViewModel @Inject constructor(
    private val cameraStreamRepository: CameraStreamRepository,
    getApplicationBoard: GetApplicationBoardUseCase,
    getApplicationConnectionMode: GetApplicationConnectionModeUseCase,
    getUseSoftApCamera: GetUseSoftApCameraUseCase,
    getControlPanelCenterMode: GetControlPanelCenterModeUseCase,
    private val saveControlPanelCenterMode: SaveControlPanelCenterModeUseCase,
    private val applyCameraHardwareRole: ApplyCameraHardwareRoleUseCase,
    getSoftApPerformancePreset: GetSoftApPerformancePresetUseCase,
    private val saveSoftApPerformancePreset: SaveSoftApPerformancePresetUseCase,
    getSoftApHudProcessingRate: GetSoftApHudProcessingRateUseCase,
    private val saveSoftApHudProcessingRate: SaveSoftApHudProcessingRateUseCase,
    private val ensureSoftApStreamQualityDefaults: EnsureSoftApStreamQualityDefaultsUseCase,
    private val applySoftApCamConfig: ApplySoftApCamConfigUseCase,
    private val softApWifiSession: SoftApWifiSession,
    private val softApNetworkResolver: SoftApNetworkResolver,
) : ViewModel() {

    private val applicationId = ApplicationId.CONTROL_PANEL
    private val runningOnEmulator = isLikelyEmulator()
    private val cameraSession = Esp32CameraLinkSession(
        repository = cameraStreamRepository,
        skipOnEmulator = runningOnEmulator,
    )
    private val cameraPaneVisible = MutableStateFlow(false)
    private val sessionArmed = MutableStateFlow(false)
    private val _cameraConnectUi =
        MutableStateFlow<ControlPanelCameraConnectUi>(ControlPanelCameraConnectUi.Idle)
    private var connectJob: Job? = null

    val cameraConnectUi: StateFlow<ControlPanelCameraConnectUi> = _cameraConnectUi.asStateFlow()

    val centerMode: StateFlow<ControlPanelCenterMode> = getControlPanelCenterMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ControlPanelCenterMode.PLOTS,
        )

    private val board: StateFlow<Esp32Board> = getApplicationBoard(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(applicationId),
        )

    private val connectionMode: StateFlow<BluetoothConnectionMode> =
        getApplicationConnectionMode(applicationId)
            .map { stored -> stored ?: BluetoothConnectionMode.CLASSIC_SIMPLE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothConnectionMode.CLASSIC_SIMPLE,
            )

    private val useSoftApCamera: StateFlow<Boolean> =
        getUseSoftApCamera(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    val cameraLinkProfile: StateFlow<CameraLinkProfile> = combine(
        board,
        connectionMode,
        useSoftApCamera,
    ) { selectedBoard, mode, overlay ->
        resolveCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraLinkProfile.CONTROL_ONLY,
    )

    val cameraPreviewState: StateFlow<CameraStreamState> = combine(
        cameraStreamRepository.streamState,
        sessionArmed,
        cameraPaneVisible,
    ) { streamState, armed, visible ->
        val streaming = visible && armed && !runningOnEmulator
        if (streaming) streamState else CameraStreamState.Idle
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraStreamState.Idle,
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

    val showStreamQualityPanel: StateFlow<Boolean> = combine(
        sessionArmed,
        cameraPaneVisible,
    ) { armed, visible ->
        armed && visible && !runningOnEmulator
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    init {
        viewModelScope.launch {
            combine(cameraPaneVisible, cameraLinkProfile) { visible, profile ->
                visible to profile.shouldStartCameraStream
            }
                .distinctUntilChanged()
                .collect { (visible, shouldStream) ->
                    if (!visible) return@collect
                    if (shouldStream) {
                        startCameraWifiConnect()
                    } else {
                        stopCameraConnectWithoutLeavingPane()
                    }
                }
        }
        viewModelScope.launch {
            combine(
                board,
                connectionMode,
                useSoftApCamera,
                sessionArmed,
                cameraPaneVisible,
            ) { selectedBoard, mode, overlay, armed, visible ->
                val profile = if (armed && visible) {
                    resolveSessionCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
                } else {
                    resolveCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
                }
                Triple(profile, armed && visible, visible)
            }
                .distinctUntilChanged()
                .collect { (profile, armed, visible) ->
                    cameraSession.setCameraEnabled(armed)
                    cameraSession.setProfile(profile)
                    if (visible && armed) {
                        cameraSession.onVisible()
                    }
                }
        }
        viewModelScope.launch {
            combine(
                sessionArmed,
                softApPerformancePreset,
                softApHudProcessingRate,
                cameraPaneVisible,
            ) { armed, preset, rate, visible ->
                Triple(
                    visible && armed && !runningOnEmulator,
                    preset,
                    rate,
                )
            }
                .distinctUntilChanged()
                .collect { (streaming, preset, rate) ->
                    cameraStreamRepository.setPreferCapturePolling(
                        shouldPreferCapturePollingForLowLatency(
                            preset = preset,
                            atRisk = isKnownSoftApLaggyClient(
                                Build.MANUFACTURER,
                                Build.MODEL,
                            ),
                            streaming = streaming,
                        ),
                    )
                    cameraStreamRepository.setHudPreviewOptions(
                        resolveSoftApHudPreviewOptions(
                            preset = preset,
                            hudRate = rate,
                            streaming = streaming,
                        ),
                    )
                }
        }
        viewModelScope.launch {
            combine(
                showStreamQualityPanel,
                softApPerformancePreset,
            ) { showPanel, preset ->
                showPanel to preset
            }
                .distinctUntilChanged()
                .collect { (armed, preset) ->
                    if (!armed) {
                        applySoftApCamConfig.clearSession(applicationId)
                        return@collect
                    }
                    applySoftApCamConfig(
                        applicationId = applicationId,
                        preset = preset,
                        onStreamRestartRequired = { cameraSession.restartIfStreaming() },
                    )
                }
        }
    }

    fun onCenterModeSelected(mode: ControlPanelCenterMode) {
        viewModelScope.launch {
            val current = centerMode.value
            val next = if (mode == ControlPanelCenterMode.CAMERA &&
                current == ControlPanelCenterMode.CAMERA
            ) {
                ControlPanelCenterMode.PLOTS
            } else {
                mode
            }
            if (next != current) {
                saveControlPanelCenterMode(next)
            }
            val enableCamera = next == ControlPanelCenterMode.CAMERA
            val storedRole = resolveCameraHardwareRole(board.value, useSoftApCamera.value)
            val nextRole = if (enableCamera) {
                if (storedRole == CameraHardwareRole.NO_CAM) {
                    CameraHardwareRole.TWO_DEVICES
                } else {
                    storedRole.coerceForApplication(applicationId)
                }
            } else {
                CameraHardwareRole.NO_CAM
            }
            if (nextRole != storedRole) {
                applyCameraHardwareRole(applicationId, nextRole)
            }
        }
    }

    fun onCameraPaneVisible(visible: Boolean) {
        cameraPaneVisible.value = visible
        if (!visible) {
            connectJob?.cancel()
            connectJob = null
            sessionArmed.value = false
            _cameraConnectUi.value = ControlPanelCameraConnectUi.Idle
            applySoftApCamConfig.clearSession(applicationId)
            cameraStreamRepository.setPreferCapturePolling(false)
            cameraStreamRepository.setHudPreviewOptions(HudPreviewOptions.FULL_QUALITY)
            cameraSession.onHidden()
            return
        }
        if (cameraLinkProfile.value.shouldStartCameraStream) {
            viewModelScope.launch {
                ensureSoftApStreamQualityDefaults(applicationId)
            }
        }
    }

    fun retryCameraWifi() {
        if (cameraPaneVisible.value) {
            startCameraWifiConnect()
        }
    }

    private fun startCameraWifiConnect() {
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            val selectedBoard = board.value
            val mode = connectionMode.value
            val overlay = useSoftApCamera.value
            val profile = resolveCameraLinkProfile(
                applicationId,
                selectedBoard,
                mode,
                overlay,
            )
            if (!profile.shouldStartCameraStream) {
                sessionArmed.value = false
                _cameraConnectUi.value = ControlPanelCameraConnectUi.Disabled
                return@launch
            }
            val target = resolveCameraVideoSoftApTarget(selectedBoard, mode)
            if (cameraVideoSoftApConflictsWithControlLink(selectedBoard, mode)) {
                sessionArmed.value = false
                _cameraConnectUi.value = ControlPanelCameraConnectUi.ControlWifiConflict(target.ssid)
                return@launch
            }
            if (runningOnEmulator) {
                sessionArmed.value = false
                _cameraConnectUi.value = ControlPanelCameraConnectUi.NeedsWifi(
                    ssid = target.ssid,
                    password = target.password,
                )
                return@launch
            }
            val wasArmed = sessionArmed.value
            sessionArmed.value = false
            _cameraConnectUi.value = ControlPanelCameraConnectUi.Connecting

            val alreadyOnCameraAp = isAlreadyOnCameraVideoSoftAp(target.ssid)
            if (alreadyOnCameraAp) {
                armVideoSession(
                    selectedBoard = selectedBoard,
                    mode = mode,
                    overlay = overlay,
                    target = target,
                    restartIfAlreadyStreaming = wasArmed,
                )
                return@launch
            }

            when (softApWifiSession.join(target.ssid, target.password)) {
                is SoftApWifiSession.JoinResult.Available,
                is SoftApWifiSession.JoinResult.AlreadyBound,
                -> armVideoSession(
                    selectedBoard = selectedBoard,
                    mode = mode,
                    overlay = overlay,
                    target = target,
                    restartIfAlreadyStreaming = wasArmed,
                )
                SoftApWifiSession.JoinResult.Unsupported -> {
                    if (softApNetworkResolver.hasEsp32SoftApLinkAddress()) {
                        armVideoSession(
                            selectedBoard = selectedBoard,
                            mode = mode,
                            overlay = overlay,
                            target = target,
                            restartIfAlreadyStreaming = wasArmed,
                        )
                    } else {
                        _cameraConnectUi.value = ControlPanelCameraConnectUi.NeedsWifi(
                            ssid = target.ssid,
                            password = target.password,
                        )
                    }
                }
                SoftApWifiSession.JoinResult.RejectedOrTimeout -> {
                    _cameraConnectUi.value = ControlPanelCameraConnectUi.NeedsWifi(
                        ssid = target.ssid,
                        password = target.password,
                    )
                }
            }
        }
    }

    private fun stopCameraConnectWithoutLeavingPane() {
        connectJob?.cancel()
        connectJob = null
        sessionArmed.value = false
        _cameraConnectUi.value = ControlPanelCameraConnectUi.Disabled
        applySoftApCamConfig.clearSession(applicationId)
        cameraStreamRepository.setPreferCapturePolling(false)
        cameraStreamRepository.setHudPreviewOptions(HudPreviewOptions.FULL_QUALITY)
        cameraSession.setCameraEnabled(false)
        cameraSession.onHidden()
    }

    private fun armVideoSession(
        selectedBoard: Esp32Board,
        mode: BluetoothConnectionMode,
        overlay: Boolean,
        target: CameraVideoSoftApTarget,
        restartIfAlreadyStreaming: Boolean,
    ) {
        val profile = resolveSessionCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
        cameraSession.setCameraEnabled(true)
        cameraSession.setProfile(profile)
        cameraSession.onVisible()
        if (restartIfAlreadyStreaming) {
            cameraSession.restartIfStreaming()
        }
        sessionArmed.value = true
        _cameraConnectUi.value = ControlPanelCameraConnectUi.Ready(
            ssid = target.ssid,
            password = target.password,
        )
    }

    private fun isAlreadyOnCameraVideoSoftAp(expectedSsid: String): Boolean {
        val boundSsid = softApWifiSession.currentSsid()
        if (!boundSsid.isNullOrBlank()) {
            return boundSsid.equals(expectedSsid, ignoreCase = true)
        }
        return softApNetworkResolver.hasEsp32SoftApLinkAddress()
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

    override fun onCleared() {
        connectJob?.cancel()
        cameraPaneVisible.value = false
        sessionArmed.value = false
        applySoftApCamConfig.clearSession(applicationId)
        cameraSession.stop()
        super.onCleared()
    }

    private companion object {
        fun isLikelyEmulator(): Boolean {
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
    }
}
