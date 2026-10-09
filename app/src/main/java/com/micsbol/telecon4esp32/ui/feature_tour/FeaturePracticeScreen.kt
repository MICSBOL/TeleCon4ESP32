package com.micsbol.telecon4esp32.ui.feature_tour

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.IndicatorState
import com.micsbol.telecon4esp32.domain.bluetooth.PanelState
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.PlotState
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.KnobChannelLink
import com.micsbol.telecon4esp32.domain.model.PlotGraphMode
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.control_panel.CartesianPlot
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelRadarDisplay
import com.micsbol.telecon4esp32.ui.control_panel.RadarDisplaySettings
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelStickDisplay
import com.micsbol.telecon4esp32.ui.control_panel.KnobOptionsMenu
import com.micsbol.telecon4esp32.ui.control_panel.StickOptionsMenu
import com.micsbol.telecon4esp32.ui.control_panel.TelemetryWidgetOptionsMenu
import com.micsbol.telecon4esp32.ui.control_panel.components.AnalogIndicator
import com.micsbol.telecon4esp32.ui.control_panel.components.BatteryStatus
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import com.micsbol.telecon4esp32.ui.control_panel.components.Joystick_RC3D
import com.micsbol.telecon4esp32.ui.control_panel.components.Knob3D
import com.micsbol.telecon4esp32.ui.control_panel.components.PushButtonSide
import com.micsbol.telecon4esp32.ui.control_panel.components.SevenSegmentedPanel
import com.micsbol.telecon4esp32.ui.control_panel.components.Switch3DButton
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcHudMetricReading
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProUiState
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCameraPanPanel
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcStickMapping
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcDriveAssistDialog
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcCenterControls
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcControlZone
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcGlassAccentEdge
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudLinkStatusIcons
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudMetricsRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudTopBarStatusRow
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcVehicleHudTopBar
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcHudPlotChrome
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcTelemetryPlotPanel
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcTelemetryPlotSession
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val PRACTICE_MOVE_THRESHOLD = 0.12f
private const val PRACTICE_GAUGE_VALUE = 64
private const val PRACTICE_NUMERIC_VALUE = 248.6f

@Composable
fun FeaturePracticeScreen(
    applicationId: ApplicationId,
    featureId: ModuleFeatureId,
    navController: NavController,
) {
    val feature = remember(applicationId, featureId) { moduleFeature(applicationId, featureId) }
    if (feature == null) {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }
    val cursor = remember(feature.id) { FeatureTourCursor(feature.steps.size) }
    val leave: () -> Unit = { navController.navigateUp() }
    BackHandler(onBack = leave)
    LaunchedEffect(cursor.finished) {
        if (cursor.finished) leave()
    }
    val step = feature.steps[cursor.index]
    var practiceConsumed by remember(cursor.index) { mutableStateOf(false) }
    val onPracticed = {
        if (step.advanceOnPractice && !practiceConsumed) {
            practiceConsumed = true
            cursor.onPracticed(true)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neo.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            FeaturePracticeStage(
                featureId = feature.id,
                stepIndex = cursor.index,
                onPracticed = onPracticed,
            )
        }
        FeatureTourCard(
            step = step,
            index = cursor.index,
            count = feature.steps.size,
            onClose = leave,
            onNext = cursor::next,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}

@Composable
private fun FeaturePracticeStage(
    featureId: ModuleFeatureId,
    stepIndex: Int,
    onPracticed: () -> Unit,
) {
    when (featureId) {
        ModuleFeatureId.CP_STICKS -> PracticeStick(onPracticed)
        ModuleFeatureId.CP_KNOBS -> PracticeKnob(onPracticed)
        ModuleFeatureId.CP_PLOTS -> PracticePlots()
        ModuleFeatureId.CP_STICK_PATH -> PracticeStickPath()
        ModuleFeatureId.CP_CAMERA -> PracticeCameraPane()
        ModuleFeatureId.CP_RADAR -> PracticeRadar()
        ModuleFeatureId.CP_SWITCHES -> PracticeSwitches(onPracticed)
        ModuleFeatureId.CP_BUTTONS -> PracticeButtons(onPracticed)
        ModuleFeatureId.CP_ANALOG -> PracticeAnalog(onPracticed)
        ModuleFeatureId.CP_BATTERY -> PracticeBattery(onPracticed)
        ModuleFeatureId.CP_NUMERIC -> PracticeNumeric(onPracticed)
        ModuleFeatureId.RC_TOP_BAR -> PracticeStatusBar(stepIndex, onPracticed)
        ModuleFeatureId.RC_THROTTLE -> PracticeRcStick(
            title = stringResource(R.string.rc_vehicle_control_throttle),
            initialMode = JoystickMode.VerticalSpring(),
            accentEdge = RcGlassAccentEdge.START,
            onPracticed = onPracticed,
        )
        ModuleFeatureId.RC_STEERING -> PracticeRcStick(
            title = stringResource(R.string.rc_vehicle_control_steering),
            initialMode = JoystickMode.HorizontalSpring(),
            accentEdge = RcGlassAccentEdge.END,
            onPracticed = onPracticed,
            showBuzzer = true,
        )
        ModuleFeatureId.RC_CENTER -> PracticeCameraActions(onPracticed)
        ModuleFeatureId.RC_PLOT -> PracticePlot()
        ModuleFeatureId.RC_PAN -> PracticeCameraPan(onPracticed)
    }
}

@Composable
private fun PracticeStick(onPracticed: () -> Unit) {
    var mode by remember { mutableStateOf<JoystickMode>(JoystickMode.Spring()) }
    var positionX by remember { mutableFloatStateOf(0f) }
    var positionY by remember { mutableFloatStateOf(0f) }
    var menuOpen by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Joystick_RC3D(
                modifier = Modifier.size(220.dp),
                mode = mode,
                onMove = { x, y ->
                    positionX = x
                    positionY = y
                    if (abs(x) > PRACTICE_MOVE_THRESHOLD || abs(y) > PRACTICE_MOVE_THRESHOLD) {
                        onPracticed()
                    }
                },
                onDoubleTap = { menuOpen = true },
            )
            StickOptionsMenu(
                expanded = menuOpen,
                selectedMode = mode,
                onModeSelected = { mode = it },
                onDismiss = { menuOpen = false },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PracticeReadout(stringResource(R.string.feature_stick_axes, positionX, positionY))
    }
}

@Composable
private fun PracticeKnob(onPracticed: () -> Unit) {
    var value by remember { mutableFloatStateOf(0.35f) }
    var menuOpen by remember { mutableStateOf(false) }
    var channelLink by remember { mutableStateOf(KnobChannelLink.DEFAULT) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Knob3D(
                value = value,
                onValueChange = { next ->
                    if (abs(next - value) > 0.04f) onPracticed()
                    value = next
                },
                onDoubleTap = { menuOpen = true },
            )
            KnobOptionsMenu(
                expanded = menuOpen,
                channelLink = channelLink,
                onChannelLinkChange = { channelLink = it },
                onDismiss = { menuOpen = false },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PracticeReadout(stringResource(R.string.feature_knob_value, (value * 255f).roundToInt()))
    }
}

@Composable
private fun PracticePlots() {
    val series = remember {
        listOf(
            PlotData("CH1", sampleWave(0.15f), 0xFF00E5FF.toInt()),
            PlotData("CH2", sampleWave(0.55f), 0xFFFF9800.toInt()),
            PlotData("CH3", sampleWave(1.1f), 0xFF69F0AE.toInt()),
            PlotData("CH4", sampleWave(1.7f), 0xFFE040FB.toInt()),
        )
    }
    var plotVisible by remember { mutableStateOf(List(4) { true }) }
    var graphModes by remember { mutableStateOf(List(4) { PlotGraphMode.CONTINUOUS }) }
    var lineStyles by remember { mutableStateOf(List(4) { PlotLineStyle.LINE }) }
    var plotOnTop by remember { mutableStateOf(List(4) { false }) }
    CartesianPlot(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        series = series,
        plotVisible = plotVisible,
        graphModes = graphModes,
        lineStyles = lineStyles,
        plotOnTop = plotOnTop,
        onPlotVisibleChange = { plotVisible = it },
        onGraphModesChange = { graphModes = it },
        onLineStylesChange = { lineStyles = it },
        onPlotOnTopChange = { plotOnTop = it },
    )
}

private fun sampleWave(phase: Float): List<Float> = List(32) { index ->
    val wave = sin(index * 0.45f + phase)
    ((wave + 1f) / 2f).coerceIn(0.05f, 0.95f)
}

@Composable
private fun PracticeStickPath() {
    var left by remember { mutableStateOf(0f to 0f) }
    var right by remember { mutableStateOf(0f to 0f) }
    var leftLink by remember { mutableStateOf(StickChannelLink.DEFAULT_LEFT) }
    var rightLink by remember { mutableStateOf(StickChannelLink.DEFAULT_RIGHT) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var phase = 0.0
        while (true) {
            val x = cos(phase).toFloat()
            val y = sin(phase).toFloat()
            left = x * 0.92f to y * 0.92f
            right = JoystickRangeShape.SQUARE.mapOutput(x, y)
            phase += 0.04
            if (phase > Math.PI * 2) phase -= Math.PI * 2
            delay(40)
        }
    }
    ControlPanelStickDisplay(
        leftStickXy = left,
        rightStickXy = right,
        leftLink = leftLink,
        rightLink = rightLink,
        onLeftLinkChange = { leftLink = it },
        onRightLinkChange = { rightLink = it },
        showSettings = showSettings,
        onShowSettingsChange = { showSettings = it },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PracticeCameraPane() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101820))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.control_panel_center_locked_camera_message),
            color = Neo.TextPrimary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PracticeRadar() {
    var settings by remember { mutableStateOf(RadarDisplaySettings()) }
    ControlPanelRadarDisplay(
        series = emptyList(),
        settings = settings,
        onSettingsChange = { settings = it },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PracticeSwitches(onPracticed: () -> Unit) {
    var states by remember { mutableStateOf(listOf(false, true, false)) }
    val onLabel = stringResource(R.string.feature_value_on)
    val offLabel = stringResource(R.string.feature_value_off)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            states.forEachIndexed { index, isOn ->
                Switch3DButton(
                    modifier = Modifier.size(72.dp),
                    isOn = isOn,
                    onStateChange = { next ->
                        states = states.toMutableList().also { it[index] = next }
                        onPracticed()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        PracticeReadout(
            stringResource(
                R.string.feature_switch_readout,
                if (states[0]) onLabel else offLabel,
                if (states[1]) onLabel else offLabel,
                if (states[2]) onLabel else offLabel,
            ),
        )
    }
}

@Composable
private fun PracticeButtons(onPracticed: () -> Unit) {
    var presses by remember { mutableIntStateOf(0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            PushButtonSide(
                modifier = Modifier.size(96.dp),
                side = ButtonSide.LEFT,
                onPress = {
                    presses += 1
                    onPracticed()
                },
            )
            PushButtonSide(
                modifier = Modifier.size(96.dp),
                side = ButtonSide.RIGHT,
                onPress = {
                    presses += 1
                    onPracticed()
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PracticeReadout(stringResource(R.string.feature_press_count, presses))
    }
}

@Composable
private fun PracticeAnalog(onPracticed: () -> Unit) {
    PracticeTelemetryGauge(
        sink = TelemetrySink.ANALOG_GAUGE,
        initialChannel = TelemetryChannel.ANALOG,
        initialLabel = "SPEED",
        onPracticed = onPracticed,
    ) { label, openMenu ->
        AnalogIndicator(
            value = PRACTICE_GAUGE_VALUE,
            title = label,
            modifier = Modifier.width(240.dp),
            onDoubleTap = openMenu,
        )
    }
}

@Composable
private fun PracticeBattery(onPracticed: () -> Unit) {
    PracticeTelemetryGauge(
        sink = TelemetrySink.BATTERY_GAUGE,
        initialChannel = TelemetryChannel.BATT,
        initialLabel = "BATTERY",
        onPracticed = onPracticed,
    ) { label, openMenu ->
        BatteryStatus(
            level = PRACTICE_GAUGE_VALUE,
            title = label,
            modifier = Modifier.width(240.dp),
            onDoubleTap = openMenu,
        )
    }
}

@Composable
private fun PracticeTelemetryGauge(
    sink: TelemetrySink,
    initialChannel: TelemetryChannel,
    initialLabel: String,
    onPracticed: () -> Unit,
    gauge: @Composable (label: String, openMenu: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var channel by remember { mutableStateOf(initialChannel) }
    var label by remember { mutableStateOf(initialLabel) }
    val openMenu = {
        menuOpen = true
        onPracticed()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopCenter) {
            gauge(label, openMenu)
            TelemetryWidgetOptionsMenu(
                expanded = menuOpen,
                sink = sink,
                selectedChannel = channel,
                widgetLabel = label,
                onChannelSelected = { channel = it },
                onLabelChange = { label = it },
                onDismiss = { menuOpen = false },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PracticeReadout(stringResource(R.string.feature_gauge_value, PRACTICE_GAUGE_VALUE))
    }
}

@Composable
private fun PracticeNumeric(onPracticed: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var channel by remember { mutableStateOf(TelemetryChannel.PANEL_LEFT) }
    var label by remember { mutableStateOf("VALUE") }
    var panelOn by remember { mutableStateOf(true) }
    var green by remember { mutableStateOf(true) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopCenter) {
            SevenSegmentedPanel(
                value = PRACTICE_NUMERIC_VALUE,
                on = panelOn,
                onColor = if (green) Color(0xFF39FF14) else Color(0xFFFF1744),
                width = 220.dp,
                title = label,
                onDoubleTap = {
                    menuOpen = true
                    onPracticed()
                },
            )
            TelemetryWidgetOptionsMenu(
                expanded = menuOpen,
                sink = TelemetrySink.PANEL_LEFT,
                selectedChannel = channel,
                widgetLabel = label,
                onChannelSelected = { channel = it },
                onLabelChange = { label = it },
                onDismiss = { menuOpen = false },
                panelOn = panelOn,
                onPanelOnChange = { panelOn = it },
                panelColorGreen = green,
                onPanelColorGreenChange = { green = it },
            )
        }
    }
}

@Composable
private fun PracticeStatusBar(stepIndex: Int, onPracticed: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    val sample = remember { practiceHudTelemetry() }
    var speedChannel by remember { mutableStateOf(TelemetryChannel.PANEL_LEFT) }
    var tempChannel by remember { mutableStateOf(TelemetryChannel.ANALOG) }
    var currentChannel by remember { mutableStateOf(TelemetryChannel.CH_5) }
    var batteryChannel by remember { mutableStateOf(TelemetryChannel.BATT) }
    val uiState = RcVehicleProUiState(
        speedKmh = RcHudMetricReading.speedKmh(speedChannel, sample) ?: 0f,
        motorTempCelsius = RcHudMetricReading.tempCelsius(tempChannel, sample),
        currentAmps = RcHudMetricReading.currentAmps(currentChannel, sample),
        batteryPercent = RcHudMetricReading.batteryPercent(batteryChannel, sample),
        isBluetoothConnected = true,
        isCameraOnline = true,
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        RcVehicleHudTopBar(
            title = stringResource(R.string.app_rc_vehicle_title),
            onNavigateBack = {},
            expanded = expanded,
            onExpandedChange = { expanded = it },
            metricsContent = {
                PracticeBarFocus(active = stepIndex == 0) {
                    RcHudMetricsRow(
                        uiState = uiState,
                        speedChannel = speedChannel,
                        tempChannel = tempChannel,
                        currentChannel = currentChannel,
                        onSpeedChannelChange = {
                            speedChannel = it
                            onPracticed()
                        },
                        onTempChannelChange = {
                            tempChannel = it
                            onPracticed()
                        },
                        onCurrentChannelChange = {
                            currentChannel = it
                            onPracticed()
                        },
                    )
                }
            },
            statusContent = {
                PracticeBarFocus(active = stepIndex == 0) {
                    RcHudTopBarStatusRow(
                        uiState = uiState,
                        batteryChannel = batteryChannel,
                        onBatteryChannelChange = {
                            batteryChannel = it
                            onPracticed()
                        },
                    )
                }
            },
            actions = {
                PracticeBarFocus(active = stepIndex == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.feature_menu_open),
                                tint = Neo.Accent,
                            )
                        }
                        RcHudLinkStatusIcons(
                            profile = CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT,
                            isControlConnected = true,
                            isControlConnecting = false,
                            isCameraOnline = true,
                            usesWifiControl = false,
                            onDisconnectedClick = {},
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun PracticeBarFocus(
    active: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (active) {
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Neo.Accent, RoundedCornerShape(16.dp))
                .padding(2.dp)
        } else {
            Modifier
        },
    ) {
        content()
    }
}

@Composable
private fun PracticeRcStick(
    title: String,
    initialMode: JoystickMode,
    accentEdge: RcGlassAccentEdge,
    onPracticed: () -> Unit,
    showBuzzer: Boolean = false,
) {
    var position by remember { mutableStateOf(0f to 0f) }
    var mode by remember { mutableStateOf(initialMode) }
    var trimMode by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        RcControlZone(
            title = title,
            stickPosition = position,
            mode = mode,
            onMove = { x, y ->
                position = x to y
                if (abs(x) > PRACTICE_MOVE_THRESHOLD || abs(y) > PRACTICE_MOVE_THRESHOLD) {
                    onPracticed()
                }
            },
            accentEdge = accentEdge,
            joystickSize = 200.dp,
            onStopClick = { position = 0f to 0f },
            onBuzzerClick = if (showBuzzer) onPracticed else null,
            onStickModeChange = { mode = it },
            onTrimModeToggle = { trimMode = !trimMode },
            trimMode = trimMode,
            onTrimNudge = { _, _ -> },
            onTrimConfirm = { trimMode = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        PracticeReadout(stringResource(R.string.feature_stick_axes, position.first, position.second))
    }
}

@Composable
private fun PracticeCameraActions(onPracticed: () -> Unit) {
    var lightsOn by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var assistOpen by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(RcVehicleProControlSettings()) }
    RcCenterControls(
        isRecording = recording,
        lightsOn = lightsOn,
        onPhotoClick = onPracticed,
        onRecordClick = {
            recording = !recording
            onPracticed()
        },
        onLightsClick = {
            lightsOn = !lightsOn
            onPracticed()
        },
        throttleTravelPercent = RcStickMapping.travelPercentLabel(settings.throttleTravel),
        steerTravelPercent = RcStickMapping.travelPercentLabel(settings.steerTravel),
        onCycleThrottleTravel = {
            settings = settings.copy(
                throttleTravel = RcStickMapping.nextTravelPreset(settings.throttleTravel),
            )
            onPracticed()
        },
        onCycleSteerTravel = {
            settings = settings.copy(
                steerTravel = RcStickMapping.nextTravelPreset(settings.steerTravel),
            )
            onPracticed()
        },
        onOpenDriveAssist = { assistOpen = true },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    if (assistOpen) {
        RcDriveAssistDialog(
            settings = settings,
            onSettingsChange = { settings = it },
            onDismiss = { assistOpen = false },
        )
    }
}

@Composable
private fun PracticePlot() {
    var chrome by remember { mutableStateOf(RcHudPlotChrome()) }
    var expanded by remember { mutableStateOf(true) }
    var routing by remember { mutableStateOf(ChannelRouting.defaults()) }
    var labels by remember { mutableStateOf(List(UserSettings.PLOT_LABEL_COUNT) { "" }) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    val stickTrail = remember {
        List(64) { index ->
            val angle = index / 64.0 * Math.PI * 2
            val x = cos(angle).toFloat()
            val y = sin(angle).toFloat()
            (x * 0.72f) to (y * 0.72f) to JoystickRangeShape.SQUARE.mapOutput(x, y)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(48)
            elapsed += 0.04f
        }
    }
    val angle = elapsed * 0.35
    val circle = cos(angle).toFloat() to sin(angle).toFloat()
    val square = JoystickRangeShape.SQUARE.mapOutput(circle.first, circle.second)
    val session = RcTelemetryPlotSession(
        battery = List(24) { index -> 70f + sin(index * 0.4f + elapsed) * 12f },
        temp = List(24) { index -> 42f + sin(index * 0.35f + elapsed * 0.8f) * 8f },
        leftStickX = stickTrail.map { it.first.first },
        leftStickY = stickTrail.map { it.first.second },
        rightStickX = stickTrail.map { it.second.first },
        rightStickY = stickTrail.map { it.second.second },
        sessionPeak = 22.4f,
        sessionAvg = 14.2f,
        sessionDistanceKm = 1.6f,
        sessionMinBattery = 61f,
        sessionMaxTemp = 48f,
        sessionElapsedSec = elapsed,
    )
    RcTelemetryPlotPanel(
        session = session,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        plotSeries = labels.map { PlotData(name = it) },
        channelRouting = routing,
        onRadarSourceChange = { sink, channel -> routing = routing.with(sink, channel) },
        onPlotLabelChange = { index, label ->
            labels = labels.toMutableList().also { it[index] = label }
        },
        onPlotChannelChange = { index, channel ->
            routing = routing.with(TelemetrySink.plotAt(index), channel)
        },
        scanSpan = chrome.radarSettings.scanSpan,
        leftStickXy = circle.first * 0.72f to circle.second * 0.72f,
        rightStickXy = square,
        chrome = chrome,
        onChromeChange = { chrome = it },
        showRadarSettings = true,
        plotWidth = 300.dp,
        plotHeight = 180.dp,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun PracticeCameraPan(onPracticed: () -> Unit) {
    var value by remember { mutableFloatStateOf(RcVehicleProLayout.CAMERA_PAN_CENTER) }
    var expanded by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(false) }
    var panelScale by remember { mutableFloatStateOf(1f) }
    var channelLink by remember { mutableStateOf(KnobChannelLink.DEFAULT) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RcCameraPanPanel(
            value = value,
            onValueChange = { next ->
                if (abs(next - value) > 0.04f) onPracticed()
                value = next
            },
            onFrontClick = { value = RcVehicleProLayout.CAMERA_PAN_CENTER },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            resizeMode = resizeMode,
            panelScale = panelScale,
            onResizeModeChange = { resizeMode = it },
            onPanelScaleChange = { panelScale = it },
            channelLink = channelLink,
            onChannelLinkChange = { channelLink = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        PracticeReadout(
            stringResource(
                R.string.rc_vehicle_camera_pan_value,
                RcVehicleProLayout.cameraPanRaw(value),
            ),
        )
    }
}

@Composable
private fun PracticeReadout(text: String) {
    Text(
        text = text,
        color = Neo.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FeatureTourCard(
    step: FeatureTourStep,
    index: Int,
    count: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 16.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.first_connection_tutorial_step_progress,
                        index + 1,
                        count,
                    ),
                    color = Neo.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(step.titleRes),
                    color = Neo.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.first_connection_tutorial_close),
                tint = Neo.TextSecondary,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose, role = Role.Button)
                    .padding(4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(step.bodyRes),
            color = Neo.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier
                .heightIn(max = 96.dp)
                .verticalScroll(rememberScrollState()),
        )
        if (!step.advanceOnPractice) {
            Spacer(modifier = Modifier.height(12.dp))
            NeoPillButton(
                text = stringResource(
                    if (index == count - 1) {
                        R.string.first_connection_tutorial_done
                    } else {
                        R.string.first_connection_tutorial_next
                    },
                ),
                onClick = onNext,
                compact = true,
            )
        }
    }
}

/** Sample link so each channel paints a different gauge value in the lesson. */
private fun practiceHudTelemetry(): TelemetryState = TelemetryState(
    panelState = PanelState(leftValue = 184, rightValue = 255),
    indicatorState = IndicatorState(analogValue = 85, batteryLevel = 86),
    plotState = PlotState(
        series = List(8) { index ->
            PlotData(dataPoints = listOf((40 + index * 20) / 255f))
        },
    ),
)
