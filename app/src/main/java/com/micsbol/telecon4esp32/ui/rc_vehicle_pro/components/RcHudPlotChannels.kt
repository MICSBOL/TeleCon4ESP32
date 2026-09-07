package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.formatEngineeringNumber
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import java.util.Locale
import kotlin.math.sin

const val RC_HUD_PLOT_HOLD_MS = 2_000L

const val HUD_PLOT_Y_MIN_DEFAULT = 0f
const val HUD_PLOT_Y_MAX_DEFAULT = 1f

data class RcHudPlotTraceStyle(
    val visible: Boolean = true,
    val colorArgb: Int,
    val lineStyle: PlotLineStyle = PlotLineStyle.LINE,
    val dashed: Boolean = false,
    val yMin: Float = HUD_PLOT_Y_MIN_DEFAULT,
    val yMax: Float = HUD_PLOT_Y_MAX_DEFAULT,
) {
    companion object {
        val PALETTE_ARGB: List<Int> = listOf(
            0xFF22FF66.toInt(),
            0xFF44E0FF.toInt(),
            0xFFFF6AD5.toInt(),
            0xFFFFE14A.toInt(),
            0xFF00FFFF.toInt(),
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFFFFFF00.toInt(),
            0xFFFF00FF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFF8000.toInt(),
            0xFF8080FF.toInt(),
        )

        fun defaultColorArgb(index: Int): Int =
            PALETTE_ARGB.getOrElse(index) { 0xFFFFFFFF.toInt() }

        fun defaults(count: Int = UserSettings.PLOT_LABEL_COUNT): List<RcHudPlotTraceStyle> =
            List(count) { index ->
                RcHudPlotTraceStyle(
                    visible = index < 3,
                    colorArgb = defaultColorArgb(index),
                    lineStyle = PlotLineStyle.LINE,
                    dashed = index == 1,
                )
            }
    }
}

fun rcHudPlotDisplaySeries(
    plotSeries: List<PlotData>,
    session: RcTelemetryPlotSession,
    routing: ChannelRouting = ChannelRouting.defaults(),
): List<PlotData> {
    val count = UserSettings.PLOT_LABEL_COUNT
    return List(count) { index ->
        val live = plotSeries.getOrNull(index)
        val channel = routing.sourceFor(TelemetrySink.plotAt(index))
        val points = rcHudResolvedPoints(
            channel = channel,
            overlay = session.controlChannels[channel],
            telemetry = live?.dataPoints.orEmpty(),
            demoIndex = channel.analogIndex() ?: index,
            elapsedSec = session.sessionElapsedSec,
        )
        PlotData(
            name = live?.name.orEmpty(),
            dataPoints = points,
            colorArgb = live?.colorArgb ?: RcHudPlotTraceStyle.defaultColorArgb(index),
        )
    }
}

fun rcHudRadarDisplaySeries(
    radarSeries: List<PlotData>,
    session: RcTelemetryPlotSession,
): List<PlotData> =
    TelemetryChannel.U8_SOURCES.mapIndexed { index, channel ->
        val live = radarSeries.getOrNull(index)
        val points = rcHudResolvedPoints(
            channel = channel,
            overlay = session.controlChannels[channel],
            telemetry = live?.dataPoints.orEmpty(),
            demoIndex = index,
            elapsedSec = session.sessionElapsedSec,
        )
        PlotData(
            name = live?.name ?: channel.name,
            dataPoints = points,
            colorArgb = live?.colorArgb ?: RcHudPlotTraceStyle.defaultColorArgb(index),
        )
    }

fun rcHudPlotChannelLabel(name: String, fallback: String): String =
    name.trim().ifBlank { fallback }

fun formatHudStickXy(x: Float, y: Float): String =
    "(%.2f,%.2f)".format(
        Locale.US,
        x.coerceIn(-1f, 1f),
        y.coerceIn(-1f, 1f),
    )

fun RcHudPlotTraceStyle.resolvedYRange(): Pair<Float, Float> {
    val min = if (yMin.isFinite()) yMin else HUD_PLOT_Y_MIN_DEFAULT
    val max = if (yMax.isFinite()) yMax else HUD_PLOT_Y_MAX_DEFAULT
    return if (max > min) min to max else min to (min + 1f)
}

fun hudPlotScaleTicks(min: Float, max: Float, count: Int = 3): List<Float> {
    val (lo, hi) = if (min.isFinite() && max.isFinite() && max > min) {
        min to max
    } else {
        HUD_PLOT_Y_MIN_DEFAULT to HUD_PLOT_Y_MAX_DEFAULT
    }
    if (count <= 1) return listOf(lo)
    val last = (count - 1).toFloat()
    return List(count) { index -> lo + (hi - lo) * index / last }
}

fun formatHudPlotScaleTick(value: Float): String =
    if (value.isFinite()) formatEngineeringNumber(value) else "0"

internal fun rcHudScaledChannelSamples(
    series: List<PlotData>,
    channel: TelemetryChannel,
    scale: Float,
    fallback: List<Float>,
): List<Float> {
    val points = series.getOrNull(channel.u8Index())?.dataPoints.orEmpty()
    if (points.isEmpty()) return fallback
    return points.map { value -> value * scale }
}

internal fun rcHudDemoSinusoid(
    channelIndex: Int,
    elapsedSec: Float,
    count: Int = RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT,
): List<Float> {
    val last = (count - 1).coerceAtLeast(1)
    val phase = elapsedSec * 2.4f + channelIndex * 0.85f
    return List(count) { index ->
        val t = phase + (2f * kotlin.math.PI.toFloat() * 1.25f * index / last)
        (sin(t) * 0.5f + 0.5f).coerceIn(0f, 1f)
    }
}

private fun rcHudResolvedPoints(
    channel: TelemetryChannel,
    overlay: List<Float>?,
    telemetry: List<Float>,
    demoIndex: Int,
    elapsedSec: Float,
): List<Float> = when {
    !overlay.isNullOrEmpty() -> overlay
    telemetry.size > 1 -> telemetry
    else -> rcHudDemoSinusoid(demoIndex, elapsedSec)
}
