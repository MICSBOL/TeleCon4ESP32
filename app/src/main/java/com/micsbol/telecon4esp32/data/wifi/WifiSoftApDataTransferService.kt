package com.micsbol.telecon4esp32.data.wifi

import android.util.Log
import com.micsbol.telecon4esp32.data.bluetooth.BluetoothDataTransport
import com.micsbol.telecon4esp32.data.bluetooth.BluetoothFrameAssembler
import com.micsbol.telecon4esp32.data.bluetooth.IncomingBluetoothFrame
import com.micsbol.telecon4esp32.data.camera.SoftApNetworkResolver
import com.micsbol.telecon4esp32.data.camera.SoftApWifiLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

/**
 * SoftAP TCP control link (`192.168.4.1:3333`) using the same newline / binary
 * framing as Classic SPP. Video stays on HTTP `/stream` (MJPEG) or `/capture` —
 * never on this TCP control socket.
 *
 * Opens the socket on the SoftAP [android.net.Network] when available (MIUI /
 * dual-SIM). CTRL payloads are coalesced (latest wins) so video load cannot grow
 * an unlimited send backlog.
 */
class WifiSoftApDataTransferService(
    private val host: String,
    private val port: Int,
    private val softApNetworkResolver: SoftApNetworkResolver,
    private val softApWifiLock: SoftApWifiLock? = null,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
) : BluetoothDataTransport {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Non-CTRL traffic (CONNECT, BTN, DATA, …). */
    private val otherChannel = Channel<ByteArray>(Channel.UNLIMITED)
    /** Latest CTRL only — older stick samples are dropped under load. */
    private val latestCtrl = AtomicReference<ByteArray?>(null)
    private val workSignal = Channel<Unit>(Channel.CONFLATED)
    private var socket: Socket? = null
    private var senderJob: kotlinx.coroutines.Job? = null
    private var wifiLockHeld = false

    fun open(): Boolean {
        return try {
            val sock = softApNetworkResolver.openTcpSocket(host, port, connectTimeoutMs)
            socket = sock
            acquireWifiLockIfNeeded()
            senderJob = serviceScope.launch {
                while (isActive) {
                    val other = otherChannel.tryReceive().getOrNull()
                    if (other != null) {
                        if (!writeAndFlush(other)) break
                        continue
                    }
                    val ctrl = latestCtrl.getAndSet(null)
                    if (ctrl != null) {
                        if (!writeAndFlush(ctrl)) break
                        continue
                    }
                    // Idle until CONNECT/BTN/DATA or a newer CTRL arrives.
                    if (workSignal.receiveCatching().isClosed) break
                }
            }
            Log.d(TAG, "SoftAP TCP open $host:$port (CTRL coalesce, no post-flush delay)")
            true
        } catch (e: IOException) {
            Log.w(TAG, "SoftAP TCP connect failed $host:$port — ${e.message}")
            close()
            false
        }
    }

    private fun writeAndFlush(data: ByteArray): Boolean {
        return try {
            val out = socket?.getOutputStream() ?: return false
            out.write(data)
            out.flush()
            true
        } catch (_: IOException) {
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
        if (sock == null || !sock.isConnected || sock.isClosed) {
            return false
        }
        return if (isCtrlPayload(data)) {
            latestCtrl.set(data)
            workSignal.trySend(Unit)
            true
        } else {
            val ok = otherChannel.trySend(data).isSuccess
            if (ok) workSignal.trySend(Unit)
            ok
        }
    }

    override fun close() {
        try {
            serviceScope.cancel()
            senderJob?.cancel()
            otherChannel.close()
            workSignal.close()
            latestCtrl.set(null)
            socket?.close()
        } catch (_: IOException) {
        } finally {
            socket = null
            senderJob = null
            releaseWifiLockIfHeld()
        }
    }

    private fun acquireWifiLockIfNeeded() {
        val lock = softApWifiLock ?: return
        if (wifiLockHeld) return
        lock.acquire(HOLDER_TCP)
        wifiLockHeld = true
    }

    private fun releaseWifiLockIfHeld() {
        val lock = softApWifiLock ?: return
        if (!wifiLockHeld) return
        lock.release(HOLDER_TCP)
        wifiLockHeld = false
    }

    companion object {
        private const val TAG = "WifiSoftApTransport"
        private const val READ_BUFFER_SIZE = 1024
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val HOLDER_TCP = "tcp"

        private fun isCtrlPayload(data: ByteArray): Boolean {
            // Text "RC:CTRL" — stick heartbeat; safe to drop older samples.
            if (data.size >= 7 &&
                data[0] == 'R'.code.toByte() &&
                data[1] == 'C'.code.toByte() &&
                data[2] == ':'.code.toByte() &&
                data[3] == 'C'.code.toByte() &&
                data[4] == 'T'.code.toByte() &&
                data[5] == 'R'.code.toByte() &&
                data[6] == 'L'.code.toByte()
            ) {
                return true
            }
            // Binary AA 55 RC state — same coalesce policy for DevKit WIFI_BINARY.
            return data.size >= 2 &&
                data[0] == 0xAA.toByte() &&
                data[1] == 0x55.toByte()
        }
    }
}
