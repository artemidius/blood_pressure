package com.artemidius.bloodpressure.activity

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.artemidius.bloodpressure.R
import com.artemidius.bloodpressure.activity.ui.theme.BloodPressureTheme


/**
 * Activity responsible for displaying the privacy policy to the user.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloodPressureTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val html = stringResource(R.string.policy_html)
                    val scrollState = rememberScrollState(0)
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        factory = { context -> TextView(context) },
                        update = {
                            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        }
                    )
                }
            }
        }
    }
}
