package com.example.emitterapp.ui.rc_screen

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.ui.rc_screen.components.AdBanner

@Composable
fun CenterDisplayLedStyle(
    modifier: Modifier = Modifier,
    series: List<PlotData> = emptyList(),
    plotType: PlotType = PlotType.CARTESIAN,
    showAdBanner: Boolean = true,
) {
    val neonMain = Color(0xFF00E5FF)
    val neonAccent = Color(0xFF7C4DFF)
    val background = Color(0xFF050B16)
    val innerBackground = Color(0xFF091226)

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .drawBehind {
                    drawNeonFrame(neonMain = neonMain, neonAccent = neonAccent)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(innerBackground),
                    contentAlignment = Alignment.Center
                ) {
                    when (plotType) {
                        PlotType.CARTESIAN -> {
                            CartesianPlot(
                                modifier = Modifier.fillMaxSize(),
                                series = series
                            )
                        }

                        PlotType.COMPLEX_CIRCULAR -> {
                            val realPart = series.getOrNull(0)?.dataPoints?.lastOrNull() ?: 0f
                            val imagPart = series.getOrNull(1)?.dataPoints?.lastOrNull() ?: 0f
                            ComplexCircularPlot(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                points = listOf(
                                    ComplexPlotData(
                                        real = realPart,
                                        imaginary = imagPart,
                                        color = neonAccent
                                    )
                                ),
                                gridColor = Color.White.copy(alpha = 0.2f)
                            )
                        }

                        PlotType.BAR_GRAPH -> {
                            BarGraph(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp, vertical = 34.dp),
                                series = series
                            )
                        }
                    }
                }

                if (showAdBanner) {
                    AdBanner(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.43f

                drawCircle(
                    color = neonAccent.copy(alpha = 0.05f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                drawCircle(
                    color = neonMain.copy(alpha = 0.08f),
                    radius = radius * 0.68f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawNeonFrame(
    neonMain: Color,
    neonAccent: Color,
) {
    val corner = 14.dp.toPx()
    val rectSize = Size(size.width, size.height)

    drawRoundRect(
        color = neonAccent.copy(alpha = 0.2f),
        size = rectSize,
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.2.dp.toPx())
    )

    drawIntoCanvas { canvas ->
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2.4.dp.toPx()
            color = neonMain.copy(alpha = 0.45f).toArgb()
            maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        }

        val corePaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.2.dp.toPx()
            color = neonMain.toArgb()
        }

        val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
        canvas.nativeCanvas.drawRoundRect(rect, corner, corner, glowPaint)
        canvas.nativeCanvas.drawRoundRect(rect, corner, corner, corePaint)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 900, heightDp = 380)
@Composable
private fun CenterDisplayLedStylePreviewCartesian() {
    CenterDisplayLedStyle(
        modifier = Modifier.size(width = 900.dp, height = 380.dp),
        series = listOf(
            PlotData(name = "Volts", dataPoints = List(80) { i -> (kotlin.math.sin(i * 0.12f) * 0.35f) + 0.55f }, colorArgb = 0xFF00FFFF.toInt()),
            PlotData(name = "Amps", dataPoints = List(80) { i -> (kotlin.math.cos(i * 0.16f) * 0.2f) + 0.3f }, colorArgb = 0xFFFF0000.toInt()),
            PlotData(name = "Temp", dataPoints = List(80) { i -> (kotlin.math.sin(i * 0.08f) * 0.22f) + 0.6f }, colorArgb = 0xFFFFFF00.toInt())
        ),
        plotType = PlotType.CARTESIAN,
        showAdBanner = false
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 900, heightDp = 380)
@Composable
private fun CenterDisplayLedStylePreviewBars() {
    CenterDisplayLedStyle(
        modifier = Modifier.size(width = 900.dp, height = 380.dp),
        series = listOf(
            PlotData(name = "Volts", dataPoints = listOf(0.7f), colorArgb = 0xFF00FFFF.toInt()),
            PlotData(name = "Amps", dataPoints = listOf(0.4f), colorArgb = 0xFFFF0000.toInt()),
            PlotData(name = "RPM", dataPoints = listOf(0.9f), colorArgb = 0xFF00FF00.toInt()),
            PlotData(name = "Temp", dataPoints = listOf(0.6f), colorArgb = 0xFFFFFF00.toInt())
        ),
        plotType = PlotType.BAR_GRAPH,
        showAdBanner = false
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 900, heightDp = 380)
@Composable
private fun CenterDisplayLedStylePreviewComplex() {
    CenterDisplayLedStyle(
        modifier = Modifier.size(width = 900.dp, height = 380.dp),
        series = listOf(
            PlotData(name = "Real", dataPoints = listOf(0.6f), colorArgb = 0xFF00FFFF.toInt()),
            PlotData(name = "Imag", dataPoints = listOf(0.35f), colorArgb = 0xFFFF00FF.toInt())
        ),
        plotType = PlotType.COMPLEX_CIRCULAR,
        showAdBanner = false
    )
}