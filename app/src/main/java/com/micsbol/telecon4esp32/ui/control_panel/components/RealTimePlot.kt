package com.micsbol.telecon4esp32.ui.control_panel.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.PlotDisplayHistories
import com.micsbol.telecon4esp32.domain.model.PlotGraphMode
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.PlotVertex
import com.micsbol.telecon4esp32.domain.model.formatEngineeringNumber
import com.micsbol.telecon4esp32.domain.model.lineVertices
import com.micsbol.telecon4esp32.domain.model.plotScaleTicks
import com.micsbol.telecon4esp32.domain.model.stairVertices
import com.micsbol.telecon4esp32.domain.model.triangleContours
import com.micsbol.telecon4esp32.domain.model.zeroLineNormalized
import kotlin.math.sin

private const val MAX_VISIBLE_PLOT_POINTS = 100

@Composable
fun RealTimePlot(
    modifier: Modifier = Modifier,
    series: List<PlotData>,
    plotRevision: Long = 0L,
    gridColor: Color = Color.White.copy(alpha = 0.2f),
    visible: List<Boolean> = emptyList(),
    graphModes: List<PlotGraphMode> = emptyList(),
    lineStyles: List<PlotLineStyle> = emptyList(),
    onTop: List<Boolean> = emptyList(),
    calibrations: List<PlotCalibration> = emptyList(),
) {

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val histories = remember { PlotDisplayHistories(MAX_VISIBLE_PLOT_POINTS) }
    val displayPoints = remember(series, plotRevision, graphModes) {
        histories.pointsFor(
            sources = series.map { it.dataPoints },
            modes = graphModes,
        )
    }

    val geometries by remember(displayPoints, canvasSize, visible, lineStyles, graphModes) {
        derivedStateOf {
            if (canvasSize == Size.Zero) {
                emptyList()
            } else {
                displayPoints.mapIndexed { index, points ->
                    if (!visible.getOrElse(index) { true }) {
                        PlotDrawGeometry()
                    } else {
                        val onChange = graphModes.getOrElse(index) { PlotGraphMode.CONTINUOUS } ==
                            PlotGraphMode.ON_CHANGE
                        buildPlotGeometry(
                            points = points,
                            canvasSize = canvasSize,
                            style = lineStyles.getOrElse(index) { PlotLineStyle.LINE },
                            rightAlignedSlots = if (onChange) MAX_VISIBLE_PLOT_POINTS else null,
                        )
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .padding(top = 2.dp)
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
        val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
        val gridStroke = 1.dp.toPx()
        val horizontalSubdivisions = 8
        val drawableHeight = size.height - topPadding
        val horizontalSpacing = drawableHeight / horizontalSubdivisions
        for (i in 1..horizontalSubdivisions) {
            val y = topY + (i * horizontalSpacing)
            drawLine(
                color = gridColor,
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width, y = y),
                strokeWidth = gridStroke,
                pathEffect = gridDash,
            )
        }

        val verticalLineCount = 12
        val verticalSpacing = size.width / verticalLineCount
        for (i in 0..verticalLineCount) {
            drawLine(
                color = gridColor,
                start = Offset(x = i * verticalSpacing, y = 0f),
                end = Offset(x = i * verticalSpacing, y = size.height),
                strokeWidth = gridStroke,
                pathEffect = gridDash,
            )
        }

        val zeroYs = linkedSetOf<Int>()
        calibrations.forEachIndexed { index, calibration ->
            if (!visible.getOrElse(index) { true }) return@forEachIndexed
            val fraction = calibration.zeroLineNormalized() ?: return@forEachIndexed
            val y = size.height * (1f - fraction)
            val key = y.toInt()
            if (!zeroYs.add(key)) return@forEachIndexed
            drawLine(
                color = Color.Cyan,
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width, y = y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val drawOrder = geometries.indices.sortedBy { index ->
            if (onTop.getOrElse(index) { false }) 1 else 0
        }
        drawOrder.forEach { index ->
            val geometry = geometries[index]
            val lineColor = series.getOrNull(index)?.colorArgb?.let { Color(it) } ?: Color.White
            val fillPath = geometry.fill
            if (fillPath != null && !fillPath.isEmpty) {
                drawPath(
                    path = fillPath,
                    color = lineColor.copy(alpha = 0.32f),
                    style = Fill,
                )
            }
            if (!geometry.stroke.isEmpty) {
                drawGlowPath(
                    path = geometry.stroke,
                    color = lineColor,
                    coreWidth = 2.dp.toPx(),
                    glowWidth = 6.dp.toPx(),
                    glowBlur = 14f
                )
            }
        }
    }
}

@Composable
internal fun PlotYAxisScale(
    min: Float,
    max: Float,
    color: Color,
    axisOnEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 2.dp),
    ) {
        val (lo, hi) = if (max > min) min to max else min to (min + 1f)
        val span = (hi - lo).coerceAtLeast(1e-6f)
        val axisX = if (axisOnEnd) size.width - 1.dp.toPx() else 1.dp.toPx()
        val tickLen = 3.5.dp.toPx()
        val labelGap = 2.dp.toPx()
        drawLine(
            color = color.copy(alpha = 0.85f),
            start = Offset(axisX, 0f),
            end = Offset(axisX, size.height),
            strokeWidth = 1.2f,
        )
        val ticks = plotScaleTicks(lo, hi)
        ticks.forEach { value ->
            val t = ((value - lo) / span).coerceIn(0f, 1f)
            val y = size.height * (1f - t)
            val tickEnd = if (axisOnEnd) axisX - tickLen else axisX + tickLen
            drawLine(
                color = color.copy(alpha = 0.75f),
                start = Offset(axisX, y),
                end = Offset(tickEnd, y),
                strokeWidth = 1.1f,
            )
        }
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val labelPaint = android.graphics.Paint().apply {
                this.color = color.toArgb()
                textSize = 7.dp.toPx()
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = if (axisOnEnd) {
                    android.graphics.Paint.Align.RIGHT
                } else {
                    android.graphics.Paint.Align.LEFT
                }
            }
            val labelX = if (axisOnEnd) axisX - labelGap else axisX + labelGap
            ticks.forEach { value ->
                val t = ((value - lo) / span).coerceIn(0f, 1f)
                val y = size.height * (1f - t)
                val minBaseline = labelPaint.textSize * 0.85f
                val maxBaseline = size.height - labelPaint.textSize * 0.15f
                val baseline = (y + labelPaint.textSize * 0.35f).coerceIn(minBaseline, maxBaseline)
                native.drawText(
                    if (value.isFinite()) formatEngineeringNumber(value) else "0",
                    labelX,
                    baseline,
                    labelPaint,
                )
            }
        }
    }
}

private data class PlotDrawGeometry(
    val stroke: Path = Path(),
    val fill: Path? = null,
)

private fun buildPlotGeometry(
    points: List<Float>,
    canvasSize: Size,
    style: PlotLineStyle,
    rightAlignedSlots: Int?,
): PlotDrawGeometry {
    if (points.isEmpty()) return PlotDrawGeometry()
    return when (style) {
        PlotLineStyle.LINE -> PlotDrawGeometry(
            stroke = verticesToStrokePath(
                lineVertices(
                    values = points,
                    width = canvasSize.width,
                    height = canvasSize.height,
                    rightAlignedSlots = rightAlignedSlots,
                ),
            ),
        )
        PlotLineStyle.STAIR -> PlotDrawGeometry(
            stroke = verticesToStrokePath(
                stairVertices(
                    values = points,
                    width = canvasSize.width,
                    height = canvasSize.height,
                    rightAlignedSlots = rightAlignedSlots,
                ),
            ),
        )
        PlotLineStyle.TRIANGLE -> trianglesToGeometry(
            triangleContours(
                values = points,
                width = canvasSize.width,
                height = canvasSize.height,
                rightAlignedSlots = rightAlignedSlots,
            ),
        )
    }
}

private fun verticesToStrokePath(vertices: List<PlotVertex>): Path {
    if (vertices.isEmpty()) return Path()
    return Path().apply {
        moveTo(vertices[0].x, vertices[0].y)
        for (i in 1 until vertices.size) {
            lineTo(vertices[i].x, vertices[i].y)
        }
    }
}

private fun trianglesToGeometry(triangles: List<List<PlotVertex>>): PlotDrawGeometry {
    if (triangles.isEmpty()) return PlotDrawGeometry()
    val fill = Path()
    val stroke = Path()
    triangles.forEach { triangle ->
        if (triangle.size < 3) return@forEach
        val a = triangle[0]
        val b = triangle[1]
        val c = triangle[2]
        fill.moveTo(a.x, a.y)
        fill.lineTo(b.x, b.y)
        fill.lineTo(c.x, c.y)
        fill.close()
        stroke.moveTo(a.x, a.y)
        stroke.lineTo(b.x, b.y)
        stroke.lineTo(c.x, c.y)
    }
    return PlotDrawGeometry(stroke = stroke, fill = fill)
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
        ),
        calibrations = listOf(PlotCalibration.DEFAULT, PlotCalibration.DEFAULT),
    )
}
