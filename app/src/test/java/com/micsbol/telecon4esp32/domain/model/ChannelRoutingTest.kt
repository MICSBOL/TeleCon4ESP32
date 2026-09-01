package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRoutingTest {

    @Test
    fun `defaults map stock Control Panel widgets`() {
        val routing = ChannelRouting.defaults()
        assertEquals(TelemetryChannel.CH_1, routing.sourceFor(TelemetrySink.PLOT_0))
        assertEquals(TelemetryChannel.CH_4, routing.sourceFor(TelemetrySink.PLOT_3))
        assertEquals(TelemetryChannel.CH_1, routing.sourceFor(TelemetrySink.RADAR_ANGLE))
        assertEquals(TelemetryChannel.CH_2, routing.sourceFor(TelemetrySink.RADAR_RANGE))
        assertEquals(TelemetryChannel.ANALOG, routing.sourceFor(TelemetrySink.ANALOG_GAUGE))
        assertEquals(TelemetryChannel.BATT, routing.sourceFor(TelemetrySink.BATTERY_GAUGE))
        assertEquals(TelemetryChannel.LED_7, routing.sourceFor(TelemetrySink.LED_7))
        assertTrue(routing.isDefault())
    }

    @Test
    fun `with ignores incompatible kinds`() {
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.ANALOG_GAUGE, TelemetryChannel.PANEL_LEFT)
        assertEquals(TelemetryChannel.ANALOG, routing.sourceFor(TelemetrySink.ANALOG_GAUGE))
    }

    @Test
    fun `encode decode round trip keeps remaps`() {
        val routing = ChannelRouting.defaults()
            .with(TelemetrySink.PLOT_0, TelemetryChannel.ANALOG)
            .with(TelemetrySink.RADAR_ANGLE, TelemetryChannel.CH_3)
            .with(TelemetrySink.LED_0, TelemetryChannel.LED_7)
        val restored = ChannelRouting.decode(routing.encode())
        assertEquals(TelemetryChannel.ANALOG, restored.sourceFor(TelemetrySink.PLOT_0))
        assertEquals(TelemetryChannel.CH_3, restored.sourceFor(TelemetrySink.RADAR_ANGLE))
        assertEquals(TelemetryChannel.LED_7, restored.sourceFor(TelemetrySink.LED_0))
        assertEquals(TelemetryChannel.CH_2, restored.sourceFor(TelemetrySink.PLOT_1))
        assertFalse(restored.isDefault())
    }

    @Test
    fun `decode blank and junk falls back to defaults`() {
        assertTrue(ChannelRouting.decode(null).isDefault())
        assertTrue(ChannelRouting.decode("").isDefault())
        assertTrue(ChannelRouting.decode("not-a-map").isDefault())
        val mixed = ChannelRouting.decode("PLOT_0=ANALOG;NOPE=PLOT_1;ANALOG_GAUGE=PANEL_LEFT")
        assertEquals(TelemetryChannel.ANALOG, mixed.sourceFor(TelemetrySink.PLOT_0))
        assertEquals(TelemetryChannel.ANALOG, mixed.sourceFor(TelemetrySink.ANALOG_GAUGE))
    }

    @Test
    fun `legacy plot source names migrate to numbered channels`() {
        val restored = ChannelRouting.decode("PLOT_0=PLOT_3;RADAR_RANGE=PLOT_1;PLOT_3=CH_8")
        assertEquals(TelemetryChannel.CH_4, restored.sourceFor(TelemetrySink.PLOT_0))
        assertEquals(TelemetryChannel.CH_2, restored.sourceFor(TelemetrySink.RADAR_RANGE))
        assertEquals(TelemetryChannel.CH_8, restored.sourceFor(TelemetrySink.PLOT_3))
    }
}
