package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.artemidius.bloodpressure.compose.picker.NumberPicker
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.data.displayDate
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState

@Composable
fun DataInputScreen(data: BloodPressureScreenState.Data, setSystolic: (String) -> Unit,
    setDiastolic: (String) -> Unit, setTimestamp: (Long) -> Unit, submit: () -> Unit,
    newReading: () -> Unit, scan: () -> Unit, history: () -> Unit, graph: () -> Unit,
    modifier: Modifier = Modifier) {
    if (data.loading) {
        Loading(modifier.semantics { contentDescription = "Loading readings" })
        return
    }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (data.editingId == null) "New reading" else "Edit reading", style = MaterialTheme.typography.headlineMedium)
        Text("Enter the values shown on your blood pressure monitor.")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PressurePicker("Systolic", data.systolic.toIntOrNull() ?: 120, setSystolic, !data.saving, Modifier.weight(1f))
            Text("X", Modifier.padding(horizontal = 24.dp))
            PressurePicker("Diastolic", data.diastolic.toIntOrNull() ?: 80, setDiastolic, !data.saving, Modifier.weight(1f))
        }
        OutlinedButton(enabled = !data.saving, onClick = { choosingTime = true }) { Text("Measured: ${PressureData(0, 0, data.timestamp).displayDate()}") }
        Button(onClick = submit, enabled = !data.saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (data.saving) "Saving…" else if (data.editingId == null) "Save reading" else "Save changes")
        }
        data.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (data.editingId != null || data.systolic.isNotEmpty() || data.diastolic.isNotEmpty()) {
            TextButton(onClick = newReading, enabled = !data.saving) { Text("Start a new reading") }
        }
        HorizontalDivider()
        OutlinedButton(onClick = scan, enabled = !data.saving, modifier = Modifier.fillMaxWidth()) { Text("Scan monitor") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = history, modifier = Modifier.weight(1f)) { Text("History") }
            OutlinedButton(onClick = graph, modifier = Modifier.weight(1f)) { Text("Trends") }
        }
    }
    if (choosingTime) MeasurementTimeDialog(data.timestamp, { choosingTime = false }, setTimestamp)

}

@Composable
private fun PressurePicker(label: String, value: Int, change: (String) -> Unit, enabled: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        NumberPicker(
            value = value,
            range = 1..300,
            enabled = enabled,
            onValueChange = { change(it.toString()) },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                stateDescription = "$value mmHg"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 1f..300f, 298)
                if (!enabled) disabled()
                else setProgress { target ->
                    change(target.toInt().coerceIn(1, 300).toString())
                    true
                }
            }
        )
        Text("mmHg", style = MaterialTheme.typography.labelMedium)
    }
}
