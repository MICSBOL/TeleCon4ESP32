package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RcStickDeadzoneTest {

    @Test
    fun applyAxis_zerosValuesInsideDeadzone() {
        assertEquals(0f, RcStickDeadzone.applyAxis(0.05f, deadzone = 0.08f), 1e-5f)
        assertEquals(0f, RcStickDeadzone.applyAxis(-0.08f, deadzone = 0.08f), 1e-5f)
        assertEquals(0f, RcStickDeadzone.applyAxis(0f, deadzone = 0.08f), 1e-5f)
    }

    @Test
    fun applyAxis_rescalesOutsideDeadzoneToFullRange() {
        assertEquals(0f, RcStickDeadzone.applyAxis(0.08f, deadzone = 0.08f), 1e-5f)
        assertEquals(1f, RcStickDeadzone.applyAxis(1f, deadzone = 0.08f), 1e-5f)
        assertEquals(-1f, RcStickDeadzone.applyAxis(-1f, deadzone = 0.08f), 1e-5f)
        val mid = RcStickDeadzone.applyAxis(0.54f, deadzone = 0.08f)
        assertTrue(mid in 0.49f..0.51f)
    }

    @Test
    fun apply_mapsBothAxes() {
        val mapped = RcStickDeadzone.apply(x = 0.04f, y = 1f, deadzone = 0.08f)
        assertEquals(0f, mapped.first, 1e-5f)
        assertEquals(1f, mapped.second, 1e-5f)
    }

    @Test
    fun applyAxis_disabledWhenDeadzoneZero() {
        assertEquals(0.05f, RcStickDeadzone.applyAxis(0.05f, deadzone = 0f), 1e-5f)
    }
}
