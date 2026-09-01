package com.micsbol.telecon4esp32.domain.camera

import android.graphics.Bitmap

sealed interface CameraStreamState {
    data object Idle : CameraStreamState
    data object Connecting : CameraStreamState
    data class Frame(val bitmap: Bitmap) : CameraStreamState
    data class Error(val message: String) : CameraStreamState
}

/**
 * SoftAP HTTP defaults shared by Kit A (CAM SoftAP video + TCP) and the optional
 * CAM video overlay (video-only SoftAP + DevKit Bluetooth). Overlay video uses the
 * starter SSID; Kit A Advanced uses [SOFTAP_SSID].
 */
object Esp32CameraDefaults {
    /** Default ESP32-CAM soft-AP address when the module hosts its own network. */
    const val DEFAULT_BASE_URL = "http://192.168.4.1"

    const val DEFAULT_SOFTAP_HOST = "192.168.4.1"
    /** Kit A SoftAP TCP control port; unused when Bluetooth is the control link. */
    const val DEFAULT_CONTROL_PORT = 3333
    /** Advanced Kit A SoftAP SSID (`proto=wifi`). */
    const val SOFTAP_SSID = "TeleCon-RC-CAM"
    /** Normal-user CAM starter SoftAP SSID (`proto=simple`). */
    const val STARTER_SOFTAP_SSID = "TeleCon-RC-CAM-Starter"
    const val SOFTAP_PASSWORD = "telecon1234"

    const val CAPTURE_PATH = "/capture"
    const val STREAM_PATH = "/stream"
    const val STATUS_PATH = "/status"
    const val CAMCONFIG_PATH = "/camconfig"

    fun captureUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$CAPTURE_PATH"

    /** Continuous multipart MJPEG (preferred for RC Vehicle Pro HUD). */
    fun streamUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$STREAM_PATH"

    fun statusUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$STATUS_PATH"

    /** SoftAP Phase 2 camera params (`framesize` / `quality` / `fps`). */
    fun camConfigUrl(
        baseUrl: String = DEFAULT_BASE_URL,
        query: String,
    ): String {
        val q = query.trim().removePrefix("?")
        val root = "${baseUrl.trim().trimEnd('/')}$CAMCONFIG_PATH"
        return if (q.isEmpty()) root else "$root?$q"
    }

    fun softApHostFromBaseUrl(baseUrl: String = DEFAULT_BASE_URL): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return trimmed
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore(':')
            .ifBlank { DEFAULT_SOFTAP_HOST }
    }
}
