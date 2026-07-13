package com.maptec.applied.demo.ui.screens.interaction.gestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.constants.Constants
import com.maptec.applied.demo.Constants as DemoConstants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun MapZoomCenterModeScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(DemoConstants.DEFAULT_MAP_CENTER)
        .zoom(DemoConstants.DEFAULT_ZOOM_LEVEL)
        .build(),
) {
    val context = LocalContext.current
    var isStyleRendered by remember { mutableStateOf(false) }

    val mapView = remember {
        val options = defaultDemoMapOptions(context)
            .apply {
                camera(cameraPosition)
            }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(DemoConstants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: Style?, message: String) {}
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                ZoomCenterModeControlPanel(mapView = mapView)
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().testTag("mapView"),
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                )
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

@Composable
private fun ZoomCenterModeControlPanel(
    mapView: MapView,
) {
    var zoomCenterMode by remember { mutableStateOf(Constants.DEFAULT_ZOOM_CENTER_MODE) }
    var userChangedMode by remember { mutableStateOf(false) }
    val userChangedModeState = rememberUpdatedState(userChangedMode)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            if (!userChangedModeState.value) {
                zoomCenterMode = map.uiSettings.gestures.zoomCenterMode
            }
        }
    }

    Text(text = stringResource(R.string.zoom_center_mode_title), style = MaterialTheme.typography.titleMedium)

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            modifier = Modifier.testTag("radio_zoom_center_gesture"),
            selected = zoomCenterMode == Constants.ZOOM_CENTER_GESTURE,
            onClick = {
                userChangedMode = true
                zoomCenterMode = Constants.ZOOM_CENTER_GESTURE
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.zoomCenterMode = zoomCenterMode
                }
            },
        )
        Text(text = stringResource(R.string.zoom_center_mode_gesture))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            modifier = Modifier.testTag("radio_zoom_center_screen"),
            selected = zoomCenterMode == Constants.ZOOM_CENTER_SCREEN,
            onClick = {
                userChangedMode = true
                zoomCenterMode = Constants.ZOOM_CENTER_SCREEN
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.zoomCenterMode = zoomCenterMode
                }
            },
        )
        Text(text = stringResource(R.string.zoom_center_mode_screen))
    }

    Text(
        text = stringResource(R.string.zoom_center_mode_hint),
        style = MaterialTheme.typography.bodySmall,
    )
}
