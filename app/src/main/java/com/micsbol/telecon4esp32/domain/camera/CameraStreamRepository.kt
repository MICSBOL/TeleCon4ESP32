package com.micsbol.telecon4esp32.domain.camera

import kotlinx.coroutines.flow.StateFlow

interface CameraStreamRepository {
    val streamState: StateFlow<CameraStreamState>

    fun startStream(baseUrl: String)

    fun stopStream()
}
