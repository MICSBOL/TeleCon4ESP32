package com.micsbol.telecon4esp32.domain.bluetooth
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.RcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
/**
 * Unit tests for [RcPacketEncoder].
 *
 * These run on the JVM with no Android context required — fast and reliable.
 */
class RcPacketEncoderTest {
    // ── Packet structure ──────────────────────────────────────────────────────
    @Test
    fun `buildRcPacket returns exactly 18 bytes`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState())
        assertEquals(RcPacketEncoder.PACKET_SIZE, packet.size)
    }
    @Test
    fun `header bytes are 0xAA and 0x55`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState())
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[1])
    }
    @Test
    fun `footer bytes are CR and LF`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState())
        assertEquals(0x0D.toByte(), packet[16])
        assertEquals(0x0A.toByte(), packet[17])
    }
    @Test
    fun `checksum covers bytes 2 to 14`() {
        val state = RcState(
            leftStickX = 50, leftStickY = -50,
            rightStickX = 0, rightStickY = 100,
            leftKnobValue = 512, rightKnobValue = 256,
            switch1 = true, switch3 = true
        )
        val packet = RcPacketEncoder.buildRcPacket(state)
        var expected = 0
        for (i in 2..14) expected += packet[i].toInt() and 0xFF
        assertEquals((expected and 0xFF).toByte(), packet[15])
    }
    // ── Stick mapping ─────────────────────────────────────────────────────────
    @Test
    fun `center stick (0) maps to 12-bit value 2047`() {
        // (0 + 100) * 4095 / 200 = 2047
        assertEquals(2047, RcPacketEncoder.stickTo12Bit(0))
    }
    @Test
    fun `max stick (+100) maps to 12-bit value 4095`() {
        assertEquals(4095, RcPacketEncoder.stickTo12Bit(100))
    }
    @Test
    fun `min stick (-100) maps to 12-bit value 0`() {
        assertEquals(0, RcPacketEncoder.stickTo12Bit(-100))
    }
    @Test
    fun `stick value above 100 is clamped to 4095`() {
        assertEquals(4095, RcPacketEncoder.stickTo12Bit(200))
    }
    @Test
    fun `stick value below -100 is clamped to 0`() {
        assertEquals(0, RcPacketEncoder.stickTo12Bit(-200))
    }
    @Test
    fun `center position is encoded correctly in packet bytes 2-3`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(leftStickX = 0))
        val encoded = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertEquals(2047, encoded)
    }
    @Test
    fun `max position encodes 4095 in packet bytes 2-3`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(leftStickX = 100))
        val encoded = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        assertEquals(4095, encoded)
    }
    // ── Knob encoding ─────────────────────────────────────────────────────────
    @Test
    fun `knob value 0 encodes to 0 in packet`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(leftKnobValue = 0))
        val encoded = (packet[10].toInt() and 0xFF) or ((packet[11].toInt() and 0xFF) shl 8)
        assertEquals(0, encoded)
    }
    @Test
    fun `knob value 1023 encodes to 1023 in packet`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(leftKnobValue = 1023))
        val encoded = (packet[10].toInt() and 0xFF) or ((packet[11].toInt() and 0xFF) shl 8)
        assertEquals(1023, encoded)
    }
    @Test
    fun `right knob is encoded at bytes 12-13`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(rightKnobValue = 512))
        val encoded = (packet[12].toInt() and 0xFF) or ((packet[13].toInt() and 0xFF) shl 8)
        assertEquals(512, encoded)
    }
    // ── Switch bitfield ───────────────────────────────────────────────────────
    @Test
    fun `all switches false produces switch byte 0x00`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState())
        assertEquals(0x00.toByte(), packet[14])
    }
    @Test
    fun `all switches true produces switch byte 0xFF`() {
        val packet = RcPacketEncoder.buildRcPacket(
            RcState(
                switch1 = true, switch2 = true, switch3 = true, switch4 = true,
                switch5 = true, switch6 = true, switch7 = true, switch8 = true
            )
        )
        assertEquals(0xFF.toByte(), packet[14])
    }
    @Test
    fun `switch1 maps to bit 0`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(switch1 = true))
        assertTrue((packet[14].toInt() and 0x01) != 0)
    }
    @Test
    fun `switch4 maps to bit 3`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(switch4 = true))
        assertTrue((packet[14].toInt() and 0x08) != 0)
    }
    @Test
    fun `switch8 maps to bit 7`() {
        val packet = RcPacketEncoder.buildRcPacket(RcState(switch8 = true))
        assertTrue((packet[14].toInt() and 0x80) != 0)
    }
    // ── Button packet ─────────────────────────────────────────────────────────
    @Test
    fun `buildButtonPacket returns 4 bytes`() {
        assertEquals(4, RcPacketEncoder.buildButtonPacket(ButtonEvent.CENTER_TOP_LEFT).size)
    }
    @Test
    fun `buildButtonPacket header is 0xBB 0x66`() {
        val pkt = RcPacketEncoder.buildButtonPacket(ButtonEvent.CENTER_TOP_RIGHT)
        assertEquals(0xBB.toByte(), pkt[0])
        assertEquals(0x66.toByte(), pkt[1])
    }
    @Test
    fun `buildButtonPacket echoes event id in bytes 2 and 3`() {
        val event = ButtonEvent.CENTER_BOTTOM_LEFT
        val pkt = RcPacketEncoder.buildButtonPacket(event)
        assertEquals(event.id, pkt[2])
        assertEquals(event.id, pkt[3])
    }
}
