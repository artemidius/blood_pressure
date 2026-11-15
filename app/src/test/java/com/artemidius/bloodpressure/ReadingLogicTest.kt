package com.artemidius.bloodpressure

import com.artemidius.bloodpressure.data.*
import com.artemidius.bloodpressure.viewmodel.graphSeries
import org.junit.Assert.*
import org.junit.Test

class ReadingLogicTest {
    @Test fun `csv preserves year UTC and stable ordering`() {
        val csv = pressureCsv(listOf(PressureData(130, 85, 1735689600000), PressureData(120, 80, 1704067200000)))
        assertEquals("systolic_mmHg,diastolic_mmHg,measured_at_UTC\r\n120,80,2024-01-01T00:00:00Z\r\n130,85,2025-01-01T00:00:00Z\r\n", csv)
    }
    @Test fun `validation rejects inverted zero and overflow without clamping`() {
        assertTrue(validPressure(85, 45))
        assertFalse(validPressure(80, 120))
        assertFalse(validPressure(120, 0))
        assertFalse(validPressure(301, 80))
    }
    @Test fun `graph uses elapsed time and both pressure values`() {
        val graph = graphSeries(listOf(PressureData(120, 80, 0), PressureData(130, 90, 1000), PressureData(140, 100, 10000)))
        assertEquals(0.1f, graph.points[1].x, 0.001f)
        assertTrue(graph.points[0].systolic > graph.points[0].diastolic)
        assertEquals(70, graph.minimum); assertEquals(150, graph.maximum)
    }
    @Test fun `empty and simultaneous readings avoid division by zero`() {
        assertTrue(graphSeries(emptyList()).points.isEmpty())
        assertEquals(0.5f, graphSeries(listOf(PressureData(120, 80, 1))).points.single().x)
        assertTrue(graphSeries(listOf(PressureData(120, 80, 1), PressureData(130, 85, 1))).points.all { it.x.isFinite() })
    }
}
