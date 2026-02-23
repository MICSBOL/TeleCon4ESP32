package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.emitterapp.ui.rc_screen.components.IndicatorTitle
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D
import com.example.emitterapp.ui.rc_screen.components.Knob3D
import com.example.emitterapp.ui.rc_screen.components.SevenSegmentedPanel
import com.example.emitterapp.ui.rc_screen.components.Switch3DButton
import kotlin.math.cos
import kotlin.math.sin

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
    panelTitle: String,
    indicatorTitle: String,
    topExtraContent: (@Composable (modifier: Modifier) -> Unit)? = null,
    aspectRatio: Float
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val baseSize = min(maxWidth, maxHeight)
        var joystickSize = 0.dp
        when {

            aspectRatio > 2.1f -> { joystickSize = maxHeight * 0.6f }
            aspectRatio > 2.0f -> { joystickSize = maxHeight * 0.6f }
            aspectRatio > 1.7f -> { joystickSize = maxHeight * 0.6f }
            aspectRatio > 1.4f -> { joystickSize = maxHeight * 0.6f }
            else -> {joystickSize = baseSize * 0.3f}
        }
        val knobSize = joystickSize * 0.4f
        val switchSize = joystickSize * 0.4f

        val extraContentSizeBattery = joystickSize * 0.35f
        val extraContentSizeAnalogIndicator = joystickSize * 0.3f

        val panelWidth = joystickSize * 0.5f
        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.Center,
        ) {
            Joystick_RC3D(
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
                    onColor = panelColor,
                    title = panelTitle
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    topExtraContent?.invoke(Modifier.size(extraContentSizeBattery))
                    IndicatorTitle(
                        modifier = Modifier.padding(2.dp),
                        text = indicatorTitle,
                        textColor = Color(0xFFFFA500), // Orange
                        glowColor = Color(0xFFFFA500).copy(alpha = 0.5f),
                        textSize = 10.sp,
                        showFrame = true // Preview without the frame
                    )
                }
            } else {
                Column {
                    topExtraContent?.invoke(Modifier.size(extraContentSizeAnalogIndicator))
                    IndicatorTitle(
                        modifier = Modifier.padding(2.dp),
                        text = indicatorTitle,
                        textColor = Color(0xFFFFA500), // Orange
                        glowColor = Color(0xFFFFA500).copy(alpha = 0.5f),
                        textSize = 10.sp,
                        showFrame = true // Preview without the frame
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                SevenSegmentedPanel(
                    width = panelWidth,
                    number = panelNumber,
                    on = panelOn,
                    onColor = panelColor,
                    title = panelTitle
                )
            }
        }

    }
}