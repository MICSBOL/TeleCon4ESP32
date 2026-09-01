package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay

private val PlotNeon = Color(0xFF22FF66)
private const val PlotSampleCount = 72
private const val PlotSampleIntervalMs = 80L
private const val ExampleSineMidKmh = 20f
private const val ExampleSineAmplitudeKmh = 14f
private const val ExampleSineCycles = 1.25f
private const val ExampleSinePhaseStep = 0.14f

@Composable
internal fun rememberTelemetryPlotSamples(
    speedKmh: Float,
    fromTelemetry: Boolean = false,
): List<Float> {
    val latestSpeed by rememberUpdatedState(speedKmh)
    val latestFromTelemetry by rememberUpdatedState(fromTelemetry)
    val liveSamples = remember { mutableStateListOf<Float>() }
    var collectingLive by remember { mutableStateOf(false) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (latestFromTelemetry) {
                collectingLive = true
                liveSamples.add(latestSpeed)
                while (liveSamples.size > PlotSampleCount) {
                    liveSamples.removeAt(0)
                }
            } else {
                collectingLive = false
                phase += ExampleSinePhaseStep
            }
            delay(PlotSampleIntervalMs)
        }
    }

    if (collectingLive) return liveSamples
    return exampleSineSamples(phase)
}

private fun exampleSineSamples(phase: Float): List<Float> {
    val last = (PlotSampleCount - 1).coerceAtLeast(1)
    return List(PlotSampleCount) { index ->
        val t = (2f * PI.toFloat() * ExampleSineCycles * index / last) + phase
        ExampleSineMidKmh + ExampleSineAmplitudeKmh * sin(t)
    }
}

@Composable
fun RcTelemetryPlotPanel(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    plotWidth: Dp = RcVehicleProLayout.TelemetryPlotWidth,
    plotHeight: Dp = RcVehicleProLayout.TelemetryPlotHeight,
) {
    RcHudCollapsibleToEdge(
        towardEnd = false,
        showContentDescription = stringResource(R.string.rc_vehicle_hud_show_telemetry_plot),
        hideContentDescription = stringResource(R.string.rc_vehicle_hud_hide_telemetry_plot),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        fillWidth = false,
        swipeEntireContent = true,
    ) {
        RcGlassCard(
            surfaceAlpha = RcVehicleProGlass.PLOT_BACKGROUND_ALPHA,
            accentEdge = RcGlassAccentEdge.END,
            contentPadding = PaddingValues(
                start = 2.dp,
                top = 10.dp,
                end = 6.dp,
                bottom = 10.dp,
            ),
            fillWidth = false,
        ) {
            val plotDescription = stringResource(R.string.rc_vehicle_telemetry_plot)
            RcTelemetryScopePlot(
                samples = samples,
                modifier = Modifier
                    .size(
                        plotWidth,
                        plotHeight,
                    )
                    .semantics { contentDescription = plotDescription },
            )
        }
    }
}

@Composable
private fun RcTelemetryScopePlot(
    samples: List<Float>,
    modifier: Modifier = Modifier,
) {
    val sampleCount = samples.size
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val yLabelGutter = 16.dp.toPx()
        val xLabelGutter = 16.dp.toPx()
        val dataPad = 10.dp.toPx()
        val plotLeft = yLabelGutter
        val plotRight = width - 4.dp.toPx()
        val plotTop = dataPad
        val plotBottom = height - xLabelGutter
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
        val peak = samples.maxOrNull()?.coerceAtLeast(0.1f) ?: 0.1f
        val timeMaxSec = ((sampleCount - 1).coerceAtLeast(1)) * (PlotSampleIntervalMs / 1000f)
        val horizonY = height * (3f / 4f)
        val vanish = Offset(width * 0.50f, horizonY)

        val radialCount = 22
        val halfSpread = width * 2.15f
        val radialHalf = radialCount / 2f
        for (i in 0..radialCount) {
            val sideT = abs(i - radialHalf) / radialHalf
            val xBottom = vanish.x + (i - radialHalf) / radialHalf * halfSpread
            drawLine(
                color = PlotNeon.copy(alpha = 0.16f + 0.18f * (1f - sideT)),
                start = vanish,
                end = Offset(xBottom, height + 8f),
                strokeWidth = 1.2f,
            )
        }

        val depthCount = 14
        for (i in 1..depthCount) {
            val persp = (i.toFloat() / depthCount).pow(1.85f)
            val y = horizonY + (height - horizonY) * persp
            drawLine(
                color = PlotNeon.copy(alpha = 0.12f + 0.22f * (1f - persp)),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.15f,
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                startY = horizonY,
                endY = height,
            ),
        )

        fun dataX(index: Int, lastIndex: Int): Float {
            return if (lastIndex <= 0) plotLeft else plotLeft + plotWidth * index / lastIndex
        }

        fun dataY(value: Float): Float {
            val t = (value / peak).coerceIn(0f, 1f)
            return plotTop + plotHeight * (1f - t)
        }

        fun drawAxesAndTicks() {
            val axisColor = PlotNeon.copy(alpha = 0.85f)
            val tickColor = PlotNeon.copy(alpha = 0.70f)
            val axisStroke = 1.5f
            val tickLen = 4.dp.toPx()
            drawLine(
                color = axisColor,
                start = Offset(plotLeft, plotTop),
                end = Offset(plotLeft, plotBottom),
                strokeWidth = axisStroke,
            )
            drawLine(
                color = axisColor,
                start = Offset(plotLeft, plotBottom),
                end = Offset(plotRight, plotBottom),
                strokeWidth = axisStroke,
            )
            val yTicks = axisTicks(peak)
            val xTicks = axisTicks(timeMaxSec)
            yTicks.forEach { value ->
                val y = dataY(value)
                drawLine(
                    color = tickColor,
                    start = Offset(plotLeft, y),
                    end = Offset(plotLeft + tickLen, y),
                    strokeWidth = 1.2f,
                )
            }
            xTicks.forEach { seconds ->
                val x = plotLeft + plotWidth * (seconds / timeMaxSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                drawLine(
                    color = tickColor,
                    start = Offset(x, plotBottom),
                    end = Offset(x, plotBottom - tickLen),
                    strokeWidth = 1.2f,
                )
            }
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val labelPaint = android.graphics.Paint().apply {
                    color = PlotNeon.toArgb()
                    textSize = 8.dp.toPx()
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                labelPaint.textAlign = android.graphics.Paint.Align.RIGHT
                yTicks.forEach { value ->
                    val y = dataY(value)
                    native.drawText(
                        formatAxisTick(value),
                        plotLeft - 2.dp.toPx(),
                        y + labelPaint.textSize * 0.35f,
                        labelPaint,
                    )
                }
                labelPaint.textAlign = android.graphics.Paint.Align.CENTER
                xTicks.forEach { seconds ->
                    val x = plotLeft + plotWidth * (seconds / timeMaxSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                    native.drawText(
                        formatAxisTick(seconds),
                        x,
                        plotBottom + 11.dp.toPx(),
                        labelPaint,
                    )
                }
            }
        }

        if (sampleCount == 0) {
            drawAxesAndTicks()
            return@Canvas
        }

        val lastIndex = (sampleCount - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()
        samples.forEachIndexed { index, value ->
            val x = dataX(index, lastIndex)
            val y = dataY(value)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, plotBottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(dataX(samples.lastIndex.coerceAtLeast(0), lastIndex), plotBottom)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PlotNeon.copy(alpha = 0.22f),
                    PlotNeon.copy(alpha = 0.03f),
                ),
                startY = plotTop,
                endY = plotBottom,
            ),
        )
        drawAxesAndTicks()
        drawPath(
            path = linePath,
            color = PlotNeon.copy(alpha = 0.16f),
            style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = linePath,
            color = PlotNeon.copy(alpha = 0.40f),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = linePath,
            color = PlotNeon,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun axisTicks(max: Float, targetCount: Int = 4): List<Float> {
    if (max <= 0f) return listOf(0f)
    val raw = max / targetCount.toFloat()
    val mag = 10f.pow(floor(log10(raw.toDouble())).toFloat())
    val residual = raw / mag
    val step = when {
        residual <= 1f -> 1f
        residual <= 2f -> 2f
        residual <= 5f -> 5f
        else -> 10f
    } * mag
    val ticks = mutableListOf(0f)
    var value = step
    while (value < max - step * 0.2f) {
        ticks.add(value)
        value += step
    }
    if (ticks.last() != max && max - ticks.last() > step * 0.75f) {
        ticks.add(max)
    }
    return ticks
}

private fun formatAxisTick(value: Float): String {
    return if (abs(value) < 0.0001f) {
        "0"
    } else if (abs(value - value.toInt()) < 0.05f) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}
