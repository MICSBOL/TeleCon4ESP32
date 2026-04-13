package com.example.emitterapp.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.domain.bluetooth.RemoteController
import com.example.emitterapp.domain.bluetooth.RemoteDevice
import com.example.emitterapp.domain.bluetooth.TelemetryState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class AndroidBluetoothController @Inject constructor(
    @ApplicationContext private val context: Context
) : RemoteController {


    private val _telemetryState = MutableStateFlow(TelemetryState())
    override val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()
    // Internal state flows using the generic RemoteDevice interface
    private val _discoveredDevices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<RemoteDevice>> = _discoveredDevices.asStateFlow()

    private val _savedDevices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    override val savedDevices: StateFlow<List<RemoteDevice>> = _savedDevices.asStateFlow()

    // Remove the old scannedDevices and pairedDevices overrides
    private val plotColorArgbs = listOf(
        0xFF00FFFF.toInt(), // Cyan
        0xFFFF0000.toInt(), // Red
        0xFF00FF00.toInt(), // Green
        0xFFFFFF00.toInt(), // Yellow
        0xFFFF00FF.toInt(), // Magenta
        0xFFFFFFFF.toInt()  // White
    )

    private val plotNameMap = mutableMapOf<Int, String>()

    private fun parseConfigPacket(bytes: ByteArray) {
        if (bytes.size < 6) {
            Log.e("BluetoothController", "Config packet too short for full header.")
            return
        }

        try {
            val payloadLength = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
            val headerAndLengthSize = 4
            val payloadStart = headerAndLengthSize

            val expectedPacketSize = headerAndLengthSize + payloadLength + 1
            if (bytes.size != expectedPacketSize) {
                Log.e("BluetoothController", "Config packet size mismatch. Expected $expectedPacketSize, but got ${bytes.size}")
                return
            }

            val checksumIndex = bytes.size - 1
            val receivedChecksum = bytes[checksumIndex]
            var calculatedChecksum = 0
            for (i in 2 until checksumIndex) {
                calculatedChecksum += bytes[i].toInt() and 0xFF
            }

            if ((calculatedChecksum and 0xFF).toByte() != receivedChecksum) {
                Log.e("BluetoothController", "Config packet checksum mismatch! Received: ${receivedChecksum.toInt() and 0xFF}, Calculated: ${calculatedChecksum and 0xFF}")
                return
            }

            var currentIndex = payloadStart
            val numSections = bytes[currentIndex++].toInt() and 0xFF

            repeat(numSections) {
                if (currentIndex + 1 >= checksumIndex) throw IOException("Incomplete section header")

                val sectionId = bytes[currentIndex++].toInt() and 0xFF
                val itemCount = bytes[currentIndex++].toInt() and 0xFF

                when (sectionId) {
                    0x01 -> {
                        val plotNames = (0 until itemCount).map {
                            if (currentIndex >= checksumIndex) throw IOException("Incomplete plot name length")
                            val nameLength = bytes[currentIndex++].toInt() and 0xFF
                            if (currentIndex + nameLength > checksumIndex) throw IOException("Incomplete plot name")
                            val name = String(bytes, currentIndex, nameLength, Charsets.UTF_8)
                            currentIndex += nameLength
                            name
                        }
                        updatePlotNames(plotNames)
                    }
                    0x02 -> {
                        val panelNames = (0 until itemCount).map {
                            if (currentIndex >= checksumIndex) throw IOException("Incomplete panel name length")
                            val nameLength = bytes[currentIndex++].toInt() and 0xFF
                            if (currentIndex + nameLength > checksumIndex) throw IOException("Incomplete panel name")
                            val name = String(bytes, currentIndex, nameLength, Charsets.UTF_8)
                            currentIndex += nameLength
                            name
                        }
                        _telemetryState.update { currentState ->
                            currentState.copy(
                                panelState = currentState.panelState.copy(
                                    leftTitle = panelNames.getOrElse(0) { currentState.panelState.leftTitle },
                                    rightTitle = panelNames.getOrElse(1) { currentState.panelState.rightTitle }
                                )
                            )
                        }
                    }
                    0x03 -> {
                        val indicatorNames = (0 until itemCount).map {
                            if (currentIndex >= checksumIndex) throw IOException("Incomplete indicator name length")
                            val nameLength = bytes[currentIndex++].toInt() and 0xFF
                            if (currentIndex + nameLength > checksumIndex) throw IOException("Incomplete indicator name")
                            val name = String(bytes, currentIndex, nameLength, Charsets.UTF_8)
                            currentIndex += nameLength
                            name
                        }
                        _telemetryState.update { currentState ->
                            currentState.copy(
                                indicatorState = currentState.indicatorState.copy(
                                    analogTitle = indicatorNames.getOrElse(0) { currentState.indicatorState.analogTitle },
                                    batteryTitle = indicatorNames.getOrElse(1) { currentState.indicatorState.batteryTitle }
                                )
                            )
                        }
                    }
                    else -> {
                        Log.w("BluetoothController", "Unknown config section ID: $sectionId")
                    }
                }
            }
            Log.d("BluetoothController", "Successfully parsed new config packet format")
        } catch (e: Exception) {
            Log.e("BluetoothController", "Error parsing new config packet: ${e.message}")
        }
    }

    private fun updatePlotNames(newNames: List<String>) {
        plotNameMap.clear()
        newNames.forEachIndexed { index, name ->
            plotNameMap[index] = name
        }

        _telemetryState.update { currentState ->
            val updatedSeries = newNames.mapIndexed { index, name ->
                val existingData = currentState.plotState.series.getOrNull(index)?.dataPoints ?: emptyList()
                val colorArgb = plotColorArgbs.getOrElse(index) { 0xFFFFFFFF.toInt() }
                PlotData(name = name, dataPoints = existingData, colorArgb = colorArgb)
            }
            currentState.copy(plotState = currentState.plotState.copy(series = updatedSeries))
        }
    }

    private fun parseIncomingPacket(bytes: ByteArray) {
        if (bytes.size < 3) return

        when {
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x11.toByte() -> parsePanelPacket(bytes)
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x22.toByte() -> {
                Log.d("BluetoothController_config", "Parsed indicator packet")
                parseIndicatorPacket(bytes)
            }
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x33.toByte() -> parsePlotPacket(bytes)
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x44.toByte() -> {
                Log.d("BluetoothController_config", "Parsed config packet")
                parseConfigPacket(bytes)
            }
        }
    }

    private fun parsePanelPacket(bytes: ByteArray) {
        if (bytes.size != 8) return

        val checksum = (bytes[2].toInt() and 0xFF) +
                (bytes[3].toInt() and 0xFF) +
                (bytes[4].toInt() and 0xFF) +
                (bytes[5].toInt() and 0xFF) +
                (bytes[6].toInt() and 0xFF)

        if ((checksum and 0xFF).toByte() != bytes[7]) {
            return
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val leftValue = buffer.getShort(2).toInt()
        val rightValue = buffer.getShort(4).toInt()
        val panelStates = buffer.get(6)
        val leftOn = (panelStates.toInt() and (1 shl 0)) != 0
        val rightOn = (panelStates.toInt() and (1 shl 1)) != 0
        val leftColorIsGreen = (panelStates.toInt() and (1 shl 2)) != 0
        val rightColorIsGreen = (panelStates.toInt() and (1 shl 3)) != 0

        _telemetryState.update { currentState ->
            currentState.copy(
                panelState = currentState.panelState.copy(
                    leftValue = leftValue,
                    rightValue = rightValue,
                    leftOn = leftOn,
                    rightOn = rightOn,
                    leftColorArgb = if (leftColorIsGreen) 0xFF00FF00.toInt() else 0xFFFF0000.toInt(),
                    rightColorArgb = if (rightColorIsGreen) 0xFF00FF00.toInt() else 0xFFFF0000.toInt()
                )
            )
        }

    }

    private fun parseIndicatorPacket(bytes: ByteArray) {
        if (bytes.size != 6) return

        var calculatedChecksum = 0
        for (i in 2 until bytes.size - 1) {
            calculatedChecksum += bytes[i].toInt() and 0xFF
        }

        if ((calculatedChecksum and 0xFF).toByte() != bytes[5]) {
            Log.e("Indicator", "Checksum mismatch")
            return
        }

        val analogValue  = bytes[2].toInt() and 0xFF
        val batteryValue = bytes[3].toInt() and 0xFF
        val ledValues    = bytes[4]

        _telemetryState.update { currentState ->
            currentState.copy(
                indicatorState = currentState.indicatorState.copy(
                    analogValue = analogValue,
                    batteryLevel = batteryValue,
                    ledValues = ledValues
                )
            )
        }
    }

    private fun parsePlotPacket(bytes: ByteArray) {
        if (bytes.size < 6) {
            Log.e("BluetoothController", "Plot packet too short for full header.")
            return
        }

        try {
            val payloadLength = (bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)
            val headerAndLengthSize = 4

            val expectedPacketSize = headerAndLengthSize + payloadLength + 1
            if (bytes.size != expectedPacketSize) {
                Log.e("BluetoothController", "Plot packet size mismatch. Expected $expectedPacketSize, but got ${bytes.size}")
                return
            }

            val checksumIndex = bytes.size - 1
            val receivedChecksum = bytes[checksumIndex]
            var calculatedChecksum = 0
            for (i in 2 until checksumIndex) {
                calculatedChecksum += bytes[i].toInt() and 0xFF
            }

            if ((calculatedChecksum and 0xFF).toByte() != receivedChecksum) {
                Log.e("BluetoothController", "Plot packet checksum mismatch! Received: ${receivedChecksum.toInt() and 0xFF}, Calculated: ${calculatedChecksum and 0xFF}")
                return
            }

            val payloadStart = headerAndLengthSize
            var currentIndex = payloadStart
            val numPlots = bytes[currentIndex++].toInt() and 0xFF

            if (payloadLength != 1 + numPlots) { 
                Log.e("BluetoothController", "Plot packet payload length does not match plot count.")
                return
            }

            val newPlotValues = (0 until numPlots).map {
                (bytes[currentIndex++].toInt() and 0xFF) / 255f
            }

            _telemetryState.update { currentState ->
                val updatedSeries = currentState.plotState.series.toMutableList()
                newPlotValues.forEachIndexed { index, value ->
                    if (index < updatedSeries.size) {
                        val oldPoints = updatedSeries[index].dataPoints.toMutableList()
                        oldPoints.add(value)
                        while (oldPoints.size > 100) {
                            oldPoints.removeAt(0)
                        }
                        updatedSeries[index] = updatedSeries[index].copy(dataPoints = oldPoints)
                    } else {
                        updatedSeries.add(
                            PlotData(
                                name = plotNameMap.getOrDefault(index, "Plot ${index + 1}"),
                                dataPoints = mutableListOf(value),
                                colorArgb = plotColorArgbs.getOrElse(index) { 0xFFFFFFFF.toInt() }
                            )
                        )
                    }
                }
                currentState.copy(
                    plotState = currentState.plotState.copy(series = updatedSeries)
                )
            }
        } catch (e: Exception) {
            Log.e("BluetoothController", "Error parsing plot packet: ${e.message}", e)
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

    private val _errors = MutableSharedFlow<String>()
    override val error: SharedFlow<String> get() = _errors.asSharedFlow()

    @Volatile
    private var isReceiverRegistered = false

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _discoveredDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    init {
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e("BluetoothController", "Missing BLUETOOTH_SCAN permission")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e("BluetoothController", "Bluetooth adapter is disabled")
            return
        }
        
        if (!isReceiverRegistered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        foundDeviceReceiver,
                        IntentFilter(BluetoothDevice.ACTION_FOUND),
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.registerReceiver(
                        foundDeviceReceiver,
                        IntentFilter(BluetoothDevice.ACTION_FOUND)
                    )
                }
                isReceiverRegistered = true
                Log.d("BluetoothController", "BroadcastReceiver registered successfully")
            } catch (e: Exception) {
                Log.e("BluetoothController", "Failed to register BroadcastReceiver: ${e.message}", e)
            }
        } else {
            Log.d("BluetoothController", "BroadcastReceiver already registered")
        }

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        
        updatePairedDevices()
        val startDiscoveryResult = bluetoothAdapter?.startDiscovery()
        Log.d("BluetoothController", "startDiscovery() called, result: $startDiscoveryResult")
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(foundDeviceReceiver)
                isReceiverRegistered = false
                Log.d("BluetoothController", "BroadcastReceiver unregistered successfully")
            } catch (e: Exception) {
                Log.e("BluetoothController", "Failed to unregister BroadcastReceiver: ${e.message}", e)
            }
        }
        
        bluetoothAdapter?.cancelDiscovery()
        Log.d("BluetoothController", "Discovery cancelled")
    }

    override fun startServer(): Flow<ConnectionResult> {
        TODO("Not yet implemented")
    }

    override fun release() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(foundDeviceReceiver)
                isReceiverRegistered = false
                Log.d("BluetoothController", "BroadcastReceiver unregistered during release")
            } catch (e: Exception) {
                Log.w("BluetoothController", "Receiver was not registered or already unregistered: ${e.message}")
            }
        }
        closeConnection()
    }

    override fun connect(device: RemoteDevice): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")

            closeConnection()
            stopDiscovery()
            // Several vendor Bluetooth stacks are timing-sensitive right after cancelDiscovery.
            delay(250L)

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            if (bluetoothDevice == null) {
                emit(ConnectionResult.Error("Device not found for address: ${device.address}"))
                return@flow
            }

            val secureSocket: BluetoothSocket = try {
                bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Failed to create socket: ${e.message}"))
                return@flow
            }

            try {
                secureSocket.connect()
                synchronized(this@AndroidBluetoothController) {
                    dataTransferService = BluetoothDataTransferService(secureSocket)
                    _isConnected.update { true }
                }
            } catch (secureError: IOException) {
                Log.w(
                    "BluetoothController",
                    "Secure RFCOMM failed, trying insecure fallback: ${secureError.message}"
                )

                try {
                    secureSocket.close()
                } catch (_: IOException) {
                }

                val insecureSocket = try {
                    bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
                } catch (e: IOException) {
                    emit(ConnectionResult.Error("Secure and insecure socket creation failed: ${e.message}"))
                    return@flow
                }

                try {
                    insecureSocket.connect()
                    synchronized(this@AndroidBluetoothController) {
                        dataTransferService = BluetoothDataTransferService(insecureSocket)
                        _isConnected.update { true }
                    }
                } catch (insecureError: IOException) {
                    emit(
                        ConnectionResult.Error(
                            "Connection failed (secure/insecure): ${secureError.message}; ${insecureError.message}"
                        )
                    )
                    try {
                        insecureSocket.close()
                    } catch (_: IOException) {
                    }
                    return@flow
                }
            }

            emit(ConnectionResult.ConnectionEstablished)
            dataTransferService!!.listenForRawBytes()
                .collect { byteArray ->
                    parseIncomingPacket(byteArray)
                }
        }.onCompletion {
            closeConnection()
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun sendMessage(message: String): BluetoothMessage? {
        TODO("Not yet implemented")
    }

    fun trySendData(data: ByteArray): Boolean? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return null
        return dataTransferService?.sendMessage(data)
    }

    fun closeConnection() {
        synchronized(this) {
            dataTransferService?.close()
            dataTransferService = null
            _isConnected.update { false }
        }
    }


    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e("BluetoothController", "Missing BLUETOOTH_CONNECT permission for paired devices")
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also { devices ->
                _savedDevices.update { devices }
                Log.d("BluetoothController", "Updated paired/saved devices: ${devices.size} devices")
            }
            ?: run {
                Log.w("BluetoothController", "No bonded devices found or bluetooth adapter unavailable")
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
    override fun disconnect() { // Rename from closeConnection
        closeConnection()
    }

    override suspend fun sendData(data: ByteArray): Boolean? { // Rename from trySendData
        return trySendData(data)
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}