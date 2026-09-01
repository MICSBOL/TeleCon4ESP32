package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.model.ApplicationId

data class SoftApStreamQualityDefaults(
    val preset: SoftApPerformancePreset,
    val hudRate: SoftApHudProcessingRate,
)

/** Presets used when the phone is flagged [DeviceCameraStreamRisk.AT_RISK]. */
fun recommendedSoftApStreamQualityForAtRiskDevice(): SoftApStreamQualityDefaults =
    SoftApStreamQualityDefaults(
        preset = SoftApPerformancePreset.SMOOTH,
        hudRate = SoftApHudProcessingRate.FPS_8,
    )

/**
 * Poll `/capture` instead of long-lived `/stream` so TCP cannot queue old JPEGs.
 * Smooth and at-risk phones prefer latest-frame latency over MJPEG fps.
 */
fun shouldPreferCapturePollingForLowLatency(
    preset: SoftApPerformancePreset,
    atRisk: Boolean,
    streaming: Boolean,
): Boolean = streaming && (atRisk || preset == SoftApPerformancePreset.SMOOTH)

fun resolveSoftApHudPreviewOptions(
    preset: SoftApPerformancePreset,
    hudRate: SoftApHudProcessingRate,
    streaming: Boolean,
): HudPreviewOptions = if (streaming) {
    preset.toHudPreviewOptions().copy(
        maxHudFps = hudRate.resolveMaxHudFps(preset),
    )
} else {
    HudPreviewOptions.FULL_QUALITY
}

/** Apps that expose runtime SoftAP stream-quality on the live camera surface. */
fun ApplicationId.supportsRuntimeSoftApStreamQuality(): Boolean = when (this) {
    ApplicationId.RC_VEHICLE_PRO,
    ApplicationId.CONTROL_PANEL,
    -> true
    else -> false
}
