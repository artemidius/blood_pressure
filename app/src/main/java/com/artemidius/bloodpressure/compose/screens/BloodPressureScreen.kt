package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.artemidius.bloodpressure.compose.drawer.NavDrawer
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.BloodPressureViewModel
import com.artemidius.bloodpressure.viewmodel.HealthConnectViewModel

@Composable
fun BloodPressureScreen(
    launchFilePicker: () -> Unit,
    launchAction: (InputScreenAction) -> Unit,
    logOut: (() -> Unit) -> Unit,
    bloodPressureViewModel: BloodPressureViewModel,
    healthConnectViewModel: HealthConnectViewModel
) {
    when (val state = bloodPressureViewModel.state.collectAsState().value) {
        is BloodPressureScreenState.Data -> {
            NavDrawer(
                launchFilePicker = launchFilePicker,
                launchLogout = { bloodPressureViewModel.showLogoutDialog(true) },
                syncIsChecked = healthConnectViewModel.state.collectAsState().value.syncSwitch?.isChecked == true,
                onSyncChanged = healthConnectViewModel::enableDataSync,
                launchAction = launchAction
            ) { insets ->
                DataInputScreen(
                    data = state,
                    setSystolic = bloodPressureViewModel::setSys,
                    setDiastolic = bloodPressureViewModel::setDia,
                    submit = {
                        bloodPressureViewModel.submitData()
                        launchAction(InputScreenAction.LaunchSuccessScreen)
                    },
                    updatePermissionStatus = healthConnectViewModel::updatePermissionStatus,
                    healthConnectState = healthConnectViewModel.state.collectAsState().value,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(insets)
                )
            }
            if (state.showLogoutDialog) {
                LogoutDialog(
                    onConfirmation = {
                        logOut {
                            bloodPressureViewModel.showLogoutDialog(false)
                            bloodPressureViewModel.refresh()
                        }
                    },
                    onDismissRequest = { bloodPressureViewModel.showLogoutDialog(false) }
                )
            }
        }
        BloodPressureScreenState.Loading -> Loading()
        BloodPressureScreenState.Unauthorized -> launchAction(InputScreenAction.LaunchLoginScreen)
    }
}
