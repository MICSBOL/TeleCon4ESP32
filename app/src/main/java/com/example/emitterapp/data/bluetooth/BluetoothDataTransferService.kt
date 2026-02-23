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
                    val header1 = inputStream.read()
                    if (header1 == -1) break
                    if (header1.toByte() != 0xCC.toByte()) continue

                    val header2 = inputStream.read()
                    if (header2 == -1) break

                    when (header2.toByte()) {
                        // THIS IS THE MAIN FIX: Handling the new variable-length format
                        0x44.toByte(), 0x33.toByte() -> { // Assuming plot packet (0x33) will also adopt this format
                            // 1. Read the two bytes for the length
                            val len1 = inputStream.read()
                            val len2 = inputStream.read()
                            if (len1 == -1 || len2 == -1) break

                            // 2. Reconstruct the 16-bit little-endian length
                            val payloadLength = (len1 and 0xFF) or ((len2 and 0xFF) shl 8)

                            // 3. Read the entire rest of the packet (payload + checksum)
                            val bytesToRead = payloadLength + 1 // +1 for the checksum byte
                            val payloadAndChecksum = ByteArray(bytesToRead)
                            var bytesRead = 0
                            while (bytesRead < bytesToRead) {
                                val readResult = inputStream.read(payloadAndChecksum, bytesRead, bytesToRead - bytesRead)
                                if (readResult == -1) throw IOException("Stream ended while reading payload")
                                bytesRead += readResult
                            }

                            // 4. Assemble the complete packet to be sent for parsing
                            val fullPacket = ByteArray(4 + bytesToRead)
                            fullPacket[0] = 0xCC.toByte()
                            fullPacket[1] = header2.toByte()
                            fullPacket[2] = len1.toByte()
                            fullPacket[3] = len2.toByte()
                            payloadAndChecksum.copyInto(fullPacket, 4)

                            emit(fullPacket)
                        }

                        // Your fixed-size packets remain the same
                        0x11.toByte() -> {
                            val packet = ByteArray(8)
                            packet[0] = 0xCC.toByte()
                            packet[1] = 0x11.toByte()
                            inputStream.read(packet, 2, 6)
                            emit(packet)
                        }
                        0x22.toByte() -> {
                            val packet = ByteArray(5)
                            packet[0] = 0xCC.toByte()
                            packet[1] = 0x22.toByte()
                            inputStream.read(packet, 2, 3)
                            emit(packet)
                        }

                        else -> {
                            Log.w("DataTransferService", "Unknown second header byte: ${header2.toByte()}")
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