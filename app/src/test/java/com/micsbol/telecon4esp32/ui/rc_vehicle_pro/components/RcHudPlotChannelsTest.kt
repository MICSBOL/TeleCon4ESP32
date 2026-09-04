package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RcHudPlotChannelsTest {

    @Test
    fun `defaults keep the first three traces visible and the second dashed`() {
        val styles = RcHudPlotTraceStyle.defaults()
        assertEquals(UserSettings.PLOT_LABEL_COUNT, styles.size)
        assertTrue(styles[0].visible)
        assertTrue(styles[1].visible)
        assertTrue(styles[1].dashed)
        assertTrue(styles[2].visible)
        assertTrue(!styles[3].visible)
        assertTrue(!styles[0].dashed)
    }

    @Test
    fun `live plot series of four channels is used as-is`() {
        val live = List(4) { index ->
            PlotData(
                name = "CH${index + 1}",
                dataPoints = listOf(0.1f, 0.2f + index / 10f, 0.4f),
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(index),
            )
        }
        val display = rcHudPlotDisplaySeries(live, RcTelemetryPlotSession())
        assertEquals(4, display.size)
        assertEquals("CH1", display[0].name)
        assertEquals(3, display[0].dataPoints.size)
        assertEquals("CH4", display[3].name)
    }

    @Test
    fun `empty live series falls back to session traces`() {
        val session = RcTelemetryPlotSession(
            speed = listOf(10f, 20f, 30f),
            command = listOf(8f, 16f, 24f),
            battery = listOf(80f, 75f, 70f),
        )
        val display = rcHudPlotDisplaySeries(emptyList(), session)
        assertEquals(4, display.size)
        assertEquals(3, display[0].dataPoints.size)
        assertTrue(display[0].dataPoints.all { it in 0f..1f })
        assertTrue(display[3].dataPoints.last() <= 0.71f)
    }

    @Test
    fun `demo fallback keeps live channel names`() {
        val session = RcTelemetryPlotSession(speed = listOf(10f, 20f, 30f))
        val named = List(4) { index ->
            PlotData(
                name = "Vel $index",
                dataPoints = emptyList(),
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(index),
            )
        }
        val display = rcHudPlotDisplaySeries(named, session)
        assertEquals("Vel 0", display[0].name)
        assertEquals("Vel 3", display[3].name)
        assertTrue(display[0].dataPoints.size > 1)
    }

    @Test
    fun `blank channel name uses fallback`() {
        assertEquals("Ch 1", rcHudPlotChannelLabel("  ", "Ch 1"))
        assertEquals("Speed", rcHudPlotChannelLabel("Speed", "Ch 1"))
    }
}
