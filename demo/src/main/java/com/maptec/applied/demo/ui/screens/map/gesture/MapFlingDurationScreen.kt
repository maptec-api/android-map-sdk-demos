package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            FlingDurationControlPanel(
                mapView = mapView, isStyleRendered, isStyleRendered
            )
        }) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { })
    }
}

@Composable
private fun FlingDurationControlPanel(
    mapView: MapView, isStyleRendered: Boolean, isStyleRendered1: Boolean
) {
    var flingDuration by remember { mutableStateOf(1000L) }
    var inertiaScrollEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            flingDuration = gestures.inertiaScrollDuration
            inertiaScrollEnabled = gestures.isInertiaScrollEnabled
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.inertia_scroll_enabled_label),
                modifier = Modifier.testTag("text_inertia_scroll_switch_label")
            )
            Switch(
                checked = inertiaScrollEnabled, onCheckedChange = { enabled ->
                    inertiaScrollEnabled = enabled
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.isInertiaScrollEnabled = enabled
                    }
                }, modifier = Modifier.testTag("switch_inertia_scroll_enabled")
            )
        }

        Text(
            text = stringResource(id = R.string.fling_duration_label, flingDuration),
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("text_fling_duration")
        )

        Slider(
            value = flingDuration.toFloat(),
            onValueChange = { value ->
                flingDuration = value.toLong()
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.inertiaScrollDuration = flingDuration
                }
            },
            valueRange = 0f..5000f,
            steps = 50,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("slider_fling_duration")
        )

        Text(
            text = "说明：开关控制手势模块是否产生惯性滑动（松手后的减速动画）。时长为手势层基础参数；设为 0 也会关闭惯性。关闭开关时仍会保存当前时长，便于再次开启。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
