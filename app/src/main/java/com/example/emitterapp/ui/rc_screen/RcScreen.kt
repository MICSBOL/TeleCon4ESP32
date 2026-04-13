package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.domain.bluetooth.IndicatorState
import com.example.emitterapp.domain.bluetooth.PanelState
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.domain.model.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcControlState
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.ui.rc_settings.SettingsUiState
import com.example.emitterapp.ui.theme.EmitterAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun RcScreen(
    bluetoothViewModel: BluetoothViewModel? = null,
    telemetryState: StateFlow<TelemetryState>? = null,
    userSettings: StateFlow<SettingsUiState>? = null,
    rcControlState: StateFlow<RcControlState>? = null
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

    DisposableEffect(actualViewModel) {
        actualViewModel?.startSendingRcData()

        onDispose {
            actualViewModel?.stopSendingRcData()
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
    }

    val collectedTelemetryState by actualTelemetryState.collectAsState()
    val collectedUserSettings by actualUserSettings.collectAsState()
    val collectedRcControlState by actualRcControlState.collectAsState()

    when (val state = collectedUserSettings) {
        is SettingsUiState.Loading -> {
            CircularProgressIndicator()
        }

        is SettingsUiState.Error -> {
            Text("Error: ${state.message}")
        }

        is SettingsUiState.Success -> {
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

                // ── Isolated state slices ──────────────────────────────────────────────
                // derivedStateOf: when only leftStickPosition changes, leftSwitches/knob
                // values don't change → their derived states don't change → composables
                // reading them are NOT recomposed.
                val leftSwitches by remember {
                    derivedStateOf { SwitchStates.of(collectedRcControlState.leftSwitches) }
                }
                val rightSwitches by remember {
                    derivedStateOf { SwitchStates.of(collectedRcControlState.rightSwitches) }
                }
                val leftKnobValue by remember {
                    derivedStateOf { collectedRcControlState.leftKnobValue }
                }
                val rightKnobValue by remember {
                    derivedStateOf { collectedRcControlState.rightKnobValue }
                }

                // ── Stable topExtraContent lambdas ─────────────────────────────────────
                // rememberUpdatedState gives us a stable State<T> whose .value is always
                // fresh. The lambdas below are created once (remember with no keys) so
                // their references never change. Reading .value inside a @Composable lambda
                // subscribes only that sub-scope to changes — ControllerSide itself stays
                // skippable.
                val analogValueState = rememberUpdatedState(collectedTelemetryState.indicatorState.analogValue)
                val analogTitleState = rememberUpdatedState(collectedTelemetryState.indicatorState.analogTitle)
                val batteryLevelState = rememberUpdatedState(collectedTelemetryState.indicatorState.batteryLevel)
                val batteryTitleState = rememberUpdatedState(collectedTelemetryState.indicatorState.batteryTitle)

                val leftTopContent: @Composable (Modifier) -> Unit = remember {
                    { mod ->
                        AnalogIndicator(
                            modifier = mod,
                            value = analogValueState.value,
                            title = analogTitleState.value
                        )
                    }
                }
                val rightTopContent: @Composable (Modifier) -> Unit = remember {
                    { mod ->
                        BatteryStatus(
                            level = batteryLevelState.value,
                            modifier = mod,
                            title = batteryTitleState.value
                        )
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.plastic_background),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                ErgonomicRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    centerContent = {
                        CenterDisplay(
                            modifier = Modifier.fillMaxSize(),
                            series = collectedTelemetryState.plotState.series
                        )
                    },
                    leftSideContent = {
                        ControllerSide(
                            modifier = Modifier.wrapContentHeight(),
                            side = ButtonSide.LEFT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.leftStickMode,
                            onMove = onLeftMove,
                            switchStates = leftSwitches,
                            onSwitchStateChange = onLeftSwitchChange,
                            knobValue = leftKnobValue,
                            onKnobValueChange = onLeftKnobChange,
                            panelNumber = collectedTelemetryState.panelState.leftValue,
                            panelOn = collectedTelemetryState.panelState.leftOn,
                            panelColor = Color(collectedTelemetryState.panelState.leftColorArgb),
                            panelTitle = collectedTelemetryState.panelState.leftTitle,
                            topExtraContent = leftTopContent,
                            ledValues = collectedTelemetryState.indicatorState.ledValues,
                            onTopPress = onTopLeftPress,
                            onBottomPress = onBottomLeftPress,
                        )
                    },
                    rightSideContent = {
                        ControllerSide(
                            modifier = Modifier.wrapContentHeight(),
                            side = ButtonSide.RIGHT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.rightStickMode,
                            onMove = onRightMove,
                            switchStates = rightSwitches,
                            onSwitchStateChange = onRightSwitchChange,
                            knobValue = rightKnobValue,
                            onKnobValueChange = onRightKnobChange,
                            panelNumber = collectedTelemetryState.panelState.rightValue,
                            panelOn = collectedTelemetryState.panelState.rightOn,
                            panelColor = Color(collectedTelemetryState.panelState.rightColorArgb),
                            panelTitle = collectedTelemetryState.panelState.rightTitle,
                            topExtraContent = rightTopContent,
                            ledValues = collectedTelemetryState.indicatorState.ledValues,
                            onTopPress = onTopRightPress,
                            onBottomPress = onBottomRightPress,
                        )
                    }
                )
            }
        }
    }
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

@Preview(showBackground = true, device = "spec:width=914dp,height=411dp,dpi=420", name = "RcScreen Landscape Phone")
@Composable
fun RcScreenPreviewDirect() {
    EmitterAppTheme {
        val fakeViewModel = FakeBluetoothViewModel()
        RcScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=160", name = "RcScreen Tablet Landscape")
@Composable
fun RcScreenPreviewTablet() {
    EmitterAppTheme {
        val fakeViewModel = FakeBluetoothViewModel()
        RcScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1024dp,height=600dp,dpi=160", name = "RcScreen Small Tablet Landscape")
@Composable
fun RcScreenPreviewSmallTablet() {
    EmitterAppTheme {
        val fakeViewModel = FakeBluetoothViewModel()
        RcScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp,dpi=160", name = "RcScreen Large Screen Landscape")
@Composable
fun RcScreenPreviewLargeScreen() {
    EmitterAppTheme {
        val fakeViewModel = FakeBluetoothViewModel()
        RcScreen(
            telemetryState = fakeViewModel.telemetryState,
            userSettings = fakeViewModel.userSettings,
            rcControlState = fakeViewModel.rcControlState
        )
    }
}
