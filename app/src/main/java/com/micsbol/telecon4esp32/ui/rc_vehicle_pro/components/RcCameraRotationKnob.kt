package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import com.micsbol.telecon4esp32.ui.theme.DarkBackground
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val MIN_PAN_ANGLE = -135f
private const val MAX_PAN_ANGLE = 135f
private const val RADIUS_FRACTION = 0.78f
internal const val PAN_THUMB_DISTANCE_FRACTION = 0.68f
internal const val PAN_THUMB_VISUAL_FRACTION = 0.09f
internal const val PAN_THUMB_GRAB_FRACTION = 0.36f
internal const val PAN_RING_INNER_FRACTION = 0.42f
internal const val PAN_RING_OUTER_FRACTION = 1.12f

/** Normalized pan: 0 = full left, 0.5 = center, 1 = full right. */
private val PAN_MARKERS = listOf(
    PanMarker(0f, true),
    PanMarker(0.25f, false),
    PanMarker(0.5f, true),
    PanMarker(0.75f, false),
    PanMarker(1f, true),
)

private data class PanMarker(val value: Float, val major: Boolean)

@Composable
fun RcCameraRotationKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    knobSize: Dp = RcVehicleProLayout.ControlZoneKnobSize,
    onDoubleTap: (() -> Unit)? = null,
) {
    val accent = brandPrimary()
    val secondary = brandSecondary()
    val ringColor = Color.White.copy(alpha = 0.14f)

    var isDragging by remember { mutableStateOf(false) }
    var panAngle by remember { mutableFloatStateOf(valueToPanAngle(value)) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val panAngleState = rememberUpdatedState(panAngle)

    LaunchedEffect(value) {
        if (!isDragging) {
            panAngle = valueToPanAngle(value)
        }
    }

    Box(
        modifier = modifier.size(knobSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pan only starts on the white thumb and follows the ring;
                    // other hits pass through so the panel can resize.
                    var lastTapUptime = 0L
                    var lastTapPosition = Offset.Zero
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val center = knobCenter(canvasWidth, canvasHeight)
                        val radius = knobRadius(canvasWidth, canvasHeight)
                        val now = SystemClock.uptimeMillis()
                        val slop = viewConfiguration.touchSlop
                        val doubleTapHandler = currentOnDoubleTap
                        val onKnob = (down.position - center).getDistance() <= radius * 1.12f
                        val isDoubleTap = doubleTapHandler != null &&
                            onKnob &&
                            now - lastTapUptime in
                            viewConfiguration.doubleTapMinTimeMillis..
                            viewConfiguration.doubleTapTimeoutMillis &&
                            (down.position - lastTapPosition).getDistance() <= slop * 2f
                        if (isDoubleTap) {
                            doubleTapHandler.invoke()
                            lastTapUptime = 0L
                            down.consume()
                            val tapPointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == tapPointerId }
                                    ?: break
                                change.consume()
                                if (!change.pressed) break
                            }
                            return@awaitEachGesture
                        }
                        if (onKnob) {
                            lastTapUptime = now
                            lastTapPosition = down.position
                        }
                        val grabRadius = maxOf(
                            radius * PAN_THUMB_GRAB_FRACTION,
                            20.dp.toPx(),
                        )
                        val startPan = panAngleState.value
                        if (!isOnPanThumb(down.position, center, radius, startPan, grabRadius)) {
                            return@awaitEachGesture
                        }
                        down.consume()
                        isDragging = true
                        var lastAngle = touchAngle(center, down.position)
                        val pointerId = down.id
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: break
                                change.consume()
                                if (!change.pressed) break
                                if (!isOnPanRing(change.position, center, radius)) {
                                    lastAngle = touchAngle(center, change.position)
                                    continue
                                }
                                val currentAngle = touchAngle(center, change.position)
                                val delta = wrappedAngleDelta(lastAngle, currentAngle)
                                panAngle = (panAngle + delta).coerceIn(MIN_PAN_ANGLE, MAX_PAN_ANGLE)
                                lastAngle = currentAngle
                                currentOnValueChange(panAngleToValue(panAngle))
                            }
                        } finally {
                            isDragging = false
                        }
                    }
                },
        ) {
            val center = knobCenter(size.width, size.height)
            val radius = knobRadius(size.width, size.height)
            val thinStroke = Stroke(width = 1.6f.dp.toPx(), cap = StrokeCap.Round)

            drawCircle(color = ringColor, radius = radius, center = center, style = thinStroke)
            drawRotationGuideArc(center, radius, accent)
            drawPanMarkers(center, radius, accent)
            drawPanWedge(center, radius, panAngle, accent, secondary)
            drawRotationChevrons(center, radius, accent)
            drawSightLine(center, radius, panAngle, Color.White.copy(alpha = 0.9f), accent)
        }

        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = null,
            tint = brandPrimary().copy(alpha = 0.92f),
            modifier = Modifier.size(knobSize * 0.28f),
        )
    }
}

private fun knobCenter(width: Float, height: Float): Offset {
    return Offset(width / 2f, height / 2f)
}

private fun knobRadius(width: Float, height: Float): Float {
    return minOf(width, height) / 2f * RADIUS_FRACTION
}

internal fun panThumbCenter(center: Offset, radius: Float, panAngle: Float): Offset {
    val radians = Math.toRadians((panAngle - 90f).toDouble()).toFloat()
    val distance = radius * PAN_THUMB_DISTANCE_FRACTION
    return Offset(
        center.x + cos(radians) * distance,
        center.y + sin(radians) * distance,
    )
}

internal fun isOnPanThumb(
    touch: Offset,
    center: Offset,
    radius: Float,
    panAngle: Float,
    hitRadius: Float = radius * PAN_THUMB_GRAB_FRACTION,
): Boolean {
    return (touch - panThumbCenter(center, radius, panAngle)).getDistance() <= hitRadius
}

internal fun isOnPanRing(
    touch: Offset,
    center: Offset,
    radius: Float,
    innerFraction: Float = PAN_RING_INNER_FRACTION,
    outerFraction: Float = PAN_RING_OUTER_FRACTION,
): Boolean {
    val distance = (touch - center).getDistance()
    return distance >= radius * innerFraction && distance <= radius * outerFraction
}

internal fun wrappedAngleDelta(fromDegrees: Float, toDegrees: Float): Float {
    var delta = toDegrees - fromDegrees
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

private fun touchAngle(center: Offset, touch: Offset): Float {
    return Math.toDegrees(atan2((touch.y - center.y).toDouble(), (touch.x - center.x).toDouble()))
        .toFloat()
}

private fun valueToPanAngle(value: Float): Float {
    return MIN_PAN_ANGLE + value.coerceIn(0f, 1f) * (MAX_PAN_ANGLE - MIN_PAN_ANGLE)
}

private fun panAngleToValue(angle: Float): Float {
    return ((angle - MIN_PAN_ANGLE) / (MAX_PAN_ANGLE - MIN_PAN_ANGLE)).coerceIn(0f, 1f)
}

private fun DrawScope.drawRotationGuideArc(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawArc(
        color = color.copy(alpha = 0.22f),
        startAngle = MIN_PAN_ANGLE - 90f,
        sweepAngle = MAX_PAN_ANGLE - MIN_PAN_ANGLE,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 1.4f.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawPanMarkers(
    center: Offset,
    radius: Float,
    color: Color,
) {
    PAN_MARKERS.forEach { marker ->
        val angle = valueToPanAngle(marker.value)
        val radians = Math.toRadians((angle - 90f).toDouble()).toFloat()
        val outer = radius * 1.02f
        val inner = if (marker.major) radius * 0.76f else radius * 0.86f
        val alpha = if (marker.major) 0.5f else 0.24f
        val strokeWidth = if (marker.major) 2f.dp.toPx() else 1.1f.dp.toPx()
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(center.x + cos(radians) * outer, center.y + sin(radians) * outer),
            end = Offset(center.x + cos(radians) * inner, center.y + sin(radians) * inner),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawPanWedge(
    center: Offset,
    radius: Float,
    panAngle: Float,
    primary: Color,
    secondary: Color,
) {
    val wedgeRadius = radius * 0.55f
    rotate(degrees = panAngle, pivot = center) {
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.28f),
                    secondary.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = center,
                radius = wedgeRadius,
            ),
            startAngle = -22f,
            sweepAngle = 44f,
            useCenter = true,
            topLeft = Offset(center.x - wedgeRadius, center.y - wedgeRadius),
            size = Size(wedgeRadius * 2f, wedgeRadius * 2f),
        )
    }
}

private fun DrawScope.drawRotationChevrons(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val chevronRadius = radius * 1.04f
    listOf(-118f, 118f).forEach { placementAngle ->
        val radians = Math.toRadians((placementAngle - 90f).toDouble()).toFloat()
        val tip = Offset(
            center.x + cos(radians) * chevronRadius,
            center.y + sin(radians) * chevronRadius,
        )
        val tangent = placementAngle + if (placementAngle < 0f) 90f else -90f
        drawChevron(tip, tangent, color.copy(alpha = 0.55f), radius * 0.1f)
    }
}

private fun DrawScope.drawChevron(
    tip: Offset,
    directionDegrees: Float,
    color: Color,
    length: Float,
) {
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(-length * 0.55f, -length * 0.38f)
        lineTo(-length * 0.2f, 0f)
        lineTo(-length * 0.55f, length * 0.38f)
        close()
    }
    rotate(degrees = directionDegrees, pivot = tip) {
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawSightLine(
    center: Offset,
    radius: Float,
    panAngle: Float,
    color: Color,
    accent: Color,
) {
    val radians = Math.toRadians((panAngle - 90f).toDouble()).toFloat()
    val end = Offset(
        center.x + cos(radians) * radius * PAN_THUMB_DISTANCE_FRACTION,
        center.y + sin(radians) * radius * PAN_THUMB_DISTANCE_FRACTION,
    )
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = center,
        end = end,
        strokeWidth = 2f.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(color = color, radius = radius * PAN_THUMB_VISUAL_FRACTION, center = end)
    drawCircle(
        color = accent.copy(alpha = 0.4f),
        radius = radius * 0.05f,
        center = end,
        style = Stroke(width = 1.2f.dp.toPx()),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E14, widthDp = 120, heightDp = 120)
@Composable
private fun RcCameraRotationKnobPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(DarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            RcCameraRotationKnob(
                value = 0.62f,
                onValueChange = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E14, widthDp = 120, heightDp = 120)
@Composable
private fun RcCameraRotationKnobCenterPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(DarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            RcCameraRotationKnob(
                value = 0.5f,
                onValueChange = {},
            )
        }
    }
}
