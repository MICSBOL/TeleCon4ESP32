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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.bluetooth.ButtonEvent
import com.example.emitterapp.ui.rc_screen.components.AdBanner
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_settings.SettingsUiState

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun RcScreen(
    bluetoothViewModel: BluetoothViewModel?
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    if (bluetoothViewModel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
        return
    }

    DisposableEffect(bluetoothViewModel) {
        bluetoothViewModel.startSendingRcData()

        onDispose {
            bluetoothViewModel.stopSendingRcData()
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

    val telemetryState by bluetoothViewModel.telemetryState.collectAsState()
    val settingsUiState by bluetoothViewModel.userSettings.collectAsState()
    val rcControlState by bluetoothViewModel.rcControlState.collectAsState()

    when (val state = settingsUiState) {
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
                            screenAspectRatio = screenAspectRatio,
                            series = telemetryState.plotState.series
                        )
                    },
                    leftSideContent = {
                        ControllerSide(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(8.dp),
                            side = ButtonSide.LEFT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.leftStickMode,
                            onMove = bluetoothViewModel::onLeftStickChanged,
                            switchStates = rcControlState.leftSwitches,
                            onSwitchStateChange = bluetoothViewModel::onLeftSwitchChanged,
                            knobValue = rcControlState.leftKnobValue,
                            onKnobValueChange = bluetoothViewModel::onLeftKnobChanged,
                            panelNumber = telemetryState.panelState.leftValue,
                            panelOn = telemetryState.panelState.leftOn,
                            panelColor = telemetryState.panelState.leftColor,
                            panelTitle = telemetryState.panelState.leftTitle,
                            topExtraContent = { modifier ->
                                AnalogIndicator(
                                    modifier = modifier,
                                    value = telemetryState.indicatorState.analogValue,
                                    title = telemetryState.indicatorState.analogTitle
                                )
                            },
                            ledValues = telemetryState.indicatorState.ledValues,
                            onTopPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) },
                            onBottomPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) },
                        )
                    },
                    rightSideContent = {
                        ControllerSide(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(8.dp),
                            side = ButtonSide.RIGHT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.rightStickMode,
                            onMove = bluetoothViewModel::onRightStickChanged,
                            switchStates = rcControlState.rightSwitches,
                            onSwitchStateChange = bluetoothViewModel::onRightSwitchChanged,
                            knobValue = rcControlState.rightKnobValue,
                            onKnobValueChange = bluetoothViewModel::onRightKnobChanged,
                            panelNumber = telemetryState.panelState.rightValue,
                            panelOn = telemetryState.panelState.rightOn,
                            panelColor = telemetryState.panelState.rightColor,
                            panelTitle = telemetryState.panelState.rightTitle,
                            topExtraContent = { modifier ->
                                BatteryStatus(
                                    level = telemetryState.indicatorState.batteryLevel,
                                    modifier = modifier,
                                    title =  telemetryState.indicatorState.batteryTitle
                                )
                            },
                            ledValues = telemetryState.indicatorState.ledValues,
                            onTopPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) },
                            onBottomPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) },
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
