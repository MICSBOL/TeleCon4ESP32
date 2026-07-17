package com.micsbol.telecon4esp32.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothFrameAssemblerTest {

    private val assembler = BluetoothFrameAssembler()

    @Test
    fun `parses text line terminated by newline`() {
        val frames = assembler.feed("GH:DATA,temp,26.2,hum,68\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("GH:DATA,temp,26.2,hum,68")), frames)
    }

    @Test
    fun `strips carriage return from text line`() {
        val frames = assembler.feed("WT:DATA,level,74\r\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("WT:DATA,level,74")), frames)
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
    fun `parses length prefixed gh packet split across chunks`() {
        // CC 47 + length(2 LE) + type + 2-byte payload + checksum
        val payload = byteArrayOf(0x01, 0x2A, 0x3B)
        val length = payload.size
        var checksum = length and 0xFF
        payload.forEach { checksum += it.toInt() and 0xFF }
        val packet = byteArrayOf(
            0xCC.toByte(), 0x47,
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
        val stream = "GH:ACK,app,GH\n".toByteArray() + indicator + "GH:DATA,temp,25\n".toByteArray()

        val frames = assembler.feed(stream)

        assertEquals(
            listOf(
                IncomingBluetoothFrame.TextLine("GH:ACK,app,GH"),
                IncomingBluetoothFrame.BinaryPacket(indicator),
                IncomingBluetoothFrame.TextLine("GH:DATA,temp,25"),
            ),
            frames,
        )
    }

    @Test
    fun `unknown binary subtype resyncs to text`() {
        val frames = assembler.feed(byteArrayOf(0xCC.toByte(), 0x7F) + "SP:DATA,solar_w,480\n".toByteArray())

        assertEquals(listOf(IncomingBluetoothFrame.TextLine("SP:DATA,solar_w,480")), frames)
    }
}
