package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RcCameraRotationKnobTest {

    private val center = Offset(50f, 50f)
    private val radius = 40f

    @Test
    fun panThumb_atCenterValue_sitsAboveKnobCenter() {
        val thumb = panThumbCenter(center, radius, panAngle = 0f)
        assertEquals(center.x, thumb.x, 0.01f)
        assertEquals(center.y - radius * PAN_THUMB_DISTANCE_FRACTION, thumb.y, 0.01f)
    }

    @Test
    fun isOnPanThumb_onlyNearWhiteCircle() {
        val thumb = panThumbCenter(center, radius, panAngle = 0f)
        val hitRadius = radius * PAN_THUMB_GRAB_FRACTION
        assertTrue(isOnPanThumb(thumb, center, radius, panAngle = 0f, hitRadius = hitRadius))
        assertFalse(isOnPanThumb(center, center, radius, panAngle = 0f, hitRadius = hitRadius))
        assertFalse(
            isOnPanThumb(
                Offset(center.x + radius, center.y),
                center,
                radius,
                panAngle = 0f,
                hitRadius = hitRadius,
            ),
        )
    }

    @Test
    fun isOnPanRing_rejectsCenterAndAcceptsThumbOrbit() {
        val thumb = panThumbCenter(center, radius, panAngle = 0f)
        assertFalse(isOnPanRing(center, center, radius))
        assertTrue(isOnPanRing(thumb, center, radius))
        assertFalse(isOnPanRing(Offset(center.x + radius * 2f, center.y), center, radius))
    }

    @Test
    fun wrappedAngleDelta_staysOnShortestArc() {
        assertEquals(20f, wrappedAngleDelta(170f, -170f), 0.01f)
        assertEquals(-20f, wrappedAngleDelta(-170f, 170f), 0.01f)
        assertEquals(15f, wrappedAngleDelta(10f, 25f), 0.01f)
    }
}
