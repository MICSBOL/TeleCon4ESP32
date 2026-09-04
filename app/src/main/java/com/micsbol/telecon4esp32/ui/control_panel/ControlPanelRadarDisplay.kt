package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private val RadarCanvasPadding = 8.dp

/**
 * Sweeping radar pane for Control Panel center. The scan sector is 180° or 270°,
 * centered on north, drawn as a holographic disc hovering above a perspective
 * floor and viewed from front-upper. Servo angle from ESP32 plot data sits at
 * the center of the colored beam; 0 and 1 are the left and right extremes.
 */
@Composable
fun ControlPanelRadarDisplay(
    series: List<PlotData>,
    modifier: Modifier = Modifier,
    settings: RadarDisplaySettings = RadarDisplaySettings(),
    onSettingsChange: (RadarDisplaySettings) -> Unit = {},
    accent: Color = brandPrimary(),
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var idleProgress by remember { mutableFloatStateOf(0.5f) }
    var idleForward by remember { mutableStateOf(true) }

    val span = settings.scanSpan.degrees
    val angleIndex = radarSeriesIndex(settings.angleSeriesIndex)
    val rangeIndex = radarSeriesIndex(settings.rangeSeriesIndex)
    val servoSample = series.getOrNull(angleIndex)?.dataPoints?.lastOrNull()
    val hasServoData = servoSample != null

    LaunchedEffect(hasServoData, span) {
        if (hasServoData) return@LaunchedEffect
        while (true) {
            delay(16L)
            val step = 16f / 2400f
            val next = idleProgress + if (idleForward) step else -step
            when {
                next >= 1f -> {
                    idleProgress = 1f
                    idleForward = false
                }
                next <= 0f -> {
                    idleProgress = 0f
                    idleForward = true
                }
                else -> idleProgress = next
            }
        }
    }

    val bearingFromNorth = if (servoSample != null) {
        radarBearingFromServoSample(servoSample, span)
    } else {
        -span / 2f + idleProgress * span
    }
    val persistence = remember { RadarPointPersistence() }
    LaunchedEffect(span, angleIndex, rangeIndex) {
        persistence.clear()
    }
    LaunchedEffect(settings.keepLastPoints) {
        if (!settings.keepLastPoints) persistence.clear()
    }

    val liveBlips = remember(series, span, settings.showHistory, settings.keepLastPoints, angleIndex, rangeIndex) {
        radarScanBlips(
            series = series,
            spanDegrees = span,
            angleSeriesIndex = angleIndex,
            rangeSeriesIndex = rangeIndex,
            showHistory = settings.keepLastPoints || settings.showHistory,
        )
    }
    val blips = remember(liveBlips, settings.keepLastPoints) {
        if (!settings.keepLastPoints) {
            liveBlips
        } else {
            liveBlips.forEach { blip ->
                persistence.ingest(
                    bearingFromNorth = blip.bearingFromNorth,
                    rangeFraction = blip.rangeFraction,
                    colorArgb = blip.color.toArgb(),
                )
            }
            persistence.points().map { point ->
                RadarBlip(
                    bearingFromNorth = point.bearingFromNorth,
                    rangeFraction = point.rangeFraction,
                    color = Color(point.colorArgb),
                    alpha = 1f,
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadarCanvasPadding),
        ) {
            val view = computeRadarViewProjection(
                width = size.width,
                height = size.height,
                spanDegrees = span,
                padding = 0f,
            )
            val origin = Offset(view.originX, view.originY)
            val sectorPath = view.sectorPath(span)
            val grid = accent.copy(alpha = 0.42f)
            val ringStroke = Stroke(width = 1.35f, join = StrokeJoin.Round)
            val ringCount = settings.rangeRingCount.coerceIn(
                RadarDisplaySettings.MIN_RANGE_RINGS,
                RadarDisplaySettings.MAX_RANGE_RINGS,
            )

            if (settings.showGrid) {
                drawRadarAirGrid(view, accent)
            }
            drawPath(
                path = view.floorShadowPath(span),
                color = Color.Black.copy(alpha = 0.22f),
            )
            drawPath(
                path = sectorPath,
                color = accent.copy(alpha = 0.12f),
                style = Stroke(width = 16f, join = StrokeJoin.Round),
            )
            drawPath(
                path = sectorPath,
                color = accent.copy(alpha = 0.20f),
                style = Stroke(width = 7f, join = StrokeJoin.Round),
            )

            clipPath(sectorPath) {
                drawPath(sectorPath, color = accent.copy(alpha = 0.05f))

                val trailWidth = (settings.beamWidth.halfAngleDegrees * 2f).coerceAtMost(span)
                if (settings.showSweepTrail) {
                    drawPath(
                        path = view.beamPath(bearingFromNorth, trailWidth),
                        color = accent.copy(alpha = 0.28f),
                    )
                }
                drawLine(
                    color = accent.copy(alpha = 1f),
                    start = origin,
                    end = view.offset(bearingFromNorth, 1f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Butt,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                )

                blips.forEach { blip ->
                    val center = view.offset(blip.bearingFromNorth, blip.rangeFraction)
                    val depth = view.depthScale(blip.bearingFromNorth, blip.rangeFraction)
                    drawCircle(
                        color = blip.color.copy(alpha = 0.25f * blip.alpha),
                        radius = 12f * depth,
                        center = center,
                    )
                    drawCircle(
                        color = blip.color.copy(alpha = 0.9f * blip.alpha),
                        radius = 5f * depth,
                        center = center,
                    )
                }
            }

            if (settings.showGrid) {
                (1..ringCount).forEach { ring ->
                    drawPath(
                        path = view.arcPath(span, ring / ringCount.toFloat()),
                        color = grid,
                        style = ringStroke,
                    )
                }
                radarGridBearingsFromNorth(span).forEach { fromNorth ->
                    drawLine(
                        color = grid,
                        start = origin,
                        end = view.offset(fromNorth, 1f),
                        strokeWidth = 1.35f,
                    )
                }
            }

            drawPath(
                path = sectorPath,
                color = accent.copy(alpha = 0.78f),
                style = Stroke(width = 2f, join = StrokeJoin.Round),
            )
        }

        RadarSettingsChipLauncher(
            spanDegrees = span.roundToInt(),
            accent = accent,
            onClick = { showSettings = true },
            modifier = Modifier.align(Alignment.TopEnd),
        )

        if (series.isEmpty() || series.all { it.dataPoints.isEmpty() }) {
            Text(
                text = stringResource(R.string.control_panel_radar_waiting),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 36.dp, end = 36.dp),
            )
        }
    }

    if (showSettings) {
        ControlPanelRadarSettingsDialog(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun RadarSettingsChipLauncher(
    spanDegrees: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = brandPrimary(),
) {
    val shape = RoundedCornerShape(12.dp)
    val openLabel = stringResource(R.string.control_panel_radar_settings_content_description)

    Row(
        modifier = modifier
            .padding(6.dp)
            .clip(shape)
            .background(Color(0xE6121824))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = openLabel,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.control_panel_radar_scan_range_degrees, spanDegrees),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
    }
}

private data class RadarBlip(
    val bearingFromNorth: Float,
    val rangeFraction: Float,
    val color: Color,
    val alpha: Float,
)

private fun radarScanBlips(
    series: List<PlotData>,
    spanDegrees: Float,
    angleSeriesIndex: Int,
    rangeSeriesIndex: Int,
    showHistory: Boolean,
): List<RadarBlip> {
    val angleSeries = series.getOrNull(radarSeriesIndex(angleSeriesIndex)) ?: return emptyList()
    val rangeSeries = series.getOrNull(radarSeriesIndex(rangeSeriesIndex)) ?: angleSeries
    val historyLimit = if (showHistory) RadarDisplaySettings.HISTORY_SAMPLE_COUNT else 1
    val angles = angleSeries.dataPoints.takeLast(historyLimit)
    val ranges = rangeSeries.dataPoints.takeLast(historyLimit)
    if (angles.isEmpty()) return emptyList()
    val count = minOf(angles.size, ranges.size.coerceAtLeast(1))
    val color = Color(rangeSeries.colorArgb)
    return List(count) { i ->
        val angleSample = angles.getOrElse(i) { angles.last() }
        val rangeSample = ranges.getOrElse(i) { ranges.lastOrNull() ?: 0.5f }
        val age = if (count == 1) 1f else (i + 1f) / count
        RadarBlip(
            bearingFromNorth = radarBearingFromServoSample(angleSample, spanDegrees),
            rangeFraction = radarRangeFraction(rangeSample),
            color = color,
            alpha = 0.28f + 0.72f * age,
        )
    }
}

private fun RadarViewProjection.offset(
    bearingFromNorth: Float,
    rangeFraction: Float,
): Offset {
    val point = project(bearingFromNorth, rangeFraction)
    return Offset(point.x, point.y)
}

private fun RadarViewProjection.sectorPath(spanDegrees: Float, steps: Int = 80): Path {
    val half = spanDegrees / 2f
    return Path().apply {
        moveTo(originX, originY)
        for (i in 0..steps) {
            val bearing = -half + spanDegrees * i / steps
            val point = offset(bearing, 1f)
            lineTo(point.x, point.y)
        }
        close()
    }
}

private fun RadarViewProjection.arcPath(
    spanDegrees: Float,
    rangeFraction: Float,
    steps: Int = 80,
): Path {
    val half = spanDegrees / 2f
    return Path().apply {
        for (i in 0..steps) {
            val bearing = -half + spanDegrees * i / steps
            val point = offset(bearing, rangeFraction)
            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }
}

private fun RadarViewProjection.beamPath(
    bearingFromNorth: Float,
    widthDegrees: Float,
    steps: Int = 36,
): Path {
    val half = widthDegrees / 2f
    return Path().apply {
        moveTo(originX, originY)
        for (i in 0..steps) {
            val bearing = bearingFromNorth - half + widthDegrees * i / steps
            val point = offset(bearing, 1f)
            lineTo(point.x, point.y)
        }
        close()
    }
}

private fun RadarViewProjection.floorShadowPath(spanDegrees: Float, steps: Int = 64): Path {
    val half = spanDegrees / 2f
    val origin = projectFloor(0f, 0f)
    return Path().apply {
        moveTo(origin.x, origin.y)
        for (i in 0..steps) {
            val bearing = -half + spanDegrees * i / steps
            val (wx, wz) = radarPlanePoint(bearing, 1f)
            val point = projectFloor(wx, wz)
            lineTo(point.x, point.y)
        }
        close()
    }
}

private fun DrawScope.drawRadarAirGrid(view: RadarViewProjection, accent: Color) {
    val xMin = -2.1f
    val xMax = 2.1f
    val zMin = -0.65f
    val zMax = 1.55f
    val xCount = 12
    val zCount = 11
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f)
    val xHalf = xCount / 2f
    for (i in 0..xCount) {
        val x = xMin + (xMax - xMin) * i / xCount
        val start = view.projectFloor(x, zMin)
        val end = view.projectFloor(x, zMax)
        val edge = abs(i - xHalf) / xHalf
        drawLine(
            color = accent.copy(alpha = 0.10f + 0.16f * (1f - edge)),
            start = Offset(start.x, start.y),
            end = Offset(end.x, end.y),
            strokeWidth = 1.15f,
            pathEffect = dash,
        )
    }
    for (i in 0..zCount) {
        val z = zMin + (zMax - zMin) * i / zCount
        val start = view.projectFloor(xMin, z)
        val end = view.projectFloor(xMax, z)
        val far = i / zCount.toFloat()
        drawLine(
            color = accent.copy(alpha = 0.10f + 0.18f * (1f - far)),
            start = Offset(start.x, start.y),
            end = Offset(end.x, end.y),
            strokeWidth = 1.15f,
            pathEffect = dash,
        )
    }
}
