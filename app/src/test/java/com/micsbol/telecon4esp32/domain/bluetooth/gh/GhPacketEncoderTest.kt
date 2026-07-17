package com.micsbol.telecon4esp32.domain.bluetooth.gh

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GhPacketEncoderTest {

    @Test
    fun `buildSetPacket encodes fan only`() {
        val packet = GhPacketEncoder.buildSetPacket(mapOf("fan" to 1))

        assertEquals(GhBinaryProtocol.OUTBOUND_HEADER_0, packet[0])
        assertEquals(GhBinaryProtocol.OUTBOUND_HEADER_1, packet[1])
        val mask = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertEquals(GhBinaryProtocol.MASK_FAN, mask)
        assertEquals(1, packet[4].toInt())
    }

    @Test
    fun `buildSetPacket encodes vent and target climate`() {
        val packet = GhPacketEncoder.buildSetPacket(
            mapOf(
                "vent" to 60,
                "target_temp" to 24,
                "target_hum" to 65,
            ),
        )

        val mask = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertEquals(
            GhBinaryProtocol.MASK_VENT or
                GhBinaryProtocol.MASK_TARGET_TEMP or
                GhBinaryProtocol.MASK_TARGET_HUM,
            mask,
        )
        assertEquals(60, packet[4].toInt())
        assertEquals(24, packet[5].toInt())
        assertEquals(65, packet[6].toInt())
    }
}

class GhBinaryTelemetryMapperTest {

    @Test
    fun `decodeDataPacket parses telemetry`() {
        val payload = ByteArray(1 + GhBinaryProtocol.DATA_PAYLOAD_SIZE)
        payload[0] = GhBinaryProtocol.TYPE_DATA

        val buffer = ByteBuffer.wrap(payload, 1, GhBinaryProtocol.DATA_PAYLOAD_SIZE)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(262) // 26.2 C
        buffer.put(68) // hum
        buffer.putShort(110) // 1.10 vpd
        buffer.put(42) // soil
        buffer.putInt(12_400) // lux
        buffer.put(40) // vent
        buffer.put(78) // tank
        buffer.put(24) // target temp
        buffer.put(65) // target hum
        buffer.put(
            (
                GhBinaryProtocol.FLAG_FAN or
                    GhBinaryProtocol.FLAG_AUTO or
                    GhBinaryProtocol.FLAG_HAS_CAMERA
                ).toByte(),
        )
        buffer.put(32) // delta 3.2
        buffer.put(90) // last irr

        val body = payload
        var checksum = 0
        checksum += (body.size and 0xFF)
        checksum += ((body.size shr 8) and 0xFF)
        for (byte in body) checksum += byte.toInt() and 0xFF

        val packet = ByteArray(4 + body.size + 1)
        packet[0] = GhBinaryProtocol.INBOUND_HEADER_0
        packet[1] = GhBinaryProtocol.INBOUND_HEADER_1
        packet[2] = (body.size and 0xFF).toByte()
        packet[3] = ((body.size shr 8) and 0xFF).toByte()
        body.copyInto(packet, destinationOffset = 4)
        packet[packet.lastIndex] = (checksum and 0xFF).toByte()

        val values = GhBinaryTelemetryMapper.decodeDataPacket(packet)

        assertNotNull(values)
        assertEquals("26.2", values!!["temp"])
        assertEquals("68", values["hum"])
        assertEquals("1.1", values["vpd"])
        assertEquals("42", values["soil"])
        assertEquals("12400", values["light"])
        assertEquals("40", values["vent"])
        assertEquals("78", values["tank"])
        assertEquals("1", values["fan"])
        assertEquals("1", values["auto"])
        assertEquals("1", values["cam"])
        assertEquals("3.2", values["delta"])
        assertEquals("90", values["last_irr"])
    }

    @Test
    fun `decodeDataPacket rejects bad checksum`() {
        val packet = byteArrayOf(
            GhBinaryProtocol.INBOUND_HEADER_0,
            GhBinaryProtocol.INBOUND_HEADER_1,
            0x12,
            0x00,
            GhBinaryProtocol.TYPE_DATA,
            0x00,
        )
        assertNull(GhBinaryTelemetryMapper.decodeDataPacket(packet))
    }

    @Test
    fun `round trip set mask order is stable`() {
        val packet = GhPacketEncoder.buildSetPacket(
            mapOf(
                "fan" to 1,
                "pump" to 0,
                "vent" to 25,
            ),
        )
        val expectedMask = GhBinaryProtocol.MASK_FAN or
            GhBinaryProtocol.MASK_PUMP or
            GhBinaryProtocol.MASK_VENT
        val mask = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertEquals(expectedMask, mask)
        assertArrayEquals(byteArrayOf(1, 0, 25), packet.copyOfRange(4, 7))
    }
}
