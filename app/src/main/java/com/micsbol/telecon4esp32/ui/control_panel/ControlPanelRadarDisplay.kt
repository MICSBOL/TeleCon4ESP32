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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val RadarCanvasPadding = 8.dp

/**
 * Sweeping radar pane for Control Panel center. The scan sector is 180° or 270°,
 * centered on 12 o'clock. Servo angle from ESP32 plot data sits at the center of the
 * colored beam; 0 and 1 are the left and right extremes of that sector.
 */
@Composable
fun ControlPanelRadarDisplay(
    series: List<PlotData>,
    modifier: Modifier = Modifier,
    settings: RadarDisplaySettings = RadarDisplaySettings(),
    onSettingsChange: (RadarDisplaySettings) -> Unit = {},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var idleProgress by remember { mutableFloatStateOf(0.5f) }
    var idleForward by remember { mutableStateOf(true) }

    val accent = brandPrimary()
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
            val layout = computeRadarSectorLayout(
                width = size.width,
                height = size.height,
                spanDegrees = span,
                padding = 0f,
            )
            val origin = Offset(layout.originX, layout.originY)
            val radius = layout.radius
            val sectorRect = Rect(
                left = origin.x - radius,
                top = origin.y - radius,
                right = origin.x + radius,
                bottom = origin.y + radius,
            )
            val sectorPath = Path().apply {
                moveTo(origin.x, origin.y)
                arcTo(
                    rect = sectorRect,
                    startAngleDegrees = layout.canvasStartAngleDegrees,
                    sweepAngleDegrees = layout.sweepAngleDegrees,
                    forceMoveTo = false,
                )
                close()
            }
            val grid = Color.White.copy(alpha = 0.22f)
            val ringCount = settings.rangeRingCount.coerceIn(
                RadarDisplaySettings.MIN_RANGE_RINGS,
                RadarDisplaySettings.MAX_RANGE_RINGS,
            )

            clipPath(sectorPath) {
                drawPath(sectorPath, color = accent.copy(alpha = 0.08f))

                if (settings.showGrid) {
                    (1..ringCount).forEach { ring ->
                        drawArc(
                            color = grid,
                            startAngle = layout.canvasStartAngleDegrees,
                            sweepAngle = layout.sweepAngleDegrees,
                            useCenter = false,
                            topLeft = Offset(
                                origin.x - radius * ring / ringCount,
                                origin.y - radius * ring / ringCount,
                            ),
                            size = Size(
                                radius * 2f * ring / ringCount,
                                radius * 2f * ring / ringCount,
                            ),
                            style = Stroke(width = 1.2f),
                        )
                    }
                    radarGridBearingsFromNorth(span).forEach { fromNorth ->
                        val canvasRad = Math.toRadians((-90f + fromNorth).toDouble()).toFloat()
                        drawLine(
                            color = grid,
                            start = origin,
                            end = Offset(
                                origin.x + cos(canvasRad) * radius,
                                origin.y + sin(canvasRad) * radius,
                            ),
                            strokeWidth = 1.2f,
                        )
                    }
                }

                val trailWidth = (settings.beamWidth.halfAngleDegrees * 2f).coerceAtMost(span)
                rotate(degrees = bearingFromNorth, pivot = origin) {
                    if (settings.showSweepTrail) {
                        drawArc(
                            color = accent.copy(alpha = 0.48f),
                            startAngle = -90f - trailWidth / 2f,
                            sweepAngle = trailWidth,
                            useCenter = true,
                            topLeft = Offset(origin.x - radius, origin.y - radius),
                            size = Size(radius * 2f, radius * 2f),
                        )
                    }
                    drawLine(
                        color = accent.copy(alpha = 1f),
                        start = origin,
                        end = Offset(origin.x, origin.y - radius),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Butt,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )
                }

                blips.forEach { blip ->
                    val canvasRad = Math.toRadians((-90f + blip.bearingFromNorth).toDouble()).toFloat()
                    val r = radius * blip.rangeFraction
                    val center = Offset(
                        origin.x + cos(canvasRad) * r,
                        origin.y + sin(canvasRad) * r,
                    )
                    drawCircle(
                        color = blip.color.copy(alpha = 0.25f * blip.alpha),
                        radius = 12f,
                        center = center,
                    )
                    drawCircle(
                        color = blip.color.copy(alpha = 0.9f * blip.alpha),
                        radius = 5f,
                        center = center,
                    )
                }
            }

            drawArc(
                color = grid.copy(alpha = 0.45f),
                startAngle = layout.canvasStartAngleDegrees,
                sweepAngle = layout.sweepAngleDegrees,
                useCenter = false,
                topLeft = Offset(origin.x - radius, origin.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 1.8f),
            )
            val leftRad = Math.toRadians(layout.canvasStartAngleDegrees.toDouble()).toFloat()
            val rightRad = Math.toRadians(
                (layout.canvasStartAngleDegrees + layout.sweepAngleDegrees).toDouble(),
            ).toFloat()
            drawLine(
                color = grid.copy(alpha = 0.45f),
                start = origin,
                end = Offset(origin.x + cos(leftRad) * radius, origin.y + sin(leftRad) * radius),
                strokeWidth = 1.8f,
            )
            drawLine(
                color = grid.copy(alpha = 0.45f),
                start = origin,
                end = Offset(origin.x + cos(rightRad) * radius, origin.y + sin(rightRad) * radius),
                strokeWidth = 1.8f,
            )
        }

        RadarSettingsChipLauncher(
            spanDegrees = span.roundToInt(),
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
) {
    val accent = brandPrimary()
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
