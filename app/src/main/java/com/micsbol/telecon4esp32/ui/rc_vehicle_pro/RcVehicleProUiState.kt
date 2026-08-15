package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.model.RcCameraPan

enum class PhotoFeedback {
    None,
    Saved,
    NoFrame,
    Failed,
}

data class RcVehicleProUiState(
    val cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    val isBluetoothConnected: Boolean = false,
    val isCameraOnline: Boolean = false,
    /** SoftAP HTTP video armed for Kit A / Kit B whenever the profile expects a camera. */
    val isCameraStreamArmed: Boolean = false,
    val isEmulatorPreview: Boolean = false,
    val speedKmh: Float = 0f,
    /** True when HUD speed comes from ESP32 `RC:DATA` / `CC 11` left panel (speed×10). */
    val speedFromTelemetry: Boolean = false,
    val batteryPercent: Int = 0,
    val motorTempCelsius: Int = 0,
    val isRecording: Boolean = false,
    val lightsOn: Boolean = false,
    val photoFeedback: PhotoFeedback = PhotoFeedback.None,
) {
    val isWifiSoftApControl: Boolean
        get() = cameraLinkProfile == CameraLinkProfile.WIFI_SOFTAP

    val isBleControlWithCamera: Boolean
        get() = cameraLinkProfile == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE

    val expectsCameraStream: Boolean
        get() = cameraLinkProfile.shouldStartCameraStream
}

object RcVehicleProGlass {
    const val SURFACE_ALPHA = 0.38f
    const val TOP_BAR_ALPHA = 0.32f
    const val JOYSTICK_ZONE_ALPHA = 0.30f
    const val ACTION_CHIP_ALPHA = 0.45f
    const val STOP_BUTTON_ALPHA = 0.72f
}

object RcVehicleProLayout {
    const val CAMERA_PAN_FRONT_RAW = RcCameraPan.FRONT_RAW
    const val CAMERA_PAN_CENTER = RcCameraPan.FRONT_NORMALIZED

    fun cameraPanRaw(normalized: Float): Int = RcCameraPan.rawFromNormalized(normalized)

    val JoystickSize = 240.dp
    val JoystickSizeCompact = 200.dp
    val ControlZoneKnobSize = 92.dp
    val ControlZoneContentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    /** Title row + spacers above the stick inside [RcControlZone]. */
    val ControlZoneChromeHeight = 22.dp
    /** Ignored stick travel around center; applied on throttle and steering. */
    const val StickDeadzone = RcStickDeadzone.DEFAULT
}
