package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.data.*
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState

@Composable
fun ListScreen(state: BloodPressureScreenState.Data, edit: (PressureData) -> Unit, delete: (PressureData) -> Unit) {
    var deleting by remember { mutableStateOf<PressureData?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium) }
        if (state.list.isEmpty() && !state.loading && state.error == null) item { Text("No readings yet. Add your first reading from the main screen.") }
        items(state.list, key = { it.id }) { reading ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(reading.displayDate(), style = MaterialTheme.typography.bodyMedium)
                    Text("${reading.systolic}/${reading.diastolic} mmHg", style = MaterialTheme.typography.headlineSmall)
                    Row {
                        TextButton(enabled = !state.saving, onClick = { edit(reading) }) { Text("Edit") }
                        TextButton(enabled = !state.saving, onClick = { deleting = reading }) { Text("Delete") }
                    }
                }
            }
        }
        state.message?.let { message -> item { Text(message) } }
    }
    deleting?.let { reading ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete reading?") },
            text = { Text("${reading.systolic}/${reading.diastolic} mmHg, ${reading.displayDate()}. If Health Connect copying is enabled, its linked copy will also be removed. Older unlinked copies must be removed in Health Connect.") },
            confirmButton = { TextButton(onClick = { deleting = null; delete(reading) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
