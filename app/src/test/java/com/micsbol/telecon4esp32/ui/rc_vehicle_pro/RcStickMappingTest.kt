package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RcStickMappingTest {

    @Test
    fun applyExpo_linearWhenZero() {
        assertEquals(0.5f, RcStickMapping.applyExpo(0.5f, expo = 0f), 1e-5f)
        assertEquals(-0.5f, RcStickMapping.applyExpo(-0.5f, expo = 0f), 1e-5f)
    }

    @Test
    fun applyExpo_softensNearCenterWhenHigh() {
        val soft = RcStickMapping.applyExpo(0.5f, expo = 1f)
        assertTrue(soft < 0.5f)
        assertEquals(1f, RcStickMapping.applyExpo(1f, expo = 1f), 1e-5f)
    }

    @Test
    fun mapSteerX_appliesTravelReverseAndTrim() {
        val settings = RcVehicleProControlSettings(
            deadzone = 0f,
            steerExpo = 0f,
            steerTravel = 0.5f,
            reverseSteer = true,
            steerTrim = 0.1f,
        )
        // raw 1 → travel 0.5 → reverse -0.5 → trim -0.4
        assertEquals(-0.4f, RcStickMapping.mapSteerX(1f, settings), 1e-5f)
    }

    @Test
    fun mapThrottleY_appliesTravelAndReverse() {
        val settings = RcVehicleProControlSettings(
            deadzone = 0f,
            throttleExpo = 0f,
            throttleTravel = 0.75f,
            reverseThrottle = true,
        )
        assertEquals(-0.75f, RcStickMapping.mapThrottleY(1f, settings), 1e-5f)
    }

    @Test
    fun nextTravelPreset_cyclesFiftySeventyFiveHundred() {
        assertEquals(0.75f, RcStickMapping.nextTravelPreset(0.50f), 1e-5f)
        assertEquals(1.00f, RcStickMapping.nextTravelPreset(0.75f), 1e-5f)
        assertEquals(0.50f, RcStickMapping.nextTravelPreset(1.00f), 1e-5f)
        assertEquals(0.50f, RcStickMapping.nextTravelPreset(0.60f), 1e-5f)
    }

    @Test
    fun travelPercentLabel_roundsPercent() {
        assertEquals(50, RcStickMapping.travelPercentLabel(0.5f))
        assertEquals(100, RcStickMapping.travelPercentLabel(1f))
    }
}
