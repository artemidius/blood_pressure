package com.artemidius.bloodpressure

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.artemidius.bloodpressure.compose.screens.SettingsScreen
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.HealthConnectState
import com.artemidius.bloodpressure.viewmodel.SyncSwitch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun setSettings(
        state: BloodPressureScreenState.Data = BloodPressureScreenState.Data(loading = false),
        accountLabel: String = "user@example.com",
        isGuest: Boolean = false,
        busy: Boolean = false,
        authMessage: String? = null,
        healthState: HealthConnectState = HealthConnectState(),
        linkGoogle: () -> Unit = {},
        toggleSync: (Boolean) -> Unit = {},
        healthSettings: () -> Unit = {},
        export: () -> Unit = {},
        signOut: () -> Unit = {},
        deleteData: () -> Unit = {},
        deleteAccount: () -> Unit = {},
        privacy: () -> Unit = {}
    ) = compose.setContent {
        BloodPressureTheme {
            SettingsScreen(state, accountLabel, isGuest, busy, authMessage, healthState,
                linkGoogle, toggleSync, healthSettings, export, signOut, deleteData, deleteAccount, privacy)
        }
    }

    @Test fun guestIsOfferedGoogleLink() {
        var linked = 0
        val label = "Guest account. Link Google before signing out, reinstalling, or clearing app data to keep access to your readings."
        setSettings(accountLabel = label, isGuest = true, linkGoogle = { linked++ })
        compose.onNodeWithText(label).assertExists()
        compose.onNodeWithText("Link Google account").performClick()
        assertEquals(1, linked)
    }

    @Test fun signedInUserSeesEmailWithoutLinkOption() {
        setSettings(accountLabel = "user@example.com")
        compose.onNodeWithText("user@example.com").assertExists()
        compose.onNodeWithText("Link Google account").assertDoesNotExist()
    }

    @Test fun syncSwitchReportsRequestedChange() {
        var health by mutableStateOf(HealthConnectState(syncSwitch = SyncSwitch(true)))
        var requested: Boolean? = null
        compose.setContent {
            BloodPressureTheme {
                SettingsScreen(BloodPressureScreenState.Data(loading = false), "user@example.com", false, false, null,
                    health, {}, { requested = it }, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Copy readings to Health Connect").assertIsOn().performClick()
        compose.runOnIdle {
            assertEquals(false, requested)
            health = HealthConnectState(syncSwitch = SyncSwitch(false))
        }
        compose.onNodeWithContentDescription("Copy readings to Health Connect").assertIsOff()
    }

    @Test fun syncSwitchHiddenWhenHealthConnectUnavailable() {
        setSettings()
        compose.onNodeWithText("Copy readings").assertDoesNotExist()
        compose.onNodeWithText("Health Connect settings").assertExists()
    }

    @Test fun exportRequiresLoadedDataWithoutErrors() {
        var state by mutableStateOf(BloodPressureScreenState.Data(loading = true))
        var exported = 0
        compose.setContent {
            BloodPressureTheme {
                SettingsScreen(state, "user@example.com", false, false, null,
                    HealthConnectState(), {}, {}, {}, { exported++ }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Export CSV").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(loading = false, error = "Sync failed") }
        compose.onNodeWithText("Export CSV").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(error = null) }
        compose.onNodeWithText("Export CSV").performClick()
        assertEquals(1, exported)
    }

    @Test fun accountActionsInvokeCallbacks() {
        var signOut = 0
        var deleteData = 0
        var deleteAccount = 0
        setSettings(signOut = { signOut++ }, deleteData = { deleteData++ }, deleteAccount = { deleteAccount++ })
        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Delete all cloud readings").performClick()
        compose.onNodeWithText("Delete account and cloud readings").performClick()
        assertEquals(1, signOut)
        assertEquals(1, deleteData)
        assertEquals(1, deleteAccount)
    }

    @Test fun busyDisablesAccountActionsAndExport() {
        setSettings(busy = true)
        compose.onNodeWithText("Sign out").assertIsNotEnabled()
        compose.onNodeWithText("Delete all cloud readings").assertIsNotEnabled()
        compose.onNodeWithText("Delete account and cloud readings").assertIsNotEnabled()
        compose.onNodeWithText("Export CSV").assertIsNotEnabled()
        compose.onNodeWithText("Privacy and data").assertIsEnabled()
    }

    @Test fun messagesFromAccountHealthAndStorageAreShown() {
        setSettings(
            authMessage = "Sign-in cancelled.",
            healthState = HealthConnectState(message = "Health Connect unavailable"),
            state = BloodPressureScreenState.Data(loading = false, message = "Export saved"))
        compose.onNodeWithText("Sign-in cancelled.").assertExists()
        compose.onNodeWithText("Health Connect unavailable").assertExists()
        compose.onNodeWithText("Export saved").assertExists()
    }
}
