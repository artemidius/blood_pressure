package com.artemidius.bloodpressure.compose.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.BloodPressureViewModel
import com.firebase.ui.auth.AuthUI

@Composable
fun BloodPressureScreen(
    launchSuccessScreen: () -> Unit,
    launchLoginScreen: () -> Unit,
    launchListScreen: () -> Unit,
    launchGraphScreen: () -> Unit,
    launchFilePicker: () -> Unit,
    launchCamera: () -> Unit,
    viewModel: BloodPressureViewModel
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            when (val state = viewModel.state.collectAsState().value) {
                is BloodPressureScreenState.Data -> {
                    DataInputScreen(
                        data = state,
                        setSystolic = viewModel::setSys,
                        setDiastolic = viewModel::setDia,
                        submit = {
                            viewModel.submitData()
                            launchSuccessScreen()
                        },
                        logOut = { viewModel.showLogoutDialog(true) },
                        showList = launchListScreen,
                        saveFile = launchFilePicker,
                        showGraph = launchGraphScreen,
                        openCamera = launchCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (state.showLogoutDialog) {
                        LogoutDialog(
                            onConfirmation = {
                                logOut(
                                    ctx = context,
                                    listener = {
                                        viewModel.showLogoutDialog(false)
                                        viewModel.refresh()
                                    }
                                )
                            },
                            onDismissRequest = { viewModel.showLogoutDialog(false) }
                        )
                    }
                }
                BloodPressureScreenState.Loading -> Loading()
                BloodPressureScreenState.Unauthorized -> launchLoginScreen()
            }
        }
        else -> { Loading() }
    }
}

private fun logOut(ctx: Context, listener: () -> Unit) {
    AuthUI.getInstance()
        .signOut(ctx)
        .addOnCompleteListener { listener() }
}
