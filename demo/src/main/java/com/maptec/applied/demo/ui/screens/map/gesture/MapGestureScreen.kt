package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun MapGestureScreen(
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

                    override fun onFailed(style: Style?, message: String) {
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
            GestureControlPanel(
                mapView = mapView, isStyleRendered = isStyleRendered
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
private fun GestureSwitchItem(
    @StringRes textResId: Int,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    // 注意：这里去掉了原来外层的 Modifier.padding(8.dp)，
    // 因为间距我们将统一交由外层的 FlowRow 来管理，这样更优雅。
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(id = textResId))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .padding(start = 8.dp)
                .testTag(testTag)
        )
    }
}
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@Composable
private fun GestureControlPanel(
    mapView: MapView,
    isStyleRendered: Boolean
) {
    var allGestureEnabled by remember { mutableStateOf(true) }
    var isScrollGestureEnabled by remember { mutableStateOf(true) }
    var isDoubleClickedZoomGestureEnabled by remember { mutableStateOf(true) }
    var isDoubleFingerZoomGestureEnabled by remember { mutableStateOf(true) }
    var isTiltGestureEnabled by remember { mutableStateOf(true) }
    var isRotateGestureEnabled by remember { mutableStateOf(true) }

    // 使用 Column 并允许其内部内容上下滑动
    Column(
        modifier = Modifier
    ) {
        // 使用 FlowRow 实现流式布局，自动换行并统一管理间距
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), // 水平相邻组件的间距
            verticalArrangement = Arrangement.spacedBy(8.dp)   // 上下行的间距
        ) {

            GestureSwitchItem(
                textResId = R.string.enable_all_gesture,
                checked = allGestureEnabled,
                testTag = "switch_all_gesture"
            ) {
                allGestureEnabled = it
                mapView.getMapAsync { map -> map.uiSettings.gestures.setAllGesturesEnabled(it) }
            }

            GestureSwitchItem(
                textResId = R.string.enable_scroll_gesture,
                checked = isScrollGestureEnabled,
                testTag = "switch_scroll_gesture"
            ) {
                isScrollGestureEnabled = it
                mapView.getMapAsync { map -> map.uiSettings.gestures.isScrollGesturesEnabled = it }
            }

            GestureSwitchItem(
                textResId = R.string.enable_tilt_gesture,
                checked = isTiltGestureEnabled,
                testTag = "switch_tilt_gesture"
            ) {
                isTiltGestureEnabled = it
                mapView.getMapAsync { map -> map.uiSettings.gestures.isTiltGesturesEnabled = it }
            }

            GestureSwitchItem(
                textResId = R.string.enable_rotate_gesture,
                checked = isRotateGestureEnabled,
                testTag = "switch_rotate_gesture"
            ) {
                isRotateGestureEnabled = it
                mapView.getMapAsync { map -> map.uiSettings.gestures.isRotateGesturesEnabled = it }
            }

            GestureSwitchItem(
                textResId = R.string.enable_double_finger_gesture,
                checked = isDoubleFingerZoomGestureEnabled,
                testTag = "switch_double_finger_zoom_gesture"
            ) {
                isDoubleFingerZoomGestureEnabled = it
                if (!it) isDoubleClickedZoomGestureEnabled = false
                mapView.getMapAsync { map -> map.uiSettings.gestures.isZoomGesturesEnabled = it }
            }

            GestureSwitchItem(
                textResId = R.string.enable_double_click_zoom_gesture,
                checked = isDoubleClickedZoomGestureEnabled,
                testTag = "switch_double_click_zoom_gesture"
            ) {
                isDoubleClickedZoomGestureEnabled = it
                if (it) isDoubleFingerZoomGestureEnabled = true
                mapView.getMapAsync { map ->
                    if (it) map.uiSettings.gestures.isZoomGesturesEnabled = true
                    map.uiSettings.gestures.isDoubleTapGesturesEnabled = it
                }
            }
        }

        // 地图渲染状态
        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}