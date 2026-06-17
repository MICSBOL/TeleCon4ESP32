package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.greenhouse.EnvironmentalChartData
import com.micsbol.telecon4esp32.ui.theme.DarkGridLine
import com.micsbol.telecon4esp32.ui.theme.LightGridLine
import com.micsbol.telecon4esp32.ui.theme.PlotCyan
import com.micsbol.telecon4esp32.ui.theme.PlotGreen
import com.micsbol.telecon4esp32.ui.theme.PlotOrange

private const val TEMP_MAX = 40f
private const val HUMIDITY_MAX = 100f
private const val VPD_MAX = 2f

private val TempAxisLabels = listOf("40", "30", "20", "10", "0")
private val HumidityAxisLabels = listOf("100", "75", "50", "25", "0")
private val VpdAxisLabels = listOf("2.0", "1.5", "1.0", "0.5", "0")

private val LeftAxisWidth = 28.dp
private val HumidityAxisWidth = 28.dp
private val VpdAxisWidth = 28.dp
private val RightAxisInset = HumidityAxisWidth + VpdAxisWidth

@Composable
private fun ChartYAxisLabels(
    values: List<String>,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color,
                textAlign = textAlign,
            )
        }
    }
}

@Composable
private fun ChartTimeLabelsRow(
    timeLabels: List<String>,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(LeftAxisWidth))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            timeLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor(),
                )
            }
        }
        Spacer(modifier = Modifier.width(RightAxisInset))
    }
}

@Composable
fun EnvironmentalChart(
    chartData: EnvironmentalChartData,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = false,
) {
    if (fillAvailableHeight) {
        ExpandableEnvironmentalChart(
            chartData = chartData,
            modifier = modifier,
        )
    } else {
        GreenhouseCard(modifier = modifier) {
            EnvironmentalChartBody(
                chartData = chartData,
                plotAreaModifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }
    }
}

@Composable
private fun ExpandableEnvironmentalChart(
    chartData: EnvironmentalChartData,
    modifier: Modifier = Modifier,
) {
    GreenhouseCard(
        modifier = modifier.fillMaxHeight(),
        fillHeight = true,
    ) {
        EnvironmentalChartBody(
            chartData = chartData,
            plotAreaModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 160.dp),
        )
    }
}

@Composable
private fun ColumnScope.EnvironmentalChartBody(
    chartData: EnvironmentalChartData,
    plotAreaModifier: Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.greenhouse_chart_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartLegendItem(
                color = PlotOrange,
                label = stringResource(R.string.greenhouse_chart_temp),
            )
            ChartLegendItem(
                color = PlotCyan,
                label = stringResource(R.string.greenhouse_chart_humidity),
            )
            ChartLegendItem(
                color = PlotGreen,
                label = stringResource(R.string.greenhouse_chart_vpd),
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = plotAreaModifier,
        verticalAlignment = Alignment.Top,
    ) {
        ChartYAxisLabels(
            values = TempAxisLabels,
            color = PlotOrange,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(LeftAxisWidth)
                .fillMaxHeight(),
        )
        EnvironmentalChartCanvas(
            chartData = chartData,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        ChartYAxisLabels(
            values = HumidityAxisLabels,
            color = PlotCyan,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .width(HumidityAxisWidth)
                .fillMaxHeight(),
        )
        ChartYAxisLabels(
            values = VpdAxisLabels,
            color = PlotGreen,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .width(VpdAxisWidth)
                .fillMaxHeight(),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    ChartTimeLabelsRow(timeLabels = chartData.timeLabels)
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = mutedTextColor(),
        )
    }
}

@Composable
private fun EnvironmentalChartCanvas(
    chartData: EnvironmentalChartData,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val gridColor = if (isSystemInDarkTheme()) DarkGridLine else LightGridLine

    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            canvasSize = coordinates.size.toSize()
        },
    ) {
        if (canvasSize == Size.Zero) return@Canvas

        val leftPad = 2.dp.toPx()
        val rightPad = 2.dp.toPx()
        val topPad = 4.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - 4.dp.toPx()

        val horizontalLines = 4
        for (i in 0..horizontalLines) {
            val y = topPad + (chartHeight / horizontalLines) * i
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val verticalDivisions = (chartData.timeLabels.size - 1).coerceAtLeast(1)
        val verticalDash = PathEffect.dashPathEffect(
            intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
            phase = 0f,
        )
        for (i in 0..verticalDivisions) {
            val x = leftPad + (chartWidth * i / verticalDivisions)
            drawLine(
                color = gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + chartHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = verticalDash,
            )
        }

        fun buildPath(values: List<Float>, maxValue: Float): Path {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = leftPad + (chartWidth * index / (values.size - 1).coerceAtLeast(1))
                val y = topPad + chartHeight - (value / maxValue) * chartHeight
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(
            path = buildPath(chartData.humiditySeries, HUMIDITY_MAX),
            color = PlotCyan,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawPath(
            path = buildPath(chartData.vpdSeries, VPD_MAX),
            color = PlotGreen,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawPath(
            path = buildPath(chartData.tempSeries, TEMP_MAX),
            color = PlotOrange,
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}
