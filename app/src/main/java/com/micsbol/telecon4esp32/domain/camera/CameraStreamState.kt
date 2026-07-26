package com.micsbol.telecon4esp32.domain.camera

import android.graphics.Bitmap

sealed interface CameraStreamState {
    data object Idle : CameraStreamState
    data object Connecting : CameraStreamState
    data class Frame(val bitmap: Bitmap) : CameraStreamState
    data class Error(val message: String) : CameraStreamState
}

object Esp32CameraDefaults {
    /** Default ESP32-CAM soft-AP address when the module hosts its own network. */
    const val DEFAULT_BASE_URL = "http://192.168.4.1"

    const val DEFAULT_SOFTAP_HOST = "192.168.4.1"
    const val DEFAULT_CONTROL_PORT = 3333
    const val SOFTAP_SSID = "TeleCon-RC-CAM"
    const val SOFTAP_PASSWORD = "telecon1234"

    const val CAPTURE_PATH = "/capture"
    const val STREAM_PATH = "/stream"
    const val STATUS_PATH = "/status"

    fun captureUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$CAPTURE_PATH"

    /** Continuous multipart MJPEG (preferred for RC Vehicle Pro HUD). */
    fun streamUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$STREAM_PATH"

    fun statusUrl(baseUrl: String = DEFAULT_BASE_URL): String =
        "${baseUrl.trim().trimEnd('/')}$STATUS_PATH"

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
