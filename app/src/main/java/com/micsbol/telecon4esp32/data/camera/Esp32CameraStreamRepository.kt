package com.micsbol.telecon4esp32.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Esp32CameraStreamRepository @Inject constructor() : CameraStreamRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _streamState = MutableStateFlow<CameraStreamState>(CameraStreamState.Idle)
    override val streamState: StateFlow<CameraStreamState> = _streamState.asStateFlow()

    private var streamJob: Job? = null
    private var activeBitmap: Bitmap? = null

    override fun startStream(baseUrl: String) {
        stopStream()
        _streamState.value = CameraStreamState.Connecting
        val normalizedBase = baseUrl.trim().trimEnd('/')
        streamJob = scope.launch {
            while (isActive) {
                try {
                    val connection = openCaptureConnection(normalizedBase)
                    connection.inputStream.use { input ->
                        val bytes = input.readBytes()
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (decoded != null) {
                            replaceFrame(decoded)
                        } else {
                            _streamState.value = CameraStreamState.Error("Invalid camera frame")
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Capture failed", e)
                    _streamState.value = CameraStreamState.Error(
                        e.message ?: "Camera connection failed",
                    )
                }
                delay(FRAME_POLL_INTERVAL_MS)
            }
        }
    }

    override fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        activeBitmap?.recycle()
        activeBitmap = null
        _streamState.value = CameraStreamState.Idle
    }

    private fun openCaptureConnection(baseUrl: String): HttpURLConnection {
        val url = URL("$baseUrl/capture")
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            doInput = true
        }
    }

    private fun replaceFrame(bitmap: Bitmap) {
        activeBitmap?.recycle()
        activeBitmap = bitmap
        _streamState.value = CameraStreamState.Frame(bitmap)
    }

    private companion object {
        private const val TAG = "Esp32CameraStream"
        const val FRAME_POLL_INTERVAL_MS = 150L
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 5_000
    }
}
