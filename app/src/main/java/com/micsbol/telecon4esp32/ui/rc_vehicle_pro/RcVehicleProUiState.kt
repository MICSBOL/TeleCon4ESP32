package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState

data class RcVehicleProUiState(
    val cameraState: CameraStreamState = CameraStreamState.Idle,
    val isBluetoothConnected: Boolean = false,
    val isCameraOnline: Boolean = false,
    val speedKmh: Float = 0f,
    val batteryPercent: Int = 0,
    val motorTempCelsius: Int = 0,
    val isRecording: Boolean = false,
    val lightsOn: Boolean = false,
)

object RcVehicleProGlass {
    const val SURFACE_ALPHA = 0.38f
    const val TOP_BAR_ALPHA = 0.32f
    const val JOYSTICK_ZONE_ALPHA = 0.30f
    const val ACTION_CHIP_ALPHA = 0.45f
    const val STOP_BUTTON_ALPHA = 0.72f
}

object RcVehicleProLayout {
    const val CAMERA_PAN_CENTER = 0.5f

    val JoystickSize = 158.dp
    val ControlZoneKnobSize = 92.dp
    val ControlZoneContentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
}
