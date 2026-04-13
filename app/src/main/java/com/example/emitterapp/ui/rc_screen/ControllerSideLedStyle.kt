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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.domain.model.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.LedIndicator
import com.example.emitterapp.ui.rc_screen.components_led_style.AnalogIndicatorLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.BatteryStatusLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.KnobLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.PushButtonSideLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.SevenSegmentedPanelLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.StickLedStyle
import com.example.emitterapp.ui.rc_screen.components_led_style.SwitchLedStyleButton
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSideLedStyle(
    modifier: Modifier = Modifier,
    side: ButtonSide,
    mode: JoystickMode,
    onMove: (x: Float, y: Float) -> Unit,
    switchStates: SwitchStates,
    onSwitchStateChange: (index: Int, isOn: Boolean) -> Unit,
    knobValue: Float,
    onKnobValueChange: (Float) -> Unit,
    panelNumber: Int,
    panelOn: Boolean,
    panelTitle: String,
    topExtraContent: (@Composable (modifier: Modifier) -> Unit)? = null,
    aspectRatio: Float,
    ledValues: Byte = 0x00,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
) {
    val neonColor = Color(0xFF00E5FF)

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
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = LocalDensity.current

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

        val switch0Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(0, v) } }
        val switch1Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(1, v) } }
        val switch2Callback = remember(onSwitchStateChange) { { v: Boolean -> onSwitchStateChange(2, v) } }

        // ── Joystick + buttons + switches + knob ─────────────────────────────────
        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.BottomEnd,
        ) {
            // Push buttons (top / bottom side buttons)
            ButtonColumnLedStyle(
                modifier = Modifier
                    .size(joystickSize * 0.4f)
                    .align(if (side == ButtonSide.RIGHT) Alignment.BottomStart else Alignment.BottomEnd),
                onTopPress = onTopPress,
                onBottomPress = onBottomPress,
                side = side,
                buttonSize = joystickSize * 0.2f,
            )

            // Joystick
            StickLedStyle(
                modifier = Modifier
                    .offset(
                        x = if (side == ButtonSide.RIGHT) (-joystickSize * -0.03f) else (joystickSize * -0.03f),
                        y = (-joystickSize * 0.1f)
                    )
                    .fillMaxSize(),
                mode = mode,
                onMove = onMove
            )

            // Switches (staircase)
            switchPositions.forEachIndexed { index, (xOffset, yOffset) ->
                Box(
                    modifier = Modifier
                        .size(switchSize)
                        .align(Alignment.TopCenter)
                        .offset(x = xOffset, y = yOffset)
                ) {
                    if (switchStates.size > index) {
                        SwitchLedStyleButton(
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

            // Knob
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .align(Alignment.Center)
                    .offset(x = -knobXOffset, y = -knobYOffset - joystickSize * 0.25f)
            ) {
                KnobLedStyle(
                    modifier = Modifier.fillMaxSize(),
                    value = knobValue,
                    onValueChange = onKnobValueChange
                )
            }
        }

        // ── Top row: LED indicators + panel + extra content ───────────────────────
        Row(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (side == ButtonSide.RIGHT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
                }
                SevenSegmentedPanelLedStyle(
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    neonColor = neonColor,
                    title = panelTitle
                )
                topExtraContent?.invoke(Modifier.size(extraContentSize))
            } else {
                topExtraContent?.invoke(Modifier.size(extraContentSize))
                SevenSegmentedPanelLedStyle(
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    neonColor = neonColor,
                    title = panelTitle
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn -> LedIndicator(isOn = isOn, size = 14.dp) }
                }
            }
        }
    }
}

@Composable
fun ButtonColumnLedStyle(
    modifier: Modifier = Modifier,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
    side: ButtonSide,
    buttonSize: Dp = 50.dp,
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalAlignment = if (side == ButtonSide.RIGHT) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PushButtonSideLedStyle(
            modifier = Modifier.size(buttonSize * 0.8f),
            side = side,
            onPress = onTopPress
        )
        PushButtonSideLedStyle(
            modifier = Modifier.size(buttonSize),
            side = side,
            onPress = onBottomPress
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B16, name = "ControllerSideLedStyle Left")
@Composable
private fun ControllerSideLedStyleLeftPreview() {
    ControllerSideLedStyle(
        side = ButtonSide.LEFT,
        mode = JoystickMode.Spring(),
        onMove = { _, _ -> },
        switchStates = SwitchStates(true, false, true),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.4f,
        onKnobValueChange = {},
        panelNumber = 1234,
        panelOn = true,
        panelTitle = "RPM",
        topExtraContent = { mod ->
            AnalogIndicatorLedStyle(modifier = mod, value = 75, title = "Analog")
        },
        aspectRatio = 2.2f,
        ledValues = 0x0F.toByte(),
        onTopPress = {},
        onBottomPress = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF050B16, name = "ControllerSideLedStyle Right")
@Composable
private fun ControllerSideLedStyleRightPreview() {
    ControllerSideLedStyle(
        side = ButtonSide.RIGHT,
        mode = JoystickMode.Spring(),
        onMove = { _, _ -> },
        switchStates = SwitchStates(false, true, false),
        onSwitchStateChange = { _, _ -> },
        knobValue = 0.7f,
        onKnobValueChange = {},
        panelNumber = 5678,
        panelOn = true,
        panelTitle = "VOLTS",
        topExtraContent = { mod ->
            BatteryStatusLedStyle(modifier = mod, level = 82, title = "Battery")
        },
        aspectRatio = 2.2f,
        ledValues = 0xF0.toByte(),
        onTopPress = {},
        onBottomPress = {}
    )
}

