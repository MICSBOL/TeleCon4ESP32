package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GhCameraGimbalTest {

    @Test
    fun `center maps to zero normalized axis`() {
        assertEquals(0f, GhCameraGimbal.toNormalizedAxis(GhCameraGimbal.CENTER), 0.001f)
        assertEquals(GhCameraGimbal.CENTER, GhCameraGimbal.fromNormalizedAxis(0f))
    }

    @Test
    fun `extremes map to full range`() {
        assertEquals(GhCameraGimbal.MIN, GhCameraGimbal.fromNormalizedAxis(-1f))
        assertEquals(GhCameraGimbal.MAX, GhCameraGimbal.fromNormalizedAxis(1f))
        assertEquals(-1f, GhCameraGimbal.toNormalizedAxis(0), 0.001f)
        assertEquals(1f, GhCameraGimbal.toNormalizedAxis(100), 0.001f)
    }

    @Test
    fun `clamp keeps values in range`() {
        assertEquals(0, GhCameraGimbal.clamp(-5))
        assertEquals(100, GhCameraGimbal.clamp(140))
        assertEquals(50, GhCameraGimbal.clamp(50))
    }
}
