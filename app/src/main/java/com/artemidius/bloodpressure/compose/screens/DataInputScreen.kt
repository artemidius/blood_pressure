package com.artemidius.bloodpressure.compose.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import com.artemidius.bloodpressure.R
import com.artemidius.bloodpressure.compose.picker.NumberPicker
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.HealthConnectState

@Composable
fun HealthPermissionDialog(
    showPermissionDialog: Boolean,
    updatePermissionStatus: (Boolean) -> Unit,
) {
    val healthPermission = setOf(HealthPermission.getWritePermission(BloodPressureRecord::class))
    val permissionContract = PermissionController.createRequestPermissionResultContract()
    val healthPermissionLauncher = rememberLauncherForActivityResult(permissionContract) { granted ->
        updatePermissionStatus(granted.containsAll(healthPermission))
    }
    LaunchedEffect(showPermissionDialog) {
        if (showPermissionDialog) healthPermissionLauncher.launch(healthPermission)
    }
}

@Composable
fun DataInputScreen(
    data: BloodPressureScreenState.Data,
    healthConnectState: HealthConnectState,
    setSystolic: (Int) -> Unit,
    setDiastolic: (Int) -> Unit,
    logOut: () -> Unit,
    submit: () -> Unit,
    saveFile: () -> Unit,
    syncData: (Boolean) -> Unit,
    launchAction: (InputScreenAction) -> Unit,
    updatePermissionStatus: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    HealthPermissionDialog(healthConnectState.launchPermissionDialog) {
        updatePermissionStatus(it)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                NumberPicker(
                    value = data.systolic,
                    range = 90..250,
                    onValueChange = { setSystolic(it) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "X",
                    modifier = Modifier.padding(24.dp)
                )
                NumberPicker(
                    value = data.diastolic,
                    range = 50..170,
                    onValueChange = { setDiastolic(it) },
                    modifier = Modifier.weight(1f)
                )
            }
            healthConnectState.syncSwitch?.isChecked?.let { isChecked ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = syncData
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.sync_data_switch))
                }
                Spacer(Modifier.height(32.dp))
            }
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.button_submit))
            }
        }
        ButtonsRow(
            saveFile = saveFile,
            showList = { launchAction(InputScreenAction.LaunchListScreen) },
            logOut = logOut,
            showGraph = { launchAction(InputScreenAction.LaunchGraphScreen) },
            openCamera = { launchAction(InputScreenAction.LaunchCamera) }
        )
    }
}
