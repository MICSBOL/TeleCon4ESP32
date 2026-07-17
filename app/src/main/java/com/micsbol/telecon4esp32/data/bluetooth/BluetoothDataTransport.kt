package com.micsbol.telecon4esp32.data.bluetooth

import kotlinx.coroutines.flow.Flow

/**
 * Established Bluetooth link (Classic SPP socket or BLE GATT session) that moves
 * raw protocol bytes. [AndroidBluetoothController] parses frames the same way
 * regardless of which transport produced them.
 */
interface BluetoothDataTransport {
    fun listenForIncoming(): Flow<IncomingBluetoothFrame>
    fun sendPacket(data: ByteArray): Boolean
    fun close()
}
