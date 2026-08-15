package com.micsbol.telecon4esp32.domain.camera

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
            SMOOTH -> 10
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
     * Smooth cuts SoftAP airtime; Balanced / High keep VGA with quality tweaks.
     */
    val camConfigQuery: String
        get() = when (this) {
            SMOOTH -> "framesize=qvga&quality=22&fps=10"
            BALANCED -> "framesize=vga&quality=15&fps=0"
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

        fun fromStored(value: String?): SoftApPerformancePreset =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}

/**
 * SoftAP HUD decode / publish options applied by [CameraStreamRepository].
 * [FULL_QUALITY] is the Greenhouse default (no downsample, no FPS cap).
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
