package com.maptec.applied.demo.ui.screens.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

private const val TRAFFIC_MAP_STYLE_LIGHT = Constants.DEFAULT_STYLE_ID
private const val TRAFFIC_MAP_STYLE_DARK = "dark"

@Composable
fun MapTrafficScreen(modifier: Modifier = Modifier) {
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }
    var isMapStyleSwitching by remember { mutableStateOf(false) }
    var isDarkMapStyle by remember { mutableStateOf(false) }
    var trafficEnabled by remember { mutableStateOf(true) }
    var currentZoom by remember { mutableStateOf("13.00") }
    // 样式加载失败时用于回滚深浅色开关。
    var previousDarkMapStyle by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPosition = remember {
        CameraPosition.Builder().target(LatLng(1.360879, 103.732578)).zoom(13.0).build()
    }

    // 路况开关内部也会触发一次样式重载，SDK 会复用这里传给 setStyle 的回调。
    val styleStatusCallback = remember {
        object : StyleStatusCallback {
            override fun onStyleLoaded(style: Style?) {}

            override fun onStyleRendered(style: Style?) {
                isMapStyleSwitching = false
                isStyleRendered = true
                maptecMap?.let { map ->
                    trafficEnabled = map.isTrafficEnabled
                    currentZoom = String.format("%.2f", map.cameraPosition.zoom)
                }
            }

            override fun onFailed(style: Style?, message: String) {
                isMapStyleSwitching = false
                isStyleRendered = true
                isDarkMapStyle = previousDarkMapStyle
                // 路况变体加载失败时 SDK 会回退到基础样式并关闭路况，以 SDK 状态为准。
                maptecMap?.let { trafficEnabled = it.isTrafficEnabled }
                showMapStyleLoadError(context, message)
            }
        }
    }

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            zoomButtonsEnabled(false)
            camera(cameraPosition)
            // Plan B：SDK 默认关，仅本页显式打开路况。
            setTrafficEnabled(true)
        }
        MapView(context, options).apply {
            onCreate(null)
            tag = "mapView"
            getMapAsync { map ->
                maptecMap = map
                currentZoom = String.format("%.2f", map.cameraPosition.zoom)
                map.addOnCameraMoveListener {
                    currentZoom = String.format("%.2f", map.cameraPosition.zoom)
                }
                map.setStyle(StyleOption(TRAFFIC_MAP_STYLE_LIGHT), styleStatusCallback)
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    LaunchedEffect(maptecMap, isStyleRendered) {
        val map = maptecMap ?: return@LaunchedEffect
        if (!isStyleRendered) return@LaunchedEffect
        trafficEnabled = map.isTrafficEnabled
        currentZoom = String.format("%.2f", map.cameraPosition.zoom)
    }

    fun switchMapStyle(dark: Boolean) {
        val map = maptecMap ?: return
        val styleId = if (dark) TRAFFIC_MAP_STYLE_DARK else TRAFFIC_MAP_STYLE_LIGHT
        if (isMapStyleSwitching) return
        if (isDarkMapStyle == dark && isStyleRendered) return

        previousDarkMapStyle = isDarkMapStyle
        isMapStyleSwitching = true
        isDarkMapStyle = dark
        isStyleRendered = false
        // 只传基础样式 id，当前路况状态由 SDK 内部带上。
        map.setStyle(StyleOption(styleId), styleStatusCallback)
    }

    fun switchTraffic(enabled: Boolean) {
        val map = maptecMap ?: return
        if (isMapStyleSwitching) return

        previousDarkMapStyle = isDarkMapStyle
        trafficEnabled = enabled
        isMapStyleSwitching = true
        isStyleRendered = false
        // 内部会切换到当前底图的路况变体，完成/失败经由 styleStatusCallback 通知。
        map.setTrafficEnabled(enabled)
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                TrafficSwitchRow(
                    label = stringResource(
                        R.string.map_traffic_map_style_label,
                        stringResource(
                            if (isDarkMapStyle) {
                                R.string.map_style_dark
                            } else {
                                R.string.map_style_light
                            },
                        ),
                    ),
                    checked = isDarkMapStyle,
                    enabled = isStyleRendered && !isMapStyleSwitching,
                    testTag = "switch_map_style",
                    onCheckedChange = { switchMapStyle(it) },
                )
                TrafficSwitchRow(
                    label = stringResource(R.string.map_traffic_switch_traffic),
                    checked = trafficEnabled,
                    enabled = isStyleRendered && !isMapStyleSwitching,
                    testTag = "switch_traffic_enabled",
                    onCheckedChange = { enabled -> switchTraffic(enabled) },
                )
                TrafficInfoRow(
                    label = stringResource(R.string.map_traffic_current_zoom),
                    value = currentZoom,
                    testTag = "text_current_zoom",
                )
            }
        },
        content = {
            Box(modifier = modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView },
                    update = {},
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("mapView"),
                )
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

@Composable
private fun TrafficSwitchRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        DemoPanelSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun TrafficInfoRow(
    label: String,
    value: String,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.testTag(testTag))
    }
}
