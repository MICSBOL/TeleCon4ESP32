package com.micsbol.telecon4esp32.domain.bluetooth.wt

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame

/** Water Tank (`WT`) binary protocol — app byte `0x57` (`W`). */
object WtBinaryProtocol {
    const val APP_BYTE: Byte = 0x57
    const val DATA_PAYLOAD_SIZE: Int = 5

    const val MASK_PUMP: Int = 1 shl 0
}

object WtPacketEncoder {
    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()
        pairs["pump"]?.let {
            mask = mask or WtBinaryProtocol.MASK_PUMP
            values += AppBinaryFrame.boolByte(it)
        }
        return AppBinaryFrame.buildSetPacket(WtBinaryProtocol.APP_BYTE, mask, values)
    }
}

object WtBinaryTelemetryMapper {
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        val (_, payload) = AppBinaryFrame.decodeInbound(bytes, WtBinaryProtocol.APP_BYTE) ?: return null
        if (payload.size < WtBinaryProtocol.DATA_PAYLOAD_SIZE) return null
        val level = payload[0].toInt() and 0xFF
        val cap = (payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)
        val pump = payload[3].toInt() and 0xFF
        val status = payload[4].toInt() and 0xFF
        return mapOf(
            "level" to level.toString(),
            "cap" to cap.toString(),
            "pump" to pump.toString(),
            "status" to status.toString(),
        )
    }
}
