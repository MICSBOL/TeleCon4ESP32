package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlotLineStyleTest {

    @Test
    fun `stair holds the previous value then jumps`() {
        val vertices = stairVertices(
            values = listOf(0.5f, 1f, 0f),
            width = 100f,
            height = 100f,
        )
        assertEquals(
            listOf(
                PlotVertex(0f, 50f),
                PlotVertex(50f, 50f),
                PlotVertex(50f, 0f),
                PlotVertex(100f, 0f),
                PlotVertex(100f, 100f),
            ),
            vertices,
        )
    }

    @Test
    fun `triangle style builds one peak per sample from the baseline`() {
        val triangles = triangleContours(
            values = listOf(1f, 0.5f),
            width = 100f,
            height = 100f,
        )
        assertEquals(2, triangles.size)
        assertEquals(
            listOf(
                PlotVertex(0f, 100f),
                PlotVertex(25f, 0f),
                PlotVertex(50f, 100f),
            ),
            triangles[0],
        )
        assertEquals(
            listOf(
                PlotVertex(50f, 100f),
                PlotVertex(75f, 50f),
                PlotVertex(100f, 100f),
            ),
            triangles[1],
        )
    }

    @Test
    fun `single sample still spans the full width`() {
        val line = lineVertices(listOf(0.25f), width = 80f, height = 40f)
        assertEquals(listOf(PlotVertex(0f, 30f), PlotVertex(80f, 30f)), line)
        val stair = stairVertices(listOf(0.25f), width = 80f, height = 40f)
        assertEquals(line, stair)
        val triangle = triangleContours(listOf(0.25f), width = 80f, height = 40f).single()
        assertEquals(
            listOf(
                PlotVertex(0f, 40f),
                PlotVertex(40f, 30f),
                PlotVertex(80f, 40f),
            ),
            triangle,
        )
    }

    @Test
    fun `right-aligned on-change samples occupy one slot each from the right`() {
        val xs = (0 until 5).map { index ->
            plotRightAlignedSlotX(index, count = 5, width = 100f, slotCount = 100)
        }
        assertEquals(listOf(95f, 96f, 97f, 98f, 99f), xs)

        val stair = stairVertices(
            values = listOf(0.5f, 1f),
            width = 100f,
            height = 100f,
            rightAlignedSlots = 100,
        )
        assertEquals(
            listOf(
                PlotVertex(98f, 50f),
                PlotVertex(99f, 50f),
                PlotVertex(99f, 0f),
                PlotVertex(100f, 0f),
            ),
            stair,
        )

        val triangle = triangleContours(
            values = listOf(1f),
            width = 100f,
            height = 100f,
            rightAlignedSlots = 100,
        ).single()
        assertEquals(
            listOf(
                PlotVertex(99f, 100f),
                PlotVertex(99.5f, 0f),
                PlotVertex(100f, 100f),
            ),
            triangle,
        )
    }
}
