package com.micsbol.telecon4esp32.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

@SuppressLint("MissingPermission")
class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val senderJob = serviceScope.launch {
        for (data in sendChannel) {
            try {
                if (socket.isConnected) {
                    socket.outputStream.write(data)
                    delay(10L)
                }
            } catch (e: IOException) {
                break
            }
        }
    }

    fun listenForIncoming(): Flow<IncomingBluetoothFrame> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val inputStream = socket.inputStream
            val lineBuffer = StringBuilder()

            try {
                while (serviceScope.isActive) {
                    val raw = inputStream.read()
                    if (raw == -1) break
                    val byte = raw.toByte()

                    when {
                        byte == BINARY_HEADER -> {
                            lineBuffer.setLength(0)
                            readBinaryFrame(inputStream, byte)?.let { emit(it) }
                        }

                        byte == LINE_FEED || raw == '\n'.code -> {
                            if (lineBuffer.isNotEmpty()) {
                                val line = lineBuffer.toString().trimEnd(CARRIAGE_RETURN)
                                lineBuffer.setLength(0)
                                if (line.isNotEmpty()) {
                                    emit(IncomingBluetoothFrame.TextLine(line))
                                }
                            }
                        }

                        raw == CARRIAGE_RETURN.code -> Unit

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
            } catch (e: IOException) {
                Log.e(TAG, "Connection lost while reading.", e)
            }
        }
    }

    private fun readBinaryFrame(
        inputStream: java.io.InputStream,
        header1: Byte,
    ): IncomingBluetoothFrame.BinaryPacket? {
        val header2Raw = inputStream.read()
        if (header2Raw == -1) return null
        val header2 = header2Raw.toByte()

        return when (header2) {
            0x44.toByte(), 0x33.toByte() -> {
                val len1 = inputStream.read()
                val len2 = inputStream.read()
                if (len1 == -1 || len2 == -1) return null

                val payloadLength = (len1 and 0xFF) or ((len2 and 0xFF) shl 8)
                val bytesToRead = payloadLength + 1
                val payloadAndChecksum = ByteArray(bytesToRead)
                var bytesRead = 0
                while (bytesRead < bytesToRead) {
                    val readResult = inputStream.read(
                        payloadAndChecksum,
                        bytesRead,
                        bytesToRead - bytesRead,
                    )
                    if (readResult == -1) throw IOException("Stream ended while reading payload")
                    bytesRead += readResult
                }

                val fullPacket = ByteArray(4 + bytesToRead)
                fullPacket[0] = header1
                fullPacket[1] = header2
                fullPacket[2] = len1.toByte()
                fullPacket[3] = len2.toByte()
                payloadAndChecksum.copyInto(fullPacket, 4)
                IncomingBluetoothFrame.BinaryPacket(fullPacket)
            }

            0x11.toByte() -> {
                val packet = ByteArray(8)
                packet[0] = header1
                packet[1] = header2
                inputStream.read(packet, 2, 6)
                IncomingBluetoothFrame.BinaryPacket(packet)
            }

            0x22.toByte() -> {
                val packet = ByteArray(6)
                packet[0] = header1
                packet[1] = header2
                var bytesRead = 0
                while (bytesRead < 4) {
                    val result = inputStream.read(packet, 2 + bytesRead, 4 - bytesRead)
                    if (result == -1) throw IOException("Stream ended while reading indicator packet")
                    bytesRead += result
                }
                IncomingBluetoothFrame.BinaryPacket(packet)
            }

            else -> {
                Log.w(TAG, "Unknown second header byte: $header2")
                null
            }
        }
    }

    fun sendPacket(data: ByteArray): Boolean {
        if (!socket.isConnected || sendChannel.isClosedForSend) return false
        return sendChannel.trySend(data).isSuccess
    }

    fun close() {
        try {
            serviceScope.cancel()
            socket.close()
        } catch (e: IOException) {
        }
    }

    companion object {
        private const val TAG = "DataTransferService"
        private const val BINARY_HEADER = 0xCC.toByte()
        private const val LINE_FEED = '\n'.code.toByte()
        private const val CARRIAGE_RETURN = '\r'
        private val PRINTABLE_ASCII_RANGE = 32..126
        private const val MAX_LINE_LENGTH = 512
    }
}
