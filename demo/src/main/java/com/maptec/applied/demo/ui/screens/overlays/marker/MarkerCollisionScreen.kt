@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.marker

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.viewmodel.MarkerCollisionViewModel
import com.maptec.applied.demo.viewmodel.MarkerCollisionViewModelFactory
import com.maptec.applied.maps.overlay.marker.MarkerCollisionBehavior

private val COLLISION_STRATEGY_OPTIONS = listOf(
    MarkerCollisionBehavior.REQUIRED to R.string.marker_collision_strategy_required,
    MarkerCollisionBehavior.OPTIONAL_AND_HIDES_LOWER_PRIORITY to
        R.string.marker_collision_strategy_optional,
    MarkerCollisionBehavior.REQUIRED_AND_HIDES_OPTIONAL to
        R.string.marker_collision_strategy_required_hides_optional,
)

@Composable
fun MarkerCollisionScreen(
    viewModel: MarkerCollisionViewModel = viewModel(factory = MarkerCollisionViewModelFactory()),
) {
    val context = LocalContext.current
    val toastMessage by viewModel.toastMessage.collectAsState()
    val collisionBehavior by viewModel.collisionBehavior.collectAsState()
    val zOrder by viewModel.zOrder.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                MarkerCollisionConfigPanel(
                    collisionBehavior = collisionBehavior,
                    zOrder = zOrder,
                    onBehaviorSelected = viewModel::setCollisionBehavior,
                    onZOrderChange = viewModel::setZOrder,
                    onAdd = viewModel::addMarker,
                    onClear = viewModel::clearAllMarkers,
                )
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { _, map ->
                    viewModel.initMap(context, map)
                },
            )
        },
    )
}

@Composable
private fun MarkerCollisionConfigPanel(
    collisionBehavior: MarkerCollisionBehavior,
    zOrder: Int,
    onBehaviorSelected: (MarkerCollisionBehavior) -> Unit,
    onZOrderChange: (Int) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
) {
    var strategyExpanded by remember { mutableStateOf(false) }
    var zOrderText by remember(zOrder) { mutableStateOf(zOrder.toString()) }

    val selectedLabel = COLLISION_STRATEGY_OPTIONS
        .first { it.first == collisionBehavior }
        .second
        .let { stringResource(it) }

    Text(
        text = stringResource(R.string.marker_collision_section_title),
        modifier = Modifier.padding(bottom = 8.dp),
    )

    ExposedDropdownMenuBox(
        expanded = strategyExpanded,
        onExpandedChange = { strategyExpanded = it },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.marker_collision_strategy_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("marker_collision_strategy_dropdown"),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
        )
        ExposedDropdownMenu(
            expanded = strategyExpanded,
            onDismissRequest = { strategyExpanded = false },
        ) {
            COLLISION_STRATEGY_OPTIONS.forEach { (behavior, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onBehaviorSelected(behavior)
                        strategyExpanded = false
                    },
                    modifier = Modifier.testTag("marker_collision_strategy_${behavior.name}"),
                )
            }
        }
    }

    OutlinedTextField(
        value = zOrderText,
        onValueChange = { raw ->
            zOrderText = raw.filter { it.isDigit() }.take(3)
            zOrderText.toIntOrNull()?.let(onZOrderChange)
        },
        label = { Text(stringResource(R.string.marker_collision_z_order_label)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("marker_collision_z_order_input"),
    )

    DemoPanelButton(
        onClick = onAdd,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("marker_collision_add_button"),
    ) {
        Text(stringResource(R.string.marker_collision_add))
    }

    Spacer(modifier = Modifier.height(8.dp))

    DemoPanelButton(
        onClick = onClear,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("marker_collision_clear_button"),
    ) {
        Text(stringResource(R.string.marker_collision_clear))
    }
}
