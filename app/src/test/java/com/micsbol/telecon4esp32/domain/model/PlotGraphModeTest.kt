package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlotGraphModeTest {

    @Test
    fun `continuous mode keeps the rolling window`() {
        val history = PlotDisplayHistory(maxPoints = 3)
        assertEquals(
            listOf(0.2f, 0.2f, 0.3f),
            history.points(listOf(0.1f, 0.1f, 0.2f, 0.2f, 0.3f), PlotGraphMode.CONTINUOUS),
        )
    }

    @Test
    fun `on-change keeps earlier changes after the source window fills with the last value`() {
        val history = PlotDisplayHistory(maxPoints = 5)
        val source = mutableListOf(0.1f, 0.2f)
        assertEquals(listOf(0.1f, 0.2f), history.points(source, PlotGraphMode.ON_CHANGE))

        repeat(10) {
            source.add(0.2f)
            val window = source.takeLast(5)
            assertEquals(listOf(0.1f, 0.2f), history.points(window, PlotGraphMode.ON_CHANGE))
        }
    }

    @Test
    fun `on-change appends only when the latest value differs`() {
        val history = PlotDisplayHistory(maxPoints = 10)
        history.points(listOf(0.1f), PlotGraphMode.ON_CHANGE)
        history.points(listOf(0.1f, 0.1f), PlotGraphMode.ON_CHANGE)
        assertEquals(
            listOf(0.1f, 0.4f),
            history.points(listOf(0.1f, 0.1f, 0.4f), PlotGraphMode.ON_CHANGE),
        )
    }

    @Test
    fun `on-change drops the oldest change only after max unique points`() {
        val history = PlotDisplayHistory(maxPoints = 3)
        history.points(listOf(0.1f), PlotGraphMode.ON_CHANGE)
        history.points(listOf(0.1f, 0.2f), PlotGraphMode.ON_CHANGE)
        history.points(listOf(0.2f, 0.3f), PlotGraphMode.ON_CHANGE)
        assertEquals(
            listOf(0.2f, 0.3f, 0.4f),
            history.points(listOf(0.3f, 0.4f), PlotGraphMode.ON_CHANGE),
        )
    }

    @Test
    fun `empty source clears on-change history`() {
        val history = PlotDisplayHistory(maxPoints = 5)
        history.points(listOf(0.1f, 0.2f), PlotGraphMode.ON_CHANGE)
        assertEquals(emptyList<Float>(), history.points(emptyList(), PlotGraphMode.ON_CHANGE))
    }

    @Test
    fun `zero max points stays empty`() {
        val history = PlotDisplayHistory(maxPoints = 0)
        assertEquals(
            emptyList<Float>(),
            history.points(listOf(0.5f), PlotGraphMode.CONTINUOUS),
        )
    }
}
