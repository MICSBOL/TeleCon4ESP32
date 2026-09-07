package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RcVehicleProControlSettingsTest {

    @Test
    fun leftStickModeFromPersisted_defaultsToVerticalHoldDown() {
        assertEquals(
            JoystickMode.VerticalHold(JoystickMode.DOWN),
            RcVehicleProControlSettings.leftStickModeFromPersisted(null, null),
        )
    }

    @Test
    fun leftStickModeFromPersisted_migratesOldSpringFlag() {
        assertEquals(
            JoystickMode.VerticalSpring(JoystickMode.CENTER),
            RcVehicleProControlSettings.leftStickModeFromPersisted(null, throttleHold = false),
        )
    }

    @Test
    fun leftStickModeFromPersisted_prefersStoredMode() {
        val stored = JoystickMode.Spring(JoystickMode.UP)
        assertEquals(
            stored,
            RcVehicleProControlSettings.leftStickModeFromPersisted(
                stored = stored.toStringRepresentation(),
                throttleHold = true,
            ),
        )
    }

    @Test
    fun rightStickModeFromPersisted_defaultsToHorizontalSpring() {
        assertEquals(
            JoystickMode.HorizontalSpring(JoystickMode.CENTER),
            RcVehicleProControlSettings.rightStickModeFromPersisted(null, null),
        )
    }

    @Test
    fun rightStickModeFromPersisted_migratesOldHoldFlag() {
        assertEquals(
            JoystickMode.HorizontalHold(JoystickMode.CENTER),
            RcVehicleProControlSettings.rightStickModeFromPersisted(null, steeringHold = true),
        )
    }

    @Test
    fun stickChannelLink_decodeBlankIsDisabled() {
        assertEquals(StickChannelLink.DEFAULT, StickChannelLink.decode(null))
        assertEquals(StickChannelLink.DEFAULT, StickChannelLink.decode(""))
    }

    @Test
    fun stickChannelLink_roundTripPreservesAxisPicks() {
        val link = StickChannelLink(
            enabled = true,
            vertical = TelemetryChannel.CH_3,
            horizontal = TelemetryChannel.CH_3,
        )
        assertEquals(link, StickChannelLink.decode(link.encode()))
    }

    @Test
    fun stickChannelLink_togglingSameChannelClearsAxis() {
        val link = StickChannelLink(enabled = true, vertical = TelemetryChannel.CH_1)
        assertEquals(
            null,
            link.toggling(JoystickAxis.VERTICAL, TelemetryChannel.CH_1).vertical,
        )
    }

    @Test
    fun stickChannelLink_withEnabledSeedsDefaultsForActiveAxes() {
        val combined = StickChannelLink.DEFAULT.withEnabled(true, JoystickAxis.COMBINED)
        assertEquals(true, combined.enabled)
        assertEquals(TelemetryChannel.CH_1, combined.vertical)
        assertEquals(TelemetryChannel.CH_2, combined.horizontal)

        val verticalOnly = StickChannelLink.DEFAULT.withEnabled(true, JoystickAxis.VERTICAL)
        assertEquals(TelemetryChannel.CH_1, verticalOnly.vertical)
        assertEquals(null, verticalOnly.horizontal)
    }

    @Test
    fun stickChannelLink_samplesMapsStickAxesToAnalogBus() {
        val link = StickChannelLink(
            enabled = true,
            vertical = TelemetryChannel.CH_1,
            horizontal = TelemetryChannel.CH_2,
        )
        val samples = link.samples(x = 1f, y = -1f, axis = JoystickAxis.COMBINED)
        assertEquals(1f, samples.getValue(TelemetryChannel.CH_2), 0.001f)
        assertEquals(1f, samples.getValue(TelemetryChannel.CH_1), 0.001f)
        assertTrue(link.copy(enabled = false).samples(1f, 1f, JoystickAxis.COMBINED).isEmpty())
    }

    @Test
    fun stickChannelLink_horizontalFillsRadarSpan() {
        assertEquals(
            0f,
            StickChannelLink.angleProgress(-1f, 0f, JoystickAxis.HORIZONTAL, 180f),
            0.001f,
        )
        assertEquals(
            1f,
            StickChannelLink.angleProgress(1f, 0f, JoystickAxis.HORIZONTAL, 270f),
            0.001f,
        )
        assertEquals(
            0.5f,
            StickChannelLink.angleProgress(0f, 0f, JoystickAxis.HORIZONTAL, 180f),
            0.001f,
        )
    }

    @Test
    fun stickChannelLink_combinedPolarCompletes180And270() {
        assertEquals(
            0.5f,
            StickChannelLink.angleProgress(0f, 1f, JoystickAxis.COMBINED, 180f),
            0.001f,
        )
        assertEquals(
            0f,
            StickChannelLink.angleProgress(-1f, 0f, JoystickAxis.COMBINED, 180f),
            0.001f,
        )
        assertEquals(
            1f,
            StickChannelLink.angleProgress(1f, 0f, JoystickAxis.COMBINED, 180f),
            0.001f,
        )
        val left270 = 0.7071f
        assertEquals(
            0f,
            StickChannelLink.angleProgress(-left270, -left270, JoystickAxis.COMBINED, 270f),
            0.02f,
        )
        assertEquals(
            1f,
            StickChannelLink.angleProgress(left270, -left270, JoystickAxis.COMBINED, 270f),
            0.02f,
        )
    }

    @Test
    fun stickChannelLink_displayXyKeepsStickWhenNoChannel() {
        val link = StickChannelLink()
        val mapped = link.displayXy(0.4f, -0.6f) { 1f }
        assertEquals(0.4f, mapped.first, 1e-5f)
        assertEquals(-0.6f, mapped.second, 1e-5f)
    }

    @Test
    fun stickChannelLink_displayXyMapsBoundChannelOntoStickRange() {
        val link = StickChannelLink(
            horizontal = TelemetryChannel.CH_2,
            vertical = TelemetryChannel.CH_1,
        )
        val mapped = link.displayXy(0.1f, 0.2f) { channel ->
            when (channel) {
                TelemetryChannel.CH_2 -> 1f
                TelemetryChannel.CH_1 -> 0f
                else -> null
            }
        }
        assertEquals(1f, mapped.first, 1e-5f)
        assertEquals(-1f, mapped.second, 1e-5f)
    }

    @Test
    fun stickChannelLink_displayXyFallsBackWhenChannelHasNoSample() {
        val link = StickChannelLink(horizontal = TelemetryChannel.CH_3)
        val mapped = link.displayXy(0.25f, -0.5f) { null }
        assertEquals(0.25f, mapped.first, 1e-5f)
        assertEquals(-0.5f, mapped.second, 1e-5f)
    }
}
