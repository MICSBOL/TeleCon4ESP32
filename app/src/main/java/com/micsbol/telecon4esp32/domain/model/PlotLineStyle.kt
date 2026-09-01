package com.micsbol.telecon4esp32.domain.model

/** How a Control Panel Cartesian trace is drawn between samples. */
enum class PlotLineStyle {
    /** Straight segments between consecutive samples. */
    LINE,
    /** Sample-and-hold steps (horizontal, then vertical). */
    STAIR,
    /** One triangle per sample from the baseline up to the value. */
    TRIANGLE,
}

data class PlotVertex(
    val x: Float,
    val y: Float,
)

fun plotValueY(value: Float, height: Float): Float =
    height - value.coerceIn(0f, 1f) * height

fun plotSampleX(index: Int, count: Int, width: Float): Float {
    if (count <= 1) return 0f
    return index * (width / (count - 1).toFloat())
}

/**
 * Left edge of sample [index] when each value occupies `1 / [slotCount]` of [width]
 * and unused slots stay empty on the left so new samples enter from the right.
 */
fun plotRightAlignedSlotX(
    index: Int,
    count: Int,
    width: Float,
    slotCount: Int,
): Float {
    if (slotCount <= 0 || count <= 0) return 0f
    val used = count.coerceAtMost(slotCount)
    val slot = width / slotCount.toFloat()
    val startSlot = slotCount - used
    return (startSlot + index.coerceIn(0, used - 1)) * slot
}

fun plotSlotWidth(width: Float, slotCount: Int): Float =
    if (slotCount <= 0) 0f else width / slotCount.toFloat()

fun lineVertices(
    values: List<Float>,
    width: Float,
    height: Float,
    rightAlignedSlots: Int? = null,
): List<PlotVertex> {
    if (values.isEmpty()) return emptyList()
    val slots = rightAlignedSlots
    if (slots != null && slots > 0) {
        val slot = plotSlotWidth(width, slots)
        val y = plotValueY(values[0], height)
        if (values.size == 1) {
            val xLeft = plotRightAlignedSlotX(0, 1, width, slots)
            return listOf(PlotVertex(xLeft, y), PlotVertex(xLeft + slot, y))
        }
        return values.mapIndexed { index, value ->
            PlotVertex(
                plotRightAlignedSlotX(index, values.size, width, slots),
                plotValueY(value, height),
            )
        }
    }
    if (values.size == 1) {
        val y = plotValueY(values[0], height)
        return listOf(PlotVertex(0f, y), PlotVertex(width, y))
    }
    return values.mapIndexed { index, value ->
        PlotVertex(plotSampleX(index, values.size, width), plotValueY(value, height))
    }
}

fun stairVertices(
    values: List<Float>,
    width: Float,
    height: Float,
    rightAlignedSlots: Int? = null,
): List<PlotVertex> {
    if (values.isEmpty()) return emptyList()
    val slots = rightAlignedSlots
    if (slots != null && slots > 0) {
        val slot = plotSlotWidth(width, slots)
        val out = ArrayList<PlotVertex>(values.size * 3)
        values.forEachIndexed { index, value ->
            val y = plotValueY(value, height)
            val xLeft = plotRightAlignedSlotX(index, values.size, width, slots)
            val xRight = xLeft + slot
            if (index == 0) {
                out.add(PlotVertex(xLeft, y))
            } else {
                val previous = out.last()
                if (previous.x != xLeft) {
                    out.add(PlotVertex(xLeft, previous.y))
                }
                out.add(PlotVertex(xLeft, y))
            }
            out.add(PlotVertex(xRight, y))
        }
        return out
    }
    if (values.size == 1) {
        val y = plotValueY(values[0], height)
        return listOf(PlotVertex(0f, y), PlotVertex(width, y))
    }
    val out = ArrayList<PlotVertex>(values.size * 2)
    var previousY = plotValueY(values[0], height)
    out.add(PlotVertex(0f, previousY))
    for (i in 1 until values.size) {
        val x = plotSampleX(i, values.size, width)
        val y = plotValueY(values[i], height)
        out.add(PlotVertex(x, previousY))
        out.add(PlotVertex(x, y))
        previousY = y
    }
    return out
}

/** One closed triangle per sample: base on the plot floor, peak at the sample. */
fun triangleContours(
    values: List<Float>,
    width: Float,
    height: Float,
    rightAlignedSlots: Int? = null,
): List<List<PlotVertex>> {
    if (values.isEmpty() || width <= 0f) return emptyList()
    val slots = rightAlignedSlots
    val slot = if (slots != null && slots > 0) {
        plotSlotWidth(width, slots)
    } else {
        width / values.size.toFloat()
    }
    return values.mapIndexed { index, value ->
        val xLeft = if (slots != null && slots > 0) {
            plotRightAlignedSlotX(index, values.size, width, slots)
        } else {
            index * slot
        }
        val xRight = xLeft + slot
        listOf(
            PlotVertex(xLeft, height),
            PlotVertex((xLeft + xRight) / 2f, plotValueY(value, height)),
            PlotVertex(xRight, height),
        )
    }
}
