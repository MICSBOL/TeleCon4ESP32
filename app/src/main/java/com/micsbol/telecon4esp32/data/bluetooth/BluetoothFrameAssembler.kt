package com.micsbol.telecon4esp32.data.bluetooth

import android.util.Log

/**
 * Incremental parser that turns a raw ESP32 byte stream into [IncomingBluetoothFrame]s.
 *
 * The same wire format flows over Classic SPP sockets and BLE notifications, so both
 * transports share this state machine. Feed bytes in any chunk size; frames are emitted
 * as soon as they are complete.
 *
 * Wire families (see docs/SIMPLE_PROTOCOL_ESP32.md and docs/BINARY_PROTOCOL_GH.md):
 * - Text lines terminated by `\n` (`APP:TYPE,key,value,...`)
 * - Binary frames starting with `0xCC`: fixed-size panel (`0x11`, 8 B) and indicator
 *   (`0x22`, 6 B) packets, and length-prefixed plot/config/GH packets (`0x33`, `0x44`, `0x47`)
 */
class BluetoothFrameAssembler {

    private enum class State {
        TEXT,
        AWAITING_SUBTYPE,
        BINARY_FIXED,
        BINARY_LENGTH_HEADER,
        BINARY_PAYLOAD,
    }

    private var state = State.TEXT
    private val lineBuffer = StringBuilder()
    private var packetBuffer = ByteArray(0)
    private var packetFill = 0
    private var expectedPacketSize = 0

    /** Consumes [chunk] and returns every frame completed by it (possibly none). */
    fun feed(chunk: ByteArray, length: Int = chunk.size): List<IncomingBluetoothFrame> {
        val frames = mutableListOf<IncomingBluetoothFrame>()
        for (i in 0 until length) {
            feedByte(chunk[i], frames)
        }
        return frames
    }

    private fun feedByte(byte: Byte, frames: MutableList<IncomingBluetoothFrame>) {
        when (state) {
            State.TEXT -> feedTextByte(byte, frames)
            State.AWAITING_SUBTYPE -> feedSubtypeByte(byte)
            State.BINARY_FIXED, State.BINARY_PAYLOAD -> feedPacketByte(byte, frames)
            State.BINARY_LENGTH_HEADER -> feedLengthHeaderByte(byte)
        }
    }

    private fun feedTextByte(byte: Byte, frames: MutableList<IncomingBluetoothFrame>) {
        val raw = byte.toInt() and 0xFF
        when {
            byte == BINARY_HEADER -> {
                lineBuffer.setLength(0)
                state = State.AWAITING_SUBTYPE
            }

            raw == '\n'.code -> {
                if (lineBuffer.isNotEmpty()) {
                    val line = lineBuffer.toString().trimEnd('\r')
                    lineBuffer.setLength(0)
                    if (line.isNotEmpty()) {
                        frames += IncomingBluetoothFrame.TextLine(line)
                    }
                }
            }

            raw == '\r'.code -> Unit

            raw in PRINTABLE_ASCII_RANGE -> {
                if (lineBuffer.length >= MAX_LINE_LENGTH) {
                    lineBuffer.setLength(0)
                    Log.w(TAG, "Discarding overlong line buffer")
                }
                lineBuffer.append(raw.toChar())
            }

            else -> Log.w(TAG, "Skipping unexpected byte: 0x${raw.toString(16)}")
        }
    }

    private fun feedSubtypeByte(byte: Byte) {
        when (byte) {
            SUBTYPE_PANEL -> startFixedPacket(byte, PANEL_PACKET_SIZE)
            SUBTYPE_INDICATOR -> startFixedPacket(byte, INDICATOR_PACKET_SIZE)
            SUBTYPE_PLOT, SUBTYPE_CONFIG,
            GH_INBOUND_APP_BYTE, WT_INBOUND_APP_BYTE, SP_INBOUND_APP_BYTE,
            SH_INBOUND_APP_BYTE, DL_INBOUND_APP_BYTE, LT_INBOUND_APP_BYTE,
            -> {
                packetBuffer = ByteArray(LENGTH_HEADER_SIZE)
                packetBuffer[0] = BINARY_HEADER
                packetBuffer[1] = byte
                packetFill = 2
                state = State.BINARY_LENGTH_HEADER
            }

            else -> {
                Log.w(TAG, "Unknown second header byte: $byte")
                state = State.TEXT
            }
        }
    }

    private fun startFixedPacket(subtype: Byte, size: Int) {
        packetBuffer = ByteArray(size)
        packetBuffer[0] = BINARY_HEADER
        packetBuffer[1] = subtype
        packetFill = 2
        expectedPacketSize = size
        state = State.BINARY_FIXED
    }

    private fun feedLengthHeaderByte(byte: Byte) {
        packetBuffer[packetFill++] = byte
        if (packetFill < LENGTH_HEADER_SIZE) return

        val payloadLength = (packetBuffer[2].toInt() and 0xFF) or
            ((packetBuffer[3].toInt() and 0xFF) shl 8)
        if (payloadLength > MAX_BINARY_PAYLOAD) {
            Log.w(TAG, "Discarding binary frame with implausible length $payloadLength")
            state = State.TEXT
            return
        }
        // Header (2) + length (2) + payload + checksum (1).
        expectedPacketSize = LENGTH_HEADER_SIZE + payloadLength + 1
        packetBuffer = packetBuffer.copyOf(expectedPacketSize)
        state = State.BINARY_PAYLOAD
    }

    private fun feedPacketByte(byte: Byte, frames: MutableList<IncomingBluetoothFrame>) {
        packetBuffer[packetFill++] = byte
        if (packetFill == expectedPacketSize) {
            frames += IncomingBluetoothFrame.BinaryPacket(packetBuffer)
            packetBuffer = ByteArray(0)
            packetFill = 0
            state = State.TEXT
        }
    }

    companion object {
        private const val TAG = "BtFrameAssembler"
        private const val BINARY_HEADER = 0xCC.toByte()
        private const val SUBTYPE_PANEL = 0x11.toByte()
        private const val SUBTYPE_INDICATOR = 0x22.toByte()
        private const val SUBTYPE_PLOT = 0x33.toByte()
        private const val SUBTYPE_CONFIG = 0x44.toByte()

        /** Greenhouse inbound telemetry (`CC 47`). */
        private const val GH_INBOUND_APP_BYTE = 0x47.toByte()
        private const val WT_INBOUND_APP_BYTE = 0x57.toByte()
        private const val SP_INBOUND_APP_BYTE = 0x53.toByte()
        private const val SH_INBOUND_APP_BYTE = 0x48.toByte()
        /** Door lock uses `K` to avoid colliding with RC config (`CC 44`). */
        private const val DL_INBOUND_APP_BYTE = 0x4B.toByte()
        private const val LT_INBOUND_APP_BYTE = 0x4C.toByte()

        private const val PANEL_PACKET_SIZE = 8
        private const val INDICATOR_PACKET_SIZE = 6
        private const val LENGTH_HEADER_SIZE = 4
        private const val MAX_BINARY_PAYLOAD = 2048
        private const val MAX_LINE_LENGTH = 512
        private val PRINTABLE_ASCII_RANGE = 32..126
    }
}
