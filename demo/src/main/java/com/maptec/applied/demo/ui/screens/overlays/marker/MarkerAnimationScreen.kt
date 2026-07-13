@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.marker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.viewmodel.MarkerAnimationViewModel
import com.maptec.applied.demo.viewmodel.MarkerAnimationViewModelFactory

private val FIELD_PADDING = 8.dp

@Composable
fun MarkerAnimationScreen(
    viewModel: MarkerAnimationViewModel = viewModel(factory = MarkerAnimationViewModelFactory()),
) {
    val context = LocalContext.current
    val markers by viewModel.markers.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
    var defaultAnimationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                MarkerAnimationDetailPanel(viewModel = viewModel)
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                Mapview(
                    modifier = Modifier.fillMaxSize(),
                    onStyleRendered = { mapView, mapLibreMap, style ->
                        viewModel.initSymbolManager(context, mapView, mapLibreMap, style)
                        if (!defaultAnimationStarted) {
                            defaultAnimationStarted = true
                            viewModel.setEnterType("bounce")
                            viewModel.addMarker()
                            viewModel.startEnterAnimation()
                        }
                    },
                )
                MainAnimationTopBar(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            if (markers.isNotEmpty()) {
                Box(Modifier.size(1.dp).testTag("symbol_layer_has_markers"))
            }
            if (selectedMarkerId != null) {
                Box(Modifier.size(1.dp).testTag("symbol_layer_marker_selected"))
            }
            if (isDragging) {
                Box(Modifier.size(1.dp).testTag("symbol_layer_marker_dragging"))
            }
        },
    )
}

@Composable
private fun MainAnimationTopBar(
    viewModel: MarkerAnimationViewModel,
    modifier: Modifier = Modifier,
) {
    val markers by viewModel.markers.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("anim_top_bar"),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stringResource(R.string.marker_anim_marker_count)}: ${markers.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = selectedMarkerId?.let {
                    "${stringResource(R.string.marker_anim_selected)}: ${it.take(8)}"
                } ?: stringResource(R.string.marker_anim_tap_to_add),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DemoPanelButton(
            onClick = { viewModel.addMarker() },
            modifier = Modifier
                .weight(0.9f)
                .testTag("symbol_btn_add_marker_top"),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(stringResource(R.string.marker_anim_add))
        }
        OutlinedButton(
            onClick = { viewModel.clearMarkers() },
            modifier = Modifier
                .weight(0.9f)
                .testTag("symbol_btn_clear_all_top"),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(stringResource(R.string.marker_anim_clear))
        }
    }
}

@Composable
private fun MarkerAnimationDetailPanel(viewModel: MarkerAnimationViewModel) {
    val markers by viewModel.markers.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarkerAnimationStatusCard(
            markerCount = markers.size,
            selectedMarkerId = selectedMarkerId,
            isDragging = isDragging,
            onAdd = viewModel::addMarker,
            onClear = viewModel::clearMarkers,
            onStopAllAnimations = viewModel::stopAllAnimations,
        )
        SelectAnimationSection(viewModel)
        EnterAnimationSection(viewModel)
        DisappearAnimationSection(viewModel)
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun MarkerAnimationStatusCard(
    markerCount: Int,
    selectedMarkerId: String?,
    isDragging: Boolean,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onStopAllAnimations: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFD),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.marker_anim_marker_count)}: $markerCount",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = when {
                            isDragging -> stringResource(R.string.marker_anim_dragging)
                            selectedMarkerId != null -> "${stringResource(R.string.marker_anim_selected)}: ${selectedMarkerId.take(8)}"
                            else -> stringResource(R.string.marker_anim_tap_empty_to_add)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DemoPanelButton(
                    onClick = onAdd,
                    modifier = Modifier.testTag("symbol_btn_add_marker"),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(stringResource(R.string.marker_anim_add))
                }
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth().testTag("symbol_btn_clear_all"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.marker_anim_clear_all))
            }
            OutlinedButton(
                onClick = onStopAllAnimations,
                modifier = Modifier.fillMaxWidth().testTag("symbol_btn_stop_all_animations"),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.marker_anim_stop_all))
            }
        }
    }
}

@Composable
private fun AnimationActionCard(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun DurationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String,
) {
    DemoNumericSliderField(
        label = stringResource(R.string.marker_anim_duration_label),
        value = value.toFloatOrNull() ?: 1000f,
        valueText = value,
        onValueChange = { onValueChange(it.toLong().toString()) },
        onValueTextChange = onValueChange,
        valueRange = 100f..5000f,
        rangeStartLabel = "100",
        rangeEndLabel = "5000",
        enabled = enabled,
        modifier = modifier,
        testTag = testTag,
    )
}

@Composable
private fun EnterAnimationSection(viewModel: MarkerAnimationViewModel) {
    val type by viewModel.enterType.collectAsState()
    val duration by viewModel.enterDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    AnimationActionCard(
        title = stringResource(R.string.marker_anim_enter_title),
        description = stringResource(R.string.marker_anim_enter_desc),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        ) {
            AnimationTypeDropdown(
                label = stringResource(R.string.marker_anim_type_label),
                options = MarkerAnimationViewModel.ENTER_OPTIONS,
                selected = type,
                onSelected = { viewModel.setEnterType(it) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_dropdown_enter",
            )
            DurationField(
                value = durationStr,
                onValueChange = {
                    durationStr = it
                    it.toLongOrNull()?.let { v -> viewModel.setEnterDurationMs(v) }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_input_enter_duration",
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        ) {
            DemoPanelButton(
                onClick = viewModel::startEnterAnimation,
                modifier = Modifier.fillMaxWidth().testTag("anim_btn_enter_start"),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.marker_anim_play_enter))
            }
            OutlinedButton(
                onClick = { viewModel.endEnterAnimation() },
                modifier = Modifier.fillMaxWidth().testTag("anim_btn_enter_end"),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.marker_anim_stop))
            }
        }
    }
}

@Composable
private fun SelectAnimationSection(viewModel: MarkerAnimationViewModel) {
    val type by viewModel.selectType.collectAsState()
    val duration by viewModel.selectDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    AnimationActionCard(
        title = stringResource(R.string.marker_anim_select_title),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        ) {
            AnimationTypeDropdown(
                label = stringResource(R.string.marker_anim_type_label),
                options = MarkerAnimationViewModel.SELECT_OPTIONS,
                selected = type,
                onSelected = { viewModel.setSelectType(it) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_dropdown_select",
            )
            DurationField(
                value = durationStr,
                onValueChange = {
                    durationStr = it
                    it.toLongOrNull()?.let { v -> viewModel.setSelectDurationMs(v) }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_input_select_duration",
            )
        }
    }
}

@Composable
private fun DisappearAnimationSection(viewModel: MarkerAnimationViewModel) {
    val type by viewModel.disappearType.collectAsState()
    val duration by viewModel.disappearDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    AnimationActionCard(
        title = stringResource(R.string.marker_anim_disappear_title),
        description = stringResource(R.string.marker_anim_disappear_desc),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        ) {
            AnimationTypeDropdown(
                label = stringResource(R.string.marker_anim_type_label),
                options = MarkerAnimationViewModel.DISAPPEAR_OPTIONS,
                selected = type,
                onSelected = { viewModel.setDisappearType(it) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_dropdown_disappear",
            )
            DurationField(
                value = durationStr,
                onValueChange = {
                    durationStr = it
                    it.toLongOrNull()?.let { v -> viewModel.setDisappearDurationMs(v) }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "anim_input_disappear_duration",
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        ) {
            DemoPanelButton(
                onClick = viewModel::startDisappearAnimation,
                modifier = Modifier.fillMaxWidth().testTag("anim_btn_disappear_start"),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.marker_anim_play_disappear))
            }
            OutlinedButton(
                onClick = { viewModel.endDisappearAnimation() },
                modifier = Modifier.fillMaxWidth().testTag("anim_btn_disappear_end"),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.marker_anim_cancel))
            }
        }
    }
}

@Composable
private fun AnimationTypeDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = options.find { it.first == selected }?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(testTag),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
