package com.micsbol.telecon4esp32.domain.camera

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

interface CameraStreamRepository {
    val streamState: StateFlow<CameraStreamState>

    fun startStream(baseUrl: String)

    fun stopStream()

    /**
     * When true, skip long-lived MJPEG `/stream` and poll `/capture` only
     * (Smooth / at-risk SoftAP low-latency path).
     * Default no-op for fakes / non-SoftAP implementations.
     */
    fun setPreferCapturePolling(prefer: Boolean) = Unit

    /**
     * SoftAP HUD decode / max publish FPS. Pass [HudPreviewOptions.FULL_QUALITY]
     * (unthrottled default) or a [SoftApPerformancePreset] mapping.
     */
    fun setHudPreviewOptions(options: HudPreviewOptions) = Unit

    /**
     * SoftAP HUD preview: cheaper BitmapFactory decode (downsample / RGB_565).
     * Maps to Balanced preset options when enabled.
     */
    fun setFastPreviewDecode(enabled: Boolean) {
        setHudPreviewOptions(
            if (enabled) {
                SoftApPerformancePreset.BALANCED.toHudPreviewOptions()
            } else {
                HudPreviewOptions.FULL_QUALITY
            },
        )
    }

    /**
     * Full-quality still from the last JPEG bytes (ignores fast-preview options).
     * Null when no frame has been received yet.
     */
    fun captureStillBitmap(): Bitmap? = null
}
