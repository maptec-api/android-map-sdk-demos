package com.maptec.applied.demo.ui.screens.tools

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoCapability
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoTab
import com.maptec.applied.demo.viewmodel.GeometryDemoViewModel
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap

private val GEOMETRY_DEMO_CAMERA = CameraPosition.Builder()
    .target(LatLng(1.3521, 103.8198))
    .zoom(13.0)
    .build()

@Composable
fun GeometryDemoScreen(viewModel: GeometryDemoViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val localizedContext = LocalContext.current
    LaunchedEffect(localizedContext) {
        viewModel.setDisplayContext(localizedContext)
        viewModel.redraw()
    }
    val density = LocalDensity.current
    val bottomChromeDp = 180.dp
    val bottomPaddingPx = with(density) { bottomChromeDp.roundToPx() }

    DemoPanelScaffold(
        sheetWidthFraction = 0.52f,
        sheetPanelMaxWidth = 320.dp,
        sheetContent = {
            GeometryDemoConfigPanel(viewModel = viewModel, uiState = uiState)
        },
        content = {
            Box(Modifier.fillMaxSize()) {
                Mapview(
                    modifier = Modifier.fillMaxSize(),
                    cameraPosition = GEOMETRY_DEMO_CAMERA,
                    onMapReady = { view, map ->
                        viewModel.attachMap(map, view.context, bottomPaddingPx)
                        map.addOnMapClickListener(object : MaptecMap.OnMapClickListener {
                            override fun onMapClick(point: LatLng): Boolean {
                                viewModel.onMapClick(point)
                                return true
                            }

                            override fun onMapClick(screen: PointF, point: LatLng): Boolean {
                                viewModel.onMapClick(point)
                                return true
                            }
                        })
                    },
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xF2FFFFFF),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.statusLine.ifBlank {
                                stringResource(uiState.capability.chipLabelRes)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.White),
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                    ) {
                        Text(
                            text = stringResource(uiState.capability.hintRes),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GeometryDemoCapability.forTab(uiState.tab).forEach { cap ->
                            val selected = cap == uiState.capability
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectCapability(cap) },
                                label = { Text(stringResource(cap.chipLabelRes)) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("geometry_chip_${cap.name}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7CF6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFE8EAED),
                                    labelColor = Color(0xFF5F6368),
                                ),
                            )
                        }
                    }

                    GeometryDemoTabRow(
                        selected = uiState.tab,
                        onSelected = viewModel::selectTab,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeometryDemoTabRow(
    selected: GeometryDemoTab,
    onSelected: (GeometryDemoTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White),
    ) {
        GeometryDemoTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                onClick = { onSelected(tab) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .testTag("geometry_tab_${tab.name}"),
                color = if (isSelected) Color(0xFFEAF2FE) else Color.White,
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(tab.titleRes),
                        color = if (isSelected) Color(0xFF2E7CF6) else Color(0xFF9AA0A6),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeometryDemoConfigPanel(
    viewModel: GeometryDemoViewModel,
    uiState: com.maptec.applied.demo.viewmodel.GeometryDemoUiState,
) {
    val cap = uiState.capability
    DemoPanelColumn {
        Text(stringResource(cap.chipLabelRes), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("API：${cap.apiLabel}", style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.geometry_demo_exclusive_note), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.geometry_demo_panel_results), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        if (uiState.panelDetail.isNotBlank()) {
            Text(uiState.panelDetail, modifier = Modifier.testTag("geometry_result_text"))
        }

        when (cap) {
            GeometryDemoCapability.Offset -> {
                Text(stringResource(R.string.geometry_offset_distance, uiState.offsetDistanceM.toInt()))
                DemoPanelSlider(
                    value = uiState.offsetDistanceM,
                    onValueChange = viewModel::updateOffsetDistance,
                    valueRange = 50f..2000f,
                )
                Text(stringResource(R.string.geometry_offset_heading, uiState.offsetHeadingDeg.toInt()))
                DemoPanelSlider(
                    value = uiState.offsetHeadingDeg,
                    onValueChange = viewModel::updateOffsetHeading,
                    valueRange = 0f..360f,
                )
            }
            GeometryDemoCapability.OffsetOrigin -> {
                Text(stringResource(R.string.geometry_offset_distance, uiState.originDistanceM.toInt()))
                DemoPanelSlider(
                    value = uiState.originDistanceM,
                    onValueChange = viewModel::updateOriginDistance,
                    valueRange = 100f..20_000f,
                )
                Text(stringResource(R.string.geometry_offset_heading, uiState.originHeadingDeg.toInt()))
                DemoPanelSlider(
                    value = uiState.originHeadingDeg,
                    onValueChange = viewModel::updateOriginHeading,
                    valueRange = 0f..360f,
                )
                DemoPanelButton(onClick = viewModel::triggerPoleNoSolution, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.geometry_demo_pole_no_solution))
                }
            }
            GeometryDemoCapability.Interpolate -> {
                Text(stringResource(R.string.geometry_offset_fraction, uiState.interpolateFraction))
                DemoPanelSlider(
                    value = uiState.interpolateFraction,
                    onValueChange = viewModel::updateInterpolateFraction,
                    valueRange = 0f..1f,
                )
            }
            GeometryDemoCapability.Path -> {
                Text(stringResource(R.string.geometry_path_tolerance, uiState.simplifyToleranceM.toInt()))
                DemoPanelSlider(
                    value = uiState.simplifyToleranceM,
                    onValueChange = viewModel::updateSimplifyTolerance,
                    valueRange = 5f..200f,
                )
                DemoPanelButton(
                    onClick = viewModel::fillSimplifyExample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geometry_simplify_example_button"),
                ) {
                    Text(stringResource(R.string.geometry_simplify_example))
                }
                DemoPanelSwitchRow(
                    label = stringResource(R.string.geometry_demo_next_click_test),
                    checked = uiState.nextClickIsTestPoint,
                    onCheckedChange = viewModel::setNextClickIsTestPoint,
                )
            }
            GeometryDemoCapability.SegmentIndex -> {
                DemoPanelSwitchRow(
                    label = stringResource(R.string.geometry_demo_closed_path),
                    checked = uiState.pathClosedForIndex,
                    onCheckedChange = viewModel::updatePathClosedForIndex,
                )
                DemoPanelSwitchRow(
                    label = stringResource(R.string.geometry_demo_next_click_test),
                    checked = uiState.nextClickIsTestPoint,
                    onCheckedChange = viewModel::setNextClickIsTestPoint,
                )
            }
            GeometryDemoCapability.Area -> {
                DemoPanelSwitchRow(
                    label = stringResource(R.string.geometry_demo_next_click_test),
                    checked = uiState.nextClickIsTestPoint,
                    onCheckedChange = viewModel::setNextClickIsTestPoint,
                )
            }
            GeometryDemoCapability.SignedArea -> {
                DemoPanelButton(
                    onClick = viewModel::fillSignedAreaCounterClockwise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geometry_sample_ccw_button"),
                ) {
                    Text(stringResource(R.string.geometry_demo_sample_ccw))
                }
                DemoPanelButton(
                    onClick = viewModel::fillSignedAreaClockwise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geometry_sample_cw_button"),
                ) {
                    Text(stringResource(R.string.geometry_demo_sample_cw))
                }
            }
            GeometryDemoCapability.IsClosed -> {
                DemoPanelButton(onClick = viewModel::closePolygonExplicitly, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.geometry_demo_close_polygon))
                }
                DemoPanelButton(
                    onClick = viewModel::fillClosedPolygonExample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geometry_closed_sample_button"),
                ) {
                    Text(stringResource(R.string.geometry_demo_closed_sample))
                }
            }
            GeometryDemoCapability.EncodeDecode -> {
                if (uiState.encodedPreview.isNotBlank()) {
                    Text(
                        text = uiState.encodedPreview,
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(stringResource(R.string.geometry_demo_decoded_count, uiState.decodedCount))
                }
            }
            else -> Unit
        }

        Spacer(Modifier.height(8.dp))
        DemoPanelButton(
            onClick = viewModel::clearPoints,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("geometry_clear_button"),
        ) {
            Text(stringResource(R.string.geometry_demo_clear_points))
        }
    }
}

@Composable
private fun DemoPanelSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        DemoPanelSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
