package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.artemidius.bloodpressure.compose.picker.NumberPicker
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState

@Composable
fun DataInputScreen(
    data: BloodPressureScreenState.Data,
    setSystolic: (Int) -> Unit,
    setDiastolic: (Int) -> Unit,
    logOut: () -> Unit,
    showList: () -> Unit,
    showGraph: () -> Unit,
    submit: () -> Unit,
    saveFile: () -> Unit,
    openCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                NumberPicker(
                    value = data.systolic,
                    range = 90..250,
                    onValueChange = { setSystolic(it) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "X",
                    modifier = Modifier.padding(24.dp)
                )
                NumberPicker(
                    value = data.diastolic,
                    range = 50..170,
                    onValueChange = { setDiastolic(it) },
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.button_submit))
            }
        }
        ButtonsRow(
            saveFile = saveFile,
            showList = showList,
            logOut = logOut,
            showGraph = showGraph,
            openCamera = openCamera
        )
    }
}
