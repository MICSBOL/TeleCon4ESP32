package com.micsbol.telecon4esp32.domain.bluetooth.gh

/**
 * Encodes outbound Greenhouse SET commands as compact binary packets.
 *
 * Layout: `AA 47 [mask u16 LE] [values in bit order] [checksum]`
 * Checksum = low byte of sum(bytes from mask through last value).
 */
object GhPacketEncoder {

    fun buildSetPacket(pairs: Map<String, Any>): ByteArray {
        var mask = 0
        val values = mutableListOf<Byte>()

        fun appendIfPresent(key: String, bit: Int, transform: (Any) -> Byte) {
            val raw = pairs[key] ?: return
            mask = mask or bit
            values.add(transform(raw))
        }

        appendIfPresent("fan", GhBinaryProtocol.MASK_FAN) { boolByte(it) }
        appendIfPresent("heater", GhBinaryProtocol.MASK_HEATER) { boolByte(it) }
        appendIfPresent("pump", GhBinaryProtocol.MASK_PUMP) { boolByte(it) }
        appendIfPresent("lights", GhBinaryProtocol.MASK_LIGHTS) { boolByte(it) }
        appendIfPresent("auto", GhBinaryProtocol.MASK_AUTO) { boolByte(it) }
        appendIfPresent("vent", GhBinaryProtocol.MASK_VENT) { percentByte(it) }
        appendIfPresent("target_temp", GhBinaryProtocol.MASK_TARGET_TEMP) { percentByte(it) }
        appendIfPresent("target_hum", GhBinaryProtocol.MASK_TARGET_HUM) { percentByte(it) }

        val bodySize = 2 + values.size + 1
        val body = ByteArray(bodySize)
        body[0] = (mask and 0xFF).toByte()
        body[1] = ((mask shr 8) and 0xFF).toByte()
        values.forEachIndexed { index, value -> body[2 + index] = value }

        var checksum = 0
        for (index in 0 until bodySize - 1) {
            checksum += body[index].toInt() and 0xFF
        }
        body[bodySize - 1] = (checksum and 0xFF).toByte()

        return byteArrayOf(GhBinaryProtocol.OUTBOUND_HEADER_0, GhBinaryProtocol.OUTBOUND_HEADER_1) +
            body
    }

    private fun boolByte(value: Any): Byte = when (value) {
        is Boolean -> if (value) 1 else 0
        is Number -> if (value.toInt() != 0) 1 else 0
        is String -> when (value.lowercase()) {
            "1", "true", "on", "yes" -> 1
            else -> 0
        }
        else -> 0
    }.toByte()

    private fun percentByte(value: Any): Byte = when (value) {
        is Number -> value.toInt().coerceIn(0, 255)
        is String -> value.toIntOrNull()?.coerceIn(0, 255) ?: 0
        else -> 0
    }.toByte()
}
