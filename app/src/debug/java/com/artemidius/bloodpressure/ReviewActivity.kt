package com.artemidius.bloodpressure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.artemidius.bloodpressure.activity.configureEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.artemidius.bloodpressure.compose.screens.*
import com.artemidius.bloodpressure.data.PressureData
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState
import com.artemidius.bloodpressure.viewmodel.ScanState

/** Debug-only visual fixture. Never reads or writes Firebase or Health Connect. */
class ReviewActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        val now = System.currentTimeMillis()
        val readings = listOf(PressureData(124, 79, now, "one"), PressureData(119, 76, now - 86400000, "two"), PressureData(127, 82, now - 5 * 86400000, "three"))
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (intent.getBooleanExtra("large", false)) 1.6f else density.fontScale)) {
                BloodPressureTheme(darkTheme = intent.getBooleanExtra("dark", false), dynamicColor = false) {
                    var route by remember { mutableStateOf(intent.getStringExtra("screen") ?: "home") }
                    var data by remember { mutableStateOf(BloodPressureScreenState.Data(systolic = "124", diastolic = "79", list = readings, loading = false)) }
                    if (route == "scan") ScanContent(ScanState(), { route = "home" }, {}, {})
                    else if (route == "loading") ScreenScaffold { Loading() }
                    else ScreenScaffold(topBar = { TopAppBar(title = { Text("Blood Pressure") }) }) {
                        when (route) {
                            "graph" -> GraphScreen(readings)
                            "history" -> ListScreen(data, { data = data.copy(systolic = it.systolic.toString(), diastolic = it.diastolic.toString(), editingId = it.id); route = "home" }, {})
                            else -> DataInputScreen(data, { data = data.copy(systolic = it) }, { data = data.copy(diastolic = it) }, { data = data.copy(timestamp = it) }, { data = data.copy(message = "Preview only — no data saved.") }, { data = data.copy(systolic = "120", diastolic = "80") }, {}, { route = "history" }, { route = "graph" })
                        }
                    }
                }
            }
        }
    }
}
