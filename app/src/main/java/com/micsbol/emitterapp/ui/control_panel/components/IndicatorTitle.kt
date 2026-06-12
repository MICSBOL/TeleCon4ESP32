package com.micsbol.emitterapp.ui.control_panel.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.emitterapp.ui.theme.EmitterAppTheme

/**
 * A composable that displays text with a fluorescent, LED-like glow effect.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param text The string content to display.
 * @param textColor The color of the crisp, main text.
 * @param glowColor The color of the blurred glow effect. Defaults to textColor.
 * @param textSize The size of the text.
 * @param glowRadius The radius of the blur effect, creating the glow.
 * @param frameGlowRadius The radius of the blur effect for the optional frame.
 * @param showFrame Toggles the visibility of the glowing frame.
 */
@Composable
fun IndicatorTitle(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = Color.White,
    glowColor: Color = textColor,
    textSize: TextUnit = 8.sp,
    glowRadius: Dp = 10.dp,
    frameGlowRadius: Dp = 100.dp,
    showFrame: Boolean = true
) {
    // 1. Define a text style that disables the font's built-in vertical padding.
    val textStyle = TextStyle(
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        fontSize = textSize,
        platformStyle = PlatformTextStyle(
            includeFontPadding = false
        )
    )

    Box(
        // The modifier from the function signature is now correctly applied ONLY to the outer Box.
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // This part for the background and frame is correct.
        Canvas(modifier = Modifier
            .align(alignment = Alignment.Center)
            .matchParentSize()) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Fill,
                cornerRadius = CornerRadius(8.dp.toPx())
            )
        }
        if (showFrame) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = frameGlowRadius * 2f)
            ) {
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.5f),
                    style = Stroke(width = 3.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = frameGlowRadius)
            ) {
                drawRoundRect(
                    color = glowColor,
                    style = Stroke(width = 2.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
        }

        HorizontalTextAnimation(
            text = "  $text  ",
            style = textStyle,
            color = glowColor,
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .blur(radius = glowRadius),
            characterThreshold = 10
        )
        HorizontalTextAnimation(
            text = "  $text  ",
            style = textStyle,
            color = textColor,
            modifier = Modifier
                .align(alignment = Alignment.Center),
            characterThreshold = 10
        )

//        Text(
//            text = "  $text  ",
//            color = glowColor,
//            style = textStyle,
//            modifier = Modifier.blur(radius = glowRadius)
//        )
//
//        Text(
//            text = "  $text  ",
//            color = textColor,
//            style = textStyle
//        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
private fun IndicatorTitlePreview() {
    EmitterAppTheme {
        IndicatorTitle(
            modifier = Modifier.padding(8.dp),
            text = "RPM",
            textColor = Color.Cyan,
            glowColor = Color.Cyan.copy(alpha = 0.7f),
            textSize = 32.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
private fun IndicatorTitleNoFramePreview() {
    EmitterAppTheme {
        IndicatorTitle(
            modifier = Modifier.padding(2.dp),
            text = "BATTERY",
            textColor = Color(0xFFFFA500), // Orange
            glowColor = Color(0xFFFFA500).copy(alpha = 0.5f),
            textSize = 8.sp,
            showFrame = true // Preview without the frame
        )
    }
}