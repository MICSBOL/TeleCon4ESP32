package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.ui.bluetooth.RcControlState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {

    private val cameraBaseUrl = Esp32CameraDefaults.DEFAULT_BASE_URL
    private val runningOnEmulator = isLikelyEmulator()

    private val _uiState = MutableStateFlow(
        RcVehicleProUiState(isEmulatorPreview = runningOnEmulator),
    )
    val uiState: StateFlow<RcVehicleProUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cameraStreamRepository.streamState,
                remoteController.isConnected,
                remoteController.telemetryState,
            ) { cameraState, connected, telemetry ->
                Triple(cameraState, connected, telemetry)
            }.collect { (cameraState, connected, telemetry) ->
                val telemetrySpeed = if (connected && telemetry.panelState.leftValue > 0) {
                    telemetry.panelState.leftValue / 10f
                } else {
                    null
                }
                _uiState.update { current ->
                    current.copy(
                        cameraState = if (runningOnEmulator) {
                            CameraStreamState.Idle
                        } else {
                            cameraState
                        },
                        isBluetoothConnected = connected,
                        isCameraOnline = !runningOnEmulator && cameraState is CameraStreamState.Frame,
                        batteryPercent = normalizeBatteryPercent(
                            telemetry.indicatorState.batteryLevel,
                        ),
                        motorTempCelsius = mapAnalogToMotorTemp(
                            telemetry.indicatorState.analogValue,
                        ),
                        speedKmh = telemetrySpeed ?: current.speedKmh,
                        speedFromTelemetry = telemetrySpeed != null,
                    )
                }
            }
        }
    }

    fun onScreenVisible() {
        // Wi‑Fi JPEG stream is independent of Bluetooth control/telemetry.
        if (runningOnEmulator) return
        cameraStreamRepository.startStream(cameraBaseUrl)
    }

    fun onScreenHidden() {
        if (runningOnEmulator) return
        cameraStreamRepository.stopStream()
        _uiState.update { it.copy(cameraState = CameraStreamState.Idle, isCameraOnline = false) }
    }

    fun updateDriveMetrics(rcControlState: RcControlState) {
        val stickSpeed = abs(rcControlState.leftStickPosition.second) * MAX_SPEED_KMH
        _uiState.update { current ->
            if (current.speedFromTelemetry) {
                current
            } else {
                current.copy(speedKmh = stickSpeed)
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

    fun onCapturePhoto() {
        val frame = (_uiState.value.cameraState as? CameraStreamState.Frame)?.bitmap ?: run {
            _uiState.update { it.copy(photoFeedback = PhotoFeedback.NoFrame) }
            clearPhotoFeedbackLater()
            return
        }
        // Copy so gallery encode is safe if the stream recycles the live frame.
        val snapshot = frame.copy(Bitmap.Config.ARGB_8888, false)
        if (snapshot == null) {
            _uiState.update { it.copy(photoFeedback = PhotoFeedback.Failed) }
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
        if (!runningOnEmulator) {
            cameraStreamRepository.stopStream()
        }
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

    private companion object {
        const val MAX_SPEED_KMH = 40f
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
