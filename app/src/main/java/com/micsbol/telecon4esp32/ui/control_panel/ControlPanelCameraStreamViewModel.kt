package com.micsbol.telecon4esp32.ui.control_panel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.data.camera.CameraJpegVideoRecorder
import com.micsbol.telecon4esp32.data.camera.saveCameraStillJpeg
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
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    getSoftApPerformancePreset: GetSoftApPerformancePresetUseCase,
    private val saveSoftApPerformancePreset: SaveSoftApPerformancePresetUseCase,
    getSoftApHudProcessingRate: GetSoftApHudProcessingRateUseCase,
    private val saveSoftApHudProcessingRate: SaveSoftApHudProcessingRateUseCase,
    private val ensureSoftApStreamQualityDefaults: EnsureSoftApStreamQualityDefaultsUseCase,
    private val applySoftApCamConfig: ApplySoftApCamConfigUseCase,
    private val softApWifiSession: SoftApWifiSession,
    private val softApNetworkResolver: SoftApNetworkResolver,
    @ApplicationContext private val appContext: Context,
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

    private val _isVideoRecording = MutableStateFlow(false)
    val isVideoRecording: StateFlow<Boolean> = _isVideoRecording.asStateFlow()

    private val _videoExport = MutableStateFlow<CameraVideoExport?>(null)
    val videoExport: StateFlow<CameraVideoExport?> = _videoExport.asStateFlow()

    private val _stillResult = MutableStateFlow<CameraStillResult?>(null)
    val stillResult: StateFlow<CameraStillResult?> = _stillResult.asStateFlow()

    private var videoJob: Job? = null

    val centerMode: StateFlow<ControlPanelCenterMode> = getControlPanelCenterMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ControlPanelCenterMode.TERMINAL,
        )

    val board: StateFlow<Esp32Board> = getApplicationBoard(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(applicationId),
        )

    val connectionMode: StateFlow<BluetoothConnectionMode> =
        getApplicationConnectionMode(applicationId)
            .map { stored -> stored ?: BluetoothConnectionMode.CLASSIC_SIMPLE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothConnectionMode.CLASSIC_SIMPLE,
            )

    /**
     * False until board and connection mode have been read. Those StateFlows
     * start as DevKit + Classic, which looks like Wi‑Fi control-only and must
     * not be written back over a saved one-CAM link.
     */
    val controlLinkReady: StateFlow<Boolean> = combine(
        getApplicationBoard(applicationId),
        getApplicationConnectionMode(applicationId),
    ) { _, _ -> true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
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
            cameraPaneVisible.collect { visible ->
                if (visible && !sessionArmed.value) {
                    startCameraWifiConnect()
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
                val profile = if (armed) {
                    resolveSessionCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
                } else {
                    resolveCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
                }
                Triple(profile, armed, visible)
            }
                .distinctUntilChanged()
                .collect { (profile, armed, visible) ->
                    val showVideo = armed && visible
                    cameraSession.setCameraEnabled(showVideo)
                    cameraSession.setProfile(profile)
                    if (showVideo) {
                        cameraSession.onVisible()
                    } else {
                        cameraSession.onHidden()
                    }
                }
        }
        viewModelScope.launch {
            combine(
                sessionArmed,
                softApPerformancePreset,
                softApHudProcessingRate,
            ) { armed, preset, rate ->
                Triple(
                    armed && !runningOnEmulator,
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
                sessionArmed,
                softApPerformancePreset,
            ) { armed, preset ->
                (armed && !runningOnEmulator) to preset
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

    fun onCenterModeSelected(
        mode: ControlPanelCenterMode,
        toggleCamera: Boolean = true,
    ) {
        viewModelScope.launch {
            val current = centerMode.value
            val next = if (
                toggleCamera &&
                mode == ControlPanelCenterMode.CAMERA &&
                current == ControlPanelCenterMode.CAMERA
            ) {
                ControlPanelCenterMode.TERMINAL
            } else {
                mode
            }
            if (next != current) {
                saveControlPanelCenterMode(next)
            }
        }
    }

    /**
     * The camera pane can leave the center display while the user looks at the
     * terminal, plots, stick, or radar. JPEG decode and the Wi-Fi lock stop
     * until this pane is visible again.
     */
    fun onCameraPaneVisible(visible: Boolean) {
        cameraPaneVisible.value = visible
        if (!visible) {
            stopVideoRecording()
            return
        }
        if (!sessionArmed.value) {
            val target = resolveCameraVideoSoftApTarget(board.value, connectionMode.value)
            _cameraConnectUi.value = ControlPanelCameraConnectUi.Connecting(
                ssid = target.ssid,
                password = target.password,
            )
            viewModelScope.launch {
                ensureSoftApStreamQualityDefaults(applicationId)
            }
        }
    }

    fun toggleVideoRecording() {
        if (_isVideoRecording.value) {
            stopVideoRecording()
            return
        }
        if (videoJob?.isActive == true) return
        _videoExport.value = null
        _isVideoRecording.value = true
        videoJob = viewModelScope.launch(Dispatchers.IO) {
            val recorder = CameraJpegVideoRecorder(appContext)
            var epoch = -1L
            var saved: String? = null
            try {
                while (isActive && _isVideoRecording.value) {
                    val now = cameraStreamRepository.lastJpegEpoch()
                    if (now != epoch) {
                        cameraStreamRepository.copyLastJpeg()?.let(recorder::appendJpeg)
                        epoch = now
                    }
                    delay(100)
                }
            } finally {
                saved = runCatching { recorder.finish() }.getOrNull()
                videoJob = null
                _isVideoRecording.value = false
                _videoExport.value = saved?.let { location ->
                    CameraVideoExport.Saved(
                        fileName = location.substringAfterLast('/'),
                        location = location,
                    )
                } ?: CameraVideoExport.Failed
            }
        }
    }

    fun stopVideoRecording() {
        _isVideoRecording.value = false
    }

    fun dismissVideoExport() {
        _videoExport.value = null
    }

    fun capturePhoto() {
        val jpeg = cameraStreamRepository.copyLastJpeg()
        if (jpeg == null) {
            _stillResult.value = CameraStillResult.NoFrame
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val saved = runCatching { saveCameraStillJpeg(appContext, jpeg) }.getOrNull()
            _stillResult.value = saved?.let { location ->
                CameraStillResult.Saved(
                    fileName = location.substringAfterLast('/'),
                    location = location,
                )
            } ?: CameraStillResult.Failed
        }
    }

    fun dismissStillResult() {
        _stillResult.value = null
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
            val target = resolveCameraVideoSoftApTarget(selectedBoard, mode)
            _cameraConnectUi.value = ControlPanelCameraConnectUi.Connecting(
                ssid = target.ssid,
                password = target.password,
            )
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
            _cameraConnectUi.value = ControlPanelCameraConnectUi.Connecting(
                ssid = target.ssid,
                password = target.password,
            )

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

    private fun armVideoSession(
        selectedBoard: Esp32Board,
        mode: BluetoothConnectionMode,
        overlay: Boolean,
        target: CameraVideoSoftApTarget,
        restartIfAlreadyStreaming: Boolean,
    ) {
        val profile = resolveSessionCameraLinkProfile(applicationId, selectedBoard, mode, overlay)
        val showVideo = cameraPaneVisible.value
        cameraSession.setCameraEnabled(showVideo)
        cameraSession.setProfile(profile)
        if (showVideo) {
            cameraSession.onVisible()
            if (restartIfAlreadyStreaming) {
                cameraSession.restartIfStreaming()
            }
        } else {
            cameraSession.onHidden()
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
        releaseCameraLink()
        super.onCleared()
    }

    private fun releaseCameraLink() {
        stopVideoRecording()
        connectJob?.cancel()
        connectJob = null
        cameraPaneVisible.value = false
        sessionArmed.value = false
        _cameraConnectUi.value = ControlPanelCameraConnectUi.Idle
        applySoftApCamConfig.clearSession(applicationId)
        cameraStreamRepository.setPreferCapturePolling(false)
        cameraStreamRepository.setHudPreviewOptions(HudPreviewOptions.FULL_QUALITY)
        cameraSession.stop()
    }

sealed interface CameraVideoExport {
    data class Saved(val fileName: String, val location: String) : CameraVideoExport
    data object Failed : CameraVideoExport
}

sealed interface CameraStillResult {
    data class Saved(val fileName: String, val location: String) : CameraStillResult
    data object NoFrame : CameraStillResult
    data object Failed : CameraStillResult
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
