package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.model.RcCameraPan
import kotlin.math.abs

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
    /** Card padding and mode-bar slot around the graph. */
    val TelemetryPlotPanelExtraHeight = 40.dp
    /** Air between the plot card and the stick work area when both pads are open. */
    val TelemetryPlotStickGap = 12.dp
    /** Graph height cap when both sticks are open so the card is not full-HUD. */
    val TelemetryPlotBothSticksMaxHeight = 160.dp
    const val TELEMETRY_PLOT_SAMPLE_COUNT = 72
    const val TELEMETRY_PLOT_INTERVAL_MS = 80L
    /** Matches HUD speed gauge mapping of full throttle. */
    const val HUD_MAX_SPEED_KMH = 40f
    const val TELEMETRY_PLOT_INTERVAL_SEC = TELEMETRY_PLOT_INTERVAL_MS / 1000f

    fun commandedSpeedKmh(throttleY: Float): Float =
        abs(throttleY).coerceIn(0f, 1f) * HUD_MAX_SPEED_KMH

    fun meanOrZero(samples: List<Float>): Float =
        if (samples.isEmpty()) 0f else samples.sum() / samples.size

    fun accelSamples(speeds: List<Float>, intervalSec: Float = TELEMETRY_PLOT_INTERVAL_SEC): List<Float> {
        if (speeds.isEmpty()) return emptyList()
        val dt = intervalSec.coerceAtLeast(0.001f)
        return List(speeds.size) { index ->
            if (index == 0) 0f else (speeds[index] - speeds[index - 1]) / dt
        }
    }

    fun distanceKm(speedsKmh: List<Float>, intervalSec: Float = TELEMETRY_PLOT_INTERVAL_SEC): Float {
        if (speedsKmh.isEmpty()) return 0f
        return speedsKmh.sumOf { abs(it).toDouble() }.toFloat() * (intervalSec / 3600f)
    }

    fun hasPlotTelemetry(series: List<PlotData>): Boolean =
        series.any { it.dataPoints.isNotEmpty() }

    fun elapsedMinutes(seconds: Float): Int = (seconds.coerceAtLeast(0f) / 60f).toInt()

    fun elapsedSecondsPart(seconds: Float): Int =
        seconds.coerceAtLeast(0f).toInt() % 60

    /** Smallest width typical of Plus / XL phones (portrait dp). */
    const val PLUS_SIZE_SMALLEST_WIDTH_DP = 400
    /** xxhdpi and above — sharp enough for a taller immersive pad. */
    const val HIGH_PIXEL_DENSITY = 2.5f
    /**
     * Android 12 media performance class. S23 Ultra and similar flagships report 33+.
     * Matches [com.micsbol.telecon4esp32.domain.camera.assessDeviceCameraStreamRisk].
     */
    const val MIN_FLAGSHIP_MEDIA_PERFORMANCE_CLASS = 31
    /** 8 GB-class phones report ~7 GB+; 6 GB is the floor used for camera flagships. */
    const val MIN_FLAGSHIP_RAM_MB = 6 * 1024L
    /** Stick glass card height when HUD chrome is hidden on a flagship display. */
    const val IMMERSIVE_STICK_HEIGHT_FRACTION = 2f / 3f

    /**
     * Flagships like S23 Ultra often stay at sw 360 in FHD+; qualify by performance
     * class or RAM, or by Plus-size width. Density must still be xxhdpi+.
     */
    fun usesImmersiveStickHeight(
        density: Float,
        smallestWidthDp: Int,
        mediaPerformanceClass: Int = 0,
        totalRamMb: Long = 0,
    ): Boolean {
        if (density < HIGH_PIXEL_DENSITY) return false
        return mediaPerformanceClass >= MIN_FLAGSHIP_MEDIA_PERFORMANCE_CLASS ||
            totalRamMb >= MIN_FLAGSHIP_RAM_MB ||
            smallestWidthDp >= PLUS_SIZE_SMALLEST_WIDTH_DP
    }

    /**
     * Visible sticks (one or both) are not capped at two-thirds of the screen;
     * they use leftover HUD height instead.
     */
    fun immersiveStickContainerHeight(
        screenHeight: Dp,
        slotHeight: Dp,
        immersiveDisplay: Boolean,
        restHudHidden: Boolean,
        anyStickVisible: Boolean,
    ): Dp? {
        if (anyStickVisible) return null
        if (!immersiveDisplay || !restHudHidden) return null
        return (screenHeight * IMMERSIVE_STICK_HEIGHT_FRACTION)
            .coerceAtMost(slotHeight)
            .coerceAtLeast(1.dp)
    }

    fun telemetryPlotSize(
        availableWidth: Dp,
        availableHeight: Dp,
    ): Pair<Dp, Dp> {
        val maxW = availableWidth.coerceAtLeast(1.dp)
        val maxH = availableHeight.coerceAtLeast(1.dp)
        val aspect = TelemetryPlotWidth.value / TelemetryPlotHeight.value
        var width = minOf(TelemetryPlotWidth, maxW)
        var height = width / aspect
        if (height > maxH) {
            height = maxH
            width = height * aspect
        }
        return width to height
    }

    /**
     * Fill the gap between the two stick pads when both are open. Width is the
     * HUD minus each pad and a small gap; height stays compact so the camera
     * and stick chrome remain visible.
     */
    fun telemetryPlotSizeBetweenSticks(
        hudWidth: Dp,
        hudHeight: Dp,
        stickPadSize: Dp,
        gap: Dp = TelemetryPlotStickGap,
    ): Pair<Dp, Dp> {
        val width = (hudWidth - stickPadSize * 2 - gap * 2).coerceAtLeast(1.dp)
        val height = minOf(
            TelemetryPlotBothSticksMaxHeight,
            hudHeight - TelemetryPlotPanelExtraHeight,
        ).coerceAtLeast(1.dp)
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

    const val STICK_RESIZE_HOLD_MS = 2000L
    const val STICK_GROUP_SCALE_MIN = 0.50f
    /** Default drag ceiling; camera pan can go higher until 80% screen height. */
    const val STICK_GROUP_SCALE_MAX = 1.50f
    const val HUD_SCALE_STORE_MAX = 8f
    /** Largest allowed height for a resizable HUD card, as a fraction of screen height. */
    const val RESIZE_MAX_HEIGHT_FRACTION = 0.80f

    fun maxResizeHeight(screenHeight: Dp): Dp =
        (screenHeight * RESIZE_MAX_HEIGHT_FRACTION).coerceAtLeast(1.dp)

    fun maxScaleForBaseHeight(baseHeight: Dp, maxHeight: Dp): Float {
        if (baseHeight <= 0.dp) return STICK_GROUP_SCALE_MAX
        return (maxHeight / baseHeight).coerceIn(STICK_GROUP_SCALE_MIN, HUD_SCALE_STORE_MAX)
    }

    fun coerceStickGroupScale(
        scale: Float,
        maxScale: Float = STICK_GROUP_SCALE_MAX,
    ): Float = scale.coerceIn(STICK_GROUP_SCALE_MIN, maxScale.coerceAtLeast(STICK_GROUP_SCALE_MIN))

    /**
     * Uniform scale from a horizontal drag so width and height change together
     * and the card keeps its shape. Drag left to grow; drag right to shrink.
     */
    fun stickGroupScaleFromDrag(
        startScale: Float,
        startXPx: Float,
        currentXPx: Float,
        containerWidthPx: Float,
        maxScale: Float = STICK_GROUP_SCALE_MAX,
    ): Float {
        val span = containerWidthPx.coerceAtLeast(1f)
        val dx = currentXPx - startXPx
        return coerceStickGroupScale(
            scale = startScale * (span - dx) / span,
            maxScale = maxScale,
        )
    }

    /**
     * Lights / photo / record fill the center column only when both sticks are
     * open. One visible stick must not stretch that cluster across leftover HUD.
     */
    fun centerControlsFillWidth(
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
    ): Boolean = leftStickExpanded && rightStickExpanded

    /**
     * Park the action cluster on the side opposite a single visible stick so a
     * resized pad does not cover Lights / Photo / Record.
     */
    fun centerControlsOverlayAlignment(
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
    ): Alignment = when {
        leftStickExpanded && !rightStickExpanded -> Alignment.BottomEnd
        rightStickExpanded && !leftStickExpanded -> Alignment.BottomStart
        else -> Alignment.BottomCenter
    }

    fun centerControlsOverlayPadding(
        leftStickExpanded: Boolean,
        rightStickExpanded: Boolean,
    ): PaddingValues = when {
        leftStickExpanded && !rightStickExpanded -> PaddingValues(end = HudCollapsedPeek)
        rightStickExpanded && !leftStickExpanded -> PaddingValues(start = HudCollapsedPeek)
        else -> PaddingValues()
    }

    /** Same column width for both sticks so they stay equal while chrome changes. */
    fun sharedStickContainerWidth(
        slotWidth: Dp,
    ): Dp {
        return (slotWidth - HudCollapsedPeek - 8.dp).coerceAtLeast(1.dp) / 2
    }

    fun stickContainerBaseHeight(
        slotHeight: Dp,
        joystickSize: Dp,
        immersiveHeight: Dp?,
    ): Dp = (immersiveHeight ?: (ControlZoneChromeHeight + joystickSize + ControlZoneFooterHeight))
        .coerceAtMost(slotHeight)
        .coerceAtLeast(1.dp)

    fun scaledStickContainerSize(
        baseWidth: Dp,
        baseHeight: Dp,
        scale: Float,
        maxWidth: Dp = 10_000.dp,
        maxHeight: Dp = 10_000.dp,
    ): Pair<Dp, Dp> {
        val s = coerceStickGroupScale(scale, HUD_SCALE_STORE_MAX)
        var width = (baseWidth * s).coerceAtLeast(1.dp)
        var height = (baseHeight * s).coerceAtLeast(1.dp)
        if (height > maxHeight && height > 0.dp) {
            val fit = maxHeight / height
            width *= fit
            height = maxHeight
        }
        if (width > maxWidth && width > 0.dp) {
            val fit = maxWidth / width
            height *= fit
            width = maxWidth
        }
        return width.coerceAtLeast(1.dp) to height.coerceAtLeast(1.dp)
    }
}
