package com.micsbol.telecon4esp32.ui.control_panel.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.copy
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.color

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

        drawIntoCanvas {
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                this.color = color.copy(alpha = 0.5f).toArgb()
                maskFilter = BlurMaskFilter(
                    radius * 1.5f, // The radius of the glow
                    BlurMaskFilter.Blur.NORMAL
                )
            }
            it.nativeCanvas.drawCircle(
                center.x,
                center.y,
                radius - borderWidth,
                paint
            )
        }

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