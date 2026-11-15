package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.data.*
import com.artemidius.bloodpressure.viewmodel.graphSeries

@Composable
fun GraphScreen(readings: List<PressureData>) {
    var days by rememberSaveable { mutableIntStateOf(30) }
    val cutoff = if (days == 0) Long.MIN_VALUE else System.currentTimeMillis() - days * 86_400_000L
    val selected = readings.filter { it.timestamp >= cutoff }.sortedBy { it.timestamp }
    val series = remember(selected) { graphSeries(selected) }
    val sysColor = MaterialTheme.colorScheme.primary
    val diaColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Trends", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7 to "7 days", 30 to "30 days", 0 to "All").forEach { (value, label) ->
                FilterChip(selected = days == value, onClick = { days = value }, label = { Text(label) })
            }
        }
        if (selected.isEmpty()) { Text("No readings in this period."); return@Column }
        Text("Systolic — solid line", color = sysColor)
        Text("Diastolic — dashed line", color = diaColor)
        Text("Pressure (mmHg)")
        Row(Modifier.fillMaxWidth().height(240.dp)) {
            Column(Modifier.fillMaxHeight().width(44.dp), verticalArrangement = Arrangement.SpaceBetween) {
                for (i in 4 downTo 0) Text((series.minimum + (series.maximum - series.minimum) * i / 4).toString(), style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp).semantics {
                contentDescription = "Blood pressure trend, ${selected.size} readings. Exact values follow below."
            }) {
                val inset = 6.dp.toPx()
                fun point(x: Float, y: Float) = Offset(inset + x * (size.width - 2 * inset), (1 - y) * size.height)
                repeat(5) { i -> val y = size.height * i / 4; drawLine(gridColor, Offset(0f, y), Offset(size.width, y)) }
                listOf(true, false).forEach { sys ->
                    val path = Path()
                    series.points.forEachIndexed { index, item ->
                        val pos = point(item.x, if (sys) item.systolic else item.diastolic)
                        if (index == 0) path.moveTo(pos.x, pos.y) else path.lineTo(pos.x, pos.y)
                        drawCircle(if (sys) sysColor else diaColor, 4.dp.toPx(), pos)
                    }
                    drawPath(path, if (sys) sysColor else diaColor, style = Stroke(2.dp.toPx(), pathEffect = if (sys) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }
            }
        }
        Text("${selected.first().displayDate()} — ${selected.last().displayDate()}", style = MaterialTheme.typography.labelMedium)
        Text("Time runs left to right; gaps reflect elapsed time.")
        HorizontalDivider()
        Text("Readings in this period", style = MaterialTheme.typography.titleMedium)
        selected.asReversed().forEach { Text("${it.displayDate()}: ${it.systolic}/${it.diastolic} mmHg") }
    }
}
