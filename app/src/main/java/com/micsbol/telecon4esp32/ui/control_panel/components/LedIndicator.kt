package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable that displays a circular LED-style indicator.
 *
 * @param modifier An optional modifier to apply to the composable.
 * @param isOn The state of the LED. If true, a glowing effect is applied.
 * @param size The total size of the indicator, including its border.
 * @param color The primary color of the LED when it is on.
 */
@Composable
fun LedIndicator(
    modifier: Modifier = Modifier,
    isOn: Boolean,
    size: Dp,
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val color = if (isOn) Color.Green else Color.Red
        val radius = size.toPx() / 2
        val center = Offset(x = radius, y = radius)
        val borderWidth = size.toPx() * 0.1f

        drawCircle(
            color = Color.Black,
            radius = radius,
            center = center
        )

        val unlitColor = color.copy(alpha = 0.2f)

        drawCircle(
            color = unlitColor,
            radius = radius - (borderWidth / 2),
            center = center
        )

        drawCircle(
            color = color.copy(alpha = 0.28f),
            radius = radius * 0.92f,
            center = center,
        )

        drawCircle(
            color = color,
            radius = radius - borderWidth,
            center = center
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun LedIndicatorPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Preview for the 'on' state with a red color
        LedIndicator(
            isOn = true,
            size = 48.dp,
        )
        Spacer(Modifier.height(16.dp))
        // Preview for the 'off' state with a red color
        LedIndicator(
            isOn = false,
            size = 48.dp,
        )
        Spacer(Modifier.height(16.dp))
        // Preview for the 'on' state with a green color
        LedIndicator(
            isOn = false,
            size = 48.dp,
        )
    }
}