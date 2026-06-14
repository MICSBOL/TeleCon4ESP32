package com.micsbol.telecon4esp32.ui.control_panel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.domain.bluetooth.IndicatorState
import com.micsbol.telecon4esp32.domain.bluetooth.PanelState
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.ui.bluetooth.RcControlState
import com.micsbol.telecon4esp32.ui.control_panel.components.AnalogIndicator
import com.micsbol.telecon4esp32.ui.control_panel.components.BatteryStatus
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.ads.InterstitialTrigger
import com.micsbol.telecon4esp32.ui.ads.rememberNavigateWithInterstitial
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsUiState
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun ControlPanelScreen(
    bluetoothViewModel: BluetoothViewModel? = null,
    telemetryState: StateFlow<TelemetryState>? = null,
    userSettings: StateFlow<SettingsUiState>? = null,
    rcControlState: StateFlow<RcControlState>? = null,
    navController: NavHostController? = null
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val actualViewModel = bluetoothViewModel
    val actualTelemetryState = telemetryState ?: bluetoothViewModel?.telemetryState ?: MutableStateFlow(TelemetryState())
    val actualUserSettings = userSettings ?: bluetoothViewModel?.userSettings ?: MutableStateFlow(SettingsUiState.Loading)
    val actualRcControlState = rcControlState ?: bluetoothViewModel?.rcControlState ?: MutableStateFlow(RcControlState())

    if (actualViewModel == null && telemetryState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
        return
    }

    val exitRcToHome = rememberNavigateWithInterstitial(
        trigger = InterstitialTrigger.RC_EXIT,
        onNavigate = {
            actualViewModel?.stopSendingRcData()
            navController?.popBackStack(Screen.Home.route, inclusive = false)
        },
    )

    BackHandler(enabled = navController != null) {
        exitRcToHome()
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
    }

    val collectedUserSettings by actualUserSettings.collectAsState()
    val settingsSyncGeneration by (bluetoothViewModel?.rcSettingsSyncGeneration
        ?: MutableStateFlow(0)).collectAsState()

    when (val state = collectedUserSettings) {
        is SettingsUiState.Loading -> {
            CircularProgressIndicator()
        }

        is SettingsUiState.Error -> {
            Text("Error: ${state.message}")
        }

        is SettingsUiState.Success -> {
            LaunchedEffect(actualViewModel, state.settings) {
                actualViewModel?.onControlPanelEntered()
            }

            DisposableEffect(actualViewModel) {
                onDispose {
                    actualViewModel?.stopSendingRcData()
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                LaunchedEffect(Unit) {
                    val dpWidth = with(density) { maxWidth }
                    val dpHeight = with(density) { maxHeight }
                    val dpi = density.density * 160
                    Log.d("DeviceMetrics", "Width: ${dpWidth}, Height: ${dpHeight}, DPI: $dpi")
                }

                val screenAspectRatio = maxWidth / maxHeight

                // ── Stable callbacks ───────────────────────────────────────────────────
                // remember(bluetoothViewModel) means the same lambda object is reused on
                // every recomposition, allowing Compose to skip ControllerSide entirely
                // when only the joystick position (which is NOT a ControllerSide param)
                // changes.
                val onLeftMove: (Float, Float) -> Unit = remember(bluetoothViewModel) {
                    { x, y -> bluetoothViewModel?.onLeftStickChanged(x, y) }
                }
                val onRightMove: (Float, Float) -> Unit = remember(bluetoothViewModel) {
                    { x, y -> bluetoothViewModel?.onRightStickChanged(x, y) }
                }
                val onLeftSwitchChange: (Int, Boolean) -> Unit = remember(bluetoothViewModel) {
                    { idx, v -> bluetoothViewModel?.onLeftSwitchChanged(idx, v) }
                }
                val onRightSwitchChange: (Int, Boolean) -> Unit = remember(bluetoothViewModel) {
                    { idx, v -> bluetoothViewModel?.onRightSwitchChanged(idx, v) }
                }
                val onLeftKnobChange: (Float) -> Unit = remember(bluetoothViewModel) {
                    { v -> bluetoothViewModel?.onLeftKnobChanged(v) }
                }
                val onRightKnobChange: (Float) -> Unit = remember(bluetoothViewModel) {
                    { v -> bluetoothViewModel?.onRightKnobChanged(v) }
                }
                val onTopLeftPress: () -> Unit = remember(bluetoothViewModel) {
                    { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) }
                }
                val onBottomLeftPress: () -> Unit = remember(bluetoothViewModel) {
                    { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) }
                }
                val onTopRightPress: () -> Unit = remember(bluetoothViewModel) {
                    { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) }
                }
                val onBottomRightPress: () -> Unit = remember(bluetoothViewModel) {
                    { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) }
                }

                Image(
                    painter = painterResource(id = R.drawable.plastic_background),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (navController != null) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(8.dp),
                        onClick = {
                            navController.navigateToApplicationSettings(ApplicationId.CONTROL_PANEL)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(
                                R.string.applications_settings_content_description,
                                stringResource(R.string.app_control_panel_settings_title),
                            ),
                            tint = brandPrimary(),
                        )
                    }
                }

                // Row keeps plot/center recompositions isolated from the side controller trees.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .fillMaxHeight(),
                    ) {
                        ControlPanelLeftControllerHost(
                            bluetoothViewModel = bluetoothViewModel,
                            telemetryState = actualTelemetryState,
                            rcControlState = actualRcControlState,
                            settings = state.settings,
                            aspectRatio = screenAspectRatio,
                            settingsSyncGeneration = settingsSyncGeneration,
                            onMove = onLeftMove,
                            onSwitchStateChange = onLeftSwitchChange,
                            onKnobValueChange = onLeftKnobChange,
                            onTopPress = onTopLeftPress,
                            onBottomPress = onBottomLeftPress,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        ControlPanelCenterPlotHost(
                            bluetoothViewModel = bluetoothViewModel,
                            telemetryState = actualTelemetryState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .fillMaxHeight(),
                    ) {
                        ControlPanelRightControllerHost(
                            bluetoothViewModel = bluetoothViewModel,
                            telemetryState = actualTelemetryState,
                            rcControlState = actualRcControlState,
                            settings = state.settings,
                            aspectRatio = screenAspectRatio,
                            settingsSyncGeneration = settingsSyncGeneration,
                            onMove = onRightMove,
                            onSwitchStateChange = onRightSwitchChange,
                            onKnobValueChange = onRightKnobChange,
                            onTopPress = onTopRightPress,
                            onBottomPress = onBottomRightPress,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanelCenterPlotHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    modifier: Modifier = Modifier,
) {
    if (bluetoothViewModel != null) {
        val plotUi by bluetoothViewModel.rcPlotUiState.collectAsState()
        ControlPanelCenterPlot(
            series = plotUi.series,
            plotRevision = plotUi.revision,
            modifier = modifier,
        )
    } else {
        val telemetry by telemetryState.collectAsState()
        val series by remember {
            derivedStateOf { telemetry.plotState.series }
        }
        val plotRevision by remember {
            derivedStateOf { telemetry.plotState.revision }
        }
        ControlPanelCenterPlot(
            series = series,
            plotRevision = plotRevision,
            modifier = modifier,
        )
    }
}

@Composable
private fun ControlPanelCenterPlot(
    series: List<PlotData>,
    plotRevision: Long,
    modifier: Modifier = Modifier,
) {
    CenterDisplay(modifier = modifier, series = series, plotRevision = plotRevision)
}

@Composable
private fun ControlPanelLeftControllerHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    rcControlState: StateFlow<RcControlState>,
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    if (bluetoothViewModel != null) {
        ControlPanelLeftControllerConnected(
            bluetoothViewModel = bluetoothViewModel,
            settings = settings,
            aspectRatio = aspectRatio,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    } else {
        val rcState by rcControlState.collectAsState()
        val telemetry by telemetryState.collectAsState()
        val sideTelemetry by remember {
            derivedStateOf { telemetry.toLeftSideTelemetry() }
        }
        val indicator by remember {
            derivedStateOf {
                SideIndicatorUi(
                    value = telemetry.indicatorState.analogValue,
                    title = telemetry.indicatorState.analogTitle,
                )
            }
        }
        ControlPanelLeftController(
            settings = settings,
            aspectRatio = aspectRatio,
            settingsSyncGeneration = settingsSyncGeneration,
            rcState = rcState,
            sideTelemetry = sideTelemetry,
            indicator = indicator,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun ControlPanelLeftControllerConnected(
    bluetoothViewModel: BluetoothViewModel,
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        aspectRatio = aspectRatio,
        side = ButtonSide.LEFT,
        modifier = Modifier.wrapContentHeight(),
    ) { metrics ->
        ControlPanelLeftTelemetryLayer(
            metrics = metrics,
            bluetoothViewModel = bluetoothViewModel,
        )
        ControlPanelLeftControlsLayer(
            metrics = metrics,
            bluetoothViewModel = bluetoothViewModel,
            mode = settings.leftStickMode,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun ControlPanelRightControllerHost(
    bluetoothViewModel: BluetoothViewModel?,
    telemetryState: StateFlow<TelemetryState>,
    rcControlState: StateFlow<RcControlState>,
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    if (bluetoothViewModel != null) {
        ControlPanelRightControllerConnected(
            bluetoothViewModel = bluetoothViewModel,
            settings = settings,
            aspectRatio = aspectRatio,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    } else {
        val rcState by rcControlState.collectAsState()
        val telemetry by telemetryState.collectAsState()
        val sideTelemetry by remember {
            derivedStateOf { telemetry.toRightSideTelemetry() }
        }
        val indicator by remember {
            derivedStateOf {
                SideIndicatorUi(
                    value = telemetry.indicatorState.batteryLevel,
                    title = telemetry.indicatorState.batteryTitle,
                )
            }
        }
        ControlPanelRightController(
            settings = settings,
            aspectRatio = aspectRatio,
            settingsSyncGeneration = settingsSyncGeneration,
            rcState = rcState,
            sideTelemetry = sideTelemetry,
            indicator = indicator,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun ControlPanelRightControllerConnected(
    bluetoothViewModel: BluetoothViewModel,
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        aspectRatio = aspectRatio,
        side = ButtonSide.RIGHT,
        modifier = Modifier.wrapContentHeight(),
    ) { metrics ->
        ControlPanelRightTelemetryLayer(
            metrics = metrics,
            bluetoothViewModel = bluetoothViewModel,
        )
        ControlPanelRightControlsLayer(
            metrics = metrics,
            bluetoothViewModel = bluetoothViewModel,
            mode = settings.rightStickMode,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            onSwitchStateChange = onSwitchStateChange,
            onKnobValueChange = onKnobValueChange,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ControlPanelLeftTelemetryLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
) {
    val sideTelemetry by bluetoothViewModel.rcLeftSideTelemetry.collectAsState()
    val indicator by bluetoothViewModel.rcLeftIndicator.collectAsState()
    ControllerSideTelemetryRow(
        modifier = Modifier.align(Alignment.TopCenter),
        side = ButtonSide.LEFT,
        telemetry = sideTelemetry,
        panelWidth = metrics.panelWidth,
        extraContentSize = metrics.extraContentSize,
        topExtraContent = { mod ->
            AnalogIndicator(modifier = mod, value = indicator.value, title = indicator.title)
        },
    )
}

@Composable
private fun ControlPanelLeftControlsLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    Box(
        modifier = Modifier.size(metrics.joystickSize),
        contentAlignment = Alignment.BottomEnd,
    ) {
        ControllerSideButtons(
            side = ButtonSide.LEFT,
            joystickSize = metrics.joystickSize,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
        ControlPanelStickSlot(
            side = ButtonSide.LEFT,
            metrics = metrics,
            mode = mode,
            settingsSyncGeneration = settingsSyncGeneration,
            stickPosition = bluetoothViewModel.rcLeftStickPosition,
            onMove = onMove,
        )
        ControlPanelSwitchesSlot(
            switchStates = bluetoothViewModel.rcLeftSwitchStates,
            onSwitchStateChange = onSwitchStateChange,
            metrics = metrics,
        )
        ControlPanelKnobSlot(
            knobValue = bluetoothViewModel.rcLeftKnobValue,
            onKnobValueChange = onKnobValueChange,
            metrics = metrics,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ControlPanelRightTelemetryLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
) {
    val sideTelemetry by bluetoothViewModel.rcRightSideTelemetry.collectAsState()
    val indicator by bluetoothViewModel.rcRightIndicator.collectAsState()
    ControllerSideTelemetryRow(
        modifier = Modifier.align(Alignment.TopCenter),
        side = ButtonSide.RIGHT,
        telemetry = sideTelemetry,
        panelWidth = metrics.panelWidth,
        extraContentSize = metrics.extraContentSize,
        topExtraContent = { mod ->
            BatteryStatus(level = indicator.value, modifier = mod, title = indicator.title)
        },
    )
}

@Composable
private fun ControlPanelRightControlsLayer(
    metrics: ControllerSideLayoutMetrics,
    bluetoothViewModel: BluetoothViewModel,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    Box(
        modifier = Modifier.size(metrics.joystickSize),
        contentAlignment = Alignment.BottomEnd,
    ) {
        ControllerSideButtons(
            side = ButtonSide.RIGHT,
            joystickSize = metrics.joystickSize,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
        ControlPanelStickSlot(
            side = ButtonSide.RIGHT,
            metrics = metrics,
            mode = mode,
            settingsSyncGeneration = settingsSyncGeneration,
            stickPosition = bluetoothViewModel.rcRightStickPosition,
            onMove = onMove,
        )
        ControlPanelSwitchesSlot(
            switchStates = bluetoothViewModel.rcRightSwitchStates,
            onSwitchStateChange = onSwitchStateChange,
            metrics = metrics,
        )
        ControlPanelKnobSlot(
            knobValue = bluetoothViewModel.rcRightKnobValue,
            onKnobValueChange = onKnobValueChange,
            metrics = metrics,
        )
    }
}

@Composable
private fun BoxScope.ControlPanelStickSlot(
    side: ButtonSide,
    metrics: ControllerSideLayoutMetrics,
    mode: JoystickMode,
    settingsSyncGeneration: Int,
    stickPosition: StateFlow<Pair<Float, Float>>,
    onMove: (Float, Float) -> Unit,
) {
    val position by stickPosition.collectAsState()
    val joystickSize = metrics.joystickSize
    ControllerSideJoystick(
        modifier = Modifier
            .offset(
                x = if (side == ButtonSide.RIGHT) (-joystickSize * -0.03f) else (joystickSize * -0.03f),
                y = (-joystickSize * 0.1f),
            )
            .fillMaxSize(),
        mode = mode,
        stickPosition = position,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
    )
}

@Composable
private fun BoxScope.ControlPanelSwitchesSlot(
    switchStates: StateFlow<SwitchStates>,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    metrics: ControllerSideLayoutMetrics,
) {
    val states by switchStates.collectAsState()
    ControllerSideSwitches(
        switchStates = states,
        onSwitchStateChange = onSwitchStateChange,
        switchPositions = metrics.switchPositions,
        switchSize = metrics.switchSize,
    )
}

@Composable
private fun BoxScope.ControlPanelKnobSlot(
    knobValue: StateFlow<Float>,
    onKnobValueChange: (Float) -> Unit,
    metrics: ControllerSideLayoutMetrics,
) {
    val value by knobValue.collectAsState()
    ControllerSideKnob(
        knobSize = metrics.knobSize,
        knobXOffset = metrics.knobXOffset,
        knobYOffset = metrics.knobYOffset,
        joystickSize = metrics.joystickSize,
        knobValue = value,
        onKnobValueChange = onKnobValueChange,
    )
}

@Composable
private fun ControlPanelLeftController(
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    rcState: RcControlState,
    sideTelemetry: SideTelemetry,
    indicator: SideIndicatorUi,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val switchStates = SwitchStates.of(rcState.leftSwitches)
    val topExtraContent: @Composable (Modifier) -> Unit = { mod ->
        AnalogIndicator(modifier = mod, value = indicator.value, title = indicator.title)
    }

    ControllerSide(
        modifier = Modifier.wrapContentHeight(),
        side = ButtonSide.LEFT,
        aspectRatio = aspectRatio,
        mode = settings.leftStickMode,
        stickPosition = rcState.leftStickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
        switchStates = switchStates,
        onSwitchStateChange = onSwitchStateChange,
        knobValue = rcState.leftKnobValue,
        onKnobValueChange = onKnobValueChange,
        telemetry = sideTelemetry,
        topExtraContent = topExtraContent,
        onTopPress = onTopPress,
        onBottomPress = onBottomPress,
    )
}

@Composable
private fun ControlPanelRightController(
    settings: UserSettings,
    aspectRatio: Float,
    settingsSyncGeneration: Int,
    rcState: RcControlState,
    sideTelemetry: SideTelemetry,
    indicator: SideIndicatorUi,
    onMove: (Float, Float) -> Unit,
    onSwitchStateChange: (Int, Boolean) -> Unit,
    onKnobValueChange: (Float) -> Unit,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val switchStates = SwitchStates.of(rcState.rightSwitches)
    val topExtraContent: @Composable (Modifier) -> Unit = { mod ->
        BatteryStatus(level = indicator.value, modifier = mod, title = indicator.title)
    }

    ControllerSide(
        modifier = Modifier.wrapContentHeight(),
        side = ButtonSide.RIGHT,
        aspectRatio = aspectRatio,
        mode = settings.rightStickMode,
        stickPosition = rcState.rightStickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
        switchStates = switchStates,
        onSwitchStateChange = onSwitchStateChange,
        knobValue = rcState.rightKnobValue,
        onKnobValueChange = onKnobValueChange,
        telemetry = sideTelemetry,
        topExtraContent = topExtraContent,
        onTopPress = onTopPress,
        onBottomPress = onBottomPress,
    )
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}

class FakeBluetoothViewModel {
    val telemetryState: StateFlow<TelemetryState> = MutableStateFlow(
        TelemetryState(
            panelState = PanelState(1234, 5678, true, true, 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), "RPM", "RPM"),
            indicatorState = IndicatorState(75, 98, 0x00.toByte(), "Analog", "Battery")
        )
    )
    val userSettings: StateFlow<SettingsUiState> = MutableStateFlow(
        SettingsUiState.Success(
            UserSettings(
                leftStickMode = JoystickMode.HorizontalHold(JoystickMode.LEFT),
                rightStickMode = JoystickMode.Spring(JoystickMode.CENTER),
                leftKnobInitialValue = 0.25f,
                rightKnobInitialValue = 0.75f,
                switchInitialStates = mapOf(0 to false, 1 to true, 2 to false, 3 to true, 4 to false, 5 to true)
            )
        )
    )
    val rcControlState: StateFlow<RcControlState> = MutableStateFlow(
        RcControlState(
            leftStickPosition = Pair(0f, 0f),
            rightStickPosition = Pair(0f, 0f),
            leftSwitches = listOf(false, false, false),
            rightSwitches = listOf(false, false, false),
            leftKnobValue = 0.25f,
            rightKnobValue = 0.75f
        )
    )

//    fun startSendingRcData() {}
//    fun stopSendingRcData() {}
//    fun onLeftStickChanged(x: Float, y: Float) {}
//    fun onRightStickChanged(x: Float, y: Float) {}
//    fun onLeftSwitchChanged(index: Int, newState: Boolean) {}
//    fun onRightSwitchChanged(index: Int, newState: Boolean) {}
//    fun onLeftKnobChanged(newValue: Float) {}
//    fun onRightKnobChanged(newValue: Float) {}
//    fun sendButtonEvent(event: ButtonEvent) {}
}

@Preview(showBackground = true, device = "spec:width=914dp,height=411dp,dpi=420", name = "ControlPanel Landscape Phone")
@Composable
fun ControlPanelScreenPreviewDirect() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=160", name = "ControlPanel Tablet Landscape")
@Composable
fun ControlPanelScreenPreviewTablet() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1024dp,height=600dp,dpi=160", name = "ControlPanel Small Tablet Landscape")
@Composable
fun ControlPanelScreenPreviewSmallTablet() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp,dpi=160", name = "ControlPanel Large Screen Landscape")
@Composable
fun ControlPanelScreenPreviewLargeScreen() {
    TeleCon4Esp32Theme {
        val fakeViewModel = FakeBluetoothViewModel()
        ControlPanelScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}
