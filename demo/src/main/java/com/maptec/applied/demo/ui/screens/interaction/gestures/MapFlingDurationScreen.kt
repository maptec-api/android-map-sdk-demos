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
fun MapFlingDurationScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).build(),
) {
    var isStyleRendered by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                FlingDurationControlPanel(
                    mapView = mapView,
                    isStyleRendered = isStyleRendered,
                )
            }
        },
        content = {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mapView"),
                factory = { mapView.apply { tag = "mapView" } },
                update = {},
            )
        },
    )
}

@Composable
private fun FlingDurationControlPanel(
    mapView: MapView,
    isStyleRendered: Boolean,
) {
    var flingDuration by remember { mutableStateOf(1000L) }
    var inertiaScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            flingDuration = gestures.inertiaScrollDuration
            inertiaScrollEnabled = gestures.isInertiaScrollEnabled
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.inertia_scroll_enabled_label),
            modifier = Modifier.testTag("text_inertia_scroll_switch_label"),
        )
        DemoPanelSwitch(
            checked = inertiaScrollEnabled,
            onCheckedChange = { enabled ->
                inertiaScrollEnabled = enabled
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.isInertiaScrollEnabled = enabled
                }
            },
            modifier = Modifier.testTag("switch_inertia_scroll_enabled"),
        )
    }

    if (inertiaScrollEnabled) {
        Text(
            text = stringResource(id = R.string.fling_duration_label, flingDuration),
            modifier = Modifier.testTag("text_fling_duration"),
        )

        DemoPanelSlider(
            value = flingDuration.toFloat(),
            onValueChange = { value ->
                flingDuration = value.toLong()
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.inertiaScrollDuration = flingDuration
                }
            },
            valueRange = 0f..5000f,
            steps = 50,
            showRangeLabels = false,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_fling_duration"),
        )

        Text(
            text = stringResource(R.string.fling_duration_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
