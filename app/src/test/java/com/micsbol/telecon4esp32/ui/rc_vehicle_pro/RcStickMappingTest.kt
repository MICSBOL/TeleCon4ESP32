package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.JoystickMode
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
            rightTrimX = 0.1f,
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

    @Test
    fun mapThrottleStick_defaultModeZerosHorizontal() {
        val settings = RcVehicleProControlSettings(deadzone = 0f, throttleExpo = 0f)
        val mapped = RcStickMapping.mapThrottleStick(0.8f, 1f, settings)
        assertEquals(0f, mapped.first, 1e-5f)
        assertEquals(1f, mapped.second, 1e-5f)
    }

    @Test
    fun mapThrottleStick_combinedSendsBothAxes() {
        val settings = RcVehicleProControlSettings(
            leftStickMode = JoystickMode.Spring(),
            deadzone = 0f,
            throttleExpo = 0f,
        )
        val mapped = RcStickMapping.mapThrottleStick(0.5f, -0.25f, settings)
        assertEquals(0.5f, mapped.first, 1e-5f)
        assertEquals(-0.25f, mapped.second, 1e-5f)
    }

    @Test
    fun mapSteerStick_defaultModeZerosVertical() {
        val settings = RcVehicleProControlSettings(deadzone = 0f, steerExpo = 0f, rightTrimX = 0f)
        val mapped = RcStickMapping.mapSteerStick(-0.4f, 0.9f, settings)
        assertEquals(-0.4f, mapped.first, 1e-5f)
        assertEquals(0f, mapped.second, 1e-5f)
    }

    @Test
    fun mapSteerStick_combinedAppliesBothTrims() {
        val settings = RcVehicleProControlSettings(
            rightStickMode = JoystickMode.Hold(),
            deadzone = 0f,
            steerExpo = 0f,
            rightTrimX = 0.1f,
            rightTrimY = 0.2f,
        )
        val mapped = RcStickMapping.mapSteerStick(0.2f, 0.5f, settings)
        assertEquals(0.3f, mapped.first, 1e-5f)
        assertEquals(0.7f, mapped.second, 1e-5f)
    }

    @Test
    fun mapThrottleStick_combinedAppliesBothTrims() {
        val settings = RcVehicleProControlSettings(
            leftStickMode = JoystickMode.Spring(),
            deadzone = 0f,
            throttleExpo = 0f,
            leftTrimX = 0.05f,
            leftTrimY = -0.1f,
        )
        val mapped = RcStickMapping.mapThrottleStick(0.2f, 0.4f, settings)
        assertEquals(0.25f, mapped.first, 1e-5f)
        assertEquals(0.3f, mapped.second, 1e-5f)
    }
}
