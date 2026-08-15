package com.micsbol.telecon4esp32.data.camera

import android.util.Log
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 SoftAP `/camconfig` client. Pushes framesize / JPEG quality / stream FPS
 * to firmware when available; degrades silently on old firmware (404 / unreachable).
 */
@Singleton
class SoftApCamConfigClient @Inject constructor(
    private val softApNetworkResolver: SoftApNetworkResolver,
) {
    @Volatile
    private var loggedUnsupported = false

    @Volatile
    private var loggedUnreachable = false

    fun apply(
        preset: SoftApPerformancePreset,
        baseUrl: String = Esp32CameraDefaults.DEFAULT_BASE_URL,
    ): SoftApCamConfigResult {
        val url = Esp32CameraDefaults.camConfigUrl(baseUrl, preset.camConfigQuery)
        var connection: HttpURLConnection? = null
        return try {
            connection = softApNetworkResolver.openHttpConnection(url).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                doInput = true
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json,*/*")
            }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    Log.d(TAG, "camconfig applied preset=$preset → $url")
                    SoftApCamConfigResult.Applied
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    if (!loggedUnsupported) {
                        loggedUnsupported = true
                        Log.i(
                            TAG,
                            "camconfig HTTP 404 — firmware without /camconfig; Phase 1 only",
                        )
                    }
                    SoftApCamConfigResult.Unsupported
                }
                else -> {
                    if (!loggedUnreachable) {
                        loggedUnreachable = true
                        Log.w(TAG, "camconfig HTTP $code from $url — Phase 1 only")
                    }
                    SoftApCamConfigResult.Unreachable
                }
            }
        } catch (e: Exception) {
            if (!loggedUnreachable) {
                loggedUnreachable = true
                Log.w(
                    TAG,
                    "camconfig unreachable (${e.javaClass.simpleName}: ${e.message}) — Phase 1 only",
                )
            }
            SoftApCamConfigResult.Unreachable
        } finally {
            connection?.disconnect()
        }
    }
}

enum class SoftApCamConfigResult {
    Applied,
    Unsupported,
    Unreachable,
}

private const val TAG = "SoftApCamConfig"
private const val CONNECT_TIMEOUT_MS = 3_000
private const val READ_TIMEOUT_MS = 3_000
