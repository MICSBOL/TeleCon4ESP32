package com.micsbol.telecon4esp32.domain.bluetooth.dl

import com.micsbol.telecon4esp32.domain.bluetooth.AppBinaryFrame

/**
 * Smart Door Lock (`DL`) binary protocol — app byte `0x4B` (`K` for locK).
 *
 * Uses `0x4B` instead of ASCII `D` (`0x44`) to avoid colliding with RC config (`CC 44`).
 */
object DlBinaryProtocol {
    const val APP_BYTE: Byte = 0x4B
    const val DATA_PAYLOAD_SIZE: Int = 8

    const val MASK_UNLOCK: Int = 1 shl 0
    const val MASK_LOCK: Int = 1 shl 1
    const val MASK_PULSE: Int = 1 shl 2
    const val MASK_MIC: Int = 1 shl 3
    const val MASK_SPK: Int = 1 shl 4
    const val MASK_CAM: Int = 1 shl 5
    const val MASK_CALL: Int = 1 shl 6
}

object DlPacketEncoder {
    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()

        fun append(key: String, bit: Int) {
            val raw = pairs[key] ?: return
            mask = mask or bit
            values += AppBinaryFrame.boolByte(raw)
        }

        append("unlock", DlBinaryProtocol.MASK_UNLOCK)
        append("lock", DlBinaryProtocol.MASK_LOCK)
        append("pulse", DlBinaryProtocol.MASK_PULSE)
        append("mic", DlBinaryProtocol.MASK_MIC)
        append("spk", DlBinaryProtocol.MASK_SPK)
        append("cam", DlBinaryProtocol.MASK_CAM)
        append("call", DlBinaryProtocol.MASK_CALL)

        return AppBinaryFrame.buildSetPacket(DlBinaryProtocol.APP_BYTE, mask, values)
    }
}

object DlBinaryTelemetryMapper {
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        val (type, payload) = AppBinaryFrame.decodeInbound(bytes, DlBinaryProtocol.APP_BYTE) ?: return null
        if (type != AppBinaryFrame.TYPE_DATA) return null
        if (payload.size < DlBinaryProtocol.DATA_PAYLOAD_SIZE) return null

        val lock = payload[0].toInt() and 0xFF
        val relay = payload[1].toInt() and 0xFF
        val door = payload[2].toInt() and 0xFF
        val mic = payload[3].toInt() and 0xFF
        val spk = payload[4].toInt() and 0xFF
        val cam = payload[5].toInt() and 0xFF
        val sig = payload[6].toInt() // signed RSSI-ish
        val online = payload[7].toInt() and 0xFF

        return mapOf(
            "lock" to lock.toString(),
            "relay" to if (relay == 0) "LOW" else "HIGH",
            "door" to door.toString(),
            "mic" to mic.toString(),
            "spk" to spk.toString(),
            "cam" to cam.toString(),
            "sig" to sig.toString(),
            "online" to online.toString(),
        )
    }
}
