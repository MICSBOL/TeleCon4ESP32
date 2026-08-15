package com.micsbol.telecon4esp32.data.camera

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.micsbol.telecon4esp32.data.wifi.SoftApWifiSession
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens SoftAP HTTP / TCP on the Wi‑Fi network that owns the ESP32 AP (192.168.4.x).
 *
 * Prefer the app-scoped SoftAP [Network] from [SoftApWifiSession] (local-only request).
 * Avoids cellular / wrong default route while BLE is active. Prefer
 * [Network.openConnection] / [Network.bindSocket] over process-wide
 * [ConnectivityManager.bindProcessToNetwork] so other app traffic is not forced onto SoftAP.
 */
@Singleton
class SoftApNetworkResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val softApWifiSession: SoftApWifiSession,
) {
    fun findSoftApNetwork(
        host: String = Esp32CameraDefaults.DEFAULT_SOFTAP_HOST,
    ): Network? {
        softApWifiSession.currentNetwork()?.let { bound ->
            Log.d(TAG, "Using SoftApWifiSession network=$bound")
            return bound
        }
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val wifiOnSoftAp = cm.allNetworks.firstOrNull { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@firstOrNull false
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@firstOrNull false
            val linkProps = cm.getLinkProperties(network) ?: return@firstOrNull false
            linkProps.linkAddresses.any { addr ->
                val ip = addr.address.hostAddress.orEmpty()
                ip == host || ip.startsWith("192.168.4.")
            }
        }
        if (wifiOnSoftAp != null) return wifiOnSoftAp

        // Some OEM SoftAP joins omit 192.168.4.x on LinkProperties briefly — use any Wi‑Fi.
        return cm.allNetworks.firstOrNull { network ->
            cm.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    fun openHttpConnection(urlString: String): HttpURLConnection {
        val url = URL(urlString)
        val host = Esp32CameraDefaults.softApHostFromBaseUrl(urlString)
        val network = findSoftApNetwork(host)
        val connection = if (network != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "HTTP via SoftAP/Wi‑Fi network=${network} → $host")
            network.openConnection(url)
        } else {
            Log.w(TAG, "No SoftAP Wi‑Fi network found — default route for $host")
            url.openConnection()
        }
        return connection as HttpURLConnection
    }

    /**
     * SoftAP TCP control (`:3333`): bind the socket to the SoftAP [Network] before connect
     * so MIUI / dual-SIM default routes do not miss 192.168.4.1.
     */
    fun openTcpSocket(
        host: String,
        port: Int,
        connectTimeoutMs: Int,
    ): Socket {
        val sock = Socket()
        sock.tcpNoDelay = true
        val network = findSoftApNetwork(host)
        if (network != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "TCP via SoftAP/Wi‑Fi network=${network} → $host:$port")
            network.bindSocket(sock)
        } else {
            Log.w(TAG, "No SoftAP Wi‑Fi network — default route TCP for $host:$port")
        }
        sock.connect(InetSocketAddress(host, port), connectTimeoutMs)
        sock.soTimeout = 0
        return sock
    }

    private companion object {
        private const val TAG = "SoftApNetwork"
    }
}
