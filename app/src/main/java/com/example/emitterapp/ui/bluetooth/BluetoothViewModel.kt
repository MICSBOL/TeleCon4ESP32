package com.example.emitterapp.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.domain.bluetooth.BluetoothController
import com.example.emitterapp.domain.bluetooth.BluetoothDeviceDomain
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.IndicatorState
import com.example.emitterapp.domain.bluetooth.PanelState
import com.example.emitterapp.domain.bluetooth.PlotState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
open class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {
    private val _navigateToScreen = Channel<String>()
    val navigateToScreen = _navigateToScreen.receiveAsFlow()
    private val _state = MutableStateFlow(BluetoothUiState())

    val panelState: StateFlow<PanelState> = bluetoothController.panelState
    val indicatorState: StateFlow<IndicatorState> = bluetoothController.indicatorState
    val plotState: StateFlow<PlotState> = bluetoothController.plotState
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
            pairedDevices = pairedDevices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var deviceConnectionJob: Job? = null

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
                    _navigateToScreen.send("rc_screen")
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

    fun sendRcControlData(currentState: RcUiState) {
        viewModelScope.launch {
            val packet = ByteArray(18)
            packet[0] = 0xAA.toByte()
            packet[1] = 0x55.toByte()

            val leftStickX_12bit = (((currentState.leftStickX + 100) * 4095) / 200).coerceIn(0, 4095)
            val leftStickY_12bit = (((currentState.leftStickY + 100) * 4095) / 200).coerceIn(0, 4095)
            val rightStickX_12bit = (((currentState.rightStickX + 100) * 4095) / 200).coerceIn(0, 4095)
            val rightStickY_12bit = (((currentState.rightStickY + 100) * 4095) / 200).coerceIn(0, 4095)

            packet[2] = (leftStickX_12bit and 0xFF).toByte()
            packet[3] = ((leftStickX_12bit shr 8) and 0xFF).toByte()

            packet[4] = (leftStickY_12bit and 0xFF).toByte()
            packet[5] = ((leftStickY_12bit shr 8) and 0xFF).toByte()

            packet[6] = (rightStickX_12bit and 0xFF).toByte()
            packet[7] = ((rightStickX_12bit shr 8) and 0xFF).toByte()

            packet[8] = (rightStickY_12bit and 0xFF).toByte()
            packet[9] = ((rightStickY_12bit shr 8) and 0xFF).toByte()

            packet[10] = (currentState.leftKnobValue and 0xFF).toByte()
            packet[11] = ((currentState.leftKnobValue shr 8) and 0xFF).toByte()

            packet[12] = (currentState.rightKnobValue and 0xFF).toByte()
            packet[13] = ((currentState.rightKnobValue shr 8) and 0xFF).toByte()

            var switchByte = 0
            if (currentState.switch1) switchByte = switchByte or (1 shl 0)
            if (currentState.switch2) switchByte = switchByte or (1 shl 1)
            if (currentState.switch3) switchByte = switchByte or (1 shl 2)
            if (currentState.switch4) switchByte = switchByte or (1 shl 3)
            if (currentState.switch5) switchByte = switchByte or (1 shl 4)
            if (currentState.switch6) switchByte = switchByte or (1 shl 5)
            if (currentState.switch7) switchByte = switchByte or (1 shl 6)
            if (currentState.switch8) switchByte = switchByte or (1 shl 7)

            packet[14] = switchByte.toByte()
            var checksum = 0;
            for (i in 2..14) {
                checksum += packet[i].toInt() and 0xFF
            }
            packet[15] = (checksum and 0xFF).toByte()
            packet[16] = 0x0D.toByte()
            packet[17] = 0x0A.toByte()
            bluetoothController.trySendData(packet)
        }
    }

    fun sendButtonEvent(event: ButtonEvent) {
        viewModelScope.launch {
            val eventPacket = ByteArray(4)
            eventPacket[0] = 0xBB.toByte()
            eventPacket[1] = 0x66.toByte()
            eventPacket[2] = event.id
            eventPacket[3] = event.id
            bluetoothController.trySendData(eventPacket)
        }
    }
}