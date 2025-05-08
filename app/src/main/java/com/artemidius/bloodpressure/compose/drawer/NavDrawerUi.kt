package com.artemidius.bloodpressure.compose.drawer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.R
import com.artemidius.bloodpressure.compose.screens.InputScreenAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavDrawer(
    launchFilePicker: () -> Unit,
    launchLogout: () -> Unit,
    launchAction: (InputScreenAction) -> Unit,
    syncIsChecked: Boolean,
    onSyncChanged: (Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    BackHandler {
        if (drawerState.isOpen) scope.launch { drawerState.close() }
        else onBackPressedDispatcher?.onBackPressed()
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.nav_draw_title),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_draw_item_save_file)) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                launchFilePicker()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_draw_item_camera)) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                launchAction(InputScreenAction.LaunchCamera)
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_draw_item_history)) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                launchAction(InputScreenAction.LaunchListScreen)
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_draw_item_sync)) },
                        selected = false,
                        badge = {
                            Switch(
                                checked = syncIsChecked,
                                onCheckedChange = onSyncChanged
                            )
                        },
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                launchAction(InputScreenAction.LaunchListScreen)
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_draw_item_log_out)) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                launchLogout()
                            }
                        }
                    )
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_draw_title)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}
