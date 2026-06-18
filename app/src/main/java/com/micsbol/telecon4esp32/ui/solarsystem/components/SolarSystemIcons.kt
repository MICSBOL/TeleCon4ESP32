package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.solarsystem.SolarIconSize
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemIconType
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SolarSystemIcon(
    type: SolarSystemIconType,
    tint: Color,
    modifier: Modifier = Modifier,
    size: SolarIconSize = SolarIconSize.Metric,
    showBadge: Boolean = true,
) {
    val badgeSize = when (size) {
        SolarIconSize.Metric -> 52.dp
        SolarIconSize.PowerFlow -> 76.dp
        SolarIconSize.Hero -> 60.dp
    }
    val iconScale = when (size) {
        SolarIconSize.Metric -> 0.68f
        SolarIconSize.PowerFlow -> 0.82f
        SolarIconSize.Hero -> 0.78f
    }

    if (showBadge && size == SolarIconSize.Metric) {
        SolarIconBadge(
            tint = tint,
            size = badgeSize,
            modifier = modifier,
        ) {
            SolarSystemIconCanvas(
                type = type,
                tint = tint,
                scale = iconScale,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(badgeSize)
                .solarIconGlow(tint),
            contentAlignment = Alignment.Center,
        ) {
            SolarSystemIconCanvas(
                type = type,
                tint = tint,
                scale = iconScale,
            )
        }
    }
}

@Composable
private fun SolarIconBadge(
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .solarIconGlow(tint),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.28f),
                            tint.copy(alpha = 0.10f),
                            tint.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private fun Modifier.solarIconGlow(tint: Color): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tint.copy(alpha = 0.34f),
                tint.copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = center,
            radius = size.minDimension * 0.85f,
        ),
        radius = size.minDimension * 0.85f,
        center = center,
    )
}

@Composable
private fun SolarSystemIconCanvas(
    type: SolarSystemIconType,
    tint: Color,
    scale: Float,
) {
    Canvas(modifier = Modifier.size(56.dp)) {
        val s = size.minDimension * scale
        val left = (size.width - s) / 2f
        val top = (size.height - s) / 2f
        val bounds = Rect(left, top, left + s, top + s)

        when (type) {
            SolarSystemIconType.SOLAR -> drawSolarIcon(bounds)
            SolarSystemIconType.LOAD -> drawLoadIcon(bounds, tint)
            SolarSystemIconType.BATTERY -> drawBatteryIcon(bounds, tint)
            SolarSystemIconType.GRID -> drawGridIcon(bounds, tint)
            SolarSystemIconType.VOLTAGE -> drawVoltageIcon(bounds, tint)
            SolarSystemIconType.CURRENT -> drawCurrentIcon(bounds, tint)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSolarIcon(bounds: Rect) {
    val sunYellow = Color(0xFFFFD700)
    val sunOrange = Color(0xFFFFA500)
    val panelBlue = Color(0xFF2196F3)

    val sunCenter = Offset(
        bounds.left + bounds.width * 0.33f,
        bounds.top + bounds.height * 0.28f,
    )
    val sunRadius = bounds.width * 0.135f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sunYellow, sunOrange),
            center = sunCenter,
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = sunCenter,
    )

    val rayLength = sunRadius * 0.62f
    val rayThickness = bounds.width * 0.07f
    repeat(8) { index ->
        val angle = index * (PI / 4.0) - (PI / 2.0)
        val inner = sunRadius * 1.05f
        val outer = sunRadius + rayLength
        drawLine(
            color = sunOrange,
            start = Offset(
                sunCenter.x + (inner * cos(angle)).toFloat(),
                sunCenter.y + (inner * sin(angle)).toFloat(),
            ),
            end = Offset(
                sunCenter.x + (outer * cos(angle)).toFloat(),
                sunCenter.y + (outer * sin(angle)).toFloat(),
            ),
            strokeWidth = rayThickness,
            cap = StrokeCap.Round,
        )
    }

    val panelTopLeft = Offset(bounds.left + bounds.width * 0.18f, bounds.top + bounds.height * 0.36f)
    val panelTopRight = Offset(bounds.right - bounds.width * 0.10f, bounds.top + bounds.height * 0.30f)
    val panelBottomRight = Offset(bounds.right - bounds.width * 0.06f, bounds.top + bounds.height * 0.70f)
    val panelBottomLeft = Offset(bounds.left + bounds.width * 0.12f, bounds.top + bounds.height * 0.76f)

    val panelPath = Path().apply {
        moveTo(panelTopLeft.x, panelTopLeft.y)
        lineTo(panelTopRight.x, panelTopRight.y)
        lineTo(panelBottomRight.x, panelBottomRight.y)
        lineTo(panelBottomLeft.x, panelBottomLeft.y)
        close()
    }

    val frameStroke = bounds.width * 0.042f
    val gridStroke = bounds.width * 0.024f
    val panelStroke = Stroke(
        width = frameStroke,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    drawPath(path = panelPath, color = panelBlue, style = panelStroke)

    val verticalFractions = listOf(1f / 3f, 2f / 3f)
    verticalFractions.forEach { fraction ->
        val top = lerp(panelTopLeft, panelTopRight, fraction)
        val bottom = lerp(panelBottomLeft, panelBottomRight, fraction)
        drawLine(
            color = panelBlue,
            start = top,
            end = bottom,
            strokeWidth = gridStroke,
            cap = StrokeCap.Round,
        )
    }

    val rowMidLeft = lerp(panelTopLeft, panelBottomLeft, 0.5f)
    val rowMidRight = lerp(panelTopRight, panelBottomRight, 0.5f)
    drawLine(
        color = panelBlue,
        start = rowMidLeft,
        end = rowMidRight,
        strokeWidth = gridStroke,
        cap = StrokeCap.Round,
    )

    val standWidth = bounds.width * 0.14f
    val standHeight = bounds.width * 0.06f
    val standCenterX = (panelBottomLeft.x + panelBottomRight.x) / 2f
    val standTop = maxOf(panelBottomLeft.y, panelBottomRight.y) + bounds.height * 0.02f
    drawRoundRect(
        color = panelBlue,
        topLeft = Offset(standCenterX - standWidth / 2f, standTop),
        size = Size(standWidth, standHeight),
        cornerRadius = CornerRadius(standHeight * 0.35f),
    )
}

private fun lerp(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    start.x + (end.x - start.x) * fraction,
    start.y + (end.y - start.y) * fraction,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBatteryIcon(bounds: Rect, tint: Color) {
    val bodyWidth = bounds.width * 0.46f
    val bodyHeight = bounds.height * 0.72f
    val left = bounds.center.x - bodyWidth / 2f
    val top = bounds.top + bounds.height * 0.12f
    val corner = bounds.width * 0.08f

    drawRoundRect(
        color = tint.copy(alpha = 0.2f),
        topLeft = Offset(left - 3f, top - 3f),
        size = Size(bodyWidth + 6f, bodyHeight + 6f),
        cornerRadius = CornerRadius(corner + 2f),
    )
    drawRoundRect(
        color = tint.copy(alpha = 0.35f),
        topLeft = Offset(left, top),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = bounds.width * 0.06f),
    )

    val capWidth = bodyWidth * 0.38f
    val capHeight = bounds.height * 0.08f
    drawRoundRect(
        color = tint,
        topLeft = Offset(bounds.center.x - capWidth / 2f, top - capHeight * 0.85f),
        size = Size(capWidth, capHeight),
        cornerRadius = CornerRadius(2f),
    )

    val fillHeight = bodyHeight * 0.62f
    drawRoundRect(
        color = tint.copy(alpha = 0.55f),
        topLeft = Offset(left + bodyWidth * 0.12f, top + bodyHeight - fillHeight - bodyWidth * 0.1f),
        size = Size(bodyWidth * 0.76f, fillHeight),
        cornerRadius = CornerRadius(corner * 0.6f),
    )

    val bolt = Path().apply {
        moveTo(bounds.center.x, top + bodyHeight * 0.28f)
        lineTo(bounds.center.x - bodyWidth * 0.12f, bounds.center.y + bodyHeight * 0.02f)
        lineTo(bounds.center.x + bodyWidth * 0.04f, bounds.center.y + bodyHeight * 0.02f)
        lineTo(bounds.center.x - bodyWidth * 0.06f, top + bodyHeight * 0.72f)
        lineTo(bounds.center.x + bodyWidth * 0.14f, bounds.center.y - bodyHeight * 0.04f)
        lineTo(bounds.center.x - bodyWidth * 0.02f, bounds.center.y - bodyHeight * 0.04f)
        close()
    }
    drawPath(path = bolt, color = Color.White)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLoadIcon(bounds: Rect, tint: Color) {
    val houseWidth = bounds.width * 0.72f
    val houseHeight = bounds.height * 0.52f
    val left = bounds.center.x - houseWidth / 2f
    val baseTop = bounds.top + bounds.height * 0.46f

    val roof = Path().apply {
        moveTo(left - houseWidth * 0.06f, baseTop)
        lineTo(bounds.center.x, bounds.top + bounds.height * 0.14f)
        lineTo(left + houseWidth + houseWidth * 0.06f, baseTop)
        close()
    }
    drawPath(path = roof, color = tint.copy(alpha = 0.85f))

    drawRoundRect(
        color = tint.copy(alpha = 0.25f),
        topLeft = Offset(left, baseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(bounds.width * 0.04f),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, baseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(bounds.width * 0.04f),
        style = Stroke(width = bounds.width * 0.045f),
    )

    val doorWidth = houseWidth * 0.22f
    val doorHeight = houseHeight * 0.42f
    drawRoundRect(
        color = tint.copy(alpha = 0.7f),
        topLeft = Offset(bounds.center.x - doorWidth / 2f, baseTop + houseHeight - doorHeight),
        size = Size(doorWidth, doorHeight),
        cornerRadius = CornerRadius(2f),
    )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(left + houseWidth * 0.14f, baseTop + houseHeight * 0.18f),
        size = Size(houseWidth * 0.2f, houseHeight * 0.22f),
        cornerRadius = CornerRadius(2f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(left + houseWidth * 0.62f, baseTop + houseHeight * 0.18f),
        size = Size(houseWidth * 0.2f, houseHeight * 0.22f),
        cornerRadius = CornerRadius(2f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridIcon(bounds: Rect, tint: Color) {
    val stroke = bounds.width * 0.028f
    val cx = bounds.center.x
    val w = bounds.width
    val h = bounds.height

    fun line(start: Offset, end: Offset, alpha: Float = 1f) {
        drawLine(
            color = tint.copy(alpha = alpha),
            start = start,
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }

    fun crossArm(y: Float, halfExtend: Float, hookDrop: Float, hookOut: Float) {
        val left = cx - halfExtend
        val right = cx + halfExtend
        line(Offset(left, y), Offset(right, y))
        line(Offset(left, y), Offset(left - hookOut, y + hookDrop))
        line(Offset(right, y), Offset(right + hookOut, y + hookDrop))
    }

    val baseY = bounds.top + h * 0.90f
    val midY = bounds.top + h * 0.62f
    val lowerArmY = bounds.top + h * 0.46f
    val upperArmY = bounds.top + h * 0.24f
    val peakY = bounds.top + h * 0.07f

    val baseHalf = w * 0.30f
    val midHalf = w * 0.22f
    val lowerHalf = w * 0.17f
    val upperHalf = w * 0.11f

    val baseLeft = Offset(cx - baseHalf, baseY)
    val baseRight = Offset(cx + baseHalf, baseY)
    val midLeft = Offset(cx - midHalf, midY)
    val midRight = Offset(cx + midHalf, midY)
    val lowerLeft = Offset(cx - lowerHalf, lowerArmY)
    val lowerRight = Offset(cx + lowerHalf, lowerArmY)
    val upperLeft = Offset(cx - upperHalf, upperArmY)
    val upperRight = Offset(cx + upperHalf, upperArmY)
    val peak = Offset(cx, peakY)

    line(baseLeft, lowerLeft)
    line(baseRight, lowerRight)
    line(lowerLeft, upperLeft)
    line(lowerRight, upperRight)
    line(upperLeft, peak)
    line(upperRight, peak)

    line(baseLeft, baseRight, alpha = 0.85f)
    line(lowerLeft, lowerRight, alpha = 0.85f)
    line(upperLeft, upperRight, alpha = 0.85f)

    line(baseLeft, lowerRight, alpha = 0.8f)
    line(baseRight, lowerLeft, alpha = 0.8f)
    line(midLeft, midRight, alpha = 0.8f)
    line(midLeft, lowerRight, alpha = 0.75f)
    line(midRight, lowerLeft, alpha = 0.75f)
    line(lowerLeft, upperRight, alpha = 0.75f)
    line(lowerRight, upperLeft, alpha = 0.75f)

    crossArm(
        y = lowerArmY,
        halfExtend = w * 0.36f,
        hookDrop = h * 0.05f,
        hookOut = w * 0.04f,
    )
    crossArm(
        y = upperArmY,
        halfExtend = w * 0.26f,
        hookDrop = h * 0.04f,
        hookOut = w * 0.035f,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVoltageIcon(bounds: Rect, tint: Color) {
    val gaugeColor = tint
    val center = bounds.center
    val arcHalfWidth = bounds.width * 0.34f
    val arcHalfHeight = bounds.width * 0.30f
    val arcRect = Rect(
        left = center.x - arcHalfWidth,
        top = center.y - arcHalfHeight,
        right = center.x + arcHalfWidth,
        bottom = center.y + arcHalfHeight,
    )
    val radiusX = arcRect.width / 2f
    val radiusY = arcRect.height / 2f
    val pivot = center

    val startAngle = 155f
    val sweepAngle = 230f
    val outerStroke = bounds.width * 0.034f
    val innerStroke = bounds.width * 0.024f

    drawArc(
        color = gaugeColor.copy(alpha = 0.22f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStroke * 1.75f, cap = StrokeCap.Round),
    )
    drawArc(
        color = gaugeColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
    )

    val inset = bounds.width * 0.048f
    val innerRect = Rect(
        left = arcRect.left + inset,
        top = arcRect.top + inset,
        right = arcRect.right - inset,
        bottom = arcRect.bottom - inset * 0.35f,
    )
    val dashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(bounds.width * 0.038f, bounds.width * 0.032f),
    )
    drawArc(
        color = gaugeColor.copy(alpha = 0.24f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = innerRect.topLeft,
        size = innerRect.size,
        style = Stroke(
            width = innerStroke * 1.6f,
            cap = StrokeCap.Round,
            pathEffect = dashEffect,
        ),
    )
    drawArc(
        color = gaugeColor.copy(alpha = 0.88f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = innerRect.topLeft,
        size = innerRect.size,
        style = Stroke(
            width = innerStroke,
            cap = StrokeCap.Round,
            pathEffect = dashEffect,
        ),
    )

    val needleAngleDeg = 328f
    val needleRad = needleAngleDeg * PI / 180.0
    val needleLength = minOf(radiusX, radiusY) * 0.72f
    val needleTip = Offset(
        pivot.x + (needleLength * cos(needleRad)).toFloat(),
        pivot.y + (needleLength * sin(needleRad)).toFloat(),
    )
    val baseRadius = outerStroke * 0.72f

    drawCircle(
        color = gaugeColor.copy(alpha = 0.22f),
        radius = baseRadius * 1.55f,
        center = pivot,
    )
    drawCircle(color = gaugeColor, radius = baseRadius, center = pivot)

    val baseHalf = outerStroke * 0.55f
    val perpX = (-sin(needleRad) * baseHalf).toFloat()
    val perpY = (cos(needleRad) * baseHalf).toFloat()
    val needlePath = Path().apply {
        moveTo(pivot.x + perpX, pivot.y + perpY)
        lineTo(needleTip.x, needleTip.y)
        lineTo(pivot.x - perpX, pivot.y - perpY)
        close()
    }
    drawPath(path = needlePath, color = gaugeColor.copy(alpha = 0.24f))
    drawPath(path = needlePath, color = gaugeColor)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentIcon(bounds: Rect, tint: Color) {
    val radius = bounds.width * 0.38f
    val center = bounds.center
    drawCircle(color = tint.copy(alpha = 0.18f), radius = radius * 1.08f, center = center)
    drawCircle(
        color = tint.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = bounds.width * 0.045f),
    )

    val wave = Path()
    val startX = center.x - radius * 0.72f
    val endX = center.x + radius * 0.72f
    val step = (endX - startX) / 24f
    wave.moveTo(startX, center.y)
    for (i in 0..24) {
        val x = startX + step * i
        val y = center.y + sin(i * PI / 4.0).toFloat() * radius * 0.42f
        wave.lineTo(x, y)
    }
    drawPath(
        path = wave,
        color = tint,
        style = Stroke(width = bounds.width * 0.055f, cap = StrokeCap.Round),
    )
}
