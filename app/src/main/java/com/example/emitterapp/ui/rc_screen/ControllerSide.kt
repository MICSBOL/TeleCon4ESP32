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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D
import com.example.emitterapp.ui.rc_screen.components.Knob3D
import com.example.emitterapp.ui.rc_screen.components.LedIndicator
import com.example.emitterapp.ui.rc_screen.components.SevenSegmentedPanel
import com.example.emitterapp.ui.rc_screen.components.Switch3DButton
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSide(
    modifier: Modifier = Modifier,
    side: ButtonSide,
    mode: JoystickMode,
    onMove: (x: Float, y: Float) -> Unit,
    switchStates: List<Boolean>,
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
    val ledStates = remember(ledValues) {
        if (side == ButtonSide.LEFT) {
            // Use the first 4 bits (0, 1, 2, 3) for the Left side
            listOf(
                (ledValues.toInt() and 0b00000001) != 0, // Check bit 0
                (ledValues.toInt() and 0b00000010) != 0, // Check bit 1
                (ledValues.toInt() and 0b00000100) != 0, // Check bit 2
                (ledValues.toInt() and 0b00001000) != 0  // Check bit 3
            )
        } else { // Side.RIGHT
            // Use the next 4 bits (4, 5, 6, 7) for the Right side
            listOf(
                (ledValues.toInt() and 0b00010000) != 0, // Check bit 4
                (ledValues.toInt() and 0b00100000) != 0, // Check bit 5
                (ledValues.toInt() and 0b01000000) != 0, // Check bit 6
                (ledValues.toInt() and 0b10000000) != 0  // Check bit 7
            )
        }
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val baseSize = min(maxWidth, maxHeight)
        var joystickSize = 0.dp
        when {

            aspectRatio > 2.1f -> {
                joystickSize = maxHeight * 0.6f
            }

            aspectRatio > 2.0f -> {
                joystickSize = maxHeight * 0.6f
            }

            aspectRatio > 1.7f -> {
                joystickSize = maxHeight * 0.6f
            }

            aspectRatio > 1.4f -> {
                joystickSize = maxHeight * 0.6f
            }

            else -> {
                joystickSize = baseSize * 0.3f
            }
        }
        val knobSize = joystickSize * 0.4f
        val switchSize = joystickSize * 0.4f

        val extraContentSizeBattery = joystickSize * 0.3f
        val extraContentSizeAnalogIndicator = joystickSize * 0.3f

        val panelWidth = joystickSize * 0.6f
        Box(
            modifier = Modifier
                .size(joystickSize),
            contentAlignment = Alignment.Center,
        ) {
            ButtonColumn(
                modifier = Modifier
//                    .size(joystickSize * 0.5f)
                    .align(
                        if (side == ButtonSide.RIGHT) Alignment.BottomStart else Alignment.BottomEnd
                    ),
                onTopPress = onTopPress,
                onBottomPress = onBottomPress,
                side = side,
                isWideScreen = false,
                buttonSize = joystickSize * 0.2f,
            )
            Joystick_RC3D(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                mode = mode,
                onMove = onMove
            )
            val angles = if (side == ButtonSide.RIGHT) {
                listOf(90f, 125f, 160f)
            } else {
                listOf(20f, 55f, 90f)
            }

            val knobAngle = if (side == ButtonSide.RIGHT) 50f else 130f
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
            if (side == ButtonSide.RIGHT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn ->
                        LedIndicator(isOn = isOn, size = 14.dp)
                    }
                }
                SevenSegmentedPanel(
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    onColor = panelColor,
                    title = panelTitle
                )
                topExtraContent?.invoke(Modifier.size(extraContentSizeBattery))

            } else {
                topExtraContent?.invoke(Modifier.size(extraContentSizeAnalogIndicator))
                SevenSegmentedPanel(
//                    modifier = Modifier.weight(1f),
                    width = panelWidth,
                    value = panelNumber / 10f,
                    on = panelOn,
                    onColor = panelColor,
                    title = panelTitle
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledStates.forEach { isOn ->
                        LedIndicator(isOn = isOn, size = 14.dp)
                    }
                }
            }
        }
    }
}