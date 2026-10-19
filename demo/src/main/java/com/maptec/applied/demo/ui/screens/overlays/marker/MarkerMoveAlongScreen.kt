@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.marker

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.viewmodel.MarkerMoveAlongUi
import com.maptec.applied.demo.viewmodel.MarkerMoveAlongViewModel
import com.maptec.applied.demo.viewmodel.MarkerMoveAlongViewModelFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.overlay.marker.along.BackgroundBehavior
import com.maptec.applied.maps.overlay.marker.along.CancelBehavior
import com.maptec.applied.maps.overlay.marker.along.MoveAlongMode

@Composable
fun MarkerMoveAlongScreen(
    viewModel: MarkerMoveAlongViewModel = viewModel(
        factory = MarkerMoveAlongViewModelFactory(LocalContext.current.applicationContext),
    ),
) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.toast) {
        ui.toast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                MarkerMoveAlongPanel(ui = ui, viewModel = viewModel)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = CameraPosition.Builder()
                    .target(LatLng(1.35, 103.83))
                    .zoom(12.0)
                    .build(),
                onStyleRendered = { _, map, _ ->
                    viewModel.bind(map)
                },
                onBeforeDestroy = { viewModel.release() },
            )
            if (ui.bound && ui.pointCount >= 2) {
                Box(Modifier.size(1.dp).testTag("move_along_has_marker"))
            }
        },
    )
}

@Composable
private fun MarkerMoveAlongPanel(
    ui: MarkerMoveAlongUi,
    viewModel: MarkerMoveAlongViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MoveAlongStatusCard(ui = ui)

        Text(stringResource(R.string.move_along_mode), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ui.mode == MoveAlongMode.DURATION,
                onClick = { viewModel.setMode(MoveAlongMode.DURATION) },
                label = { Text(stringResource(R.string.move_along_mode_duration)) },
                modifier = Modifier.testTag("move_along_mode_duration"),
            )
            FilterChip(
                selected = ui.mode == MoveAlongMode.SPEED,
                onClick = { viewModel.setMode(MoveAlongMode.SPEED) },
                label = { Text(stringResource(R.string.move_along_mode_speed)) },
                modifier = Modifier.testTag("move_along_mode_speed"),
            )
        }

        if (ui.mode == MoveAlongMode.DURATION) {
            DemoNumericSliderField(
                label = stringResource(R.string.move_along_duration),
                value = ui.durationMs.toFloat(),
                valueText = ui.durationMs.toString(),
                onValueChange = { viewModel.setDurationMs(it.toLong()) },
                onValueTextChange = { it.toLongOrNull()?.let(viewModel::setDurationMs) },
                valueRange = 1_000f..30_000f,
                rangeStartLabel = "1s",
                rangeEndLabel = "30s",
                steps = 29,
                testTag = "move_along_input_duration",
            )
        } else {
            DemoNumericSliderField(
                label = stringResource(R.string.move_along_speed),
                value = ui.speedMps.toFloat(),
                valueText = ui.speedMps.toString(),
                onValueChange = { viewModel.setSpeedMps(it.toDouble()) },
                onValueTextChange = { it.toDoubleOrNull()?.let(viewModel::setSpeedMps) },
                valueRange = 10f..200f,
                rangeStartLabel = "10",
                rangeEndLabel = "200",
                testTag = "move_along_input_speed",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.move_along_auto_rotate))
            DemoPanelSwitch(
                checked = ui.autoRotate,
                onCheckedChange = { viewModel.setAutoRotate(it) },
                modifier = Modifier.testTag("move_along_switch_auto_rotate"),
            )
        }

        DemoNumericSliderField(
            label = stringResource(R.string.move_along_heading_offset),
            value = ui.headingOffset.toFloat(),
            valueText = ui.headingOffset.toString(),
            onValueChange = { viewModel.setHeadingOffset(it.toDouble()) },
            onValueTextChange = { it.toDoubleOrNull()?.let(viewModel::setHeadingOffset) },
            valueRange = -180f..180f,
            rangeStartLabel = "-180",
            rangeEndLabel = "180",
            testTag = "move_along_input_heading",
        )
        DemoNumericSliderField(
            label = stringResource(R.string.move_along_smoothing),
            value = ui.orientationSmoothingMs.toFloat(),
            valueText = ui.orientationSmoothingMs.toString(),
            onValueChange = { viewModel.setOrientationSmoothingMs(it.toLong()) },
            onValueTextChange = { it.toLongOrNull()?.let(viewModel::setOrientationSmoothingMs) },
            valueRange = 0f..800f,
            rangeStartLabel = "0",
            rangeEndLabel = "800",
            steps = 16,
            testTag = "move_along_input_smoothing",
        )
        DemoNumericSliderField(
            label = stringResource(R.string.move_along_progress_interval),
            value = ui.progressCallbackIntervalMs.toFloat(),
            valueText = ui.progressCallbackIntervalMs.toString(),
            onValueChange = { viewModel.setProgressCallbackIntervalMs(it.toLong()) },
            onValueTextChange = { it.toLongOrNull()?.let(viewModel::setProgressCallbackIntervalMs) },
            valueRange = 16f..500f,
            rangeStartLabel = "16",
            rangeEndLabel = "500",
            testTag = "move_along_input_interval",
        )

        Text(stringResource(R.string.move_along_background), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ui.backgroundBehavior == BackgroundBehavior.PAUSE,
                onClick = { viewModel.setBackgroundBehavior(BackgroundBehavior.PAUSE) },
                label = { Text(stringResource(R.string.move_along_background_pause)) },
            )
            FilterChip(
                selected = ui.backgroundBehavior == BackgroundBehavior.CONTINUE,
                onClick = { viewModel.setBackgroundBehavior(BackgroundBehavior.CONTINUE) },
                label = { Text(stringResource(R.string.move_along_background_continue)) },
            )
        }

        var seekDragging by remember { mutableStateOf(false) }
        var seekDragValue by remember { mutableStateOf(0f) }
        val seekShown = if (seekDragging) seekDragValue else ui.seekProgress
        DemoNumericSliderField(
            label = stringResource(R.string.move_along_seek),
            value = seekShown,
            valueText = "%.2f".format(seekShown),
            onValueChange = { value ->
                seekDragging = true
                seekDragValue = value
            },
            onValueTextChange = { text ->
                text.toFloatOrNull()?.let { value ->
                    seekDragging = false
                    viewModel.seekTo(value)
                }
            },
            onValueChangeFinished = {
                viewModel.seekTo(seekDragValue)
                seekDragging = false
            },
            valueRange = 0f..1f,
            rangeStartLabel = "0",
            rangeEndLabel = "1",
            testTag = "move_along_input_seek",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoPanelButton(
                onClick = { viewModel.start() },
                modifier = Modifier.testTag("move_along_btn_start"),
            ) { Text(stringResource(R.string.move_along_start)) }
            DemoPanelButton(
                onClick = { viewModel.pause() },
                modifier = Modifier.testTag("move_along_btn_pause"),
            ) { Text(stringResource(R.string.move_along_pause)) }
            DemoPanelButton(
                onClick = { viewModel.resume() },
                modifier = Modifier.testTag("move_along_btn_resume"),
            ) { Text(stringResource(R.string.move_along_resume)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoPanelButton(
                onClick = { viewModel.finish() },
                modifier = Modifier.testTag("move_along_btn_finish"),
            ) { Text(stringResource(R.string.move_along_finish)) }
            DemoPanelButton(
                onClick = { viewModel.cancel(CancelBehavior.KEEP_CURRENT) },
                modifier = Modifier.testTag("move_along_btn_cancel_keep"),
            ) { Text(stringResource(R.string.move_along_cancel_keep)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoPanelButton(
                onClick = { viewModel.cancel(CancelBehavior.RESET_TO_START) },
                modifier = Modifier.testTag("move_along_btn_cancel_reset"),
            ) { Text(stringResource(R.string.move_along_cancel_reset)) }
            DemoPanelButton(
                onClick = { viewModel.cancel(CancelBehavior.REMOVE_MARKER) },
                modifier = Modifier.testTag("move_along_btn_cancel_remove"),
            ) { Text(stringResource(R.string.move_along_cancel_remove)) }
        }
    }
}

@Composable
private fun MoveAlongStatusCard(ui: MarkerMoveAlongUi) {
    val empty = stringResource(R.string.move_along_value_empty)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val pathSource = stringResource(
                if (ui.usingFallback) {
                    R.string.move_along_path_fallback
                } else {
                    R.string.move_along_path_route
                },
            )
            val pointCount = stringResource(R.string.move_along_value_points, ui.pointCount)
            val error = ui.errorCode.ifEmpty { stringResource(R.string.move_along_value_no_error) }
            val latlng = if (ui.latitude != null && ui.longitude != null) {
                "%.6f, %.6f".format(ui.latitude, ui.longitude)
            } else {
                empty
            }
            val heading = ui.headingDegrees?.let { "%.1f°".format(it) } ?: empty
            val segment = ui.segmentIndex?.toString() ?: empty
            val traveled = ui.traveledMeters?.let { "%.1f".format(it) } ?: empty
            val remaining = ui.remainingMeters?.let { "%.1f".format(it) } ?: empty
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_path_source),
                value = "$pathSource  ·  $pointCount",
            )
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_state),
                value = "${ui.state}  ·  $error",
                modifier = Modifier.testTag("move_along_state"),
            )
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_latlng),
                value = latlng,
            )
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_heading),
                value = "$heading  ·  $segment",
            )
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_progress),
                value = "%.1f%%  ·  %s / %s m".format(ui.seekProgress * 100f, traveled, remaining),
                modifier = Modifier.testTag("move_along_progress"),
            )
            MoveAlongStatusRow(
                label = stringResource(R.string.move_along_label_event),
                value = ui.lastEvent.ifEmpty { empty },
            )
        }
    }
}

@Composable
private fun MoveAlongStatusRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
