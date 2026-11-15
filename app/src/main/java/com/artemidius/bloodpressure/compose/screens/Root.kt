package com.artemidius.bloodpressure.compose.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemidius.bloodpressure.activity.PermissionsRationaleActivity
import com.artemidius.bloodpressure.auth.signInIntent
import com.artemidius.bloodpressure.data.awaitResult
import com.artemidius.bloodpressure.viewmodel.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root() {
    val context = LocalContext.current
    val vm: BloodPressureViewModel = viewModel()
    val health: HealthConnectViewModel = viewModel()
    val scanner: ScanViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val healthState by health.state.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf("home") }
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    var authMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reauthUser by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val drawer = rememberDrawerState(DrawerValue.Closed)

    DisposableEffect(vm) {
        var previousUserId = Firebase.auth.currentUser?.uid
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (previousUserId != null && previousUserId != auth.currentUser?.uid) health.enableDataSync(false)
            if (previousUserId != auth.currentUser?.uid) { scanner.cancel(); route = "home" }
            previousUserId = auth.currentUser?.uid
            user = auth.currentUser
            vm.refresh()
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }
    LaunchedEffect(route) { if (route != "camera") scanner.cancel() }
    DisposableEffect(owner, health) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) health.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val authLauncher = rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
        user = Firebase.auth.currentUser
        val expected = reauthUser
        reauthUser = null
        if (result.resultCode == Activity.RESULT_OK) {
            vm.refresh()
            authMessage = null
            if (expected != null) {
                if (user?.uid == expected) confirmation = "account"
                else authMessage = "A different account was selected. No account was deleted."
            }
        } else {
            authMessage = if (result.idpResponse?.error != null)
                "Sign-in did not complete. Your current account is unchanged. If this Google account already exists, export guest readings before switching accounts."
            else "Sign-in cancelled."
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { vm.saveFile(it, context.contentResolver) }
    val healthPermissions = setOf(HealthPermission.getWritePermission(BloodPressureRecord::class))
    val healthLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        health.updatePermissionStatus(it.containsAll(healthPermissions))
    }
    LaunchedEffect(healthState.launchPermissionDialog) {
        if (healthState.launchPermissionDialog) {
            try { healthLauncher.launch(healthPermissions) }
            catch (_: Exception) { health.updatePermissionStatus(false) }
        }
    }
    BackHandler(enabled = drawer.isOpen || route != "home" || busy) {
        if (!busy) {
            if (drawer.isOpen) scope.launch { drawer.close() }
            else { scanner.cancel(); route = "home" }
        }
    }
    fun privacy() { context.startActivity(Intent(context, PermissionsRationaleActivity::class.java)) }
    fun healthSettings() {
        try { context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }
        catch (_: Exception) { vm.report("Install or update Health Connect, then try again.") }
    }

    when (val current = state) {
        BloodPressureScreenState.Loading -> ScreenScaffold { Loading() }
        BloodPressureScreenState.Unauthorized -> ScreenScaffold {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Blood Pressure", style = MaterialTheme.typography.headlineLarge)
                Text("Keep a record of your monitor readings, review trends, and export your history.")
                Text("Sign in with Google to recover your history on another device. Guest readings are tied to this installation; link Google before signing out or clearing app data.")
                Button(onClick = { authLauncher.launch(signInIntent()) }) { Text("Sign in or continue as guest") }
                authMessage?.let { Text(it) }
                TextButton(onClick = ::privacy) { Text("Privacy and data") }
            }
        }
        is BloodPressureScreenState.Data -> {
            if (route == "camera") {
                ScanScreen(scanner, onClosed = { scanner.cancel(); route = "home" }, onResult = {
                    vm.setInput(it)
                    route = "home"
                    Toast.makeText(context, "Reading updated: ${it.systolic}/${it.diastolic} mmHg", Toast.LENGTH_SHORT).show()
                })
            } else ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = !busy, drawerContent = {
                ModalDrawerSheet(windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Blood Pressure", style = MaterialTheme.typography.titleLarge)
                        listOf("home" to "New reading", "history" to "History", "graph" to "Trends", "settings" to "Account and settings").forEach { (destination, label) ->
                            NavigationDrawerItem(label = { Text(label) }, selected = route == destination, onClick = {
                                if (!busy) { route = destination; scope.launch { drawer.close() } }
                            })
                        }
                        TextButton(onClick = ::privacy) { Text("Privacy and data") }
                    }
                }
            }) {
                ScreenScaffold(topBar = {
                    TopAppBar(title = { Text("Blood Pressure") }, navigationIcon = {
                        IconButton(enabled = !busy, onClick = { if (route != "home") route = "home" else scope.launch { drawer.open() } }) {
                            Icon(if (route == "home") Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (route == "home") "Open menu" else "Back")
                        }
                    })
                }) {
                    Column(Modifier.fillMaxSize()) {
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (current.loading && route != "home") LinearProgressIndicator(Modifier.fillMaxWidth())
                        current.error?.let { error ->
                            Row(Modifier.padding(horizontal = 16.dp)) {
                                Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = vm::refresh) { Text("Retry") }
                            }
                        }
                        if (!current.loading && (current.pending || current.cached)) Text(
                            if (current.pending) "Changes are waiting for cloud confirmation." else "Showing cached readings; cloud may be unavailable.",
                            Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall)
                        when (route) {
                            "history" -> ListScreen(current, edit = { vm.edit(it); route = "home" }, delete = vm::delete)
                            "graph" -> GraphScreen(current.list)
                            "settings" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Account and settings", style = MaterialTheme.typography.headlineSmall)
                                Text(if (user?.isAnonymous == true) "Guest account. Link Google before signing out, reinstalling, or clearing app data to keep access to your readings." else user?.email ?: "Signed in")
                                if (user?.isAnonymous == true) Button(enabled = !busy && !current.saving, onClick = { authLauncher.launch(signInIntent(upgrade = true)) }) { Text("Link Google account") }
                                authMessage?.let { Text(it) }
                                HorizontalDivider()
                                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                                Text("When enabled, saving copies this reading to Health Connect. Saving changes updates its linked copy. Existing history is not automatically imported or copied.")
                                healthState.syncSwitch?.let { toggle ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Copy readings", Modifier.weight(1f))
                                        Switch(checked = toggle.isChecked, enabled = !busy, onCheckedChange = health::enableDataSync,
                                            modifier = Modifier.semantics { contentDescription = "Copy readings to Health Connect" })
                                    }
                                }
                                healthState.message?.let { Text(it) }
                                TextButton(onClick = ::healthSettings) { Text("Health Connect settings") }
                                HorizontalDivider()
                                Button(enabled = !busy && !current.loading && current.error == null, onClick = { exportLauncher.launch(vm.fileName) }) { Text("Export CSV") }
                                Text("Export includes the readings currently shown in history. Dates are in UTC.")
                                current.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                                TextButton(enabled = !busy && !current.saving && !current.pending, onClick = { confirmation = "logout" }) { Text("Sign out") }
                                TextButton(enabled = !busy && !current.saving && !current.pending, onClick = { confirmation = "data" }) { Text("Delete all cloud readings") }
                                TextButton(enabled = !busy && !current.saving && !current.pending, onClick = {
                                    val currentUser = Firebase.auth.currentUser
                                    val lastSignIn = currentUser?.metadata?.lastSignInTimestamp ?: 0
                                    if (currentUser != null && !currentUser.isAnonymous && System.currentTimeMillis() - lastSignIn > 240_000) {
                                        reauthUser = currentUser.uid
                                        authLauncher.launch(signInIntent(upgrade = true))
                                    } else confirmation = "account"
                                }) { Text("Delete account and cloud readings") }
                                TextButton(onClick = ::privacy) { Text("Privacy and data") }
                            }
                            else -> DataInputScreen(current, vm::setSys, vm::setDia, vm::setTimestamp, vm::submitData, vm::newReading,
                                scan = { route = "camera" }, history = { route = "history" }, graph = { route = "graph" })
                        }
                    }
                }
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(when (action) { "logout" -> "Sign out?"; "data" -> "Delete all cloud readings?"; else -> "Delete account and cloud readings?" }) },
            text = { Text(if (action == "logout") {
                if (user?.isAnonymous == true) "Guest access cannot be recovered after signing out. Link a Google account or export your readings first."
                else "Your cloud readings remain available when you sign in again."
            } else "This permanently deletes your cloud readings${if (action == "account") " and account" else ""}. Export first if you need a copy. Exported files and existing Health Connect copies must be deleted separately.") },
            confirmButton = { TextButton(onClick = {
                confirmation = null
                busy = true
                val target = Firebase.auth.currentUser
                scope.launch {
                    try {
                        check(target != null && Firebase.auth.currentUser?.uid == target.uid)
                        if (action == "logout") AuthUI.getInstance().signOut(context).awaitResult()
                        else {
                            vm.deleteAllReadings()
                            if (action == "account") target.delete().awaitResult()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        vm.report("Could not complete the request. Some cloud readings may already be deleted. Reconnect or sign in again, then retry.")
                    } finally { busy = false }
                }
            }) { Text(if (action == "logout") "Sign out" else "Delete permanently") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } })
    }
}
