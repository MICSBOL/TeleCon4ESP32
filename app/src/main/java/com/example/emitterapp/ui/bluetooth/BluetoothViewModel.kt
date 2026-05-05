package com.example.emitterapp.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.RemoteController
import com.example.emitterapp.domain.bluetooth.RemoteDevice
import com.example.emitterapp.domain.bluetooth.RcPacketEncoder
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.domain.model.ButtonEvent
import com.example.emitterapp.domain.model.RcState
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.domain.use_case.GetLastDeviceUseCase
import com.example.emitterapp.domain.use_case.GetUserSettingsUseCase
import com.example.emitterapp.domain.use_case.SaveLastDeviceUseCase
import com.example.emitterapp.ui.rc_settings.SettingsUiState
import com.example.emitterapp.ui.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class BluetoothViewModel @Inject constructor(
    private val remoteController: RemoteController,
    private val getUserSettings: GetUserSettingsUseCase,
    private val getLastDevice: GetLastDeviceUseCase,
    private val saveLastDevice: SaveLastDeviceUseCase,
) : ViewModel() {

    private val _navigateToScreen = Channel<String>()
    val navigateToScreen = _navigateToScreen.receiveAsFlow()
    private val _state = MutableStateFlow(BluetoothUiState())

    val userSettings: StateFlow<SettingsUiState> = getUserSettings()
        .map<UserSettings, SettingsUiState> { settings ->
            SettingsUiState.Success(settings)
        }
        .catch {
            emit(SettingsUiState.Error(it.message ?: "Failed to load settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState.Loading
        )

    val telemetryState: StateFlow<TelemetryState> = remoteController.telemetryState

    val state = combine(
        remoteController.discoveredDevices,
        remoteController.savedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private val _rcControlState = MutableStateFlow(RcControlState())
    val rcControlState: StateFlow<RcControlState> = _rcControlState

    val lastDeviceName: StateFlow<String?> = getLastDevice()
        .map { it?.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var connectingDevice: RemoteDevice? = null
    private var deviceConnectionJob: Job? = null
    private var sendingJob: Job? = null

    init {
        viewModelScope.launch {
            val initialSettings = userSettings
                .filterIsInstance<SettingsUiState.Success>()
                .first()
                .settings

            if (_rcControlState.value == RcControlState()) {
                fun toNormalized(pos: Pair<Int, Int>): Pair<Float, Float> {
                    val x = (pos.first - 6) / 6f
                    val y = (pos.second - 6) / -6f
                    return Pair(x, y)
                }
                Log.d("BluetoothViewModel", "Applying initial saved settings to pristine RcControlState.")
                _rcControlState.update {
                    it.copy(
                        leftStickPosition = toNormalized(initialSettings.leftStickMode.initialPosition),
                        rightStickPosition = toNormalized(initialSettings.rightStickMode.initialPosition),
                        leftKnobValue = initialSettings.leftKnobInitialValue,
                        rightKnobValue = initialSettings.rightKnobInitialValue,
                        leftSwitches = listOf(
                            initialSettings.switchInitialStates[0] ?: false,
                            initialSettings.switchInitialStates[1] ?: false,
                            initialSettings.switchInitialStates[2] ?: false
                        ),
                        rightSwitches = listOf(
                            initialSettings.switchInitialStates[3] ?: false,
                            initialSettings.switchInitialStates[4] ?: false,
                            initialSettings.switchInitialStates[5] ?: false
                        )
                    )
                }
            }
        }
    }

    fun startSendingRcData() {
        if (sendingJob?.isActive == true) return
        sendingJob = viewModelScope.launch {
            Log.d("BluetoothViewModel", "Starting RC data sending loop.")
            while (isActive) {
                val currentState = rcControlState.value
                val rcState = RcState(
                    leftStickX = (currentState.leftStickPosition.first * 100).toInt(),
                    leftStickY = (currentState.leftStickPosition.second * 100).toInt(),
                    rightStickX = (currentState.rightStickPosition.first * 100).toInt(),
                    rightStickY = (currentState.rightStickPosition.second * 100).toInt(),
                    switch1 = currentState.leftSwitches[0],
                    switch2 = currentState.leftSwitches[1],
                    switch3 = currentState.leftSwitches[2],
                    switch4 = currentState.rightSwitches[0],
                    switch5 = currentState.rightSwitches[1],
                    switch6 = currentState.rightSwitches[2],
                    switch7 = false,
                    switch8 = false,
                    leftKnobValue = (currentState.leftKnobValue * 1023).toInt().coerceIn(0, 1023),
                    rightKnobValue = (currentState.rightKnobValue * 1023).toInt().coerceIn(0, 1023)
                )
                remoteController.sendData(RcPacketEncoder.buildRcPacket(rcState))
                delay(50L)
            }
        }
    }

    fun stopSendingRcData() {
        Log.d("BluetoothViewModel", "Stopping RC data sending loop.")
        sendingJob?.cancel()
        sendingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopSendingRcData()
        disconnectFromDevice()
    }

    fun connectToDevice(device: RemoteDevice) {
        Log.d("BluetoothViewModel", "Connecting to device: ${device.name}")
        connectingDevice = device
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = remoteController.connect(device).listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        remoteController.disconnect()
        _state.update { it.copy(isConnecting = false, isConnected = false) }
    }

    fun startScan() {
        _state.update { it.copy(isScanning = true) }
        remoteController.startDiscovery()
    }

    fun stopScan() {
        _state.update { it.copy(isScanning = false) }
        remoteController.stopDiscovery()
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun quickConnect() {
        viewModelScope.launch {
            val lastDevice = getLastDevice().first()
            if (lastDevice == null) {
                Log.d("BluetoothViewModel", "No last device saved, navigating to Bluetooth screen.")
                _navigateToScreen.send("bluetooth")
                return@launch
            }
            val (address, _) = lastDevice
            val savedDevice = remoteController.savedDevices.value.find { it.address == address }
            if (savedDevice == null) {
                Log.d("BluetoothViewModel", "Last device $address not in paired list, navigating to Bluetooth screen.")
                _navigateToScreen.send("bluetooth")
                return@launch
            }
            Log.d("BluetoothViewModel", "Quick connecting to last device: ${savedDevice.name}")
            connectToDevice(savedDevice)
        }
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    connectingDevice?.let { device ->
                        viewModelScope.launch {
                            saveLastDevice(device.address, device.name)
                        }
                    }
                    _state.update {
                        it.copy(isConnected = true, isConnecting = false, errorMessage = null)
                    }
                    // Navigate to Home screen instead of RC screen, keeping connection active
                    _navigateToScreen.send("home")
                }
                is ConnectionResult.TransferSucceeded -> {
                    _state.update { it.copy(messages = it.messages + result.message) }
                }
                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(isConnected = false, isConnecting = false, errorMessage = result.message)
                    }
                }
            }
        }.catch { throwable ->
            remoteController.disconnect()
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    errorMessage = throwable.message ?: "Unknown connection error"
                )
            }
        }.launchIn(viewModelScope)
    }

    fun sendButtonEvent(event: ButtonEvent) {
        viewModelScope.launch {
            remoteController.sendData(RcPacketEncoder.buildButtonPacket(event))
        }
    }

    fun onLeftStickChanged(x: Float, y: Float) {
        _rcControlState.update { it.copy(leftStickPosition = Pair(x, y)) }
    }

    fun onRightStickChanged(x: Float, y: Float) {
        _rcControlState.update { it.copy(rightStickPosition = Pair(x, y)) }
    }

    fun onLeftSwitchChanged(index: Int, newState: Boolean) {
        _rcControlState.update {
            val newSwitches = it.leftSwitches.toMutableList().also { list -> list[index] = newState }
            it.copy(leftSwitches = newSwitches)
        }
    }

    fun onRightSwitchChanged(index: Int, newState: Boolean) {
        _rcControlState.update {
            val newSwitches = it.rightSwitches.toMutableList().also { list -> list[index] = newState }
            it.copy(rightSwitches = newSwitches)
        }
    }

    fun onLeftKnobChanged(newValue: Float) {
        _rcControlState.update { it.copy(leftKnobValue = newValue) }
    }

    fun onRightKnobChanged(newValue: Float) {
        _rcControlState.update { it.copy(rightKnobValue = newValue) }
    }
}

data class RcControlState(
    val leftStickPosition: Pair<Float, Float> = Pair(0f, 0f),
    val rightStickPosition: Pair<Float, Float> = Pair(0f, 0f),
    val leftSwitches: List<Boolean> = List(3) { false },
    val rightSwitches: List<Boolean> = List(3) { false },
    val leftKnobValue: Float = 0.5f,
    val rightKnobValue: Float = 0.5f
)
