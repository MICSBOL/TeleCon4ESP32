package com.micsbol.telecon4esp32.domain.bluetooth.wt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WtBinaryProtocolTest {

    @Test
    fun `set packet encodes pump bit`() {
        val packet = WtPacketEncoder.buildSetPacket(mapOf("pump" to 1))
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(WtBinaryProtocol.APP_BYTE, packet[1])
        assertEquals(1.toByte(), packet[2]) // mask low
        assertEquals(0.toByte(), packet[3]) // mask high
        assertEquals(1.toByte(), packet[4]) // pump value
    }

    @Test
    fun `data packet round trips core fields`() {
        // CC 57 + len=6 (type+5) + type 01 + payload + checksum
        val payload = byteArrayOf(74, 0xF4.toByte(), 0x01, 1, 0) // level 74, cap 500, pump on, status normal
        val length = 1 + payload.size
        var checksum = (length and 0xFF) + ((length shr 8) and 0xFF) + 0x01
        payload.forEach { checksum += it.toInt() and 0xFF }
        val packet = byteArrayOf(
            0xCC.toByte(), WtBinaryProtocol.APP_BYTE,
            (length and 0xFF).toByte(), ((length shr 8) and 0xFF).toByte(),
            0x01,
        ) + payload + byteArrayOf((checksum and 0xFF).toByte())

        val values = WtBinaryTelemetryMapper.decodeDataPacket(packet)
        assertNotNull(values)
        assertEquals("74", values!!["level"])
        assertEquals("500", values["cap"])
        assertEquals("1", values["pump"])
        assertEquals("0", values["status"])
    }
}
