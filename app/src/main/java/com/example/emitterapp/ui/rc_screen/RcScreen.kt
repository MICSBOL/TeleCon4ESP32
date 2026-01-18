package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.bluetooth.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcUiState
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@SuppressLint("RestrictedApi", "UnusedBoxWithConstraintsScope")
@Composable
fun RcScreen(
    bluetoothViewModel: BluetoothViewModel?
) {
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


    var leftStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }

    var leftSwitches by remember { mutableStateOf(listOf(false, false, false)) }
    var rightSwitches by remember { mutableStateOf(listOf(false, false, false)) }

    var lefKnobValue by remember { mutableStateOf(0.5f) }
    var rightKnobValue by remember { mutableStateOf(0.5f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val currentState = RcUiState(
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
                leftKnobValue = (lefKnobValue * 1023).toInt().coerceIn(0, 1023),
                rightKnobValue = (rightKnobValue * 1023).toInt().coerceIn(0, 1023)
            )

            bluetoothViewModel.sendRcControlData(currentState)
//            delay(50L)
            delay(1000L)
        }
    }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    mode = JoystickMode.HorizontalHold(initialPosition = JoystickMode.LEFT),
                    onMove = { x, y ->
                        leftStickPosition = Pair(x, y)
                    },
                    switchStates = leftSwitches,
                    onSwitchStateChange = { index, newState ->
                        leftSwitches = leftSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = lefKnobValue,
                    onKnobValueChange = { newValue -> lefKnobValue = newValue },
                    panelNumber = panelState.leftValue,
                    panelOn = panelState.leftOn,
                    panelColor = panelState.leftColor,
                    topExtraContent = { modifier ->
                        AnalogIndicator(modifier = modifier, value = indicatorState.analogValue)
                    }
                )
            },
            rightSideContent = {
                ControllerSide(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    side = Side.RIGHT,
                    mode = JoystickMode.Spring(initialPosition = JoystickMode.CENTER),
                    onMove = { x, y ->
                        rightStickPosition = Pair(x, y)
                    },
                    switchStates = rightSwitches,
                    onSwitchStateChange = { index, newState ->
                        rightSwitches = rightSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = rightKnobValue,
                    onKnobValueChange = { newValue -> rightKnobValue = newValue },
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

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        if (activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
    }
}

enum class Side {
    LEFT, RIGHT
}
