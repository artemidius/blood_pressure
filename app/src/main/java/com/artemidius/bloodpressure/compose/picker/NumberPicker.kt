package com.artemidius.bloodpressure.compose.picker

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun NumberPicker(
    modifier: Modifier = Modifier,
    label: (Int) -> String = { it.toString() },
    value: Int,
    onValueChange: (Int) -> Unit,
    dividersColor: Color = MaterialTheme.colorScheme.primary,
    range: Iterable<Int>,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 24.sp),
) {
    ListItemPicker(
        modifier = modifier,
        label = label,
        value = value,
        onValueChange = onValueChange,
        dividersColor = dividersColor,
        list = range.toList(),
        textStyle = textStyle
    )
}