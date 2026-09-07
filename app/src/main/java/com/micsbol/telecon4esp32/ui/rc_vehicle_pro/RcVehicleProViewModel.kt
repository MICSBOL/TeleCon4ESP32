package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraLinkSession
import com.micsbol.telecon4esp32.domain.camera.HudPreviewOptions
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.camera.isCameraStreamAtRisk
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveSoftApHudPreviewOptions
import com.micsbol.telecon4esp32.domain.camera.shouldPreferCapturePollingForLowLatency
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import com.micsbol.telecon4esp32.domain.use_case.ApplySoftApCamConfigUseCase
import com.micsbol.telecon4esp32.domain.use_case.EnsureSoftApStreamQualityDefaultsUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUseSoftApCameraUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudPlotChrome
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.decodeRcHudPlotChrome
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.encodeRcHudPlotChrome
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class RcVehicleProViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cameraStreamRepository: CameraStreamRepository,
    private val remoteController: RemoteController,
    private val settingsRepository: ISettingsRepository,
    getApplicationBoard: GetApplicationBoardUseCase,
    getApplicationConnectionMode: GetApplicationConnectionModeUseCase,
    getUseSoftApCamera: GetUseSoftApCameraUseCase,
    getSoftApPerformancePreset: GetSoftApPerformancePresetUseCase,
    getSoftApHudProcessingRate: GetSoftApHudProcessingRateUseCase,
    private val saveSoftApPerformancePreset: SaveSoftApPerformancePresetUseCase,
    private val saveSoftApHudProcessingRate: SaveSoftApHudProcessingRateUseCase,
    private val ensureSoftApStreamQualityDefaults: EnsureSoftApStreamQualityDefaultsUseCase,
    private val applySoftApCamConfig: ApplySoftApCamConfigUseCase,
) : ViewModel() {

    private val runningOnEmulator = isLikelyEmulator()
    private val cameraSession = Esp32CameraLinkSession(
        repository = cameraStreamRepository,
        skipOnEmulator = runningOnEmulator,
    )
    private val screenVisibleFlow = MutableStateFlow(false)
    private val applicationId = ApplicationId.RC_VEHICLE_PRO

    private val board: StateFlow<Esp32Board> = getApplicationBoard(ApplicationId.RC_VEHICLE_PRO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(ApplicationId.RC_VEHICLE_PRO),
        )

    private val connectionMode: StateFlow<BluetoothConnectionMode> =
        getApplicationConnectionMode(ApplicationId.RC_VEHICLE_PRO)
            .map { stored -> stored ?: BluetoothConnectionMode.CLASSIC_SIMPLE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothConnectionMode.CLASSIC_SIMPLE,
            )

    private val useSoftApCamera: StateFlow<Boolean> =
        getUseSoftApCamera(ApplicationId.RC_VEHICLE_PRO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    val softApPerformancePreset: StateFlow<SoftApPerformancePreset> =
        getSoftApPerformancePreset(ApplicationId.RC_VEHICLE_PRO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApPerformancePreset.defaultFor(ApplicationId.RC_VEHICLE_PRO),
            )

    val softApHudProcessingRate: StateFlow<SoftApHudProcessingRate> =
        getSoftApHudProcessingRate(ApplicationId.RC_VEHICLE_PRO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApHudProcessingRate.DEFAULT,
            )

    val cameraLinkProfile: StateFlow<CameraLinkProfile> = combine(
        board,
        connectionMode,
        useSoftApCamera,
    ) { selectedBoard, selectedMode, overlay ->
        resolveCameraLinkProfile(
            ApplicationId.RC_VEHICLE_PRO,
            selectedBoard,
            selectedMode,
            overlay,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraLinkProfile.CONTROL_ONLY,
    )

    /**
     * SoftAP frames only — collected by the camera layer so HUD/controls do not
     * recompose on every JPEG.
     */
    val cameraPreviewState: StateFlow<CameraStreamState> = combine(
        cameraStreamRepository.streamState,
        cameraLinkProfile,
        screenVisibleFlow,
    ) { streamState, profile, visible ->
        val streaming = visible && profile.shouldStartCameraStream && !runningOnEmulator
        if (streaming) streamState else CameraStreamState.Idle
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraStreamState.Idle,
    )

    private val _uiState = MutableStateFlow(
        RcVehicleProUiState(isEmulatorPreview = runningOnEmulator),
    )
    val uiState: StateFlow<RcVehicleProUiState> = _uiState.asStateFlow()

    val controlSettings: StateFlow<RcVehicleProControlSettings> =
        settingsRepository.rcVehicleProControlSettingsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RcVehicleProControlSettings.DEFAULT,
            )

    private val _plotChrome = MutableStateFlow(RcHudPlotChrome())
    val plotChrome: StateFlow<RcHudPlotChrome> = _plotChrome.asStateFlow()

    fun updateControlSettings(transform: (RcVehicleProControlSettings) -> RcVehicleProControlSettings) {
        viewModelScope.launch {
            val next = transform(controlSettings.value)
            settingsRepository.saveRcVehicleProControlSettings(next)
        }
    }

    fun updatePlotChrome(chrome: RcHudPlotChrome) {
        _plotChrome.value = chrome
        viewModelScope.launch {
            settingsRepository.saveRcVehicleProHudPlotChrome(encodeRcHudPlotChrome(chrome))
        }
    }

    fun cycleThrottleTravel() {
        updateControlSettings {
            it.copy(throttleTravel = RcStickMapping.nextTravelPreset(it.throttleTravel))
        }
    }

    fun cycleSteerTravel() {
        updateControlSettings {
            it.copy(steerTravel = RcStickMapping.nextTravelPreset(it.steerTravel))
        }
    }

    init {
        viewModelScope.launch {
            _plotChrome.value = decodeRcHudPlotChrome(
                settingsRepository.rcVehicleProHudPlotChromeFlow().first(),
            )
        }
        viewModelScope.launch {
            cameraLinkProfile.collect { profile ->
                // SoftAP video auto-arms for Kit A and DevKit Bluetooth + overlay whenever the profile expects a camera.
                cameraSession.setCameraEnabled(profile.shouldStartCameraStream)
                cameraSession.setProfile(profile)
                _uiState.update {
                    it.copy(
                        cameraLinkProfile = profile,
                        isCameraStreamArmed = profile.shouldStartCameraStream,
                    )
                }
            }
        }
        // HUD decode options: only when SoftAP arming / preset / rate change — not per frame.
        viewModelScope.launch {
            combine(
                cameraLinkProfile,
                softApPerformancePreset,
                softApHudProcessingRate,
                screenVisibleFlow,
            ) { profile, preset, rate, visible ->
                SoftApHudOptionsSnapshot(
                    streaming = visible && profile.shouldStartCameraStream && !runningOnEmulator,
                    preset = preset,
                    rate = rate,
                )
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    cameraStreamRepository.setPreferCapturePolling(
                        shouldPreferCapturePollingForLowLatency(
                            preset = snapshot.preset,
                            atRisk = appContext.isCameraStreamAtRisk(),
                            streaming = snapshot.streaming,
                        ),
                    )
                    cameraStreamRepository.setHudPreviewOptions(
                        resolveSoftApHudPreviewOptions(
                            preset = snapshot.preset,
                            hudRate = snapshot.rate,
                            streaming = snapshot.streaming,
                        ),
                    )
                }
        }
        // HUD chrome (online / telemetry / link) — exclude Frame bitmaps so stick UI stays quiet.
        viewModelScope.launch {
            combine(
                cameraStreamRepository.streamState
                    .map { it is CameraStreamState.Frame }
                    .distinctUntilChanged(),
                remoteController.isConnected,
                remoteController.telemetryState,
                cameraLinkProfile,
                screenVisibleFlow,
            ) { hasFrame, connected, telemetry, profile, visible ->
                val streaming = visible && profile.shouldStartCameraStream && !runningOnEmulator
                val telemetrySpeed = if (connected && telemetry.panelState.leftValue > 0) {
                    telemetry.panelState.leftValue / 10f
                } else {
                    null
                }
                SoftApHudChromeSnapshot(
                    profile = profile,
                    connected = connected,
                    isCameraOnline = streaming && hasFrame,
                    batteryPercent = normalizeBatteryPercent(telemetry.indicatorState.batteryLevel),
                    motorTempCelsius = mapAnalogToMotorTemp(telemetry.indicatorState.analogValue),
                    telemetrySpeed = telemetrySpeed,
                )
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    _uiState.update { current ->
                        current.copy(
                            cameraLinkProfile = snapshot.profile,
                            isCameraStreamArmed = snapshot.profile.shouldStartCameraStream,
                            isBluetoothConnected = snapshot.connected,
                            isCameraOnline = snapshot.isCameraOnline,
                            batteryPercent = snapshot.batteryPercent,
                            motorTempCelsius = snapshot.motorTempCelsius,
                            speedKmh = snapshot.telemetrySpeed ?: current.speedKmh,
                            speedFromTelemetry = snapshot.telemetrySpeed != null,
                        )
                    }
                }
        }
        // Phase 2: push SoftAP preset to firmware `/camconfig` when SoftAP HUD is armed.
        viewModelScope.launch {
            combine(cameraLinkProfile, softApPerformancePreset, screenVisibleFlow) { profile, preset, visible ->
                Triple(profile, preset, visible)
            }
                .map { (profile, preset, visible) ->
                    val armed = visible && profile.shouldStartCameraStream && !runningOnEmulator
                    armed to preset
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
                        onStreamRestartRequired = {
                            cameraSession.restartIfStreaming()
                        },
                    )
                }
        }
    }

    fun onScreenVisible() {
        screenVisibleFlow.value = true
        cameraSession.setCameraEnabled(cameraLinkProfile.value.shouldStartCameraStream)
        cameraSession.onVisible()
        viewModelScope.launch {
            ensureSoftApStreamQualityDefaults(applicationId)
            if (cameraLinkProfile.value.shouldStartCameraStream && !runningOnEmulator) {
                applySoftApCamConfig(
                    applicationId = applicationId,
                    preset = softApPerformancePreset.value,
                    onStreamRestartRequired = { cameraSession.restartIfStreaming() },
                )
            }
        }
    }

    fun onScreenHidden() {
        screenVisibleFlow.value = false
        applySoftApCamConfig.clearSession(applicationId)
        cameraStreamRepository.setPreferCapturePolling(false)
        cameraStreamRepository.setHudPreviewOptions(HudPreviewOptions.FULL_QUALITY)
        cameraSession.onHidden()
        _uiState.update {
            it.copy(
                isCameraOnline = false,
                isCameraStreamArmed = cameraLinkProfile.value.shouldStartCameraStream,
            )
        }
    }

    fun updateDriveMetricsFromThrottle(throttleY: Float) {
        val stickSpeed = abs(throttleY) * RcVehicleProLayout.HUD_MAX_SPEED_KMH
        val displaySpeed = (stickSpeed * 10f).roundToInt() / 10f
        _uiState.update { current ->
            when {
                current.speedFromTelemetry -> current
                current.speedKmh == displaySpeed -> current
                else -> current.copy(speedKmh = displaySpeed)
            }
        }
    }

    fun onEmergencyStop() {
        _uiState.update {
            it.copy(speedKmh = 0f, speedFromTelemetry = false)
        }
    }

    fun onToggleRecording() {
        _uiState.update { it.copy(isRecording = !it.isRecording) }
    }

    fun onToggleLights() {
        _uiState.update { it.copy(lightsOn = !it.lightsOn) }
    }

    fun onSoftApPerformancePresetChanged(preset: SoftApPerformancePreset) {
        viewModelScope.launch {
            saveSoftApPerformancePreset(ApplicationId.RC_VEHICLE_PRO, preset)
        }
    }

    fun onSoftApHudProcessingRateChanged(rate: SoftApHudProcessingRate) {
        viewModelScope.launch {
            saveSoftApHudProcessingRate(ApplicationId.RC_VEHICLE_PRO, rate)
        }
    }

    fun onCapturePhoto() {
        // Prefer full-quality decode of the last JPEG (HUD may be downsampled RGB_565).
        val still = cameraStreamRepository.captureStillBitmap()
        val snapshot = still ?: run {
            val frame = (cameraPreviewState.value as? CameraStreamState.Frame)?.bitmap
            frame?.copy(Bitmap.Config.ARGB_8888, false)
        }
        if (snapshot == null) {
            _uiState.update { it.copy(photoFeedback = PhotoFeedback.NoFrame) }
            clearPhotoFeedbackLater()
            return
        }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    saveFrameToGallery(snapshot)
                } finally {
                    snapshot.recycle()
                }
            }
            _uiState.update {
                it.copy(
                    photoFeedback = if (saved) PhotoFeedback.Saved else PhotoFeedback.Failed,
                )
            }
            clearPhotoFeedbackLater()
        }
    }

    fun consumePhotoFeedback() {
        _uiState.update { it.copy(photoFeedback = PhotoFeedback.None) }
    }

    override fun onCleared() {
        screenVisibleFlow.value = false
        applySoftApCamConfig.clearSession(applicationId)
        cameraSession.stop()
        super.onCleared()
    }

    private fun clearPhotoFeedbackLater() {
        viewModelScope.launch {
            delay(PHOTO_FEEDBACK_MS)
            _uiState.update { it.copy(photoFeedback = PhotoFeedback.None) }
        }
    }

    private fun saveFrameToGallery(bitmap: Bitmap): Boolean {
        val resolver = appContext.contentResolver
        val fileName = "TeleCon_RC_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/TeleCon",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    return false
                }
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    /** ESP32 may send battery as 0–100 % or 0–255 gauge; normalize to percent. */
    private fun normalizeBatteryPercent(raw: Int): Int {
        val value = raw.coerceAtLeast(0)
        return if (value <= 100) value else ((value / 255f) * 100f).roundToInt().coerceIn(0, 100)
    }

    /** Maps RC analog gauge (0–255) to a plausible motor temperature °C. */
    private fun mapAnalogToMotorTemp(analogValue: Int): Int {
        return (25 + (analogValue.coerceIn(0, 255) * 0.2f)).roundToInt()
    }

    private data class SoftApHudOptionsSnapshot(
        val streaming: Boolean,
        val preset: SoftApPerformancePreset,
        val rate: SoftApHudProcessingRate,
    )

    private data class SoftApHudChromeSnapshot(
        val profile: CameraLinkProfile,
        val connected: Boolean,
        val isCameraOnline: Boolean,
        val batteryPercent: Int,
        val motorTempCelsius: Int,
        val telemetrySpeed: Float?,
    )

    private companion object {
        const val JPEG_QUALITY = 92
        const val PHOTO_FEEDBACK_MS = 2_000L

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
