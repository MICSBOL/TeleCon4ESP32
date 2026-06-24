package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val MIN_KNOB_ANGLE = 135f
private const val MAX_KNOB_ANGLE = 405f

@Composable
fun DashboardOutputKnobWidget(
    level: Float,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val percent = (level.coerceIn(0f, 1f) * 100f).toInt()

    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            DashboardOutputDial(
                level = level,
                enabled = enabled,
                onLevelChange = onLevelChange,
            )
        }

        Text(
            text = stringResource(R.string.custom_dashboard_output_percent, percent),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = StatusConnected,
        )
    }
}

@Composable
private fun DashboardOutputDial(
    level: Float,
    enabled: Boolean,
    onLevelChange: (Float) -> Unit,
) {
    val accent = brandPrimary()
    val secondary = brandSecondary()
    val trackColor = Color.White.copy(alpha = 0.12f)

    var isDragging by remember { mutableStateOf(false) }
    var knobAngle by remember { mutableFloatStateOf(levelToAngle(level)) }
    val currentOnLevelChange by rememberUpdatedState(onLevelChange)

    LaunchedEffect(level) {
        if (!isDragging) {
            knobAngle = levelToAngle(level)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                ) { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = touchAngle(center, change.position)
                    knobAngle = angle.coerceIn(MIN_KNOB_ANGLE, MAX_KNOB_ANGLE)
                    currentOnLevelChange(angleToLevel(knobAngle))
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height * 0.58f)
        val radius = minOf(size.width, size.height) * 0.34f
        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)

        drawArc(
            color = trackColor,
            startAngle = MIN_KNOB_ANGLE,
            sweepAngle = MAX_KNOB_ANGLE - MIN_KNOB_ANGLE,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = stroke,
        )

        val sweep = (knobAngle - MIN_KNOB_ANGLE).coerceAtLeast(0f)
        drawArc(
            brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.35f), accent, secondary)),
            startAngle = MIN_KNOB_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = stroke,
        )

        val pointerAngle = Math.toRadians(knobAngle.toDouble())
        val pointerEnd = Offset(
            center.x + cos(pointerAngle).toFloat() * radius * 0.72f,
            center.y + sin(pointerAngle).toFloat() * radius * 0.72f,
        )
        drawCircle(color = accent, radius = 8.dp.toPx(), center = center)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = center,
            end = pointerEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun touchAngle(center: Offset, touch: Offset): Float {
    val angle = Math.toDegrees(atan2((touch.y - center.y).toDouble(), (touch.x - center.x).toDouble()))
        .toFloat()
    return if (angle < 0f) angle + 360f else angle
}

private fun levelToAngle(level: Float): Float {
    return MIN_KNOB_ANGLE + level.coerceIn(0f, 1f) * (MAX_KNOB_ANGLE - MIN_KNOB_ANGLE)
}

private fun angleToLevel(angle: Float): Float {
    return ((angle - MIN_KNOB_ANGLE) / (MAX_KNOB_ANGLE - MIN_KNOB_ANGLE)).coerceIn(0f, 1f)
}
