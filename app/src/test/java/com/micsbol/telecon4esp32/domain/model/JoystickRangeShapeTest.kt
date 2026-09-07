package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class JoystickRangeShapeTest {

    @Test
    fun `circle leaves a diagonal rim below full throw`() {
        val diagonal = 1f / sqrt(2f)
        val mapped = JoystickRangeShape.CIRCLE.mapOutput(diagonal, diagonal)
        assertEquals(diagonal, mapped.first, 1e-5f)
        assertEquals(diagonal, mapped.second, 1e-5f)
    }

    @Test
    fun `square maps a diagonal rim to full throw on both axes`() {
        val diagonal = 1f / sqrt(2f)
        val mapped = JoystickRangeShape.SQUARE.mapOutput(diagonal, diagonal)
        assertEquals(1f, mapped.first, 1e-5f)
        assertEquals(1f, mapped.second, 1e-5f)
    }

    @Test
    fun `square keeps cardinal full throw`() {
        assertEquals(1f, JoystickRangeShape.SQUARE.mapOutput(1f, 0f).first, 1e-5f)
        assertEquals(0f, JoystickRangeShape.SQUARE.mapOutput(1f, 0f).second, 1e-5f)
        assertEquals(0f, JoystickRangeShape.SQUARE.mapOutput(0f, -1f).first, 1e-5f)
        assertEquals(-1f, JoystickRangeShape.SQUARE.mapOutput(0f, -1f).second, 1e-5f)
    }

    @Test
    fun `square visual inverse restores the circular diagonal`() {
        val diagonal = 1f / sqrt(2f)
        val command = JoystickRangeShape.SQUARE.mapOutput(diagonal, diagonal)
        val visual = JoystickRangeShape.SQUARE.toVisual(command.first, command.second)
        assertEquals(diagonal, visual.first, 1e-5f)
        assertEquals(diagonal, visual.second, 1e-5f)
    }

    @Test
    fun `unknown stored value defaults to circle`() {
        assertEquals(JoystickRangeShape.CIRCLE, JoystickRangeShape.fromStored(null))
        assertEquals(JoystickRangeShape.CIRCLE, JoystickRangeShape.fromStored("NOPE"))
        assertEquals(JoystickRangeShape.SQUARE, JoystickRangeShape.fromStored("SQUARE"))
    }
}
