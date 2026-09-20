package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.runtime.saveable.SaverScope
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.ui.control_panel.RadarBeamWidth
import com.micsbol.telecon4esp32.ui.control_panel.RadarDisplaySettings
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
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
        styles.forEach { style ->
            assertEquals(HUD_PLOT_Y_MIN_DEFAULT, style.yMin)
            assertEquals(HUD_PLOT_Y_MAX_DEFAULT, style.yMax)
        }
    }

    @Test
    fun `resolved y range keeps min below max and ignores non-finite values`() {
        val base = RcHudPlotTraceStyle(colorArgb = 0)
        assertEquals(0f to 4094f, base.resolvedYRange())
        assertEquals(
            -20f to 80f,
            base.copy(yMin = -20f, yMax = 80f).resolvedYRange(),
        )
        assertEquals(5f to 6f, base.copy(yMin = 5f, yMax = 5f).resolvedYRange())
        assertEquals(0f to 4094f, base.copy(yMin = Float.NaN, yMax = Float.NaN).resolvedYRange())
    }

    @Test
    fun `hud plot scale ticks include min mid and max`() {
        assertEquals(listOf(0f, 2047f, 4094f), hudPlotScaleTicks(0f, 4094f))
        assertEquals(listOf(-10f, 10f, 30f), hudPlotScaleTicks(-10f, 30f))
        assertEquals(listOf(0f, 1f), hudPlotScaleTicks(0f, 1f, count = 2))
        assertEquals("0.5", formatHudPlotScaleTick(0.5f))
        assertEquals("255", formatHudPlotScaleTick(255f))
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
    fun `empty live series falls back to sinusoid traces`() {
        val display = rcHudPlotDisplaySeries(
            emptyList(),
            RcTelemetryPlotSession(sessionElapsedSec = 1.2f),
        )
        assertEquals(4, display.size)
        assertEquals(RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT, display[0].dataPoints.size)
        assertTrue(display[0].dataPoints.all { it in 0f..1f })
        assertTrue(display[0].dataPoints != display[1].dataPoints)
    }

    @Test
    fun `demo fallback keeps live channel names`() {
        val named = List(4) { index ->
            PlotData(
                name = "Vel $index",
                dataPoints = emptyList(),
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(index),
            )
        }
        val display = rcHudPlotDisplaySeries(named, RcTelemetryPlotSession(sessionElapsedSec = 0.4f))
        assertEquals("Vel 0", display[0].name)
        assertEquals("Vel 3", display[3].name)
        assertTrue(display[0].dataPoints.size > 1)
    }

    @Test
    fun `stick overlay wins over telemetry for the linked channel`() {
        val live = List(4) { index ->
            PlotData(
                name = "CH${index + 1}",
                dataPoints = listOf(0.1f, 0.2f, 0.3f),
                colorArgb = RcHudPlotTraceStyle.defaultColorArgb(index),
            )
        }
        val overlay = listOf(0.9f, 0.8f, 0.7f)
        val display = rcHudPlotDisplaySeries(
            plotSeries = live,
            session = RcTelemetryPlotSession(
                controlChannels = mapOf(TelemetryChannel.CH_1 to overlay),
            ),
            routing = ChannelRouting.defaults(),
        )
        assertEquals(overlay, display[0].dataPoints)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), display[1].dataPoints)
    }

    @Test
    fun `unlinked empty telemetry uses sinusoid while another slot stays live`() {
        val live = listOf(
            PlotData(name = "CH1", dataPoints = emptyList()),
            PlotData(name = "CH2", dataPoints = listOf(0.2f, 0.4f, 0.6f)),
            PlotData(name = "", dataPoints = emptyList()),
            PlotData(name = "", dataPoints = emptyList()),
        )
        val display = rcHudPlotDisplaySeries(live, RcTelemetryPlotSession(sessionElapsedSec = 0.5f))
        assertEquals(RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT, display[0].dataPoints.size)
        assertEquals(listOf(0.2f, 0.4f, 0.6f), display[1].dataPoints)
    }

    @Test
    fun `radar uses overlay then telemetry then sinusoid`() {
        val telemetry = listOf(
            PlotData(name = "CH_1", dataPoints = emptyList()),
            PlotData(name = "CH_2", dataPoints = listOf(0.1f, 0.2f)),
        )
        val radar = rcHudRadarDisplaySeries(
            radarSeries = telemetry,
            session = RcTelemetryPlotSession(
                sessionElapsedSec = 0.8f,
                controlChannels = mapOf(TelemetryChannel.CH_1 to listOf(0.55f, 0.66f)),
            ),
        )
        assertEquals(TelemetryChannel.U8_SOURCES.size, radar.size)
        assertEquals(listOf(0.55f, 0.66f), radar[0].dataPoints)
        assertEquals(listOf(0.1f, 0.2f), radar[1].dataPoints)
        assertEquals(RcVehicleProLayout.TELEMETRY_PLOT_SAMPLE_COUNT, radar[2].dataPoints.size)
    }

    @Test
    fun `hud sample readout remaps 0-1 onto the selected y scale`() {
        val style = RcHudPlotTraceStyle(colorArgb = 0, yMin = 0f, yMax = 4094f)
        assertEquals("0", style.formatSample(0f))
        assertEquals("2047", style.formatSample(0.5f))
        assertEquals("4094", style.formatSample(1f))
        val bipolar = style.copy(yMin = -2047f, yMax = 2047f)
        assertEquals("0", bipolar.formatSample(0.5f))
        assertEquals("-2047", bipolar.formatSample(0f))
    }

    @Test
    fun `stick xy label uses the (x,y) format`() {
        assertEquals("(0.50,-0.25)", formatHudStickXy(0.5f, -0.25f))
        assertEquals("(-1.00,1.00)", formatHudStickXy(-2f, 2f))
        assertEquals("(0.00,0.00)", formatHudStickXy(0f, 0f))
    }

    @Test
    fun `scope samples scale the selected channel and fall back when empty`() {
        val series = listOf(
            PlotData(name = "CH1", dataPoints = listOf(0.25f, 0.5f, 1f)),
            PlotData(name = "CH2", dataPoints = emptyList()),
        )
        assertEquals(
            listOf(25f, 50f, 100f),
            rcHudScaledChannelSamples(
                series = series,
                channel = TelemetryChannel.CH_1,
                scale = 100f,
                fallback = listOf(7f),
            ),
        )
        assertEquals(
            listOf(7f),
            rcHudScaledChannelSamples(
                series = series,
                channel = TelemetryChannel.CH_2,
                scale = 100f,
                fallback = listOf(7f),
            ),
        )
    }

    @Test
    fun `demo sinusoid reaches the 0 and 1 radar extremes`() {
        val samples = rcHudDemoSinusoid(channelIndex = 0, elapsedSec = 0f)
        assertTrue(samples.min() <= 0.02f)
        assertTrue(samples.max() >= 0.98f)
    }

    @Test
    fun `hud plot chrome saver keeps tab styles across restore`() {
        val original = RcHudPlotChrome(
            mode = RcTelemetryPlotMode.TEMP,
            modeBarVisible = false,
            legendVisible = false,
            traceStyles = RcHudPlotTraceStyle.defaults().mapIndexed { index, style ->
                style.copy(
                    visible = index == 0,
                    dashed = true,
                    yMin = index * -10f,
                    yMax = 100f + index,
                )
            },
            batteryStyle = RcHudScopeStyle.batteryDefault().copy(fill = false, dashed = true),
            tempStyle = RcHudScopeStyle.tempDefault().copy(colorArgb = 0xFF00FF00.toInt()),
            stickStyle = RcHudStickStyle.defaults().copy(showGrid = false, showAxes = false),
            radarSettings = RadarDisplaySettings(
                beamWidth = RadarBeamWidth.WIDE,
                showHistory = false,
            ),
        )
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val saved = with(RcHudPlotChromeSaver) { scope.save(original) }
        val restored = RcHudPlotChromeSaver.restore(checkNotNull(saved))
        assertEquals(original.mode, restored?.mode)
        assertEquals(false, restored?.modeBarVisible)
        assertEquals(false, restored?.legendVisible)
        assertEquals(original.traceStyles, restored?.traceStyles)
        assertEquals(original.batteryStyle, restored?.batteryStyle)
        assertEquals(original.tempStyle, restored?.tempStyle)
        assertEquals(original.stickStyle, restored?.stickStyle)
        assertEquals(original.radarSettings, restored?.radarSettings)
    }

    @Test
    fun `hud plot chrome encode survives a DataStore round trip`() {
        val original = RcHudPlotChrome(
            mode = RcTelemetryPlotMode.RADAR,
            modeBarVisible = false,
            legendVisible = false,
            traceStyles = RcHudPlotTraceStyle.defaults().mapIndexed { index, style ->
                style.copy(yMin = index.toFloat(), yMax = 10f + index)
            },
        )
        val restored = decodeRcHudPlotChrome(encodeRcHudPlotChrome(original))
        assertEquals(original, restored)
        assertEquals(RcHudPlotChrome(), decodeRcHudPlotChrome(""))
        assertEquals(RcHudPlotChrome(), decodeRcHudPlotChrome(null))
    }

    @Test
    fun `trace style saver keeps y range and restores old saves as 0 to 1`() {
        val styles = RcHudPlotTraceStyle.defaults().mapIndexed { index, style ->
            style.copy(yMin = index.toFloat(), yMax = 10f + index)
        }
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val saved = with(RcHudPlotTraceStyleListSaver) { scope.save(styles) }
        val restored = RcHudPlotTraceStyleListSaver.restore(checkNotNull(saved))
        assertEquals(styles, restored)

        val legacy = RcHudPlotTraceStyleListSaver.restore(
            "1,34,LINE,0;1,35,LINE,1;1,36,TRIANGLE,0;0,37,STAIR,0",
        )
        assertEquals(0f, legacy?.get(0)?.yMin)
        assertEquals(1f, legacy?.get(0)?.yMax)
        assertEquals(0f, legacy?.get(3)?.yMin)
        assertEquals(1f, legacy?.get(3)?.yMax)
    }
}
