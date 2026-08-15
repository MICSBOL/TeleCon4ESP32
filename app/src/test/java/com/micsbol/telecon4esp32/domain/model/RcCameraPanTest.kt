package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RcCameraPanTest {

    @Test
    fun frontNormalized_encodesTo511() {
        assertEquals(511, RcCameraPan.rawFromNormalized(RcCameraPan.FRONT_NORMALIZED))
        assertEquals(511, RcCameraPan.FRONT_RAW)
    }

    @Test
    fun extremes_mapToRange() {
        assertEquals(0, RcCameraPan.rawFromNormalized(0f))
        assertEquals(1023, RcCameraPan.rawFromNormalized(1f))
    }
}
