package com.micsbol.telecon4esp32.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.micsbol.telecon4esp32.domain.camera.CameraStreamRepository
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.camera.HudPreviewOptions
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Live ESP32-CAM SoftAP video (Kit A and Kit B).
 *
 * Prefers continuous MJPEG at [Esp32CameraDefaults.STREAM_PATH], then falls back to
 * [Esp32CameraDefaults.CAPTURE_PATH]. Kit B uses a video-only CAM + separate DevKit
 * BLE, so `/stream` stays preferred while BLE is connected.
 *
 * [setPreferCapturePolling] is used for Smooth / at-risk SoftAP so the HUD
 * reads `/capture` (latest still) instead of a queued MJPEG `/stream`.
 *
 * [setHudPreviewOptions] opts RC Vehicle Pro SoftAP HUD into cheaper decode and
 * optional max publish FPS. Full-quality preview leaves [HudPreviewOptions.FULL_QUALITY].
 */
@Singleton
class Esp32CameraStreamRepository @Inject constructor(
    private val softApNetworkResolver: SoftApNetworkResolver,
    private val softApWifiLock: SoftApWifiLock,
) : CameraStreamRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _streamState = MutableStateFlow<CameraStreamState>(CameraStreamState.Idle)
    override val streamState: StateFlow<CameraStreamState> = _streamState.asStateFlow()

    private var streamJob: Job? = null
    private var activeBitmap: Bitmap? = null
    private var activeBaseUrl: String? = null
    private val preferCaptureOnly = AtomicBoolean(false)
    private val hudPreviewOptions = AtomicReference(HudPreviewOptions.FULL_QUALITY)
    /** Last JPEG for full-quality stills (photo) while HUD may use downsampled decode. */
    private val lastJpegBytes = AtomicReference<ByteArray?>(null)
    private val jpegEpoch = AtomicLong(0L)
    private var loggedDecodeOptions = false
    private var wifiLockHeld = false

    /** Reused on the single stream Job — avoid per-frame scan / assemble allocations. */
    private val jpegScanBuffer = ByteArray(SCAN_CHUNK_BYTES)
    private val jpegAssembleBuffer = ByteArrayOutputStream(JPEG_HINT_BYTES)

    override fun setPreferCapturePolling(prefer: Boolean) {
        val previous = preferCaptureOnly.getAndSet(prefer)
        if (previous != prefer) {
            Log.d(TAG, "preferCaptureOnly $previous → $prefer")
        }
    }

    override fun setHudPreviewOptions(options: HudPreviewOptions) {
        val previous = hudPreviewOptions.getAndSet(options)
        if (previous != options) {
            loggedDecodeOptions = false
            Log.d(
                TAG,
                "hudPreviewOptions inSampleSize=${options.inSampleSize} " +
                    "rgb565=${options.useRgb565} maxFps=${options.maxHudFps}",
            )
        }
    }

    override fun setFastPreviewDecode(enabled: Boolean) {
        setHudPreviewOptions(
            if (enabled) {
                SoftApPerformancePreset.BALANCED.toHudPreviewOptions()
            } else {
                HudPreviewOptions.FULL_QUALITY
            },
        )
    }

    override fun captureStillBitmap(): Bitmap? {
        val jpeg = lastJpegBytes.get() ?: return null
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
    }

    override fun copyLastJpeg(): ByteArray? = lastJpegBytes.get()?.copyOf()

    override fun lastJpegEpoch(): Long = jpegEpoch.get()

    private fun publishJpeg(jpeg: ByteArray?) {
        lastJpegBytes.set(jpeg)
        jpegEpoch.incrementAndGet()
    }

    override fun startStream(baseUrl: String) {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        // Idempotent: avoid tearing down a healthy feed when UI re-arms the same URL.
        if (streamJob?.isActive == true && activeBaseUrl == normalizedBase) {
            Log.d(TAG, "startStream ignored — already streaming $normalizedBase")
            return
        }
        stopStreamInternal(clearPrefer = false)
        activeBaseUrl = normalizedBase
        acquireWifiLockIfNeeded()
        _streamState.value = CameraStreamState.Connecting
        streamJob = scope.launch {
            logDecodeOptionsOnce()
            while (isActive) {
                if (preferCaptureOnly.get()) {
                    pollCaptureFrames(normalizedBase)
                    continue
                }
                val streamed = try {
                    consumeMjpegStream(normalizedBase)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "MJPEG /stream failed, falling back to /capture", e)
                    false
                }
                if (!isActive) break
                if (preferCaptureOnly.get()) {
                    pollCaptureFrames(normalizedBase)
                    continue
                }
                if (streamed) {
                    // SoftAP+BLE often drops long MJPEG after a few frames — use /capture.
                    Log.d(TAG, "MJPEG ended with frames — switching to /capture")
                    pollCaptureFrames(normalizedBase)
                } else {
                    pollCaptureFrames(normalizedBase)
                }
            }
        }
    }

    override fun stopStream() {
        stopStreamInternal(clearPrefer = true)
    }

    private fun stopStreamInternal(clearPrefer: Boolean) {
        streamJob?.cancel()
        streamJob = null
        activeBaseUrl = null
        if (clearPrefer) {
            preferCaptureOnly.set(false)
        }
        publishJpeg(null)
        releaseWifiLockIfHeld()
        // Publish Idle before recycle so Compose drops the Image. Recycle is always
        // deferred — Huawei display lists can draw a frame after we replace it.
        val stale = listOfNotNull(activeBitmap)
        activeBitmap = null
        _streamState.value = CameraStreamState.Idle
        stale.forEach { scheduleRecycle(it) }
    }

    private fun acquireWifiLockIfNeeded() {
        if (wifiLockHeld) return
        softApWifiLock.acquire(HOLDER_STREAM)
        wifiLockHeld = true
    }

    private fun releaseWifiLockIfHeld() {
        if (!wifiLockHeld) return
        softApWifiLock.release(HOLDER_STREAM)
        wifiLockHeld = false
    }

    /**
     * @return true if the HTTP MJPEG connection delivered at least one frame.
     */
    private suspend fun consumeMjpegStream(baseUrl: String): Boolean {
        val connection = openGetConnection(Esp32CameraDefaults.streamUrl(baseUrl)).apply {
            readTimeout = STREAM_READ_TIMEOUT_MS
        }
        val status = connection.responseCode
        if (status == HTTP_UNAVAILABLE) {
            // Firmware TELECON_CAPTURE_WHEN_BLE: expected while BLE is linked.
            Log.d(
                TAG,
                "Stream HTTP 503 — use /capture while BLE connected (expected)",
            )
            connection.disconnect()
            return false
        }
        if (status != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "Stream HTTP $status from ${Esp32CameraDefaults.streamUrl(baseUrl)}")
            connection.disconnect()
            return false
        }
        val contentType = connection.contentType?.lowercase().orEmpty()
        if (!contentType.contains("multipart") && !contentType.contains("mjpeg")) {
            Log.d(TAG, "Stream Content-Type=$contentType — scanning JPEG markers")
        }
        Log.d(TAG, "MJPEG /stream connected — ${Esp32CameraDefaults.streamUrl(baseUrl)}")
        var frames = 0
        var dropped = 0
        var lastPublishMs = 0L
        try {
            BufferedInputStream(connection.inputStream, INPUT_BUFFER_BYTES).use { input ->
                while (coroutineContext.isActive) {
                    if (preferCaptureOnly.get()) {
                        Log.d(TAG, "Leaving /stream — preferCaptureOnly")
                        break
                    }
                    // Drop every complete JPEG already in the socket — decode only what
                    // arrives after the backlog so the HUD is not a delayed movie.
                    var skipped = 0
                    while (input.available() > 0 && skipped < MAX_SKIP_AHEAD_FRAMES) {
                        if (!discardNextJpeg(input)) break
                        dropped++
                        skipped++
                    }
                    val jpeg = readNextJpeg(input) ?: break
                    // Always retain JPEG for photo; may skip decode/publish under FPS cap.
                    publishJpeg(jpeg)
                    val now = System.currentTimeMillis()
                    val minGap = hudPreviewOptions.get().minPublishGapMs(MIN_PUBLISH_GAP_MS)
                    if (lastPublishMs > 0L && now - lastPublishMs < minGap) {
                        dropped++
                        continue
                    }
                    val decoded = decodeHudJpeg(jpeg) ?: continue
                    frames++
                    lastPublishMs = now
                    replaceFrame(decoded)
                    yield()
                }
            }
        } finally {
            connection.disconnect()
            Log.d(
                TAG,
                "MJPEG /stream ended after $frames frame(s), dropped $dropped under load",
            )
        }
        return frames > 0
    }

    private suspend fun pollCaptureFrames(baseUrl: String) {
        val mode = if (preferCaptureOnly.get()) "BLE-linked" else "fallback"
        Log.d(TAG, "Using /capture poll ($mode)")
        var okStreak = 0
        var lastPublishMs = 0L
        while (coroutineContext.isActive) {
            if (!preferCaptureOnly.get() && okStreak >= CAPTURE_BEFORE_STREAM_RETRY) {
                Log.d(TAG, "Capture stable — retrying /stream")
                return
            }
            if (preferCaptureOnly.get() && lastPublishMs > 0L) {
                val minGap = hudPreviewOptions.get().minPublishGapMs(MIN_PUBLISH_GAP_MS)
                val elapsed = System.currentTimeMillis() - lastPublishMs
                if (elapsed < minGap) {
                    delay(minGap - elapsed)
                }
            }
            try {
                val connection = openGetConnection(Esp32CameraDefaults.captureUrl(baseUrl))
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Capture HTTP $status from ${Esp32CameraDefaults.captureUrl(baseUrl)}")
                    connection.disconnect()
                    if (_streamState.value !is CameraStreamState.Frame) {
                        _streamState.value = CameraStreamState.Error("HTTP $status")
                    }
                    delay(ERROR_RETRY_MS)
                    continue
                }
                connection.inputStream.use { input ->
                    val bytes = input.readBytes()
                    publishJpeg(bytes)
                    val now = System.currentTimeMillis()
                    val minGap = hudPreviewOptions.get().minPublishGapMs(MIN_PUBLISH_GAP_MS)
                    if (lastPublishMs > 0L && now - lastPublishMs < minGap) {
                        // Drain SoftAP /capture without decoding every response.
                    } else {
                        val decoded = decodeHudJpeg(bytes)
                        if (decoded != null) {
                            replaceFrame(decoded)
                            lastPublishMs = now
                            okStreak++
                        } else {
                            Log.w(TAG, "Capture decode failed (${bytes.size} bytes)")
                            if (_streamState.value !is CameraStreamState.Frame) {
                                _streamState.value = CameraStreamState.Error("Invalid camera frame")
                            }
                        }
                    }
                }
                connection.disconnect()
                yield()
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                Log.w(TAG, "Capture failed", e)
                if (_streamState.value !is CameraStreamState.Frame) {
                    _streamState.value = CameraStreamState.Error(
                        e.message ?: "Camera connection failed",
                    )
                }
                delay(ERROR_RETRY_MS)
                if (!preferCaptureOnly.get()) {
                    return
                }
            }
        }
    }

    private fun openGetConnection(urlString: String): HttpURLConnection {
        return softApNetworkResolver.openHttpConnection(urlString).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            doInput = true
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Connection", "keep-alive")
            setRequestProperty("Accept", "*/*")
        }
    }

    private fun decodeHudJpeg(jpeg: ByteArray): Bitmap? {
        val options = hudPreviewOptions.get()
        if (options.inSampleSize <= 1 && !options.useRgb565) {
            return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        }
        return BitmapFactory.decodeByteArray(
            jpeg,
            0,
            jpeg.size,
            BitmapFactory.Options().apply {
                inSampleSize = options.inSampleSize.coerceAtLeast(1)
                if (options.useRgb565) {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            },
        )
    }

    private fun logDecodeOptionsOnce() {
        if (loggedDecodeOptions) return
        loggedDecodeOptions = true
        val options = hudPreviewOptions.get()
        if (options.inSampleSize <= 1 && !options.useRgb565) {
            Log.d(
                TAG,
                "HUD decode: full quality (ARGB default), maxFps=${options.maxHudFps}",
            )
        } else {
            Log.d(
                TAG,
                "HUD decode: inSampleSize=${options.inSampleSize} " +
                    "rgb565=${options.useRgb565} maxFps=${options.maxHudFps} " +
                    "(photo uses full-quality last JPEG)",
            )
        }
    }

    /**
     * Reads the next JPEG from a multipart MJPEG (or raw concatenated JPEGs) by SOI/EOI
     * markers using chunked buffer scans (no per-byte [InputStream.read] hot path).
     */
    private fun readNextJpeg(input: InputStream): ByteArray? {
        val scan = jpegScanBuffer
        var prev = -1
        // Find SOI (FF D8), then accumulate until EOI (FF D9).
        while (true) {
            val n = input.read(scan)
            if (n < 0) return null
            var i = 0
            while (i < n) {
                val b = scan[i].toInt() and 0xFF
                if (prev == 0xFF && b == 0xD8) {
                    return readJpegBodyAfterSoi(input, scan, i + 1, n)
                }
                prev = b
                i++
            }
        }
    }

    /**
     * Drains one SoftAP JPEG without retaining bytes — used when the socket is already
     * backed up so we can keep only the latest frame for decode.
     *
     * @return true if a full SOI…EOI frame was discarded; false on EOF / oversized frame.
     */
    private fun discardNextJpeg(input: InputStream): Boolean {
        val scan = jpegScanBuffer
        var prev = -1
        var inFrame = false
        var frameBytes = 0
        while (true) {
            val n = input.read(scan)
            if (n < 0) return false
            var i = 0
            while (i < n) {
                val b = scan[i].toInt() and 0xFF
                if (!inFrame) {
                    if (prev == 0xFF && b == 0xD8) {
                        inFrame = true
                        frameBytes = 2
                    }
                    prev = b
                    i++
                    continue
                }
                frameBytes++
                if (prev == 0xFF && b == 0xD9) {
                    return true
                }
                if (frameBytes > MAX_JPEG_BYTES) {
                    Log.w(TAG, "JPEG frame exceeded $MAX_JPEG_BYTES bytes while discarding — resync")
                    return false
                }
                prev = b
                i++
            }
        }
    }

    /** [firstChunk] bytes from [start] onward follow the SOI marker already consumed. */
    private fun readJpegBodyAfterSoi(
        input: InputStream,
        firstChunk: ByteArray,
        start: Int,
        firstLen: Int,
    ): ByteArray? {
        val out = jpegAssembleBuffer
        out.reset()
        out.write(0xFF)
        out.write(0xD8)
        var prev = 0xD8
        fun consume(buf: ByteArray, from: Int, len: Int): Int {
            // 1 = EOI complete, 0 = need more, -1 = oversized / resync
            var i = from
            while (i < len) {
                val b = buf[i].toInt() and 0xFF
                out.write(b)
                if (prev == 0xFF && b == 0xD9) {
                    return 1
                }
                prev = b
                if (out.size() > MAX_JPEG_BYTES) {
                    Log.w(TAG, "JPEG frame exceeded $MAX_JPEG_BYTES bytes — resync")
                    return -1
                }
                i++
            }
            return 0
        }
        if (start < firstLen) {
            when (consume(firstChunk, start, firstLen)) {
                1 -> return out.toByteArray()
                -1 -> return null
            }
        }
        val scan = jpegScanBuffer
        while (true) {
            val n = input.read(scan)
            if (n < 0) return null
            when (consume(scan, 0, n)) {
                1 -> return out.toByteArray()
                -1 -> return null
            }
        }
    }

    private fun replaceFrame(bitmap: Bitmap) {
        val previous = activeBitmap
        activeBitmap = bitmap
        _streamState.value = CameraStreamState.Frame(bitmap)
        if (previous != null && previous !== bitmap) {
            scheduleRecycle(previous)
        }
    }

    /** Never recycle on the stream thread — Compose / hardware layers may still draw it. */
    private fun scheduleRecycle(bitmap: Bitmap) {
        scope.launch {
            delay(BITMAP_RECYCLE_DELAY_MS)
            if (bitmap === activeBitmap) return@launch
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private companion object {
        private const val TAG = "Esp32CameraStream"
        private const val HTTP_UNAVAILABLE = 503
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val STREAM_READ_TIMEOUT_MS = 15_000
        private const val ERROR_RETRY_MS = 250L
        private const val CAPTURE_BEFORE_STREAM_RETRY = 30
        private const val INPUT_BUFFER_BYTES = 8 * 1024
        private const val SCAN_CHUNK_BYTES = 4 * 1024
        private const val JPEG_HINT_BYTES = 16 * 1024
        private const val MAX_JPEG_BYTES = 512 * 1024
        /** Soft floor between Compose publishes — prefer latest over decoding every SoftAP JPEG. */
        private const val MIN_PUBLISH_GAP_MS = 16L
        /** Cap discard-loop so a flood cannot starve decode forever. */
        private const val MAX_SKIP_AHEAD_FRAMES = 24
        private const val HOLDER_STREAM = "stream"
        /** Huawei/EMUI display lists can replay a BitmapPainter after the next frame. */
        private const val BITMAP_RECYCLE_DELAY_MS = 500L
    }
}
