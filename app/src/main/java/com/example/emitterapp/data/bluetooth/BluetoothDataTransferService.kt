package com.example.emitterapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
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

    fun listenForIncomingMessages(): Flow<BluetoothMessage> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val buffer = ByteArray(1024)
            while (true) {
                val byteCount = try {
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    break
                }
                if (byteCount <= 0) continue

                emit(
                    BluetoothMessage(
                        message = String(buffer, 0, byteCount),
                        senderName = socket.remoteDevice?.name ?: "ESP32",
                        isFromLocalUser = false
                    )
                )
            }
        }
    }

    fun listenForRawBytes(): Flow<ByteArray> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val inputStream = socket.inputStream

            try {
                while (serviceScope.isActive) {
                    // 1. Hunt for the first header byte (0xCC)
                    val header1 = inputStream.read()
                    if (header1 == -1) break // End of stream, connection lost
                    if (header1.toByte() != 0xCC.toByte()) continue // Not our packet, keep looking

                    // 2. Found 0xCC, now read the second header byte to identify the packet
                    val header2 = inputStream.read()
                    if (header2 == -1) break

                    when (header2.toByte()) {
                        // --- New Config Packet (Variable Length) ---
                        0x44.toByte() -> {
                            val payloadLength = inputStream.read()
                            if (payloadLength == -1) break

                            // Total packet size = Header(2) + Length(1) + Payload
                            val fullPacketSize = 3 + payloadLength
                            val packet = ByteArray(fullPacketSize)
                            packet[0] = 0xCC.toByte()
                            packet[1] = 0x44.toByte()
                            packet[2] = payloadLength.toByte()

                            // Read the entire payload into the rest of the buffer
                            var bytesRead = 0
                            while (bytesRead < payloadLength) {
                                val readResult = inputStream.read(
                                    packet,
                                    3 + bytesRead,
                                    payloadLength - bytesRead
                                )
                                if (readResult == -1) throw IOException("Stream ended while reading config payload")
                                bytesRead += readResult
                            }
                            emit(packet)
                        }

                        // --- Old Fixed-Size Packets ---
                        0x11.toByte() -> { // Panel Packet (8 bytes total)
                            val packet = ByteArray(8)
                            packet[0] = 0xCC.toByte()
                            packet[1] = 0x11.toByte()
                            inputStream.read(packet, 2, 6) // Read remaining 6 bytes
                            emit(packet)
                        }

                        0x22.toByte() -> { // Indicator Packet (5 bytes total)
                            val packet = ByteArray(5)
                            packet[0] = 0xCC.toByte()
                            packet[1] = 0x22.toByte()
                            inputStream.read(packet, 2, 3) // Read remaining 3 bytes
                            emit(packet)
                        }

                        // IMPORTANT NOTE: Your Plot Packet (0x33) is also variable-length but lacks a length field.
                        // This new reader will likely fail to parse it correctly. You should update your
                        // ESP32 code to send the plot packet with a length field, just like the config packet.
                        0x33.toByte() -> {
                            // This is a temporary, best-effort read for the plot packet.
                            // It is not robust and should be updated.
                            val tempBuffer = ByteArray(1024)
                            tempBuffer[0] = 0xCC.toByte()
                            tempBuffer[1] = 0x33.toByte()
                            // Try to read a reasonable number of bytes
                            val bytesRead = inputStream.read(tempBuffer, 2, 256)
                            if (bytesRead > 0) {
                                emit(tempBuffer.copyOf(bytesRead + 2))
                            }
                        }

                        else -> {
                            Log.w(
                                "DataTransferService",
                                "Unknown second header byte: ${header2.toByte()}"
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("DataTransferService", "Connection lost while reading.", e)
            }
        }
    }

    fun sendMessage(data: ByteArray): Boolean {
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
}