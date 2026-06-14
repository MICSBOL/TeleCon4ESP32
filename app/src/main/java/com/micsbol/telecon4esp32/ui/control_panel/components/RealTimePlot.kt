package com.micsbol.telecon4esp32.ui.control_panel.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import kotlin.math.sin

private const val MAX_VISIBLE_PLOT_POINTS = 100

@Composable
fun RealTimePlot(
    modifier: Modifier = Modifier,
    series: List<PlotData>,
    plotRevision: Long = 0L,
    gridColor: Color = Color.White.copy(alpha = 0.2f),
) {

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val paths by remember(series, plotRevision, canvasSize) {
        derivedStateOf {
            if (canvasSize == Size.Zero) {
                emptyList()
            } else {
                series.map { plotData ->
                    buildPlotPath(
                        points = plotData.dataPoints.takeLast(MAX_VISIBLE_PLOT_POINTS),
                        canvasSize = canvasSize,
                    )
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .padding(top = 8.dp)
            .padding(2.dp)
            .onGloballyPositioned { layoutCoordinates ->
                canvasSize = layoutCoordinates.size.toSize()
            }
    ) {
        val maxLineColor = Color.White.copy(alpha = 0.4f)
        val topPadding = 1.dp.toPx()
        val topY = topPadding
        drawLine(
            color = maxLineColor,
            start = Offset(x = 0f, y = topY),
            end = Offset(x = size.width, y = topY),
            strokeWidth = 1.5.dp.toPx()
        )
        val horizontalSubdivisions = 8
        val drawableHeight = size.height - topPadding
        val horizontalSpacing = drawableHeight / horizontalSubdivisions
        for (i in 1..horizontalSubdivisions) {
            val y = topY + (i * horizontalSpacing)
            drawLine(
                color = if(i == horizontalSubdivisions / 2) Color.Cyan else gridColor,
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width, y = y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val verticalLineCount = 12
        val verticalSpacing = size.width / verticalLineCount
        for (i in 0..verticalLineCount) {
            drawLine(
                color = if(i == verticalLineCount / 2) Color.Cyan else gridColor,
                start = Offset(x = i * verticalSpacing, y = 0f),
                end = Offset(x = i * verticalSpacing, y = size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        paths.forEachIndexed { index, path ->
            val lineColor = series.getOrNull(index)?.colorArgb?.let { Color(it) } ?: Color.White
            drawGlowPath(
                path = path,
                color = lineColor,
                coreWidth = 2.dp.toPx(),
                glowWidth = 6.dp.toPx(),
                glowBlur = 14f
            )
        }
    }
}

private fun buildPlotPath(points: List<Float>, canvasSize: Size): Path {
    if (points.isEmpty()) return Path()

    return when (points.size) {
        1 -> {
            val y = canvasSize.height - (points[0].coerceIn(0f, 1f) * canvasSize.height)
            Path().apply {
                moveTo(0f, y)
                lineTo(canvasSize.width, y)
            }
        }
        else -> Path().apply {
            val stepX = canvasSize.width / (points.size - 1).toFloat()
            moveTo(
                x = 0f,
                y = canvasSize.height - (points[0].coerceIn(0f, 1f) * canvasSize.height),
            )
            for (i in 1 until points.size) {
                val x = i * stepX
                val y = canvasSize.height - (points[i].coerceIn(0f, 1f) * canvasSize.height)
                lineTo(x, y)
            }
        }
    }
}

private fun DrawScope.drawGlowPath(
    path: Path,
    color: Color,
    coreWidth: Float,
    glowWidth: Float,
    glowBlur: Float,
) {
    // Soft outer blur to emulate light-emitter trails.
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeWidth = glowWidth
            this.color = color.copy(alpha = 0.45f).toArgb()
            maskFilter = BlurMaskFilter(glowBlur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
    }

    // Crisp core line above the glow.
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = coreWidth)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RealTimePlotMultiLinePreview() {
    // Create sample data for two different sine waves
    val sampleData1 = remember {
        (0..100).map { (sin(it * 0.1f) * 0.4f) + 0.6f }
    }
    val sampleData2 = remember {
        (0..100).map { (sin(it * 0.2f) * 0.3f) + 0.25f }
    }

    RealTimePlot(
        modifier = Modifier
            .height(200.dp)
            .width(400.dp),
        series = listOf(
            PlotData(dataPoints = sampleData1, colorArgb = 0xFF00FFFF.toInt()),
            PlotData(dataPoints = sampleData2, colorArgb = 0xFFFF0000.toInt())
        )
    )
}
