package com.micsbol.telecon4esp32.ui.greenhouse

import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState

data class GreenhouseCameraUiState(
    val cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    val cameraState: CameraStreamState = CameraStreamState.Idle,
    val isCameraOnline: Boolean = false,
    val isEmulatorPreview: Boolean = false,
    val isBluetoothOnline: Boolean = false,
    val temperatureC: Float = 26.2f,
    val humidityPercent: Int = 68,
    val vpdKpa: Float = 1.1f,
    val soilPercent: Int = 42,
    val tankPercent: Int = 78,
    val isAutoMode: Boolean = true,
    val isStable: Boolean = true,
    val fanOn: Boolean = false,
    val lightsOn: Boolean = false,
    /** Camera pan 0–100 (50 = center). Sent as `cam_pan` in GH:SET. */
    val camPanPercent: Int = 50,
    /** Camera tilt 0–100 (50 = center). Sent as `cam_tilt` in GH:SET. */
    val camTiltPercent: Int = 50,
)
