package com.micsbol.telecon4esp32.ui.control_panel
import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.fromString
import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
/**
 * Unit tests for [JoystickMode] serialization / deserialization.
 *
 * These are pure JVM tests — no Android context needed.
 */
class JoystickModeSerializationTest {
    // ── Round-trip tests ──────────────────────────────────────────────────────
    @Test
    fun `Spring with default position round-trips correctly`() {
        val mode = JoystickMode.Spring()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `Hold with default position round-trips correctly`() {
        val mode = JoystickMode.Hold()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `VerticalSpring with default position round-trips correctly`() {
        val mode = JoystickMode.VerticalSpring()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `VerticalHold with default position round-trips correctly`() {
        val mode = JoystickMode.VerticalHold()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `HorizontalSpring with default position round-trips correctly`() {
        val mode = JoystickMode.HorizontalSpring()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `HorizontalHold with default position round-trips correctly`() {
        val mode = JoystickMode.HorizontalHold()
        assertEquals(mode, fromString(mode.toStringRepresentation()))
    }
    @Test
    fun `custom position is preserved after round-trip`() {
        val mode = JoystickMode.Hold(Pair(2, 9))
        val restored = fromString(mode.toStringRepresentation())
        assertEquals(mode, restored)
        assertEquals(Pair(2, 9), restored.initialPosition)
    }
    // ── toStringRepresentation format ─────────────────────────────────────────
    @Test
    fun `toStringRepresentation has format ModeName,x,y`() {
        val mode = JoystickMode.Spring(JoystickMode.CENTER)
        val parts = mode.toStringRepresentation().split(",")
        assertEquals(3, parts.size)
        assertEquals("Spring", parts[0])
    }
    // ── fromString error handling ─────────────────────────────────────────────
    @Test
    fun `fromString with null returns Spring default`() {
        val mode = fromString(null)
        assertTrue(mode is JoystickMode.Spring)
        assertEquals(JoystickMode.CENTER, mode.initialPosition)
    }
    @Test
    fun `fromString with empty string returns Spring default`() {
        val mode = fromString("")
        assertTrue(mode is JoystickMode.Spring)
    }
    @Test
    fun `fromString with too few parts returns Spring default`() {
        val mode = fromString("Hold,6")
        assertTrue(mode is JoystickMode.Spring)
    }
    @Test
    fun `fromString with non-numeric coordinates returns Spring with default position`() {
        val mode = fromString("Hold,abc,xyz")
        assertTrue(mode is JoystickMode.Hold)
        // Non-parseable coords fall back to 6,6 (CENTER)
        assertEquals(JoystickMode.CENTER, mode.initialPosition)
    }
    @Test
    fun `fromString with unknown mode name returns Spring default`() {
        val mode = fromString("UnknownMode,6,6")
        assertTrue(mode is JoystickMode.Spring)
    }
    // ── Companion constants ───────────────────────────────────────────────────
    @Test
    fun `CENTER is (6, 6)`() {
        assertEquals(Pair(6, 6), JoystickMode.CENTER)
    }
    @Test
    fun `UP position is (6, 0)`() {
        assertEquals(Pair(6, 0), JoystickMode.UP)
    }
    @Test
    fun `DOWN position is (6, 12)`() {
        assertEquals(Pair(6, 12), JoystickMode.DOWN)
    }
    @Test
    fun `LEFT position is (0, 6)`() {
        assertEquals(Pair(0, 6), JoystickMode.LEFT)
    }
    @Test
    fun `RIGHT position is (12, 6)`() {
        assertEquals(Pair(12, 6), JoystickMode.RIGHT)
    }

    @Test
    fun `vertical rest positions are up center down`() {
        val mode = JoystickMode.VerticalHold()
        assertEquals(
            listOf(JoystickMode.CENTER, JoystickMode.UP, JoystickMode.DOWN),
            mode.allowedRestPositions(),
        )
    }

    @Test
    fun `horizontal rest positions are center left right`() {
        val mode = JoystickMode.HorizontalSpring()
        assertEquals(
            listOf(JoystickMode.CENTER, JoystickMode.LEFT, JoystickMode.RIGHT),
            mode.allowedRestPositions(),
        )
    }

    @Test
    fun `combined rest positions include all five extremes`() {
        val mode = JoystickMode.Hold()
        assertEquals(
            listOf(
                JoystickMode.CENTER,
                JoystickMode.UP,
                JoystickMode.DOWN,
                JoystickMode.LEFT,
                JoystickMode.RIGHT,
            ),
            mode.allowedRestPositions(),
        )
    }

    @Test
    fun `toggling axes switches between vertical horizontal and combined`() {
        val combined = JoystickMode.Spring()
        val horizontalOnly = combined.togglingAxis(JoystickAxis.VERTICAL)
        assertTrue(horizontalOnly is JoystickMode.HorizontalSpring)
        val combinedAgain = horizontalOnly.togglingAxis(JoystickAxis.VERTICAL)
        assertTrue(combinedAgain is JoystickMode.Spring)
        val verticalOnly = combinedAgain.togglingAxis(JoystickAxis.HORIZONTAL)
        assertTrue(verticalOnly is JoystickMode.VerticalSpring)
    }

    @Test
    fun `invalid rest for axis falls back to center`() {
        val vertical = JoystickMode.Hold(JoystickMode.LEFT).withAxis(JoystickAxis.VERTICAL)
        assertTrue(vertical is JoystickMode.VerticalHold)
        assertEquals(JoystickMode.CENTER, vertical.initialPosition)
    }

    @Test
    fun `grid rest converts to normalized stick`() {
        assertEquals(Pair(0f, 0f), JoystickMode.Spring().initialPositionNormalized())
        assertEquals(Pair(0f, 1f), JoystickMode.Hold(JoystickMode.UP).initialPositionNormalized())
        assertEquals(Pair(0f, -1f), JoystickMode.Hold(JoystickMode.DOWN).initialPositionNormalized())
        assertEquals(Pair(-1f, 0f), JoystickMode.Hold(JoystickMode.LEFT).initialPositionNormalized())
        assertEquals(Pair(1f, 0f), JoystickMode.Hold(JoystickMode.RIGHT).initialPositionNormalized())
    }
}
