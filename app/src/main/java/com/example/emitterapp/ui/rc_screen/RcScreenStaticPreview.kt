package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.IndicatorState
import com.example.emitterapp.domain.bluetooth.PanelState
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.domain.bluetooth.TelemetryState
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.theme.EmitterAppTheme
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RcScreenStaticPreview() {
//    val telemetry = TelemetryState(
//        leftPanelValue = 1234,
//        rightPanelValue = 5678,
//        leftPanelOn = true,
//        rightPanelOn = true,
//        leftPanelColor = Color.Green,
//        rightPanelColor = Color.Red
//    )

    val plotData = remember { mutableStateListOf<Float>() }
    val maxDataPoints = 100

//    LaunchedEffect(telemetry) {
//        val normalizedValue = (telemetry.analogIndicatorValue / 100f).coerceIn(0f, 1f)
//        plotData.add(normalizedValue)
//
//        while (plotData.size > maxDataPoints) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
//                plotData.removeFirst()
//            }
//        }
//    }


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
                    onTopLeftPress = {},
                    onTopRightPress = {},
                    onBottomLeftPress = {},
                    onBottomRightPress = {},
                    screenAspectRatio = screenAspectRatio,
                )
            },
            leftSideContent = {
                ControllerSide(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    side = Side.LEFT,
                    aspectRatio = screenAspectRatio,
                    mode = JoystickMode.HorizontalHold(initialPosition = JoystickMode.LEFT),
                    onMove = { x, y -> leftStickPosition = Pair(x, y) },
                    switchStates = leftSwitches,
                    onSwitchStateChange = { index, newState ->
                        leftSwitches = leftSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = lefKnobValue,
                    onKnobValueChange = { newValue -> lefKnobValue = newValue },
                    panelNumber = 100,
                    panelOn = true,
                    panelColor = Color.Red,
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
                    aspectRatio = screenAspectRatio,
                    mode = JoystickMode.Spring(initialPosition = JoystickMode.CENTER),
                    onMove = { x, y -> rightStickPosition = Pair(x, y) },
                    switchStates = rightSwitches,
                    onSwitchStateChange = { index, newState ->
                        rightSwitches = rightSwitches.toMutableList().also { it[index] = newState }
                    },
                    knobValue = rightKnobValue,
                    onKnobValueChange = { newValue -> rightKnobValue = newValue },
                    panelNumber = 100,
                    panelOn = true,
                    panelColor = Color.Green,
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


@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=800dp,height=600dp,dpi=240")
@Preview(device = "spec:width=2340px,height=1080px,dpi=440")
@Preview(device = "spec:width=2520px,height=1080px,dpi=440")
@Preview(device = "spec:width=1920px,height=1080px,dpi=420")
@Preview(device = Devices.AUTOMOTIVE_1024p)
@Preview(device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=2048px,height=1536px,dpi=320")
@Preview(device = "spec:width=891dp,height=411dp", name = "Medium Phone Landscape")
@Preview(showSystemUi = true, device = "spec:width=411dp,height=891dp", name = "Medium Phone")
@Preview(device = "spec:width=891dp,height=411dp,dpi=420", name = "Medium Phone Emu (Correct)")
@Preview(device = "spec:width=914dp,height=411dp,dpi=420", name = "Medium Phone Emu (Accurate)")
@Composable
fun RcScreenPreview() {
    EmitterAppTheme {
        RcScreenStaticPreview()
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Preview(device = "spec:width=914dp,height=411dp,dpi=420", name = "Medium Phone Emu (Accurate)")
@Composable
fun RcScreenPreview2() {
    // Create a static, representative list of data for the preview.
    val previewPlotData = remember {
        val points1 = List(100) { Random.nextFloat() * 0.5f + 0.3f }
        val points2 = List(100) { Random.nextFloat() * 0.4f }
        listOf(
            PlotData(name = "Volts", dataPoints = points1, color = Color.Cyan),
            PlotData(name = "Amps", dataPoints = points2, color = Color.Red)
        )
    }

    EmitterAppTheme {
        // We create a static version of the screen for previewing layout.
        // This is much more reliable than using a live data ViewModel.
        val panelState = PanelState(1234, 5678, true, true)
        val indicatorState = IndicatorState(75, 98)

        // The UI for the static preview
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
                        onTopLeftPress = { },
                        onTopRightPress = { },
                        onBottomLeftPress = { },
                        onBottomRightPress = { },
                        screenAspectRatio = screenAspectRatio,
                        series = previewPlotData
                    )
                },
                leftSideContent = {
                    ControllerSide(
                        modifier = Modifier
                            .wrapContentHeight()
                            .padding(8.dp),
                        side = Side.LEFT,
                        aspectRatio = screenAspectRatio,
                        mode = JoystickMode.HorizontalHold(initialPosition = JoystickMode.LEFT),
                        onMove = { _, _ -> },
                        switchStates = listOf(true, false, true),
                        onSwitchStateChange = { _, _ -> },
                        knobValue = 0.25f,
                        onKnobValueChange = { },
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
                        aspectRatio = screenAspectRatio,
                        mode = JoystickMode.Spring(initialPosition = JoystickMode.CENTER),
                        onMove = { _, _ -> },
                        switchStates = listOf(false, true, false),
                        onSwitchStateChange = { _, _ -> },
                        knobValue = 0.75f,
                        onKnobValueChange = { },
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RcScreenStaticLayout(
    series: List<PlotData> = emptyList(),
    panelState: PanelState,
    indicatorState: IndicatorState
) {
    var leftStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightStickPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var leftSwitches by remember { mutableStateOf(listOf(true, false, true)) }
    var rightSwitches by remember { mutableStateOf(listOf(false, true, false)) }
    var lefKnobValue by remember { mutableStateOf(0.25f) }
    var rightKnobValue by remember { mutableStateOf(0.75f) }

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
                    onTopLeftPress = {},
                    onTopRightPress = {},
                    onBottomLeftPress = {},
                    onBottomRightPress = {},
                    screenAspectRatio = screenAspectRatio,
                    series = series
                )
            },
            leftSideContent = {
                ControllerSide(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    side = Side.LEFT,
                    aspectRatio = screenAspectRatio,
                    mode = JoystickMode.HorizontalHold(initialPosition = JoystickMode.LEFT),
                    onMove = { x, y -> leftStickPosition = Pair(x, y) },
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
                    aspectRatio = screenAspectRatio,
                    mode = JoystickMode.Spring(initialPosition = JoystickMode.CENTER),
                    onMove = { x, y -> rightStickPosition = Pair(x, y) },
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

@Preview(device = Devices.AUTOMOTIVE_1024p, name = "Landscape Tablet")
@Preview(device = "spec:width=914dp,height=411dp,dpi=420", name = "Medium Phone Emu (Accurate)")
@Composable
fun RcScreenCombinedPreview() {
    // Create static, representative data that will be used by all previews.
    val previewPlotData = remember {
        val points1 = List(100) { Random.nextFloat() * 0.5f + 0.3f }
        val points2 = List(100) { Random.nextFloat() * 0.4f }
        listOf(
            PlotData(name = "Volts", dataPoints = points1, color = Color.Cyan),
            PlotData(name = "Amps", dataPoints = points2, color = Color.Red)
        )
    }
    val panelState = PanelState(1234, 5678, true, true, Color.Green, Color.Red)
    val indicatorState = IndicatorState(75, 98)

    EmitterAppTheme {
        // Pass the static data to the layout composable.
        RcScreenStaticLayout(
            series = previewPlotData,
            panelState = panelState,
            indicatorState = indicatorState
        )
    }
}