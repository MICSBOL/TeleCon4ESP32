package com.micsbol.telecon4esp32.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothFrameAssemblerTest {

    private val assembler = BluetoothFrameAssembler()

    @Test
    fun `parses text line terminated by newline`() {
        val frames = assembler.feed("RC:DATA,batt,82,left,150\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("RC:DATA,batt,82,left,150")), frames)
    }

    @Test
    fun `strips carriage return from text line`() {
        val frames = assembler.feed("RC:DATA,batt,74\r\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("RC:DATA,batt,74")), frames)
    }

    @Test
    fun `parses text line split across chunks`() {
        val first = assembler.feed("RC:CTRL,lx,-5".toByteArray())
        val second = assembler.feed("0,ly,100\n".toByteArray())

        assertTrue(first.isEmpty())
        assertEquals(listOf(IncomingBluetoothFrame.TextLine("RC:CTRL,lx,-50,ly,100")), second)
    }

    @Test
    fun `parses fixed size panel packet`() {
        val packet = byteArrayOf(
            0xCC.toByte(), 0x11,
            0x10, 0x00, 0x20, 0x00, 0x03,
            (0x10 + 0x20 + 0x03).toByte(),
        )

        val frames = assembler.feed(packet)

        assertEquals(listOf<IncomingBluetoothFrame>(IncomingBluetoothFrame.BinaryPacket(packet)), frames)
    }

    @Test
    fun `parses length prefixed plot packet split across chunks`() {
        // CC 33 + length(2 LE) + payload + checksum
        val payload = byteArrayOf(0x01, 0x2A, 0x3B)
        val length = payload.size
        var checksum = length and 0xFF
        payload.forEach { checksum += it.toInt() and 0xFF }
        val packet = byteArrayOf(
            0xCC.toByte(), 0x33,
            length.toByte(), 0x00,
        ) + payload + byteArrayOf((checksum and 0xFF).toByte())

        val first = assembler.feed(packet.copyOfRange(0, 3))
        val second = assembler.feed(packet.copyOfRange(3, packet.size))

        assertTrue(first.isEmpty())
        assertEquals(listOf<IncomingBluetoothFrame>(IncomingBluetoothFrame.BinaryPacket(packet)), second)
    }

    @Test
    fun `binary packet interleaved with text lines`() {
        val indicator = byteArrayOf(
            0xCC.toByte(), 0x22,
            0x2A, 0x58, 0x0F,
            (0x2A + 0x58 + 0x0F).toByte(),
        )
        val stream = "RC:ACK,app,RC\n".toByteArray() + indicator + "RC:DATA,batt,82\n".toByteArray()

        val frames = assembler.feed(stream)

        assertEquals(
            listOf(
                IncomingBluetoothFrame.TextLine("RC:ACK,app,RC"),
                IncomingBluetoothFrame.BinaryPacket(indicator),
                IncomingBluetoothFrame.TextLine("RC:DATA,batt,82"),
            ),
            frames,
        )
    }

    @Test
    fun `unknown binary subtype resyncs to text`() {
        val frames = assembler.feed(byteArrayOf(0xCC.toByte(), 0x7F) + "RC:DATA,batt,82\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("RC:DATA,batt,82")), frames)
    }
}
