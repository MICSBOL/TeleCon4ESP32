package com.micsbol.telecon4esp32.data.bluetooth

/**
 * A single inbound frame from the Bluetooth socket.
 */
sealed interface IncomingBluetoothFrame {
    data class BinaryPacket(val bytes: ByteArray) : IncomingBluetoothFrame {
        override fun equals(other: Any?): Boolean =
            other is BinaryPacket && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class TextLine(val line: String) : IncomingBluetoothFrame
}
