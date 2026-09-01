package com.micsbol.telecon4esp32.domain.model

/** How a Control Panel Cartesian trace consumes incoming samples. */
enum class PlotGraphMode {
    /** Append every sample (current scrolling oscilloscope). */
    CONTINUOUS,
    /** Keep a point only when the value differs from the previous sample. */
    ON_CHANGE,
}

/**
 * Per-trace display buffer. Continuous mode follows the live sample window.
 * On-change mode keeps its own history and does not drop older changes just
 * because the live window has filled with repeats of the last value.
 */
class PlotDisplayHistory(private val maxPoints: Int) {
    private var mode: PlotGraphMode = PlotGraphMode.CONTINUOUS
    private val onChangePoints = mutableListOf<Float>()

    fun points(source: List<Float>, mode: PlotGraphMode): List<Float> {
        if (maxPoints <= 0) return emptyList()
        if (source.isEmpty()) {
            onChangePoints.clear()
            this.mode = mode
            return emptyList()
        }
        if (mode == PlotGraphMode.CONTINUOUS) {
            this.mode = mode
            return rollingWindow(source)
        }
        if (this.mode != PlotGraphMode.ON_CHANGE) {
            onChangePoints.clear()
            onChangePoints.addAll(compactConsecutiveDuplicates(rollingWindow(source)))
            this.mode = mode
            return onChangePoints.toList()
        }
        val latest = source.last()
        if (onChangePoints.isEmpty() || onChangePoints.last() != latest) {
            onChangePoints.add(latest)
            while (onChangePoints.size > maxPoints) {
                onChangePoints.removeAt(0)
            }
        }
        return onChangePoints.toList()
    }

    private fun rollingWindow(source: List<Float>): List<Float> =
        if (source.size <= maxPoints) source else source.takeLast(maxPoints)
}

class PlotDisplayHistories(private val maxPoints: Int) {
    private val traces = mutableListOf<PlotDisplayHistory>()

    fun pointsFor(
        sources: List<List<Float>>,
        modes: List<PlotGraphMode>,
    ): List<List<Float>> {
        while (traces.size < sources.size) {
            traces.add(PlotDisplayHistory(maxPoints))
        }
        return sources.mapIndexed { index, source ->
            traces[index].points(
                source = source,
                mode = modes.getOrElse(index) { PlotGraphMode.CONTINUOUS },
            )
        }
    }
}

fun compactConsecutiveDuplicates(points: List<Float>): List<Float> {
    if (points.size <= 1) return points
    val out = ArrayList<Float>(points.size)
    var last = points[0]
    out.add(last)
    for (i in 1 until points.size) {
        val value = points[i]
        if (value != last) {
            out.add(value)
            last = value
        }
    }
    return out
}
