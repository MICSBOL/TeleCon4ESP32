package com.example.emitterapp.ui.rc_screen

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.ui.rc_screen.components.AdBanner
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.HorizontalTextAnimation
import com.example.emitterapp.ui.rc_screen.components.PushButtonSide
import com.example.emitterapp.ui.rc_screen.components.RealTimePlot
import kotlinx.coroutines.delay
import kotlin.apply
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun CenterDisplay(
    modifier: Modifier = Modifier,
    series: List<PlotData> = emptyList()
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier
                        .padding(bottom = 18.dp)
                        .background(
                            Color(0xFF0A0F1A),
                        )
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (PlotType.CARTESIAN) {
                            PlotType.CARTESIAN -> {
                                CartesianPlot(
                                    modifier = Modifier,
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
                                            color = Color.Magenta
                                        )
                                    )
                                )
                            }

                            PlotType.BAR_GRAPH -> {
                                BarGraph(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 32.dp, vertical = 34.dp),
                                    series = series,
                                )
                            }
                        }
                    }
                    AdBanner(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.center_frame_blue),
                    contentDescription = "Center Display Frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}

enum class PlotType {
    CARTESIAN,
    COMPLEX_CIRCULAR,
    BAR_GRAPH
}

@Composable
fun CartesianPlot(
    modifier: Modifier,
    series: List<PlotData> = emptyList()
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.15f)
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.Start
        ) {

            series.forEach { plotData ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        HorizontalTextAnimation(
                            text = plotData.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .background(Color(plotData.colorArgb))
                    )
                }
            }

        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealTimePlot(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 8.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                series = series
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Time (s)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

data class ComplexPlotData(
    val real: Float,
    val imaginary: Float,
    val color: Color
)

@Composable
fun ComplexCircularPlot(
    modifier: Modifier = Modifier,
    points: List<ComplexPlotData>,
    gridColor: Color = Color.White.copy(alpha = 0.3f)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2

        (1..4).forEach { i ->
            drawCircle(
                color = gridColor,
                radius = radius * (i / 4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        (0 until 360 step 45).forEach { angle ->
            val angleInRadians = Math.toRadians(angle.toDouble()).toFloat()
            val start = Offset(centerX, centerY)
            val end = Offset(
                x = centerX + radius * cos(angleInRadians),
                y = centerY + radius * sin(angleInRadians)
            )
            drawLine(gridColor, start, end, strokeWidth = 1.dp.toPx())
        }

        drawLine(
            gridColor,
            Offset(centerX, 0f),
            Offset(centerX, size.height), strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            gridColor,
            Offset(0f, centerY),
            Offset(size.width, centerY), strokeWidth = 1.5.dp.toPx()
        )

        points.forEach { point ->
            // Calculate magnitude and phase
            val magnitude = sqrt(point.real * point.real + point.imaginary * point.imaginary)
            val phase = atan2(point.imaginary, point.real)

            val pointRadius = magnitude.coerceAtMost(1f) * radius

            val x = centerX + pointRadius * cos(phase)
            val y = centerY + pointRadius * sin(phase) // Y-axis is standard here, not inverted

            drawLine(
                color = point.color.copy(alpha = 0.7f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(color = point.color, radius = 8f, center = Offset(x, y))
        }
    }
}


@Composable
fun BarGraph(
    modifier: Modifier = Modifier,
    series: List<PlotData>,
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        series.forEach { plotData ->
            val currentValue = plotData.dataPoints.lastOrNull() ?: 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlowingBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(currentValue),
                        color = Color(plotData.colorArgb)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plotData.name,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun GlowingBar(
    modifier: Modifier = Modifier,
    color: Color
) {
    val glowColor = color.copy(alpha = 0.3f)
    val cornerRadius = CornerRadius(x = 2.dp.value, y = 2.dp.value)

    Box(
        modifier = modifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        this.color = glowColor.toArgb()
                        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.x,
                        cornerRadius.y,
                        paint
                    )
                }
            }
            .background(color, shape = RoundedCornerShape(2.dp))
    )
}

@Composable
fun HistogramPlot(
    modifier: Modifier = Modifier,
    binCounts: List<Int>, // The processed counts for each bin
    barColor: Color = Color(0xFF4682B4)// A good color for statistical plots
) {
    val maxCount = binCounts.maxOrNull() ?: 1

    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), // Bars are close together
        verticalAlignment = Alignment.Bottom
    ) {
        binCounts.forEach { count ->
            val barHeight = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

            GlowingBar(
                modifier = Modifier
                    .weight(1f) // Each bar takes equal width
                    .fillMaxHeight(barHeight),
                color = barColor
            )
        }
    }
}


@Composable
fun ButtonColumn(
    modifier: Modifier = Modifier,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
    side: ButtonSide,
    buttonSize: Dp = 50.dp
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalAlignment =
            if (side == ButtonSide.RIGHT) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PushButtonSide(
            modifier = Modifier.size(buttonSize * 0.8f),
            side = side,
            onPress = onTopPress
        )
        PushButtonSide(modifier = Modifier.size(buttonSize), side = side, onPress = onBottomPress)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun HistogramPlotPreview() {
    val sampleBinCounts = listOf(
        10,
        25,
        40,
        80,
        110,
        75,
        35,
        20,
        12,
        5,
        10,
        1,
        5,
        7,
        65,
        77,
        88,
        99,
        100,
        120,
        70,
        5,
        64
    )

    HistogramPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .padding(16.dp),
        binCounts = sampleBinCounts
    )
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun DynamicHistogramPreview() {
    val historicalData = remember { mutableStateListOf<Float>() }
    val binCounts = remember { mutableStateListOf<Int>() }

    val numBins = 20 // The number of bars to display in the histogram
    val maxHistorySize = 1000 // We'll analyze the last 1000 samples

    LaunchedEffect(Unit) {
        while (true) {
            val newDataPoint =
                (sin(System.currentTimeMillis() / 2000f * 2 * PI.toFloat()) + Random.nextFloat() * 0.5f).coerceIn(
                    -1f,
                    1f
                )
            historicalData.add((newDataPoint + 1f) / 2f) // Normalize to 0-1 range

            if (historicalData.size > maxHistorySize) {
                historicalData.removeFirst()
            }

            if (historicalData.size == maxHistorySize) {
                val newBins = IntArray(numBins) { 0 } // Create an array of zeros

                historicalData.forEach { value ->
                    val binIndex = (value * (numBins - 1)).toInt().coerceIn(0, numBins - 1)
                    newBins[binIndex]++
                }

                binCounts.clear()
                binCounts.addAll(newBins.toList())
            }

            delay(16L) // Generate new data at ~60fps
        }
    }

    HistogramPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        binCounts = binCounts
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun ComplexCircularPlotPreview() {
    var complexPoint1 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Cyan)) }
    var complexPoint2 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Yellow)) }
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val magnitude1 = 0.8f
            complexPoint1 = complexPoint1.copy(
                real = magnitude1 * cos(time * 1.5f),
                imaginary = magnitude1 * sin(time * 1.5f)
            )

            val magnitude2 = 0.3f + (sin(time * 0.5f) * 0.2f)
            complexPoint2 = complexPoint2.copy(
                real = magnitude2 * cos(time * 3f),
                imaginary = magnitude2 * sin(time * 3f)
            )

            time += 0.02f
            delay(16L) // ~60fps
        }
    }

    ComplexCircularPlot(
        modifier = Modifier
            .size(300.dp)
            .background(Color.Black.copy(alpha = 0.5f)),
        points = listOf(complexPoint1, complexPoint2)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun BarGraphPreview() {
    val sampleSeries = listOf(
        PlotData(name = "Volts", dataPoints = listOf(0.75f), colorArgb = 0xFF00FFFF.toInt()),
        PlotData(name = "Amps", dataPoints = listOf(0.40f), colorArgb = 0xFFFF0000.toInt()),
        PlotData(name = "RPM", dataPoints = listOf(0.90f), colorArgb = 0xFF00FF00.toInt()),
        PlotData(name = "Temp", dataPoints = listOf(0.60f), colorArgb = 0xFFFFFF00.toInt())
    )
    BarGraph(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        series = sampleSeries,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun InteractiveCenterDisplayPreview() {

    val voltsData = remember { mutableStateListOf<Float>() }
    val ampsData = remember { mutableStateListOf<Float>() }
    val rpmData = remember { mutableStateListOf<Float>() }
    val tempData = remember { mutableStateListOf<Float>() }

    val maxDataPoints = 100
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val voltsPoint = (sin(time * 1.5f * PI.toFloat()) * 0.4f) + 0.6f
            voltsData.add(voltsPoint)
            if (voltsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    voltsData.removeFirst()
                }
            }

            val ampsPoint = (cos(time * 3f * PI.toFloat()) * 0.2f) + 0.25f
            ampsData.add(ampsPoint)
            if (ampsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    ampsData.removeFirst()
                }
            }

            val rpmSignal = sin(time * 2f * PI.toFloat()) // Use a different frequency
            val rpmPoint = if (rpmSignal >= 0) 0.9f else 0.1f
            rpmData.add(rpmPoint / 2 + 0.25f)
            if (rpmData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    rpmData.removeFirst()
                }
            }
            val period = 1f // The wave will repeat every 2 "time" units
            val tempPoint = (time % period) / period
            tempData.add(tempPoint / 2 + 0.25f)
            if (tempData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    tempData.removeFirst()
                }
            }

            time += 0.02f
            delay(16L)
        }
    }

    CenterDisplay(
        modifier = Modifier.size(width = 800.dp, height = 400.dp),
        series = listOf(
            PlotData(name = "Volts", dataPoints = voltsData, colorArgb = 0xFF00FFFF.toInt()),
            PlotData(name = "Amps", dataPoints = ampsData, colorArgb = 0xFFFF0000.toInt()),
            PlotData(name = "RMP", dataPoints = rpmData, colorArgb = 0xFF00FF00.toInt()),
            PlotData(name = "Temp", dataPoints = tempData, colorArgb = 0xFFFFFF00.toInt())
        )
    )
}

@Composable
fun ScrollingBarPlot(
    modifier: Modifier = Modifier,
    dataPoints: List<Float>, // Now takes a list of raw data points
    barColor: Color = Color(0xFF4682B4) // SteelBlue
) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp), // Bars are very close
        verticalAlignment = Alignment.Bottom
    ) {
        val last100Points = dataPoints.takeLast(100)

        last100Points.forEach { point ->
            val barHeight = point.coerceIn(0f, 1f)

            GlowingBar(
                modifier = Modifier
                    .weight(1f) // Each bar takes equal width
                    .fillMaxHeight(barHeight),
                color = barColor
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun ScrollingBarPlotPreview() {
    val dataStream = remember { mutableStateListOf<Float>() }
    val maxSamples = 100 // We want to display 100 bars
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val newDataPoint = (sin(time * 2 * PI.toFloat()) + 1f) / 2f
            dataStream.add(newDataPoint)

            while (dataStream.size > maxSamples) {
                dataStream.removeFirst()
            }
            time += 0.05f
            delay(50L) // Add a new data point every 50ms
        }
    }

    ScrollingBarPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        dataPoints = dataStream
    )
}