package com.example.emitterapp.domain.bluetooth
import com.example.emitterapp.ui.bluetooth.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcUiState
/**
 * Pure, stateless packet encoder.
 * Keeping all bit-packing logic here makes it easy to unit-test
 * without coroutines or Android context.
 *
 * Packet layout (18 bytes):
 * [0]     0xAA  – header byte 0
 * [1]     0x55  – header byte 1
 * [2-3]   leftStickX  (12-bit LE, mapped from -100..100 → 0..4095)
 * [4-5]   leftStickY  (12-bit LE)
 * [6-7]   rightStickX (12-bit LE)
 * [8-9]   rightStickY (12-bit LE)
 * [10-11] leftKnob    (12-bit LE, range 0-1023)
 * [12-13] rightKnob   (12-bit LE, range 0-1023)
 * [14]    switches    (bitfield: switch1=bit0 … switch8=bit7)
 * [15]    checksum    (sum of bytes 2-14, low byte)
 * [16]    0x0D  – CR
 * [17]    0x0A  – LF
 */
object RcPacketEncoder {
    const val PACKET_SIZE = 18
    const val HEADER_0: Byte = 0xAA.toByte()
    const val HEADER_1: Byte = 0x55.toByte()
    const val FOOTER_CR: Byte = 0x0D.toByte()
    const val FOOTER_LF: Byte = 0x0A.toByte()
    fun buildRcPacket(state: RcUiState): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = HEADER_0
        packet[1] = HEADER_1
        pack12BitLE(packet, 2, stickTo12Bit(state.leftStickX))
        pack12BitLE(packet, 4, stickTo12Bit(state.leftStickY))
        pack12BitLE(packet, 6, stickTo12Bit(state.rightStickX))
        pack12BitLE(packet, 8, stickTo12Bit(state.rightStickY))
        pack12BitLE(packet, 10, state.leftKnobValue)
        pack12BitLE(packet, 12, state.rightKnobValue)
        var switches = 0
        if (state.switch1) switches = switches or (1 shl 0)
        if (state.switch2) switches = switches or (1 shl 1)
        if (state.switch3) switches = switches or (1 shl 2)
        if (state.switch4) switches = switches or (1 shl 3)
        if (state.switch5) switches = switches or (1 shl 4)
        if (state.switch6) switches = switches or (1 shl 5)
        if (state.switch7) switches = switches or (1 shl 6)
        if (state.switch8) switches = switches or (1 shl 7)
        packet[14] = switches.toByte()
        var checksum = 0
        for (i in 2..14) checksum += packet[i].toInt() and 0xFF
        packet[15] = (checksum and 0xFF).toByte()
        packet[16] = FOOTER_CR
        packet[17] = FOOTER_LF
        return packet
    }
    fun buildButtonPacket(event: ButtonEvent): ByteArray = byteArrayOf(
        0xBB.toByte(),
        0x66.toByte(),
        event.id,
        event.id
    )
    /** Maps stick value from [-100, 100] to [0, 4095]. */
    fun stickTo12Bit(value: Int): Int =
        (((value + 100) * 4095) / 200).coerceIn(0, 4095)
    private fun pack12BitLE(packet: ByteArray, offset: Int, value: Int) {
        packet[offset]     = (value and 0xFF).toByte()
        packet[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
