package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.solarsystem.ProductionChartData
import com.micsbol.telecon4esp32.ui.theme.DarkGridLine
import com.micsbol.telecon4esp32.ui.theme.LightGridLine
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import java.util.Locale

private const val POWER_MAX_KW = 1.2f
private val PowerAxisValues = listOf(1.2f, 0.9f, 0.6f, 0.3f, 0.0f)
private val LeftAxisWidth = 36.dp

@Composable
fun ProductionChart(
    chartData: ProductionChartData,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.solar_system_section_production_today),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.solar_system_period_today),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProductionChartLegend()
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .width(LeftAxisWidth)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                PowerAxisValues.forEach { value ->
                    Text(
                        text = String.format(Locale.US, "%.1f", value),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = PlotOrange,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
            ) {
                ProductionChartCanvas(
                    chartData = chartData,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(LeftAxisWidth))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                chartData.timeLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedTextColor(),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.solar_system_production_axis_unit),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = LeftAxisWidth),
            style = MaterialTheme.typography.labelSmall,
            color = mutedTextColor(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProductionChartLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = PlotOrange)
        }
        Text(
            text = stringResource(R.string.solar_system_production_axis_unit),
            style = MaterialTheme.typography.labelSmall,
            color = mutedTextColor(),
        )
    }
}

@Composable
private fun ProductionChartCanvas(
    chartData: ProductionChartData,
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
        val bottomPad = 4.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

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

        fun pointAt(index: Int): Offset {
            val value = chartData.powerSeries[index]
            val x = leftPad + (chartWidth * index / (chartData.powerSeries.size - 1).coerceAtLeast(1))
            val y = topPad + chartHeight - (value / POWER_MAX_KW) * chartHeight
            return Offset(x, y)
        }

        val linePath = Path()
        chartData.powerSeries.forEachIndexed { index, _ ->
            val point = pointAt(index)
            if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(leftPad + chartWidth, topPad + chartHeight)
            lineTo(leftPad, topPad + chartHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PlotOrange.copy(alpha = 0.45f),
                    PlotOrange.copy(alpha = 0.05f),
                ),
                startY = topPad,
                endY = topPad + chartHeight,
            ),
        )
        drawPath(
            path = linePath,
            color = PlotOrange,
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}
