package com.example.emitterapp.data.bluetooth





import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.emitterapp.domain.bluetooth.BluetoothController
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain
import com.example.emitterapp.domain.bluetooth.BluetoothMessage
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

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
        } catch (e: Exception) { /* Already registered */ }
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
        } catch (e: Exception) { /* Not registered */ }
        closeConnection()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")

            // Proactively close any old connection before starting a new one.
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
                    // Atomically create and assign the service, then update state.
                    synchronized(this@AndroidBluetoothController) {
                        dataTransferService = BluetoothDataTransferService(socket)
                        _isConnected.update { true }
                    }
                    emit(ConnectionResult.ConnectionEstablished)

                    // Start listening for messages using the new, valid service.
                    emitAll(
                        dataTransferService!!.listenForIncomingMessages()
                            .map { ConnectionResult.TransferSucceeded(it) }
                    )
                }
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Connection failed: ${e.message}"))
                socket?.close()
            }
        }.onCompletion {
            // This is crucial: ensures cleanup happens even if the flow is cancelled.
            closeConnection()
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    // The server implementation would follow the same robust pattern.
    override fun startBluetoothServer(): Flow<ConnectionResult> = emptyFlow()


    override suspend fun trySendData(data: ByteArray): Boolean? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return null

        // This is now guaranteed to call the single, active service instance's queueing method.
        return dataTransferService?.sendMessage(data)
    }

    // This is the single, thread-safe source of truth for cleanup.
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



//
//@SuppressLint("MissingPermission")
//class AndroidBluetoothController(
//    private val context: Context
//) : BluetoothController {
//
//    private val bluetoothManager by lazy {
//        context.getSystemService(BluetoothManager::class.java)
//    }
//    private val bluetoothAdapter by lazy {
//        bluetoothManager?.adapter
//    }
//
//    // A single, nullable reference to the transfer service.
//    // @Volatile ensures that writes to this variable are visible to all threads immediately.
//    @Volatile
//    private var dataTransferService: BluetoothDataTransferService? = null
//
//    private val _isConnected = MutableStateFlow(false)
//    override val isConnected: StateFlow<Boolean> get() = _isConnected.asStateFlow()
//
//    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _scannedDevices.asStateFlow()
//
//    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _pairedDevices.asStateFlow()
//
//    private val _errors = MutableSharedFlow<String>()
//    override val error: SharedFlow<String> get() = _errors.asSharedFlow()
//
//    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
//        _scannedDevices.update { devices ->
//            val newDevice = device.toBluetoothDeviceDomain()
//            if (newDevice in devices) devices else devices + newDevice
//        }
//    }
//
//    init {
//        updatePairedDevices()
//    }
//
//    override fun startDiscovery() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
//        try {
//            context.registerReceiver(
//                foundDeviceReceiver,
//                IntentFilter(BluetoothDevice.ACTION_FOUND)
//            )
//        } catch (e: Exception) { /* Already registered */ }
//        updatePairedDevices()
//        bluetoothAdapter?.startDiscovery()
//    }
//
//    override fun stopDiscovery() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
//        bluetoothAdapter?.cancelDiscovery()
//    }
//
//    override fun release() {
//        try {
//            context.unregisterReceiver(foundDeviceReceiver)
//        } catch (e: Exception) { /* Not registered */ }
//        closeConnection()
//    }
//
//    // This is now the ONLY way to start a connection. It's atomic.
//    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
//        return flow {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("No BLUETOOTH_CONNECT permission")
//
//            // Proactively close any old connection in a thread-safe way before starting a new one.
//            closeConnection()
//            stopDiscovery()
//
//            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//            val socket: BluetoothSocket? = try {
//                bluetoothDevice?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
//            } catch (e: IOException) {
//                emit(ConnectionResult.Error("Failed to create socket: ${e.message}"))
//                return@flow
//            }
//
//            try {
//                socket?.connect()
//
//                if (socket != null) {
//                    // --- ATOMIC SERVICE CREATION AND STATE UPDATE ---
//                    // This synchronized block ensures that no other thread can interfere
//                    // while we are setting up the new connection.
//                    synchronized(this@AndroidBluetoothController) {
//                        dataTransferService = BluetoothDataTransferService(socket)
//                        _isConnected.update { true }
//                    }
//                    emit(ConnectionResult.ConnectionEstablished)
//
//                    // Start listening for messages using the new, valid service
//                    emitAll(
//                        dataTransferService!!.listenForIncomingMessages()
//                            .map { ConnectionResult.TransferSucceeded(it) }
//                    )
//                }
//            } catch (e: IOException) {
//                emit(ConnectionResult.Error("Connection failed: ${e.message}"))
//                socket?.close()
//            }
//        }.onCompletion {
//            // This is crucial: ensures cleanup happens even if the flow is cancelled.
//            closeConnection()
//        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
//    }
//
//    // startBluetoothServer would have a similar robust structure. For simplicity, we focus on connectToDevice.
//    override fun startBluetoothServer(): Flow<ConnectionResult> {
//        // ... Implementation would follow the same robust pattern as connectToDevice
//        return emptyFlow() // Placeholder
//    }
//
//
//    override suspend fun trySendData(data: ByteArray): Boolean? {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return null
//
//        // This is now guaranteed to call the single, active service instance's queueing method.
//        // It's safe because dataTransferService is volatile and a singleton.
//        return dataTransferService?.sendMessage(data)
//    }
//
//    // Thread-safe and robust close logic
//    override fun closeConnection() {
//        synchronized(this) {
//            dataTransferService?.close()
//            dataTransferService = null
//            _isConnected.update { false }
//        }
//    }
//
//    // Not used in the RC setup
//    override suspend fun trySendMessage(message: String): BluetoothMessage? = null
//
//    private fun updatePairedDevices() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
//        bluetoothAdapter
//            ?.bondedDevices
//            ?.map { it.toBluetoothDeviceDomain() }
//            ?.also { devices -> _pairedDevices.update { devices } }
//    }
//
//    private fun hasPermission(permission: String): Boolean {
//        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
//    }
//
//    companion object {
//        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
//    }
//}




//@SuppressLint("MissingPermission")
//class AndroidBluetoothController(
//    private val context: Context
//) : BluetoothController {
//
//    private val bluetoothManager by lazy {
//        context.getSystemService(BluetoothManager::class.java)
//    }
//
//    private val bluetoothAdapter by lazy {
//        bluetoothManager?.adapter
//    }
//
//    @Volatile
//    private var dataTransferService: BluetoothDataTransferService? = null
//
//    // Sockets are now private and managed internally
////    private var serverSocket: BluetoothServerSocket? = null
////    private var clientSocket: BluetoothSocket? = null
//    private val _isConnected = MutableStateFlow(false)
//    override val isConnected: StateFlow<Boolean>
//        get() = _isConnected.asStateFlow()
//    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//
//    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
//        get() = _scannedDevices.asStateFlow()
//
//    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//
//    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
//        get() = _pairedDevices.asStateFlow()
//
//    private val _errors = MutableSharedFlow<String>()
//    override val error: SharedFlow<String>
//        get() = _errors.asSharedFlow()
//
//    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
//        _scannedDevices.update { devices ->
//            val newDevice = device.toBluetoothDeviceDomain()
//            if (newDevice in devices) devices else devices + newDevice
//        }
//    }
//
//    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
//        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
//            _isConnected.update { isConnected }
//        } else {
//            CoroutineScope(Dispatchers.IO).launch {
//                _errors.emit("Can't connect to a non-paired device.")
//            }
//        }
//    }
//
//    private var currentServerSocket: BluetoothServerSocket? = null
//    private var currentClientSocket: BluetoothSocket? = null
//
//    init {
//        updatePairedDevices()
//    }
//
//    override fun startDiscovery() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            return
//        }
//
//        context.registerReceiver(
//            foundDeviceReceiver,
//            IntentFilter(BluetoothDevice.ACTION_FOUND)
//        )
//
//        updatePairedDevices()
//        bluetoothAdapter?.startDiscovery()
//    }
//
//    override fun stopDiscovery() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
//            return
//        }
//
//        bluetoothAdapter?.cancelDiscovery()
//    }
//
//    override fun release() {
//        context.unregisterReceiver(foundDeviceReceiver)
//        context.unregisterReceiver(bluetoothStateReceiver)
//        closeConnection()
//    }
//
//    private fun updatePairedDevices() {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return
//        }
////        val paired = bluetoothAdapter?.bondedDevices
////        Log.d("pairedDevices", paired?.size.toString())
//        bluetoothAdapter
//            ?.bondedDevices
//            ?.map { it.toBluetoothDeviceDomain() }
//            ?.also { devices ->
//                _pairedDevices.update {
//                    devices
//                }
//            }
//
//    }
//
//    private fun hasPermission(permission: String): Boolean {
//        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
//    }
//
//    override fun startBluetoothServer(): Flow<ConnectionResult> {
//        return flow {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                throw SecurityException("No BLUETOOTH_CONNECT permission")
//            }
//
//            closeConnection()
//
//            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
//                "chat_service",
//                UUID.fromString(SERVICE_UUID)
//            )
//
//            var shouldLop = true
//            while (shouldLop) {
//                currentClientSocket = try {
//                    currentServerSocket?.accept()
//                } catch (e: IOException) {
//                    shouldLop = false
//                    null
//                }
//                emit(ConnectionResult.ConnectionEstablished)
//                currentClientSocket?.let { socket ->
//                    currentServerSocket?.close()
//                    dataTransferService = BluetoothDataTransferService(socket)
//                    emit(ConnectionResult.ConnectionEstablished)
//                    emitAll(
//                        dataTransferService!!.listenForIncomingMessages()
//                            .map { ConnectionResult.TransferSucceeded(it) }
//                    )
//                }
//            }
//        }.onCompletion {
//            closeConnection()
//        }.flowOn(Dispatchers.IO)
//    }
//
//    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
//        return flow {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                throw SecurityException("No BLUETOOTH_CONNECT permission")
//            }
//
//            closeConnection()
//
//            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//
//            currentClientSocket = bluetoothDevice
//                ?.createRfcommSocketToServiceRecord(
//                    UUID.fromString(SERVICE_UUID)
//                )
//            stopDiscovery()
//
//            currentClientSocket?.let { socket ->
//                try {
//                    socket.connect()
//                    _isConnected.update { true }
//                    dataTransferService = BluetoothDataTransferService(socket)
//                    emit(ConnectionResult.ConnectionEstablished)
//                    emitAll(
//                        dataTransferService!!.listenForIncomingMessages()
//                            .map { ConnectionResult.TransferSucceeded(it) }
//                    )
//                } catch (e: IOException) {
//                    socket.close()
//                    currentClientSocket = null
//                    _isConnected.update { false }
//                    emit(ConnectionResult.Error("Connection failed: ${e.message}"))
//                    closeConnection()
//                }
//            }
//        }.onCompletion {
//            closeConnection()
//        }.flowOn(Dispatchers.IO)
//    }
//
//    override suspend fun trySendMessage(message: String): BluetoothMessage? {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return null
//        }
//        if (dataTransferService == null) {
//            return null
//        }
//        val bluetoothMessage = BluetoothMessage(
//            message = message,
//            senderName = bluetoothAdapter?.name ?: "Unknown name",
//            isFromLocalUser = true
//        )
//        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())
//        return bluetoothMessage
//    }
//
//    override suspend fun trySendData(data: ByteArray): Boolean? {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return null
//        }
//        if (dataTransferService == null) {
//            return false
//        }
//        return dataTransferService!!.sendMessage(data)
//    }
//
//    override fun closeConnection() {
//        synchronized(this) {
//            dataTransferService?.close()
//            dataTransferService = null
//            try {
//                currentClientSocket?.close()
//            } catch (e: IOException) { /* Ignore */
//            }
//            try {
//                currentServerSocket?.close()
//            } catch (e: IOException) { /* Ignore */
//            }
//            currentClientSocket = null
//            currentServerSocket = null
//            _isConnected.update { false }
//        }
//    }
//
//    companion object {
//        //        const val SERVICE_UUID = "27b7d1da-08c7-4505-a6d1-2459987e5e2d"
//        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
//    }
//}