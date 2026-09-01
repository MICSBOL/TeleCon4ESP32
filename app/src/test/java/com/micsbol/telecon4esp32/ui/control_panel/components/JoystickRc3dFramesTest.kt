package com.micsbol.telecon4esp32.ui.control_panel.components

import com.micsbol.telecon4esp32.domain.model.JoystickMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoystickRc3dFramesTest {

    @Test
    fun `center spring wiggle stays on the 13 by 13 grid`() {
        val center = joystickRc3dFrameIndex(6, 6)
        val wiggle = joystickRc3dWiggleFrame(JoystickMode.Spring(), center)
        assertEquals(joystickRc3dFrameIndex(5, 5), wiggle)
        assertTrue(wiggle in 0 until JOYSTICK_RC3D_FRAME_COUNT)
    }

    @Test
    fun `top-left spring wiggle does not go negative`() {
        val origin = joystickRc3dFrameIndex(0, 0)
        val wiggle = joystickRc3dWiggleFrame(JoystickMode.Spring(), origin)
        assertTrue(wiggle in 0 until JOYSTICK_RC3D_FRAME_COUNT)
        assertEquals(joystickRc3dFrameIndex(1, 1), wiggle)
    }

    @Test
    fun `vertical spring at the top row stays in range`() {
        val top = joystickRc3dFrameIndex(6, 0)
        val wiggle = joystickRc3dWiggleFrame(JoystickMode.VerticalSpring(JoystickMode.UP), top)
        assertEquals(joystickRc3dFrameIndex(6, 1), wiggle)
    }

    @Test
    fun `horizontal spring at the left edge stays in range`() {
        val left = joystickRc3dFrameIndex(0, 6)
        val wiggle = joystickRc3dWiggleFrame(JoystickMode.HorizontalSpring(JoystickMode.LEFT), left)
        assertEquals(joystickRc3dFrameIndex(1, 6), wiggle)
    }

    @Test
    fun `normalized center maps to frame 84`() {
        assertEquals(84, normalizedStickToFrame(0f, 0f))
    }
}
