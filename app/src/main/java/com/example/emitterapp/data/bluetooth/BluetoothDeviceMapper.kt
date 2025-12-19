package com.example.emitterapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}