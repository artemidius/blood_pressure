package com.artemidius.bloodpressure.compose.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

@Composable
fun PressureListItem(
    start: String,
    end: String,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp)
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        val composableWidth = maxWidth
        val textMeasurer = rememberTextMeasurer()

        val startTextStartPadding = 0.dp
        val startTextEndPadding = 4.dp
        val endTextStartPadding = 0.dp
        val endTextEndPadding = 0.dp

        val startWidth = textMeasurer.measure(
            text = start,
            style = textStyle,
        ).size.width.pxToDp()
        val endWidth = textMeasurer.measure(
            text = end,
            style = textStyle,
        ).size.width.pxToDp()
        val dotWidth = textMeasurer.measure(
            text = ".",
            style = textStyle,
        ).size.width.pxToDp()

        val composableWidthInFloat = floor(composableWidth.value)
        val startAndEndComposableWidthWithPadding = (startWidth.value) + (endWidth.value) +
                (startTextStartPadding.value) + (startTextEndPadding.value) +
                (endTextStartPadding.value) + (endTextEndPadding.value)

        val calculatedNumberOfDots =
            ((composableWidthInFloat - startAndEndComposableWidthWithPadding) / dotWidth.value).toInt()
        val numberOfDots = if (calculatedNumberOfDots < 0) {
            0
        } else {
            calculatedNumberOfDots
        }
        val dotText = ".".repeat(numberOfDots)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = start,
                style = textStyle,
                modifier = Modifier
                    .padding(
                        start = startTextStartPadding,
                        end = startTextEndPadding,
                    )
            )
            Text(
                text = dotText,
                style = textStyle,
                modifier = Modifier.weight(1F),
            )
            Text(
                text = end,
                style = textStyle,
                modifier = Modifier
                    .padding(
                        start = endTextStartPadding,
                        end = endTextEndPadding,
                    )
            )
        }
    }
}

@Composable
private fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp()}

@Preview(showBackground = true)
@Composable
fun PressureListItemPreview() {
    PressureListItem(
        start = "120",
        end = "80"
    )
}