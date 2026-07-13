package com.maptec.applied.demo.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val defaultColorOptions = listOf(
    "#2F80ED",
    "#3A5EFB",
    "#3F7BF9",
    "#8CC8FF",
    "#CFE1FF",
    "#00A63E",
    "#A5D6A7",
    "#FF0000",
    "#FF9800",
    "#FFC107",
    "#9C27B0",
    "#00BCD4",
    "#424242",
    "#E0E0E0",
    "#FFFFFF",
    "#000000",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    testTag: String? = null,
    colorOptions: List<String> = defaultColorOptions,
) {
    var expanded by remember { mutableStateOf(false) }
    val normalizedValue = value.trim().uppercase()
    val options = remember(colorOptions, normalizedValue) {
        buildList {
            if (normalizedValue.isNotBlank() && colorOptions.none { it.equals(normalizedValue, ignoreCase = true) }) {
                add(normalizedValue)
            }
            addAll(colorOptions)
        }.distinctBy { it.uppercase() }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = normalizedValue,
            onValueChange = { onValueChange(it.trim().uppercase()) },
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = { ColorSwatch(normalizedValue) },
            trailingIcon = {
                IconButton(
                    onClick = { if (enabled) expanded = !expanded },
                    enabled = enabled,
                ) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            supportingText = supportingText,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { colorHex ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorSwatch(colorHex)
                            Text(colorHex.uppercase())
                        }
                    },
                    onClick = {
                        onValueChange(colorHex.uppercase())
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(colorHex: String) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(parseComposeColor(colorHex))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

private fun parseComposeColor(colorHex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(colorHex.trim())) }
        .getOrDefault(Color.Transparent)
}
