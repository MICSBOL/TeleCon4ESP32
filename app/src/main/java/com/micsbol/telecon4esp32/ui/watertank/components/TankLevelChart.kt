package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
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
import com.micsbol.telecon4esp32.ui.theme.DarkGridLine
import com.micsbol.telecon4esp32.ui.theme.LightGridLine
import com.micsbol.telecon4esp32.ui.theme.PlotCyan
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright
import com.micsbol.telecon4esp32.ui.watertank.TankLevelChartData

private val LevelAxisValues = listOf(100, 80, 60, 40, 20, 0)
private val LeftAxisWidth = 36.dp

@Composable
fun TankLevelChart(
    chartData: TankLevelChartData,
    modifier: Modifier = Modifier,
) {
    WaterTankCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.water_tank_chart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = PlotCyan)
                }
                Text(
                    text = stringResource(R.string.water_tank_chart_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor(),
                )
            }
        }
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
                LevelAxisValues.forEach { value ->
                    Text(
                        text = stringResource(R.string.water_tank_percent_value, value),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = mutedTextColor(),
                        textAlign = TextAlign.End,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
            ) {
                TankLevelChartCanvas(
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
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChartStatCard(
                icon = Icons.Default.Timeline,
                label = stringResource(R.string.water_tank_stat_average),
                value = stringResource(R.string.water_tank_percent_value, chartData.averagePercent),
                modifier = Modifier.weight(1f),
            )
            ChartStatCard(
                icon = Icons.Default.ArrowDownward,
                label = stringResource(R.string.water_tank_stat_minimum),
                value = stringResource(R.string.water_tank_percent_value, chartData.minimumPercent),
                subtext = chartData.minimumLabel,
                modifier = Modifier.weight(1f),
            )
            ChartStatCard(
                icon = Icons.Default.ArrowUpward,
                label = stringResource(R.string.water_tank_stat_maximum),
                value = stringResource(R.string.water_tank_percent_value, chartData.maximumPercent),
                subtext = chartData.maximumLabel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChartStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtext: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TechCyanBright,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = mutedTextColor(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtext != null) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor(),
            )
        }
    }
}

@Composable
private fun TankLevelChartCanvas(
    chartData: TankLevelChartData,
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

        val horizontalLines = 5
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
            val value = chartData.levelSeries[index]
            val x = leftPad + (chartWidth * index / (chartData.levelSeries.size - 1).coerceAtLeast(1))
            val y = topPad + chartHeight - (value / 100f) * chartHeight
            return Offset(x, y)
        }

        val linePath = Path()
        chartData.levelSeries.forEachIndexed { index, _ ->
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
                    PlotCyan.copy(alpha = 0.45f),
                    PlotCyan.copy(alpha = 0.05f),
                ),
                startY = topPad,
                endY = topPad + chartHeight,
            ),
        )
        drawPath(
            path = linePath,
            color = PlotCyan,
            style = Stroke(width = 2.5.dp.toPx()),
        )

        val lastIndex = chartData.levelSeries.lastIndex
        val lastPoint = pointAt(lastIndex)
        drawCircle(color = PlotCyan, radius = 4.dp.toPx(), center = lastPoint)
        drawCircle(
            color = PlotCyan.copy(alpha = 0.25f),
            radius = 8.dp.toPx(),
            center = lastPoint,
        )
    }
}

@Composable
fun WaterTankFooter(
    isOnline: Boolean,
    lastUpdatedTime: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(
                    color = if (isOnline) {
                        androidx.compose.ui.graphics.Color(0xFF10B981)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFF43F5E)
                    },
                )
            }
            Text(
                text = stringResource(
                    if (isOnline) R.string.water_tank_sensor_online else R.string.water_tank_sensor_offline,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.water_tank_last_updated_time, lastUpdatedTime),
            style = MaterialTheme.typography.labelSmall,
            color = mutedTextColor(),
        )
    }
}
