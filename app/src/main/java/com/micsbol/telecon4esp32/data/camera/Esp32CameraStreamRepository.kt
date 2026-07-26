package com.micsbol.telecon4esp32.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Live ESP32-CAM SoftAP video.
 *
 * Prefers continuous MJPEG at [Esp32CameraDefaults.STREAM_PATH] (lowest latency).
 * Falls back to polling [Esp32CameraDefaults.CAPTURE_PATH] with no artificial FPS cap.
 */
@Singleton
class Esp32CameraStreamRepository @Inject constructor() : CameraStreamRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _streamState = MutableStateFlow<CameraStreamState>(CameraStreamState.Idle)
    override val streamState: StateFlow<CameraStreamState> = _streamState.asStateFlow()

    private var streamJob: Job? = null
    private var activeBitmap: Bitmap? = null
    /** Previous frame kept one cycle so Compose can finish drawing before recycle. */
    private var pendingRecycle: Bitmap? = null

    override fun startStream(baseUrl: String) {
        stopStream()
        _streamState.value = CameraStreamState.Connecting
        val normalizedBase = baseUrl.trim().trimEnd('/')
        streamJob = scope.launch {
            while (isActive) {
                val streamed = try {
                    consumeMjpegStream(normalizedBase)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "MJPEG /stream failed, falling back to /capture", e)
                    false
                }
                if (!isActive) break
                if (streamed) {
                    // Stream ended (disconnect) — reconnect quickly.
                    delay(ERROR_RETRY_MS)
                    continue
                }
                pollCaptureFrames(normalizedBase)
            }
        }
    }

    override fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        recycleBitmaps()
        _streamState.value = CameraStreamState.Idle
    }

    /**
     * @return true if the HTTP MJPEG connection was established (even if it later ended).
     */
    private suspend fun consumeMjpegStream(baseUrl: String): Boolean {
        val connection = openGetConnection(Esp32CameraDefaults.streamUrl(baseUrl)).apply {
            // Long-lived stream — generous read timeout between frames.
            readTimeout = STREAM_READ_TIMEOUT_MS
        }
        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "Stream HTTP $status from ${Esp32CameraDefaults.streamUrl(baseUrl)}")
            connection.disconnect()
            return false
        }
        val contentType = connection.contentType?.lowercase().orEmpty()
        if (!contentType.contains("multipart") && !contentType.contains("mjpeg")) {
            // Some firmwares omit multipart in Content-Type; still try JPEG SOI scanning.
            Log.d(TAG, "Stream Content-Type=$contentType — scanning JPEG markers")
        }
        Log.d(TAG, "MJPEG /stream connected — ${Esp32CameraDefaults.streamUrl(baseUrl)}")
        try {
            BufferedInputStream(connection.inputStream, INPUT_BUFFER_BYTES).use { input ->
                while (coroutineContext.isActive) {
                    val jpeg = readNextJpeg(input) ?: break
                    val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                    if (decoded != null) {
                        replaceFrame(decoded)
                    }
                    yield()
                }
            }
        } finally {
            connection.disconnect()
        }
        return true
    }

    private suspend fun pollCaptureFrames(baseUrl: String) {
        Log.d(TAG, "Using /capture poll fallback (no delay between successful frames)")
        while (coroutineContext.isActive) {
            try {
                val connection = openGetConnection(Esp32CameraDefaults.captureUrl(baseUrl))
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Capture HTTP $status from ${Esp32CameraDefaults.captureUrl(baseUrl)}")
                    connection.disconnect()
                    _streamState.value = CameraStreamState.Error("HTTP $status")
                    delay(ERROR_RETRY_MS)
                    // Offer MJPEG again after a capture failure streak.
                    return
                }
                connection.inputStream.use { input ->
                    val bytes = input.readBytes()
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (decoded != null) {
                        replaceFrame(decoded)
                    } else {
                        Log.w(TAG, "Capture decode failed (${bytes.size} bytes)")
                        _streamState.value = CameraStreamState.Error("Invalid camera frame")
                    }
                }
                connection.disconnect()
                // No artificial FPS cap — SoftAP + ESP32 capture set the pace.
                yield()
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                Log.w(TAG, "Capture failed", e)
                _streamState.value = CameraStreamState.Error(
                    e.message ?: "Camera connection failed",
                )
                delay(ERROR_RETRY_MS)
                return
            }
        }
    }

    private fun openGetConnection(urlString: String): HttpURLConnection {
        val url = URL(urlString)
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            doInput = true
            useCaches = false
            instanceFollowRedirects = false
        }
    }

    /**
     * Reads the next JPEG from a multipart MJPEG (or raw concatenated JPEGs) by SOI/EOI markers.
     */
    private fun readNextJpeg(input: InputStream): ByteArray? {
        // Seek SOI 0xFF 0xD8
        var prev = -1
        while (true) {
            val b = input.read()
            if (b < 0) return null
            if (prev == 0xFF && b == 0xD8) break
            prev = b
        }
        val out = ByteArrayOutputStream(JPEG_HINT_BYTES)
        out.write(0xFF)
        out.write(0xD8)
        prev = 0xD8
        while (true) {
            val b = input.read()
            if (b < 0) return null
            out.write(b)
            if (prev == 0xFF && b == 0xD9) {
                return out.toByteArray()
            }
            prev = b
            if (out.size() > MAX_JPEG_BYTES) {
                Log.w(TAG, "JPEG frame exceeded $MAX_JPEG_BYTES bytes — resync")
                return null
            }
        }
    }

    private fun replaceFrame(bitmap: Bitmap) {
        pendingRecycle?.recycle()
        pendingRecycle = activeBitmap
        activeBitmap = bitmap
        _streamState.value = CameraStreamState.Frame(bitmap)
    }

    private fun recycleBitmaps() {
        pendingRecycle?.recycle()
        pendingRecycle = null
        activeBitmap?.recycle()
        activeBitmap = null
    }

    private companion object {
        private const val TAG = "Esp32CameraStream"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val STREAM_READ_TIMEOUT_MS = 15_000
        private const val ERROR_RETRY_MS = 250L
        private const val INPUT_BUFFER_BYTES = 64 * 1024
        private const val JPEG_HINT_BYTES = 32 * 1024
        private const val MAX_JPEG_BYTES = 512 * 1024
    }
}
