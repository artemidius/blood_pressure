package com.artemidius.bloodpressure.activity

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.artemidius.bloodpressure.compose.screens.ScreenScaffold
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.artemidius.bloodpressure.R
import com.artemidius.bloodpressure.ui.theme.BloodPressureTheme


/**
 * Activity responsible for displaying the privacy policy to the user.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        setContent {
            BloodPressureTheme {
                ScreenScaffold {
                    val html = stringResource(R.string.policy_html)
                    val scrollState = rememberScrollState(0)
                    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        factory = { context -> TextView(context) },
                        update = {
                            it.setTextColor(textColor)
                            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        }
                    )
                }
            }
        }
    }
}
