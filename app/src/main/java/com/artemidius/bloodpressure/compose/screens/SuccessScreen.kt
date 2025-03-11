package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemidius.bloodpressure.R

@Composable
fun SuccessScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onGoToMain: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.success_screen_message),
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onClose() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(stringResource(R.string.button_close_app))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onGoToMain() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .alpha(0.6f)
        ) {
            Text(stringResource(R.string.button_go_to_main))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SuccessScreenPreview() {
    SuccessScreen(
        onClose = {},
        onGoToMain = {}
    )
}

