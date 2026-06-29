package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDoubleTapTiltResetScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).tilt(45.0) // 默认给一点倾斜，方便观察复位效果
        .build(),
) {
    val context = LocalContext.current

    var isStyleRendered by remember { mutableStateOf(false) }


    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
            // 默认开启倾斜复位，方便演示
            doubleTapTiltResetEnabled(true)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {
                    }

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(
                        style: Style?, message: String
                    ) {
                    }
                })
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
            DoubleTapTiltResetControlPanel(
                mapView = mapView, isStyleRendered = isStyleRendered
            )
        }) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { })
    }
}

@Composable
private fun DoubleTapTiltResetControlPanel(
    mapView: MapView, isStyleRendered: Boolean
) {
    var isTiltResetEnabled by remember { mutableStateOf(true) }
    var currentTilt by remember { mutableStateOf(0.0) }

    // 初始化状态并监听倾斜角变化
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isTiltResetEnabled = map.uiSettings.gestures.isDoubleTapTiltResetEnabled
            currentTilt = map.cameraPosition.tilt
            map.addOnCameraMoveListener {
                currentTilt = map.cameraPosition.tilt
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.enable_double_tap_tilt_reset))
            Switch(
                checked = isTiltResetEnabled, onCheckedChange = { enabled ->
                    isTiltResetEnabled = enabled
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.isDoubleTapTiltResetEnabled = enabled
                    }
                }, modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("switch_double_tap_tilt_reset")
            )
        }

        Text(
            text = "当前倾斜角: ${String.format("%.1f", currentTilt)}°",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "说明：开启后双击地图将复位倾斜角为 0，且不再触发缩放。建议先双指上滑倾斜地图再测试。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
