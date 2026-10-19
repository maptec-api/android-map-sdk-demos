package com.maptec.applied.demo.ui.screens.overlays.marker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.viewmodel.MarkerLayerViewModel
import com.maptec.applied.demo.viewmodel.MarkerLayerViewModelFactory

internal enum class MarkerLayerMode {
    BASIC,
    STYLE,
    URL,
    SDF,
}

@Composable
internal fun MarkerLayerScreen(
    mode: MarkerLayerMode,
    modifier: Modifier = Modifier,
    viewModel: MarkerLayerViewModel = viewModel(factory = MarkerLayerViewModelFactory()),
) {
    val context = LocalContext.current
    val markers by viewModel.markers.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
    val dragCompletedCount by viewModel.dragCompletedCount.collectAsState()
    var defaultMarkerAdded by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                MarkerLayerDetailPanel(viewModel = viewModel, mode = mode)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { mapView, mapLibreMap ->
                    mapLibreMap.getStyle { style ->
                        viewModel.initSymbolManager(context, mapView, mapLibreMap, style)
                        if (!defaultMarkerAdded) {
                            defaultMarkerAdded = true
                            when (mode) {
                                MarkerLayerMode.BASIC, MarkerLayerMode.STYLE -> viewModel.addMarkerByType()
                                MarkerLayerMode.URL -> viewModel.addMarkerByUrl()
                                MarkerLayerMode.SDF -> viewModel.addMarkerBySdf()
                            }
                        }
                    }
                },
            )

            // 增强型测试指示器：移至底部中心，增加 zIndex，并赋予固定尺寸和微弱颜色
            Box(Modifier.fillMaxSize()) {
                if (markers.isNotEmpty()) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(10.dp)
                            .background(Color.Red.copy(alpha = 0.01f))
                            .zIndex(100f)
                            .testTag("symbol_layer_has_markers")
                    )
                }
                if (selectedMarkerId != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(10.dp)
                            .background(Color.Blue.copy(alpha = 0.01f))
                            .zIndex(101f)
                            .testTag("symbol_layer_marker_selected")
                    )
                }
                if (isDragging) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(10.dp)
                            .background(Color.Green.copy(alpha = 0.01f))
                            .zIndex(102f)
                            .testTag("symbol_layer_marker_dragging")
                    )
                }
                if (dragCompletedCount > 0) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(10.dp)
                            .background(Color.Yellow.copy(alpha = 0.01f))
                            .zIndex(103f)
                            .testTag("symbol_layer_marker_drag_completed")
                    )
                }
            }
        },
    )
}

@Composable
internal fun MarkerLayerDetailPanel(
    viewModel: MarkerLayerViewModel,
    mode: MarkerLayerMode,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MarkerCommonParamsSection(viewModel)
        when (mode) {
            MarkerLayerMode.URL -> MarkerUrlAddSection(viewModel)
            MarkerLayerMode.BASIC, MarkerLayerMode.STYLE -> MarkerTypeAddSection(viewModel)
            MarkerLayerMode.SDF -> MarkerSdfAddSection(viewModel)
        }
        MarkerClearAllSection(viewModel)
        Spacer(modifier = Modifier.height(8.dp))
    }
}
