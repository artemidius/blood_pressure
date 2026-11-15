package com.artemidius.bloodpressure.compose.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.data.measurementTimestamp
import com.artemidius.bloodpressure.data.pickerDate
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeasurementTimeDialog(timestamp: Long, onDismiss: () -> Unit, onSelected: (Long) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val initial = remember(timestamp) { Instant.ofEpochMilli(timestamp).atZone(zone) }
    val today = pickerDate(System.currentTimeMillis(), zone)
    val date = rememberDatePickerState(initialSelectedDateMillis = pickerDate(timestamp, zone),
        selectableDates = remember(today) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            override fun isSelectableYear(year: Int) = year <= Instant.ofEpochMilli(today).atZone(java.time.ZoneOffset.UTC).year
        } })
    val time = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current))
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    var typingTime by rememberSaveable { mutableStateOf(false) }

    if (!choosingTime) {
        DatePickerDialog(onDismissRequest = onDismiss,
            confirmButton = { TextButton(enabled = date.selectedDateMillis != null,
                onClick = { choosingTime = true }) { Text("Next") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) {
            DatePicker(date, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    } else {
        BasicAlertDialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.width(IntrinsicSize.Min).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Measurement time", style = MaterialTheme.typography.headlineSmall)
                    Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        if (typingTime) TimeInput(time) else TimePicker(time)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { typingTime = !typingTime }) { Text(if (typingTime) "Clock" else "Type time") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = {
                            onSelected(measurementTimestamp(requireNotNull(date.selectedDateMillis), time.hour, time.minute, zone))
                            onDismiss()
                        }) { Text("Set") }
                    }
                }
            }
        }
    }
}
