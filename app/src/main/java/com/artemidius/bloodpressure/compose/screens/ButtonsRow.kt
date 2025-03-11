package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemidius.bloodpressure.R

@Composable
fun ButtonsRow(
    saveFile: () -> Unit,
    showList: () -> Unit,
    showGraph: () -> Unit,
    openCamera: () -> Unit,
    logOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val size = 48.dp
        LargeIconButton(
            onClick = openCamera,
            icon = Icons.Default.Build,
            text = stringResource(R.string.nav_btn_camera),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = saveFile,
            icon = Icons.Filled.Share,
            text = stringResource(R.string.nav_btn_share),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = showGraph,
            icon = Icons.Filled.DateRange,
            text = stringResource(R.string.nav_btn_graph),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = showList,
            icon = Icons.AutoMirrored.Filled.List,
            text = stringResource(R.string.nav_btn_history),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = logOut,
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            text = stringResource(R.string.nav_btn_log_out),
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun LargeIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            content = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = modifier
                )
            },
            modifier = modifier
        )
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}