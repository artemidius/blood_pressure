package com.artemidius.bloodpressure.compose.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.artemidius.bloodpressure.auth.signInIntent
import com.artemidius.bloodpressure.navigation.Camera
import com.artemidius.bloodpressure.navigation.DataGraph
import com.artemidius.bloodpressure.navigation.DataInput
import com.artemidius.bloodpressure.navigation.Login
import com.artemidius.bloodpressure.navigation.PressureList
import com.artemidius.bloodpressure.navigation.Success
import com.artemidius.bloodpressure.viewmodel.BloodPressureViewModel
import com.artemidius.bloodpressure.viewmodel.CameraPreviewViewModel
import com.artemidius.bloodpressure.viewmodel.HealthConnectViewModel
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract

@ExperimentalGetImage
@Composable
fun Root() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bloodPressureViewModel: BloodPressureViewModel = viewModel()
    val cameraPreviewViewModel: CameraPreviewViewModel = viewModel()
    val healthConnectViewModel: HealthConnectViewModel = viewModel()
    val authLauncher = rememberLauncherForActivityResult(
        contract = FirebaseAuthUIActivityResultContract(),
        onResult = {
            bloodPressureViewModel.refresh()
            navController.navigate(DataInput)
        }
    )
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { bloodPressureViewModel.saveFile(it, context.contentResolver) }
    )
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DataInput,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable<DataInput> {
                BloodPressureScreen(
                    launchFilePicker = { filePickerLauncher.launch(bloodPressureViewModel.fileName) },
                    launchAction = navController::launchAction,
                    bloodPressureViewModel = bloodPressureViewModel,
                    healthConnectViewModel = healthConnectViewModel
                )
            }
            composable<Success> {
                SuccessScreen(
                    onClose = { (context as Activity).finish() },
                    onGoToMain = {
                        navController.navigate(DataInput)
                        bloodPressureViewModel.refresh()
                    },
                )
            }
            composable<PressureList> {
                ListScreen(
                    state = bloodPressureViewModel.state.collectAsState().value,
                    goBack = {
                        bloodPressureViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }
            composable<Login> {
                Unauthorized(
                    launchAuthScreen = { authLauncher.launch(signInIntent) },
                )
            }
            composable<DataGraph> {
                GraphScreen(bloodPressureViewModel)
            }
            composable<Camera> {
                CameraScreen(
                    onClosed = navController::popBackStack,
                    onResult = {
                        bloodPressureViewModel.setInput(it)
                        navController.popBackStack()
                    },
                    viewModel = cameraPreviewViewModel,
                )
            }
        }
    }
}

fun NavController.launchAction(action: InputScreenAction) {
    when (action) {
        InputScreenAction.LaunchCamera -> navigate(Camera)
        InputScreenAction.LaunchGraphScreen -> navigate(DataGraph)
        InputScreenAction.LaunchListScreen -> navigate(PressureList)
        InputScreenAction.LaunchLoginScreen -> navigate(Login)
        InputScreenAction.LaunchSuccessScreen -> navigate(Success)
    }
}
