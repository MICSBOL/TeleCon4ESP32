package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.toPlotByte

/**
 * Last-value histories for slow U8 fields so they can feed plot / radar sinks.
 * Ingest on every telemetry emission (plot ticks repeat analog/batt as a step).
 */
class AnalogChannelHistory(
    private val maxPoints: Int = SimpleProtocolTelemetryMapper.MAX_PLOT_POINTS,
) {
    private val analog = ArrayDeque<Float>(maxPoints + 1)
    private val batt = ArrayDeque<Float>(maxPoints + 1)

    fun ingest(telemetry: TelemetryState) {
        analog.addLast(u8ToNormalized(telemetry.indicatorState.analogValue))
        batt.addLast(u8ToNormalized(telemetry.indicatorState.batteryLevel))
        while (analog.size > maxPoints) analog.removeFirst()
        while (batt.size > maxPoints) batt.removeFirst()
    }

    fun analogPoints(): List<Float> = analog.toList()

    fun battPoints(): List<Float> = batt.toList()

    fun points(channel: TelemetryChannel): List<Float> = when (channel) {
        TelemetryChannel.ANALOG -> analogPoints()
        TelemetryChannel.BATT -> battPoints()
        else -> emptyList()
    }
}

object TelemetryChannelRouter {

    fun plotSeries(
        telemetry: TelemetryState,
        routing: ChannelRouting,
        analogHistory: AnalogChannelHistory,
        plotLabels: List<String>,
        plotColors: List<Int> = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB,
    ): List<PlotData> = List(UserSettings.PLOT_LABEL_COUNT) { index ->
        val sink = plotSink(index)
        val channel = routing.sourceFor(sink)
        val settingsLabel = plotLabels.getOrElse(index) { "" }
        val sourceIndex = channel.analogIndex()
        val telemetryName = if (sourceIndex == index) {
            telemetry.plotState.series.getOrNull(index)?.name.orEmpty()
        } else {
            ""
        }
        val points = u8Points(channel, telemetry, analogHistory)
        PlotData(
            name = telemetryName.ifBlank { settingsLabel },
            dataPoints = points,
            colorArgb = plotColors.getOrElse(index) { 0xFFFFFFFF.toInt() },
        )
    }

    fun radarSeries(
        telemetry: TelemetryState,
        analogHistory: AnalogChannelHistory,
        plotColors: List<Int> = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB,
    ): List<PlotData> = TelemetryChannel.U8_SOURCES.mapIndexed { index, channel ->
        PlotData(
            name = channel.name,
            dataPoints = u8Points(channel, telemetry, analogHistory),
            colorArgb = plotColors.getOrElse(index) { 0xFFFFFFFF.toInt() },
        )
    }

    fun analogGaugeU8(telemetry: TelemetryState, routing: ChannelRouting): Int =
        lastU8(routing.sourceFor(TelemetrySink.ANALOG_GAUGE), telemetry)

    fun batteryGaugeU8(telemetry: TelemetryState, routing: ChannelRouting): Int =
        lastU8(routing.sourceFor(TelemetrySink.BATTERY_GAUGE), telemetry)

    fun panelLeft(telemetry: TelemetryState, routing: ChannelRouting): Int =
        lastI32(routing.sourceFor(TelemetrySink.PANEL_LEFT), telemetry)

    fun panelRight(telemetry: TelemetryState, routing: ChannelRouting): Int =
        lastI32(routing.sourceFor(TelemetrySink.PANEL_RIGHT), telemetry)

    fun ledByte(telemetry: TelemetryState, routing: ChannelRouting): Byte {
        var mask = 0
        repeat(8) { bit ->
            val sink = ledSink(bit)
            if (bitValue(routing.sourceFor(sink), telemetry)) {
                mask = mask or (1 shl bit)
            }
        }
        return mask.toByte()
    }

    fun u8Points(
        channel: TelemetryChannel,
        telemetry: TelemetryState,
        analogHistory: AnalogChannelHistory,
    ): List<Float> {
        val plotIndex = channel.analogIndex()
        if (plotIndex != null) {
            return telemetry.plotState.series.getOrNull(plotIndex)?.dataPoints.orEmpty()
        }
        return analogHistory.points(channel)
    }

    fun lastU8(channel: TelemetryChannel, telemetry: TelemetryState): Int {
        val plotIndex = channel.analogIndex()
        if (plotIndex != null) {
            val normalized = telemetry.plotState.series.getOrNull(plotIndex)?.dataPoints?.lastOrNull()
            return normalized?.toPlotByte() ?: 0
        }
        return when (channel) {
            TelemetryChannel.ANALOG -> telemetry.indicatorState.analogValue.coerceIn(0, 255)
            TelemetryChannel.BATT -> telemetry.indicatorState.batteryLevel.coerceIn(0, 255)
            else -> 0
        }
    }

    private fun lastI32(channel: TelemetryChannel, telemetry: TelemetryState): Int = when (channel) {
        TelemetryChannel.PANEL_LEFT -> telemetry.panelState.leftValue
        TelemetryChannel.PANEL_RIGHT -> telemetry.panelState.rightValue
        else -> 0
    }

    private fun bitValue(channel: TelemetryChannel, telemetry: TelemetryState): Boolean {
        val bit = channel.ledBitIndex() ?: return false
        return (telemetry.indicatorState.ledValues.toInt() and (1 shl bit)) != 0
    }

    private fun plotSink(index: Int): TelemetrySink = when (index) {
        0 -> TelemetrySink.PLOT_0
        1 -> TelemetrySink.PLOT_1
        2 -> TelemetrySink.PLOT_2
        else -> TelemetrySink.PLOT_3
    }

    private fun ledSink(bit: Int): TelemetrySink = when (bit) {
        0 -> TelemetrySink.LED_0
        1 -> TelemetrySink.LED_1
        2 -> TelemetrySink.LED_2
        3 -> TelemetrySink.LED_3
        4 -> TelemetrySink.LED_4
        5 -> TelemetrySink.LED_5
        6 -> TelemetrySink.LED_6
        else -> TelemetrySink.LED_7
    }

    private fun TelemetryChannel.ledBitIndex(): Int? = when (this) {
        TelemetryChannel.LED_0 -> 0
        TelemetryChannel.LED_1 -> 1
        TelemetryChannel.LED_2 -> 2
        TelemetryChannel.LED_3 -> 3
        TelemetryChannel.LED_4 -> 4
        TelemetryChannel.LED_5 -> 5
        TelemetryChannel.LED_6 -> 6
        TelemetryChannel.LED_7 -> 7
        else -> null
    }
}

private fun u8ToNormalized(value: Int): Float = value.coerceIn(0, 255) / 255f
