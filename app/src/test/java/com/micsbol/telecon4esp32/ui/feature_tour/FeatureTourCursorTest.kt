package com.micsbol.telecon4esp32.ui.feature_tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureTourCursorTest {
    @Test
    fun practiceGestureAdvancesOnlyTheStepThatAsksForIt() {
        val cursor = FeatureTourCursor(stepCount = 3)

        cursor.onPracticed(advances = false)
        assertEquals(0, cursor.index)

        cursor.onPracticed(advances = true)
        assertEquals(1, cursor.index)
        assertFalse(cursor.finished)
    }

    @Test
    fun lastStepFinishesTheLesson() {
        val cursor = FeatureTourCursor(stepCount = 2)
        cursor.next()
        assertFalse(cursor.finished)
        cursor.next()
        assertTrue(cursor.finished)
        assertEquals(1, cursor.index)
    }
}
