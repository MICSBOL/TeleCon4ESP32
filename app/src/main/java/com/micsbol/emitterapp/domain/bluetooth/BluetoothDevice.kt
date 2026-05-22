package com.micsbol.emitterapp.domain.bluetooth

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    override val name: String?,
    override val address: String
): RemoteDevice
