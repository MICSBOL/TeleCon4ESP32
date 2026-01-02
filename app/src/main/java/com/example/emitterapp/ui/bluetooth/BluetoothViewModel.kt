package com.example.emitterapp.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.domain.bluetooth.BluetoothController
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {
    private val _navigateToScreen = Channel<String>()
    val navigateToScreen = _navigateToScreen.receiveAsFlow()
    private val testRcState = RcUiState(
        leftStickX = 200,   // Pushing right
        leftStickY = 50,    // Pushing down
        rightStickX = 127,  // Centered
        rightStickY = 127,  // Centered
        leftKnob = 255,     // Max
        rightKnob = 0,      // Min
        switch1 = true,     // ON
        switch2 = false,    // OFF
        switch3 = true      // ON
    )
    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        Log.d(
            "BluetoothViewModel",
            "State updated. Scanned Devices: ${scannedDevices.size}, Paired Devices: ${pairedDevices.size}"
        )
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var deviceConnectionJob: Job? = null

    init {

    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        Log.d("BluetoothViewModel", "Connecting to device: ${device.name}")
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun waitForIncomingConnections() {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            val bluetoothMessage = bluetoothController.trySendMessage(message)
            if (bluetoothMessage != null) {
                _state.update {
                    it.copy(
                        messages = it.messages + bluetoothMessage
                    )
                }
            }
        }
    }

    fun startScan() {
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                    _navigateToScreen.send("test_bluetooth")
                }

                is ConnectionResult.TransferSucceeded -> {
                    _state.update {
                        it.copy(messages = it.messages + result.message)
                    }
                }

                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }.catch { throwable ->
            bluetoothController.closeConnection()
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun sendTextRcPacket() {
        sendRcControlData(testRcState)
        Log.d("BluetoothViewModel", "Sent TEST RC Packet: $testRcState")
    }

    fun sendRcControlData(currentState: RcUiState) {
        viewModelScope.launch {
            val packet = ByteArray(10)
            // --- 1. Header ---
            packet[0] = 0xAA.toByte() // Start Byte 1
            packet[1] = 0x55.toByte() // Start Byte 2
            // --- 2. Analog Values (Sticks & Knobs) ---
            // coerceIn ensures the value is safely within the 0-255 byte range
            packet[2] = currentState.leftStickX.coerceIn(0, 255).toByte()
            packet[3] = currentState.leftStickY.coerceIn(0, 255).toByte()
            packet[4] = currentState.rightStickX.coerceIn(0, 255).toByte()
            packet[5] = currentState.rightStickY.coerceIn(0, 255).toByte()
            packet[6] = currentState.leftKnob.coerceIn(0, 255).toByte()
            packet[7] = currentState.rightKnob.coerceIn(0, 255).toByte()

            // --- 3. Digital Values (Switches) ---
            // We pack 8 boolean switches into a single byte (bitmask)
            var switchByte = 0
            if (currentState.switch1) switchByte = switchByte or (1 shl 0) // Bit 0
            if (currentState.switch2) switchByte = switchByte or (1 shl 1) // Bit 1
            if (currentState.switch3) switchByte = switchByte or (1 shl 2) // Bit 2
            if (currentState.switch4) switchByte = switchByte or (1 shl 3) // Bit 3
            if (currentState.switch5) switchByte = switchByte or (1 shl 4) // Bit 4
            if (currentState.switch6) switchByte = switchByte or (1 shl 5) // Bit 5
            if (currentState.switch7) switchByte = switchByte or (1 shl 6) // Bit 6
            if (currentState.switch8) switchByte = switchByte or (1 shl 7) // Bit 7)

            packet[8] = switchByte.toByte()
            var checksum = 0;
            for (i in 2..8) {
                checksum += packet[i].toInt() and 0xFF
            }
            packet[9] = (checksum and 0xFF).toByte()

            bluetoothController.trySendUiPacket(packet)
        }
    }
}