@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.viewmodel.OverlayPickViewModel
import com.maptec.applied.demo.viewmodel.OverlayPickViewModelFactory
import com.maptec.applied.maps.MaptecMap

@Composable
fun OverlayPickScreen(
    viewModel: OverlayPickViewModel = viewModel(factory = OverlayPickViewModelFactory())
) {
    val context = LocalContext.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    DisposableEffect(mapRef) {
        val map = mapRef
        if (map != null) {
            viewModel.attachMap(map, context)
        }
        onDispose {
            viewModel.detachMap()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            OverlayPickDetailPanel(viewModel = viewModel)
        },
        content = { padding ->
            Mapview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("overlay_pick_map"),
                onMapReady = { _, map ->
                    map.getStyle {
                        mapRef = map
                    }
                }
            )
        }
    )
}

@Composable
private fun OverlayPickDetailPanel(viewModel: OverlayPickViewModel) {
    val pickSnapshot by viewModel.pickSnapshot.collectAsState()
    val eventLog by viewModel.eventLog.collectAsState()
    val lineClickable by viewModel.lineClickable.collectAsState()
    val markerClickable by viewModel.markerClickable.collectAsState()
    val lineConsume by viewModel.lineConsume.collectAsState()
    val markerConsume by viewModel.markerConsume.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("overlay_pick_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.overlay_pick_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.pickAtMarker() },
                modifier = Modifier.weight(1f).testTag("overlay_pick_center"),
            ) {
                Text(stringResource(R.string.overlay_pick_at_marker))
            }
            OutlinedButton(
                onClick = { viewModel.resetScene(context) },
                modifier = Modifier.weight(1f).testTag("overlay_pick_reset"),
            ) {
                Text(stringResource(R.string.overlay_pick_reset))
            }
        }

        ToggleRow(
            label = stringResource(R.string.overlay_pick_line_clickable),
            checked = lineClickable,
            onCheckedChange = viewModel::setLineClickable,
            testTag = "overlay_pick_line_clickable",
        )
        ToggleRow(
            label = stringResource(R.string.overlay_pick_marker_clickable),
            checked = markerClickable,
            onCheckedChange = viewModel::setMarkerClickable,
            testTag = "overlay_pick_marker_clickable",
        )
        ToggleRow(
            label = stringResource(R.string.overlay_pick_line_consume),
            checked = lineConsume,
            onCheckedChange = viewModel::setLineConsume,
            testTag = "overlay_pick_line_consume",
        )
        ToggleRow(
            label = stringResource(R.string.overlay_pick_marker_consume),
            checked = markerConsume,
            onCheckedChange = viewModel::setMarkerConsume,
            testTag = "overlay_pick_marker_consume",
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.overlay_pick_ex_result_title),
            style = MaterialTheme.typography.titleSmall,
        )
        if (pickSnapshot == null) {
            Text(
                text = stringResource(R.string.overlay_pick_no_result),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val snapshot = pickSnapshot!!
            Text(
                text = stringResource(
                    R.string.overlay_pick_tap_format,
                    snapshot.gesture,
                    snapshot.tap.latitude,
                    snapshot.tap.longitude,
                    snapshot.screen.x,
                    snapshot.screen.y,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (snapshot.exHits.isEmpty()) {
                Text(text = stringResource(R.string.overlay_pick_ex_empty))
            } else {
                snapshot.exHits.forEach { hit ->
                    Text(text = hit, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = stringResource(
                    R.string.overlay_pick_marker_wrapper,
                    snapshot.resolvedMarker ?: stringResource(R.string.overlay_pick_none),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.overlay_pick_listener_log),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(onClick = viewModel::clearLog) {
                Text(stringResource(R.string.overlay_pick_clear_log))
            }
        }
        if (eventLog.isEmpty()) {
            Text(
                text = stringResource(R.string.overlay_pick_log_empty),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            eventLog.asReversed().forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
