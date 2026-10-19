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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun MapTwoFingerTapZoomScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL)
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
                TwoFingerTapZoomControlPanel(mapView = mapView)
            }
        },
        content = {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("mapView"),
                factory = { mapView.apply { tag = "mapView" } },
                update = {},
            )
            if (isStyleRendered) {
                Box(modifier = Modifier.testTag("mapRendered"))
            }
        },
    )
}

@Composable
private fun TwoFingerTapZoomControlPanel(
    mapView: MapView,
) {
    var isEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isEnabled = map.uiSettings.gestures.isTwoFingerTapZoomEnabled
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(id = R.string.enable_two_finger_tap_zoom))
        DemoPanelSwitch(
            checked = isEnabled,
            onCheckedChange = { enabled ->
                isEnabled = enabled
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.isTwoFingerTapZoomEnabled = enabled
                }
            },
            modifier = Modifier.testTag("switch_two_finger_tap_zoom"),
        )
    }

    Text(
        text = stringResource(R.string.two_finger_tap_zoom_hint),
        modifier = Modifier.testTag("two_finger_tap_zoom_hint"),
        style = MaterialTheme.typography.bodySmall,
    )
}
