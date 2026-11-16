package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.HealthConnectState

@Composable
fun SettingsScreen(
    state: BloodPressureScreenState.Data,
    accountLabel: String,
    isGuest: Boolean,
    busy: Boolean,
    authMessage: String?,
    healthState: HealthConnectState,
    linkGoogle: () -> Unit,
    toggleSync: (Boolean) -> Unit,
    healthSettings: () -> Unit,
    export: () -> Unit,
    signOut: () -> Unit,
    deleteData: () -> Unit,
    deleteAccount: () -> Unit,
    privacy: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Account and settings", style = MaterialTheme.typography.headlineSmall)
        Text(accountLabel)
        if (isGuest) Button(enabled = !busy && !state.saving, onClick = linkGoogle) { Text("Link Google account") }
        authMessage?.let { Text(it) }
        HorizontalDivider()
        Text("Health Connect", style = MaterialTheme.typography.titleMedium)
        Text("When enabled, saving copies this reading to Health Connect. Saving changes updates its linked copy. Existing history is not automatically imported or copied.")
        healthState.syncSwitch?.let { toggle ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Copy readings", Modifier.weight(1f))
                Switch(checked = toggle.isChecked, enabled = !busy, onCheckedChange = toggleSync,
                    modifier = Modifier.semantics { contentDescription = "Copy readings to Health Connect" })
            }
        }
        healthState.message?.let { Text(it) }
        TextButton(onClick = healthSettings) { Text("Health Connect settings") }
        HorizontalDivider()
        Button(enabled = !busy && !state.loading && state.error == null, onClick = export) { Text("Export CSV") }
        Text("Export includes the readings currently shown in history. Dates are in UTC.")
        state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        TextButton(enabled = !busy && !state.saving && !state.pending, onClick = signOut) { Text("Sign out") }
        TextButton(enabled = !busy && !state.saving && !state.pending, onClick = deleteData) { Text("Delete all cloud readings") }
        TextButton(enabled = !busy && !state.saving && !state.pending, onClick = deleteAccount) { Text("Delete account and cloud readings") }
        TextButton(onClick = privacy) { Text("Privacy and data") }
    }
}
