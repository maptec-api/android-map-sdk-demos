package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.maptec.applied.demo.R
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import com.maptec.applied.constants.Constants as SdkConstants
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDoubleTapZoomFactorScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).build(),
) {
    val context = LocalContext.current
    val logs = remember { mutableStateListOf<String>() }
    val lastZoomState = remember { mutableStateOf(0.0) }

    var isStyleRendered by remember { mutableStateOf(false) }


    fun addLog(msg: String) {
        val currentTime = System.currentTimeMillis()
        val timeStr =
            SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(currentTime))
        logs.add(0, "[$timeStr] $msg")
        if (logs.size > 50) logs.removeAt(logs.size - 1)
    }

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

                lastZoomState.value = map.cameraPosition.zoom

                map.addOnCameraMoveListener {

                }

                map.addOnCameraIdleListener {
                    val currentZoom = map.cameraPosition.zoom
                    val diff = currentZoom - lastZoomState.value
                    if (Math.abs(diff) > 0.01) {
                        addLog(
                            "Zoom 变化: ${
                                String.format(
                                    "%.2f", lastZoomState.value
                                )
                            } -> ${String.format("%.2f", currentZoom)} (增量: ${
                                String.format(
                                    "%.2f", diff
                                )
                            })"
                        )
                        lastZoomState.value = currentZoom
                    }
                }
            }

//            setOnTouchListener { _, event ->
//                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
//                    addLog("屏幕按下")
//                }
//                false
//            }
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
            DoubleTapZoomFactorControlPanel(
                mapView = mapView, logs = logs, isStyleRendered = isStyleRendered, onClearLogs = {
                    logs.clear()
                })
        }) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { })
    }
}

@Composable
private fun DoubleTapZoomFactorControlPanel(
    mapView: MapView, logs: List<String>, isStyleRendered: Boolean, onClearLogs: () -> Unit,
) {
    val context = LocalContext.current
    var isCustomFactorEnabled by remember { mutableStateOf(true) }
    var zoomFactor by remember { mutableStateOf(SdkConstants.DEFAULT_DOUBLE_TAP_ZOOM_FACTOR) }
    var zoomLevel by remember { mutableStateOf(1.0) }


    // 初始化状态
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isCustomFactorEnabled = map.uiSettings.gestures.isCustomDoubleTapZoomFactorEnabled
            zoomFactor = map.uiSettings.gestures.doubleTapZoomFactor
            zoomLevel = ln(zoomFactor.toDouble()) / ln(2.0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Text(text = "启用自定义缩放因子")
//            Switch(
//                checked = isCustomFactorEnabled,
//                onCheckedChange = { enabled ->
//                    isCustomFactorEnabled = enabled
//                    mapView.getMapAsync { map ->
//                        map.uiSettings.gestures.isCustomDoubleTapZoomFactorEnabled = enabled
//                    }
//                },
//                modifier = Modifier
//                    .padding(start = 8.dp)
//                    .testTag("switch_custom_zoom_factor")
//            )
//        }

        Text(
            text = context.getString(R.string.double_tap_zoom_factor_label, zoomFactor),
            modifier = Modifier.testTag("text_zoom_factor")
        )

        Slider(
            value = zoomFactor,
            onValueChange = { value ->
                zoomFactor = value
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.doubleTapZoomFactor = zoomFactor
                }
                zoomLevel = ln(zoomFactor.toDouble()) / ln(2.0)
            },
            valueRange = SdkConstants.MIN_DOUBLE_TAP_ZOOM_FACTOR..SdkConstants.MAX_DOUBLE_TAP_ZOOM_FACTOR,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("slider_zoom_factor")
        )

        Text(
            text = context.getString(R.string.double_tap_zoom_factor_description),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = context.getString(R.string.double_tap_zoom_level_label, zoomLevel),
            modifier = Modifier.padding(top = 8.dp).testTag("text_zoom_level"),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = context.getString(R.string.double_tap_zoom_hint),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = context.getString(R.string.double_tap_zoom_log_title), style = MaterialTheme.typography.titleSmall)
            Button(onClick = onClearLogs, modifier = Modifier.testTag("button_clear_log")) {
                Text(context.getString(R.string.double_tap_zoom_clear_log))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(top = 8.dp)
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(logs) { log ->
                    Text(text = log, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
