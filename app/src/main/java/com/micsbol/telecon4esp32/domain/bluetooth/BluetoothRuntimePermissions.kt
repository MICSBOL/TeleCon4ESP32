package com.micsbol.telecon4esp32.domain.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime Bluetooth permissions for Classic discovery and BLE scan.
 *
 * Android 12+ uses [Manifest.permission.BLUETOOTH_SCAN] with `neverForLocation`
 * plus [Manifest.permission.BLUETOOTH_CONNECT]. Location is not requested and
 * is not required to be on. Android 11 and below still need fine location for
 * `startDiscovery()`.
 */
object BluetoothRuntimePermissions {
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun allGranted(context: Context): Boolean =
        required().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
}
