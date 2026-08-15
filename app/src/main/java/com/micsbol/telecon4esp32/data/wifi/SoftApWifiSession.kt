package com.micsbol.telecon4esp32.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Holds an app-scoped SoftAP [Network] via [WifiNetworkSpecifier] (API 29+).
 *
 * Requests Wi‑Fi **without** [NetworkCapabilities.NET_CAPABILITY_INTERNET] so Android
 * does not treat the ESP32 SoftAP as a failed internet route and tear it down for this
 * app session. Keep [release] paired with SoftAP TCP/HTTP disconnect.
 */
@Singleton
class SoftApWifiSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val guard = Any()
    private var boundNetwork: Network? = null
    private var boundSsid: String? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun currentNetwork(): Network? = synchronized(guard) { boundNetwork }

    fun currentSsid(): String? = synchronized(guard) { boundSsid }

    /**
     * Requests SoftAP association for [ssid]. Shows the system Wi‑Fi panel on API 29+.
     * Reuses an existing bind when the SSID matches.
     */
    suspend fun join(
        ssid: String,
        password: String,
        timeoutMs: Long = JOIN_TIMEOUT_MS,
    ): JoinResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i(TAG, "WifiNetworkSpecifier unavailable (API < 29) — use system Wi‑Fi")
            return JoinResult.Unsupported
        }
        val trimmedSsid = ssid.trim()
        if (trimmedSsid.isEmpty()) {
            return JoinResult.RejectedOrTimeout
        }
        synchronized(guard) {
            if (boundNetwork != null && boundSsid == trimmedSsid) {
                Log.d(TAG, "SoftAP already bound ssid=$trimmedSsid network=$boundNetwork")
                return JoinResult.AlreadyBound(boundNetwork!!)
            }
        }
        release()

        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return JoinResult.Unsupported

        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(trimmedSsid)
                    .setWpa2Passphrase(password)
                    .build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        synchronized(guard) {
                            boundNetwork = network
                            boundSsid = trimmedSsid
                            networkCallback = this
                        }
                        Log.i(TAG, "SoftAP available ssid=$trimmedSsid network=$network")
                        if (cont.isActive) {
                            cont.resume(JoinResult.Available(network))
                        }
                    }

                    override fun onUnavailable() {
                        Log.w(TAG, "SoftAP unavailable / user cancelled ssid=$trimmedSsid")
                        synchronized(guard) {
                            if (networkCallback === this) {
                                networkCallback = null
                                boundNetwork = null
                                boundSsid = null
                            }
                        }
                        if (cont.isActive) {
                            cont.resume(JoinResult.RejectedOrTimeout)
                        }
                    }

                    override fun onLost(network: Network) {
                        synchronized(guard) {
                            if (boundNetwork == network) {
                                Log.w(TAG, "SoftAP lost ssid=$trimmedSsid network=$network")
                                boundNetwork = null
                            }
                        }
                    }
                }

                cont.invokeOnCancellation {
                    runCatching { cm.unregisterNetworkCallback(callback) }
                    synchronized(guard) {
                        if (networkCallback === callback) {
                            networkCallback = null
                            boundNetwork = null
                            boundSsid = null
                        }
                    }
                }

                try {
                    Log.i(TAG, "Requesting SoftAP ssid=$trimmedSsid (local-only, no internet)")
                    cm.requestNetwork(request, callback)
                } catch (e: SecurityException) {
                    Log.e(TAG, "requestNetwork denied: ${e.message}")
                    if (cont.isActive) cont.resume(JoinResult.Unsupported)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "requestNetwork failed: ${e.message}")
                    if (cont.isActive) cont.resume(JoinResult.RejectedOrTimeout)
                }
            }
        }

        return result ?: run {
            Log.w(TAG, "SoftAP join timed out ssid=$trimmedSsid")
            release()
            JoinResult.RejectedOrTimeout
        }
    }

    /** Drops the SoftAP [NetworkRequest] so the OS may leave the ESP32 AP. */
    fun release() {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback: ConnectivityManager.NetworkCallback?
        synchronized(guard) {
            callback = networkCallback
            networkCallback = null
            boundNetwork = null
            val ssid = boundSsid
            boundSsid = null
            if (callback != null) {
                Log.d(TAG, "Releasing SoftAP bind ssid=$ssid")
            }
        }
        if (callback != null && cm != null) {
            runCatching { cm.unregisterNetworkCallback(callback) }
                .onFailure { Log.w(TAG, "unregisterNetworkCallback: ${it.message}") }
        }
    }

    sealed interface JoinResult {
        data class Available(val network: Network) : JoinResult
        data class AlreadyBound(val network: Network) : JoinResult
        /** API &lt; 29 or ConnectivityManager missing — caller uses system Wi‑Fi. */
        data object Unsupported : JoinResult
        data object RejectedOrTimeout : JoinResult
    }

    private companion object {
        private const val TAG = "SoftApWifiSession"
        private const val JOIN_TIMEOUT_MS = 60_000L
    }
}
