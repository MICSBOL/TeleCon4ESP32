package com.micsbol.telecon4esp32.domain.bluetooth.lt

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame

/**
 * Smart Lighting (`LT`) binary protocol — app byte `0x4C` (`L`).
 *
 * Wire prefix remains `LT` for SIMPLE lines (help text historically said `SL:`).
 */
object LtBinaryProtocol {
    const val APP_BYTE: Byte = 0x4C
    /** on_count, total, flags (auto_away|motion|sunset), then 12 zone bits packed in 2 bytes. */
    const val DATA_PAYLOAD_SIZE: Int = 5

    const val MASK_ALL: Int = 1 shl 0
    const val MASK_ZONE: Int = 1 shl 1
    const val MASK_AUTO_AWAY: Int = 1 shl 2
    const val MASK_MOTION: Int = 1 shl 3
    const val MASK_SUNSET: Int = 1 shl 4
}

object LtPacketEncoder {
    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()

        pairs["all"]?.let {
            mask = mask or LtBinaryProtocol.MASK_ALL
            values += AppBinaryFrame.boolByte(it)
        }
        if (pairs.containsKey("zone_id")) {
            mask = mask or LtBinaryProtocol.MASK_ZONE
            values += AppBinaryFrame.u8Byte(pairs.getValue("zone_id"))
            values += AppBinaryFrame.boolByte(pairs["state"] ?: 1)
        }
        pairs["auto_away"]?.let {
            mask = mask or LtBinaryProtocol.MASK_AUTO_AWAY
            values += AppBinaryFrame.boolByte(it)
        }
        pairs["motion"]?.let {
            mask = mask or LtBinaryProtocol.MASK_MOTION
            values += AppBinaryFrame.boolByte(it)
        }
        pairs["sunset"]?.let {
            mask = mask or LtBinaryProtocol.MASK_SUNSET
            values += AppBinaryFrame.boolByte(it)
        }

        return AppBinaryFrame.buildSetPacket(LtBinaryProtocol.APP_BYTE, mask, values)
    }
}

object LtBinaryTelemetryMapper {
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        val (type, payload) = AppBinaryFrame.decodeInbound(bytes, LtBinaryProtocol.APP_BYTE) ?: return null
        if (type != AppBinaryFrame.TYPE_DATA) return null
        if (payload.size < LtBinaryProtocol.DATA_PAYLOAD_SIZE) return null

        val onCount = payload[0].toInt() and 0xFF
        val total = payload[1].toInt() and 0xFF
        val flags = payload[2].toInt() and 0xFF
        val zones = (payload[3].toInt() and 0xFF) or ((payload[4].toInt() and 0xFF) shl 8)

        return buildMap {
            put("on_count", onCount.toString())
            put("total", total.toString())
            put("auto_away", if ((flags and 1) != 0) "1" else "0")
            put("motion", if ((flags and 2) != 0) "1" else "0")
            put("sunset", if ((flags and 4) != 0) "1" else "0")
            put("zones", zones.toString())
            put("online", "1")
        }
    }
}
