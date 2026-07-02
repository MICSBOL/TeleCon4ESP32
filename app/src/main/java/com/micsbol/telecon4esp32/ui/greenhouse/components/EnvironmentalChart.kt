package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass
import com.micsbol.telecon4esp32.ui.greenhouse.EnvironmentalChartData

private const val TEMP_MAX = 40f
private const val HUMIDITY_MAX = 100f
private const val VPD_MAX = 2f

private val TempAxisLabels = listOf("40", "30", "20", "10", "0")
private val HumidityAxisLabels = listOf("100", "75", "50", "25", "0")
private val VpdAxisLabels = listOf("2", "1.5", "1", "0.5", "0")

private val GreenhouseGridLine = Color.White.copy(alpha = 0.22f)

private val CompactChartWidthThreshold = 420.dp

private val ChartAxisFontSizeCompact = 9.sp
private val ChartAxisFontSizeRegular = 10.sp
private val ChartTimeFontSizeCompact = 8.sp
private val ChartTimeFontSizeRegular = 9.sp

@Composable
private fun ChartYAxisLabels(
    values: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val fontSize = if (compact) ChartAxisFontSizeCompact else ChartAxisFontSizeRegular
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        values.forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun ChartTimeLabelsRow(
    timeLabels: List<String>,
    leftAxisWidth: Dp,
    rightAxisInset: Dp,
    compact: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(leftAxisWidth))
        Row(modifier = Modifier.weight(1f)) {
            timeLabels.forEachIndexed { index, label ->
                val showLabel = !compact ||
                    index == 0 ||
                    index == timeLabels.lastIndex ||
                    index % 2 == 0
                val alignment = when (index) {
                    0 -> Alignment.TopStart
                    timeLabels.lastIndex -> Alignment.TopEnd
                    else -> Alignment.TopCenter
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = alignment,
                ) {
                    if (showLabel) {
                        val fontSize = if (compact) ChartTimeFontSizeCompact else ChartTimeFontSizeRegular
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = fontSize,
                                lineHeight = fontSize,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = GreenhouseGlass.ChartAxisTime,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            textAlign = when (index) {
                                0 -> TextAlign.Start
                                timeLabels.lastIndex -> TextAlign.End
                                else -> TextAlign.Center
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(rightAxisInset))
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isPortrait =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        val compactChart = isPortrait || maxWidth < CompactChartWidthThreshold
        val leftAxisWidth = if (compactChart) 28.dp else 26.dp
        val humidityAxisWidth = if (compactChart) 32.dp else 30.dp
        val vpdAxisWidth = if (compactChart) 30.dp else 28.dp
        val rightAxisInset = humidityAxisWidth + vpdAxisWidth

        Column(modifier = Modifier.fillMaxWidth()) {
            ChartHeader(compactChart = compactChart)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = plotAreaModifier,
                verticalAlignment = Alignment.Top,
            ) {
                ChartYAxisLabels(
                    values = TempAxisLabels,
                    color = GreenhouseGlass.ChartTemp,
                    compact = compactChart,
                    modifier = Modifier
                        .width(leftAxisWidth)
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
                    color = GreenhouseGlass.ChartHumidity,
                    compact = compactChart,
                    modifier = Modifier
                        .width(humidityAxisWidth)
                        .fillMaxHeight(),
                )
                ChartYAxisLabels(
                    values = VpdAxisLabels,
                    color = GreenhouseGlass.ChartGreen,
                    compact = compactChart,
                    modifier = Modifier
                        .width(vpdAxisWidth)
                        .fillMaxHeight(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ChartTimeLabelsRow(
                timeLabels = chartData.timeLabels,
                leftAxisWidth = leftAxisWidth,
                rightAxisInset = rightAxisInset,
                compact = compactChart,
            )
        }
    }
}

@Composable
private fun ChartHeader(
    compactChart: Boolean,
) {
    if (compactChart) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.greenhouse_chart_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = GreenhouseGlass.TextPrimary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChartLegendItem(
                    color = GreenhouseGlass.ChartTemp,
                    label = stringResource(R.string.greenhouse_chart_temp),
                    compact = true,
                )
                ChartLegendItem(
                    color = GreenhouseGlass.ChartHumidity,
                    label = stringResource(R.string.greenhouse_chart_humidity),
                    compact = true,
                )
                ChartLegendItem(
                    color = GreenhouseGlass.ChartGreen,
                    label = stringResource(R.string.greenhouse_chart_vpd),
                    compact = true,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.greenhouse_chart_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = GreenhouseGlass.TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartLegendItem(
                    color = GreenhouseGlass.ChartTemp,
                    label = stringResource(R.string.greenhouse_chart_temp),
                )
                ChartLegendItem(
                    color = GreenhouseGlass.ChartHumidity,
                    label = stringResource(R.string.greenhouse_chart_humidity),
                )
                ChartLegendItem(
                    color = GreenhouseGlass.ChartGreen,
                    label = stringResource(R.string.greenhouse_chart_vpd),
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    compact: Boolean = false,
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
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (compact) 10.sp else 11.sp,
            ),
            color = GreenhouseGlass.ChartLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
private fun EnvironmentalChartCanvas(
    chartData: EnvironmentalChartData,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

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
                color = GreenhouseGridLine,
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
                color = GreenhouseGridLine,
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
            color = GreenhouseGlass.ChartHumidity,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawPath(
            path = buildPath(chartData.vpdSeries, VPD_MAX),
            color = GreenhouseGlass.ChartGreen,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawPath(
            path = buildPath(chartData.tempSeries, TEMP_MAX),
            color = GreenhouseGlass.ChartTemp,
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}
