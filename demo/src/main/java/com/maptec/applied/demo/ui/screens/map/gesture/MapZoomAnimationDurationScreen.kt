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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
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
            ZoomAnimationDurationControlPanel(
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
private fun ZoomAnimationDurationControlPanel(
    mapView: MapView, isStyleRendered: Boolean
) {
    var isEnabled by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(300L) }

    // 初始化状态
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isEnabled = map.uiSettings.gestures.isZoomAnimationDurationEnabled
            duration = map.uiSettings.gestures.zoomAnimationDuration
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "启用自定义缩放动画时长")
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    isEnabled = enabled
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.isZoomAnimationDurationEnabled = enabled
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("switch_zoom_animation_enabled")
            )
        }

        Text(
            text = "动画时长: $duration ms",
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("text_zoom_animation_duration")
        )

        Slider(
            value = duration.toFloat(),
            onValueChange = { value ->
                duration = value.toLong()
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.zoomAnimationDuration = duration
                }
            },
            valueRange = 0f..2000f,
            steps = 39, // (2000 - 0) / 50 - 1 = 39 steps for 50ms intervals
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("slider_zoom_animation_duration")
        )

        Text(
            text = "说明：设置双击、两指单击等操作时的缩放过渡时长。0ms 表示立即完成。关闭开关后将恢复系统默认 300ms。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
