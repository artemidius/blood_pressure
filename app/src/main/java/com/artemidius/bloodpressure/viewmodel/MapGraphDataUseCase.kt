package com.artemidius.bloodpressure.viewmodel

import com.artemidius.bloodpressure.data.PressureData
import kotlin.math.ceil
import kotlin.math.floor

data class GraphPoint(val x: Float, val systolic: Float, val diastolic: Float)
data class GraphSeries(val points: List<GraphPoint>, val minimum: Int, val maximum: Int)

/** Coordinates in 0..1, with y increasing upward. Time spacing is preserved. */
fun graphSeries(readings: List<PressureData>): GraphSeries {
    if (readings.isEmpty()) return GraphSeries(emptyList(), 0, 100)
    val sorted = readings.sortedBy { it.timestamp }
    val minimum = (floor(sorted.minOf { minOf(it.systolic, it.diastolic) } / 10.0) * 10 - 10).toInt().coerceAtLeast(0)
    val maximum = (ceil(sorted.maxOf { maxOf(it.systolic, it.diastolic) } / 10.0) * 10 + 10).toInt()
    val span = sorted.last().timestamp - sorted.first().timestamp
    return GraphSeries(sorted.map {
        GraphPoint(if (span == 0L) 0.5f else ((it.timestamp - sorted.first().timestamp).toDouble() / span).toFloat(),
            (it.systolic - minimum).toFloat() / (maximum - minimum),
            (it.diastolic - minimum).toFloat() / (maximum - minimum))
    }, minimum, maximum)
}
