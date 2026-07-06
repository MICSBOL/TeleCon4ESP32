package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.solarsystem.SolarIconSize
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import com.micsbol.telecon4esp32.ui.solarsystem.SolarSystemIconType
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
        SolarIconSize.Metric -> 54.dp
        SolarIconSize.PowerFlow -> 76.dp
        SolarIconSize.Hero -> 60.dp
    }
    val iconScale = when (size) {
        SolarIconSize.Metric -> 0.66f
        SolarIconSize.PowerFlow -> 0.82f
        SolarIconSize.Hero -> 0.78f
    }

    if (showBadge && size == SolarIconSize.Metric) {
        SolarIconBadge(
            tint = tint,
            size = badgeSize,
            modifier = modifier,
        ) {
            SolarSystemIconCanvas(type = type, tint = tint, scale = iconScale)
        }
    } else {
        Box(
            modifier = modifier
                .size(badgeSize)
                .solarIconGlow(tint),
            contentAlignment = Alignment.Center,
        ) {
            SolarSystemIconCanvas(type = type, tint = tint, scale = iconScale)
        }
    }
}

/** Soft rounded tile for metric and panel cards. */
@Composable
private fun SolarIconBadge(
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val cornerRadius = 14.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SolarGlass.BadgeBackground.copy(alpha = SolarGlass.ChipSurfaceAlpha))
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun Modifier.solarIconGlow(tint: Color): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tint.copy(alpha = 0.18f),
                tint.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = center,
            radius = size.minDimension * 0.75f,
        ),
        radius = size.minDimension * 0.75f,
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

// --- Shared helpers --------------------------------------------------------

/** A subtle sheen so flat strokes read as polished metal/neon. */
private fun sheen(tint: Color): Brush =
    Brush.linearGradient(listOf(lerp(tint, Color.White, 0.4f), tint))

private fun lerp(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    start.x + (end.x - start.x) * fraction,
    start.y + (end.y - start.y) * fraction,
)

// --- Icons -----------------------------------------------------------------

private fun DrawScope.drawSolarIcon(bounds: Rect) {
    val w = bounds.width
    val h = bounds.height

    // Sun (upper-left) with rays.
    val sunCenter = Offset(bounds.left + w * 0.31f, bounds.top + h * 0.26f)
    val sunRadius = w * 0.125f
    val rayInner = sunRadius * 1.4f
    val rayOuter = sunRadius * 2.05f
    repeat(8) { index ->
        val angle = index * (PI / 4.0) - (PI / 2.0)
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000))),
            start = Offset(
                sunCenter.x + (rayInner * cos(angle)).toFloat(),
                sunCenter.y + (rayInner * sin(angle)).toFloat(),
            ),
            end = Offset(
                sunCenter.x + (rayOuter * cos(angle)).toFloat(),
                sunCenter.y + (rayOuter * sin(angle)).toFloat(),
            ),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFB300)),
            center = sunCenter,
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = sunCenter,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = sunRadius * 0.34f,
        center = Offset(sunCenter.x - sunRadius * 0.28f, sunCenter.y - sunRadius * 0.28f),
    )

    // Solar panel (filled parallelogram with grid + frame).
    val tl = Offset(bounds.left + w * 0.15f, bounds.top + h * 0.44f)
    val tr = Offset(bounds.right - w * 0.05f, bounds.top + h * 0.37f)
    val br = Offset(bounds.right - w * 0.01f, bounds.top + h * 0.73f)
    val bl = Offset(bounds.left + w * 0.10f, bounds.top + h * 0.80f)
    val panel = Path().apply {
        moveTo(tl.x, tl.y)
        lineTo(tr.x, tr.y)
        lineTo(br.x, br.y)
        lineTo(bl.x, bl.y)
        close()
    }
    drawPath(
        path = panel,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4FC3F7), Color(0xFF1565C0)),
            startY = tr.y,
            endY = br.y,
        ),
    )
    listOf(0.25f, 0.5f, 0.75f).forEach { f ->
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = lerp(tl, tr, f),
            end = lerp(bl, br, f),
            strokeWidth = w * 0.018f,
        )
    }
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = lerp(tl, bl, 0.5f),
        end = lerp(tr, br, 0.5f),
        strokeWidth = w * 0.018f,
    )
    drawPath(
        path = panel,
        color = Color(0xFF0D47A1),
        style = Stroke(width = w * 0.03f, join = StrokeJoin.Round),
    )

    // Stand / pole.
    val standCx = (bl.x + br.x) / 2f
    val standTop = maxOf(bl.y, br.y) + h * 0.01f
    drawLine(
        brush = sheen(Color(0xFF90A4AE)),
        start = Offset(standCx, standTop),
        end = Offset(standCx, bounds.bottom),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF607D8B),
        start = Offset(standCx - w * 0.12f, bounds.bottom),
        end = Offset(standCx + w * 0.12f, bounds.bottom),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBatteryIcon(bounds: Rect, tint: Color) {
    val bodyWidth = bounds.width * 0.5f
    val bodyHeight = bounds.height * 0.72f
    val left = bounds.center.x - bodyWidth / 2f
    val top = bounds.top + bounds.height * 0.14f
    val corner = bounds.width * 0.1f
    val stroke = bounds.width * 0.058f

    // Terminal cap.
    val capWidth = bodyWidth * 0.42f
    val capHeight = bounds.height * 0.07f
    drawRoundRect(
        brush = sheen(tint),
        topLeft = Offset(bounds.center.x - capWidth / 2f, top - capHeight * 0.9f),
        size = Size(capWidth, capHeight),
        cornerRadius = CornerRadius(capHeight * 0.4f),
    )

    // Body fill + frame.
    drawRoundRect(
        color = tint.copy(alpha = 0.12f),
        topLeft = Offset(left, top),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(corner),
    )
    // Charge level (gradient) anchored to the bottom.
    val inset = stroke * 0.9f
    val fillH = (bodyHeight - inset * 2f) * 0.66f
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(lerp(tint, Color.White, 0.35f), tint),
        ),
        topLeft = Offset(left + inset, top + bodyHeight - inset - fillH),
        size = Size(bodyWidth - inset * 2f, fillH),
        cornerRadius = CornerRadius(corner * 0.5f),
    )
    drawRoundRect(
        brush = sheen(tint),
        topLeft = Offset(left, top),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = stroke),
    )

    // Lightning bolt.
    val cx = bounds.center.x
    val bolt = Path().apply {
        moveTo(cx + bodyWidth * 0.10f, top + bodyHeight * 0.20f)
        lineTo(cx - bodyWidth * 0.16f, top + bodyHeight * 0.56f)
        lineTo(cx - bodyWidth * 0.01f, top + bodyHeight * 0.56f)
        lineTo(cx - bodyWidth * 0.10f, top + bodyHeight * 0.84f)
        lineTo(cx + bodyWidth * 0.18f, top + bodyHeight * 0.44f)
        lineTo(cx + bodyWidth * 0.02f, top + bodyHeight * 0.44f)
        close()
    }
    drawPath(path = bolt, color = Color.White)
}

private fun DrawScope.drawLoadIcon(bounds: Rect, tint: Color) {
    val houseWidth = bounds.width * 0.7f
    val houseHeight = bounds.height * 0.5f
    val left = bounds.center.x - houseWidth / 2f
    val baseTop = bounds.top + bounds.height * 0.44f
    val cx = bounds.center.x
    val corner = bounds.width * 0.05f

    // Roof.
    val roof = Path().apply {
        moveTo(left - houseWidth * 0.08f, baseTop + houseHeight * 0.04f)
        lineTo(cx, bounds.top + bounds.height * 0.12f)
        lineTo(left + houseWidth + houseWidth * 0.08f, baseTop + houseHeight * 0.04f)
        close()
    }
    drawPath(path = roof, brush = sheen(tint))

    // Body.
    drawRoundRect(
        color = tint.copy(alpha = 0.16f),
        topLeft = Offset(left, baseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(corner),
    )
    drawRoundRect(
        brush = sheen(tint),
        topLeft = Offset(left, baseTop),
        size = Size(houseWidth, houseHeight),
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = bounds.width * 0.05f),
    )

    // Door.
    val doorWidth = houseWidth * 0.24f
    val doorHeight = houseHeight * 0.46f
    drawRoundRect(
        brush = sheen(tint),
        topLeft = Offset(cx - doorWidth / 2f, baseTop + houseHeight - doorHeight),
        size = Size(doorWidth, doorHeight),
        cornerRadius = CornerRadius(doorWidth * 0.18f, doorWidth * 0.18f),
    )
    drawCircle(
        color = Color.White,
        radius = bounds.width * 0.012f,
        center = Offset(cx + doorWidth * 0.28f, baseTop + houseHeight - doorHeight * 0.5f),
    )

    // Windows.
    val winSize = houseWidth * 0.2f
    val winY = baseTop + houseHeight * 0.18f
    listOf(left + houseWidth * 0.13f, left + houseWidth * 0.67f).forEach { wx ->
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(wx, winY),
            size = Size(winSize, winSize),
            cornerRadius = CornerRadius(winSize * 0.12f),
        )
        drawLine(
            color = tint,
            start = Offset(wx + winSize / 2f, winY),
            end = Offset(wx + winSize / 2f, winY + winSize),
            strokeWidth = bounds.width * 0.014f,
        )
        drawLine(
            color = tint,
            start = Offset(wx, winY + winSize / 2f),
            end = Offset(wx + winSize, winY + winSize / 2f),
            strokeWidth = bounds.width * 0.014f,
        )
    }
}

private fun DrawScope.drawGridIcon(bounds: Rect, tint: Color) {
    val w = bounds.width
    val h = bounds.height
    val cx = bounds.center.x
    val stroke = w * 0.04f
    val brush = sheen(tint)

    fun line(a: Offset, b: Offset, weight: Float = 1f) {
        drawLine(
            brush = brush,
            start = a,
            end = b,
            strokeWidth = stroke * weight,
            cap = StrokeCap.Round,
        )
    }

    val baseY = bounds.top + h * 0.94f
    val topY = bounds.top + h * 0.18f
    val tipY = bounds.top + h * 0.05f
    val baseHalf = w * 0.27f
    val topHalf = w * 0.085f

    fun halfAt(y: Float): Float {
        val t = ((y - baseY) / (topY - baseY)).coerceIn(0f, 1f)
        return baseHalf + (topHalf - baseHalf) * t
    }
    fun leftAt(y: Float) = Offset(cx - halfAt(y), y)
    fun rightAt(y: Float) = Offset(cx + halfAt(y), y)

    // Legs + tip.
    line(leftAt(baseY), leftAt(topY))
    line(rightAt(baseY), rightAt(topY))
    line(leftAt(topY), Offset(cx, tipY))
    line(rightAt(topY), Offset(cx, tipY))

    // Horizontal belts + X braces between section levels.
    val levels = listOf(baseY, bounds.top + h * 0.72f, bounds.top + h * 0.52f, topY)
    levels.forEach { y -> line(leftAt(y), rightAt(y), weight = 0.85f) }
    for (i in 0 until levels.size - 1) {
        val y0 = levels[i]
        val y1 = levels[i + 1]
        line(leftAt(y0), rightAt(y1), weight = 0.7f)
        line(rightAt(y0), leftAt(y1), weight = 0.7f)
    }

    // Cross arms with insulators.
    val arms = listOf(
        Triple(bounds.top + h * 0.30f, w * 0.34f, w * 0.05f),
        Triple(bounds.top + h * 0.46f, w * 0.40f, w * 0.06f),
    )
    arms.forEach { (y, half, hook) ->
        line(Offset(cx - half, y), Offset(cx + half, y))
        line(Offset(cx - half, y), Offset(cx - half - hook * 0.6f, y + hook))
        line(Offset(cx + half, y), Offset(cx + half + hook * 0.6f, y + hook))
        drawCircle(color = tint, radius = w * 0.024f, center = Offset(cx - half, y))
        drawCircle(color = tint, radius = w * 0.024f, center = Offset(cx + half, y))
    }
}

private fun DrawScope.drawVoltageIcon(bounds: Rect, tint: Color) {
    val center = bounds.center
    val arcHalfWidth = bounds.width * 0.34f
    val arcHalfHeight = bounds.width * 0.34f
    val arcRect = Rect(
        left = center.x - arcHalfWidth,
        top = center.y - arcHalfHeight,
        right = center.x + arcHalfWidth,
        bottom = center.y + arcHalfHeight,
    )
    val radiusX = arcRect.width / 2f
    val radiusY = arcRect.height / 2f
    val startAngle = 155f
    val sweepAngle = 230f
    val outerStroke = bounds.width * 0.034f

    drawArc(
        color = tint.copy(alpha = 0.22f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStroke * 1.9f, cap = StrokeCap.Round),
    )
    drawArc(
        brush = sheen(tint),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
    )

    // Tick marks around the dial.
    val tickCount = 5
    repeat(tickCount) { i ->
        val a = (startAngle + sweepAngle * i / (tickCount - 1)) * PI / 180.0
        val outer = Offset(
            center.x + (radiusX * 0.92f * cos(a)).toFloat(),
            center.y + (radiusY * 0.92f * sin(a)).toFloat(),
        )
        val inner = Offset(
            center.x + (radiusX * 0.72f * cos(a)).toFloat(),
            center.y + (radiusY * 0.72f * sin(a)).toFloat(),
        )
        drawLine(color = tint.copy(alpha = 0.7f), start = inner, end = outer, strokeWidth = bounds.width * 0.02f, cap = StrokeCap.Round)
    }

    // Needle.
    val needleRad = 328f * PI / 180.0
    val needleLength = minOf(radiusX, radiusY) * 0.74f
    val needleTip = Offset(
        center.x + (needleLength * cos(needleRad)).toFloat(),
        center.y + (needleLength * sin(needleRad)).toFloat(),
    )
    val baseHalf = outerStroke * 0.6f
    val perpX = (-sin(needleRad) * baseHalf).toFloat()
    val perpY = (cos(needleRad) * baseHalf).toFloat()
    val needlePath = Path().apply {
        moveTo(center.x + perpX, center.y + perpY)
        lineTo(needleTip.x, needleTip.y)
        lineTo(center.x - perpX, center.y - perpY)
        close()
    }
    drawPath(path = needlePath, brush = sheen(tint))
    drawCircle(color = tint.copy(alpha = 0.22f), radius = outerStroke * 1.5f, center = center)
    drawCircle(color = tint, radius = outerStroke * 0.9f, center = center)
}

private fun DrawScope.drawCurrentIcon(bounds: Rect, tint: Color) {
    val radius = bounds.width * 0.38f
    val center = bounds.center

    drawCircle(color = tint.copy(alpha = 0.14f), radius = radius * 1.1f, center = center)
    drawCircle(
        brush = sheen(tint),
        radius = radius,
        center = center,
        style = Stroke(width = bounds.width * 0.045f),
    )

    fun wave(amplitudeScale: Float): Path {
        val path = Path()
        val startX = center.x - radius * 0.74f
        val endX = center.x + radius * 0.74f
        val steps = 40
        val step = (endX - startX) / steps
        for (i in 0..steps) {
            val x = startX + step * i
            val y = center.y + sin(i * 2.0 * PI / steps * 1.5).toFloat() * radius * amplitudeScale
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    // Faint trailing wave for depth.
    drawPath(
        path = wave(0.46f),
        color = tint.copy(alpha = 0.3f),
        style = Stroke(width = bounds.width * 0.055f, cap = StrokeCap.Round),
    )
    drawPath(
        path = wave(0.42f),
        brush = sheen(tint),
        style = Stroke(width = bounds.width * 0.05f, cap = StrokeCap.Round),
    )
}
