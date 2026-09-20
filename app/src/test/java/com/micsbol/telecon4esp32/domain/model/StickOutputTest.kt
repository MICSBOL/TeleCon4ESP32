package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StickOutputTest {

    @Test
    fun centerNormalizedIsUnsigned2047() {
        assertEquals(StickOutput.CENTER, StickOutput.normalizedToUnsigned(0f))
        assertEquals(2047, StickOutput.percentToUnsigned(0))
    }

    @Test
    fun fullRangeEndsAreZeroAnd4094() {
        assertEquals(0, StickOutput.normalizedToUnsigned(-1f))
        assertEquals(4094, StickOutput.normalizedToUnsigned(1f))
        assertEquals(0, StickOutput.percentToUnsigned(-100))
        assertEquals(4094, StickOutput.percentToUnsigned(100))
    }

    @Test
    fun unitFromRestMapsDownEndToZeroAndUpToMax() {
        assertEquals(0f, StickOutput.unitFromRest(-1f, rest = -1f), 0.001f)
        assertEquals(1f, StickOutput.unitFromRest(1f, rest = -1f), 0.001f)
        assertEquals(0f, StickOutput.unitFromRest(-1f, rest = 1f), 0.001f)
        assertEquals(1f, StickOutput.unitFromRest(1f, rest = 1f), 0.001f)
        assertEquals(0.5f, StickOutput.unitFromRest(0f, rest = 0f), 0.001f)
    }

    @Test
    fun plotScaleRemapsNative4094OntoUserRange() {
        val unipolar = PlotCalibration(offset = 0f, span = 255f)
        assertEquals(0f, StickOutput.toEngineering(-1f, unipolar), 0.02f)
        assertEquals(127.5f, StickOutput.toEngineering(0f, unipolar), 0.02f)
        assertEquals(255f, StickOutput.toEngineering(1f, unipolar), 0.02f)

        val bipolar = PlotCalibration(offset = -2047f, span = 4094f)
        assertEquals(-2047f, StickOutput.toEngineering(-1f, bipolar), 0.02f)
        assertEquals(0f, StickOutput.toEngineering(0f, bipolar), 0.02f)
        assertEquals(2047f, StickOutput.toEngineering(1f, bipolar), 0.02f)

        val full = PlotCalibration(offset = 0f, span = 4094f)
        assertEquals(0f, StickOutput.toEngineering(-1f, full), 0.02f)
        assertEquals(2047f, StickOutput.toEngineering(0f, full), 0.02f)
        assertEquals(4094f, StickOutput.toEngineering(1f, full), 0.02f)
    }
}
