package com.micsbol.telecon4esp32.ui.control_panel

import com.micsbol.telecon4esp32.domain.model.PlotGraphMode
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlPanelPlotDisplaySettingsTest {

    @Test
    fun `encode round trip keeps plot visualizer settings`() {
        val original = ControlPanelPlotDisplaySettings(
            radarSettings = RadarDisplaySettings(
                scanSpan = RadarScanSpan.DEGREES_270,
                beamWidth = RadarBeamWidth.WIDE,
                showGrid = false,
            ),
            plotVisible = listOf(true, false, true, false),
            graphModes = listOf(
                PlotGraphMode.ON_CHANGE,
                PlotGraphMode.CONTINUOUS,
                PlotGraphMode.ON_CHANGE,
                PlotGraphMode.CONTINUOUS,
            ),
            lineStyles = listOf(
                PlotLineStyle.STAIR,
                PlotLineStyle.LINE,
                PlotLineStyle.TRIANGLE,
                PlotLineStyle.LINE,
            ),
            plotOnTop = listOf(false, true, false, false),
            leftStickLink = StickChannelLink(
                enabled = true,
                vertical = TelemetryChannel.CH_1,
                horizontal = TelemetryChannel.CH_2,
            ),
            rightStickLink = StickChannelLink.DEFAULT,
        )
        val restored = ControlPanelPlotDisplaySettings.decode(original.encode())
        assertEquals(original, restored)
    }

    @Test
    fun `blank encoding restores defaults`() {
        assertEquals(
            ControlPanelPlotDisplaySettings.DEFAULT,
            ControlPanelPlotDisplaySettings.decode(""),
        )
        assertEquals(
            ControlPanelPlotDisplaySettings.DEFAULT,
            ControlPanelPlotDisplaySettings.decode(null),
        )
    }
}
