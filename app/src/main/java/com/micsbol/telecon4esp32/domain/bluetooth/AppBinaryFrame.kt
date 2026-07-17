package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Shared binary framing for TeleCon application packets (GH-style):
 * - Phone → ESP32 SET: `AA <app> [mask u16 LE] [values…] [checksum]`
 * - ESP32 → Phone DATA: `CC <app> [length u16 LE] [type] [payload…] [checksum]`
 *
 * Checksum is the low byte of the sum of all bytes after the 2-byte header
 * (for SET: from mask through last value; for DATA: from length through last payload byte).
 */
object AppBinaryFrame {

    const val OUTBOUND_HEADER: Byte = 0xAA.toByte()
    const val INBOUND_HEADER: Byte = 0xCC.toByte()
    const val TYPE_DATA: Byte = 0x01

    fun buildSetPacket(appByte: Byte, mask: Int, values: List<Byte>): ByteArray {
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

        return byteArrayOf(OUTBOUND_HEADER, appByte) + body
    }

    /**
     * Validates a length-prefixed inbound frame and returns `(type, payload)` or null.
     */
    fun decodeInbound(bytes: ByteArray, appByte: Byte): Pair<Byte, ByteArray>? {
        if (bytes.size < 6) return null
        if (bytes[0] != INBOUND_HEADER || bytes[1] != appByte) return null

        val payloadLength = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
        val expectedSize = 4 + payloadLength + 1
        if (bytes.size != expectedSize || payloadLength < 1) return null

        val checksumIndex = bytes.size - 1
        var calculatedChecksum = 0
        for (index in 2 until checksumIndex) {
            calculatedChecksum += bytes[index].toInt() and 0xFF
        }
        if ((calculatedChecksum and 0xFF).toByte() != bytes[checksumIndex]) return null

        val type = bytes[4]
        val payload = bytes.copyOfRange(5, checksumIndex)
        return type to payload
    }

    fun boolByte(value: Any): Byte = when (value) {
        is Boolean -> if (value) 1 else 0
        is Number -> if (value.toInt() != 0) 1 else 0
        is String -> when (value.lowercase()) {
            "1", "true", "on", "yes" -> 1
            else -> 0
        }
        else -> 0
    }.toByte()

    fun u8Byte(value: Any): Byte = when (value) {
        is Number -> value.toInt().coerceIn(0, 255)
        is String -> value.toIntOrNull()?.coerceIn(0, 255) ?: 0
        else -> 0
    }.toByte()
}
