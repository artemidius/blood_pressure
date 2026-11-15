package com.artemidius.bloodpressure.compose.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.viewmodel.ScanState
import com.artemidius.bloodpressure.viewmodel.ScanViewModel

@Composable
fun ScanScreen(viewModel: ScanViewModel, onClosed: () -> Unit, onResult: (ImageData) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val latestResult by rememberUpdatedState(onResult)
    val camera = rememberLauncherForActivityResult(TakePicture(), viewModel::photoReturned)
    val gallery = rememberLauncherForActivityResult(PickVisualMedia(), viewModel::galleryReturned)
    LaunchedEffect(state.result) {
        if (state.result != null) viewModel.consumeResult()?.let(latestResult)
    }
    ScanContent(state, onClosed, takePhoto = {
        viewModel.takePhoto()?.let { uri ->
            try { camera.launch(uri) } catch (_: Exception) { viewModel.launchFailed() }
        }
    }, chooseImage = {
        viewModel.chooseGallery()
        try { gallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
        catch (_: Exception) { viewModel.launchFailed() }
    })
}

@Composable
fun ScanContent(state: ScanState, onClosed: () -> Unit, takePhoto: () -> Unit, chooseImage: () -> Unit) {
    ScreenScaffold {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = onClosed) { Text(if (state.processing) "Cancel" else "Back") }
            if (state.processing || state.result != null) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Recognizing reading" })
                    Spacer(Modifier.height(20.dp))
                    Text("Recognizing reading…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            } else {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (state.error == null) "Scan monitor" else "Try another image", style = MaterialTheme.typography.headlineSmall)
                    state.error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
                    Text("Take a photo or choose an image showing both SYS and DIA values clearly.")
                    Button(onClick = takePhoto, enabled = !state.waitingForImage, modifier = Modifier.fillMaxWidth()) { Text("Camera") }
                    OutlinedButton(onClick = chooseImage, enabled = !state.waitingForImage, modifier = Modifier.fillMaxWidth()) { Text("Gallery") }
                    if (state.waitingForImage) Text("Waiting for an image…")
                }
            }
        }
    }
}
