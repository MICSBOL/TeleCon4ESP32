package com.micsbol.emitterapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.micsbol.emitterapp.domain.bluetooth.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}