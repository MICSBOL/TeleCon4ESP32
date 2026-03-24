package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D
import com.example.emitterapp.ui.rc_screen.components.Knob3D
import com.example.emitterapp.ui.rc_screen.components.LedIndicator
import com.example.emitterapp.ui.rc_screen.components.SevenSegmentedPanel
import com.example.emitterapp.ui.rc_screen.components.Switch3DButton
import com.example.emitterapp.ui.rc_screen.components.AnalogIndicator
import com.example.emitterapp.ui.rc_screen.components.BatteryStatus
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSide(
    modifier: Modifier = Modifier,
    side: ButtonSide,
    mode: JoystickMode,
    onMove: (x: Float, y: Float) -> Unit,
    // @Stable SwitchStates instead of List<Boolean> — allows Compose to skip this composable
    switchStates: SwitchStates,
    onSwitchStateChange: (index: Int, inOn: Boolean) -> Unit,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
    panelNumber: Int,
    panelOn: Boolean,
    panelColor: Color,
    panelTitle: String,
    topExtraContent: (@Composable (modifier: Modifier) -> Unit)? = null,
    aspectRatio: Float,
    ledValues: Byte = 0x00,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val ledStates = remember(ledValues, side) {
        if (side == ButtonSide.LEFT) {
            listOf(
                (ledValues.toInt() and 0b00000001) != 0,
                (ledValues.toInt() and 0b00000010) != 0,
                (ledValues.toInt() and 0b00000100) != 0,
                (ledValues.toInt() and 0b00001000) != 0
            )
        } else {
            listOf(
                (ledValues.toInt() and 0b00010000) != 0,
                (ledValues.toInt() and 0b00100000) != 0,
                (ledValues.toInt() and 0b01000000) != 0,
                (ledValues.toInt() and 0b10000000) != 0
            )
        }
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight().padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = LocalDensity.current
        // Cache size calculations — only recompute when screen density or aspect ratio changes.
        val joystickSize: Dp = remember(density.density, aspectRatio) {
            val mmInDp = density.density * 160f / 25.4f
            val targetMm = if (aspectRatio > 2.0f) 30f else 100f
            val maxSize = if (aspectRatio > 2.0f) 200.dp else 250.dp
            (targetMm * mmInDp).dp.coerceIn(100.dp, maxSize)
        }
        val switchMultiplier = 0.27f
        val knobSize = joystickSize * 0.35f
        val switchSize = joystickSize * switchMultiplier
        val switchStep: Dp = remember(joystickSize, switchSize) {
            ((joystickSize - switchSize) / 3.5f).coerceAtLeast(0.dp)
        }
        val extraContentSize = joystickSize * 0.3f
        val panelWidth = joystickSize * 0.6f

        // Cache the staircase positions — only recompute on layout changes, not on every drag.
        val switchPositions = remember(side, switchStep, joystickSize) {
            if (side == ButtonSide.RIGHT) {
                listOf(
                    Pair(0.dp,              -joystickSize * 0.30f),
                    Pair(switchStep,        -joystickSize * 0.22f),
                    Pair(switchStep * 2f,   -joystickSize * 0.10f),
                )
            } else {
                listOf(
                    Pair(0.dp,              -joystickSize * 0.30f),
                    Pair(-switchStep,       -joystickSize * 0.22f),
                    Pair(-switchStep * 2f,  -joystickSize * 0.10f),
                )
            }
        }

        // Cache trig — these only depend on side + joystickSize, never on user input.
        val knobXOffset: Dp
        val knobYOffset: Dp
        remember(side, joystickSize) {
            val angle = if (side == ButtonSide.RIGHT) 50.0 else 130.0
            val radius = joystickSize.value * 0.45f
            val rad = Math.toRadians(angle)
            Pair((radius * cos(rad)).dp, (radius * sin(rad)).dp)
        }.also { (x, y) ->
            knobXOffset = x
            knobYOffset = y
        }

        // Stable per-switch callbacks — same references across recompositions as long as
        // onSwitchStateChange doesn't change (it won't after our RcScreen fix).
        val switch0Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(0, v) } }
        val switch1Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(1, v) } }
        val switch2Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(2, v) } }

        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.BottomEnd,
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
            Joystick_RC3D(
                modifier = Modifier
                    .offset(
                        x = if (side == ButtonSide.RIGHT) (-joystickSize * -0.03f) else (joystickSize * -0.03f),
                        y = (-joystickSize * 0.1f)
                    )
                    .fillMaxSize(),
                mode = mode,
                onMove = onMove
            )

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

            Box(
                modifier = Modifier
                    .size(knobSize)
                    .align(Alignment.Center)
                    .offset(x = -knobXOffset, y = -knobYOffset - joystickSize * 0.25f)
            ) {
                Knob3D(value = knobValue, onValueChange = onKnobValueChange)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (side == ButtonSide.RIGHT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
                }
                SevenSegmentedPanel(
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    onColor = panelColor,
                    title = panelTitle
                )
                topExtraContent?.invoke(Modifier.size(extraContentSize))
            } else {
                topExtraContent?.invoke(Modifier.size(extraContentSize))
                SevenSegmentedPanel(
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    onColor = panelColor,
                    title = panelTitle
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
                }
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
        onMove = { _, _ -> },
        switchStates = SwitchStates(false, false, false),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.5f,
        onKnobValueChange = {},
        panelNumber = 1234,
        panelOn = true,
        panelColor = Color.Red,
        panelTitle = "RPM",
        topExtraContent = { modifier ->
            AnalogIndicator(modifier = modifier, value = 75, title = "Analog")
        },
        aspectRatio = 2.2f,
        ledValues = 0x0F.toByte(),
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
        onMove = { _, _ -> },
        switchStates = SwitchStates(false, false, false),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.7f,
        onKnobValueChange = {},
        panelNumber = 5678,
        panelOn = true,
        panelColor = Color.Green,
        panelTitle = "RPM",
        topExtraContent = { modifier ->
            BatteryStatus(level = 98, modifier = modifier, title = "Battery")
        },
        aspectRatio = 2.2f,
        ledValues = 0xF0.toByte(),
        onTopPress = {},
        onBottomPress = {}
    )
}
