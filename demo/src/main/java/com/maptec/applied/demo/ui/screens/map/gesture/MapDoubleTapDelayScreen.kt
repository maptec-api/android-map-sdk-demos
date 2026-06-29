package com.maptec.applied.demo.ui.screens.map.gesture

import android.view.MotionEvent
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
import androidx.compose.material3.Switch
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDoubleTapDelayScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL)
        .build(),
) {
    val context = LocalContext.current
    var isStyleRendered by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    val lastClickTimeState = remember { mutableStateOf(0L) }

    fun addLog(msg: String) {
        val currentTime = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(currentTime))
        val lastTime = lastClickTimeState.value
        val delta = if (lastTime > 0) currentTime - lastTime else 0
        val finalMsg = if (delta > 0 && delta < 2000) "$msg (间隔: ${delta}ms)" else msg
        logs.add(0, "[$timeStr] $finalMsg")
        lastClickTimeState.value = currentTime
        if (logs.size > 50) logs.removeAt(logs.size - 1)
    }

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

            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    addLog("屏幕按下")
                }
                false
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
            DoubleTapDelayControlPanel(
                mapView = mapView,
                logs = logs,
                isStyleRendered = isStyleRendered,
                onClearLogs = {
                    logs.clear()
                    lastClickTimeState.value = 0L
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { }
        )
    }
}

@Composable
private fun DoubleTapDelayControlPanel(
    mapView: MapView,
    logs: List<String>,
    isStyleRendered: Boolean,
    onClearLogs: () -> Unit
) {
    var isCustomDetectionEnabled by remember { mutableStateOf(true) }
    var doubleTapDelay by remember { mutableStateOf(300L) }

    // 初始化状态
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isCustomDetectionEnabled = map.uiSettings.gestures.isCustomDoubleTapDetectionEnabled
            doubleTapDelay = map.uiSettings.gestures.doubleTapDelay
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "启用自定义双击检测")
            Switch(
                checked = isCustomDetectionEnabled,
                onCheckedChange = { enabled ->
                    isCustomDetectionEnabled = enabled
                    mapView.getMapAsync { map ->
                        map.uiSettings.gestures.isCustomDoubleTapDetectionEnabled = enabled
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("switch_custom_double_tap")
            )
        }

        Text(
            text = "双击延时: $doubleTapDelay ms",
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("text_double_tap_delay")
        )

        Slider(
            value = doubleTapDelay.toFloat(),
            onValueChange = { value ->
                doubleTapDelay = value.toLong()
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.doubleTapDelay = doubleTapDelay
                }
            },
            valueRange = 100f..1000f,
            steps = 18, // (1000 - 100) / 50 = 18 steps for 50ms intervals
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("slider_double_tap_delay")
        )

        Text(
            text = "说明：启用自定义检测后，两次点击间隔小于上述延迟时间将判定为双击放大。建议测试 300ms-500ms。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "点击日志 (显示点击间隔):", style = MaterialTheme.typography.titleSmall)
            Button(onClick = onClearLogs) {
                Text("清除")
            }
        }

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
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
    }
}
