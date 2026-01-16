package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.ui.bluetooth.BluetoothViewModel
import com.example.emitterapp.ui.bluetooth.ButtonEvent
import com.example.emitterapp.ui.bluetooth.RcUiState
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D_C
import com.example.emitterapp.ui.rc_screen.components.Knob3D
import com.example.emitterapp.ui.rc_screen.components.PushButtonSide
import com.example.emitterapp.ui.rc_screen.components.RealTimePlot
import com.example.emitterapp.ui.rc_screen.components.SevenSegmentedPanel
import com.example.emitterapp.ui.rc_screen.components.Switch3DButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("RestrictedApi")
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

    val state by bluetoothViewModel.state.collectAsState()
    val telemetry = state.telemetryState


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
                onTopLeftPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_LEFT) },
                onTopRightPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_TOP_RIGHT) },
                onBottomLeftPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_LEFT) },
                onBottomRightPress = { bluetoothViewModel.sendButtonEvent(ButtonEvent.CENTER_BOTTOM_RIGHT) }
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
                panelNumber = telemetry.leftPanelValue,
                panelOn = telemetry.leftPanelOn,
                panelColor = telemetry.leftPanelColor,
                topExtraContent = { modifier ->
                    AnalogIndicator(modifier = modifier, value = telemetry.analogIndicatorValue)
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
                panelNumber = telemetry.rightPanelValue,
                panelOn = telemetry.rightPanelOn,
                panelColor = telemetry.rightPanelColor,
                topExtraContent = { modifier ->
                    BatteryStatus(
                        level = telemetry.batteryLevel,
                        modifier = modifier
                    )
                }
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
    screenAspectRatio: Float = 0f,
    plotData: List<Float> = emptyList()
) {
    val isWideScreen = screenAspectRatio > 1.7f
    val is4Over3 = screenAspectRatio == 4 / 3f
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.center_frame_blue),
                    contentDescription = "Center Display Frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxHeight(0.90f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp), // Padding inside the frame
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.15f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box{
                                Text(
                                    text = "Volts",
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(Color.Cyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box{
                                Text(
                                    text = "Amp",
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(Color.Red)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RealTimePlot(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            dataPoints = plotData
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Time (s)",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!isWideScreen) { //&& !is4Over3){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.RIGHT,
                            onPress = onTopRightPress
                        )
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.LEFT,
                            onPress = onTopLeftPress
                        )
                    }
                }
            }

            if (isWideScreen) {  //|| is4Over3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                        .padding(horizontal = 8.dp),
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = if (isWideScreen) (-20).dp else 0.dp)
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
    onKnobValueChange: (Float) -> Unit,
    panelNumber: Int,
    panelOn: Boolean,
    panelColor: Color,
    topExtraContent: (@Composable (modifier: Modifier) -> Unit)? = null
) {

    BoxWithConstraints(
        modifier = modifier.fillMaxHeight(),
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

        val extraContentSizeBattery = joystickSize * 0.35f
        val extraContentSizeAnalogIndicator = joystickSize * 0.3f

        val panelWidth = joystickSize * 0.5f
        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.Center,
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
            } else {
                listOf(20f, 55f, 90f)
            }

            val knobAngle = if (side == Side.RIGHT) 50f else 130f
            val radius = joystickSize * 0.45f
            val knobRadius = joystickSize * 0.55f

            angles.forEachIndexed { index, angle ->
                val angleInRadians = Math.toRadians(angle.toDouble())
                val xOffset = (radius.value * cos(angleInRadians)).dp - 8.dp
                val yOffset = (radius.value * sin(angleInRadians)).dp + 24.dp

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
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (side == Side.RIGHT) {
                SevenSegmentedPanel(
                    width = panelWidth,
                    number = panelNumber,
                    on = panelOn,
                    onColor = panelColor
                )
                Spacer(modifier = Modifier.size(10.dp))
                topExtraContent?.invoke(Modifier.size(extraContentSizeBattery))
            } else {
                topExtraContent?.invoke(Modifier.size(extraContentSizeAnalogIndicator))
                Spacer(modifier = Modifier.size(10.dp))
                SevenSegmentedPanel(
                    width = panelWidth,
                    number = panelNumber,
                    on = panelOn,
                    onColor = panelColor
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


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RcScreenStaticPreview() {
    val telemetry = TelemetryState(
        leftPanelValue = 1234,
        rightPanelValue = 5678,
        leftPanelOn = true,
        rightPanelOn = true,
        leftPanelColor = Color.Green,
        rightPanelColor = Color.Red
    )

    val plotData = remember { mutableStateListOf<Float>() }
    val maxDataPoints = 100

    LaunchedEffect(telemetry) {
        val normalizedValue = (telemetry.analogIndicatorValue / 100f).coerceIn(0f, 1f)
        plotData.add(normalizedValue)

        while (plotData.size > maxDataPoints) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                plotData.removeFirst()
            }
        }
    }


    var leftStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var leftSwitches by remember { mutableStateOf(listOf(true, false, true)) }
    var rightSwitches by remember { mutableStateOf(listOf(false, true, false)) }
    var lefKnobValue by remember { mutableStateOf(0.25f) }
    var rightKnobValue by remember { mutableStateOf(0.75f) }

    var sidePanelAspectRatio by remember { mutableStateOf(4 / 3f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenAspectRatio = maxWidth / maxHeight
        Image(
            painter = painterResource(id = R.drawable.plastic_background), // <-- REPLACE with your background image
            contentDescription = "Background",
            contentScale = ContentScale.Crop, // Or ContentScale.FillBounds
            modifier = Modifier.fillMaxSize()
        )

        ErgonomicRow(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            centerContent = {
                CenterDisplay(
                    modifier = Modifier.fillMaxSize(),
                    onTopLeftPress = {},
                    onTopRightPress = {},
                    onBottomLeftPress = {},
                    onBottomRightPress = {},
                    screenAspectRatio = screenAspectRatio,
                    plotData = plotData
                )
            },
            leftSideContent = {
                ControllerSide(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    side = Side.LEFT,
                    mode = JoystickMode.HorizontalHold(initialPosition = JoystickMode.LEFT),
                    onMove = { x, y -> leftStickPosition = Pair(x, y) },
                    switchStates = leftSwitches,
                    onSwitchStateChange = { index, newState ->
                        leftSwitches = leftSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = lefKnobValue,
                    onKnobValueChange = { newValue -> lefKnobValue = newValue },
                    panelNumber = telemetry.leftPanelValue,
                    panelOn = telemetry.leftPanelOn,
                    panelColor = telemetry.leftPanelColor,
                    topExtraContent = { modifier ->
                        AnalogIndicator(modifier = modifier, value = 50)
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
                    onMove = { x, y -> rightStickPosition = Pair(x, y) },
                    switchStates = rightSwitches,
                    onSwitchStateChange = { index, newState ->
                        rightSwitches = rightSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = rightKnobValue,
                    onKnobValueChange = { newValue -> rightKnobValue = newValue },
                    panelNumber = telemetry.rightPanelValue,
                    panelOn = telemetry.rightPanelOn,
                    panelColor = telemetry.rightPanelColor,
                    topExtraContent = { modifier ->
                        BatteryStatus(
                            level = 100,
                            modifier = modifier
                        )
                    }
                )
            }
        )
    }
}

//@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
//@Preview(device = "spec:width=800dp,height=600dp,dpi=240")
//@Preview(device = "spec:width=2340px,height=1080px,dpi=440")
//@Preview(device = "spec:width=2520px,height=1080px,dpi=440")
//@Preview(device = "spec:width=1920px,height=1080px,dpi=420")
//@Preview(device = Devices.AUTOMOTIVE_1024p)
//@Preview(device = "spec:width=2560px,height=1600px,dpi=320")
//@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
//@Preview(device = "spec:width=2048px,height=1536px,dpi=320")
//@Composable
//fun RcScreenPreview() {
//    RcScreenStaticPreview()
//}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun InteractiveCenterDisplayPreview() {
    val plotData = remember { mutableStateListOf<Float>() }
    val maxDataPoints = 100
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val newPoint = (sin(time * 1 * PI.toFloat()) * 0.25f) + 0.5f
            plotData.add(newPoint)

            if (plotData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    plotData.removeFirst()
                }
            }
            time += 0.02f
            delay(16L)
        }
    }

    CenterDisplay(
        modifier = Modifier.size(width = 800.dp, height = 400.dp),
        onTopLeftPress = {},
        onTopRightPress = {},
        onBottomLeftPress = {},
        onBottomRightPress = {},
        screenAspectRatio = 800f / 400f, // Use a fixed aspect ratio for the preview
        plotData = plotData
    )
}