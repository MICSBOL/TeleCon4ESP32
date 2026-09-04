package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.model.ApplicationId

/**
 * SoftAP HTTP video load presets for mid-range vs flagship phones
 * (Kit A SoftAP and Kit B SoftAP camera + DevKit BLE).
 *
 * Phase 1 (Android): HUD decode sample size, max publish FPS;
 * SoftAP CTRL period applies to Kit A TCP only.
 * Phase 2 (firmware `/camconfig`): framesize / JPEG quality / stream FPS on-air
 * when the CAM sketch supports it (404 → Phase 1 only).
 */
enum class SoftApPerformancePreset {
    /** Mid-range (e.g. Poco X3) — lower HUD + CTRL load; softer image. */
    SMOOTH,

    /** Default — balanced SoftAP decode and CTRL heartbeat. */
    BALANCED,

    /** Flagship — full HUD decode, uncapped publish FPS. */
    HIGH_QUALITY;

    val hudInSampleSize: Int
        get() = when (this) {
            SMOOTH -> 4
            BALANCED -> 2
            HIGH_QUALITY -> 1
        }

    val useRgb565: Boolean
        get() = this != HIGH_QUALITY

    /** Null = no hard cap beyond the SoftAP publish floor. */
        val maxHudFps: Int?
        get() = when (this) {
            SMOOTH -> 8
            BALANCED -> 15
            HIGH_QUALITY -> null
        }

    /** SoftAP CTRL heartbeat; must stay well under ESP32 TELECON_CTRL_TIMEOUT_MS (~750). */
    val softApCtrlPeriodMs: Long
        get() = when (this) {
            SMOOTH -> 150L
            BALANCED, HIGH_QUALITY -> 100L
        }

    /**
     * Phase 2 firmware `/camconfig` query (no leading `?`).
     * Smooth cuts SoftAP airtime; Balanced paces VGA so one-board TCP RC can breathe.
     */
    val camConfigQuery: String
        get() = when (this) {
            SMOOTH -> "framesize=qqvga&quality=28&fps=8&ampdu_rx=0"
            BALANCED -> "framesize=vga&quality=15&fps=12"
            HIGH_QUALITY -> "framesize=vga&quality=12&fps=0"
        }

    /** True when firmware framesize may change vs Balanced default (QVGA). */
    val mayChangeFramesize: Boolean
        get() = this == SMOOTH

    fun toHudPreviewOptions(): HudPreviewOptions = HudPreviewOptions(
        inSampleSize = hudInSampleSize,
        useRgb565 = useRgb565,
        maxHudFps = maxHudFps,
    )

    companion object {
        val DEFAULT = BALANCED

        /** UI / fail-safe ceiling — never approach the ESP32 CTRL timeout. */
        const val MAX_CTRL_PERIOD_MS = 200L

        /** RC Vehicle one-board SoftAP defaults to Smooth; Control Panel stays Balanced. */
        fun defaultFor(applicationId: ApplicationId): SoftApPerformancePreset =
            if (applicationId == ApplicationId.RC_VEHICLE_PRO) SMOOTH else DEFAULT

        fun fromStored(
            value: String?,
            default: SoftApPerformancePreset = DEFAULT,
        ): SoftApPerformancePreset =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: default
    }
}

/**
 * SoftAP HUD decode / publish options applied by [CameraStreamRepository].
 * [FULL_QUALITY] is the unthrottled camera default (no downsample, no FPS cap).
 */
data class HudPreviewOptions(
    val inSampleSize: Int = 1,
    val useRgb565: Boolean = false,
    val maxHudFps: Int? = null,
) {
    fun minPublishGapMs(floorMs: Long = 16L): Long {
        val fromFps = maxHudFps?.takeIf { it > 0 }?.let { 1_000L / it } ?: 0L
        return maxOf(floorMs, fromFps)
    }

    val isFullQuality: Boolean
        get() = inSampleSize <= 1 && !useRgb565 && maxHudFps == null

    companion object {
        val FULL_QUALITY = HudPreviewOptions()
    }
}
