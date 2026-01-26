package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.emitterapp.ui.bluetooth.RcUiState
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_settings.SettingsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
    }

    val panelState by bluetoothViewModel.panelState.collectAsState()
    val indicatorState by bluetoothViewModel.indicatorState.collectAsState()
    val plotState by bluetoothViewModel.plotState.collectAsState()
    val settingsUiState by bluetoothViewModel.settingsState.collectAsState()
    val rcControlState by bluetoothViewModel.rcControlState.collectAsState()

//    var leftStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
//    var rightStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }

//    var leftSwitches by remember { mutableStateOf(listOf(false, false, false)) }
//    var rightSwitches by remember { mutableStateOf(listOf(false, false, false)) }
//
//    var leftKnobValue by remember { mutableStateOf(0.5f) }
//    var rightKnobValue by remember { mutableStateOf(0.5f) }
//    LaunchedEffect(settingsUiState) {
//        if (settingsUiState is SettingsUiState.Success) {
//            val loadedSettings = (settingsUiState as SettingsUiState.Success).settings
//            fun toNormalized(pos: Pair<Int, Int>): Pair<Float, Float> {
//                val x = (pos.first - 6) / 6f
//                val y = (pos.second - 6) / -6f
//                return Pair(x, y)
//            }
//            leftStickPosition =
//                toNormalized((settingsUiState as SettingsUiState.Success).settings.leftStickMode.initialPosition)
//            rightStickPosition =
//                toNormalized((settingsUiState as SettingsUiState.Success).settings.rightStickMode.initialPosition)
//
//            leftKnobValue = loadedSettings.leftKnobInitialValue
//            rightKnobValue = loadedSettings.rightKnobInitialValue
//            val switchMap = loadedSettings.switchInitialStates
//
//            leftSwitches = listOf(
//                switchMap[0] ?: false,
//                switchMap[1] ?: false,
//                switchMap[2] ?: false
//            )
//            rightSwitches = listOf(
//                switchMap[3] ?: false,
//                switchMap[4] ?: false,
//                switchMap[5] ?: false
//            )
//        }
//    }
//    LaunchedEffect(Unit) {
//        while (isActive) {
//            val currentState = RcUiState(
//                leftStickX = (leftStickPosition.first * 100).toInt(),
//                leftStickY = (leftStickPosition.second * 100).toInt(),
//                rightStickX = (rightStickPosition.first * 100).toInt(),
//                rightStickY = (rightStickPosition.second * 100).toInt(),
//                switch1 = leftSwitches[0],
//                switch2 = leftSwitches[1],
//                switch3 = leftSwitches[2],
//                switch4 = rightSwitches[0],
//                switch5 = rightSwitches[1],
//                switch6 = rightSwitches[2],
//                leftKnobValue = (leftKnobValue * 1023).toInt().coerceIn(0, 1023),
//                rightKnobValue = (rightKnobValue * 1023).toInt().coerceIn(0, 1023)
//            )
//
//            bluetoothViewModel.sendRcControlData(currentState)
////            delay(50L)
//            delay(1000L)
//        }
//    }


    when (val state = settingsUiState) {
        is SettingsUiState.Loading -> {
            // Show a loading indicator in the center while settings are loading
            CircularProgressIndicator()
        }

        is SettingsUiState.Error -> {
            // Show an error message
            Text("Error: ${state.message}")
        }

        is SettingsUiState.Success -> {
            // Once settings are loaded successfully, draw the main UI
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
                            onTopLeftPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) },
                            onTopRightPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) },
                            onBottomLeftPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) },
                            onBottomRightPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) },
                            screenAspectRatio = screenAspectRatio,
                            series = plotState.series
                        )
                    },
                    leftSideContent = {
                        ControllerSide(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(8.dp),
                            side = Side.LEFT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.leftStickMode,
                            onMove = bluetoothViewModel::onLeftStickChanged,
                            switchStates = rcControlState.leftSwitches,
                            onSwitchStateChange = bluetoothViewModel::onLeftSwitchChanged,
                            knobValue = rcControlState.leftKnobValue,
                            onKnobValueChange = bluetoothViewModel::onLeftKnobChanged,
                            panelNumber = panelState.leftValue,
                            panelOn = panelState.leftOn,
                            panelColor = panelState.leftColor,
                            topExtraContent = { modifier ->
                                AnalogIndicator(
                                    modifier = modifier,
                                    value = indicatorState.analogValue
                                )
                            }
                        )
                    },
                    rightSideContent = {
                        ControllerSide(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(8.dp),
                            side = Side.RIGHT,
                            aspectRatio = screenAspectRatio,
                            mode = state.settings.rightStickMode,
                            onMove = bluetoothViewModel::onRightStickChanged,
                            switchStates = rcControlState.rightSwitches,
                            onSwitchStateChange = bluetoothViewModel::onRightSwitchChanged,
                            knobValue = rcControlState.rightKnobValue,
                            onKnobValueChange = bluetoothViewModel::onRightKnobChanged,
                            panelNumber = panelState.rightValue,
                            panelOn = panelState.rightOn,
                            panelColor = panelState.rightColor,
                            topExtraContent = { modifier ->
                                BatteryStatus(
                                    level = indicatorState.batteryLevel,
                                    modifier = modifier
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}


// --- START OF NEW COMPOSABLE ---
@Composable
fun KnobSettingsSliders(
    leftValue: Float,
    rightValue: Float,
    onLeftChange: (Float) -> Unit,
    onRightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Slider for the Left Knob
        Text("Left Knob", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = leftValue,
            onValueChange = onLeftChange,
            valueRange = 0f..1f, // Standard range for a normalized value
            steps = 9 // This creates 10 steps (0.0, 0.1, 0.2, ...) for finer control
        )

        // Spacer between the two sliders
        Spacer(modifier = Modifier.height(8.dp))

        // Slider for the Right Knob
        Text("Right Knob", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = rightValue,
            onValueChange = onRightChange,
            valueRange = 0f..1f,
            steps = 9
        )
    }
}
// --- END OF NEW COMPOSABLE ---

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // This block is called when RcScreen leaves the composition
            activity.requestedOrientation = originalOrientation
        }
    }
}

enum class Side {
    LEFT, RIGHT
}
