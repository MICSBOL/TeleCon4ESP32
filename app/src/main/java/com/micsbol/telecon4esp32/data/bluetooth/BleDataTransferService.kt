package com.micsbol.telecon4esp32.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * BLE counterpart of [BluetoothDataTransferService].
 *
 * Implements a Nordic UART Service (NUS) client: the ESP32 exposes an RX characteristic
 * the phone writes protocol bytes to and a TX characteristic that notifies telemetry back.
 * The byte stream is identical to the Classic SPP wire format, so incoming notifications
 * are reassembled with the shared [BluetoothFrameAssembler].
 */
@SuppressLint("MissingPermission")
class BleDataTransferService(
    private val context: Context,
    private val device: BluetoothDevice,
) : BluetoothDataTransport {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val incoming = Channel<ByteArray>(Channel.UNLIMITED)
    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val writeAck = Channel<Unit>(Channel.CONFLATED)
    private val sessionReady = CompletableDeferred<Boolean>()

    @Volatile
    private var gatt: BluetoothGatt? = null

    @Volatile
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    private var maxWriteSize = DEFAULT_MAX_WRITE_SIZE

    @Volatile
    var isConnected = false
        private set

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected; discovering services")
                    g.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected (status $status)")
                    isConnected = false
                    sessionReady.complete(false)
                    incoming.close()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                sessionReady.complete(false)
                return
            }
            val service = g.getService(NUS_SERVICE_UUID)
            val rx = service?.getCharacteristic(NUS_RX_CHAR_UUID)
            val tx = service?.getCharacteristic(NUS_TX_CHAR_UUID)
            if (service == null || rx == null || tx == null) {
                Log.e(TAG, "NUS service/characteristics not found on ${device.address}")
                sessionReady.complete(false)
                return
            }
            rxCharacteristic = rx
            // MTU first; notifications are enabled from onMtuChanged.
            if (!g.requestMtu(REQUESTED_MTU)) {
                enableTelemetryNotifications(g, tx)
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                maxWriteSize = mtu - ATT_WRITE_OVERHEAD
            }
            val tx = g.getService(NUS_SERVICE_UUID)?.getCharacteristic(NUS_TX_CHAR_UUID)
            if (tx != null) {
                enableTelemetryNotifications(g, tx)
            } else {
                sessionReady.complete(false)
            }
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.uuid == CCCD_UUID) {
                isConnected = status == BluetoothGatt.GATT_SUCCESS
                sessionReady.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            writeAck.trySend(Unit)
        }

        // Pre-API 33 callback; value is read from the characteristic.
        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                characteristic.value?.let { incoming.trySend(it.copyOf()) }
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            incoming.trySend(value.copyOf())
        }
    }

    private fun enableTelemetryNotifications(
        g: BluetoothGatt,
        tx: BluetoothGattCharacteristic,
    ) {
        if (!g.setCharacteristicNotification(tx, true)) {
            sessionReady.complete(false)
            return
        }
        val cccd = tx.getDescriptor(CCCD_UUID)
        if (cccd == null) {
            // No CCCD — some stacks still notify; treat the session as ready.
            isConnected = true
            sessionReady.complete(true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }
    }

    /**
     * Connects and prepares the NUS session (service discovery, MTU, notifications).
     * Returns true when the link is ready for protocol traffic.
     */
    suspend fun open(timeoutMs: Long = OPEN_TIMEOUT_MS): Boolean {
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        val ready = withTimeoutOrNull(timeoutMs) { sessionReady.await() } == true
        if (!ready) {
            close()
            return false
        }
        startSender()
        return true
    }

    private fun startSender() {
        serviceScope.launch {
            for (data in sendChannel) {
                var offset = 0
                while (offset < data.size) {
                    val end = minOf(offset + maxWriteSize, data.size)
                    if (!writeChunk(data.copyOfRange(offset, end))) {
                        Log.w(TAG, "BLE write failed; dropping remainder of packet")
                        break
                    }
                    offset = end
                }
            }
        }
    }

    private suspend fun writeChunk(chunk: ByteArray): Boolean {
        val g = gatt ?: return false
        val rx = rxCharacteristic ?: return false
        val writeType = if (rx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(rx, chunk, writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            rx.writeType = writeType
            @Suppress("DEPRECATION")
            rx.value = chunk
            @Suppress("DEPRECATION")
            g.writeCharacteristic(rx)
        }
        if (!started) return false

        // The stack signals readiness for the next write via onCharacteristicWrite.
        withTimeoutOrNull(WRITE_ACK_TIMEOUT_MS) { writeAck.receive() }
        return true
    }

    override fun listenForIncoming(): Flow<IncomingBluetoothFrame> = flow {
        val assembler = BluetoothFrameAssembler()
        for (chunk in incoming) {
            assembler.feed(chunk).forEach { emit(it) }
        }
    }

    override fun sendPacket(data: ByteArray): Boolean {
        if (!isConnected || sendChannel.isClosedForSend) return false
        return sendChannel.trySend(data).isSuccess
    }

    override fun close() {
        isConnected = false
        serviceScope.cancel()
        incoming.close()
        try {
            gatt?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to close GATT: ${e.message}")
        }
        gatt = null
        rxCharacteristic = null
    }

    companion object {
        private const val TAG = "BleDataTransferService"

        /** Nordic UART Service — the de facto BLE serial bridge on ESP32. */
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

        /** Phone → ESP32 writes. */
        val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

        /** ESP32 → phone notifications. */
        val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private const val REQUESTED_MTU = 247
        private const val ATT_WRITE_OVERHEAD = 3
        private const val DEFAULT_MAX_WRITE_SIZE = 20
        private const val OPEN_TIMEOUT_MS = 10_000L
        private const val WRITE_ACK_TIMEOUT_MS = 500L
    }
}
