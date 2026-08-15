package com.micsbol.telecon4esp32.data.camera

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a high-perf / low-latency [WifiManager.WifiLock] while SoftAP HTTP stream
 * and/or SoftAP TCP control are active. Reference-counted so either holder can
 * release without dropping the lock for the other.
 */
@Singleton
class SoftApWifiLock @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val guard = Any()
    private var holders = 0
    private var wifiLock: WifiManager.WifiLock? = null
    private var modeLabel: String? = null

    fun acquire(holder: String) {
        synchronized(guard) {
            holders++
            if (holders == 1) {
                acquireLockLocked()
            } else {
                Log.d(TAG, "WifiLock retain holders=$holders (+$holder)")
            }
        }
    }

    fun release(holder: String) {
        synchronized(guard) {
            if (holders <= 0) {
                holders = 0
                return
            }
            holders--
            if (holders == 0) {
                releaseLockLocked(holder)
            } else {
                Log.d(TAG, "WifiLock retain holders=$holders (-$holder)")
            }
        }
    }

    private fun acquireLockLocked() {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            Log.w(TAG, "WifiManager unavailable — SoftAP WifiLock skipped")
            return
        }
        val (lock, label) = createBestLock(wifiManager)
        wifiLock = lock
        modeLabel = label
        lock.acquire()
        Log.i(TAG, "[SoftAP] WifiLock acquired mode=$label")
    }

    private fun releaseLockLocked(lastHolder: String) {
        val lock = wifiLock
        wifiLock = null
        val label = modeLabel
        modeLabel = null
        if (lock != null && lock.isHeld) {
            lock.release()
            Log.d(TAG, "[SoftAP] WifiLock released mode=$label (-$lastHolder)")
        }
    }

    private fun createBestLock(wifiManager: WifiManager): Pair<WifiManager.WifiLock, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val lowLatency = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                LOCK_TAG,
            )
            return lowLatency to "FULL_LOW_LATENCY"
        }
        @Suppress("DEPRECATION")
        val highPerf = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            LOCK_TAG,
        )
        return highPerf to "FULL_HIGH_PERF"
    }

    private companion object {
        private const val TAG = "SoftApWifiLock"
        private const val LOCK_TAG = "TeleCon:SoftAP"
    }
}
