package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artemidius.bloodpressure.viewmodel.BloodPressureScreenState

@Composable
fun ListScreen(
    state: BloodPressureScreenState,
    goBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        BloodPressureScreenState.Loading -> Loading()
        is BloodPressureScreenState.Data -> {
            Column(modifier = modifier.fillMaxSize()) {
                IconButton(onClick = { goBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nav Back")
                }
                LazyColumn(modifier.padding(16.dp)) {
                    items(state.list.size) { index ->
                        val item = state.list[index]
                        PressureListItem(
                            start = item.date,
                            end = "${item.systolic}/${item.diastolic}",
                            textStyle = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
        else -> {}
    }
}
