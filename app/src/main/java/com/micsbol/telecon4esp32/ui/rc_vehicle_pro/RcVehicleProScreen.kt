package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.autoConnectSoftApControlWhenCameraOnline
import com.micsbol.telecon4esp32.domain.camera.isSoftApControl
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.domain.bluetooth.AnalogChannelHistory
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryChannelRouter
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.bluetooth.LocalApplicationBluetoothSession
import com.micsbol.telecon4esp32.ui.camera.SoftApRuntimeStreamQualityControl
import com.micsbol.telecon4esp32.ui.camera.StreamQualityLauncherStyle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.LocalDisconnectedBannerInsets
import com.micsbol.telecon4esp32.ui.components.LocalHudSystemBarsHidden
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.reportTutorialAnchor
import com.micsbol.telecon4esp32.ui.control_panel.HideHudSystemBars
import com.micsbol.telecon4esp32.ui.control_panel.LockHudLandscape
import com.micsbol.telecon4esp32.ui.control_panel.RadarScanSpan
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPanPanel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcDriveAssistDialog
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPreview
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCenterControls
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcControlZone
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcGlassAccentEdge
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudLinkStatusIcons
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudMetricsRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudNoticeBanner
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudCameraConnectIcon
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudTopBarStatusRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudPlotChrome
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcTelemetryPlotPanel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcTelemetryPlotSession
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcVehicleHudTopBar
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.rememberTelemetryPlotSession
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcStickTrim
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcStickMapping
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout.CAMERA_PAN_CENTER
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun RcVehicleProScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    viewModel: RcVehicleProViewModel = hiltViewModel(),
) {
    LockHudLandscape()
    HideHudSystemBars()

    // Connection and toggles only. Speed, sticks, and telemetry are read in the
    // badges and plot so a packet does not rebuild the stick layout.
    val chrome by remember(viewModel.uiState) {
        viewModel.uiState.map { it.toHudChrome() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.uiState.value.toHudChrome())
    val isBluetoothConnecting by bluetoothViewModel.state
        .map { it.isConnecting }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)
    val controlSettings by viewModel.controlSettings.collectAsStateWithLifecycle()
    val plotChrome by viewModel.plotChrome.collectAsStateWithLifecycle()
    val rcTutorial = LocalRcVehicleProTutorial.current
    val bluetoothSession = LocalApplicationBluetoothSession.current
    val onBluetoothConnect by rememberUpdatedState(bluetoothSession?.onConnect)
    val linkProfile = chrome.cameraLinkProfile
    val usesWifiLink = linkProfile.isSoftApControl

    // SoftAP-only (Kit A): once video works, SoftAP is reachable — open TCP control.
    // Overlay (SoftAP video + DevKit Bluetooth): SoftAP HTTP video only; never SoftAP TCP.
    LaunchedEffect(
        linkProfile,
        chrome.isCameraOnline,
        chrome.isBluetoothConnected,
    ) {
        if (
            linkProfile.autoConnectSoftApControlWhenCameraOnline &&
            chrome.isCameraOnline &&
            !chrome.isBluetoothConnected
        ) {
            bluetoothViewModel.ensureWifiSoftApConnected(ApplicationId.RC_VEHICLE_PRO)
        }
    }

    LaunchedEffect(chrome.photoFeedback) {
        when (chrome.photoFeedback) {
            PhotoFeedback.None -> Unit
            PhotoFeedback.Saved,
            PhotoFeedback.NoFrame,
            PhotoFeedback.Failed -> {
                delay(2_000L)
                viewModel.consumePhotoFeedback()
            }
        }
    }

    LaunchedEffect(Unit) {
        bluetoothViewModel.onRcVehicleProEntered()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onScreenVisible()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenHidden()
            bluetoothViewModel.stopSendingRcData()
        }
    }

    LaunchedEffect(chrome.isBluetoothConnected) {
        if (chrome.isBluetoothConnected) {
            bluetoothViewModel.syncRcCameraPanFront()
        }
    }

    RcThrottleSpeedSampler(
        leftStick = bluetoothViewModel.rcLeftStickPosition,
        onThrottleY = viewModel::updateDriveMetricsFromThrottle,
    )

    BackHandler {
        navController.navigateUp()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Isolated collector: new SoftAP frames recompose only this layer.
        RcVehicleProCameraLayer(
            cameraPreviewState = viewModel.cameraPreviewState,
            cameraLinkProfile = viewModel.cameraLinkProfile,
            isControlConnected = chrome.isBluetoothConnected,
            onConnectControl = { onBluetoothConnect?.invoke() },
        )

        val showStreamQuality = chrome.isCameraStreamArmed
        val preset by viewModel.softApPerformancePreset.collectAsStateWithLifecycle()
        val hudRate by viewModel.softApHudProcessingRate.collectAsStateWithLifecycle()

        CompositionLocalProvider(LocalHudSystemBarsHidden provides true) {
        RcVehicleProContent(
            uiState = RcVehicleProUiState(
                cameraLinkProfile = chrome.cameraLinkProfile,
                isBluetoothConnected = chrome.isBluetoothConnected,
                isCameraOnline = chrome.isCameraOnline,
                isCameraStreamArmed = chrome.isCameraStreamArmed,
                isRecording = chrome.isRecording,
                lightsOn = chrome.lightsOn,
                photoFeedback = chrome.photoFeedback,
            ),
            liveUiState = viewModel.uiState,
            telemetryState = bluetoothViewModel.telemetryState,
            leftStickPosition = bluetoothViewModel.rcLeftStickPosition,
            rightStickPosition = bluetoothViewModel.rcRightStickPosition,
            cameraKnob = bluetoothViewModel.rcRightKnobValue,
            channelRoutingState = bluetoothViewModel.rcChannelRouting,
            plotLabelsState = bluetoothViewModel.rcPlotDisplayLabels,
            controlSettings = controlSettings,
            plotChrome = plotChrome,
            onPlotChromeChange = viewModel::updatePlotChrome,
            onRadarSourceChange = bluetoothViewModel::saveChannelBinding,
            onPlotLabelChange = bluetoothViewModel::savePlotLabel,
            onPlotChannelChange = { index, channel ->
                bluetoothViewModel.saveChannelBinding(TelemetrySink.plotAt(index), channel)
            },
            onNavigateBack = { navController.navigateUp() },
            onBluetoothDisconnectedClick = { onBluetoothConnect?.invoke() },
            settingsAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            navController.navigate(
                                Screen.ModuleFeatureMenu.createRoute(ApplicationId.RC_VEHICLE_PRO),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.feature_menu_open),
                            tint = brandPrimary(),
                        )
                    }
                    RcHudCameraConnectIcon(
                        cameraEnabled = linkProfile.shouldStartCameraStream,
                        isCameraOnline = chrome.isCameraOnline,
                        onConnectClick = viewModel::connectCamera,
                    )
                    RcHudLinkStatusIcons(
                        profile = linkProfile,
                        isControlConnected = chrome.isBluetoothConnected,
                        isControlConnecting = isBluetoothConnecting,
                        isCameraOnline = chrome.isCameraOnline,
                        usesWifiControl = usesWifiLink,
                        onDisconnectedClick = { onBluetoothConnect?.invoke() },
                    )
                    if (showStreamQuality) {
                        SoftApRuntimeStreamQualityControl(
                            selectedPreset = preset,
                            selectedHudRate = hudRate,
                            onPresetSelected = viewModel::onSoftApPerformancePresetChanged,
                            onHudRateSelected = viewModel::onSoftApHudProcessingRateChanged,
                            launcherStyle = StreamQualityLauncherStyle.ICON_BUTTON,
                        )
                    }
                    ApplicationSettingsIconButton(
                        applicationId = ApplicationId.RC_VEHICLE_PRO,
                        navController = navController,
                    )
                }
            },
            onThrottleMove = bluetoothViewModel::onLeftStickChanged,
            onSteeringMove = bluetoothViewModel::onRightStickChanged,
            onControlSettingsChange = viewModel::updateControlSettings,
            onCycleThrottleTravel = viewModel::cycleThrottleTravel,
            onCycleSteerTravel = viewModel::cycleSteerTravel,
            onPhotoClick = viewModel::onCapturePhoto,
            onRecordClick = viewModel::onToggleRecording,
            onLightsClick = {
                bluetoothViewModel.onLeftSwitchChanged(0, !chrome.lightsOn)
                viewModel.onToggleLights()
            },
            onBuzzerClick = {
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT)
            },
            onCameraFrontClick = {
                bluetoothViewModel.onRightKnobChanged(CAMERA_PAN_CENTER)
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT)
            },
            onCameraKnobChange = bluetoothViewModel::onRightKnobChanged,
            onStopClick = {
                viewModel.onEmergencyStop()
            },
        )
        }
    }
}

/**
 * Owns camera StateFlow collection so HUD/controls skip recomposition on each JPEG.
 */
@Composable
private fun RcVehicleProCameraLayer(
    cameraPreviewState: StateFlow<CameraStreamState>,
    cameraLinkProfile: StateFlow<CameraLinkProfile>,
    isControlConnected: Boolean,
    onConnectControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraState by cameraPreviewState.collectAsStateWithLifecycle()
    val profile by cameraLinkProfile.collectAsStateWithLifecycle()
    RcCameraPreview(
        cameraState = cameraState,
        cameraLinkProfile = profile,
        isControlConnected = isControlConnected,
        onConnectControl = onConnectControl,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun RcVehicleProContent(
    uiState: RcVehicleProUiState,
    controlSettings: RcVehicleProControlSettings = RcVehicleProControlSettings.DEFAULT,
    plotChrome: RcHudPlotChrome = RcHudPlotChrome(),
    onPlotChromeChange: (RcHudPlotChrome) -> Unit = {},
    onNavigateBack: () -> Unit,
    onBluetoothDisconnectedClick: () -> Unit = {},
    settingsAction: @Composable () -> Unit,
    onThrottleMove: (Float, Float) -> Unit,
    onSteeringMove: (Float, Float) -> Unit,
    onControlSettingsChange: ((RcVehicleProControlSettings) -> RcVehicleProControlSettings) -> Unit = {},
    onCycleThrottleTravel: () -> Unit = {},
    onCycleSteerTravel: () -> Unit = {},
    liveUiState: StateFlow<RcVehicleProUiState>? = null,
    telemetryState: StateFlow<com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState>? = null,
    leftStickPosition: StateFlow<Pair<Float, Float>>? = null,
    rightStickPosition: StateFlow<Pair<Float, Float>>? = null,
    cameraKnob: StateFlow<Float>? = null,
    channelRoutingState: StateFlow<ChannelRouting>? = null,
    plotLabelsState: StateFlow<List<String>>? = null,
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    onBuzzerClick: () -> Unit,
    onCameraFrontClick: () -> Unit,
    onCameraKnobChange: (Float) -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Camera stays full-bleed; HUD/controls clear status, cutouts, and side nav
    // (including OEM landscape builds that under-report horizontal insets).
    var leftTrimMode by remember { mutableStateOf(false) }
    var rightTrimMode by remember { mutableStateOf(false) }
    var showDriveAssist by remember { mutableStateOf(false) }
    var rawThrottle by remember {
        mutableStateOf(controlSettings.leftStickMode.initialPositionNormalized())
    }
    var rawSteer by remember {
        mutableStateOf(controlSettings.rightStickMode.initialPositionNormalized())
    }
    var stickSettingsGeneration by remember { mutableIntStateOf(0) }
    var panExpanded by remember { mutableStateOf(false) }
    var plotExpanded by remember { mutableStateOf(true) }
    var topBarExpanded by remember { mutableStateOf(false) }
    var centerExpanded by remember { mutableStateOf(false) }
    var leftStickExpanded by remember { mutableStateOf(true) }
    var rightStickExpanded by remember { mutableStateOf(true) }
    var stickResizeMode by remember { mutableStateOf(false) }
    var panResizeMode by remember { mutableStateOf(false) }
    val rcTutorial = LocalRcVehicleProTutorial.current
    if (rcTutorial != null) {
        val tutorialActive by rcTutorial.active.collectAsStateWithLifecycle()
        val tutorialStep by rcTutorial.step.collectAsStateWithLifecycle()
        LaunchedEffect(tutorialActive, tutorialStep) {
            if (!tutorialActive) return@LaunchedEffect
            topBarExpanded = true
            leftStickExpanded = true
            rightStickExpanded = true
            when (tutorialStep) {
                RcVehicleProTutorialStep.CENTER_ACTIONS -> centerExpanded = true
                RcVehicleProTutorialStep.TELEMETRY -> plotExpanded = true
                RcVehicleProTutorialStep.CAMERA_PAN -> panExpanded = true
                else -> Unit
            }
        }
    }
    val radarScanSpan = plotChrome.radarSettings.scanSpan
    val uiFlow = liveUiState ?: remember { MutableStateFlow(uiState) }
    val telemetryFlow = telemetryState ?: remember {
        MutableStateFlow(com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState())
    }
    val leftStickFlow = leftStickPosition ?: remember { MutableStateFlow(0f to 0f) }
    val rightStickFlow = rightStickPosition ?: remember { MutableStateFlow(0f to 0f) }
    val cameraKnobFlow = cameraKnob ?: remember { MutableStateFlow(0.5f) }
    val routingFlow = channelRoutingState ?: remember { MutableStateFlow(ChannelRouting.defaults()) }
    val labelsFlow = plotLabelsState ?: remember { MutableStateFlow(emptyList<String>()) }
    val chrome by remember(uiFlow) {
        uiFlow.map { it.toHudChrome() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = uiFlow.value.toHudChrome())
    val disconnectedBannerInsets = LocalDisconnectedBannerInsets.current
    DisposableEffect(disconnectedBannerInsets) {
        disconnectedBannerInsets.suppressHostOverlay = true
        onDispose { disconnectedBannerInsets.suppressHostOverlay = false }
    }

    val latestControlSettings = rememberUpdatedState(controlSettings)

    fun publishThrottle(x: Float = rawThrottle.first, y: Float = rawThrottle.second) {
        rawThrottle = Pair(x, y)
        val mapped = RcStickMapping.mapThrottleStick(x, y, latestControlSettings.value)
        onThrottleMove(mapped.first, mapped.second)
    }

    fun publishSteer(x: Float = rawSteer.first, y: Float = rawSteer.second) {
        rawSteer = Pair(x, y)
        val mapped = RcStickMapping.mapSteerStick(x, y, latestControlSettings.value)
        onSteeringMove(mapped.first, mapped.second)
    }

    fun commitScanSpan(span: RadarScanSpan) {
        onPlotChromeChange(
            plotChrome.copy(radarSettings = plotChrome.radarSettings.copy(scanSpan = span)),
        )
    }

    val leftStickMode = controlSettings.leftStickMode
    val rightStickMode = controlSettings.rightStickMode

    fun publishLeftAtRest(settings: RcVehicleProControlSettings = latestControlSettings.value) {
        val rest = settings.leftStickMode.initialPositionNormalized()
        rawThrottle = rest
        val mapped = RcStickMapping.mapThrottleStick(rest.first, rest.second, settings)
        onThrottleMove(mapped.first, mapped.second)
    }

    fun publishRightAtRest(settings: RcVehicleProControlSettings = latestControlSettings.value) {
        val rest = settings.rightStickMode.initialPositionNormalized()
        rawSteer = rest
        val mapped = RcStickMapping.mapSteerStick(rest.first, rest.second, settings)
        onSteeringMove(mapped.first, mapped.second)
    }

    fun nudgeStickTrim(isLeft: Boolean, axis: JoystickAxis, steps: Int) {
        stickSettingsGeneration += 1
        var next = controlSettings
        onControlSettingsChange { current ->
            next = if (isLeft) {
                if (axis == JoystickAxis.HORIZONTAL) {
                    current.copy(leftTrimX = RcStickTrim.nudge(current.leftTrimX, steps))
                } else {
                    current.copy(leftTrimY = RcStickTrim.nudge(current.leftTrimY, steps))
                }
            } else {
                if (axis == JoystickAxis.HORIZONTAL) {
                    current.copy(rightTrimX = RcStickTrim.nudge(current.rightTrimX, steps))
                } else {
                    current.copy(rightTrimY = RcStickTrim.nudge(current.rightTrimY, steps))
                }
            }
            next
        }
        if (isLeft) publishLeftAtRest(next) else publishRightAtRest(next)
    }

    // Re-emit mapped channels when dual-rate / expo / reverse / trim change while held.
    LaunchedEffect(controlSettings) {
        val settings = latestControlSettings.value
        val throttle = RcStickMapping.mapThrottleStick(rawThrottle.first, rawThrottle.second, settings)
        onThrottleMove(throttle.first, throttle.second)
        val steer = RcStickMapping.mapSteerStick(rawSteer.first, rawSteer.second, settings)
        onSteeringMove(steer.first, steer.second)
    }

    if (showDriveAssist) {
        RcDriveAssistDialog(
            settings = controlSettings,
            onSettingsChange = { next -> onControlSettingsChange { next } },
            onDismiss = { showDriveAssist = false },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        RcVehicleHudTopBar(
            modifier = Modifier.reportTutorialAnchor(TutorialAnchor.RC_TOP_BAR),
            title = stringResource(R.string.app_rc_vehicle_title),
            onNavigateBack = onNavigateBack,
            expanded = topBarExpanded,
            onExpandedChange = { topBarExpanded = it },
            metricsContent = {
                RcLiveHudMetrics(
                    uiState = uiFlow,
                    speedChannel = controlSettings.speedChannel,
                    tempChannel = controlSettings.tempChannel,
                    currentChannel = controlSettings.currentChannel,
                    onSpeedChannelChange = { channel ->
                        onControlSettingsChange { it.copy(speedChannel = channel) }
                    },
                    onTempChannelChange = { channel ->
                        onControlSettingsChange { it.copy(tempChannel = channel) }
                    },
                    onCurrentChannelChange = { channel ->
                        onControlSettingsChange { it.copy(currentChannel = channel) }
                    },
                )
            },
            statusContent = {
                RcLiveHudStatus(
                    uiState = uiFlow,
                    batteryChannel = controlSettings.batteryChannel,
                    onBatteryChannelChange = { channel ->
                        onControlSettingsChange { it.copy(batteryChannel = channel) }
                    },
                )
            },
            actions = settingsAction,
            hideRowStartContent = {
                if (!plotExpanded) {
                    RcVehicleHudPlotPanel(
                        uiState = uiFlow,
                        telemetryState = telemetryFlow,
                        leftStick = leftStickFlow,
                        rightStick = rightStickFlow,
                        cameraKnob = cameraKnobFlow,
                        channelRoutingState = routingFlow,
                        plotLabelsState = labelsFlow,
                        controlSettings = controlSettings,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier
                            .wrapContentWidth()
                            .reportTutorialAnchor(TutorialAnchor.RC_PLOT),
                        expanded = false,
                        onExpandedChange = { plotExpanded = it },
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        chrome = plotChrome,
                        onChromeChange = onPlotChromeChange,
                    )
                }
            },
            hideRowEndContent = {
                if (!panExpanded) {
                    RcLiveCameraPanPanel(
                        modifier = Modifier
                            .wrapContentWidth()
                            .reportTutorialAnchor(TutorialAnchor.RC_CAMERA_PAN),
                        expanded = false,
                        onExpandedChange = { panExpanded = it },
                        valueFlow = cameraKnobFlow,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
                        channelLink = controlSettings.cameraKnobChannel,
                        occupiedChannels = controlSettings.occupiedChannels(exceptCameraKnob = true),
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(cameraKnobChannel = next) }
                        },
                    )
                }
            },
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .safeHudPadding(
                    includeTop = false,
                    includeBottom = false,
                    includeHorizontal = true,
                )
                .padding(start = 4.dp, end = 4.dp),
        ) {
            val leftStickAlone = leftStickExpanded && !rightStickExpanded
            val rightStickAlone = rightStickExpanded && !leftStickExpanded
            val panOverlapsRightStick = panExpanded && rightStickExpanded
            val bothSticksVisible = leftStickExpanded && rightStickExpanded
            val configuration = LocalConfiguration.current
            val maxHudHeight = RcVehicleProLayout.maxResizeHeight(
                configuration.screenHeightDp.dp,
            )
            val joystickSize = RcVehicleProLayout.joystickPadSize(
                slotWidth = maxWidth,
                slotHeight = maxHeight,
                centerExpanded = centerExpanded && bothSticksVisible,
                leftStickExpanded = leftStickExpanded,
                rightStickExpanded = rightStickExpanded,
                fillLeftover = true,
            )
            val centerReserved = RcVehicleProLayout.centerControlsReservedWidth(
                slotWidth = maxWidth,
                centerExpanded = centerExpanded,
                bothSticksExpanded = bothSticksVisible,
            )
            val baseStickWidth = joystickSize
            val baseStickHeight = joystickSize
            val stickGroupMaxScale = RcVehicleProLayout.maxScaleForBaseHeight(
                baseHeight = baseStickHeight,
                maxHeight = maxHudHeight,
            )
            val stickGroupScale = RcVehicleProLayout.coerceStickGroupScale(
                scale = controlSettings.stickGroupScale,
                maxScale = stickGroupMaxScale,
            )
            val (stickWidth, stickHeight) = RcVehicleProLayout.scaledStickContainerSize(
                baseWidth = baseStickWidth,
                baseHeight = baseStickHeight,
                scale = stickGroupScale,
                maxWidth = (maxWidth - centerReserved).coerceAtLeast(1.dp) / 2,
                maxHeight = minOf(maxHeight, maxHudHeight),
            )
            val halfScreenWidth = maxWidth / 2
            val centerOverlayMaxWidth = if (centerExpanded && bothSticksVisible) {
                centerReserved
            } else {
                RcVehicleProLayout.CenterControlsExpandedWidth
            }
            val panTowardCenterPadding = if (panOverlapsRightStick) {
                stickWidth
            } else {
                0.dp
            }
            val oneStickPlotSlotWidth = when {
                leftStickAlone || rightStickAlone -> {
                    (maxWidth - halfScreenWidth - RcVehicleProLayout.HudCollapsedPeek - 8.dp -
                        RcVehicleProLayout.HudEdgeChevronWidth)
                        .coerceAtLeast(1.dp)
                }
                else -> {
                    (maxWidth - RcVehicleProLayout.HudEdgeChevronWidth).coerceAtLeast(1.dp)
                }
            }
            val (fittedPlotWidth, fittedPlotHeight) = if (bothSticksVisible) {
                RcVehicleProLayout.telemetryPlotSizeBetweenSticks(
                    hudWidth = maxWidth,
                    hudHeight = maxHeight,
                    stickPadSize = joystickSize,
                )
            } else {
                RcVehicleProLayout.telemetryPlotSize(
                    availableWidth = oneStickPlotSlotWidth,
                    availableHeight = maxHeight,
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (plotExpanded && bothSticksVisible) {
                    RcVehicleHudPlotPanel(
                        uiState = uiFlow,
                        telemetryState = telemetryFlow,
                        leftStick = leftStickFlow,
                        rightStick = rightStickFlow,
                        cameraKnob = cameraKnobFlow,
                        channelRoutingState = routingFlow,
                        plotLabelsState = labelsFlow,
                        controlSettings = controlSettings,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .wrapContentWidth()
                            .zIndex(1.5f)
                            .reportTutorialAnchor(TutorialAnchor.RC_PLOT),
                        expanded = plotExpanded,
                        onExpandedChange = { plotExpanded = it },
                        plotWidth = fittedPlotWidth,
                        plotHeight = fittedPlotHeight,
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        chrome = plotChrome,
                        onChromeChange = onPlotChromeChange,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val leftSlotModifier = when {
                        !leftStickExpanded -> Modifier.wrapContentWidth()
                        else -> Modifier.width(stickWidth).height(stickHeight)
                    }
                    val rightSlotModifier = when {
                        !rightStickExpanded -> Modifier.wrapContentWidth()
                        else -> Modifier.width(stickWidth).height(stickHeight)
                    }
                    RcControlZone(
                        modifier = leftSlotModifier
                            .reportTutorialAnchor(TutorialAnchor.RC_THROTTLE),
                        title = stringResource(R.string.rc_vehicle_control_throttle),
                        collapsibleToNearestEdge = true,
                        chromeExpanded = leftStickExpanded,
                        onChromeExpandedChange = { leftStickExpanded = it },
                        stickPosition = rawThrottle,
                        mode = leftStickMode,
                        accentEdge = RcGlassAccentEdge.START,
                        joystickSize = joystickSize,
                        fillHeight = leftStickExpanded,
                        resizeMode = stickResizeMode,
                        stickGroupScale = stickGroupScale,
                        onResizeModeChange = { stickResizeMode = it },
                        onStickGroupScaleChange = { next ->
                            onControlSettingsChange { it.copy(stickGroupScale = next) }
                        },
                        maxScale = stickGroupMaxScale,
                        onMove = { x, y ->
                            if (!leftTrimMode) {
                                publishThrottle(x, y)
                            }
                        },
                        onStopClick = {
                            leftTrimMode = false
                            rightTrimMode = false
                            publishLeftAtRest()
                            publishRightAtRest()
                            onStopClick()
                        },
                        onStickModeChange = { next ->
                            stickSettingsGeneration += 1
                            onControlSettingsChange { it.copy(leftStickMode = next) }
                            val rest = next.initialPositionNormalized()
                            publishThrottle(rest.first, rest.second)
                        },
                        rangeShape = controlSettings.leftStickRangeShape,
                        onRangeShapeChange = { next ->
                            stickSettingsGeneration += 1
                            onControlSettingsChange { it.copy(leftStickRangeShape = next) }
                        },
                        channelLink = controlSettings.leftStickChannels,
                        occupiedChannels = controlSettings.occupiedChannels(exceptLeftStick = true),
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(leftStickChannels = next) }
                        },
                        settingsSyncGeneration = stickSettingsGeneration,
                        stickConfigContentDescription = stringResource(
                            R.string.control_panel_widget_config_content_description,
                            stringResource(R.string.rc_vehicle_control_throttle),
                        ),
                        visualOffset = RcStickTrim.visualOffset(
                            leftStickMode,
                            controlSettings.leftTrimX,
                            controlSettings.leftTrimY,
                        ),
                        trimMode = leftTrimMode,
                        trimX = RcStickTrim.toChannelUnits(controlSettings.leftTrimX),
                        trimY = RcStickTrim.toChannelUnits(controlSettings.leftTrimY),
                        onTrimModeToggle = {
                            leftTrimMode = !leftTrimMode
                            if (leftTrimMode) {
                                rightTrimMode = false
                                publishLeftAtRest()
                            }
                        },
                        onTrimNudge = { axis, steps -> nudgeStickTrim(isLeft = true, axis, steps) },
                        onTrimConfirm = {
                            leftTrimMode = false
                            publishLeftAtRest()
                        },
                        onOptionsMenuOpened = { rcTutorial?.onStickOptionsOpened() },
                    )

                    RcControlZone(
                        modifier = rightSlotModifier
                            .reportTutorialAnchor(TutorialAnchor.RC_STEERING),
                        title = stringResource(R.string.rc_vehicle_control_steering),
                        collapsibleToNearestEdge = true,
                        chromeExpanded = rightStickExpanded,
                        onChromeExpandedChange = { rightStickExpanded = it },
                        stickPosition = rawSteer,
                        mode = rightStickMode,
                        accentEdge = RcGlassAccentEdge.END,
                        joystickSize = joystickSize,
                        fillHeight = rightStickExpanded,
                        resizeMode = stickResizeMode,
                        stickGroupScale = stickGroupScale,
                        onResizeModeChange = { stickResizeMode = it },
                        onStickGroupScaleChange = { next ->
                            onControlSettingsChange { it.copy(stickGroupScale = next) }
                        },
                        maxScale = stickGroupMaxScale,
                        onMove = { x, y ->
                            if (!rightTrimMode) {
                                publishSteer(x, y)
                            }
                        },
                        onBuzzerClick = onBuzzerClick,
                        onStickModeChange = { next ->
                            stickSettingsGeneration += 1
                            onControlSettingsChange { it.copy(rightStickMode = next) }
                            val rest = next.initialPositionNormalized()
                            publishSteer(rest.first, rest.second)
                        },
                        rangeShape = controlSettings.rightStickRangeShape,
                        onRangeShapeChange = { next ->
                            stickSettingsGeneration += 1
                            onControlSettingsChange { it.copy(rightStickRangeShape = next) }
                        },
                        channelLink = controlSettings.rightStickChannels,
                        occupiedChannels = controlSettings.occupiedChannels(exceptRightStick = true),
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(rightStickChannels = next) }
                        },
                        settingsSyncGeneration = stickSettingsGeneration,
                        stickConfigContentDescription = stringResource(
                            R.string.control_panel_widget_config_content_description,
                            stringResource(R.string.rc_vehicle_control_steering),
                        ),
                        visualOffset = RcStickTrim.visualOffset(
                            rightStickMode,
                            controlSettings.rightTrimX,
                            controlSettings.rightTrimY,
                        ),
                        trimMode = rightTrimMode,
                        trimX = RcStickTrim.toChannelUnits(controlSettings.rightTrimX),
                        trimY = RcStickTrim.toChannelUnits(controlSettings.rightTrimY),
                        onTrimModeToggle = {
                            rightTrimMode = !rightTrimMode
                            if (rightTrimMode) {
                                leftTrimMode = false
                                publishRightAtRest()
                            }
                        },
                        onTrimNudge = { axis, steps -> nudgeStickTrim(isLeft = false, axis, steps) },
                        onTrimConfirm = {
                            rightTrimMode = false
                            publishRightAtRest()
                        },
                        onOptionsMenuOpened = { rcTutorial?.onStickOptionsOpened() },
                    )
                    }
                    RcCenterControls(
                        modifier = Modifier
                            .align(
                                RcVehicleProLayout.centerControlsOverlayAlignment(
                                    leftStickExpanded = leftStickExpanded,
                                    rightStickExpanded = rightStickExpanded,
                                ),
                            )
                            .padding(
                                RcVehicleProLayout.centerControlsOverlayPadding(
                                    leftStickExpanded = leftStickExpanded,
                                    rightStickExpanded = rightStickExpanded,
                                ),
                            )
                            .widthIn(max = centerOverlayMaxWidth)
                            .wrapContentWidth()
                            .zIndex(2f)
                            .reportTutorialAnchor(TutorialAnchor.RC_CENTER),
                        fillWidth = false,
                        expanded = centerExpanded,
                        onExpandedChange = { centerExpanded = it },
                        isRecording = chrome.isRecording,
                        lightsOn = chrome.lightsOn,
                        onPhotoClick = onPhotoClick,
                        onRecordClick = onRecordClick,
                        onLightsClick = onLightsClick,
                        throttleTravelPercent = RcStickMapping.travelPercentLabel(
                            controlSettings.throttleTravel,
                        ),
                        steerTravelPercent = RcStickMapping.travelPercentLabel(
                            controlSettings.steerTravel,
                        ),
                        onCycleThrottleTravel = onCycleThrottleTravel,
                        onCycleSteerTravel = onCycleSteerTravel,
                        onOpenDriveAssist = { showDriveAssist = true },
                    )
                    if (plotExpanded && !bothSticksVisible) {
                    val plotAlignment = if (leftStickAlone) {
                        Alignment.TopEnd
                    } else {
                        Alignment.TopStart
                    }
                    RcVehicleHudPlotPanel(
                        uiState = uiFlow,
                        telemetryState = telemetryFlow,
                        leftStick = leftStickFlow,
                        rightStick = rightStickFlow,
                        cameraKnob = cameraKnobFlow,
                        channelRoutingState = routingFlow,
                        plotLabelsState = labelsFlow,
                        controlSettings = controlSettings,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier
                            .align(plotAlignment)
                            .wrapContentWidth()
                            .zIndex(1f)
                            .reportTutorialAnchor(TutorialAnchor.RC_PLOT),
                        expanded = plotExpanded,
                        onExpandedChange = { plotExpanded = it },
                        plotWidth = fittedPlotWidth,
                        plotHeight = fittedPlotHeight,
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        chrome = plotChrome,
                        onChromeChange = onPlotChromeChange,
                    )
                    }
                    if (panExpanded) {
                    val cameraPanMaxScale = RcVehicleProLayout.maxScaleForBaseHeight(
                        baseHeight = RcVehicleProLayout.ControlZoneKnobSize,
                        maxHeight = maxHudHeight,
                    )
                    val cameraPanScale = RcVehicleProLayout.coerceStickGroupScale(
                        scale = controlSettings.cameraPanScale,
                        maxScale = cameraPanMaxScale,
                    )
                    RcLiveCameraPanPanel(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentWidth()
                            .zIndex(1f)
                            .offset(x = if (panExpanded) -panTowardCenterPadding else 0.dp)
                            .reportTutorialAnchor(TutorialAnchor.RC_CAMERA_PAN),
                        expanded = panExpanded,
                        onExpandedChange = { panExpanded = it },
                        valueFlow = cameraKnobFlow,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
                        resizeMode = panResizeMode,
                        panelScale = cameraPanScale,
                        onResizeModeChange = { panResizeMode = it },
                        onPanelScaleChange = { next ->
                            onControlSettingsChange { it.copy(cameraPanScale = next) }
                        },
                        maxScale = cameraPanMaxScale,
                        channelLink = controlSettings.cameraKnobChannel,
                        occupiedChannels = controlSettings.occupiedChannels(exceptCameraKnob = true),
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(cameraKnobChannel = next) }
                        },
                    )
                    }
                }
            }
        }
        val photoNotice = when (chrome.photoFeedback) {
            PhotoFeedback.Saved -> stringResource(R.string.rc_vehicle_photo_saved)
            PhotoFeedback.NoFrame -> stringResource(R.string.rc_vehicle_photo_no_frame)
            PhotoFeedback.Failed -> stringResource(R.string.rc_vehicle_photo_failed)
            PhotoFeedback.None -> null
        }
        if (photoNotice != null) {
            RcHudNoticeBanner(text = photoNotice)
        }
    }
}

private data class PlotHudSeries(
    val radar: List<PlotData>,
    val plots: List<PlotData>,
)

private data class RcHudChrome(
    val cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    val isBluetoothConnected: Boolean = false,
    val isCameraOnline: Boolean = false,
    val isCameraStreamArmed: Boolean = false,
    val isRecording: Boolean = false,
    val lightsOn: Boolean = false,
    val photoFeedback: PhotoFeedback = PhotoFeedback.None,
)

private fun RcVehicleProUiState.toHudChrome() = RcHudChrome(
    cameraLinkProfile = cameraLinkProfile,
    isBluetoothConnected = isBluetoothConnected,
    isCameraOnline = isCameraOnline,
    isCameraStreamArmed = isCameraStreamArmed,
    isRecording = isRecording,
    lightsOn = lightsOn,
    photoFeedback = photoFeedback,
)

private data class RcHudMetricValues(
    val speedKmh: Float = 0f,
    val speedFromTelemetry: Boolean = false,
    val batteryPercent: Int = 0,
    val motorTempCelsius: Int = 0,
    val currentAmps: Float = 0f,
)

private fun RcVehicleProUiState.toMetricValues() = RcHudMetricValues(
    speedKmh = speedKmh,
    speedFromTelemetry = speedFromTelemetry,
    batteryPercent = batteryPercent,
    motorTempCelsius = motorTempCelsius,
    currentAmps = currentAmps,
)

@Composable
private fun RcThrottleSpeedSampler(
    leftStick: StateFlow<Pair<Float, Float>>,
    onThrottleY: (Float) -> Unit,
) {
    val throttleY by remember(leftStick) {
        leftStick.map { it.second }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = leftStick.value.second)
    LaunchedEffect(throttleY) {
        onThrottleY(throttleY)
    }
}

@Composable
private fun RcLiveHudMetrics(
    uiState: StateFlow<RcVehicleProUiState>,
    speedChannel: TelemetryChannel,
    tempChannel: TelemetryChannel,
    currentChannel: TelemetryChannel,
    onSpeedChannelChange: (TelemetryChannel) -> Unit,
    onTempChannelChange: (TelemetryChannel) -> Unit,
    onCurrentChannelChange: (TelemetryChannel) -> Unit,
) {
    val metrics by remember(uiState) {
        uiState.map { it.toMetricValues() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = uiState.value.toMetricValues())
    RcHudMetricsRow(
        uiState = RcVehicleProUiState(
            speedKmh = metrics.speedKmh,
            motorTempCelsius = metrics.motorTempCelsius,
            currentAmps = metrics.currentAmps,
            batteryPercent = metrics.batteryPercent,
        ),
        speedChannel = speedChannel,
        tempChannel = tempChannel,
        currentChannel = currentChannel,
        onSpeedChannelChange = onSpeedChannelChange,
        onTempChannelChange = onTempChannelChange,
        onCurrentChannelChange = onCurrentChannelChange,
    )
}

@Composable
private fun RcLiveHudStatus(
    uiState: StateFlow<RcVehicleProUiState>,
    batteryChannel: TelemetryChannel,
    onBatteryChannelChange: (TelemetryChannel) -> Unit,
) {
    val batteryPercent by remember(uiState) {
        uiState.map { it.batteryPercent }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = uiState.value.batteryPercent)
    RcHudTopBarStatusRow(
        uiState = RcVehicleProUiState(batteryPercent = batteryPercent),
        batteryChannel = batteryChannel,
        onBatteryChannelChange = onBatteryChannelChange,
    )
}

@Composable
private fun RcLiveCameraPanPanel(
    valueFlow: StateFlow<Float>,
    onValueChange: (Float) -> Unit,
    onFrontClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    resizeMode: Boolean = false,
    panelScale: Float = 1f,
    onResizeModeChange: (Boolean) -> Unit = {},
    onPanelScaleChange: (Float) -> Unit = {},
    maxScale: Float = RcVehicleProLayout.STICK_GROUP_SCALE_MAX,
    channelLink: com.micsbol.telecon4esp32.domain.model.KnobChannelLink,
    occupiedChannels: Set<TelemetryChannel>,
    onChannelLinkChange: (com.micsbol.telecon4esp32.domain.model.KnobChannelLink) -> Unit,
) {
    val value by valueFlow.collectAsStateWithLifecycle()
    RcCameraPanPanel(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        value = value,
        onValueChange = onValueChange,
        onFrontClick = onFrontClick,
        resizeMode = resizeMode,
        panelScale = panelScale,
        onResizeModeChange = onResizeModeChange,
        onPanelScaleChange = onPanelScaleChange,
        maxScale = maxScale,
        channelLink = channelLink,
        occupiedChannels = occupiedChannels,
        onChannelLinkChange = onChannelLinkChange,
    )
}

@Composable
private fun RcVehicleHudPlotPanel(
    uiState: StateFlow<RcVehicleProUiState>,
    telemetryState: StateFlow<com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState>,
    leftStick: StateFlow<Pair<Float, Float>>,
    rightStick: StateFlow<Pair<Float, Float>>,
    cameraKnob: StateFlow<Float>,
    channelRoutingState: StateFlow<ChannelRouting>,
    plotLabelsState: StateFlow<List<String>>,
    controlSettings: RcVehicleProControlSettings,
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit,
    onPlotLabelChange: (Int, String) -> Unit,
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    plotWidth: Dp = RcVehicleProLayout.TelemetryPlotWidth,
    plotHeight: Dp = RcVehicleProLayout.TelemetryPlotHeight,
    scanSpan: RadarScanSpan = RadarScanSpan.DEGREES_180,
    onScanSpanChange: (RadarScanSpan) -> Unit = {},
    chrome: RcHudPlotChrome = RcHudPlotChrome(),
    onChromeChange: (RcHudPlotChrome) -> Unit = {},
) {
    val telemetry by telemetryState.collectAsStateWithLifecycle()
    val leftStickXy by leftStick.collectAsStateWithLifecycle()
    val rightStickXy by rightStick.collectAsStateWithLifecycle()
    val cameraKnobValue by cameraKnob.collectAsStateWithLifecycle()
    val channelRouting by channelRoutingState.collectAsStateWithLifecycle()
    val plotLabels by plotLabelsState.collectAsStateWithLifecycle()
    val metrics by remember(uiState) {
        uiState.map { it.toMetricValues() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = uiState.value.toMetricValues())
    val analogHistory = remember { AnalogChannelHistory() }
    remember(telemetry, telemetry.plotState.revision) {
        analogHistory.ingest(telemetry)
        telemetry.plotState.revision
    }
    val plotHudSeries = remember(
        telemetry,
        telemetry.plotState.revision,
        channelRouting,
        plotLabels,
    ) {
        PlotHudSeries(
            radar = TelemetryChannelRouter.radarSeries(telemetry, analogHistory),
            plots = TelemetryChannelRouter.plotSeries(
                telemetry = telemetry,
                routing = channelRouting,
                analogHistory = analogHistory,
                plotLabels = plotLabels,
            ),
        )
    }
    val session = rememberTelemetryPlotSession(
        speedKmh = metrics.speedKmh,
        batteryPercent = metrics.batteryPercent,
        motorTempCelsius = metrics.motorTempCelsius,
        leftStickXy = leftStickXy,
        rightStickXy = rightStickXy,
        fromTelemetry = metrics.speedFromTelemetry,
        controlValues = StickChannelLink.merge(
            controlSettings.leftStickChannels.samples(
                x = leftStickXy.first,
                y = leftStickXy.second,
                axis = controlSettings.leftStickMode.axis,
                restX = controlSettings.leftStickMode.initialPositionNormalized().first,
                restY = controlSettings.leftStickMode.initialPositionNormalized().second,
            ),
            controlSettings.rightStickChannels.samples(
                x = rightStickXy.first,
                y = rightStickXy.second,
                axis = controlSettings.rightStickMode.axis,
                restX = controlSettings.rightStickMode.initialPositionNormalized().first,
                restY = controlSettings.rightStickMode.initialPositionNormalized().second,
            ),
            controlSettings.cameraKnobChannel.sample(cameraKnobValue),
        ),
    )
    RcTelemetryPlotPanel(
        session = session,
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        plotWidth = plotWidth,
        plotHeight = plotHeight,
        radarSeries = plotHudSeries.radar,
        plotSeries = plotHudSeries.plots,
        channelRouting = channelRouting,
        onRadarSourceChange = onRadarSourceChange,
        onPlotLabelChange = onPlotLabelChange,
        onPlotChannelChange = onPlotChannelChange,
        scanSpan = scanSpan,
        onScanSpanChange = onScanSpanChange,
        leftStickXy = leftStickXy,
        rightStickXy = rightStickXy,
        chrome = chrome,
        onChromeChange = onChromeChange,
    )
}

@Preview(showBackground = true, widthDp = 844, heightDp = 390)
@Composable
private fun RcVehicleProContentPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            RcCameraPreview(
                cameraState = CameraStreamState.Idle,
                cameraLinkProfile = CameraLinkProfile.WIFI_SOFTAP,
                isControlConnected = true,
                modifier = Modifier.fillMaxSize(),
            )
            RcVehicleProContent(
                uiState = RcVehicleProUiState(
                    isBluetoothConnected = true,
                    isCameraOnline = true,
                    speedKmh = 18.6f,
                    batteryPercent = 76,
                    motorTempCelsius = 48,
                ),
                onNavigateBack = {},
                settingsAction = {},
                onThrottleMove = { _, _ -> },
                onSteeringMove = { _, _ -> },
                onPhotoClick = {},
                onRecordClick = {},
                onLightsClick = {},
                onBuzzerClick = {},
                onCameraFrontClick = {},
                onCameraKnobChange = {},
                onStopClick = {},
            )
        }
    }
}
