package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
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
    /** SoftAP HTTP video armed for Kit A / DevKit Bluetooth + overlay whenever the profile expects a camera. */
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

    val isBluetoothControlWithCamera: Boolean
        get() = cameraLinkProfile == CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT

    val expectsCameraStream: Boolean
        get() = cameraLinkProfile.shouldStartCameraStream
}

object RcVehicleProGlass {
    const val SURFACE_ALPHA = 0.38f
    const val PLOT_BACKGROUND_ALPHA = 0.50f
    const val TOP_BAR_ALPHA = 0.32f
    const val JOYSTICK_ZONE_ALPHA = 0.30f
    const val ACTION_CHIP_ALPHA = 0.45f
    const val MENU_SURFACE_ALPHA = 0.78f
    const val STOP_BUTTON_ALPHA = 0.72f
}

object RcVehicleProLayout {
    const val CAMERA_PAN_FRONT_RAW = RcCameraPan.FRONT_RAW
    const val CAMERA_PAN_CENTER = RcCameraPan.FRONT_NORMALIZED

    fun cameraPanRaw(normalized: Float): Int = RcCameraPan.rawFromNormalized(normalized)

    val JoystickSize = 320.dp
    val JoystickSizeCompact = 200.dp
    val ControlZoneKnobSize = 92.dp
    val ControlZoneContentPadding = PaddingValues(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 0.dp)
    /** Title row + spacers above the stick inside [RcControlZone]. */
    val ControlZoneChromeHeight = 22.dp
    /** Stop / buzzer row below the stick so those buttons never cover the pad. */
    val ControlZoneFooterHeight = 46.dp
    val HudCollapsedPeek = 32.dp
    val HudEdgeChevronWidth = 30.dp
    val TelemetryPlotWidth = 340.dp
    val TelemetryPlotHeight = 250.dp

    fun telemetryPlotSize(
        availableWidth: Dp,
        availableHeight: Dp,
    ): Pair<Dp, Dp> {
        val maxW = availableWidth.coerceAtLeast(96.dp)
        val maxH = availableHeight.coerceAtLeast(72.dp)
        val aspect = TelemetryPlotWidth.value / TelemetryPlotHeight.value
        var width = minOf(TelemetryPlotWidth, maxW)
        var height = width / aspect
        if (height > maxH) {
            height = maxH
            width = height * aspect
        }
        return width to height
    }
    /** Ignored stick travel around center; applied on throttle and steering. */
    const val StickDeadzone = RcStickDeadzone.DEFAULT

    fun joystickPadSize(
        slotWidth: Dp,
        slotHeight: Dp,
        centerExpanded: Boolean,
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
        fillLeftover: Boolean,
    ): Dp {
        val availableH = (slotHeight - ControlZoneChromeHeight - ControlZoneFooterHeight)
            .coerceAtLeast(1.dp)
        val expandedSticks = listOf(leftStickExpanded, rightStickExpanded).count { it }
        val centerW = when {
            !centerExpanded -> HudCollapsedPeek
            expandedSticks == 1 -> slotWidth / 2f
            else -> slotWidth / 3f
        }
        val collapsedSticks = 2 - expandedSticks
        val leftover = (slotWidth - centerW - 8.dp - HudCollapsedPeek * collapsedSticks)
            .coerceAtLeast(1.dp)
        val slotW = if (expandedSticks == 1) {
            slotWidth / 2f
        } else {
            leftover / expandedSticks.coerceAtLeast(1)
        }
        val availableW = (slotW - HudEdgeChevronWidth).coerceAtLeast(1.dp)
        val cap = if (fillLeftover) 480.dp else JoystickSize
        return minOf(cap, availableH, availableW).coerceAtLeast(1.dp)
    }

    fun leftStickColumnWidth(
        slotWidth: Dp,
        centerExpanded: Boolean,
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
    ): Dp = stickColumnWidth(
        slotWidth = slotWidth,
        centerExpanded = centerExpanded,
        leftStickExpanded = leftStickExpanded,
        rightStickExpanded = rightStickExpanded,
        thisStickExpanded = leftStickExpanded,
    )

    fun rightStickColumnWidth(
        slotWidth: Dp,
        centerExpanded: Boolean,
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
    ): Dp = stickColumnWidth(
        slotWidth = slotWidth,
        centerExpanded = centerExpanded,
        leftStickExpanded = leftStickExpanded,
        rightStickExpanded = rightStickExpanded,
        thisStickExpanded = rightStickExpanded,
    )

    private fun stickColumnWidth(
        slotWidth: Dp,
        centerExpanded: Boolean,
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
        thisStickExpanded: Boolean,
    ): Dp {
        if (!thisStickExpanded) return 0.dp
        val expandedSticks = listOf(leftStickExpanded, rightStickExpanded).count { it }
        if (expandedSticks == 1) return slotWidth / 2f
        val centerW = if (centerExpanded) slotWidth / 3f else HudCollapsedPeek
        return (slotWidth - centerW - 8.dp).coerceAtLeast(1.dp) / 2
    }

    val HudChromeSwipeThreshold = 36.dp
    const val HUD_CHROME_FLING_VELOCITY = 800f
    const val HUD_CHROME_ANIM_MS = 220
    const val HUD_CHROME_FADE_MS = 180
}
