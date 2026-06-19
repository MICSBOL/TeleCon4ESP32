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
}
