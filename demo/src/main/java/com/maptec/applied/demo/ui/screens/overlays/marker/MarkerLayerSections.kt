@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.marker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.MarkerLayerViewModel
import com.maptec.applied.style.Property

private val ICON_ANCHOR_OPTIONS = listOf(
    Property.ICON_ANCHOR_BOTTOM to R.string.marker_anchor_bottom,
    Property.ICON_ANCHOR_CENTER to R.string.marker_anchor_center,
    Property.ICON_ANCHOR_TOP to R.string.marker_anchor_top,
    Property.ICON_ANCHOR_LEFT to R.string.marker_anchor_left,
    Property.ICON_ANCHOR_RIGHT to R.string.marker_anchor_right,
    Property.ICON_ANCHOR_TOP_LEFT to R.string.marker_anchor_top_left,
    Property.ICON_ANCHOR_TOP_RIGHT to R.string.marker_anchor_top_right,
    Property.ICON_ANCHOR_BOTTOM_LEFT to R.string.marker_anchor_bottom_left,
    Property.ICON_ANCHOR_BOTTOM_RIGHT to R.string.marker_anchor_bottom_right,
)

private val FIELD_PAIR_PADDING = 8.dp

@Composable
internal fun MarkerCommonParamsSection(viewModel: MarkerLayerViewModel) {
    val defaultLatLng by viewModel.defaultLatLng.collectAsState()
    val iconOpacity by viewModel.iconOpacity.collectAsState()
    val iconAnchor by viewModel.iconAnchor.collectAsState()
    val iconScaleWithZoom by viewModel.iconScaleWithZoom.collectAsState()
    val iconSize by viewModel.iconSize.collectAsState()
    val minScale by viewModel.minScale.collectAsState()
    val minZoom by viewModel.minZoom.collectAsState()
    val maxZoom by viewModel.maxZoom.collectAsState()

    var iconOpacityStr by remember { mutableStateOf(formatMarkerValue(iconOpacity)) }
    var iconSizeStr by remember { mutableStateOf(formatMarkerValue(iconSize)) }
    var minScaleStr by remember { mutableStateOf(formatMarkerValue(minScale)) }
    var minZoomStr by remember { mutableStateOf(minZoom.toString()) }
    var maxZoomStr by remember { mutableStateOf(maxZoom.toString()) }
    var localOpacity by remember { mutableFloatStateOf(iconOpacity) }
    var localIconSize by remember { mutableFloatStateOf(iconSize) }
    var localMinScale by remember { mutableFloatStateOf(minScale) }
    var localMinZoom by remember { mutableIntStateOf(minZoom) }
    var localMaxZoom by remember { mutableIntStateOf(maxZoom) }
    var anchorExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(iconOpacity) { localOpacity = iconOpacity }
    LaunchedEffect(iconSize) { localIconSize = iconSize }
    LaunchedEffect(minScale) { localMinScale = minScale }
    LaunchedEffect(minZoom) {
        localMinZoom = minZoom
        minZoomStr = minZoom.toString()
    }
    LaunchedEffect(maxZoom) {
        localMaxZoom = maxZoom
        maxZoomStr = maxZoom.toString()
    }

    val iconSizeError = remember(iconSizeStr) {
        iconSizeStr.toFloatOrNull()?.takeIf { it in 0.1f..5f } == null && iconSizeStr.isNotBlank()
    }
    val opacityError = remember(iconOpacityStr) {
        iconOpacityStr.toFloatOrNull()?.takeIf { it in 0f..1f } == null && iconOpacityStr.isNotBlank()
    }
    val selectedAnchorLabel = ICON_ANCHOR_OPTIONS
        .firstOrNull { it.first == iconAnchor }
        ?.second
        ?.let { stringResource(it) }
        ?: iconAnchor

    MarkerSectionTitle(stringResource(R.string.marker_common_params))
    LatLngOutlinedTextField(
        value = defaultLatLng,
        onValueChange = { viewModel.setDefaultLatLng(it) },
        label = stringResource(R.string.marker_position),
        hint = "1.360879,103.732578",
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        testTag = "symbol_input_default_latlng",
    )
    MarkerSliderField(
        label = stringResource(R.string.marker_icon_opacity),
        value = localOpacity,
        valueText = iconOpacityStr,
        onValueChange = { viewModel.previewIconOpacity(it); localOpacity = it },
        onValueChangeFinished = {
            viewModel.setIconOpacity(localOpacity)
            iconOpacityStr = formatMarkerValue(localOpacity)
        },
        onValueTextChange = {
            iconOpacityStr = it
            it.toFloatOrNull()?.let { value ->
                localOpacity = value
                viewModel.setIconOpacity(value)
            }
        },
        valueRange = 0f..1f,
        rangeStartLabel = "0",
        rangeEndLabel = "1",
        isError = opacityError,
        testTag = "symbol_input_icon_opacity",
    )
    ExposedDropdownMenuBox(
        expanded = anchorExpanded,
        onExpandedChange = { anchorExpanded = it },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedAnchorLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.marker_icon_anchor)) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = anchorExpanded) },
        )
        ExposedDropdownMenu(
            expanded = anchorExpanded,
            onDismissRequest = { anchorExpanded = false },
        ) {
            ICON_ANCHOR_OPTIONS.forEach { (value, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        viewModel.setIconAnchor(value)
                        anchorExpanded = false
                    },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING),
        ) {
            Text(stringResource(R.string.marker_scale_with_zoom))
            DemoPanelSwitch(
                checked = iconScaleWithZoom,
                onCheckedChange = { viewModel.setIconScaleWithZoom(it) },
                modifier = Modifier.testTag("symbol_switch_icon_scale_with_zoom"),
            )
        }
    }
    if (!iconScaleWithZoom) {
        MarkerSliderField(
            label = stringResource(R.string.marker_icon_size_max_scale),
            value = localIconSize,
            valueText = iconSizeStr,
            onValueChange = { viewModel.previewIconSize(it); localIconSize = it },
            onValueChangeFinished = {
                viewModel.setIconSize(localIconSize)
                iconSizeStr = formatMarkerValue(localIconSize)
            },
            onValueTextChange = {
                iconSizeStr = it
                it.toFloatOrNull()?.let { value ->
                    localIconSize = value
                    viewModel.setIconSize(value)
                }
            },
            valueRange = 0.1f..5f,
            rangeStartLabel = "0.1",
            rangeEndLabel = "5",
            isError = iconSizeError,
            testTag = "symbol_input_icon_size",
        )
    }
    if (iconScaleWithZoom) {
        MarkerSliderField(
            label = stringResource(R.string.marker_min_scale),
            value = localMinScale,
            valueText = minScaleStr,
            onValueChange = { viewModel.previewMinScale(it); localMinScale = it },
            onValueChangeFinished = {
                viewModel.setMinScale(localMinScale)
                minScaleStr = formatMarkerValue(localMinScale)
            },
            onValueTextChange = {
                minScaleStr = it
                it.toFloatOrNull()?.let { value ->
                    localMinScale = value
                    viewModel.setMinScale(value)
                }
            },
            valueRange = 0.1f..5f,
            rangeStartLabel = "0.1",
            rangeEndLabel = "5",
        )
        DemoNumericSliderField(
            label = stringResource(R.string.marker_min_scale_zoom),
            value = localMinZoom.toFloat(),
            valueText = minZoomStr,
            onValueChange = {
                val zoom = it.toInt()
                localMinZoom = zoom
                viewModel.previewMinZoom(zoom)
            },
            onValueChangeFinished = {
                viewModel.setMinZoom(localMinZoom)
                minZoomStr = localMinZoom.toString()
            },
            onValueTextChange = {
                minZoomStr = it
                it.toIntOrNull()?.let { value ->
                    localMinZoom = value
                    viewModel.setMinZoom(value)
                }
            },
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            testTag = "symbol_input_min_zoom",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DemoNumericSliderField(
            label = stringResource(R.string.marker_max_scale_zoom),
            value = localMaxZoom.toFloat(),
            valueText = maxZoomStr,
            onValueChange = {
                val zoom = it.toInt()
                localMaxZoom = zoom
                viewModel.previewMaxZoom(zoom)
            },
            onValueChangeFinished = {
                viewModel.setMaxZoom(localMaxZoom)
                maxZoomStr = localMaxZoom.toString()
            },
            onValueTextChange = {
                maxZoomStr = it
                it.toIntOrNull()?.let { value ->
                    localMaxZoom = value
                    viewModel.setMaxZoom(value)
                }
            },
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            testTag = "symbol_input_max_zoom",
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
internal fun MarkerUrlAddSection(viewModel: MarkerLayerViewModel) {
    val defaultLatLng by viewModel.defaultLatLng.collectAsState()
    val iconUrl by viewModel.iconUrl.collectAsState()
    val iconUrlId by viewModel.iconUrlId.collectAsState()

    MarkerSectionTitle(stringResource(R.string.marker_add_by_icon_url_title))
    OutlinedTextField(
        value = iconUrl,
        onValueChange = { viewModel.setIconUrl(it) },
        label = { Text(stringResource(R.string.marker_icon_url)) },
        supportingText = { Text(stringResource(R.string.marker_image_url_hint), color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("symbol_input_icon_url"),
    )
    OutlinedTextField(
        value = iconUrlId,
        onValueChange = { viewModel.setIconUrlId(it) },
        label = { Text(stringResource(R.string.marker_icon_url_id)) },
        supportingText = { Text(stringResource(R.string.marker_icon_style_name_hint), color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("symbol_input_icon_url_id"),
    )
    DemoPanelButton(
        onClick = { viewModel.addMarkerByUrl() },
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("symbol_btn_add_by_url"),
        enabled = validateLatLng(defaultLatLng) == null,
    ) {
        Text(stringResource(R.string.marker_add_by_url), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun MarkerTypeAddSection(viewModel: MarkerLayerViewModel) {
    val defaultLatLng by viewModel.defaultLatLng.collectAsState()
    val iconType by viewModel.iconType.collectAsState()
    var typeExpanded by remember { mutableStateOf(false) }

    MarkerSectionTitle(stringResource(R.string.marker_add_by_icon_type_title))
    ExposedDropdownMenuBox(
        expanded = typeExpanded,
        onExpandedChange = { typeExpanded = it },
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        OutlinedTextField(
            value = MarkerLayerViewModel.ICON_TYPE_OPTIONS.find { it.first == iconType }?.second ?: iconType,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.marker_icon_type)) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
        )
        ExposedDropdownMenu(
            expanded = typeExpanded,
            onDismissRequest = { typeExpanded = false },
        ) {
            MarkerLayerViewModel.ICON_TYPE_OPTIONS.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        viewModel.setIconType(value)
                        typeExpanded = false
                    },
                )
            }
        }
    }
    DemoPanelButton(
        onClick = { viewModel.addMarkerByType() },
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("symbol_btn_add_by_type"),
        enabled = validateLatLng(defaultLatLng) == null,
    ) {
        Text(stringResource(R.string.marker_add_by_type), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun MarkerSdfAddSection(viewModel: MarkerLayerViewModel) {
    val defaultLatLng by viewModel.defaultLatLng.collectAsState()
    val sdfIconColor by viewModel.sdfIconColor.collectAsState()
    val colorError = remember(sdfIconColor) { validateMarkerColor(sdfIconColor) }
    val colorErrorText = colorError?.let { stringResource(it) }

    MarkerSectionTitle(stringResource(R.string.marker_add_sdf_title))
    ColorPickerField(
        value = sdfIconColor,
        onValueChange = { viewModel.setSdfIconColor(it) },
        label = stringResource(R.string.marker_icon_color),
        supportingText = colorErrorText?.let { { Text(it, color = Color.Red) } }
            ?: { Text(stringResource(R.string.marker_icon_id_hint), color = Color.Gray) },
        isError = colorError != null,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        testTag = "symbol_input_sdf_icon_color",
    )
    DemoPanelButton(
        onClick = { viewModel.addMarkerBySdf() },
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("symbol_btn_add_sdf"),
        enabled = validateLatLng(defaultLatLng) == null,
    ) {
        Text(stringResource(R.string.marker_add_sdf), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun MarkerClearAllSection(viewModel: MarkerLayerViewModel) {
    MarkerSectionDivider()
    DemoPanelButton(
        onClick = { viewModel.clearMarkers() },
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("symbol_btn_clear_all"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
        Text(stringResource(R.string.marker_clear_all), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MarkerSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun MarkerSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = Color.LightGray,
    )
}

@Composable
private fun MarkerSliderField(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueTextChange: (String) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    rangeStartLabel: String,
    rangeEndLabel: String,
    isError: Boolean = false,
    testTag: String? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) = DemoNumericSliderField(
    label = label,
    value = value,
    valueText = valueText,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    onValueTextChange = onValueTextChange,
    valueRange = valueRange,
    rangeStartLabel = rangeStartLabel,
    rangeEndLabel = rangeEndLabel,
    isError = isError,
    testTag = testTag,
    modifier = Modifier.padding(bottom = 10.dp),
)

private fun formatMarkerValue(value: Float): String =
    String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

private fun validateMarkerColor(input: String): Int? {
    val t = input.trim()
    if (t.isBlank()) return null
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(t)) null else R.string.marker_color_format_error
}
