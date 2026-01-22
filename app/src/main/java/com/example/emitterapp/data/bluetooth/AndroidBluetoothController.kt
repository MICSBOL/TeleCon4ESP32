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
import androidx.compose.ui.graphics.Color
import com.example.emitterapp.domain.bluetooth.BluetoothController
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.IndicatorState
import com.example.emitterapp.domain.bluetooth.PanelState
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.domain.bluetooth.PlotState
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
    private val _panelState = MutableStateFlow(PanelState())
    override val panelState: StateFlow<PanelState> = _panelState.asStateFlow()

    private val _indicatorState = MutableStateFlow(IndicatorState())
    override val indicatorState: StateFlow<IndicatorState> = _indicatorState.asStateFlow()

    private val _plotState = MutableStateFlow(PlotState())
    override val plotState: StateFlow<PlotState> = _plotState.asStateFlow()

    private val plotColors =
        listOf(Color.Cyan, Color.Red, Color.Green, Color.Yellow, Color.Magenta, Color.White)

    private val plotNameMap = mutableMapOf<Int, String>()

    private fun parseConfigPacket(bytes: ByteArray) {
        if (bytes.size < 4) return
        val numPlots = bytes[2].toInt() and 0xFF

        plotNameMap.clear()

        var currentIndex = 3
        for (i in 0 until numPlots) {
            if (currentIndex >= bytes.size) {
                Log.e("BluetoothController", "Config packet parsing error: index out of bounds for name length.")
                return
            }
            val nameLength = bytes[currentIndex].toInt() and 0xFF
            currentIndex++

            if (currentIndex + nameLength > bytes.size) {
                Log.e("BluetoothController", "Config packet parsing error: index out of bounds for name bytes.")
                return
            }
            val nameBytes = bytes.sliceArray(currentIndex until currentIndex + nameLength)
            val name = String(nameBytes, Charsets.UTF_8)
            plotNameMap[i] = name

            currentIndex += nameLength
        }

        Log.d("BluetoothController", "Parsed config, names updated: $plotNameMap")
    }

    private fun parseIncomingPacket(bytes: ByteArray) {
        if (bytes.size < 3) return

        when {
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x11.toByte() -> parsePanelPacket(bytes)
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x22.toByte() -> parseIndicatorPacket(bytes)
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x33.toByte() -> parsePlotPacket(bytes)
            bytes[0] == 0xCC.toByte() && bytes[1] == 0x44.toByte() -> parseConfigPacket(bytes)

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

        _panelState.update {
            it.copy(
                leftValue = leftValue,
                rightValue = rightValue,
                leftOn = leftOn,
                rightOn = rightOn,
                leftColor = if (leftColorIsGreen) Color.Green else Color.Red,
                rightColor = if (rightColorIsGreen) Color.Green else Color.Red
            )
        }
    }

    private fun parseIndicatorPacket(bytes: ByteArray) {
        if (bytes.size != 5) return

        val checksum = (bytes[2].toInt() and 0xFF) + (bytes[3].toInt() and 0xFF)

        if ((checksum and 0xFF).toByte() != bytes[4]) {
            return
        }

        val analogValue = bytes[2].toInt() and 0xFF
        val batteryValue = bytes[3].toInt() and 0xFF

        _indicatorState.update {
            it.copy(
                analogValue = analogValue,
                batteryLevel = batteryValue
            )
        }
    }

    private fun parsePlotPacket(bytes: ByteArray) {
        if (bytes.size < 4) return
        val numPlots = bytes[2].toInt() and 0xFF
        if (bytes.size != 4 + numPlots) return

        var checksum = bytes[2].toInt() and 0xFF
        for (i in 0 until numPlots) {
            checksum += bytes[3 + i].toInt() and 0xFF
        }
        if ((checksum and 0xFF).toByte() != bytes.last()) {
            return
        }

        val newPlotValues = (0 until numPlots).map { (bytes[3 + it].toInt() and 0xFF) / 255f }

        _plotState.update { currentState ->
            val updatedSeries = currentState.series.toMutableList()
            newPlotValues.forEachIndexed { index, value ->
                if (index < updatedSeries.size) {
                    val oldPoints = updatedSeries[index].dataPoints.toMutableList()
                    oldPoints.add(value)
                    while (oldPoints.size > 100) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            oldPoints.removeFirst()
                        }
                    }
                    updatedSeries[index] = updatedSeries[index].copy(dataPoints = oldPoints)
                } else {
                    updatedSeries.add(
                        PlotData(
                            name = plotNameMap.getOrDefault(index, "Plot ${index + 1}"),
                            dataPoints = mutableListOf(value),
                            color = plotColors.getOrElse(index) { Color.White }
                        )
                    )
                }
            }
            currentState.copy(series = updatedSeries)
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
                            parseIncomingPacket(byteArray)
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