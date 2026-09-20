package com.micsbol.telecon4esp32.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    val deviceName = try {
        name
    } catch (_: SecurityException) {
        null
    }
    return BluetoothDeviceDomain(
        name = deviceName,
        address = address,
    )
}