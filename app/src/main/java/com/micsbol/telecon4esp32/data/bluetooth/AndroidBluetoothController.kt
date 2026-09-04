package com.micsbol.telecon4esp32.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.EspMessage
import com.micsbol.telecon4esp32.domain.bluetooth.LineProtocolCodec
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteDevice
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.RcBinaryTelemetryMapper
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolTelemetryMapper
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.data.camera.SoftApNetworkResolver
import com.micsbol.telecon4esp32.data.camera.SoftApWifiLock
import com.micsbol.telecon4esp32.data.wifi.SoftApWifiSession
import com.micsbol.telecon4esp32.data.wifi.WifiSoftApDataTransferService
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
class AndroidBluetoothController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val softApNetworkResolver: SoftApNetworkResolver,
    private val softApWifiLock: SoftApWifiLock,
    private val softApWifiSession: SoftApWifiSession,
) : RemoteController {


    private val _telemetryState = MutableStateFlow(TelemetryState())
    override val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private val _messages = MutableSharedFlow<EspMessage>(extraBufferCapacity = 64)
    override val messages: SharedFlow<EspMessage> = _messages.asSharedFlow()
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
        _telemetryState.update { currentState ->
            val (updated, names) = RcBinaryTelemetryMapper.applyPlotConfigNames(
                names = newNames,
                current = currentState,
                plotColors = plotColorArgbs,
            )
            plotNameMap.clear()
            plotNameMap.putAll(names)
            updated
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

    private fun parseIncomingLine(line: String) {
        val message = LineProtocolCodec.decode(line) ?: return
        if (activeTransport == BluetoothTransportType.WIFI &&
            message.app == SimpleProtocolEncoder.RC_APP
        ) {
            when (message.type) {
                "DATA", "ACK", "NAK" ->
                    Log.d(RC_WIFI_TAG, "[TELEM RX] $line")
            }
        }
        _messages.tryEmit(message)
        if (message.app != SimpleProtocolEncoder.RC_APP) return

        when (message.type) {
            "DATA" -> _telemetryState.update { current ->
                SimpleProtocolTelemetryMapper.applyRcData(message.values, current)
            }
            "PLOTCFG" -> _telemetryState.update { current ->
                val (updated, names) = SimpleProtocolTelemetryMapper.applyRcPlotConfig(
                    values = message.values,
                    current = current,
                    plotColors = plotColorArgbs,
                )
                plotNameMap.clear()
                plotNameMap.putAll(names)
                updated
            }
            "PLOT" -> _telemetryState.update { current ->
                SimpleProtocolTelemetryMapper.applyRcPlot(
                    values = message.values,
                    current = current,
                    plotNames = plotNameMap,
                    plotColors = plotColorArgbs,
                )
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
        // Byte 6 still occupies the on/color flags slot for wire compatibility;
        // appearance is configured in Panel settings, not from firmware.

        _telemetryState.update { currentState ->
            currentState.copy(
                panelState = currentState.panelState.copy(
                    leftValue = leftValue,
                    rightValue = rightValue,
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

            val take = minOf(numPlots, SimpleProtocolTelemetryMapper.MAX_PLOT_SERIES)
            val newPlotValues = (0 until take).map {
                (bytes[currentIndex++].toInt() and 0xFF) / 255f
            }

            _telemetryState.update { currentState ->
                RcBinaryTelemetryMapper.applyPlotSamples(
                    normalizedSamples = newPlotValues,
                    current = currentState,
                    plotNames = plotNameMap,
                    plotColors = plotColorArgbs,
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
    private var dataTransferService: BluetoothDataTransport? = null
    private var activeTransport: BluetoothTransportType? = null
    private var lastSoftApCtrlLogLine: String? = null

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

    @Volatile
    private var bleScanCallback: ScanCallback? = null

    init {
        updatePairedDevices()
    }

    override fun startDiscovery(transport: BluetoothTransportType) {
        if (transport == BluetoothTransportType.WIFI) {
            Log.d("BluetoothController", "SoftAP mode — skipping Bluetooth discovery")
            return
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e("BluetoothController", "Missing BLUETOOTH_SCAN permission")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e("BluetoothController", "Bluetooth adapter is disabled")
            return
        }

        updatePairedDevices()
        _discoveredDevices.value = emptyList()
        // Scan Classic and BLE together so the picker lists both SPP and GATT devices,
        // regardless of whether Settings currently selects Classic or BLE for connect.
        startClassicDiscovery()
        startBleScan()
    }

    private fun startClassicDiscovery() {
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

        val startDiscoveryResult = bluetoothAdapter?.startDiscovery()
        Log.d("BluetoothController", "startDiscovery() called, result: $startDiscoveryResult")
    }

    private fun startBleScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BluetoothController", "BLE scanner unavailable")
            return
        }
        stopBleScan()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: result.device.name
                _discoveredDevices.update { devices ->
                    val newDevice = com.micsbol.telecon4esp32.domain.bluetooth.BluetoothDevice(
                        name = name,
                        address = result.device.address,
                    )
                    if (devices.any { it.address == newDevice.address }) devices
                    else devices + newDevice
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BluetoothController", "BLE scan failed: $errorCode")
            }
        }
        bleScanCallback = callback
        scanner.startScan(callback)
        Log.d("BluetoothController", "BLE scan started")
    }

    private fun stopBleScan() {
        val callback = bleScanCallback ?: return
        bleScanCallback = null
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (e: Exception) {
            Log.w("BluetoothController", "Failed to stop BLE scan: ${e.message}")
        }
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

        stopBleScan()

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
        stopBleScan()
        closeConnection()
    }

    override fun connect(
        device: RemoteDevice,
        transport: BluetoothTransportType,
    ): Flow<ConnectionResult> {
        return flow {
            closeConnection()
            stopDiscovery()

            if (transport == BluetoothTransportType.WIFI) {
                val (host, port) = parseSoftApEndpoint(device.address)
                val softApSsid = device.name?.trim().orEmpty()
                    .ifBlank { Esp32CameraDefaults.SOFTAP_SSID }
                when (
                    val join = softApWifiSession.join(
                        ssid = softApSsid,
                        password = Esp32CameraDefaults.SOFTAP_PASSWORD,
                    )
                ) {
                    is SoftApWifiSession.JoinResult.Available,
                    is SoftApWifiSession.JoinResult.AlreadyBound,
                    -> Log.i(RC_WIFI_TAG, "SoftAP Wi‑Fi bound for $softApSsid ($join)")
                    SoftApWifiSession.JoinResult.Unsupported ->
                        Log.i(
                            RC_WIFI_TAG,
                            "SoftAP specifier unsupported — using system Wi‑Fi / resolver for $softApSsid",
                        )
                    SoftApWifiSession.JoinResult.RejectedOrTimeout ->
                        Log.w(
                            RC_WIFI_TAG,
                            "SoftAP join cancelled/timeout for $softApSsid — trying TCP anyway",
                        )
                }
                val wifiService = WifiSoftApDataTransferService(
                    host,
                    port,
                    softApNetworkResolver,
                    softApWifiLock,
                )
                if (!wifiService.open()) {
                    softApWifiSession.release()
                    emit(
                        ConnectionResult.Error(
                            "SoftAP TCP $host:$port unreachable. Join Wi-Fi $softApSsid first.",
                        ),
                    )
                    return@flow
                }
                synchronized(this@AndroidBluetoothController) {
                    dataTransferService = wifiService
                    activeTransport = BluetoothTransportType.WIFI
                    _isConnected.update { true }
                }
                emit(ConnectionResult.SocketEstablished)
                wifiService.listenForIncoming()
                    .collect { frame ->
                        when (frame) {
                            is IncomingBluetoothFrame.BinaryPacket -> parseIncomingPacket(frame.bytes)
                            is IncomingBluetoothFrame.TextLine -> parseIncomingLine(frame.line)
                        }
                    }
                return@flow
            }

            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            // Several vendor Bluetooth stacks are timing-sensitive right after cancelDiscovery.
            delay(250L)

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            if (bluetoothDevice == null) {
                emit(ConnectionResult.Error("Device not found for address: ${device.address}"))
                return@flow
            }

            if (!ensureBonded(bluetoothDevice)) {
                emit(
                    ConnectionResult.Error(
                        "Pairing failed or was cancelled for ${device.name ?: device.address}.",
                    ),
                )
                return@flow
            }
            updatePairedDevices()

            if (transport == BluetoothTransportType.BLE) {
                val bleService = BleDataTransferService(context, bluetoothDevice)
                if (!bleService.open()) {
                    emit(
                        ConnectionResult.Error(
                            "BLE (Nordic UART) connection failed. If the ESP32 runs Classic-only firmware, select Classic Simple or Classic Binary in app Settings.",
                        ),
                    )
                    return@flow
                }
                synchronized(this@AndroidBluetoothController) {
                    dataTransferService = bleService
                    activeTransport = BluetoothTransportType.BLE
                    _isConnected.update { true }
                }
                emit(ConnectionResult.SocketEstablished)
                bleService.listenForIncoming()
                    .collect { frame ->
                        when (frame) {
                            is IncomingBluetoothFrame.BinaryPacket -> parseIncomingPacket(frame.bytes)
                            is IncomingBluetoothFrame.TextLine -> parseIncomingLine(frame.line)
                        }
                    }
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
                    activeTransport = BluetoothTransportType.CLASSIC
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
                        activeTransport = BluetoothTransportType.CLASSIC
                        _isConnected.update { true }
                    }
                } catch (insecureError: IOException) {
                    emit(
                        ConnectionResult.Error(
                            "Classic SPP (RFCOMM) connection failed. If the ESP32 runs BLE-only firmware, select BLE Binary in app Settings."
                        )
                    )
                    try {
                        insecureSocket.close()
                    } catch (_: IOException) {
                    }
                    return@flow
                }
            }

            emit(ConnectionResult.SocketEstablished)
            dataTransferService!!.listenForIncoming()
                .collect { frame ->
                    when (frame) {
                        is IncomingBluetoothFrame.BinaryPacket -> parseIncomingPacket(frame.bytes)
                        is IncomingBluetoothFrame.TextLine -> parseIncomingLine(frame.line)
                    }
                }
        }.onCompletion {
            closeConnection()
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    fun closeConnection() {
        synchronized(this) {
            dataTransferService?.close()
            dataTransferService = null
            activeTransport = null
            lastSoftApCtrlLogLine = null
            _isConnected.update { false }
        }
        softApWifiSession.release()
    }

    /**
     * Ensures the remote device is bonded before RFCOMM/GATT connect.
     * Already-bonded devices return immediately; otherwise creates a bond and waits.
     */
    private suspend fun ensureBonded(device: BluetoothDevice): Boolean {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            return true
        }

        Log.d("BluetoothController", "Pairing with ${device.address} (bondState=${device.bondState})")
        val bonded = withTimeoutOrNull(BOND_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        val changed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java,
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (changed?.address != device.address) return

                        when (
                            intent.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE,
                                BluetoothDevice.ERROR,
                            )
                        ) {
                            BluetoothDevice.BOND_BONDED -> {
                                try {
                                    context.unregisterReceiver(this)
                                } catch (_: IllegalArgumentException) {
                                }
                                if (cont.isActive) cont.resume(true)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                val previous = intent.getIntExtra(
                                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                                    BluetoothDevice.ERROR,
                                )
                                if (previous == BluetoothDevice.BOND_BONDING) {
                                    try {
                                        context.unregisterReceiver(this)
                                    } catch (_: IllegalArgumentException) {
                                    }
                                    if (cont.isActive) cont.resume(false)
                                }
                            }
                        }
                    }
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                    } else {
                        @Suppress("DEPRECATION")
                        context.registerReceiver(receiver, filter)
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothController", "Failed to register bond receiver: ${e.message}", e)
                    if (cont.isActive) cont.resume(false)
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: IllegalArgumentException) {
                    }
                }

                val started = if (device.bondState == BluetoothDevice.BOND_BONDING) {
                    true
                } else {
                    try {
                        device.createBond()
                    } catch (e: Exception) {
                        Log.e("BluetoothController", "createBond failed: ${e.message}", e)
                        false
                    }
                }
                if (!started) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: IllegalArgumentException) {
                    }
                    if (cont.isActive) cont.resume(device.bondState == BluetoothDevice.BOND_BONDED)
                }
            }
        }

        val success = bonded == true || device.bondState == BluetoothDevice.BOND_BONDED
        Log.d("BluetoothController", "Pairing result for ${device.address}: $success")
        return success
    }

    private fun parseSoftApEndpoint(address: String): Pair<String, Int> {
        val trimmed = address.trim()
        val hostPart = trimmed.substringBefore(':').ifBlank {
            Esp32CameraDefaults.DEFAULT_SOFTAP_HOST
        }
        val portPart = trimmed.substringAfter(':', missingDelimiterValue = "")
        val port = portPart.toIntOrNull() ?: Esp32CameraDefaults.DEFAULT_CONTROL_PORT
        return hostPart to port
    }

    private fun requiresBluetoothPermission(): Boolean =
        activeTransport != BluetoothTransportType.WIFI


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

    override suspend fun sendData(data: ByteArray): Boolean? {
        if (requiresBluetoothPermission() &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            return null
        }
        // SoftAP TCP carries the same AA 55 / BB 66 / CC frames as Classic/BLE Binary
        // (DevKit WIFI_BINARY). CAM SoftAP stays SIMPLE text and does not use sendData.
        if (activeTransport == BluetoothTransportType.WIFI) {
            logSoftApBinaryTx(data)
        }
        return dataTransferService?.sendPacket(data)
    }

    override suspend fun sendLine(line: String): Boolean? {
        if (requiresBluetoothPermission() &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            return null
        }
        if (activeTransport == BluetoothTransportType.WIFI) {
            logSoftApTx(line)
        }
        val payload = if (line.endsWith("\n")) line else "$line\n"
        return dataTransferService?.sendPacket(payload.toByteArray(Charsets.UTF_8))
    }

    private fun logSoftApBinaryTx(data: ByteArray) {
        if (data.size < 2) return
        // Skip AA 55 stick flood (~20 Hz); log BB 66 buttons and other frames.
        if (data[0] == 0xAA.toByte() && data[1] == 0x55.toByte()) return
        Log.d(
            RC_WIFI_TAG,
            "[PANEL TX] binary ${data.size} B hdr=%02X %02X".format(
                data[0].toInt() and 0xFF,
                data[1].toInt() and 0xFF,
            ),
        )
    }

    private fun logSoftApTx(line: String) {
        val trimmed = line.trimEnd('\n')
        when {
            trimmed.startsWith("RC:CONNECT") ||
                trimmed.startsWith("RC:BTN") ->
                Log.d(RC_WIFI_TAG, "[PANEL TX] $trimmed")
            trimmed.startsWith("RC:CTRL") -> {
                if (trimmed != lastSoftApCtrlLogLine) {
                    lastSoftApCtrlLogLine = trimmed
                    Log.d(RC_WIFI_TAG, "[PANEL TX] $trimmed")
                }
            }
        }
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val RC_WIFI_TAG = "RcWifiSoftAp"
        private const val BOND_TIMEOUT_MS = 30_000L
    }
}