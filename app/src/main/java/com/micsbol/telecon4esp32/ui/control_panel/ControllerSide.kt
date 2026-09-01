package com.micsbol.telecon4esp32.ui.control_panel

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.micsbol.telecon4esp32.domain.bluetooth.TelemetryState
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.control_panel.components.Joystick_RC3D
import com.micsbol.telecon4esp32.ui.control_panel.components.Knob3D
import com.micsbol.telecon4esp32.ui.control_panel.components.LedIndicator
import com.micsbol.telecon4esp32.ui.control_panel.components.SevenSegmentedPanel
import com.micsbol.telecon4esp32.ui.control_panel.components.Switch3DButton
import com.micsbol.telecon4esp32.ui.control_panel.components.AnalogIndicator
import com.micsbol.telecon4esp32.ui.control_panel.components.BatteryStatus
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
    val ledSize: Dp,
    val ledSpacing: Dp,
)

/**
 * Fraction of content width reserved for one controller side.
 * Wider/shorter phones get a tighter side budget so the center plot stays readable.
 */
internal fun controllerSideWidthFraction(aspectRatio: Float): Float = when {
    aspectRatio >= 2.4f -> 0.27f
    aspectRatio >= 2.0f -> 0.29f
    aspectRatio >= 1.6f -> 0.31f
    else -> 0.33f
}

@Composable
internal fun rememberControllerSideLayoutMetrics(
    side: ButtonSide,
    availableHeight: Dp,
    contentWidth: Dp,
): ControllerSideLayoutMetrics {
    return remember(availableHeight, contentWidth, side) {
        buildControllerSideLayoutMetrics(
            side = side,
            availableHeight = availableHeight,
            contentWidth = contentWidth,
        )
    }
}

/**
 * Builds side-controller sizes from the real available window (dp), so the panel
 * fills short landscape phones and large tablets without overflow or empty gaps.
 *
 * Density is intentionally unused: Compose dp already normalizes pixel density.
 */
internal fun buildControllerSideLayoutMetrics(
    side: ButtonSide,
    availableHeight: Dp,
    contentWidth: Dp,
): ControllerSideLayoutMetrics {
    val safeHeight = availableHeight.coerceAtLeast(80.dp)
    val safeWidth = contentWidth.coerceAtLeast(160.dp)
    val aspectRatio = safeWidth / safeHeight
    val sideBudget = safeWidth * controllerSideWidthFraction(aspectRatio)

    // Column layout: telemetry (~26%) above a weighted controls region (~74%).
    // Leave a clear band above the stick for a larger knob (fills the mid gap).
    val controlsBudget = (safeHeight * 0.74f).coerceAtLeast(72.dp)
    val joystickFromHeight = controlsBudget * 0.70f
    val joystickFromWidth = sideBudget * 0.98f
    val joystickSize = minOf(joystickFromHeight, joystickFromWidth)
        .coerceIn(72.dp, 300.dp)

    val switchMultiplier = 0.24f
    val switchSize = joystickSize * switchMultiplier
    val switchStep = ((joystickSize - switchSize) / 3.5f).coerceAtLeast(0.dp)
    // Raise switches above the gimbal rim (more negative Y = higher), keep sizes.
    val switchPositions = if (side == ButtonSide.RIGHT) {
        listOf(
            Pair(0.dp, -joystickSize * 0.48f),
            Pair(switchStep, -joystickSize * 0.40f),
            Pair(switchStep * 2f, -joystickSize * 0.30f),
        )
    } else {
        listOf(
            Pair(0.dp, -joystickSize * 0.48f),
            Pair(-switchStep, -joystickSize * 0.40f),
            Pair(-switchStep * 2f, -joystickSize * 0.30f),
        )
    }
    // Size the knob from the free band above the stick so it fills that space.
    val knobGap = (controlsBudget - joystickSize).coerceAtLeast(joystickSize * 0.32f)
    val knobSize = minOf(knobGap * 0.95f, joystickSize * 0.50f)
        .coerceIn(40.dp, 150.dp)
    // Unused by layout now (knob is top/center-aligned), kept for metrics stability.
    val angle = if (side == ButtonSide.RIGHT) 72.0 else 108.0
    val radius = joystickSize.value * 0.58f
    val rad = Math.toRadians(angle)
    return ControllerSideLayoutMetrics(
        joystickSize = joystickSize,
        switchSize = switchSize,
        knobSize = knobSize,
        knobXOffset = (radius * cos(rad)).dp,
        knobYOffset = (radius * sin(rad)).dp,
        switchPositions = switchPositions,
        extraContentSize = joystickSize * 0.28f,
        panelWidth = joystickSize * 0.55f,
        ledSize = (joystickSize * 0.08f).coerceIn(8.dp, 14.dp),
        ledSpacing = (joystickSize * 0.04f).coerceIn(3.dp, 8.dp),
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun ControllerSideLayout(
    side: ButtonSide,
    contentWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(ControllerSideLayoutMetrics) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        val metrics = rememberControllerSideLayoutMetrics(
            side = side,
            availableHeight = maxHeight,
            contentWidth = contentWidth,
        )
        // Width follows the stick + telemetry row so the center plot keeps remaining space.
        val sideWidth = max(
            metrics.joystickSize,
            metrics.panelWidth + metrics.extraContentSize + metrics.ledSize + 10.dp,
        )
        Box(
            modifier = Modifier
                .width(sideWidth)
                .fillMaxHeight(),
        ) {
            content(metrics)
        }
    }
}

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
    contentWidth: Dp,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    ControllerSideLayout(
        side = side,
        contentWidth = contentWidth,
        modifier = modifier,
    ) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            ControllerSideTelemetryRow(
                modifier = Modifier.fillMaxWidth(),
                side = side,
                telemetry = telemetry,
                panelWidth = metrics.panelWidth,
                extraContentSize = metrics.extraContentSize,
                ledSize = metrics.ledSize,
                ledSpacing = metrics.ledSpacing,
                topExtraContent = topExtraContent,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
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
            }
        }
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
    // Knob sits just above the stick (same size); bottom padding clears the gimbal rim.
    val knobClearanceAboveStick = 6.dp
    Box(modifier = Modifier.fillMaxSize()) {
        ControllerSideKnob(
            modifier = Modifier
                .align(if (side == ButtonSide.LEFT) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    bottom = joystickSize + knobClearanceAboveStick,
                    start = if (side == ButtonSide.RIGHT) 2.dp else 0.dp,
                    end = if (side == ButtonSide.LEFT) 2.dp else 0.dp,
                ),
            knobSize = metrics.knobSize,
            knobValue = knobValue,
            onKnobValueChange = onKnobValueChange,
        )
        Box(
            modifier = Modifier
                .size(joystickSize)
                .align(Alignment.BottomCenter),
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
                        y = (-joystickSize * 0.1f),
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
        }
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
    onDoubleTap: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    Joystick_RC3D(
        modifier = modifier,
        mode = mode,
        stickPosition = stickPosition,
        settingsSyncGeneration = settingsSyncGeneration,
        onMove = onMove,
        onDoubleTap = onDoubleTap,
        contentDescription = contentDescription,
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
internal fun ControllerSideKnob(
    knobSize: Dp,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(knobSize)) {
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
    ledSize: Dp = 14.dp,
    ledSpacing: Dp = 8.dp,
    topExtraContent: (@Composable (Modifier) -> Unit)?,
    onPanelDoubleTap: (() -> Unit)? = null,
    panelMenu: @Composable () -> Unit = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(ledSpacing)) {
                ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = ledSize) }
            }
            Box {
                SevenSegmentedPanel(
                    width = panelWidth,
                    value = telemetry.panelNumber / 10f,
                    on = telemetry.panelOn,
                    onColor = panelColor,
                    title = telemetry.panelTitle,
                    onDoubleTap = onPanelDoubleTap,
                )
                panelMenu()
            }
            topExtraContent?.invoke(Modifier.size(extraContentSize))
        } else {
            topExtraContent?.invoke(Modifier.size(extraContentSize))
            Box {
                SevenSegmentedPanel(
                    width = panelWidth,
                    value = telemetry.panelNumber / 10f,
                    on = telemetry.panelOn,
                    onColor = panelColor,
                    title = telemetry.panelTitle,
                    onDoubleTap = onPanelDoubleTap,
                )
                panelMenu()
            }
            Column(verticalArrangement = Arrangement.spacedBy(ledSpacing)) {
                ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = ledSize) }
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
        contentWidth = 900.dp,
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
        contentWidth = 900.dp,
        onTopPress = {},
        onBottomPress = {}
    )
}
