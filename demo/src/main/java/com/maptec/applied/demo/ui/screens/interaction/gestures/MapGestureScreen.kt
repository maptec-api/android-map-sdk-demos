package com.maptec.applied.demo.ui.screens.interaction.gestures

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
                GestureControlPanel(
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
private fun GestureSwitchRow(
    @StringRes textResId: Int,
    checked: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = textResId),
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun GestureControlPanel(
    mapView: MapView,
    isStyleRendered: Boolean,
) {
    var allGestureEnabled by remember { mutableStateOf(true) }
    var isScrollGestureEnabled by remember { mutableStateOf(true) }
    var isDoubleClickedZoomGestureEnabled by remember { mutableStateOf(true) }
    var isDoubleFingerZoomGestureEnabled by remember { mutableStateOf(true) }
    var isTiltGestureEnabled by remember { mutableStateOf(true) }
    var isRotateGestureEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            allGestureEnabled = gestures.areAllGesturesEnabled()
            isScrollGestureEnabled = gestures.isScrollGesturesEnabled
            isTiltGestureEnabled = gestures.isTiltGesturesEnabled
            isRotateGestureEnabled = gestures.isRotateGesturesEnabled
            isDoubleFingerZoomGestureEnabled = gestures.isZoomGesturesEnabled
            isDoubleClickedZoomGestureEnabled = gestures.isDoubleTapGesturesEnabled
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GestureSwitchRow(
            textResId = R.string.enable_all_gesture,
            checked = allGestureEnabled,
            testTag = "switch_all_gesture",
        ) { enabled ->
            allGestureEnabled = enabled
            mapView.getMapAsync { map -> map.uiSettings.gestures.setAllGesturesEnabled(enabled) }
        }

        if (allGestureEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GestureSwitchRow(
                    textResId = R.string.enable_scroll_gesture,
                    checked = isScrollGestureEnabled,
                    testTag = "switch_scroll_gesture",
                ) { enabled ->
                    isScrollGestureEnabled = enabled
                    mapView.getMapAsync { map -> map.uiSettings.gestures.isScrollGesturesEnabled = enabled }
                }

                GestureSwitchRow(
                    textResId = R.string.enable_tilt_gesture,
                    checked = isTiltGestureEnabled,
                    testTag = "switch_tilt_gesture",
                ) { enabled ->
                    isTiltGestureEnabled = enabled
                    mapView.getMapAsync { map -> map.uiSettings.gestures.isTiltGesturesEnabled = enabled }
                }

                GestureSwitchRow(
                    textResId = R.string.enable_rotate_gesture,
                    checked = isRotateGestureEnabled,
                    testTag = "switch_rotate_gesture",
                ) { enabled ->
                    isRotateGestureEnabled = enabled
                    mapView.getMapAsync { map -> map.uiSettings.gestures.isRotateGesturesEnabled = enabled }
                }

                GestureSwitchRow(
                    textResId = R.string.enable_double_finger_gesture,
                    checked = isDoubleFingerZoomGestureEnabled,
                    testTag = "switch_double_finger_zoom_gesture",
                ) { enabled ->
                    isDoubleFingerZoomGestureEnabled = enabled
                    if (!enabled) isDoubleClickedZoomGestureEnabled = false
                    mapView.getMapAsync { map -> map.uiSettings.gestures.isZoomGesturesEnabled = enabled }
                }

                if (isDoubleFingerZoomGestureEnabled) {
                    GestureSwitchRow(
                        textResId = R.string.enable_double_click_zoom_gesture,
                        checked = isDoubleClickedZoomGestureEnabled,
                        testTag = "switch_double_click_zoom_gesture",
                        modifier = Modifier.padding(start = 16.dp),
                    ) { enabled ->
                        isDoubleClickedZoomGestureEnabled = enabled
                        if (enabled) isDoubleFingerZoomGestureEnabled = true
                        mapView.getMapAsync { map ->
                            if (enabled) map.uiSettings.gestures.isZoomGesturesEnabled = true
                            map.uiSettings.gestures.isDoubleTapGesturesEnabled = enabled
                        }
                    }
                }
            }
        }
    }

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
