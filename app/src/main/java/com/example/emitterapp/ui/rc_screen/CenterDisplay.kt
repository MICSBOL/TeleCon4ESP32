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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
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
    onTopLeftPress: () -> Unit,
    onTopRightPress: () -> Unit,
    onBottomLeftPress: () -> Unit,
    onBottomRightPress: () -> Unit,
    screenAspectRatio: Float = 0f,
    series: List<PlotData> = emptyList()
) {
    var plotType by remember { mutableStateOf(PlotType.CARTESIAN) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            plotType = when (plotType) {
                PlotType.CARTESIAN -> PlotType.COMPLEX_CIRCULAR
                PlotType.COMPLEX_CIRCULAR -> PlotType.BAR_GRAPH
                PlotType.BAR_GRAPH -> PlotType.CARTESIAN
            }
        }
    }
    val isWideScreen = screenAspectRatio > 1.7f
    val is4Over3 = screenAspectRatio == 4 / 3f
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

                Box(
                    modifier = Modifier
                        .padding(bottom = 28.dp)
                        .background(
                            Color(0xFF0A0F1A),
//                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    when (plotType) {
                        PlotType.CARTESIAN -> {
                            CartesianPlot(
                                modifier = Modifier.align(Alignment.TopCenter),
                                series = series
                            )
                        }

                        PlotType.COMPLEX_CIRCULAR -> {
                            val realPart = series.getOrNull(0)?.dataPoints?.lastOrNull() ?: 0f
                            val imagPart = series.getOrNull(1)?.dataPoints?.lastOrNull() ?: 0f
                            ComplexCircularPlot(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxSize()
                                    .padding(24.dp),
                                points = listOf(
                                    // Create a ComplexPlotData point from the last known values of "Volts" and "Amps"
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
                                    .align(Alignment.TopCenter)
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp, vertical = 34.dp),
                                series = series,
                            )
                        }
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.center_frame_blue),
                    contentDescription = "Center Display Frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                if (!isWideScreen) { //&& !is4Over3){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.RIGHT,
                            onPress = onTopRightPress
                        )
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.LEFT,
                            onPress = onTopLeftPress
                        )
                    }
                }
            }

            if (isWideScreen) {  //|| is4Over3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PushButtonSide(
                        modifier = Modifier.size(50.dp),
                        side = ButtonSide.RIGHT,
                        onPress = onTopRightPress
                    )
                    PushButtonSide(
                        modifier = Modifier.size(50.dp),
                        side = ButtonSide.LEFT,
                        onPress = onTopLeftPress
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = if (isWideScreen) (-20).dp else 0.dp)
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.RIGHT,
                    onPress = onBottomRightPress
                )
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.LEFT,
                    onPress = onBottomLeftPress
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
            .padding(vertical = 4.dp, horizontal = 4.dp), // Padding inside the frame
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
                        Text(
                            text = plotData.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .background(plotData.color)
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
                        color = plotData.color
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

            // You could use your GlowingBar or a simple Box
            GlowingBar(
                modifier = Modifier
                    .weight(1f) // Each bar takes equal width
                    .fillMaxHeight(barHeight),
                color = barColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun HistogramPlotPreview() {
    // 1. In a real app, this data would be collected and processed.
    // 2. For a preview, we just provide static, pre-calculated bin counts.
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
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun DynamicHistogramPreview() {
    // This list holds the raw data points we collect over time
    val historicalData = remember { mutableStateListOf<Float>() }
    // This list holds the processed bin counts that the UI will draw
    val binCounts = remember { mutableStateListOf<Int>() }

    val numBins = 20 // The number of bars to display in the histogram
    val maxHistorySize = 1000 // We'll analyze the last 1000 samples

    LaunchedEffect(Unit) {
        while (true) {
            // 1. Generate new data points rapidly, simulating a sensor reading
            // We use a sine wave with some random noise to make it more realistic
            val newDataPoint = (sin(System.currentTimeMillis() / 2000f * 2 * PI.toFloat()) + Random.nextFloat() * 0.5f).coerceIn(-1f, 1f)
            historicalData.add((newDataPoint + 1f) / 2f) // Normalize to 0-1 range

            // Keep the historical data list constrained to the last 1000 samples
            if (historicalData.size > maxHistorySize) {
                historicalData.removeFirst()
            }

            // 2. Process the data into bins only when we have a full set of data
            if (historicalData.size == maxHistorySize) {
                val newBins = IntArray(numBins) { 0 } // Create an array of zeros

                // For each data point, figure out which bin it belongs to and increment that bin's count
                historicalData.forEach { value ->
                    val binIndex = (value * (numBins - 1)).toInt().coerceIn(0, numBins - 1)
                    newBins[binIndex]++
                }

                // Update the state that the UI observes
                binCounts.clear()
                binCounts.addAll(newBins.toList())
            }

            delay(16L) // Generate new data at ~60fps
        }
    }

    // Call the HistogramPlot with the dynamically updated bin counts
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
    // 1. Create state holders for two complex numbers
    var complexPoint1 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Cyan)) }
    var complexPoint2 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Yellow)) }
    var time by remember { mutableStateOf(0f) }

    // 2. Simulate the data changing over time
    LaunchedEffect(Unit) {
        while (true) {
            // Point 1 (e.g., Volts/Amps) - Rotates steadily with a large magnitude
            val magnitude1 = 0.8f
            complexPoint1 = complexPoint1.copy(
                real = magnitude1 * cos(time * 1.5f),
                imaginary = magnitude1 * sin(time * 1.5f)
            )

            // Point 2 (e.g., RPM/Temp) - Rotates faster with a smaller, fluctuating magnitude
            val magnitude2 = 0.3f + (sin(time * 0.5f) * 0.2f)
            complexPoint2 = complexPoint2.copy(
                real = magnitude2 * cos(time * 3f),
                imaginary = magnitude2 * sin(time * 3f)
            )

            time += 0.02f
            delay(16L) // ~60fps
        }
    }

    // 3. Call the ComplexCircularPlot composable with the simulated data
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
        PlotData(name = "Volts", dataPoints = listOf(0.75f), color = Color.Cyan),     // 75% height
        PlotData(name = "Amps", dataPoints = listOf(0.40f), color = Color.Red),      // 40% height
        PlotData(name = "RPM", dataPoints = listOf(0.90f), color = Color.Green),    // 90% height
        PlotData(name = "Temp", dataPoints = listOf(0.60f), color = Color.Yellow)    // 60% height
    )
    val sampleLabels = listOf("Volts", "Amps", "RPM", "Temp")

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
        onTopLeftPress = {},
        onTopRightPress = {},
        onBottomLeftPress = {},
        onBottomRightPress = {},
        screenAspectRatio = 800f / 400f,
        series = listOf(
            PlotData(name = "Volts", dataPoints = voltsData, color = Color.Cyan),
            PlotData(name = "Amps", dataPoints = ampsData, color = Color.Red),
            PlotData(name = "RMP", dataPoints = rpmData, color = Color.Green),
            PlotData(name = "Temp", dataPoints = tempData, color = Color.Yellow)
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
        // We only draw up to the last 100 samples
        val last100Points = dataPoints.takeLast(100)

        last100Points.forEach { point ->
            // The height of each bar is the individual data point's value
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

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun ScrollingBarPlotPreview() {
    // This list holds the raw data points as they are generated
    val dataStream = remember { mutableStateListOf<Float>() }
    val maxSamples = 100 // We want to display 100 bars
    var time by remember {mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            // Generate a new data point using a sine wave
            val newDataPoint = (sin(time * 2 * PI.toFloat()) + 1f) / 2f
            dataStream.add(newDataPoint)

            // Keep the list constrained to the last 100 samples
            while (dataStream.size > maxSamples) {
                dataStream.removeFirst()
            }
            time += 0.05f
            delay(50L) // Add a new data point every 50ms
        }
    }

    // Call the ScrollingBarPlot with the dynamically updating list of data points
    ScrollingBarPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        dataPoints = dataStream
    )
}