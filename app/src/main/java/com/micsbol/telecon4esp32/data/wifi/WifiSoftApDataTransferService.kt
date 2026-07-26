package com.micsbol.telecon4esp32.data.wifi

import android.util.Log
import com.micsbol.telecon4esp32.data.bluetooth.BluetoothDataTransport
import com.micsbol.telecon4esp32.data.bluetooth.BluetoothFrameAssembler
import com.micsbol.telecon4esp32.data.bluetooth.IncomingBluetoothFrame
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
import java.net.InetSocketAddress
import java.net.Socket

/**
 * SoftAP TCP control link (`192.168.4.1:3333`) using the same newline / binary
 * framing as Classic SPP. Video stays on HTTP `/stream` (MJPEG) or `/capture` —
 * never on this TCP control socket.
 */
class WifiSoftApDataTransferService(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
) : BluetoothDataTransport {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var socket: Socket? = null
    private var senderJob: kotlinx.coroutines.Job? = null

    fun open(): Boolean {
        return try {
            val sock = Socket()
            sock.tcpNoDelay = true
            sock.connect(InetSocketAddress(host, port), connectTimeoutMs)
            sock.soTimeout = 0
            socket = sock
            senderJob = serviceScope.launch {
                for (data in sendChannel) {
                    try {
                        val out = socket?.getOutputStream() ?: break
                        out.write(data)
                        out.flush()
                        delay(10L)
                    } catch (_: IOException) {
                        break
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.w(TAG, "SoftAP TCP connect failed $host:$port — ${e.message}")
            close()
            false
        }
    }

    override fun listenForIncoming(): Flow<IncomingBluetoothFrame> {
        return flow {
            val sock = socket ?: return@flow
            val inputStream = sock.getInputStream()
            val assembler = BluetoothFrameAssembler()
            val buffer = ByteArray(READ_BUFFER_SIZE)

            try {
                while (serviceScope.isActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    assembler.feed(buffer, bytesRead).forEach { emit(it) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "SoftAP TCP connection lost while reading.", e)
            }
        }
    }

    override fun sendPacket(data: ByteArray): Boolean {
        val sock = socket
        if (sock == null || !sock.isConnected || sock.isClosed || sendChannel.isClosedForSend) {
            return false
        }
        return sendChannel.trySend(data).isSuccess
    }

    override fun close() {
        try {
            serviceScope.cancel()
            senderJob?.cancel()
            sendChannel.close()
            socket?.close()
        } catch (_: IOException) {
        } finally {
            socket = null
            senderJob = null
        }
    }

    companion object {
        private const val TAG = "WifiSoftApTransport"
        private const val READ_BUFFER_SIZE = 1024
        private const val CONNECT_TIMEOUT_MS = 5_000
    }
}
