package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.example.emitterapp.domain.bluetooth.IndicatorState
import com.example.emitterapp.domain.bluetooth.PanelState
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.domain.model.UserSettings
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.domain.model.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcControlState
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.rc_screen.components_led_style.AnalogIndicatorLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.BatteryStatusLedStyle
import com.example.emitterapp.ui.rc_settings.SettingsUiState
import com.example.emitterapp.ui.theme.EmitterAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun RcScreenLedStyle(
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
    val backCallback = remember {
        { navController?.popBackStack(Screen.Home.route, inclusive = false) }
    }
    DisposableEffect(actualViewModel) {
        actualViewModel?.startSendingRcData()
        onDispose { actualViewModel?.stopSendingRcData() }
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
        is SettingsUiState.Loading -> CircularProgressIndicator()
        is SettingsUiState.Error   -> Text("Error: ${state.message}")
        is SettingsUiState.Success -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B16))   // neon dark background
            ) {
                val screenAspectRatio = maxWidth / maxHeight

                // ── Stable callbacks ──────────────────────────────────────────────
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

                // ── Isolated state slices ─────────────────────────────────────────
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

                // ── LED-style top-extra lambdas ───────────────────────────────────
                val analogValueState  = rememberUpdatedState(collectedTelemetryState.indicatorState.analogValue)
                val analogTitleState  = rememberUpdatedState(collectedTelemetryState.indicatorState.analogTitle)
                val batteryLevelState = rememberUpdatedState(collectedTelemetryState.indicatorState.batteryLevel)
                val batteryTitleState = rememberUpdatedState(collectedTelemetryState.indicatorState.batteryTitle)

                val leftTopContent: @Composable (Modifier) -> Unit = remember {
                    { mod ->
                        AnalogIndicatorLedStyle(
                            modifier = mod,
                            value    = analogValueState.value,
                            title    = analogTitleState.value
                        )
                    }
                }
                val rightTopContent: @Composable (Modifier) -> Unit = remember {
                    { mod ->
                        BatteryStatusLedStyle(
                            level    = batteryLevelState.value,
                            modifier = mod,
                            title    = batteryTitleState.value
                        )
                    }
                }

                // ── Layout ────────────────────────────────────────────────────────
                ErgonomicRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    centerContent = {
                        CenterDisplayLedStyle(
                            modifier    = Modifier.fillMaxSize(),
                            series      = collectedTelemetryState.plotState.series,
                            showAdBanner = false
                        )
                    },
                    leftSideContent = {
                        ControllerSideLedStyle(
                            modifier           = Modifier.wrapContentHeight(),
                            side               = ButtonSide.LEFT,
                            aspectRatio        = screenAspectRatio,
                            mode               = state.settings.leftStickMode,
                            onMove             = onLeftMove,
                            switchStates       = leftSwitches,
                            onSwitchStateChange = onLeftSwitchChange,
                            knobValue          = leftKnobValue,
                            onKnobValueChange  = onLeftKnobChange,
                            panelNumber        = collectedTelemetryState.panelState.leftValue,
                            panelOn            = collectedTelemetryState.panelState.leftOn,
                            panelTitle         = collectedTelemetryState.panelState.leftTitle,
                            topExtraContent    = leftTopContent,
                            ledValues          = collectedTelemetryState.indicatorState.ledValues,
                            onTopPress         = onTopLeftPress,
                            onBottomPress      = onBottomLeftPress,
                        )
                    },
                    rightSideContent = {
                        ControllerSideLedStyle(
                            modifier           = Modifier.wrapContentHeight(),
                            side               = ButtonSide.RIGHT,
                            aspectRatio        = screenAspectRatio,
                            mode               = state.settings.rightStickMode,
                            onMove             = onRightMove,
                            switchStates       = rightSwitches,
                            onSwitchStateChange = onRightSwitchChange,
                            knobValue          = rightKnobValue,
                            onKnobValueChange  = onRightKnobChange,
                            panelNumber        = collectedTelemetryState.panelState.rightValue,
                            panelOn            = collectedTelemetryState.panelState.rightOn,
                            panelTitle         = collectedTelemetryState.panelState.rightTitle,
                            topExtraContent    = rightTopContent,
                            ledValues          = collectedTelemetryState.indicatorState.ledValues,
                            onTopPress         = onTopRightPress,
                            onBottomPress      = onBottomRightPress,
                        )
                    }
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, device = "spec:width=914dp,height=411dp,dpi=420", name = "RcScreenLedStyle Landscape Phone")
@Composable
private fun RcScreenLedStylePreviewPhone() {
    EmitterAppTheme {
        val fake = FakeBluetoothViewModel()
        RcScreenLedStyle(
            telemetryState = fake.telemetryState,
            userSettings   = fake.userSettings,
            rcControlState = fake.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=160", name = "RcScreenLedStyle Tablet Landscape")
@Composable
private fun RcScreenLedStylePreviewTablet() {
    EmitterAppTheme {
        val fake = FakeBluetoothViewModel()
        RcScreenLedStyle(
            telemetryState = fake.telemetryState,
            userSettings   = fake.userSettings,
            rcControlState = fake.rcControlState
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp,dpi=160", name = "RcScreenLedStyle Large Screen")
@Composable
private fun RcScreenLedStylePreviewLargeScreen() {
    EmitterAppTheme {
        val fake = FakeBluetoothViewModel()
        RcScreenLedStyle(
            telemetryState = fake.telemetryState,
            userSettings   = fake.userSettings,
            rcControlState = fake.rcControlState
        )
    }
}
