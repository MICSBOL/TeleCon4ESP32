package com.micsbol.telecon4esp32.domain.bluetooth.gh

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes inbound `CC 47` Greenhouse telemetry packets into the same key map
 * used by the SIMPLE `GH:DATA` line protocol.
 */
object GhBinaryTelemetryMapper {

    /**
     * @return value map for [com.micsbol.telecon4esp32.domain.bluetooth.EspMessage], or null if invalid.
     */
    fun decodeDataPacket(bytes: ByteArray): Map<String, String>? {
        if (bytes.size < 8) return null
        if (bytes[0] != GhBinaryProtocol.INBOUND_HEADER_0 ||
            bytes[1] != GhBinaryProtocol.INBOUND_HEADER_1
        ) {
            return null
        }

        val payloadLength = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
        val expectedSize = 4 + payloadLength + 1
        if (bytes.size != expectedSize || payloadLength < 1 + GhBinaryProtocol.DATA_PAYLOAD_SIZE) {
            return null
        }

        val checksumIndex = bytes.size - 1
        var calculatedChecksum = 0
        for (index in 2 until checksumIndex) {
            calculatedChecksum += bytes[index].toInt() and 0xFF
        }
        if ((calculatedChecksum and 0xFF).toByte() != bytes[checksumIndex]) {
            return null
        }

        val type = bytes[4]
        if (type != GhBinaryProtocol.TYPE_DATA) return null

        val payloadOffset = 5
        val buffer = ByteBuffer.wrap(bytes, payloadOffset, GhBinaryProtocol.DATA_PAYLOAD_SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)

        val tempX10 = buffer.short.toInt()
        val humidity = buffer.get().toInt() and 0xFF
        val vpdX100 = buffer.short.toInt() and 0xFFFF
        val soil = buffer.get().toInt() and 0xFF
        val lightLux = buffer.int.toLong() and 0xFFFFFFFFL
        val vent = buffer.get().toInt() and 0xFF
        val tank = buffer.get().toInt() and 0xFF
        val targetTemp = buffer.get().toInt() and 0xFF
        val targetHum = buffer.get().toInt() and 0xFF
        val flags = buffer.get().toInt() and 0xFF
        val deltaX10 = buffer.get().toInt()
        val lastIrrMinutes = buffer.get().toInt() and 0xFF

        return buildMap {
            put("temp", formatTemp(tempX10))
            put("hum", humidity.toString())
            put("vpd", formatVpd(vpdX100))
            put("soil", soil.toString())
            put("light", lightLux.toString())
            put("vent", vent.toString())
            put("tank", tank.toString())
            put("target_temp", targetTemp.toString())
            put("target_hum", targetHum.toString())
            put("fan", flagBit(flags, GhBinaryProtocol.FLAG_FAN))
            put("heater", flagBit(flags, GhBinaryProtocol.FLAG_HEATER))
            put("pump", flagBit(flags, GhBinaryProtocol.FLAG_PUMP))
            put("lights", flagBit(flags, GhBinaryProtocol.FLAG_LIGHTS))
            put("auto", flagBit(flags, GhBinaryProtocol.FLAG_AUTO))
            put("stable", flagBit(flags, GhBinaryProtocol.FLAG_STABLE))
            put("cam", flagBit(flags, GhBinaryProtocol.FLAG_HAS_CAMERA))
            put("delta", (deltaX10 / 10f).toString())
            put("last_irr", lastIrrMinutes.toString())
        }
    }

    private fun formatTemp(tempX10: Int): String = (tempX10 / 10f).toString()

    private fun formatVpd(vpdX100: Int): String = (vpdX100 / 100f).toString()

    private fun flagBit(flags: Int, bit: Int): String = if ((flags and bit) != 0) "1" else "0"
}
