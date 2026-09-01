package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryChannelRouterTest {

    @Test
    fun `default routing leaves analog gauge and plot 0 on the wire fields`() {
        val telemetry = TelemetryState(
            panelState = PanelState(leftValue = 111, rightValue = 222),
            indicatorState = IndicatorState(
                analogValue = 40,
                batteryLevel = 80,
                ledValues = 0b0000_0001.toByte(),
            ),
            plotState = PlotState(
                series = listOf(
                    PlotData(name = "Plot 1", dataPoints = listOf(0.5f)),
                    PlotData(name = "Plot 2", dataPoints = listOf(0.25f)),
                    PlotData(name = "Plot 3", dataPoints = listOf(0.1f)),
                    PlotData(name = "Plot 4", dataPoints = listOf(0.9f)),
                ),
            ),
        )
        val routing = ChannelRouting.defaults()
        assertEquals(40, TelemetryChannelRouter.analogGaugeU8(telemetry, routing))
        assertEquals(80, TelemetryChannelRouter.batteryGaugeU8(telemetry, routing))
        assertEquals(111, TelemetryChannelRouter.panelLeft(telemetry, routing))
        assertEquals(0b0000_0001.toByte(), TelemetryChannelRouter.ledByte(telemetry, routing))
        val plots = TelemetryChannelRouter.plotSeries(
            telemetry = telemetry,
            routing = routing,
            analogHistory = AnalogChannelHistory(),
            plotLabels = listOf("V", "", "", ""),
        )
        assertEquals(listOf(0.5f), plots[0].dataPoints)
        assertEquals(listOf(0.25f), plots[1].dataPoints)
    }

    @Test
    fun `plot sink can show analog history and analog gauge can show a plot`() {
        val telemetry = TelemetryState(
            indicatorState = IndicatorState(analogValue = 255, batteryLevel = 10),
            plotState = PlotState(
                series = listOf(
                    PlotData(dataPoints = listOf(0.2f, 0.4f)),
                    PlotData(dataPoints = listOf(0f)),
                    PlotData(dataPoints = emptyList()),
                    PlotData(dataPoints = emptyList()),
                ),
            ),
        )
        val history = AnalogChannelHistory()
        history.ingest(telemetry)
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.PLOT_0, TelemetryChannel.ANALOG)
            .with(TelemetrySink.ANALOG_GAUGE, TelemetryChannel.CH_1)
        val plots = TelemetryChannelRouter.plotSeries(
            telemetry = telemetry,
            routing = routing,
            analogHistory = history,
            plotLabels = listOf("A", "", "", ""),
        )
        assertEquals(listOf(1f), plots[0].dataPoints)
        assertEquals((0.4f * 255f).toInt(), TelemetryChannelRouter.analogGaugeU8(telemetry, routing))
    }

    @Test
    fun `plot sink can bind to CH8 when eight analog samples are present`() {
        val telemetry = TelemetryState(
            plotState = PlotState(
                series = List(8) { index ->
                    PlotData(name = "CH${index + 1}", dataPoints = listOf(index / 7f))
                },
            ),
        )
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.PLOT_0, TelemetryChannel.CH_8)
        val plots = TelemetryChannelRouter.plotSeries(
            telemetry = telemetry,
            routing = routing,
            analogHistory = AnalogChannelHistory(),
            plotLabels = listOf("A", "", "", ""),
        )
        assertEquals(listOf(1f), plots[0].dataPoints)
        assertEquals(255, TelemetryChannelRouter.lastU8(TelemetryChannel.CH_8, telemetry))
    }

    @Test
    fun `led sink remap swaps display bits`() {
        val telemetry = TelemetryState(
            indicatorState = IndicatorState(ledValues = 0b1000_0000.toByte()),
        )
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.LED_0, TelemetryChannel.LED_7)
            .with(TelemetrySink.LED_7, TelemetryChannel.LED_0)
        assertEquals(0b0000_0001.toByte(), TelemetryChannelRouter.ledByte(telemetry, routing))
    }

    @Test
    fun `panels can swap sides`() {
        val telemetry = TelemetryState(
            panelState = PanelState(leftValue = 5, rightValue = 9),
        )
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.PANEL_LEFT, TelemetryChannel.PANEL_RIGHT)
        assertEquals(9, TelemetryChannelRouter.panelLeft(telemetry, routing))
        assertEquals(9, TelemetryChannelRouter.panelRight(telemetry, routing))
    }
}
