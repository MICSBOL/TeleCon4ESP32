package com.micsbol.telecon4esp32.domain.bluetooth.sh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShBinaryProtocolTest {

    @Test
    fun `buildSetPacket encodes room device`() {
        val packet = ShPacketEncoder.buildSetPacket(
            mapOf(
                "room_id" to 0,
                "device_id" to 1,
                "state" to 1,
            ),
        )
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(ShBinaryProtocol.APP_BYTE, packet[1])
        val mask = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertTrue((mask and ShBinaryProtocol.MASK_ROOM_DEVICE) != 0)
        assertEquals(0, packet[4].toInt() and 0xFF)
        assertEquals(1, packet[5].toInt() and 0xFF)
        assertEquals(1, packet[6].toInt() and 0xFF)
    }

    @Test
    fun `buildSetPacket encodes scene away`() {
        val packet = ShPacketEncoder.buildSetPacket(
            mapOf("scene" to SmartHomeProtocol.SCENE_AWAY),
        )
        val mask = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertTrue((mask and ShBinaryProtocol.MASK_SCENE) != 0)
        assertEquals(ShBinaryProtocol.SCENE_AWAY, packet[4].toInt() and 0xFF)
    }

    @Test
    fun `decodeDataPacket reads device flags`() {
        val payload = ByteArray(ShBinaryProtocol.DATA_PAYLOAD_SIZE)
        payload[0] = 22 // climate_temp
        payload[1] = 0
        payload[2] = 18 // energy 1.8 kW → 18
        payload[3] = 0
        payload[4] = 0
        payload[5] = 42
        payload[6] = 0
        payload[7] = 0
        payload[8] = 2
        payload[9] = 1
        payload[10] = 0
        payload[11] = 1
        val flags = (1 shl 0) or (1 shl 3)
        payload[12] = (flags and 0xFF).toByte()
        payload[13] = ((flags shr 8) and 0xFF).toByte()

        val length = 1 + payload.size
        var checksum = (length and 0xFF) + ((length shr 8) and 0xFF) + 0x01
        payload.forEach { checksum += it.toInt() and 0xFF }
        val frame = byteArrayOf(
            0xCC.toByte(), ShBinaryProtocol.APP_BYTE,
            (length and 0xFF).toByte(), ((length shr 8) and 0xFF).toByte(),
            0x01,
        ) + payload + byteArrayOf((checksum and 0xFF).toByte())

        val map = ShBinaryTelemetryMapper.decodeDataPacket(frame)
        assertNotNull(map)
        assertEquals("22", map!!["climate_temp"])
        assertEquals("1.8", map["energy_kw"])
        assertEquals("1", map["living_light"])
        assertEquals("0", map["living_ambience"])
        assertEquals("1", map["kitchen_light"])
        assertEquals("2", map["living_on"])
    }
}
