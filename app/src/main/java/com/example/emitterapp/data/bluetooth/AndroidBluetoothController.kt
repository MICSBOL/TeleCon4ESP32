package com.example.emitterapp.data.bluetooth


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.example.emitterapp.domain.bluetooth.BluetoothController
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.TelemetryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val _telemetryState = MutableStateFlow(TelemetryState())
    override val telemetryState: StateFlow<TelemetryState>
        get() = _telemetryState.asStateFlow()

    private fun parseTelemetryPacket(bytes: ByteArray) {
        if (bytes.size < 10) return
        if (bytes[0] != 0xCC.toByte() || bytes[1] != 0xDD.toByte()) return

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val leftValue = buffer.getShort(2).toInt()
        val rightValue = buffer.getShort(4).toInt()
        val panelStates = buffer.get(6)

        val analogValue = buffer.get(7).toInt() and 0xFF
        val batteryValue = buffer.get(8).toInt() and 0xFF

        val checksum = buffer.get(9)

        var calculatedChecksum = 0
        for(i in 2..8){
            calculatedChecksum += bytes[i].toInt() and 0xFF
        }

        if ((calculatedChecksum and 0xFF).toByte() != checksum) {
            return
        }

        val leftOn = (panelStates.toInt() and (1 shl 0)) != 0
        val rightOn = (panelStates.toInt() and (1 shl 1)) != 0
        val leftColorIsGreen = (panelStates.toInt() and (1 shl 2)) != 0
        val rightColorIsGreen = (panelStates.toInt() and (1 shl 3)) != 0

        _telemetryState.update {
            it.copy(
                leftPanelValue = leftValue,
                rightPanelValue = rightValue,
                leftPanelOn = leftOn,
                rightPanelOn = rightOn,
                leftPanelColor = if (leftColorIsGreen) Color.Green else Color.Red,
                rightPanelColor = if (rightColorIsGreen) Color.Green else Color.Red,
                analogIndicatorValue = analogValue,
                batteryLevel = batteryValue
            )
        }
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    @Volatile
    private var dataTransferService: BluetoothDataTransferService? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val error: SharedFlow<String> get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    init {
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        try {
            context.registerReceiver(
                foundDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        } catch (e: Exception) { /* Already registered */
        }
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
        try {
            context.unregisterReceiver(foundDeviceReceiver)
        } catch (e: Exception) { /* Not registered */
        }
        closeConnection()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")

            closeConnection()
            stopDiscovery()

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            val socket: BluetoothSocket? = try {
                bluetoothDevice?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Failed to create socket: ${e.message}"))
                return@flow
            }

            try {
                socket?.connect()
                if (socket != null) {
                    synchronized(this@AndroidBluetoothController) {
                        dataTransferService = BluetoothDataTransferService(socket)
                        _isConnected.update { true }
                    }
                    emit(ConnectionResult.ConnectionEstablished)
                    dataTransferService!!.listenForRawBytes()
                        .collect { byteArray ->
                            parseTelemetryPacket(byteArray)
                        }
                }
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Connection failed: ${e.message}"))
                socket?.close()
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> = emptyFlow()


    override suspend fun trySendData(data: ByteArray): Boolean? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return null
        return dataTransferService?.sendMessage(data)
    }

    override fun closeConnection() {
        synchronized(this) {
            dataTransferService?.close()
            dataTransferService = null
            _isConnected.update { false }
        }
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? = null

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also { devices -> _pairedDevices.update { devices } }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}