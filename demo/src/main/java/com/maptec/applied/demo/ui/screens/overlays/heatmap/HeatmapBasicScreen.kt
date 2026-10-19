package com.maptec.applied.demo.ui.screens.overlays.heatmap

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.maptec.applied.maps.overlay.heatmap.HeatmapTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapBasicScreen() {
    val state = rememberHeatmapDemoState()
    var lastTile by remember { mutableStateOf<HeatmapTile?>(null) }

    HeatmapScaffold(
        defaultOptions = { state.buildOptions() },
        onTileDrawn = { lastTile = it },
        sheetContent = { mapView, _ ->
            HeatmapParameterInputs(state)
            HeatmapLivePreviewEffect(state, lastTile)
            DrawHeatmapButton(
                state = state,
                mapView = mapView,
                onTileDrawn = { lastTile = it },
            )
            LoadExternalHeatmapButton(
                state = state,
                mapView = mapView,
                onTileDrawn = { lastTile = it },
            )
        },
    )
}
