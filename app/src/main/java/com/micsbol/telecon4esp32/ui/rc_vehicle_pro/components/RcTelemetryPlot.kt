package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.lineVertices
import com.micsbol.telecon4esp32.domain.model.stairVertices
import com.micsbol.telecon4esp32.domain.model.triangleContours
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.control_panel.ControlPanelRadarDisplay
import com.micsbol.telecon4esp32.ui.control_panel.RADAR_PLOT_HORIZON_FRACTION
import com.micsbol.telecon4esp32.ui.control_panel.RadarDisplaySettings
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private val PlotNeon = Color(0xFF22FF66)
private val PlotCommand = Color(0xFF44E0FF)
private val PlotPeak = Color(0xFFFFE14A)
private val PlotAvg = Color(0xFFE8EEF8)
private val PlotAccel = Color(0xFFFF6AD5)
private val PlotModeBarHeight = 26.dp
private val PlotLegendBarHeight = 22.dp
private const val ExampleSineMidKmh = 20f
private const val ExampleSineAmplitudeKmh = 14f
private const val ExampleSinePhaseStep = 0.14f

enum class RcTelemetryPlotMode {
    SPEED,
    BATTERY,
    TEMP,
    RADAR,
    ENVELOPE,
    STATS,
}

data class RcTelemetryPlotSession(
    val speed: List<Float> = emptyList(),
    val command: List<Float> = emptyList(),
    val battery: List<Float> = emptyList(),
    val temp: List<Float> = emptyList(),
    val steerX: List<Float> = emptyList(),
    val throttleY: List<Float> = emptyList(),
    val sessionPeak: Float = 0f,
    val sessionAvg: Float = 0f,
    val sessionDistanceKm: Float = 0f,
    val sessionMinBattery: Float = 0f,
    val sessionMaxTemp: Float = 0f,
    val sessionElapsedSec: Float = 0f,
)

@Composable
internal fun rememberTelemetryPlotSession(
    speedKmh: Float,
    batteryPercent: Int,
    motorTempCelsius: Int,
    throttleY: Float,
    steerX: Float,
    fromTelemetry: Boolean = false,
): RcTelemetryPlotSession {
    val latestSpeed by rememberUpdatedState(speedKmh)
    val latestBattery by rememberUpdatedState(batteryPercent.toFloat())
    val latestTemp by rememberUpdatedState(motorTempCelsius.toFloat())
    val latestThrottle by rememberUpdatedState(throttleY)
    val latestSteer by rememberUpdatedState(steerX)
    val latestFromTelemetry by rememberUpdatedState(fromTelemetry)
    var session by remember { mutableStateOf(RcTelemetryPlotSession()) }
    var phase by remember { mutableFloatStateOf(0f) }
    val speedBuf = remember { ArrayDeque<Float>() }
    val commandBuf = remember { ArrayDeque<Float>() }
    val batteryBuf = remember { ArrayDeque<Float>() }
    val tempBuf = remember { ArrayDeque<Float>() }
    val steerBuf = remember { ArrayDeque<Float>() }
    val throttleBuf = remember { ArrayDeque<Float>() }

    LaunchedEffect(Unit) {
        var collectingLive = false
        var sessionSum = 0.0
        var sessionCount = 0
        var sessionPeak = 0f
        var sessionDistance = 0f
        var sessionMinBattery = Float.POSITIVE_INFINITY
        var sessionMaxTemp = Float.NEGATIVE_INFINITY
        var elapsedSec = 0f
        val maxPoints = RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT
        val dt = RcVehicleProLayout.TELEMETRY_PLOT_INTERVAL_SEC
        fun push(buffer: ArrayDeque<Float>, value: Float) {
            buffer.addLast(value)
            while (buffer.size > maxPoints) buffer.removeFirst()
        }
        while (true) {
            if (latestFromTelemetry && !collectingLive) {
                collectingLive = true
                speedBuf.clear()
                commandBuf.clear()
                batteryBuf.clear()
                tempBuf.clear()
                steerBuf.clear()
                throttleBuf.clear()
                sessionSum = 0.0
                sessionCount = 0
                sessionPeak = 0f
                sessionDistance = 0f
                sessionMinBattery = Float.POSITIVE_INFINITY
                sessionMaxTemp = Float.NEGATIVE_INFINITY
                elapsedSec = 0f
            }
            val speed = if (latestFromTelemetry) {
                latestSpeed
            } else {
                collectingLive = false
                phase += ExampleSinePhaseStep
                exampleSineValue(phase)
            }
            val command = RcVehicleProLayout.commandedSpeedKmh(latestThrottle)
            if (latestFromTelemetry) {
                push(speedBuf, speed)
            } else {
                speedBuf.clear()
                speedBuf.addAll(exampleSineWindow(phase))
            }
            push(commandBuf, command)
            push(batteryBuf, latestBattery)
            push(tempBuf, latestTemp)
            push(steerBuf, latestSteer.coerceIn(-1f, 1f))
            push(throttleBuf, latestThrottle.coerceIn(-1f, 1f))
            sessionSum += speed
            sessionCount += 1
            sessionPeak = maxOf(sessionPeak, speed)
            sessionDistance += abs(speed) * (dt / 3600f)
            sessionMinBattery = minOf(sessionMinBattery, latestBattery)
            sessionMaxTemp = maxOf(sessionMaxTemp, latestTemp)
            elapsedSec += dt
            session = RcTelemetryPlotSession(
                speed = speedBuf.toList(),
                command = commandBuf.toList(),
                battery = batteryBuf.toList(),
                temp = tempBuf.toList(),
                steerX = steerBuf.toList(),
                throttleY = throttleBuf.toList(),
                sessionPeak = sessionPeak,
                sessionAvg = if (sessionCount == 0) 0f else (sessionSum / sessionCount).toFloat(),
                sessionDistanceKm = sessionDistance,
                sessionMinBattery = if (sessionMinBattery.isFinite()) sessionMinBattery else 0f,
                sessionMaxTemp = if (sessionMaxTemp.isFinite()) sessionMaxTemp else 0f,
                sessionElapsedSec = elapsedSec,
            )
            delay(RcVehicleProLayout.TELEMETRY_PLOT_INTERVAL_MS)
        }
    }
    return session
}

private fun exampleSineValue(phase: Float): Float {
    return ExampleSineMidKmh + ExampleSineAmplitudeKmh * sin(phase)
}

private fun exampleSineWindow(phase: Float): List<Float> {
    val last = (RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT - 1).coerceAtLeast(1)
    return List(RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT) { index ->
        val t = (2f * kotlin.math.PI.toFloat() * 1.25f * index / last) + phase
        exampleSineValue(t)
    }
}

@Composable
fun RcTelemetryPlotPanel(
    session: RcTelemetryPlotSession,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    plotWidth: Dp = RcVehicleProLayout.TelemetryPlotWidth,
    plotHeight: Dp = RcVehicleProLayout.TelemetryPlotHeight,
    radarSeries: List<PlotData> = emptyList(),
    plotSeries: List<PlotData> = emptyList(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
) {
    RcHudCollapsibleToEdge(
        towardEnd = false,
        showContentDescription = stringResource(R.string.rc_vehicle_hud_show_telemetry_plot),
        hideContentDescription = stringResource(R.string.rc_vehicle_hud_hide_telemetry_plot),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        fillWidth = false,
        swipeEntireContent = true,
        collapsedIcon = Icons.AutoMirrored.Filled.ShowChart,
    ) {
        RcGlassCard(
            surfaceAlpha = RcVehicleProGlass.PLOT_BACKGROUND_ALPHA,
            accentEdge = RcGlassAccentEdge.END,
            contentPadding = PaddingValues(
                start = 2.dp,
                top = 6.dp,
                end = 6.dp,
                bottom = 8.dp,
            ),
            fillWidth = false,
        ) {
            RcTelemetryPlotBody(
                session = session,
                plotWidth = plotWidth,
                plotHeight = plotHeight,
                radarSeries = radarSeries,
                plotSeries = plotSeries,
                channelRouting = channelRouting,
                onRadarSourceChange = onRadarSourceChange,
                onPlotLabelChange = onPlotLabelChange,
                onPlotChannelChange = onPlotChannelChange,
            )
        }
    }
}

@Composable
private fun RcTelemetryPlotBody(
    session: RcTelemetryPlotSession,
    plotWidth: Dp,
    plotHeight: Dp,
    radarSeries: List<PlotData>,
    plotSeries: List<PlotData>,
    channelRouting: ChannelRouting,
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit,
    onPlotLabelChange: (Int, String) -> Unit,
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(RcTelemetryPlotMode.SPEED) }
    var modeBarVisible by rememberSaveable { mutableStateOf(true) }
    var legendVisible by rememberSaveable { mutableStateOf(true) }
    var radarSettings by remember { mutableStateOf(RadarDisplaySettings()) }
    var showPlotSettings by rememberSaveable { mutableStateOf(false) }
    var traceStyles by rememberSaveable(stateSaver = RcHudPlotTraceStyleListSaver) {
        mutableStateOf(RcHudPlotTraceStyle.defaults())
    }
    val displaySeries = remember(plotSeries, session) {
        rcHudPlotDisplaySeries(plotSeries, session)
    }
    val plotDescription = stringResource(
        when (mode) {
            RcTelemetryPlotMode.SPEED -> R.string.rc_vehicle_telemetry_plot
            RcTelemetryPlotMode.BATTERY -> R.string.rc_vehicle_plot_mode_battery
            RcTelemetryPlotMode.TEMP -> R.string.rc_vehicle_plot_mode_temp
            RcTelemetryPlotMode.RADAR -> R.string.rc_vehicle_plot_mode_radar
            RcTelemetryPlotMode.ENVELOPE -> R.string.rc_vehicle_plot_mode_envelope
            RcTelemetryPlotMode.STATS -> R.string.rc_vehicle_plot_mode_stats
        },
    )
    val legendShown = mode == RcTelemetryPlotMode.SPEED && legendVisible
    val graphHeight = plotHeight +
        (if (modeBarVisible) 0.dp else PlotModeBarHeight) -
        (if (legendShown) PlotLegendBarHeight else 0.dp)
    val hideModesLabel = stringResource(R.string.rc_vehicle_plot_hide_mode_bar)
    val showModesLabel = stringResource(R.string.rc_vehicle_plot_show_mode_bar)
    val hideLegendLabel = stringResource(R.string.rc_vehicle_plot_hide_legend)
    val showLegendLabel = stringResource(R.string.rc_vehicle_plot_show_legend)
    Column(modifier = Modifier.width(plotWidth)) {
        if (modeBarVisible) {
            RcTelemetryPlotModeRow(
                selected = mode,
                onSelect = { mode = it },
                onHide = { modeBarVisible = false },
                hideContentDescription = hideModesLabel,
            )
        }
        if (legendShown) {
            RcHudPlotLegendRow(
                series = displaySeries,
                traceStyles = traceStyles,
                onHide = { legendVisible = false },
                hideContentDescription = hideLegendLabel,
            )
        }
        Box(
            modifier = Modifier
                .size(plotWidth, graphHeight)
                .semantics { contentDescription = plotDescription },
        ) {
            when (mode) {
                RcTelemetryPlotMode.SPEED -> RcHudChannelPlot(
                    series = displaySeries,
                    traceStyles = traceStyles,
                    onOpenSettings = { showPlotSettings = true },
                )
                RcTelemetryPlotMode.BATTERY -> RcTelemetryScopePlot(
                    samples = session.battery,
                    yFloor = 100f,
                )
                RcTelemetryPlotMode.TEMP -> RcTelemetryScopePlot(
                    samples = session.temp,
                    yFloor = 80f,
                )
                RcTelemetryPlotMode.RADAR -> RcHudRadarPlot(
                    series = radarSeries,
                    settings = radarSettings.copy(
                        angleSeriesIndex = channelRouting
                            .sourceFor(TelemetrySink.RADAR_ANGLE)
                            .u8Index(),
                        rangeSeriesIndex = channelRouting
                            .sourceFor(TelemetrySink.RADAR_RANGE)
                            .u8Index(),
                    ),
                    onSettingsChange = { updated ->
                        radarSettings = updated
                        val angle = TelemetryChannel.u8At(updated.angleSeriesIndex)
                        val range = TelemetryChannel.u8At(updated.rangeSeriesIndex)
                        if (angle != channelRouting.sourceFor(TelemetrySink.RADAR_ANGLE)) {
                            onRadarSourceChange(TelemetrySink.RADAR_ANGLE, angle)
                        }
                        if (range != channelRouting.sourceFor(TelemetrySink.RADAR_RANGE)) {
                            onRadarSourceChange(TelemetrySink.RADAR_RANGE, range)
                        }
                    },
                )
                RcTelemetryPlotMode.ENVELOPE -> RcStickEnvelopePlot(
                    steerX = session.steerX,
                    throttleY = session.throttleY,
                )
                RcTelemetryPlotMode.STATS -> RcTelemetryPlotStats(session = session)
            }
            if (!modeBarVisible) {
                PlotModeBarChevron(
                    expand = true,
                    contentDescription = showModesLabel,
                    onClick = { modeBarVisible = true },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            if (mode == RcTelemetryPlotMode.SPEED && !legendVisible) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = showLegendLabel,
                    tint = brandPrimary(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = if (modeBarVisible) 2.dp else 20.dp)
                        .size(18.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = { legendVisible = true },
                        )
                        .semantics { role = Role.Button },
                )
            }
        }
    }

    if (showPlotSettings) {
        RcHudPlotSettingsDialog(
            series = displaySeries,
            traceStyles = traceStyles,
            channelRouting = channelRouting,
            onTraceStylesChange = { traceStyles = it },
            onPlotLabelChange = onPlotLabelChange,
            onPlotChannelChange = onPlotChannelChange,
            onDismiss = { showPlotSettings = false },
        )
    }
}

@Composable
private fun RcTelemetryPlotModeRow(
    selected: RcTelemetryPlotMode,
    onSelect: (RcTelemetryPlotMode) -> Unit,
    onHide: () -> Unit,
    hideContentDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RcTelemetryPlotMode.entries.forEach { mode ->
                RcPlotModeChip(
                    label = stringResource(mode.labelRes()),
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
        PlotModeBarChevron(
            expand = false,
            contentDescription = hideContentDescription,
            onClick = onHide,
        )
    }
}

@Composable
private fun RcHudPlotLegendRow(
    series: List<PlotData>,
    traceStyles: List<RcHudPlotTraceStyle>,
    onHide: () -> Unit,
    hideContentDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            series.forEachIndexed { index, plotData ->
                val style = traceStyles.getOrElse(index) {
                    RcHudPlotTraceStyle(
                        visible = true,
                        colorArgb = plotData.colorArgb,
                    )
                }
                if (!style.visible) return@forEachIndexed
                val color = Color(style.colorArgb)
                val fallback = stringResource(
                    R.string.rc_vehicle_plot_settings_channel,
                    index + 1,
                )
                val label = rcHudPlotChannelLabel(plotData.name, fallback)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .background(color, RoundedCornerShape(1.dp)),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
        PlotModeBarChevron(
            expand = false,
            contentDescription = hideContentDescription,
            onClick = onHide,
        )
    }
}

@Composable
private fun PlotModeBarChevron(
    expand: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = if (expand) {
            Icons.Filled.KeyboardDoubleArrowDown
        } else {
            Icons.Filled.KeyboardDoubleArrowUp
        },
        contentDescription = contentDescription,
        tint = brandPrimary(),
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .size(18.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button },
    )
}

@Composable
private fun RcPlotModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = brandPrimary()
    val background = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.ACTION_CHIP_ALPHA)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) accent else Color.White.copy(alpha = 0.84f),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.28f) else background)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun RcTelemetryPlotStats(session: RcTelemetryPlotSession) {
    val minutes = RcVehicleProLayout.elapsedMinutes(session.sessionElapsedSec)
    val seconds = RcVehicleProLayout.elapsedSecondsPart(session.sessionElapsedSec)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_peak),
            value = stringResource(R.string.rc_vehicle_speed_value, session.sessionPeak),
        )
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_avg),
            value = stringResource(R.string.rc_vehicle_speed_value, session.sessionAvg),
        )
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_distance),
            value = stringResource(R.string.rc_vehicle_plot_stats_distance_value, session.sessionDistanceKm),
        )
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_battery),
            value = stringResource(R.string.rc_vehicle_battery_value, session.sessionMinBattery.toInt()),
        )
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_temp),
            value = stringResource(R.string.rc_vehicle_motor_temp_value, session.sessionMaxTemp.toInt()),
        )
        RcPlotStatRow(
            label = stringResource(R.string.rc_vehicle_plot_stats_time),
            value = stringResource(R.string.rc_vehicle_plot_stats_time_value, minutes, seconds),
        )
    }
}

@Composable
private fun RcPlotStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PlotNeon,
        )
    }
}

@Composable
private fun RcHudChannelPlot(
    series: List<PlotData>,
    traceStyles: List<RcHudPlotTraceStyle>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onOpenSettings) {
                detectHoldMillis(RC_HUD_PLOT_HOLD_MS, onOpenSettings)
            },
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas
        val yLabelGutter = 16.dp.toPx()
        val xLabelGutter = 16.dp.toPx()
        val dataPad = 10.dp.toPx()
        val plotLeft = yLabelGutter
        val plotRight = width - 4.dp.toPx()
        val plotTop = dataPad
        val plotBottom = height - xLabelGutter
        val plotWidthPx = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeightPx = (plotBottom - plotTop).coerceAtLeast(1f)
        val sampleCount = series.maxOfOrNull { it.dataPoints.size } ?: 0
        val timeMaxSec = ((sampleCount - 1).coerceAtLeast(1)) *
            RcVehicleProLayout.TELEMETRY_PLOT_INTERVAL_SEC
        drawPerspectiveGrid(width, height)
        fun dataY(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return plotTop + plotHeightPx * (1f - t)
        }
        drawPlotAxes(
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotTop = plotTop,
            plotBottom = plotBottom,
            plotWidth = plotWidthPx,
            peak = 1f,
            timeMaxSec = timeMaxSec,
            dataY = ::dataY,
        )
        val firstVisible = series.indices.firstOrNull { index ->
            traceStyles.getOrElse(index) { RcHudPlotTraceStyle.defaults()[index.coerceAtMost(3)] }.visible &&
                series[index].dataPoints.isNotEmpty()
        }
        series.forEachIndexed { index, plotData ->
            val style = traceStyles.getOrElse(index) {
                RcHudPlotTraceStyle(
                    visible = true,
                    colorArgb = plotData.colorArgb,
                )
            }
            if (!style.visible || plotData.dataPoints.isEmpty()) return@forEachIndexed
            val color = Color(style.colorArgb)
            val glow = index == firstVisible && style.lineStyle == PlotLineStyle.LINE && !style.dashed
            drawHudTrace(
                values = plotData.dataPoints,
                color = color,
                style = style.lineStyle,
                dashed = style.dashed,
                glow = glow,
                plotLeft = plotLeft,
                plotTop = plotTop,
                plotWidth = plotWidthPx,
                plotHeight = plotHeightPx,
            )
        }
    }
}

private fun DrawScope.drawHudTrace(
    values: List<Float>,
    color: Color,
    style: PlotLineStyle,
    dashed: Boolean,
    glow: Boolean,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float,
) {
    when (style) {
        PlotLineStyle.TRIANGLE -> {
            triangleContours(values, plotWidth, plotHeight).forEach { contour ->
                if (contour.size < 3) return@forEach
                val path = Path()
                contour.forEachIndexed { i, vertex ->
                    val x = plotLeft + vertex.x
                    val y = plotTop + vertex.y
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = color.copy(alpha = 0.55f), style = Fill)
                drawPath(path, color = color, style = Stroke(width = 1.2f))
            }
        }
        PlotLineStyle.STAIR, PlotLineStyle.LINE -> {
            val vertices = if (style == PlotLineStyle.STAIR) {
                stairVertices(values, plotWidth, plotHeight)
            } else {
                lineVertices(values, plotWidth, plotHeight)
            }
            if (vertices.isEmpty()) return
            val path = Path()
            vertices.forEachIndexed { i, vertex ->
                val x = plotLeft + vertex.x
                val y = plotTop + vertex.y
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (glow) {
                val fill = Path()
                fill.addPath(path)
                fill.lineTo(plotLeft + vertices.last().x, plotTop + plotHeight)
                fill.lineTo(plotLeft + vertices.first().x, plotTop + plotHeight)
                fill.close()
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.03f)),
                        startY = plotTop,
                        endY = plotTop + plotHeight,
                    ),
                )
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.16f),
                    style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.40f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = if (glow) 2.2f else 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null,
                ),
            )
        }
    }
}

@Composable
private fun RcTelemetryScopePlot(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    overlay: List<Float> = emptyList(),
    accel: List<Float> = emptyList(),
    markerHigh: Float? = null,
    markerMid: Float? = null,
    yFloor: Float = 0.1f,
) {
    val sampleCount = samples.size
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val yLabelGutter = 16.dp.toPx()
        val xLabelGutter = 16.dp.toPx()
        val dataPad = 10.dp.toPx()
        val plotLeft = yLabelGutter
        val plotRight = width - 4.dp.toPx()
        val plotTop = dataPad
        val plotBottom = height - xLabelGutter
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
        val overlayMax = overlay.maxOrNull() ?: 0f
        val peak = maxOf(
            samples.maxOrNull() ?: 0f,
            overlayMax,
            markerHigh ?: 0f,
            yFloor,
        ).coerceAtLeast(0.1f)
        val timeMaxSec = ((sampleCount - 1).coerceAtLeast(1)) *
            RcVehicleProLayout.TELEMETRY_PLOT_INTERVAL_SEC
        drawPerspectiveGrid(width, height)

        fun dataX(index: Int, lastIndex: Int): Float {
            return if (lastIndex <= 0) plotLeft else plotLeft + plotWidth * index / lastIndex
        }

        fun dataY(value: Float): Float {
            val t = (value / peak).coerceIn(0f, 1f)
            return plotTop + plotHeight * (1f - t)
        }

        fun drawAxesAndTicks() {
            drawPlotAxes(
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                plotWidth = plotWidth,
                peak = peak,
                timeMaxSec = timeMaxSec,
                dataY = ::dataY,
            )
        }

        if (sampleCount == 0) {
            drawAxesAndTicks()
            return@Canvas
        }

        val lastIndex = (sampleCount - 1).coerceAtLeast(1)
        drawFilledTrace(samples, lastIndex, plotBottom, PlotNeon, ::dataX, ::dataY)
        drawAxesAndTicks()
        markerHigh?.let { drawMarkerLine(it, PlotPeak, plotLeft, plotRight, ::dataY) }
        markerMid?.let { drawMarkerLine(it, PlotAvg, plotLeft, plotRight, ::dataY) }
        if (overlay.isNotEmpty()) {
            drawLineTrace(
                values = overlay,
                lastIndex = (overlay.size - 1).coerceAtLeast(1),
                color = PlotCommand,
                stroke = 2f,
                dashed = true,
                dataX = ::dataX,
                dataY = ::dataY,
            )
        }
        if (accel.isNotEmpty()) {
            val accelPeak = accel.maxOf { abs(it) }.coerceAtLeast(0.1f)
            fun accelY(value: Float): Float {
                val t = ((value / accelPeak).coerceIn(-1f, 1f) + 1f) / 2f
                return plotTop + plotHeight * (1f - t)
            }
            drawLine(
                color = PlotAccel.copy(alpha = 0.28f),
                start = Offset(plotLeft, accelY(0f)),
                end = Offset(plotRight, accelY(0f)),
                strokeWidth = 1f,
            )
            drawLineTrace(
                values = accel,
                lastIndex = (accel.size - 1).coerceAtLeast(1),
                color = PlotAccel,
                stroke = 1.6f,
                dashed = false,
                dataX = ::dataX,
                dataY = ::accelY,
            )
        }
        drawGlowingTrace(samples, lastIndex, PlotNeon, ::dataX, ::dataY)
    }
}

@Composable
private fun RcHudRadarPlot(
    series: List<PlotData>,
    settings: RadarDisplaySettings,
    onSettingsChange: (RadarDisplaySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    ControlPanelRadarDisplay(
        series = series,
        settings = settings,
        onSettingsChange = onSettingsChange,
        accent = PlotNeon,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun RcStickEnvelopePlot(
    steerX: List<Float>,
    throttleY: List<Float>,
    modifier: Modifier = Modifier,
) {
    val count = minOf(steerX.size, throttleY.size)
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas
        drawPerspectiveGrid(width, height)
        val pad = 18.dp.toPx()
        val left = pad
        val top = pad
        val right = width - 8.dp.toPx()
        val bottom = height - pad
        val plotW = (right - left).coerceAtLeast(1f)
        val plotH = (bottom - top).coerceAtLeast(1f)
        val cx = left + plotW / 2f
        val cy = top + plotH / 2f
        val axis = PlotNeon.copy(alpha = 0.55f)
        drawLine(axis, Offset(left, cy), Offset(right, cy), 1.4f)
        drawLine(axis, Offset(cx, top), Offset(cx, bottom), 1.4f)
        if (count == 0) return@Canvas
        fun mapX(value: Float) = cx + (value.coerceIn(-1f, 1f) * plotW / 2f)
        fun mapY(value: Float) = cy - (value.coerceIn(-1f, 1f) * plotH / 2f)
        val trail = Path()
        for (index in 0 until count) {
            val x = mapX(steerX[index])
            val y = mapY(throttleY[index])
            if (index == 0) trail.moveTo(x, y) else trail.lineTo(x, y)
        }
        drawPath(
            path = trail,
            color = PlotNeon.copy(alpha = 0.22f),
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = trail,
            color = PlotNeon,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val lastX = mapX(steerX.last())
        val lastY = mapY(throttleY.last())
        drawCircle(color = PlotCommand, radius = 6.5f, center = Offset(lastX, lastY))
        drawCircle(color = Color.White, radius = 2.6f, center = Offset(lastX, lastY))
    }
}

private fun DrawScope.drawPerspectiveGrid(width: Float, height: Float) {
    val horizonY = height * RADAR_PLOT_HORIZON_FRACTION
    val vanish = Offset(width * 0.50f, horizonY)
    val radialCount = 22
    val halfSpread = width * 2.15f
    val radialHalf = radialCount / 2f
    for (i in 0..radialCount) {
        val sideT = abs(i - radialHalf) / radialHalf
        val xBottom = vanish.x + (i - radialHalf) / radialHalf * halfSpread
        drawLine(
            color = PlotNeon.copy(alpha = 0.16f + 0.18f * (1f - sideT)),
            start = vanish,
            end = Offset(xBottom, height + 8f),
            strokeWidth = 1.2f,
        )
    }
    val depthCount = 14
    for (i in 1..depthCount) {
        val persp = (i.toFloat() / depthCount).pow(1.85f)
        val y = horizonY + (height - horizonY) * persp
        drawLine(
            color = PlotNeon.copy(alpha = 0.12f + 0.22f * (1f - persp)),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.15f,
        )
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
            startY = horizonY,
            endY = height,
        ),
    )
}

private fun DrawScope.drawPlotAxes(
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    plotWidth: Float,
    peak: Float,
    timeMaxSec: Float,
    dataY: (Float) -> Float,
) {
    val axisColor = PlotNeon.copy(alpha = 0.85f)
    val tickColor = PlotNeon.copy(alpha = 0.70f)
    val axisStroke = 1.5f
    val tickLen = 4.dp.toPx()
    drawLine(axisColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), axisStroke)
    drawLine(axisColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), axisStroke)
    val yTicks = axisTicks(peak)
    val xTicks = axisTicks(timeMaxSec)
    yTicks.forEach { value ->
        val y = dataY(value)
        drawLine(tickColor, Offset(plotLeft, y), Offset(plotLeft + tickLen, y), 1.2f)
    }
    xTicks.forEach { seconds ->
        val x = plotLeft + plotWidth * (seconds / timeMaxSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
        drawLine(tickColor, Offset(x, plotBottom), Offset(x, plotBottom - tickLen), 1.2f)
    }
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val labelPaint = android.graphics.Paint().apply {
            color = PlotNeon.toArgb()
            textSize = 8.dp.toPx()
            isAntiAlias = true
            isFakeBoldText = true
        }
        labelPaint.textAlign = android.graphics.Paint.Align.RIGHT
        yTicks.forEach { value ->
            val y = dataY(value)
            native.drawText(
                formatAxisTick(value),
                plotLeft - 2.dp.toPx(),
                y + labelPaint.textSize * 0.35f,
                labelPaint,
            )
        }
        labelPaint.textAlign = android.graphics.Paint.Align.CENTER
        xTicks.forEach { seconds ->
            val x = plotLeft + plotWidth * (seconds / timeMaxSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
            native.drawText(
                formatAxisTick(seconds),
                x,
                plotBottom + 11.dp.toPx(),
                labelPaint,
            )
        }
    }
}

private fun DrawScope.drawFilledTrace(
    samples: List<Float>,
    lastIndex: Int,
    plotBottom: Float,
    color: Color,
    dataX: (Int, Int) -> Float,
    dataY: (Float) -> Float,
) {
    val fillPath = Path()
    samples.forEachIndexed { index, value ->
        val x = dataX(index, lastIndex)
        val y = dataY(value)
        if (index == 0) {
            fillPath.moveTo(x, plotBottom)
            fillPath.lineTo(x, y)
        } else {
            fillPath.lineTo(x, y)
        }
    }
    fillPath.lineTo(dataX(samples.lastIndex.coerceAtLeast(0), lastIndex), plotBottom)
    fillPath.close()
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.03f)),
            startY = 0f,
            endY = plotBottom,
        ),
    )
}

private fun DrawScope.drawGlowingTrace(
    samples: List<Float>,
    lastIndex: Int,
    color: Color,
    dataX: (Int, Int) -> Float,
    dataY: (Float) -> Float,
) {
    val linePath = Path()
    samples.forEachIndexed { index, value ->
        val x = dataX(index, lastIndex)
        val y = dataY(value)
        if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
    }
    drawPath(
        path = linePath,
        color = color.copy(alpha = 0.16f),
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = linePath,
        color = color.copy(alpha = 0.40f),
        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = linePath,
        color = color,
        style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawLineTrace(
    values: List<Float>,
    lastIndex: Int,
    color: Color,
    stroke: Float,
    dashed: Boolean,
    dataX: (Int, Int) -> Float,
    dataY: (Float) -> Float,
) {
    val path = Path()
    values.forEachIndexed { index, value ->
        val x = dataX(index, lastIndex)
        val y = dataY(value)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null,
        ),
    )
}

private fun DrawScope.drawMarkerLine(
    value: Float,
    color: Color,
    plotLeft: Float,
    plotRight: Float,
    dataY: (Float) -> Float,
) {
    val y = dataY(value)
    drawLine(
        color = color.copy(alpha = 0.85f),
        start = Offset(plotLeft, y),
        end = Offset(plotRight, y),
        strokeWidth = 1.3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f),
    )
}

private fun RcTelemetryPlotMode.labelRes(): Int = when (this) {
    RcTelemetryPlotMode.SPEED -> R.string.rc_vehicle_plot_mode_speed
    RcTelemetryPlotMode.BATTERY -> R.string.rc_vehicle_plot_mode_battery
    RcTelemetryPlotMode.TEMP -> R.string.rc_vehicle_plot_mode_temp
    RcTelemetryPlotMode.RADAR -> R.string.rc_vehicle_plot_mode_radar
    RcTelemetryPlotMode.ENVELOPE -> R.string.rc_vehicle_plot_mode_envelope
    RcTelemetryPlotMode.STATS -> R.string.rc_vehicle_plot_mode_stats
}

private fun axisTicks(max: Float, targetCount: Int = 4): List<Float> {
    if (max <= 0f) return listOf(0f)
    val raw = max / targetCount.toFloat()
    val mag = 10f.pow(floor(log10(raw.toDouble())).toFloat())
    val residual = raw / mag
    val step = when {
        residual <= 1f -> 1f
        residual <= 2f -> 2f
        residual <= 5f -> 5f
        else -> 10f
    } * mag
    val ticks = mutableListOf(0f)
    var value = step
    while (value < max - step * 0.2f) {
        ticks.add(value)
        value += step
    }
    if (ticks.last() != max && max - ticks.last() > step * 0.75f) {
        ticks.add(max)
    }
    return ticks
}

private fun formatAxisTick(value: Float): String {
    return if (abs(value) < 0.0001f) {
        "0"
    } else if (abs(value - value.toInt()) < 0.05f) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}

private val RcHudPlotTraceStyleListSaver = Saver<List<RcHudPlotTraceStyle>, String>(
    save = { styles ->
        styles.joinToString(";") { style ->
            listOf(
                if (style.visible) "1" else "0",
                style.colorArgb.toString(),
                style.lineStyle.name,
                if (style.dashed) "1" else "0",
            ).joinToString(",")
        }
    },
    restore = { encoded ->
        val parsed = encoded.split(';').mapNotNull { token ->
            val parts = token.split(',')
            if (parts.size < 4) return@mapNotNull null
            RcHudPlotTraceStyle(
                visible = parts[0] == "1",
                colorArgb = parts[1].toIntOrNull() ?: RcHudPlotTraceStyle.defaultColorArgb(0),
                lineStyle = PlotLineStyle.entries.find { it.name == parts[2] } ?: PlotLineStyle.LINE,
                dashed = parts[3] == "1",
            )
        }
        List(UserSettings.PLOT_LABEL_COUNT) { index ->
            parsed.getOrElse(index) { RcHudPlotTraceStyle.defaults()[index] }
        }
    },
)

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectHoldMillis(
    durationMs: Long,
    onHold: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var cancelled = false
        val up = withTimeoutOrNull(durationMs) {
            val event = waitForUpOrCancellation()
            if (event == null) cancelled = true
            event
        }
        if (up == null && !cancelled) {
            onHold()
            waitForUpOrCancellation()
        }
    }
}
