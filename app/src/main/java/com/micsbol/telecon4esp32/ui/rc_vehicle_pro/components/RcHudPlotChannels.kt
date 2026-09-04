package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import kotlin.math.abs

const val RC_HUD_PLOT_HOLD_MS = 2_000L

data class RcHudPlotTraceStyle(
    val visible: Boolean = true,
    val colorArgb: Int,
    val lineStyle: PlotLineStyle = PlotLineStyle.LINE,
    val dashed: Boolean = false,
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
): List<PlotData> {
    val count = UserSettings.PLOT_LABEL_COUNT
    val live = plotSeries.take(count)
    if (live.any { it.dataPoints.size > 1 }) {
        return List(count) { index ->
            live.getOrNull(index) ?: PlotData(
                name = "",
                dataPoints = emptyList(),
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(index),
            )
        }
    }
    val demo = demoHudPlotSeries(session, count)
    return demo.mapIndexed { index, plotData ->
        plotData.copy(name = live.getOrNull(index)?.name.orEmpty())
    }
}

fun rcHudPlotChannelLabel(name: String, fallback: String): String =
    name.trim().ifBlank { fallback }

private fun demoHudPlotSeries(
    session: RcTelemetryPlotSession,
    count: Int,
): List<PlotData> {
    val speedPeak = maxOf(session.speed.maxOrNull() ?: 0f, 40f, 0.1f)
    val accel = RcVehicleProLayout.accelSamples(session.speed)
    val accelPeak = accel.maxOfOrNull { abs(it) }?.coerceAtLeast(0.1f) ?: 0.1f
    val traces = listOf(
        PlotData(
            name = "",
            dataPoints = session.speed.map { (it / speedPeak).coerceIn(0f, 1f) },
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(0),
        ),
        PlotData(
            name = "",
            dataPoints = session.command.map { (it / speedPeak).coerceIn(0f, 1f) },
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(1),
        ),
        PlotData(
            name = "",
            dataPoints = accel.map { ((it / accelPeak) + 1f) / 2f },
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(2),
        ),
        PlotData(
            name = "",
            dataPoints = session.battery.map { (it / 100f).coerceIn(0f, 1f) },
            colorArgb = RcHudPlotTraceStyle.defaultColorArgb(3),
        ),
    )
    return traces.take(count)
}
