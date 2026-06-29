package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.constants.Constants
import com.maptec.applied.demo.Constants as DemoConstants
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapZoomCenterModeScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(DemoConstants.DEFAULT_MAP_CENTER)
        .zoom(DemoConstants.DEFAULT_ZOOM_LEVEL)
        .build(),
) {
    val context = LocalContext.current

    val mapView = remember {
        val options = defaultDemoMapOptions(context)
            .apply {
                camera(cameraPosition)
            }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(DemoConstants.DEFAULT_STYLE_ID), null)
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            ZoomCenterModeControlPanel(mapView = mapView)
        }
    ) { _ ->
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { }
        )
    }
}

@Composable
private fun ZoomCenterModeControlPanel(
    mapView: MapView
) {
    var zoomCenterMode by remember { mutableStateOf(Constants.DEFAULT_ZOOM_CENTER_MODE) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            zoomCenterMode = map.uiSettings.gestures.zoomCenterMode
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "缩放中心模式:", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                modifier = Modifier.testTag("radio_zoom_center_gesture"),
                selected = zoomCenterMode == Constants.ZOOM_CENTER_GESTURE,
                onClick = {
                    zoomCenterMode = Constants.ZOOM_CENTER_GESTURE
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.zoomCenterMode = zoomCenterMode
                    }
                }
            )
            Text(text = "手势焦点 (gesture-center)")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                modifier = Modifier.testTag("radio_zoom_center_screen"),
                selected = zoomCenterMode == Constants.ZOOM_CENTER_SCREEN,
                onClick = {
                    zoomCenterMode = Constants.ZOOM_CENTER_SCREEN
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.zoomCenterMode = zoomCenterMode
                    }
                }
            )
            Text(text = "屏幕中心 (screen-center)")
        }

        Text(
            text = "说明：'gesture-center'以手势点（如双指中点、双击点）为焦点缩放；'screen-center'以屏幕中心为中心缩放。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
