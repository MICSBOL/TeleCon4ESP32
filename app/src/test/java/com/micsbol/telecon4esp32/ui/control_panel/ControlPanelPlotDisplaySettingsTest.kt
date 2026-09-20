package com.micsbol.telecon4esp32.ui.control_panel

import com.micsbol.telecon4esp32.domain.model.KnobChannelLink
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

    @Test
    fun `legacy encoding without knobs does not restore stick channel links`() {
        val legacy = ControlPanelPlotDisplaySettings(
            leftStickLink = StickChannelLink(
                enabled = true,
                vertical = TelemetryChannel.CH_1,
                horizontal = TelemetryChannel.CH_2,
            ),
            rightStickLink = StickChannelLink(
                enabled = true,
                horizontal = TelemetryChannel.CH_4,
            ),
        )
        val parts = legacy.encode().split("\u001e").take(7)
        val restored = ControlPanelPlotDisplaySettings.decode(parts.joinToString("\u001e"))
        assertEquals(StickChannelLink.DEFAULT, restored.leftStickLink)
        assertEquals(StickChannelLink.DEFAULT, restored.rightStickLink)
        assertEquals(KnobChannelLink.DEFAULT, restored.leftKnobLink)
        assertEquals(KnobChannelLink.DEFAULT, restored.rightKnobLink)
    }

    @Test
    fun occupiedChannels_excludesTheControlBeingEdited() {
        val settings = ControlPanelPlotDisplaySettings(
            leftStickLink = StickChannelLink(
                enabled = true,
                vertical = TelemetryChannel.CH_1,
            ),
            rightStickLink = StickChannelLink(
                enabled = true,
                horizontal = TelemetryChannel.CH_2,
            ),
            leftKnobLink = KnobChannelLink(
                enabled = true,
                channel = TelemetryChannel.CH_3,
            ),
            rightKnobLink = KnobChannelLink(
                enabled = true,
                channel = TelemetryChannel.CH_4,
            ),
        )
        assertEquals(
            setOf(TelemetryChannel.CH_1, TelemetryChannel.CH_2, TelemetryChannel.CH_4),
            settings.occupiedChannels(exceptLeftKnob = true),
        )
        assertEquals(
            setOf(TelemetryChannel.CH_2, TelemetryChannel.CH_3, TelemetryChannel.CH_4),
            settings.occupiedChannels(exceptLeftStick = true),
        )
        assertEquals(
            emptySet<TelemetryChannel>(),
            ControlPanelPlotDisplaySettings.DEFAULT.occupiedChannels(),
        )
    }
}
