package com.example.emitterapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
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
            if(!socket.isConnected){
                return@flow
            }
            val buffer = ByteArray(1024)
            while (serviceScope.isActive){
                val byteCount = try {
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    break
                }
                if(byteCount > 0){
                    val receivedBytes = buffer.copyOf(byteCount)
                    emit(receivedBytes)
                }
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