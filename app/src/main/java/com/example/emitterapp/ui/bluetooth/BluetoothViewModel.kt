package com.example.emitterapp.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.domain.bluetooth.ConnectionResult
import com.example.emitterapp.domain.bluetooth.RemoteController
import com.example.emitterapp.domain.bluetooth.RemoteDevice
import com.example.emitterapp.domain.bluetooth.RcPacketEncoder
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.ui.rc_screen.SideIndicatorUi
import com.example.emitterapp.ui.rc_screen.SideTelemetry
import com.example.emitterapp.ui.rc_screen.SwitchStates
import com.example.emitterapp.ui.rc_screen.toLeftSideTelemetry
import com.example.emitterapp.ui.rc_screen.toRightSideTelemetry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import com.example.emitterapp.domain.model.ButtonEvent
import com.example.emitterapp.domain.model.RcState
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.domain.use_case.GetLastDeviceUseCase
import com.example.emitterapp.domain.use_case.GetUserSettingsUseCase
import com.example.emitterapp.domain.use_case.SaveLastDeviceUseCase
import com.example.emitterapp.ui.rc_settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
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

    private val _rcPlotSeries = MutableStateFlow<List<PlotData>>(emptyList())

    /** Plot series throttled for RC UI (~30 fps) so BT flood does not starve switch/knob animations. */
    val rcPlotSeries: StateFlow<List<PlotData>> = _rcPlotSeries

    /** Panel/LED slice — does not change when only plot points are appended. */
    val rcLeftSideTelemetry: StateFlow<SideTelemetry> = telemetryState
        .map { it.toLeftSideTelemetry() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = telemetryState.value.toLeftSideTelemetry(),
        )

    val rcRightSideTelemetry: StateFlow<SideTelemetry> = telemetryState
        .map { it.toRightSideTelemetry() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = telemetryState.value.toRightSideTelemetry(),
        )

    val rcLeftIndicator: StateFlow<SideIndicatorUi> = telemetryState
        .map {
            SideIndicatorUi(
                value = it.indicatorState.analogValue,
                title = it.indicatorState.analogTitle,
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SideIndicatorUi(
                telemetryState.value.indicatorState.analogValue,
                telemetryState.value.indicatorState.analogTitle,
            ),
        )

    val rcRightIndicator: StateFlow<SideIndicatorUi> = telemetryState
        .map {
            SideIndicatorUi(
                value = it.indicatorState.batteryLevel,
                title = it.indicatorState.batteryTitle,
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SideIndicatorUi(
                telemetryState.value.indicatorState.batteryLevel,
                telemetryState.value.indicatorState.batteryTitle,
            ),
        )

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

    val rcLeftSwitchStates: StateFlow<SwitchStates> = rcControlState
        .map { SwitchStates.of(it.leftSwitches) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SwitchStates.of(_rcControlState.value.leftSwitches),
        )

    val rcRightSwitchStates: StateFlow<SwitchStates> = rcControlState
        .map { SwitchStates.of(it.rightSwitches) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SwitchStates.of(_rcControlState.value.rightSwitches),
        )

    val rcLeftStickPosition: StateFlow<Pair<Float, Float>> = rcControlState
        .map { it.leftStickPosition }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _rcControlState.value.leftStickPosition,
        )

    val rcRightStickPosition: StateFlow<Pair<Float, Float>> = rcControlState
        .map { it.rightStickPosition }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _rcControlState.value.rightStickPosition,
        )

    val rcLeftKnobValue: StateFlow<Float> = rcControlState
        .map { it.leftKnobValue }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _rcControlState.value.leftKnobValue,
        )

    val rcRightKnobValue: StateFlow<Float> = rcControlState
        .map { it.rightKnobValue }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _rcControlState.value.rightKnobValue,
        )

    val lastDeviceName: StateFlow<String?> = getLastDevice()
        .map { it?.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var connectingDevice: RemoteDevice? = null
    private var deviceConnectionJob: Job? = null
    private var sendingJob: Job? = null
    private var plotThrottleJob: Job? = null
    private val rcPlotScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Snapshot of settings last written to [rcControlState]. */
    private var lastAppliedSettings: UserSettings? = null

    private val _rcSettingsSyncGeneration = MutableStateFlow(0)
    val rcSettingsSyncGeneration: StateFlow<Int> = _rcSettingsSyncGeneration

    companion object {
        private const val RC_PLOT_UI_PERIOD_MS = 33L
    }

    init {
        viewModelScope.launch {
            val settings = userSettings
                .filterIsInstance<SettingsUiState.Success>()
                .first()
                .settings
            applySettingsIfChanged(settings)
        }
    }

    private fun startRcPlotUiThrottling() {
        if (plotThrottleJob?.isActive == true) return
        plotThrottleJob = rcPlotScope.launch {
            var latest = emptyList<PlotData>()
            var hasPending = false
            val collectJob = launch {
                telemetryState
                    .map { it.plotState.series }
                    .collect { series ->
                        latest = series
                        hasPending = true
                    }
            }
            try {
                while (isActive) {
                    delay(RC_PLOT_UI_PERIOD_MS)
                    if (hasPending) {
                        _rcPlotSeries.value = latest
                        hasPending = false
                    }
                }
            } finally {
                collectJob.cancel()
            }
        }
    }

    private fun stopRcPlotUiThrottling() {
        plotThrottleJob?.cancel()
        plotThrottleJob = null
    }

    /**
     * Called when an RC screen is shown. Applies [UserSettings] after a cold start or when
     * RcSettings changed; otherwise keeps the last in-session [rcControlState].
     */
    fun onRcScreenEntered() {
        val settings = (userSettings.value as? SettingsUiState.Success)?.settings ?: return
        applySettingsIfChanged(settings)
        startRcPlotUiThrottling()
        startSendingRcData()
    }

    private fun applySettingsIfChanged(settings: UserSettings) {
        if (settings == lastAppliedSettings) return
        applySettingsToRcControl(settings)
        lastAppliedSettings = settings
        _rcSettingsSyncGeneration.update { it + 1 }
    }

    private fun applySettingsToRcControl(settings: UserSettings) {
        Log.d("BluetoothViewModel", "Applying saved settings to RcControlState.")
        _rcControlState.update {
            it.copy(
                leftStickPosition = gridPositionToNormalized(settings.leftStickMode.initialPosition),
                rightStickPosition = gridPositionToNormalized(settings.rightStickMode.initialPosition),
                leftKnobValue = settings.leftKnobInitialValue,
                rightKnobValue = settings.rightKnobInitialValue,
                leftSwitches = listOf(
                    settings.switchInitialStates[0] ?: false,
                    settings.switchInitialStates[1] ?: false,
                    settings.switchInitialStates[2] ?: false
                ),
                rightSwitches = listOf(
                    settings.switchInitialStates[3] ?: false,
                    settings.switchInitialStates[4] ?: false,
                    settings.switchInitialStates[5] ?: false
                )
            )
        }
    }

    private fun gridPositionToNormalized(pos: Pair<Int, Int>): Pair<Float, Float> {
        val x = (pos.first - 6) / 6f
        val y = (pos.second - 6) / -6f
        return Pair(x, y)
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
        stopRcPlotUiThrottling()
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
