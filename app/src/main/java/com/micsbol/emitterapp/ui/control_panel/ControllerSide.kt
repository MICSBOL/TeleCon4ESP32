package com.micsbol.emitterapp.ui.control_panel

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.domain.bluetooth.TelemetryState
import com.micsbol.emitterapp.ui.control_panel.components.ButtonSide
import com.micsbol.emitterapp.domain.model.JoystickMode
import com.micsbol.emitterapp.ui.control_panel.components.Joystick_RC3D
import com.micsbol.emitterapp.ui.control_panel.components.Knob3D
import com.micsbol.emitterapp.ui.control_panel.components.LedIndicator
import com.micsbol.emitterapp.ui.control_panel.components.SevenSegmentedPanel
import com.micsbol.emitterapp.ui.control_panel.components.Switch3DButton
import com.micsbol.emitterapp.ui.control_panel.components.AnalogIndicator
import com.micsbol.emitterapp.ui.control_panel.components.BatteryStatus
import kotlin.math.cos
import kotlin.math.sin

/**
 * @Stable tells the Compose compiler that equals() is reliable for this class.
 * Using List<Boolean> as a parameter type is "unstable" — Compose would NEVER
 * skip ControllerSide even if the list content didn't change.  With this
 * @Stable data class, Compose can compare instances with equals() and skip
 * the composable when no switch actually changed.
 */
@Stable
data class SwitchStates(
    val s0: Boolean = false,
    val s1: Boolean = false,
    val s2: Boolean = false,
) {
    operator fun get(index: Int): Boolean = when (index) { 0 -> s0; 1 -> s1; else -> s2 }
    val size: Int get() = 3

    companion object {
        val DEFAULT = SwitchStates()
        fun of(list: List<Boolean>) = SwitchStates(
            list.getOrElse(0) { false },
            list.getOrElse(1) { false },
            list.getOrElse(2) { false },
        )
    }
}

/** Telemetry slice for one controller side — stable for Compose skipping. */
@Stable
data class SideTelemetry(
    val panelNumber: Int,
    val panelOn: Boolean,
    val panelColorArgb: Int,
    val panelTitle: String,
    val ledValues: Byte,
)

/** Analog or battery indicator in the side header row. */
@Stable
data class SideIndicatorUi(
    val value: Int,
    val title: String,
)

internal fun TelemetryState.toLeftSideTelemetry() = SideTelemetry(
    panelNumber = panelState.leftValue,
    panelOn = panelState.leftOn,
    panelColorArgb = panelState.leftColorArgb,
    panelTitle = panelState.leftTitle,
    ledValues = indicatorState.ledValues,
)

internal fun TelemetryState.toRightSideTelemetry() = SideTelemetry(
    panelNumber = panelState.rightValue,
    panelOn = panelState.rightOn,
    panelColorArgb = panelState.rightColorArgb,
    panelTitle = panelState.rightTitle,
    ledValues = indicatorState.ledValues,
)

/** Cached layout metrics for a controller side — stable across RC input updates. */
@Stable
data class ControllerSideLayoutMetrics(
    val joystickSize: Dp,
    val switchSize: Dp,
    val knobSize: Dp,
    val knobXOffset: Dp,
    val knobYOffset: Dp,
    val switchPositions: List<Pair<Dp, Dp>>,
    val extraContentSize: Dp,
    val panelWidth: Dp,
)

@Composable
internal fun rememberControllerSideLayoutMetrics(
    side: ButtonSide,
    aspectRatio: Float,
): ControllerSideLayoutMetrics {
    val density = LocalDensity.current
    return remember(density.density, aspectRatio, side) {
        buildControllerSideLayoutMetrics(side, aspectRatio, density)
    }
}

internal fun buildControllerSideLayoutMetrics(
    side: ButtonSide,
    aspectRatio: Float,
    density: Density,
): ControllerSideLayoutMetrics {
    val mmInDp = density.density * 160f / 25.4f
    val targetMm = if (aspectRatio > 2.0f) 30f else 100f
    val maxDp = if (aspectRatio > 2.0f) 200.dp else 250.dp
    val joystickSize = (targetMm * mmInDp).dp.coerceIn(100.dp, maxDp)
    val switchMultiplier = 0.27f
    val switchSize = joystickSize * switchMultiplier
    val switchStep = ((joystickSize - switchSize) / 3.5f).coerceAtLeast(0.dp)
    val switchPositions = if (side == ButtonSide.RIGHT) {
        listOf(
            Pair(0.dp, -joystickSize * 0.30f),
            Pair(switchStep, -joystickSize * 0.22f),
            Pair(switchStep * 2f, -joystickSize * 0.10f),
        )
    } else {
        listOf(
            Pair(0.dp, -joystickSize * 0.30f),
            Pair(-switchStep, -joystickSize * 0.22f),
            Pair(-switchStep * 2f, -joystickSize * 0.10f),
        )
    }
    val angle = if (side == ButtonSide.RIGHT) 50.0 else 130.0
    val radius = joystickSize.value * 0.45f
    val rad = Math.toRadians(angle)
    return ControllerSideLayoutMetrics(
        joystickSize = joystickSize,
        switchSize = switchSize,
        knobSize = joystickSize * 0.35f,
        knobXOffset = (radius * cos(rad)).dp,
        knobYOffset = (radius * sin(rad)).dp,
        switchPositions = switchPositions,
        extraContentSize = joystickSize * 0.3f,
        panelWidth = joystickSize * 0.6f,
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun ControllerSideLayout(
    aspectRatio: Float,
    side: ButtonSide,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(ControllerSideLayoutMetrics) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight().padding(8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val metrics = rememberControllerSideLayoutMetrics(side, aspectRatio)
        content(metrics)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSide(
    modifier: Modifier = Modifier,
    side: ButtonSide,
    mode: JoystickMode,
    stickPosition: Pair<Float, Float>,
    settingsSyncGeneration: Int = 0,
    onMove: (x: Float, y: Float) -> Unit,
    // @Stable SwitchStates instead of List<Boolean> — allows Compose to skip this composable
    switchStates: SwitchStates,
    onSwitchStateChange: (index: Int, inOn: Boolean) -> Unit,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
    telemetry: SideTelemetry,
    topExtraContent: (@Composable (modifier: Modifier) -> Unit)? = null,
    aspectRatio: Float,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        aspectRatio = aspectRatio,
        side = side,
        modifier = modifier,
    ) { metrics ->
        ControllerSideControls(
            side = side,
            mode = mode,
            stickPosition = stickPosition,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
            switchStates = switchStates,
            onSwitchStateChange = onSwitchStateChange,
            knobValue = knobValue,
            onKnobValueChange = onKnobValueChange,
            metrics = metrics,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
        ControllerSideTelemetryRow(
            modifier = Modifier.align(Alignment.TopCenter),
            side = side,
            telemetry = telemetry,
            panelWidth = metrics.panelWidth,
            extraContentSize = metrics.extraContentSize,
            topExtraContent = topExtraContent,
        )
    }
}

@Composable
internal fun ControllerSideControls(
    side: ButtonSide,
    mode: JoystickMode,
    stickPosition: Pair<Float, Float>,
    settingsSyncGeneration: Int,
    onMove: (x: Float, y: Float) -> Unit,
    switchStates: SwitchStates,
    onSwitchStateChange: (index: Int, inOn: Boolean) -> Unit,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
    metrics: ControllerSideLayoutMetrics,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val joystickSize = metrics.joystickSize
    Box(
        modifier = Modifier.size(joystickSize),
        contentAlignment = Alignment.BottomEnd,
    ) {
        ControllerSideButtons(
            side = side,
            joystickSize = joystickSize,
            onTopPress = onTopPress,
            onBottomPress = onBottomPress,
        )
        ControllerSideJoystick(
            modifier = Modifier
                .offset(
                    x = if (side == ButtonSide.RIGHT) (-joystickSize * -0.03f) else (joystickSize * -0.03f),
                    y = (-joystickSize * 0.1f)
                )
                .fillMaxSize(),
            mode = mode,
            stickPosition = stickPosition,
            settingsSyncGeneration = settingsSyncGeneration,
            onMove = onMove,
        )
        ControllerSideSwitches(
            switchStates = switchStates,
            onSwitchStateChange = onSwitchStateChange,
            switchPositions = metrics.switchPositions,
            switchSize = metrics.switchSize,
        )
        ControllerSideKnob(
            knobSize = metrics.knobSize,
            knobXOffset = metrics.knobXOffset,
            knobYOffset = metrics.knobYOffset,
            joystickSize = joystickSize,
            knobValue = knobValue,
            onKnobValueChange = onKnobValueChange,
        )
    }
}

@Composable
internal fun BoxScope.ControllerSideButtons(
    side: ButtonSide,
    joystickSize: Dp,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ButtonColumn(
        modifier = Modifier
            .size(joystickSize * 0.4f)
            .align(if (side == ButtonSide.RIGHT) Alignment.BottomStart else Alignment.BottomEnd),
        onTopPress = onTopPress,
        onBottomPress = onBottomPress,
        side = side,
        buttonSize = joystickSize * 0.2f,
    )
}

@Composable
internal fun ControllerSideJoystick(
    mode: JoystickMode,
    stickPosition: Pair<Float, Float>,
    settingsSyncGeneration: Int,
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Joystick_RC3D(
        modifier = modifier,
        mode = mode,
        stickPosition = stickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
    )
}

@Composable
internal fun BoxScope.ControllerSideSwitches(
    switchStates: SwitchStates,
    onSwitchStateChange: (index: Int, inOn: Boolean) -> Unit,
    switchPositions: List<Pair<Dp, Dp>>,
    switchSize: Dp,
) {
    val switch0Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(0, v) } }
    val switch1Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(1, v) } }
    val switch2Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(2, v) } }

    switchPositions.forEachIndexed { index, (xOffset, yOffset) ->
        Box(
            modifier = Modifier
                .size(switchSize)
                .align(Alignment.TopCenter)
                .offset(x = xOffset, y = yOffset)
        ) {
            if (switchStates.size > index) {
                Switch3DButton(
                    isOn = switchStates[index],
                    onStateChange = when (index) {
                        0 -> switch0Callback
                        1 -> switch1Callback
                        else -> switch2Callback
                    }
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.ControllerSideKnob(
    knobSize: Dp,
    knobXOffset: Dp,
    knobYOffset: Dp,
    joystickSize: Dp,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(knobSize)
            .align(Alignment.Center)
            .offset(x = -knobXOffset, y = -knobYOffset - joystickSize * 0.25f)
    ) {
        Knob3D(value = knobValue, onValueChange = onKnobValueChange)
    }
}

@Composable
internal fun ControllerSideTelemetryRow(
    modifier: Modifier,
    side: ButtonSide,
    telemetry: SideTelemetry,
    panelWidth: Dp,
    extraContentSize: Dp,
    topExtraContent: (@Composable (Modifier) -> Unit)?,
) {
    val ledStates = remember(telemetry.ledValues, side) {
        if (side == ButtonSide.LEFT) {
            listOf(
                (telemetry.ledValues.toInt() and 0b00000001) != 0,
                (telemetry.ledValues.toInt() and 0b00000010) != 0,
                (telemetry.ledValues.toInt() and 0b00000100) != 0,
                (telemetry.ledValues.toInt() and 0b00001000) != 0
            )
        } else {
            listOf(
                (telemetry.ledValues.toInt() and 0b00010000) != 0,
                (telemetry.ledValues.toInt() and 0b00100000) != 0,
                (telemetry.ledValues.toInt() and 0b01000000) != 0,
                (telemetry.ledValues.toInt() and 0b10000000) != 0
            )
        }
    }
    val panelColor = Color(telemetry.panelColorArgb)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (side == ButtonSide.RIGHT) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
            }
            SevenSegmentedPanel(
                width = panelWidth,
                value = telemetry.panelNumber / 10f,
                on = telemetry.panelOn,
                onColor = panelColor,
                title = telemetry.panelTitle
            )
            topExtraContent?.invoke(Modifier.size(extraContentSize))
        } else {
            topExtraContent?.invoke(Modifier.size(extraContentSize))
            SevenSegmentedPanel(
                width = panelWidth,
                value = telemetry.panelNumber / 10f,
                on = telemetry.panelOn,
                onColor = panelColor,
                title = telemetry.panelTitle
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
            }
        }
    }
}

@Preview(showBackground = true, name = "ControllerSide Left")
@Composable
fun ControllerSideLeftPreview() {
    ControllerSide(
        side = ButtonSide.LEFT,
        mode = JoystickMode.Spring(),
        stickPosition = Pair(0f, 0f),
        onMove = { _, _ -> },
        switchStates = SwitchStates(false, false, false),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.5f,
        onKnobValueChange = {},
        telemetry = SideTelemetry(
            panelNumber = 1234,
            panelOn = true,
            panelColorArgb = Color.Red.toArgb(),
            panelTitle = "RPM",
            ledValues = 0x0F.toByte(),
        ),
        topExtraContent = { modifier ->
            AnalogIndicator(modifier = modifier, value = 75, title = "Analog")
        },
        aspectRatio = 2.2f,
        onTopPress = {},
        onBottomPress = {}
    )
}

@Preview(showBackground = true, name = "ControllerSide Right")
@Composable
fun ControllerSideRightPreview() {
    ControllerSide(
        side = ButtonSide.RIGHT,
        mode = JoystickMode.Spring(),
        stickPosition = Pair(0f, 0f),
        onMove = { _, _ -> },
        switchStates = SwitchStates(false, false, false),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.7f,
        onKnobValueChange = {},
        telemetry = SideTelemetry(
            panelNumber = 5678,
            panelOn = true,
            panelColorArgb = Color.Green.toArgb(),
            panelTitle = "RPM",
            ledValues = 0xF0.toByte(),
        ),
        topExtraContent = { modifier ->
            BatteryStatus(level = 98, modifier = modifier, title = "Battery")
        },
        aspectRatio = 2.2f,
        onTopPress = {},
        onBottomPress = {}
    )
}
