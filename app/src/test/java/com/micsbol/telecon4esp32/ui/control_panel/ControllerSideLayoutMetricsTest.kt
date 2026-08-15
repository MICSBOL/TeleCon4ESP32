package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerSideLayoutMetricsTest {

    @Test
    fun shortLandscapePhone_joystickFitsInControlsBudget() {
        // Typical short landscape phone usable area after insets (~830x407 px).
        val height = 160.dp
        val width = 360.dp
        val metrics = buildControllerSideLayoutMetrics(
            side = ButtonSide.LEFT,
            availableHeight = height,
            contentWidth = width,
        )

        val controlsBudget = height * 0.74f
        assertTrue(
            "Joystick must fit in controls budget with knob clearance (was ${metrics.joystickSize})",
            metrics.joystickSize <= controlsBudget * 0.75f,
        )
        assertTrue(
            "Knob should be large enough to fill the mid gap (was ${metrics.knobSize})",
            metrics.knobSize >= metrics.joystickSize * 0.35f,
        )
        assertTrue(
            "Joystick must leave room for telemetry (was ${metrics.joystickSize})",
            metrics.joystickSize <= height * 0.78f,
        )
        val sideBudget = width * controllerSideWidthFraction(width / height)
        assertTrue(
            "Side must not consume more than its width budget",
            metrics.joystickSize <= sideBudget,
        )
        // Two sides + center: center should keep at least ~35% of width.
        val twoSides = sideBudget * 2f
        assertTrue(
            "Center plot must keep usable width",
            (width - twoSides) >= width * 0.35f,
        )
    }

    @Test
    fun tabletLandscape_scalesUpButStaysCapped() {
        val metrics = buildControllerSideLayoutMetrics(
            side = ButtonSide.RIGHT,
            availableHeight = 800.dp,
            contentWidth = 1280.dp,
        )
        assertTrue(metrics.joystickSize >= 200.dp)
        assertTrue(metrics.joystickSize <= 300.dp)
        assertTrue(metrics.ledSize in 8.dp..14.dp)
    }

    @Test
    fun veryWidePhone_usesTighterSideFraction() {
        assertEquals(0.27f, controllerSideWidthFraction(2.5f), 0.001f)
        assertEquals(0.29f, controllerSideWidthFraction(2.1f), 0.001f)
        assertEquals(0.31f, controllerSideWidthFraction(1.7f), 0.001f)
        assertEquals(0.33f, controllerSideWidthFraction(1.3f), 0.001f)
    }

    @Test
    fun tinyHeight_stillProducesUsableMinimum() {
        val metrics = buildControllerSideLayoutMetrics(
            side = ButtonSide.LEFT,
            availableHeight = 90.dp,
            contentWidth = 200.dp,
        )
        assertTrue(metrics.joystickSize >= 72.dp)
        assertTrue(metrics.panelWidth > 0.dp)
        assertTrue(metrics.switchSize > 0.dp)
        assertTrue(metrics.knobSize > 0.dp)
    }

    @Test
    fun metricsAreDensityIndependent_sameDpInputsSameOutputs() {
        val a = buildControllerSideLayoutMetrics(
            side = ButtonSide.LEFT,
            availableHeight = 400.dp,
            contentWidth = 900.dp,
        )
        val b = buildControllerSideLayoutMetrics(
            side = ButtonSide.LEFT,
            availableHeight = 400.dp,
            contentWidth = 900.dp,
        )
        assertEquals(a.joystickSize, b.joystickSize)
        assertEquals(a.panelWidth, b.panelWidth)
        assertEquals(a.ledSize, b.ledSize)
    }
}
