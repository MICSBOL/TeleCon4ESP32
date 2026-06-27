package com.micsbol.telecon4esp32.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteDevice
import com.micsbol.telecon4esp32.domain.bluetooth.RcPacketEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolEncoder
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.ui.control_panel.SideIndicatorUi
import com.micsbol.telecon4esp32.ui.control_panel.SideTelemetry
import com.micsbol.telecon4esp32.ui.control_panel.SwitchStates
import com.micsbol.telecon4esp32.ui.control_panel.toLeftSideTelemetry
import com.micsbol.telecon4esp32.ui.control_panel.toRightSideTelemetry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.RcState
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastDeviceUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastDeviceUseCase
import com.micsbol.telecon4esp32.ui.rc_settings.DisplayLabelDraft
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsUiState
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
) : ViewModel() {

    private val controlPanelProtocolMode: StateFlow<BluetoothProtocolMode> =
        getApplicationProtocolMode(ApplicationId.CONTROL_PANEL)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.CONTROL_PANEL),
            )

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

    private val _rcPlotUiState = MutableStateFlow(RcPlotUiState())

    /** Plot series throttled for RC UI (~30 fps) so BT flood does not starve switch/knob animations. */
    val rcPlotUiState: StateFlow<RcPlotUiState> = _rcPlotUiState

    /** Always kept in sync with DataStore; updated immediately when labels are applied in settings. */
    private val _telemetryLabelSettings = MutableStateFlow(TelemetryLabelSettings())
    private val telemetryLabelsFromSettings: StateFlow<TelemetryLabelSettings> =
        _telemetryLabelSettings.asStateFlow()

    val rcPlotDisplayLabels: StateFlow<List<String>> = _telemetryLabelSettings
        .map { it.plotLabels }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = List(UserSettings.PLOT_LABEL_COUNT) { "" },
        )

    /** Panel/LED slice — does not change when only plot points are appended. */
    val rcLeftSideTelemetry: StateFlow<SideTelemetry> = combine(
        telemetryState,
        telemetryLabelsFromSettings,
    ) { telemetry, labels ->
        telemetry.toLeftSideTelemetry().copy(
            panelTitle = telemetry.panelState.leftTitle.orSettingsFallback(labels.leftPanelUnit),
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = telemetryState.value.toLeftSideTelemetry(),
        )

    val rcRightSideTelemetry: StateFlow<SideTelemetry> = combine(
        telemetryState,
        telemetryLabelsFromSettings,
    ) { telemetry, labels ->
        telemetry.toRightSideTelemetry().copy(
            panelTitle = telemetry.panelState.rightTitle.orSettingsFallback(labels.rightPanelUnit),
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = telemetryState.value.toRightSideTelemetry(),
        )

    val rcLeftIndicator: StateFlow<SideIndicatorUi> = combine(
        telemetryState,
        telemetryLabelsFromSettings,
    ) { telemetry, labels ->
        SideIndicatorUi(
            value = telemetry.indicatorState.analogValue,
            title = telemetry.indicatorState.analogTitle
                .orSettingsFallback(labels.analogIndicatorUnit),
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

    val rcRightIndicator: StateFlow<SideIndicatorUi> = combine(
        telemetryState,
        telemetryLabelsFromSettings,
    ) { telemetry, labels ->
        SideIndicatorUi(
            value = telemetry.indicatorState.batteryLevel,
            title = telemetry.indicatorState.batteryTitle
                .orSettingsFallback(labels.batteryLabel),
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
    private var rcDataSendingActive = false
    private val rcPlotScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Snapshot of settings last written to [rcControlState]. */
    private var lastAppliedSettings: UserSettings? = null

    private val _rcSettingsSyncGeneration = MutableStateFlow(0)
    val rcSettingsSyncGeneration: StateFlow<Int> = _rcSettingsSyncGeneration

    companion object {
        private const val RC_PLOT_UI_PERIOD_MS = 33L
        /** NavGraph pops the back stack when this route is emitted after a successful connect. */
        const val POP_BACK_ON_CONNECT = "__pop_back_on_connect__"
    }

    private var postConnectPopBack = false

    /** Call before opening [Screen.Bluetooth] so a successful connect returns to the previous screen. */
    fun preparePostConnectPopBack() {
        postConnectPopBack = true
    }

    init {
        viewModelScope.launch {
            getUserSettings().collect { settings ->
                _telemetryLabelSettings.value = settings.toTelemetryLabelSettings()
            }
        }
        viewModelScope.launch {
            val settings = getUserSettings().first()
            applySettingsIfChanged(settings)
        }
        viewModelScope.launch {
            controlPanelProtocolMode
                .drop(1)
                .collect {
                    if (rcDataSendingActive) {
                        restartRcDataSending()
                    }
                }
        }
    }

    private fun startRcPlotUiThrottling() {
        if (plotThrottleJob?.isActive == true) return
        plotThrottleJob = rcPlotScope.launch {
            var latest = RcPlotUiState()
            var hasPending = false
            val collectJob = launch {
                telemetryState
                    .map { state ->
                        RcPlotUiState(
                            series = state.plotState.series,
                            revision = state.plotState.revision,
                        )
                    }
                    .collect { plotState ->
                        latest = plotState
                        hasPending = true
                    }
            }
            try {
                while (isActive) {
                    delay(RC_PLOT_UI_PERIOD_MS)
                    if (hasPending) {
                        _rcPlotUiState.value = latest
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
    fun onControlPanelEntered() {
        viewModelScope.launch {
            val settings = getUserSettings().first()
            _telemetryLabelSettings.value = settings.toTelemetryLabelSettings()
            applySettingsIfChanged(settings)
            _rcSettingsSyncGeneration.update { it + 1 }
            startRcPlotUiThrottling()
            startSendingRcData()
        }
    }

    /** Applies display labels immediately so the control panel updates before DataStore propagates. */
    fun applyDisplayLabelSettings(draft: DisplayLabelDraft) {
        _telemetryLabelSettings.value = draft.toTelemetryLabelSettings()
        val persisted = (userSettings.value as? SettingsUiState.Success)?.settings
        if (persisted != null) {
            lastAppliedSettings = persisted.copy(
                leftPanelUnit = draft.leftPanelUnit,
                rightPanelUnit = draft.rightPanelUnit,
                analogIndicatorUnit = draft.analogIndicatorUnit,
                batteryLabel = draft.batteryLabel,
                plotLabels = draft.plotLabels,
            )
        }
        _rcSettingsSyncGeneration.update { it + 1 }
    }

    fun startSendingRcData() {
        rcDataSendingActive = true
        restartRcDataSending()
    }

    private fun restartRcDataSending() {
        sendingJob?.cancel()
        sendingJob = when (controlPanelProtocolMode.value) {
            BluetoothProtocolMode.ADVANCED -> startAdvancedRcSendingLoop()
            BluetoothProtocolMode.SIMPLE -> startSimpleRcSendingOnChange()
        }
    }

    private fun startAdvancedRcSendingLoop(): Job = viewModelScope.launch {
        Log.d("BluetoothViewModel", "Starting advanced RC data sending loop.")
        while (isActive) {
            remoteController.sendRcControl(
                rcControlState.value.toRcState(),
                BluetoothProtocolMode.ADVANCED,
            )
            delay(50L)
        }
    }

    private fun startSimpleRcSendingOnChange(): Job = viewModelScope.launch {
        Log.d("BluetoothViewModel", "Starting simple RC data sending on control changes.")
        rcControlState.collect { controlState ->
            remoteController.sendRcControl(
                controlState.toRcState(),
                BluetoothProtocolMode.SIMPLE,
            )
        }
    }

    fun stopSendingRcData() {
        Log.d("BluetoothViewModel", "Stopping RC data sending.")
        rcDataSendingActive = false
        sendingJob?.cancel()
        sendingJob = null
        stopRcPlotUiThrottling()
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
                    if (postConnectPopBack) {
                        postConnectPopBack = false
                        _navigateToScreen.send(POP_BACK_ON_CONNECT)
                    } else {
                        _navigateToScreen.send("home")
                    }
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
            remoteController.sendRcButton(event, controlPanelProtocolMode.value)
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

private fun RcControlState.toRcState(): RcState = RcState(
    leftStickX = (leftStickPosition.first * 100).toInt(),
    leftStickY = (leftStickPosition.second * 100).toInt(),
    rightStickX = (rightStickPosition.first * 100).toInt(),
    rightStickY = (rightStickPosition.second * 100).toInt(),
    switch1 = leftSwitches[0],
    switch2 = leftSwitches[1],
    switch3 = leftSwitches[2],
    switch4 = rightSwitches[0],
    switch5 = rightSwitches[1],
    switch6 = rightSwitches[2],
    switch7 = false,
    switch8 = false,
    leftKnobValue = (leftKnobValue * 1023).toInt().coerceIn(0, 1023),
    rightKnobValue = (rightKnobValue * 1023).toInt().coerceIn(0, 1023),
)

private suspend fun RemoteController.sendRcControl(
    rcState: RcState,
    protocolMode: BluetoothProtocolMode,
) {
    when (protocolMode) {
        BluetoothProtocolMode.SIMPLE ->
            sendLine(SimpleProtocolEncoder.buildRcCtrlLine(rcState))
        BluetoothProtocolMode.ADVANCED ->
            sendData(RcPacketEncoder.buildRcPacket(rcState))
    }
}

private suspend fun RemoteController.sendRcButton(
    event: ButtonEvent,
    protocolMode: BluetoothProtocolMode,
) {
    when (protocolMode) {
        BluetoothProtocolMode.SIMPLE ->
            sendLine(SimpleProtocolEncoder.buildRcButtonLine(event))
        BluetoothProtocolMode.ADVANCED ->
            sendData(RcPacketEncoder.buildButtonPacket(event))
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

/** Throttled plot snapshot for the RC screen (series history + monotonic revision). */
data class RcPlotUiState(
    val series: List<PlotData> = emptyList(),
    val revision: Long = 0L,
)

private data class TelemetryLabelSettings(
    val leftPanelUnit: String = "",
    val rightPanelUnit: String = "",
    val analogIndicatorUnit: String = "",
    val batteryLabel: String = "",
    val plotLabels: List<String> = List(UserSettings.PLOT_LABEL_COUNT) { "" },
)

private fun String.orSettingsFallback(settingsLabel: String): String =
    if (isBlank()) settingsLabel else this

private fun UserSettings.toTelemetryLabelSettings() = TelemetryLabelSettings(
    leftPanelUnit = leftPanelUnit,
    rightPanelUnit = rightPanelUnit,
    analogIndicatorUnit = analogIndicatorUnit,
    batteryLabel = batteryLabel,
    plotLabels = plotLabels,
)

private fun DisplayLabelDraft.toTelemetryLabelSettings() = TelemetryLabelSettings(
    leftPanelUnit = leftPanelUnit,
    rightPanelUnit = rightPanelUnit,
    analogIndicatorUnit = analogIndicatorUnit,
    batteryLabel = batteryLabel,
    plotLabels = plotLabels,
)
