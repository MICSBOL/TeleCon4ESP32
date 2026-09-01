package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import org.junit.Assert.assertEquals
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
}
