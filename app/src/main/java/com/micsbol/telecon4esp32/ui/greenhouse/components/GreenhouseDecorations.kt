package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass
import kotlin.math.sin

private const val TEMP_SLIDER_MIN = 10f
private const val TEMP_SLIDER_MAX = 40f

@Composable
fun TemperatureGradientSlider(
    temperatureC: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = remember(temperatureC) {
        ((temperatureC - TEMP_SLIDER_MIN) / (TEMP_SLIDER_MAX - TEMP_SLIDER_MIN))
            .coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackHeight = 8.dp.toPx()
            val trackTop = (size.height - trackHeight) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(GreenhouseGlass.TempGradient),
                topLeft = Offset(0f, trackTop),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f),
            )

            val thumbX = size.width * fraction
            val thumbRadius = 10.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, size.height / 2f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = thumbRadius + 4.dp.toPx(),
                center = Offset(thumbX, size.height / 2f),
            )
        }
    }
}

@Composable
fun MetricSparkline(
    seed: Int,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val points = remember(seed) {
        List(12) { index ->
            val wave = sin(index * 0.85 + seed * 0.31).toFloat()
            val trend = index / 11f
            (0.35f + trend * 0.25f + wave * 0.18f).coerceIn(0.08f, 0.92f)
        }
    }

    Canvas(modifier = modifier.height(36.dp)) {
        if (points.size < 2) return@Canvas

        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size - 1)
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.28f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
