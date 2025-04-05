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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
            painter = painterResource(id = R.drawable.baseline_camera_alt_24),
            text = stringResource(R.string.nav_btn_camera),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = saveFile,
            painter = painterResource(id = R.drawable.baseline_save_alt_24),
            text = stringResource(R.string.nav_btn_share),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = showList,
            imageVector = Icons.AutoMirrored.Filled.List,
            text = stringResource(R.string.nav_btn_history),
            modifier = Modifier.size(size)
        )
        Spacer(Modifier.weight(1f))
        LargeIconButton(
            onClick = logOut,
            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
            text = stringResource(R.string.nav_btn_log_out),
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun LargeIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
   LargeIconButton(
       onClick,
       text
   ) {
       Icon(
           imageVector = imageVector,
           contentDescription = null,
           modifier = modifier
       )
   }
}

@Composable
private fun LargeIconButton(
    onClick: () -> Unit,
    painter: Painter,
    text: String,
    modifier: Modifier = Modifier
) {
    LargeIconButton(onClick, text) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Composable
private fun LargeIconButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            content = {
                icon()
            },
            modifier = modifier
        )
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}