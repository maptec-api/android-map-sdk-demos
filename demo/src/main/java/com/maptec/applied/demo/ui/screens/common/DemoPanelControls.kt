package com.maptec.applied.demo.ui.screens.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun DemoPanelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(6.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoPanelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    showRangeLabels: Boolean = true,
) {
    Column(modifier = modifier) {
        MaterialSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(28.dp),
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            interactionSource = interactionSource,
            steps = steps,
            valueRange = valueRange,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = enabled,
                    thumbSize = DpSize(14.dp, 14.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    enabled = enabled,
                    colors = colors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 2.dp,
                )
            },
        )
        if (showRangeLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDemoSliderValue(valueRange.start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDemoSliderValue(valueRange.endInclusive),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DemoPanelSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable (() -> Unit))? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    MaterialSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer(scaleX = 0.72f, scaleY = 0.72f),
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        onClick = onClick,
        modifier = modifier.height(36.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    )
}

@Composable
fun DemoNumericSliderField(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueTextChange: (String) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    rangeStartLabel: String,
    rangeEndLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    testTag: String? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DemoPanelSlider(
                value = value.coerceIn(valueRange),
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier.weight(1f),
                showRangeLabels = false,
            )
            OutlinedTextField(
                value = valueText,
                onValueChange = onValueTextChange,
                singleLine = true,
                enabled = enabled,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(88.dp)
                    .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = rangeStartLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = rangeEndLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(96.dp))
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error else Color.Gray,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

fun formatDemoSliderValue(value: Float): String =
    String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
