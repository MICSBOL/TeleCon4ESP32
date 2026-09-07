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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.autoConnectSoftApControlWhenCameraOnline
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
import com.micsbol.telecon4esp32.ui.components.LocalDisconnectedBannerInsets
import com.micsbol.telecon4esp32.ui.components.LocalHudSystemBarsHidden
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBluetoothConnecting by bluetoothViewModel.state
        .map { it.isConnecting }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)
    val cameraKnobValue by bluetoothViewModel.rcRightKnobValue.collectAsStateWithLifecycle()
    // Mapped throttle Y after drive-assist pipeline (for HUD speed estimate).
    val leftStickPosition by bluetoothViewModel.rcLeftStickPosition.collectAsStateWithLifecycle()
    val rightStickPosition by bluetoothViewModel.rcRightStickPosition.collectAsStateWithLifecycle()
    val telemetry by bluetoothViewModel.telemetryState.collectAsStateWithLifecycle()
    val channelRouting by bluetoothViewModel.rcChannelRouting.collectAsStateWithLifecycle()
    val plotLabels by bluetoothViewModel.rcPlotDisplayLabels.collectAsStateWithLifecycle()
    val controlSettings by viewModel.controlSettings.collectAsStateWithLifecycle()
    val plotChrome by viewModel.plotChrome.collectAsStateWithLifecycle()
    val bluetoothSession = LocalApplicationBluetoothSession.current
    val onBluetoothConnect by rememberUpdatedState(bluetoothSession?.onConnect)
    val linkProfile = uiState.cameraLinkProfile
    val isWifiSoftApMode = linkProfile == CameraLinkProfile.WIFI_SOFTAP
    val usesWifiLink = isWifiSoftApMode || bluetoothSession?.usesWifiLink == true

    // SoftAP-only (Kit A): once video works, SoftAP is reachable — open TCP control.
    // Overlay (SoftAP video + DevKit Bluetooth): SoftAP HTTP video only; never SoftAP TCP.
    LaunchedEffect(
        linkProfile,
        uiState.isCameraOnline,
        uiState.isBluetoothConnected,
    ) {
        if (
            linkProfile.autoConnectSoftApControlWhenCameraOnline &&
            uiState.isCameraOnline &&
            !uiState.isBluetoothConnected
        ) {
            bluetoothViewModel.ensureWifiSoftApConnected(ApplicationId.RC_VEHICLE_PRO)
        }
    }

    LaunchedEffect(uiState.photoFeedback) {
        when (uiState.photoFeedback) {
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
        viewModel.onScreenVisible()
    }

    LaunchedEffect(uiState.isBluetoothConnected) {
        if (uiState.isBluetoothConnected) {
            bluetoothViewModel.syncRcCameraPanFront()
        }
    }

    LaunchedEffect(leftStickPosition.second) {
        viewModel.updateDriveMetricsFromThrottle(leftStickPosition.second)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenHidden()
            bluetoothViewModel.stopSendingRcData()
        }
    }

    BackHandler {
        navController.navigateUp()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Isolated collector: new SoftAP frames recompose only this layer.
        RcVehicleProCameraLayer(
            cameraPreviewState = viewModel.cameraPreviewState,
            cameraLinkProfile = viewModel.cameraLinkProfile,
            isControlConnected = uiState.isBluetoothConnected,
            onConnectControl = { onBluetoothConnect?.invoke() },
        )

        val showStreamQuality = uiState.isCameraStreamArmed
        val preset by viewModel.softApPerformancePreset.collectAsStateWithLifecycle()
        val hudRate by viewModel.softApHudProcessingRate.collectAsStateWithLifecycle()

        CompositionLocalProvider(LocalHudSystemBarsHidden provides true) {
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
        RcVehicleProContent(
            uiState = uiState,
            controlSettings = controlSettings,
            plotChrome = plotChrome,
            onPlotChromeChange = viewModel::updatePlotChrome,
            leftStickXy = leftStickPosition,
            rightStickXy = rightStickPosition,
            radarSeries = plotHudSeries.radar,
            plotSeries = plotHudSeries.plots,
            channelRouting = channelRouting,
            onRadarSourceChange = bluetoothViewModel::saveChannelBinding,
            onPlotLabelChange = bluetoothViewModel::savePlotLabel,
            onPlotChannelChange = { index, channel ->
                bluetoothViewModel.saveChannelBinding(TelemetrySink.plotAt(index), channel)
            },
            onNavigateBack = { navController.navigateUp() },
            onBluetoothDisconnectedClick = { onBluetoothConnect?.invoke() },
            settingsAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RcHudLinkStatusIcons(
                        profile = linkProfile,
                        isControlConnected = uiState.isBluetoothConnected,
                        isControlConnecting = isBluetoothConnecting,
                        isCameraOnline = uiState.isCameraOnline,
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
                bluetoothViewModel.onLeftSwitchChanged(0, !uiState.lightsOn)
                viewModel.onToggleLights()
            },
            onBuzzerClick = {
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT)
            },
            onCameraFrontClick = {
                bluetoothViewModel.onRightKnobChanged(CAMERA_PAN_CENTER)
                bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT)
            },
            cameraKnobValue = cameraKnobValue,
            onCameraKnobChange = bluetoothViewModel::onRightKnobChanged,
            onStopClick = {
                viewModel.onEmergencyStop()
            },
            onSaveSteerCenter = bluetoothViewModel::saveSteerCenter,
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
    leftStickXy: Pair<Float, Float> = Pair(0f, 0f),
    rightStickXy: Pair<Float, Float> = Pair(0f, 0f),
    radarSeries: List<PlotData> = emptyList(),
    plotSeries: List<PlotData> = emptyList(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLightsClick: () -> Unit,
    onBuzzerClick: () -> Unit,
    onCameraFrontClick: () -> Unit,
    cameraKnobValue: Float,
    onCameraKnobChange: (Float) -> Unit,
    onStopClick: () -> Unit,
    onSaveSteerCenter: () -> Unit = {},
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
    val radarScanSpan = plotChrome.radarSettings.scanSpan
    val telemetryPlotSession = rememberTelemetryPlotSession(
        speedKmh = uiState.speedKmh,
        batteryPercent = uiState.batteryPercent,
        motorTempCelsius = uiState.motorTempCelsius,
        leftStickXy = leftStickXy,
        rightStickXy = rightStickXy,
        fromTelemetry = uiState.speedFromTelemetry,
        controlValues = StickChannelLink.merge(
            controlSettings.leftStickChannels.samples(
                x = rawThrottle.first,
                y = rawThrottle.second,
                axis = controlSettings.leftStickMode.axis,
                radarSpanDegrees = radarScanSpan.degrees,
            ),
            controlSettings.rightStickChannels.samples(
                x = rawSteer.first,
                y = rawSteer.second,
                axis = controlSettings.rightStickMode.axis,
                radarSpanDegrees = radarScanSpan.degrees,
            ),
        ),
    )
    val disconnectedBannerInsets = LocalDisconnectedBannerInsets.current
    DisposableEffect(disconnectedBannerInsets) {
        disconnectedBannerInsets.suppressHostOverlay = true
        onDispose { disconnectedBannerInsets.suppressHostOverlay = false }
    }

    fun publishThrottle(x: Float = rawThrottle.first, y: Float = rawThrottle.second) {
        rawThrottle = Pair(x, y)
        val mapped = RcStickMapping.mapThrottleStick(x, y, controlSettings)
        onThrottleMove(mapped.first, mapped.second)
    }

    fun publishSteer(x: Float = rawSteer.first, y: Float = rawSteer.second) {
        rawSteer = Pair(x, y)
        val mapped = RcStickMapping.mapSteerStick(x, y, controlSettings)
        onSteeringMove(mapped.first, mapped.second)
    }

    fun commitScanSpan(span: RadarScanSpan) {
        onPlotChromeChange(
            plotChrome.copy(radarSettings = plotChrome.radarSettings.copy(scanSpan = span)),
        )
    }

    val leftStickMode = controlSettings.leftStickMode
    val rightStickMode = controlSettings.rightStickMode

    fun publishLeftAtRest(settings: RcVehicleProControlSettings = controlSettings) {
        val rest = settings.leftStickMode.initialPositionNormalized()
        rawThrottle = rest
        val mapped = RcStickMapping.mapThrottleStick(rest.first, rest.second, settings)
        onThrottleMove(mapped.first, mapped.second)
    }

    fun publishRightAtRest(settings: RcVehicleProControlSettings = controlSettings) {
        val rest = settings.rightStickMode.initialPositionNormalized()
        rawSteer = rest
        val mapped = RcStickMapping.mapSteerStick(rest.first, rest.second, settings)
        onSteeringMove(mapped.first, mapped.second)
    }

    fun nudgeStickTrim(isLeft: Boolean, axis: JoystickAxis, steps: Int) {
        val next = if (isLeft) {
            if (axis == JoystickAxis.HORIZONTAL) {
                controlSettings.copy(leftTrimX = RcStickTrim.nudge(controlSettings.leftTrimX, steps))
            } else {
                controlSettings.copy(leftTrimY = RcStickTrim.nudge(controlSettings.leftTrimY, steps))
            }
        } else {
            if (axis == JoystickAxis.HORIZONTAL) {
                controlSettings.copy(rightTrimX = RcStickTrim.nudge(controlSettings.rightTrimX, steps))
            } else {
                controlSettings.copy(rightTrimY = RcStickTrim.nudge(controlSettings.rightTrimY, steps))
            }
        }
        onControlSettingsChange { next }
        if (isLeft) publishLeftAtRest(next) else publishRightAtRest(next)
    }

    // Re-emit mapped channels when dual-rate / expo / reverse / trim change while held.
    LaunchedEffect(controlSettings) {
        val throttle = RcStickMapping.mapThrottleStick(rawThrottle.first, rawThrottle.second, controlSettings)
        onThrottleMove(throttle.first, throttle.second)
        val steer = RcStickMapping.mapSteerStick(rawSteer.first, rawSteer.second, controlSettings)
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
            title = stringResource(R.string.app_rc_vehicle_title),
            onNavigateBack = onNavigateBack,
            expanded = topBarExpanded,
            onExpandedChange = { topBarExpanded = it },
            metricsContent = {
                RcHudMetricsRow(uiState = uiState)
            },
            statusContent = {
                RcHudTopBarStatusRow(uiState = uiState)
            },
            actions = settingsAction,
            hideRowStartContent = {
                if (!plotExpanded) {
                    RcVehicleHudPlotPanel(
                        session = telemetryPlotSession,
                        radarSeries = radarSeries,
                        plotSeries = plotSeries,
                        channelRouting = channelRouting,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier.wrapContentWidth(),
                        expanded = false,
                        onExpandedChange = { plotExpanded = it },
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        leftStickXy = leftStickXy,
                        rightStickXy = rightStickXy,
                        chrome = plotChrome,
                        onChromeChange = onPlotChromeChange,
                    )
                }
            },
            hideRowEndContent = {
                if (!panExpanded) {
                    RcCameraPanPanel(
                        modifier = Modifier.wrapContentWidth(),
                        expanded = false,
                        onExpandedChange = { panExpanded = it },
                        value = cameraKnobValue,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
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
                        session = telemetryPlotSession,
                        radarSeries = radarSeries,
                        plotSeries = plotSeries,
                        channelRouting = channelRouting,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .wrapContentWidth()
                            .zIndex(1.5f),
                        expanded = plotExpanded,
                        onExpandedChange = { plotExpanded = it },
                        plotWidth = fittedPlotWidth,
                        plotHeight = fittedPlotHeight,
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        leftStickXy = leftStickXy,
                        rightStickXy = rightStickXy,
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
                        modifier = leftSlotModifier,
                        title = stringResource(R.string.rc_vehicle_control_throttle),
                        collapsibleToNearestEdge = true,
                        chromeExpanded = leftStickExpanded,
                        onChromeExpandedChange = { leftStickExpanded = it },
                        stickPosition = if (leftTrimMode) {
                            leftStickMode.initialPositionNormalized()
                        } else {
                            rawThrottle
                        },
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
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(leftStickChannels = next) }
                        },
                        settingsSyncGeneration = stickSettingsGeneration,
                        stickConfigContentDescription = stringResource(
                            R.string.control_panel_widget_config_content_description,
                            stringResource(R.string.rc_vehicle_control_throttle),
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
                        collapsedIcon = Icons.Filled.SwapVert,
                    )

                    RcControlZone(
                        modifier = rightSlotModifier,
                        title = stringResource(R.string.rc_vehicle_control_steering),
                        collapsibleToNearestEdge = true,
                        chromeExpanded = rightStickExpanded,
                        onChromeExpandedChange = { rightStickExpanded = it },
                        stickPosition = if (rightTrimMode) {
                            rightStickMode.initialPositionNormalized()
                        } else {
                            rawSteer
                        },
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
                        onChannelLinkChange = { next ->
                            onControlSettingsChange { it.copy(rightStickChannels = next) }
                        },
                        settingsSyncGeneration = stickSettingsGeneration,
                        stickConfigContentDescription = stringResource(
                            R.string.control_panel_widget_config_content_description,
                            stringResource(R.string.rc_vehicle_control_steering),
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
                            if (rightStickMode.axis != JoystickAxis.VERTICAL) {
                                onSaveSteerCenter()
                            }
                            rightTrimMode = false
                            publishRightAtRest()
                        },
                        collapsedIcon = Icons.Filled.SwapHoriz,
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
                            .zIndex(2f),
                        fillWidth = false,
                        expanded = centerExpanded,
                        onExpandedChange = { centerExpanded = it },
                        isRecording = uiState.isRecording,
                        lightsOn = uiState.lightsOn,
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
                        session = telemetryPlotSession,
                        radarSeries = radarSeries,
                        plotSeries = plotSeries,
                        channelRouting = channelRouting,
                        onRadarSourceChange = onRadarSourceChange,
                        onPlotLabelChange = onPlotLabelChange,
                        onPlotChannelChange = onPlotChannelChange,
                        modifier = Modifier
                            .align(plotAlignment)
                            .wrapContentWidth()
                            .zIndex(1f),
                        expanded = plotExpanded,
                        onExpandedChange = { plotExpanded = it },
                        plotWidth = fittedPlotWidth,
                        plotHeight = fittedPlotHeight,
                        scanSpan = radarScanSpan,
                        onScanSpanChange = ::commitScanSpan,
                        leftStickXy = leftStickXy,
                        rightStickXy = rightStickXy,
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
                    RcCameraPanPanel(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentWidth()
                            .zIndex(1f)
                            .offset(x = if (panExpanded) -panTowardCenterPadding else 0.dp),
                        expanded = panExpanded,
                        onExpandedChange = { panExpanded = it },
                        value = cameraKnobValue,
                        onValueChange = onCameraKnobChange,
                        onFrontClick = onCameraFrontClick,
                        resizeMode = panResizeMode,
                        panelScale = cameraPanScale,
                        onResizeModeChange = { panResizeMode = it },
                        onPanelScaleChange = { next ->
                            onControlSettingsChange { it.copy(cameraPanScale = next) }
                        },
                        maxScale = cameraPanMaxScale,
                    )
                    }
                }
            }
        }
        val photoNotice = when (uiState.photoFeedback) {
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

@Composable
private fun RcVehicleHudPlotPanel(
    session: RcTelemetryPlotSession,
    radarSeries: List<PlotData>,
    plotSeries: List<PlotData>,
    channelRouting: ChannelRouting,
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
    leftStickXy: Pair<Float, Float> = Pair(0f, 0f),
    rightStickXy: Pair<Float, Float> = Pair(0f, 0f),
    chrome: RcHudPlotChrome = RcHudPlotChrome(),
    onChromeChange: (RcHudPlotChrome) -> Unit = {},
) {
    RcTelemetryPlotPanel(
        session = session,
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        plotWidth = plotWidth,
        plotHeight = plotHeight,
        radarSeries = radarSeries,
        plotSeries = plotSeries,
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
                cameraKnobValue = 0.5f,
                onCameraKnobChange = {},
                onStopClick = {},
            )
        }
    }
}
