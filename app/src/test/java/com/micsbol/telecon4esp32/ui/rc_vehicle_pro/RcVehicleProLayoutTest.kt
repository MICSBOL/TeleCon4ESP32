package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RcVehicleProLayoutTest {

    @Test
    fun s23UltraFhdPlus_qualifiesByPerformanceClass() {
        // S23 Ultra at FHD+ often reports sw 360, not Plus-size 400.
        assertTrue(
            RcVehicleProLayout.usesImmersiveStickHeight(
                density = 3.0f,
                smallestWidthDp = 360,
                mediaPerformanceClass = 33,
            ),
        )
    }

    @Test
    fun s23UltraClass_qualifiesByRamWhenPerformanceClassMissing() {
        assertTrue(
            RcVehicleProLayout.usesImmersiveStickHeight(
                density = 3.0f,
                smallestWidthDp = 360,
                mediaPerformanceClass = 0,
                totalRamMb = 8 * 1024L,
            ),
        )
    }

    @Test
    fun plusSizeHighDensity_stillQualifiesWithoutFlagshipSignals() {
        assertTrue(
            RcVehicleProLayout.usesImmersiveStickHeight(
                density = 2.5f,
                smallestWidthDp = 411,
            ),
        )
    }

    @Test
    fun midRangeAndLowDensity_doNotQualify() {
        assertFalse(
            RcVehicleProLayout.usesImmersiveStickHeight(
                density = 3.0f,
                smallestWidthDp = 360,
                mediaPerformanceClass = 0,
                totalRamMb = 4 * 1024L,
            ),
        )
        assertFalse(
            RcVehicleProLayout.usesImmersiveStickHeight(
                density = 2.0f,
                smallestWidthDp = 411,
                mediaPerformanceClass = 33,
                totalRamMb = 8 * 1024L,
            ),
        )
    }

    @Test
    fun immersiveStickHeight_notAppliedWhenSticksVisible() {
        assertNull(
            RcVehicleProLayout.immersiveStickContainerHeight(
                screenHeight = 412.dp,
                slotHeight = 400.dp,
                immersiveDisplay = true,
                restHudHidden = true,
                anyStickVisible = true,
            ),
        )
    }

    @Test
    fun immersiveStickHeight_skippedWhenChromeVisibleOrIneligible() {
        assertNull(
            RcVehicleProLayout.immersiveStickContainerHeight(
                screenHeight = 412.dp,
                slotHeight = 400.dp,
                immersiveDisplay = false,
                restHudHidden = true,
                anyStickVisible = true,
            ),
        )
        assertNull(
            RcVehicleProLayout.immersiveStickContainerHeight(
                screenHeight = 412.dp,
                slotHeight = 400.dp,
                immersiveDisplay = true,
                restHudHidden = false,
                anyStickVisible = true,
            ),
        )
    }

    @Test
    fun stickContainerBaseHeight_usesLeftoverNotTwoThirds() {
        val joystick = 300.dp
        val height = RcVehicleProLayout.stickContainerBaseHeight(
            slotHeight = 400.dp,
            joystickSize = joystick,
            immersiveHeight = null,
        )
        val leftover = RcVehicleProLayout.ControlZoneChromeHeight +
            joystick +
            RcVehicleProLayout.ControlZoneFooterHeight
        assertEquals(leftover.coerceAtMost(400.dp), height)
        assertTrue(height > 400.dp * RcVehicleProLayout.IMMERSIVE_STICK_HEIGHT_FRACTION)
    }

    @Test
    fun maxResizeHeight_isEightyPercentOfScreen() {
        assertEquals(320.dp, RcVehicleProLayout.maxResizeHeight(400.dp))
    }

    @Test
    fun maxScaleForBaseHeight_reachesEightyPercent() {
        assertEquals(
            320f / 92f,
            RcVehicleProLayout.maxScaleForBaseHeight(
                baseHeight = 92.dp,
                maxHeight = 320.dp,
            ),
            0.001f,
        )
    }

    @Test
    fun scaledStickContainer_keepsAspectRatio() {
        val (width, height) = RcVehicleProLayout.scaledStickContainerSize(
            baseWidth = 300.dp,
            baseHeight = 200.dp,
            scale = 0.5f,
        )
        assertEquals(150.dp, width)
        assertEquals(100.dp, height)
    }

    @Test
    fun stickGroupScaleFromDrag_leftGrowsRightShrinks() {
        assertEquals(
            1.25f,
            RcVehicleProLayout.stickGroupScaleFromDrag(
                startScale = 1f,
                startXPx = 100f,
                currentXPx = 50f,
                containerWidthPx = 200f,
            ),
            0.001f,
        )
        assertEquals(
            0.72f,
            RcVehicleProLayout.stickGroupScaleFromDrag(
                startScale = 0.8f,
                startXPx = 100f,
                currentXPx = 120f,
                containerWidthPx = 200f,
            ),
            0.001f,
        )
        assertEquals(
            RcVehicleProLayout.STICK_GROUP_SCALE_MIN,
            RcVehicleProLayout.stickGroupScaleFromDrag(
                startScale = 0.5f,
                startXPx = 100f,
                currentXPx = 400f,
                containerWidthPx = 200f,
            ),
            0.001f,
        )
        assertEquals(
            RcVehicleProLayout.STICK_GROUP_SCALE_MAX,
            RcVehicleProLayout.stickGroupScaleFromDrag(
                startScale = 1.5f,
                startXPx = 100f,
                currentXPx = -200f,
                containerWidthPx = 200f,
            ),
            0.001f,
        )
        assertEquals(
            3.2f,
            RcVehicleProLayout.stickGroupScaleFromDrag(
                startScale = 1f,
                startXPx = 500f,
                currentXPx = 60f,
                containerWidthPx = 200f,
                maxScale = 3.2f,
            ),
            0.001f,
        )
    }

    @Test
    fun scaledStickContainer_capsHeightAtEightyPercent() {
        val (width, height) = RcVehicleProLayout.scaledStickContainerSize(
            baseWidth = 200.dp,
            baseHeight = 200.dp,
            scale = 8f,
            maxWidth = 400.dp,
            maxHeight = 320.dp,
        )
        assertEquals(320.dp, height)
        assertEquals(320.dp, width)
    }

    @Test
    fun scaledStickContainer_canGrowPastDefaultLayout() {
        val (width, height) = RcVehicleProLayout.scaledStickContainerSize(
            baseWidth = 200.dp,
            baseHeight = 200.dp,
            scale = 1.5f,
            maxWidth = 400.dp,
            maxHeight = 400.dp,
        )
        assertEquals(300.dp, width)
        assertEquals(300.dp, height)
    }

    @Test
    fun centerControlsFillWidth_onlyWhenBothSticksOpen() {
        assertTrue(
            RcVehicleProLayout.centerControlsFillWidth(
                leftStickExpanded = true,
                rightStickExpanded = true,
            ),
        )
        assertFalse(
            RcVehicleProLayout.centerControlsFillWidth(
                leftStickExpanded = true,
                rightStickExpanded = false,
            ),
        )
        assertFalse(
            RcVehicleProLayout.centerControlsFillWidth(
                leftStickExpanded = false,
                rightStickExpanded = true,
            ),
        )
    }

    @Test
    fun centerControlsOverlay_parksOnOppositeSideOfSingleStick() {
        assertEquals(
            Alignment.BottomEnd,
            RcVehicleProLayout.centerControlsOverlayAlignment(
                leftStickExpanded = true,
                rightStickExpanded = false,
            ),
        )
        assertEquals(
            Alignment.BottomStart,
            RcVehicleProLayout.centerControlsOverlayAlignment(
                leftStickExpanded = false,
                rightStickExpanded = true,
            ),
        )
        assertEquals(
            Alignment.BottomCenter,
            RcVehicleProLayout.centerControlsOverlayAlignment(
                leftStickExpanded = true,
                rightStickExpanded = true,
            ),
        )
        assertEquals(
            PaddingValues(end = RcVehicleProLayout.HudCollapsedPeek),
            RcVehicleProLayout.centerControlsOverlayPadding(
                leftStickExpanded = true,
                rightStickExpanded = false,
            ),
        )
        assertEquals(
            PaddingValues(start = RcVehicleProLayout.HudCollapsedPeek),
            RcVehicleProLayout.centerControlsOverlayPadding(
                leftStickExpanded = false,
                rightStickExpanded = true,
            ),
        )
    }

    @Test
    fun sharedStickContainerWidth_doesNotShrinkWhenHudIsVisible() {
        val width = RcVehicleProLayout.sharedStickContainerWidth(
            slotWidth = 900.dp,
        )
        assertEquals((900.dp - RcVehicleProLayout.HudCollapsedPeek - 8.dp) / 2, width)
    }

    @Test
    fun sharedStickContainerWidth_leavesCenterGutterWhenCenterExpanded() {
        val slot = 800.dp
        val reserved = RcVehicleProLayout.centerControlsReservedWidth(
            slotWidth = slot,
            centerExpanded = true,
            bothSticksExpanded = true,
        )
        assertEquals(RcVehicleProLayout.CenterControlsExpandedWidth, reserved)
        val width = RcVehicleProLayout.sharedStickContainerWidth(
            slotWidth = slot,
            centerReserved = reserved,
        )
        assertEquals((slot - reserved - 8.dp) / 2, width)
        assertEquals(
            RcVehicleProLayout.HudCollapsedPeek,
            RcVehicleProLayout.centerControlsReservedWidth(
                slotWidth = slot,
                centerExpanded = false,
                bothSticksExpanded = true,
            ),
        )
    }

    @Test
    fun commandedSpeedKmh_mapsFullThrottleToHudCap() {
        assertEquals(0f, RcVehicleProLayout.commandedSpeedKmh(0f), 0.001f)
        assertEquals(40f, RcVehicleProLayout.commandedSpeedKmh(1f), 0.001f)
        assertEquals(40f, RcVehicleProLayout.commandedSpeedKmh(-1f), 0.001f)
        assertEquals(20f, RcVehicleProLayout.commandedSpeedKmh(0.5f), 0.001f)
    }

    @Test
    fun accelSamples_usesSpeedDeltaOverInterval() {
        val accel = RcVehicleProLayout.accelSamples(
            speeds = listOf(0f, 8f, 8f),
            intervalSec = 0.08f,
        )
        assertEquals(0f, accel[0], 0.001f)
        assertEquals(100f, accel[1], 0.001f)
        assertEquals(0f, accel[2], 0.001f)
    }

    @Test
    fun distanceKm_integratesWindow() {
        val km = RcVehicleProLayout.distanceKm(
            speedsKmh = listOf(36f, 36f),
            intervalSec = 1f,
        )
        assertEquals(0.02f, km, 0.0001f)
    }

    @Test
    fun hasPlotTelemetry_requiresAtLeastOneSample() {
        assertFalse(RcVehicleProLayout.hasPlotTelemetry(emptyList()))
        assertFalse(
            RcVehicleProLayout.hasPlotTelemetry(
                listOf(PlotData(dataPoints = emptyList())),
            ),
        )
        assertTrue(
            RcVehicleProLayout.hasPlotTelemetry(
                listOf(PlotData(dataPoints = listOf(0.2f))),
            ),
        )
    }

    @Test
    fun meanOrZero_emptyIsZero() {
        assertEquals(0f, RcVehicleProLayout.meanOrZero(emptyList()), 0.001f)
        assertEquals(3f, RcVehicleProLayout.meanOrZero(listOf(2f, 4f)), 0.001f)
    }

    @Test
    fun telemetryPlotBetweenSticks_fillsGapBetweenPads() {
        val hudWidth = 900.dp
        val hudHeight = 400.dp
        val pad = 200.dp
        val (width, height) = RcVehicleProLayout.telemetryPlotSizeBetweenSticks(
            hudWidth = hudWidth,
            hudHeight = hudHeight,
            stickPadSize = pad,
        )
        assertEquals(
            hudWidth - pad * 2 - RcVehicleProLayout.TelemetryPlotStickGap * 2,
            width,
        )
        assertEquals(RcVehicleProLayout.TelemetryPlotBothSticksMaxHeight, height)
        assertTrue(width > hudWidth / 3f)
        assertTrue(height < hudHeight / 2)
        assertTrue(height < RcVehicleProLayout.TelemetryPlotHeight)
    }
}
