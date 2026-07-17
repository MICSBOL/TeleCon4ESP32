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
) : BluetoothDataTransport {
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

    override fun listenForIncoming(): Flow<IncomingBluetoothFrame> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val inputStream = socket.inputStream
            val assembler = BluetoothFrameAssembler()
            val buffer = ByteArray(READ_BUFFER_SIZE)

            try {
                while (serviceScope.isActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    assembler.feed(buffer, bytesRead).forEach { emit(it) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection lost while reading.", e)
            }
        }
    }

    override fun sendPacket(data: ByteArray): Boolean {
        if (!socket.isConnected || sendChannel.isClosedForSend) return false
        return sendChannel.trySend(data).isSuccess
    }

    override fun close() {
        try {
            serviceScope.cancel()
            socket.close()
        } catch (e: IOException) {
        }
    }

    companion object {
        private const val TAG = "DataTransferService"
        private const val READ_BUFFER_SIZE = 1024
    }
}
