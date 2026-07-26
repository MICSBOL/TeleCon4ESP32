package com.micsbol.telecon4esp32.ui.bluetooth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.ActiveBluetoothSession
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothSessionContext
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionResult
import com.micsbol.telecon4esp32.domain.bluetooth.Esp32SoftApDevice
import com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure
import com.micsbol.telecon4esp32.domain.bluetooth.ProtocolHandshake
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
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.navigation.mainRoute
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.RcState
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastApplicationUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetLastDeviceUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveLastApplicationUseCase
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withTimeoutOrNull
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
    private val getLastApplication: GetLastApplicationUseCase,
    private val saveLastApplication: SaveLastApplicationUseCase,
    private val getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val getApplicationTransportType: GetApplicationTransportTypeUseCase,
) : ViewModel() {

    val controlPanelProtocolMode: StateFlow<BluetoothProtocolMode> =
        getApplicationProtocolMode(ApplicationId.CONTROL_PANEL)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothProtocolMode.defaultFor(ApplicationId.CONTROL_PANEL),
            )

    val controlPanelTransportType: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(ApplicationId.CONTROL_PANEL)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothTransportType.CLASSIC,
            )

    /** Persisted transport (Classic / BLE) selected for [applicationId]. */
    fun observeTransportType(applicationId: ApplicationId): Flow<BluetoothTransportType> =
        getApplicationTransportType(applicationId)

    /** Persisted protocol mode (Simple / Advanced) selected for [applicationId]. */
    fun observeProtocolMode(applicationId: ApplicationId): Flow<BluetoothProtocolMode> =
        getApplicationProtocolMode(applicationId)

    val lastApplicationId: StateFlow<ApplicationId?> = getLastApplication()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
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
        private const val HANDSHAKE_TIMEOUT_MS = 2_500L
        /** SoftAP CTRL heartbeat — must stay under ESP32 TELECON_CTRL_TIMEOUT_MS (~750). */
        private const val SOFTAP_CTRL_PERIOD_MS = 100L
        private const val RC_WIFI_TAG = "RcWifiSoftAp"
        /** NavGraph pops the back stack when this route is emitted after a successful connect. */
        const val POP_BACK_ON_CONNECT = "__pop_back_on_connect__"
    }

    private var pendingSessionContext: BluetoothSessionContext? = null
    private var requestedSessionContext: BluetoothSessionContext? = null
    private var handshakeJob: Job? = null

    val activeSession: StateFlow<ActiveBluetoothSession?> = _state
        .map { it.activeSession }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun isSessionActiveFor(applicationId: ApplicationId): Boolean =
        _state.value.activeSession?.applicationId == applicationId && _state.value.isConnected

    fun hasSessionConflict(applicationId: ApplicationId): Boolean {
        val session = _state.value.activeSession ?: return false
        return _state.value.isConnected && session.applicationId != applicationId
    }

    fun requestApplicationConnection(
        applicationId: ApplicationId,
        protocolMode: BluetoothProtocolMode,
        transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    ) {
        requestedSessionContext = BluetoothSessionContext(applicationId, protocolMode, transport)
    }

    fun clearRequestedApplicationConnection() {
        requestedSessionContext = null
    }

    fun connectForApplication(
        applicationId: ApplicationId,
        protocolMode: BluetoothProtocolMode,
        device: RemoteDevice,
        transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    ) {
        val active = _state.value.activeSession
        if (active != null && active.applicationId != applicationId && _state.value.isConnected) {
            disconnectFromDevice()
        }
        pendingSessionContext = BluetoothSessionContext(applicationId, protocolMode, transport)
        requestedSessionContext = BluetoothSessionContext(applicationId, protocolMode, transport)
        Log.d("BluetoothViewModel", "Connecting to device: ${device.name} for $applicationId over $transport")
        connectingDevice = device
        handshakeJob?.cancel()
        _state.update {
            it.copy(
                isConnecting = true,
                errorMessage = null,
                connectFailure = null,
                handshakeFailure = null,
            )
        }
        deviceConnectionJob = remoteController.connect(device, transport).listen()
    }

    /**
     * Connects RC control over SoftAP TCP (`192.168.4.1:3333`) without opening the
     * Bluetooth device picker. Requires [requestApplicationConnection] with WIFI transport.
     */
    fun connectToWifiSoftAp() {
        val context = requestedSessionContext
        if (context == null || context.transport != BluetoothTransportType.WIFI) {
            _state.update {
                it.copy(connectFailure = BluetoothConnectFailure.MissingSessionContext)
            }
            return
        }
        connectForApplication(
            applicationId = context.applicationId,
            protocolMode = BluetoothProtocolMode.SIMPLE,
            device = Esp32SoftApDevice.Default,
            transport = BluetoothTransportType.WIFI,
        )
    }

    /**
     * SoftAP HUD helper: open TCP control when the camera proves SoftAP is reachable.
     * No-ops if already connected/connecting for [applicationId].
     */
    fun ensureWifiSoftApConnected(applicationId: ApplicationId = ApplicationId.RC_VEHICLE_PRO) {
        if (isSessionActiveFor(applicationId)) return
        if (_state.value.isConnecting) return
        if (hasSessionConflict(applicationId)) return
        requestApplicationConnection(
            applicationId,
            BluetoothProtocolMode.SIMPLE,
            BluetoothTransportType.WIFI,
        )
        connectToWifiSoftAp()
    }

    private var postConnectNavigateRoute: String? = null

    /** Call before opening the in-app connect sheet so success closes the sheet. */
    fun preparePostConnectPopBack() {
        postConnectNavigateRoute = POP_BACK_ON_CONNECT
    }

    /** After a successful connect from Home, navigate to this application route. */
    fun preparePostConnectNavigateTo(route: String) {
        postConnectNavigateRoute = route
    }

    fun markRecentApplication(applicationId: ApplicationId) {
        viewModelScope.launch {
            saveLastApplication(applicationId)
        }
    }

    /**
     * Reopens the last project: continue if already connected for that app,
     * otherwise open the Bluetooth picker and navigate into the app after success.
     */
    fun openRecentProject(applicationId: ApplicationId, onNavigate: (String) -> Unit) {
        if (isSessionActiveFor(applicationId)) {
            onNavigate(applicationId.mainRoute())
            return
        }
        viewModelScope.launch {
            val mode = getApplicationProtocolMode(applicationId).first()
            val transport = getApplicationTransportType(applicationId).first()
            requestApplicationConnection(applicationId, mode, transport)
            saveLastApplication(applicationId)
            if (transport == BluetoothTransportType.WIFI) {
                // Open the app first so SoftAP errors use the in-app dialog (no BT picker).
                onNavigate(applicationId.mainRoute())
                connectToWifiSoftAp()
                return@launch
            }
            preparePostConnectNavigateTo(applicationId.mainRoute())
            onNavigate(Screen.Bluetooth.route)
        }
    }

    fun continueLastSession(onNavigate: (String) -> Unit) {
        val session = _state.value.activeSession
        if (session != null && _state.value.isConnected) {
            onNavigate(session.applicationId.mainRoute())
            return
        }
        onNavigate(Screen.Applications.route)
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
        // RC Vehicle Pro (and Control Panel) send with the active session's protocol.
        // Restart the TX loop when the session is established or its mode changes.
        viewModelScope.launch {
            activeSession
                .drop(1)
                .collect {
                    if (rcDataSendingActive) {
                        restartRcDataSending()
                    }
                }
        }
        // Keep UI session in sync with the real link so app Bluetooth buttons
        // turn red/enabled as soon as the ESP32 drops for any reason.
        viewModelScope.launch {
            remoteController.isConnected.collect { linked ->
                if (!linked) {
                    clearSessionAfterLinkDown()
                }
            }
        }
    }

    /**
     * Protocol used for RC CTRL/BTN TX. Prefer the connected (or pending) RC session
     * so RC Vehicle Pro uses its own Classic Simple / Binary / BLE setting instead of
     * always following Control Panel DataStore.
     *
     * SoftAP TCP firmware only speaks SIMPLE text (`RC:CTRL` / `RC:BTN`); never send
     * ADVANCED binary (`AA 55`) on that link even if a binary mode was persisted earlier.
     */
    private fun currentRcProtocolMode(): BluetoothProtocolMode {
        if (currentRcTransport() == BluetoothTransportType.WIFI) {
            return BluetoothProtocolMode.SIMPLE
        }

        fun BluetoothSessionContext.isRcApp(): Boolean =
            applicationId == ApplicationId.CONTROL_PANEL ||
                applicationId == ApplicationId.RC_VEHICLE_PRO

        _state.value.activeSession?.let { session ->
            if (session.applicationId == ApplicationId.CONTROL_PANEL ||
                session.applicationId == ApplicationId.RC_VEHICLE_PRO
            ) {
                return session.protocolMode
            }
        }
        pendingSessionContext?.takeIf { it.isRcApp() }?.let { return it.protocolMode }
        requestedSessionContext?.takeIf { it.isRcApp() }?.let { return it.protocolMode }
        return controlPanelProtocolMode.value
    }

    private fun currentRcTransport(): BluetoothTransportType? {
        fun BluetoothSessionContext.isRcApp(): Boolean =
            applicationId == ApplicationId.CONTROL_PANEL ||
                applicationId == ApplicationId.RC_VEHICLE_PRO

        _state.value.activeSession?.let { session ->
            if (session.applicationId == ApplicationId.CONTROL_PANEL ||
                session.applicationId == ApplicationId.RC_VEHICLE_PRO
            ) {
                return session.transport
            }
        }
        pendingSessionContext?.takeIf { it.isRcApp() }?.let { return it.transport }
        requestedSessionContext?.takeIf { it.isRcApp() }?.let { return it.transport }
        return null
    }

    private fun clearSessionAfterLinkDown() {
        val current = _state.value
        if (!current.isConnected && current.activeSession == null && !current.isConnecting) {
            return
        }
        handshakeJob?.cancel()
        pendingSessionContext = null
        connectingDevice = null
        if (rcDataSendingActive) {
            stopSendingRcData()
        }
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false,
                activeSession = null,
            )
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
        // SoftAP TCP accepts only SIMPLE text lines — never the ADVANCED binary loop.
        val mode = if (currentRcTransport() == BluetoothTransportType.WIFI) {
            BluetoothProtocolMode.SIMPLE
        } else {
            currentRcProtocolMode()
        }
        sendingJob = when (mode) {
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
        val softAp = currentRcTransport() == BluetoothTransportType.WIFI
        if (softAp) {
            // SoftAP firmware fail-safe (~750 ms) needs a CTRL heartbeat.
            // ESP32 Serial still prints [PANEL RX] only when the payload changes.
            Log.d(RC_WIFI_TAG, "Starting SoftAP RC CTRL heartbeat (${SOFTAP_CTRL_PERIOD_MS}ms).")
            while (isActive) {
                remoteController.sendRcControl(
                    rcControlState.value.toRcState(),
                    BluetoothProtocolMode.SIMPLE,
                )
                delay(SOFTAP_CTRL_PERIOD_MS)
            }
        } else {
            Log.d("BluetoothViewModel", "Starting simple RC data sending on control changes.")
            rcControlState.collect { controlState ->
                remoteController.sendRcControl(
                    controlState.toRcState(),
                    BluetoothProtocolMode.SIMPLE,
                )
            }
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
        val context = requestedSessionContext
        if (context == null) {
            _state.update {
                it.copy(connectFailure = BluetoothConnectFailure.MissingSessionContext)
            }
            return
        }
        connectForApplication(context.applicationId, context.protocolMode, device, context.transport)
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        handshakeJob?.cancel()
        pendingSessionContext = null
        remoteController.disconnect()
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false,
                activeSession = null,
            )
        }
    }

    fun startScan() {
        _state.update { it.copy(isScanning = true) }
        val transport = requestedSessionContext?.transport ?: BluetoothTransportType.CLASSIC
        remoteController.startDiscovery(transport)
    }

    fun stopScan() {
        _state.update { it.copy(isScanning = false) }
        remoteController.stopDiscovery()
    }

    fun dismissError() {
        _state.update {
            it.copy(
                errorMessage = null,
                connectFailure = null,
                handshakeFailure = null,
            )
        }
    }

    fun openApplications() {
        viewModelScope.launch {
            _navigateToScreen.send(Screen.Applications.route)
        }
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.SocketEstablished -> {
                    val device = connectingDevice
                    val context = pendingSessionContext
                    if (device == null || context == null) {
                        failConnection(BluetoothConnectFailure.MissingSessionContext)
                        return@onEach
                    }
                    startHandshake(device, context)
                }
                is ConnectionResult.SessionEstablished -> {
                    // Reserved for controller-driven sessions; handshake completes in ViewModel.
                }
                is ConnectionResult.Error -> {
                    val transport = pendingSessionContext?.transport
                    failConnection(
                        BluetoothConnectFailure.fromLinkError(result.message, transport),
                    )
                }
            }
        }.catch { throwable ->
            val transport = pendingSessionContext?.transport
            failConnection(
                BluetoothConnectFailure.fromLinkError(
                    throwable.message ?: "Unknown connection error",
                    transport,
                ),
            )
        }.launchIn(viewModelScope)
    }

    private fun startHandshake(device: RemoteDevice, context: BluetoothSessionContext) {
        handshakeJob?.cancel()
        handshakeJob = viewModelScope.launch {
            val appPrefix = context.applicationId.protocolPrefix()
            val connectLine = ProtocolHandshake.buildConnectLine(
                appPrefix,
                context.protocolMode,
                context.transport,
            )
            remoteController.sendLine(connectLine)

            val response = withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) {
                remoteController.messages
                    .filter { message ->
                        message.app == appPrefix &&
                            (message.type == ProtocolHandshake.ACK_TYPE ||
                                message.type == ProtocolHandshake.NAK_TYPE)
                    }
                    .first()
            }

            when {
                response?.type == ProtocolHandshake.ACK_TYPE -> {
                    completeSession(device, context, handshakeConfirmed = true)
                }
                response?.type == ProtocolHandshake.NAK_TYPE -> {
                    val failure = ProtocolHandshake.parseNakReason(response.values)
                        ?: HandshakeFailure.Unknown("nak")
                    failHandshake(failure)
                }
                else -> {
                    // No ACK/NAK (wrong firmware, wrong proto build, or silent board).
                    failHandshake(HandshakeFailure.Timeout)
                }
            }
        }
    }

    private suspend fun completeSession(
        device: RemoteDevice,
        context: BluetoothSessionContext,
        handshakeConfirmed: Boolean,
    ) {
        saveLastDevice(device.address, device.name)
        saveLastApplication(context.applicationId)
        val session = ActiveBluetoothSession(
            applicationId = context.applicationId,
            protocolMode = context.protocolMode,
            deviceName = device.name,
            deviceAddress = device.address,
            handshakeConfirmed = handshakeConfirmed,
            transport = context.transport,
        )
        _state.update {
            it.copy(
                isConnected = true,
                isConnecting = false,
                errorMessage = null,
                connectFailure = null,
                handshakeFailure = null,
                activeSession = session,
            )
        }
        pendingSessionContext = null
        if (context.transport == BluetoothTransportType.WIFI && rcDataSendingActive) {
            restartRcDataSending()
        }
        val destination = postConnectNavigateRoute
        postConnectNavigateRoute = null
        if (destination != null) {
            _navigateToScreen.send(destination)
        }
    }

    private fun failHandshake(failure: HandshakeFailure) {
        remoteController.disconnect()
        pendingSessionContext = null
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                activeSession = null,
                connectFailure = null,
                errorMessage = null,
                handshakeFailure = failure,
            )
        }
    }

    private fun failConnection(failure: BluetoothConnectFailure) {
        remoteController.disconnect()
        pendingSessionContext = null
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                activeSession = null,
                handshakeFailure = null,
                errorMessage = null,
                connectFailure = failure,
            )
        }
    }

    fun sendButtonEvent(event: ButtonEvent) {
        viewModelScope.launch {
            remoteController.sendRcButton(event, currentRcProtocolMode())
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
