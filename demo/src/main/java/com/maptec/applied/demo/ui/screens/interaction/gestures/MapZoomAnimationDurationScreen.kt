package com.maptec.applied.demo.ui.screens.interaction.gestures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun MapZoomAnimationDurationScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).build(),
) {
    val context = LocalContext.current
    var isStyleRendered by remember { mutableStateOf(false) }

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}
                    override fun onStyleRendered(style: Style?) { isStyleRendered = true }
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
                ZoomAnimationDurationControlPanel(
                    mapView = mapView,
                    isStyleRendered = isStyleRendered,
                )
            }
        },
        content = {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("mapView"),
                factory = { mapView.apply { tag = "mapView" } },
                update = {},
            )
        },
    )
}

@Composable
private fun ZoomAnimationDurationControlPanel(
    mapView: MapView,
    isStyleRendered: Boolean,
) {
    var isEnabled by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(300L) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isEnabled = map.uiSettings.gestures.isZoomAnimationDurationEnabled
            duration = map.uiSettings.gestures.zoomAnimationDuration
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.zoom_animation_duration_enabled))
        DemoPanelSwitch(
            checked = isEnabled,
            onCheckedChange = { enabled ->
                isEnabled = enabled
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.isZoomAnimationDurationEnabled = enabled
                }
            },
            modifier = Modifier.testTag("switch_zoom_animation_enabled"),
        )
    }

    Text(
        text = stringResource(R.string.zoom_animation_duration_label, duration),
        modifier = Modifier.testTag("text_zoom_animation_duration"),
    )

    DemoPanelSlider(
        value = duration.toFloat(),
        onValueChange = { value ->
            duration = value.toLong()
            mapView.getMapAsync { map ->
                map.uiSettings.gestures.zoomAnimationDuration = duration
            }
        },
        valueRange = 0f..2000f,
        steps = 39,
        showRangeLabels = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_zoom_animation_duration"),
    )

    Text(
        text = stringResource(R.string.zoom_animation_duration_hint),
        style = MaterialTheme.typography.bodySmall,
    )

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
