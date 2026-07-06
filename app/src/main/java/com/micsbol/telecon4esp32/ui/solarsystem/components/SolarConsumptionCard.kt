package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.solarsystem.ConsumptionChartData
import com.micsbol.telecon4esp32.ui.solarsystem.SolarChartPeriod
import com.micsbol.telecon4esp32.ui.solarsystem.SolarEnergyBreakdown
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

private val ChartHeight = 170.dp
private val AxisWidth = 54.dp
private val BarStrokeWidth = 2.dp
private val DotOuterRadius = 3.5.dp
private val DotInnerRadius = 1.75.dp
private val BarDotGap = 2.dp

@Composable
fun SolarConsumptionCard(
    consumedKwh: Float,
    chartData: ConsumptionChartData,
    breakdown: SolarEnergyBreakdown,
    chartPeriod: SolarChartPeriod,
    onPeriodSelected: (SolarChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SolarGlass.AccentAmber),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = SolarGlass.TextOnGlassPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.solar_system_energy_kwh_value,
                        String.format(Locale.US, "%.0f", consumedKwh),
                    ),
                    color = SolarGlass.TextOnGlassPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.solar_system_electricity_consumed),
                    color = SolarGlass.TextOnGlassSecondary,
                    fontSize = 13.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PeriodChip(
                    label = stringResource(R.string.solar_system_period_today),
                    selected = chartPeriod == SolarChartPeriod.DAY,
                    onClick = { onPeriodSelected(SolarChartPeriod.DAY) },
                )
                PeriodChip(
                    label = stringResource(R.string.solar_system_period_week),
                    selected = chartPeriod == SolarChartPeriod.WEEK,
                    onClick = { onPeriodSelected(SolarChartPeriod.WEEK) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        EnergyChartSection(
            chartData = chartData,
            chartPeriod = chartPeriod,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BreakdownStat(
                value = breakdown.producedKwh,
                label = stringResource(R.string.solar_system_breakdown_produce),
            )
            BreakdownStat(
                value = breakdown.exportedKwh,
                label = stringResource(R.string.solar_system_breakdown_exported),
            )
            BreakdownStat(
                value = breakdown.batteryUsedKwh,
                label = stringResource(R.string.solar_system_breakdown_battery_used),
            )
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    SolarGlass.ChipBackground.copy(alpha = SolarGlass.ChipSurfaceAlpha)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = SolarGlass.TextOnGlassSecondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
}

@Composable
private fun BreakdownStat(
    value: Float,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(
                R.string.solar_system_energy_kwh_value,
                String.format(Locale.US, "%.0f", value),
            ),
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun EnergyChartSection(
    chartData: ConsumptionChartData,
    chartPeriod: SolarChartPeriod,
    modifier: Modifier = Modifier,
) {
    val maxValue = remember(chartData) {
        val maxProduced = chartData.producedSeries.maxOrNull() ?: 0f
        val maxConsumed = chartData.consumedSeries.maxOrNull() ?: 0f
        niceAxisMaximum(max(maxProduced, maxConsumed).coerceAtLeast(1f))
    }
    val producedLabel = stringResource(R.string.solar_system_chart_label_produced)
    val consumedLabel = stringResource(R.string.solar_system_chart_label_consumed)
    val zeroLabel = stringResource(R.string.solar_system_chart_axis_zero)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        ChartYAxis(
            maxValue = maxValue,
            producedLabel = producedLabel,
            consumedLabel = consumedLabel,
            zeroLabel = zeroLabel,
            modifier = Modifier
                .width(AxisWidth)
                .height(ChartHeight),
        )
        Column(modifier = Modifier.weight(1f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val metrics = remember(maxWidth, chartData) {
                    computeChartLayout(
                        chartWidth = maxWidth,
                        chartData = chartData,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    BidirectionalEnergyChart(
                        chartData = chartData,
                        metrics = metrics,
                        maxValue = maxValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ChartHeight),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ChartGroupLabels(
                        chartData = chartData,
                        chartPeriod = chartPeriod,
                        metrics = metrics,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartYAxis(
    maxValue: Float,
    producedLabel: String,
    consumedLabel: String,
    zeroLabel: String,
    modifier: Modifier = Modifier,
) {
    val midValue = maxValue / 2f
    val topTick = formatAxisTick(maxValue)
    val midTick = formatAxisTick(midValue)
    val bottomTick = formatAxisTick(maxValue)

    Column(
        modifier = modifier.padding(end = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = producedLabel,
                color = SolarGlass.ChartProduced,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            Text(
                text = topTick,
                color = SolarGlass.TextOnGlassMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = midTick,
            color = SolarGlass.TextOnGlassMuted,
            fontSize = 9.sp,
            textAlign = TextAlign.End,
        )
        Text(
            text = zeroLabel,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
        Text(
            text = midTick,
            color = SolarGlass.TextOnGlassMuted,
            fontSize = 9.sp,
            textAlign = TextAlign.End,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = bottomTick,
                color = SolarGlass.TextOnGlassMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.End,
            )
            Text(
                text = consumedLabel,
                color = SolarGlass.ChartConsumed,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ChartGroupLabels(
    chartData: ConsumptionChartData,
    chartPeriod: SolarChartPeriod,
    metrics: ChartLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val isDayView = chartPeriod == SolarChartPeriod.DAY
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(metrics.interGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chartData.groupLabels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatChartGroupLabel(label, isDayView),
                    color = SolarGlass.TextOnGlassMuted,
                    fontSize = if (isDayView) 9.sp else 10.sp,
                    fontWeight = if (isDayView) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun formatChartGroupLabel(
    label: String,
    isDayView: Boolean,
): String = if (isDayView) {
    stringResource(R.string.solar_system_chart_hour_label, label)
} else {
    label
}

@Composable
private fun BidirectionalEnergyChart(
    chartData: ConsumptionChartData,
    metrics: ChartLayoutMetrics,
    maxValue: Float,
    modifier: Modifier = Modifier,
) {
    val barCount = metrics.barCount
    val highlightIndices = remember(chartData) {
        buildHighlightIndices(
            produced = chartData.producedSeries,
            consumed = chartData.consumedSeries,
            barsPerGroup = metrics.barsPerGroup,
            barCount = barCount,
        )
    }

    Canvas(
        modifier = modifier.height(ChartHeight),
    ) {
        val centerY = size.height / 2f
        val halfHeight = centerY - 6.dp.toPx()
        val strokeWidth = BarStrokeWidth.toPx()
        val dotOuterRadius = DotOuterRadius.toPx()
        val dotInnerRadius = DotInnerRadius.toPx()
        val barDotGap = BarDotGap.toPx()

        drawChartGrid(
            metrics = metrics,
            centerY = centerY,
            halfHeight = halfHeight,
        )

        repeat(barCount) { index ->
            val centerX = metrics.barCenterX(index, density = this)

            val produced = chartData.producedSeries.getOrElse(index) { 0f }
            val consumed = chartData.consumedSeries.getOrElse(index) { 0f }

            if (produced > 0f) {
                val upHeight = (produced / maxValue) * halfHeight
                val highlight = index in highlightIndices.produced
                val upEnd = centerY - dotOuterRadius - barDotGap
                drawLine(
                    color = SolarGlass.ChartProduced.copy(
                        alpha = if (highlight) {
                            SolarGlass.ChartHighlightAlpha
                        } else {
                            SolarGlass.ChartFadedAlpha
                        },
                    ),
                    start = Offset(centerX, upEnd - upHeight),
                    end = Offset(centerX, upEnd),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            if (consumed > 0f) {
                val downHeight = (consumed / maxValue) * halfHeight
                val highlight = index in highlightIndices.consumed
                val downStart = centerY + dotOuterRadius + barDotGap
                drawLine(
                    color = SolarGlass.ChartConsumed.copy(
                        alpha = if (highlight) {
                            SolarGlass.ChartHighlightAlpha
                        } else {
                            SolarGlass.ChartFadedAlpha
                        },
                    ),
                    start = Offset(centerX, downStart),
                    end = Offset(centerX, downStart + downHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            val producedDominant = when {
                produced <= 0f && consumed <= 0f -> true
                else -> produced >= consumed
            }
            val dotOuterColor = if (producedDominant) {
                SolarGlass.ChartProduced.copy(alpha = 0.30f)
            } else {
                SolarGlass.ChartConsumed.copy(alpha = 0.30f)
            }
            val dotInnerColor = if (producedDominant) {
                SolarGlass.ChartProduced
            } else {
                SolarGlass.ChartConsumed
            }
            drawCircle(
                color = dotOuterColor,
                radius = dotOuterRadius,
                center = Offset(centerX, centerY),
            )
            drawCircle(
                color = dotInnerColor,
                radius = dotInnerRadius,
                center = Offset(centerX, centerY),
            )
        }
    }
}

private data class ChartLayoutMetrics(
    val groupCount: Int,
    val barsPerGroup: Int,
    val barCount: Int,
    val chartWidth: Dp,
    val interGap: Dp,
    val intraGap: Dp,
) {
    fun groupSlotWidth(density: androidx.compose.ui.unit.Density): Float {
        val widthPx = with(density) { chartWidth.toPx() }
        val interGapPx = with(density) { interGap.toPx() }
        return (widthPx - interGapPx * (groupCount - 1).coerceAtLeast(0)) / groupCount
    }

    fun barWidthPx(density: androidx.compose.ui.unit.Density): Float {
        val slot = groupSlotWidth(density)
        val intraGapPx = with(density) { intraGap.toPx() }
        return (slot - intraGapPx * (barsPerGroup - 1).coerceAtLeast(0)) / barsPerGroup
    }

    fun barLeft(index: Int, density: androidx.compose.ui.unit.Density): Float {
        val groupIndex = index / barsPerGroup
        val offsetInGroup = index % barsPerGroup
        val interGapPx = with(density) { interGap.toPx() }
        val intraGapPx = with(density) { intraGap.toPx() }
        val barWidth = barWidthPx(density)
        val groupStartX = groupIndex * (groupSlotWidth(density) + interGapPx)
        return groupStartX + offsetInGroup * (barWidth + intraGapPx)
    }

    fun barCenterX(index: Int, density: androidx.compose.ui.unit.Density): Float {
        val left = barLeft(index, density)
        return left + barWidthPx(density) / 2f
    }

    fun groupLeftX(groupIndex: Int, density: androidx.compose.ui.unit.Density): Float {
        val interGapPx = with(density) { interGap.toPx() }
        return groupIndex * (groupSlotWidth(density) + interGapPx)
    }

    fun groupDividerX(groupIndex: Int, density: androidx.compose.ui.unit.Density): Float {
        if (groupIndex <= 0) return 0f
        val interGapPx = with(density) { interGap.toPx() }
        return groupLeftX(groupIndex, density) - interGapPx / 2f
    }
}

private fun DrawScope.drawChartGrid(
    metrics: ChartLayoutMetrics,
    centerY: Float,
    halfHeight: Float,
) {
    val gridColor = SolarGlass.TextOnGlassMuted.copy(alpha = SolarGlass.ChartGridAlpha)
    val majorGridColor = SolarGlass.TextOnGlassMuted.copy(alpha = SolarGlass.ChartGridMajorAlpha)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
    val gridStroke = 1.dp.toPx()
    val dividerStroke = 1.dp.toPx()

    repeat(metrics.groupCount) { groupIndex ->
        if (groupIndex % 2 == 1) {
            val left = metrics.groupLeftX(groupIndex, density = this)
            val width = metrics.groupSlotWidth(this)
            drawRect(
                color = SolarGlass.TextOnGlassMuted.copy(alpha = SolarGlass.ChartSegmentAlpha),
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
            )
        }
    }

    val valueLevels = listOf(
        centerY - halfHeight,
        centerY - halfHeight / 2f,
        centerY,
        centerY + halfHeight / 2f,
        centerY + halfHeight,
    )
    valueLevels.forEachIndexed { index, y ->
        val isBaseline = index == 2
        drawLine(
            color = if (isBaseline) majorGridColor else gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = if (isBaseline) gridStroke * 1.2f else gridStroke,
            pathEffect = if (isBaseline) null else dashEffect,
        )
    }

    repeat(metrics.groupCount - 1) { groupIndex ->
        val dividerX = metrics.groupDividerX(groupIndex + 1, density = this)
        drawLine(
            color = majorGridColor,
            start = Offset(dividerX, 0f),
            end = Offset(dividerX, size.height),
            strokeWidth = dividerStroke,
            pathEffect = dashEffect,
        )
    }

    drawLine(
        color = majorGridColor.copy(alpha = SolarGlass.ChartGridAlpha),
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = gridStroke,
    )
    drawLine(
        color = majorGridColor.copy(alpha = SolarGlass.ChartGridAlpha),
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = gridStroke,
    )
}

private fun computeChartLayout(
    chartWidth: Dp,
    chartData: ConsumptionChartData,
): ChartLayoutMetrics {
    val groupCount = chartData.groupLabels.size.coerceAtLeast(1)
    val barsPerGroup = chartData.barsPerGroup.coerceAtLeast(1)
    val barCount = max(
        chartData.producedSeries.size,
        chartData.consumedSeries.size,
    ).coerceAtLeast(groupCount * barsPerGroup)

    return ChartLayoutMetrics(
        groupCount = groupCount,
        barsPerGroup = barsPerGroup,
        barCount = barCount,
        chartWidth = chartWidth,
        interGap = when {
            groupCount >= 8 -> 4.dp
            groupCount > 4 -> 6.dp
            else -> 8.dp
        },
        intraGap = if (groupCount >= 8) 2.dp else 3.dp,
    )
}

private data class ChartHighlightIndices(
    val produced: Set<Int>,
    val consumed: Set<Int>,
)

private fun buildHighlightIndices(
    produced: List<Float>,
    consumed: List<Float>,
    barsPerGroup: Int,
    barCount: Int,
): ChartHighlightIndices {
    val producedHighlights = mutableSetOf<Int>()
    val consumedHighlights = mutableSetOf<Int>()
    val groupCount = (barCount + barsPerGroup - 1) / barsPerGroup

    repeat(groupCount) { groupIndex ->
        val groupStart = groupIndex * barsPerGroup
        val groupEnd = minOf(groupStart + barsPerGroup, barCount)

        var maxProduced = 0f
        var maxProducedIndex = -1
        var maxConsumed = 0f
        var maxConsumedIndex = -1

        for (index in groupStart until groupEnd) {
            val prod = produced.getOrElse(index) { 0f }
            if (prod > maxProduced) {
                maxProduced = prod
                maxProducedIndex = index
            }
            val cons = consumed.getOrElse(index) { 0f }
            if (cons > maxConsumed) {
                maxConsumed = cons
                maxConsumedIndex = index
            }
        }

        if (maxProducedIndex >= 0) producedHighlights += maxProducedIndex
        if (maxConsumedIndex >= 0) consumedHighlights += maxConsumedIndex
    }

    return ChartHighlightIndices(
        produced = producedHighlights,
        consumed = consumedHighlights,
    )
}

@Composable
private fun formatAxisTick(value: Float): String = stringResource(
    R.string.solar_system_energy_kwh_value,
    String.format(Locale.US, "%.0f", value),
)

private fun niceAxisMaximum(rawMax: Float): Float {
    val magnitude = when {
        rawMax <= 5f -> 1f
        rawMax <= 20f -> 5f
        rawMax <= 50f -> 10f
        rawMax <= 200f -> 25f
        else -> 50f
    }
    return ceil(rawMax / magnitude) * magnitude
}
