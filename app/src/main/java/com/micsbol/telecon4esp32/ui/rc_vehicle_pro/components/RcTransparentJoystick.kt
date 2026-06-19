package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.theme.DarkBackground
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val RADIUS_FRACTION = 0.78f
private val LEVEL_POSITIONS = listOf(-1f, -0.5f, 0f, 0.5f, 1f)

@Composable
fun RcTransparentJoystick(
    stickPosition: Pair<Float, Float>,
    mode: JoystickMode,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = RcVehicleProLayout.JoystickSize,
) {
    val ringColor = brandPrimary().copy(alpha = 0.45f)
    val levelColor = brandPrimary()
    val arcBrush = Brush.sweepGradient(
        listOf(brandPrimary().copy(alpha = 0.2f), brandPrimary(), brandSecondary()),
    )
    val thumbColor = Color.White.copy(alpha = 0.85f)
    val innerRingColor = Color.White.copy(alpha = 0.12f)

    val initialNormalized = remember(mode) { mode.toInitialNormalized() }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mode, initialNormalized) {
                    fun updateFromTouch(touch: Offset) {
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val radius = joystickRadius(this.size.width.toFloat(), this.size.height.toFloat())
                        val vector = constrainVector(touch - center, radius, mode)
                        val normalized = vectorToNormalized(vector, radius)
                        onMove(normalized.first, normalized.second)
                    }

                    detectDragGestures(
                        onDragStart = { offset -> updateFromTouch(offset) },
                        onDragEnd = {
                            when (mode) {
                                is JoystickMode.Spring,
                                is JoystickMode.VerticalSpring,
                                is JoystickMode.HorizontalSpring,
                                -> onMove(initialNormalized.first, initialNormalized.second)

                                else -> Unit
                            }
                        },
                        onDragCancel = {
                            when (mode) {
                                is JoystickMode.Spring,
                                is JoystickMode.VerticalSpring,
                                is JoystickMode.HorizontalSpring,
                                -> onMove(initialNormalized.first, initialNormalized.second)

                                else -> Unit
                            }
                        },
                    ) { change, _ ->
                        change.consume()
                        updateFromTouch(change.position)
                    }
                },
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = joystickRadius(this.size.width.toFloat(), this.size.height.toFloat())
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)

            drawCircle(
                color = innerRingColor,
                radius = radius,
                center = center,
                style = stroke,
            )

            drawRingLevelTicks(center, radius, levelColor)
            drawAxisLevelLines(center, radius, mode, levelColor)

            val arcSweep = arcSweepDegrees(stickPosition, mode)
            if (arcSweep != 0f) {
                drawArc(
                    brush = arcBrush,
                    startAngle = arcStartAngle(stickPosition, mode),
                    sweepAngle = arcSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            val thumbOffset = normalizedToOffset(stickPosition, radius)
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = radius * 0.24f,
                center = center + thumbOffset + Offset(0f, 2.dp.toPx()),
            )
            drawCircle(
                color = thumbColor,
                radius = radius * 0.22f,
                center = center + thumbOffset,
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = radius * 0.12f,
                center = center + thumbOffset,
                style = stroke,
            )
        }
    }
}

private fun joystickRadius(width: Float, height: Float): Float {
    return minOf(width, height) / 2f * RADIUS_FRACTION
}

private fun DrawScope.drawRingLevelTicks(
    center: Offset,
    radius: Float,
    levelColor: Color,
) {
    val tickCount = 24
    val outerRadius = radius * 1.02f
    val innerRadius = radius * 0.9f
    for (index in 0 until tickCount) {
        val angle = (index.toFloat() / tickCount) * 2f * PI.toFloat()
        val isMajor = index % 6 == 0
        val start = Offset(
            center.x + cos(angle) * outerRadius,
            center.y + sin(angle) * outerRadius,
        )
        val endRadius = if (isMajor) innerRadius * 0.96f else innerRadius
        val end = Offset(
            center.x + cos(angle) * endRadius,
            center.y + sin(angle) * endRadius,
        )
        drawLine(
            color = levelColor.copy(alpha = if (isMajor) 0.42f else 0.2f),
            start = start,
            end = end,
            strokeWidth = if (isMajor) 1.6f.dp.toPx() else 1f.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawAxisLevelLines(
    center: Offset,
    radius: Float,
    mode: JoystickMode,
    levelColor: Color,
) {
    when (mode) {
        is JoystickMode.VerticalSpring,
        is JoystickMode.VerticalHold,
        -> {
            drawLine(
                color = levelColor.copy(alpha = 0.18f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            LEVEL_POSITIONS.forEach { level ->
                drawHorizontalLevelTick(center, radius, level, levelColor)
            }
        }

        is JoystickMode.HorizontalSpring,
        is JoystickMode.HorizontalHold,
        -> {
            drawLine(
                color = levelColor.copy(alpha = 0.18f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            LEVEL_POSITIONS.forEach { level ->
                drawVerticalLevelTick(center, radius, level, levelColor)
            }
        }

        else -> {
            drawLine(
                color = levelColor.copy(alpha = 0.14f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = levelColor.copy(alpha = 0.14f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawHorizontalLevelTick(
    center: Offset,
    radius: Float,
    level: Float,
    levelColor: Color,
) {
    val y = center.y - level * radius
    val isCenter = level == 0f
    val isMajor = abs(level) == 1f
    val halfWidth = when {
        isCenter -> radius * 0.42f
        isMajor -> radius * 0.28f
        else -> radius * 0.18f
    }
    val alpha = when {
        isCenter -> 0.58f
        isMajor -> 0.45f
        else -> 0.28f
    }
    drawLine(
        color = levelColor.copy(alpha = alpha),
        start = Offset(center.x - halfWidth, y),
        end = Offset(center.x + halfWidth, y),
        strokeWidth = if (isCenter) 1.8f.dp.toPx() else 1.2f.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawVerticalLevelTick(
    center: Offset,
    radius: Float,
    level: Float,
    levelColor: Color,
) {
    val x = center.x + level * radius
    val isCenter = level == 0f
    val isMajor = abs(level) == 1f
    val halfHeight = when {
        isCenter -> radius * 0.42f
        isMajor -> radius * 0.28f
        else -> radius * 0.18f
    }
    val alpha = when {
        isCenter -> 0.58f
        isMajor -> 0.45f
        else -> 0.28f
    }
    drawLine(
        color = levelColor.copy(alpha = alpha),
        start = Offset(x, center.y - halfHeight),
        end = Offset(x, center.y + halfHeight),
        strokeWidth = if (isCenter) 1.8f.dp.toPx() else 1.2f.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun JoystickMode.toInitialNormalized(): Pair<Float, Float> {
    val initialGridPos = when (this) {
        is JoystickMode.Spring -> initialPosition
        is JoystickMode.Hold -> initialPosition
        is JoystickMode.VerticalSpring -> initialPosition
        is JoystickMode.VerticalHold -> initialPosition
        is JoystickMode.HorizontalSpring -> initialPosition
        is JoystickMode.HorizontalHold -> initialPosition
    }
    val (gridX, gridY) = initialGridPos
    return Pair((gridX - 6) / 6f, -(gridY - 6) / 6f)
}

private fun constrainVector(
    vector: Offset,
    radius: Float,
    mode: JoystickMode,
): Offset {
    var constrained = vector
    val distance = sqrt(constrained.x * constrained.x + constrained.y * constrained.y)
    if (distance > radius) {
        val angle = atan2(constrained.y, constrained.x)
        constrained = Offset(cos(angle) * radius, sin(angle) * radius)
    }
    return when (mode) {
        is JoystickMode.VerticalSpring,
        is JoystickMode.VerticalHold,
        -> Offset(0f, constrained.y)

        is JoystickMode.HorizontalSpring,
        is JoystickMode.HorizontalHold,
        -> Offset(constrained.x, 0f)

        else -> constrained
    }
}

private fun vectorToNormalized(vector: Offset, radius: Float): Pair<Float, Float> {
    if (radius <= 0f) return Pair(0f, 0f)
    return Pair(
        (vector.x / radius).coerceIn(-1f, 1f),
        (-(vector.y / radius)).coerceIn(-1f, 1f),
    )
}

private fun normalizedToOffset(position: Pair<Float, Float>, radius: Float): Offset {
    return Offset(position.first * radius, -position.second * radius)
}

private fun arcStartAngle(position: Pair<Float, Float>, mode: JoystickMode): Float {
    return when (mode) {
        is JoystickMode.VerticalSpring,
        is JoystickMode.VerticalHold,
        -> if (position.second >= 0f) 270f else 90f

        is JoystickMode.HorizontalSpring,
        is JoystickMode.HorizontalHold,
        -> if (position.first >= 0f) 0f else 180f

        else -> {
            val angle = Math.toDegrees(atan2(-position.second.toDouble(), position.first.toDouble()))
            angle.toFloat() - 90f
        }
    }
}

private fun arcSweepDegrees(position: Pair<Float, Float>, mode: JoystickMode): Float {
    val magnitude = when (mode) {
        is JoystickMode.VerticalSpring,
        is JoystickMode.VerticalHold,
        -> abs(position.second)

        is JoystickMode.HorizontalSpring,
        is JoystickMode.HorizontalHold,
        -> abs(position.first)

        else -> sqrt(position.first * position.first + position.second * position.second)
    }
    return (magnitude.coerceIn(0f, 1f) * 72f).coerceAtLeast(0f)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E14, widthDp = 180, heightDp = 180)
@Composable
private fun RcTransparentJoystickThrottlePreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(DarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            RcTransparentJoystick(
                stickPosition = Pair(0f, 0.55f),
                mode = JoystickMode.VerticalHold(JoystickMode.DOWN),
                onMove = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E14, widthDp = 180, heightDp = 180)
@Composable
private fun RcTransparentJoystickSteeringPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(DarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            RcTransparentJoystick(
                stickPosition = Pair(-0.4f, 0f),
                mode = JoystickMode.HorizontalSpring(JoystickMode.CENTER),
                onMove = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E14, widthDp = 380, heightDp = 200)
@Composable
private fun RcTransparentJoystickPairPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Row(
            modifier = Modifier
                .background(DarkBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RcTransparentJoystick(
                stickPosition = Pair(0f, 0.55f),
                mode = JoystickMode.VerticalHold(JoystickMode.DOWN),
                onMove = { _, _ -> },
            )
            RcTransparentJoystick(
                stickPosition = Pair(0.35f, 0f),
                mode = JoystickMode.HorizontalSpring(JoystickMode.CENTER),
                onMove = { _, _ -> },
            )
        }
    }
}
