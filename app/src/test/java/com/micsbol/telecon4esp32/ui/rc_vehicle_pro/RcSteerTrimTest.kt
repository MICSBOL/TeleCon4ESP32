package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.JoystickMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RcSteerTrimTest {

    @Test
    fun nudge_movesByOneChannelStep() {
        assertEquals(0.01f, RcSteerTrim.nudge(0f, steps = 1), 1e-5f)
        assertEquals(-0.02f, RcSteerTrim.nudge(0f, steps = -2), 1e-5f)
    }

    @Test
    fun nudge_clampsToMax() {
        assertEquals(RcSteerTrim.MAX, RcSteerTrim.nudge(RcSteerTrim.MAX, steps = 1), 1e-5f)
        assertEquals(-RcSteerTrim.MAX, RcSteerTrim.nudge(-RcSteerTrim.MAX, steps = -1), 1e-5f)
    }

    @Test
    fun apply_addsTrimAndClamps() {
        assertEquals(0.05f, RcSteerTrim.apply(stickX = 0f, trim = 0.05f), 1e-5f)
        assertEquals(1f, RcSteerTrim.apply(stickX = 0.99f, trim = 0.05f), 1e-5f)
    }

    @Test
    fun toChannelUnits_matchesWireInts() {
        assertEquals(3, RcSteerTrim.toChannelUnits(0.03f))
        assertEquals(-1, RcSteerTrim.toChannelUnits(-0.01f))
    }

    @Test
    fun visualOffset_usesEnabledAxesOnly() {
        assertEquals(
            Pair(0.03f, 0.05f),
            RcStickTrim.visualOffset(JoystickMode.Spring(), trimX = 0.03f, trimY = 0.05f),
        )
        assertEquals(
            Pair(0f, 0.05f),
            RcStickTrim.visualOffset(JoystickMode.VerticalSpring(), trimX = 0.03f, trimY = 0.05f),
        )
        assertEquals(
            Pair(0.03f, 0f),
            RcStickTrim.visualOffset(JoystickMode.HorizontalSpring(), trimX = 0.03f, trimY = 0.05f),
        )
    }

    @Test
    fun subtract_convertsVisualRestBackToGeometric() {
        val offset = Pair(0.03f, 0.06f)
        val visualRest = RcStickTrim.add(Pair(0f, 0f), offset)
        assertEquals(Pair(0.03f, 0.06f), visualRest)
        assertEquals(Pair(0f, 0f), RcStickTrim.subtract(visualRest, offset))
    }
}
