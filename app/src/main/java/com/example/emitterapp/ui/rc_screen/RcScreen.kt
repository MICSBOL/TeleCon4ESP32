package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.emitterapp.R
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.bluetooth.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcUiState
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D_C
import com.example.emitterapp.ui.rc_screen.components.Knob3D
import com.example.emitterapp.ui.rc_screen.components.PushButtonSide
import com.example.emitterapp.ui.rc_screen.components.Switch3DButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("RestrictedApi")
@Composable
fun RcScreen(
    bluetoothViewModel: BluetoothViewModel?
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
    }

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

            bluetoothViewModel?.sendRcControlData(currentState)
//            delay(50L)
            delay(1000L)
        }
    }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    ErgonomicRow(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        centerContent = {
            CenterDisplay(
                modifier = Modifier.fillMaxSize(),
                // The Top-Left button in the UI should send the TOP_LEFT event.
                onTopLeftPress = { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) },
                onTopRightPress = { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) },

                // The Bottom-Left button in the UI should send the BOTTOM_LEFT event.
                onBottomLeftPress = { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) },
                onBottomRightPress = { bluetoothViewModel?.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) }
            )
        },
        leftSideContent = {
            ControllerSide(
                modifier = Modifier
                    .fillMaxHeight()
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
                onKnobValueChange = { newValue -> lefKnobValue = newValue }
            )
        },
        rightSideContent = {
            ControllerSide(
                modifier = Modifier
                    .fillMaxHeight()
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
                onKnobValueChange = { newValue -> rightKnobValue = newValue }
            )
        }
    )
}

@Composable
fun CenterDisplay(
    modifier: Modifier = Modifier,
    onTopLeftPress: () -> Unit,
    onTopRightPress: () -> Unit,
    onBottomLeftPress: () -> Unit,
    onBottomRightPress: () -> Unit,
) {
    Box(
        modifier = modifier
//            .padding(vertical = 16.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.car_bouncing01),
                contentDescription = "Center Screen",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PushButtonSide(
                    modifier = Modifier.size(50.dp),
                    side = ButtonSide.RIGHT,
                    onPress = onTopRightPress
                )
                PushButtonSide(
                    modifier = Modifier.size(50.dp),
                    side = ButtonSide.LEFT,
                    onPress = onTopLeftPress
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.RIGHT,
                    onPress = onBottomRightPress
                )
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.LEFT,
                    onPress = onBottomLeftPress
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSide(
    modifier: Modifier = Modifier,
    side: Side,
    mode: JoystickMode,
    onMove: (x: Float, y: Float) -> Unit,
    switchStates: List<Boolean>,
    onSwitchStateChange: (index: Int, inOn: Boolean) -> Unit,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
//            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val baseSize = min(maxWidth, maxHeight)
        val aspectRatio = maxWidth / maxHeight
        val sizePercentage = when {
            aspectRatio > 2.0f -> 0.7f
            aspectRatio > 1.7f -> 0.8f
            aspectRatio > 1.4f -> 0.6f
            else -> 0.3f
        }
        val joystickSize = baseSize * sizePercentage
        val knobSize = joystickSize * 0.4f
        val switchSize = joystickSize * 0.4f
        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.Center
        ) {
            Joystick_RC3D_C(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                mode = mode,
                onMove = onMove
            )
            val angles = if (side == Side.RIGHT) {
                listOf(90f, 125f, 160f)
//                listOf(92f, 117f, 142f)
            } else {
                listOf(20f, 55f, 90f)
//                listOf(42f, 67f, 92f)
            }

            val knobAngle = if (side == Side.RIGHT) 50f else 130f
            val radius = joystickSize * 0.45f
            val knobRadius = joystickSize * 0.55f

            angles.forEachIndexed { index, angle ->
                val angleInRadians = Math.toRadians(angle.toDouble())
                val xOffset = (radius.value * cos(angleInRadians)).dp
                val yOffset = (radius.value * sin(angleInRadians)).dp

                Box(
                    modifier = Modifier
                        .size(switchSize)
                        .align(Alignment.Center)
                        .offset(x = -xOffset, y = -yOffset)
                ) {
                    if (switchStates.size > index) {
                        Switch3DButton(
                            isOn = switchStates[index],
                            onStateChange = { newState ->
                                onSwitchStateChange(index, newState)
                            }
                        )
                    }
                }
            }

            val knobAngleRadians = Math.toRadians(knobAngle.toDouble())
            val knobXOffset = (knobRadius.value * cos(knobAngleRadians)).dp
            val knobYOffset = (knobRadius.value * sin(knobAngleRadians)).dp
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .align(Alignment.Center)
                    .offset(x = -knobXOffset, y = -knobYOffset - 10.dp)
            ) {
                Knob3D(
                    value = knobValue,
                    onValueChange = onKnobValueChange
                )
            }
        }
    }
}

@Composable
fun ErgonomicRow(
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit,
    leftSideContent: @Composable () -> Unit,
    rightSideContent: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->

        val sideMaxWidth = (constraints.maxWidth * 0.7f).toInt()
//        val sideMaxWidth = (constraints.maxWidth * 0.35f).toInt()
        val sideConstraints = constraints.copy(minWidth = 0, maxWidth = sideMaxWidth)
        val leftPlaceable =
            subcompose("left") { leftSideContent() }.first().measure(sideConstraints)
        val rightPlaceable =
            subcompose("right") { rightSideContent() }.first().measure(sideConstraints)
        val centerWidth = constraints.maxWidth - leftPlaceable.width - rightPlaceable.width
        val coercedCenterWidth = centerWidth.coerceAtLeast(0)
        val centerPlaceable = subcompose("center") { centerContent() }
            .first()
            .measure(
                constraints.copy(
                    minWidth = coercedCenterWidth,
                    maxWidth = coercedCenterWidth
                )
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            leftPlaceable.placeRelative(0, 0)
            centerPlaceable.placeRelative(leftPlaceable.width, 0)
            rightPlaceable.placeRelative(leftPlaceable.width + centerPlaceable.width, 0)
        }
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


@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=800dp,height=600dp,dpi=240")
@Preview(device = "spec:width=2340px,height=1080px,dpi=440")
@Preview(device = "spec:width=2520px,height=1080px,dpi=440")
@Preview(device = "spec:width=1920px,height=1080px,dpi=420")
@Preview(device = Devices.AUTOMOTIVE_1024p)
@Preview(device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=2048px,height=1536px,dpi=320")
@Composable
fun RcScreenPreview() {
    RcScreen(null)
}