package com.micsbol.telecon4esp32.domain.bluetooth

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    override val name: String?,
    override val address: String
): RemoteDevice
