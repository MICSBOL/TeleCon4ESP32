package com.micsbol.telecon4esp32.ui.control_panel

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import org.junit.Assert.assertEquals
import org.junit.Test

class PlotLabelDisplayTest {

    @Test
    fun `plotSeriesForDisplay pads settings labels into four UI slots`() {
        val series = plotSeriesForDisplay(
            telemetrySeries = listOf(
                PlotData(name = "Volts", dataPoints = listOf(0.5f), colorArgb = 0xFF00FFFF.toInt()),
                PlotData(name = "Amps", dataPoints = listOf(0.3f), colorArgb = 0xFFFF0000.toInt()),
                PlotData(name = "RPM", dataPoints = listOf(0.8f), colorArgb = 0xFF00FF00.toInt()),
            ),
            plotLabels = listOf("", "", "", "Temp"),
        )

        assertEquals(4, series.size)
        assertEquals("Volts", series[0].name)
        assertEquals("RPM", series[2].name)
        assertEquals("Temp", series[3].name)
        assertEquals(emptyList<Float>(), series[3].dataPoints)
    }

    @Test
    fun `plotSeriesForDisplay uses settings when telemetry name is default`() {
        val series = plotSeriesForDisplay(
            telemetrySeries = listOf(
                PlotData(name = "Plot 1", dataPoints = emptyList()),
            ),
            plotLabels = listOf("Voltage", "", "", ""),
        )

        assertEquals("Voltage", series[0].name)
    }
}
