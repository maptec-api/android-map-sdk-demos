package com.maptec.applied.demo.ui.screens.map

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MapEventListenerScreen - 地图事件监听演示
 *
 * 演示以下事件的监听与开关：
 * 1. 地图视角移动中 (OnCameraMoveListener)
 * 2. 地图长按 (OnMapLongClickListener)
 * 3. 快速滑动 (OnFlingListener)
 * 4. 视角停止移动 (OnCameraIdleListener)
 * 5. 视角开始移动 (OnCameraMoveStartedListener)
 * 6. 地图点击 (OnMapClickListener)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapEventListenerScreen() {
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    
    // Logs
    val logs = remember { mutableStateListOf<String>() }
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
        if (logs.size > 100) logs.removeLast()
    }

    // Switch States
    var isCameraMoveEnabled by remember { mutableStateOf(false) }
    var isMapLongClickEnabled by remember { mutableStateOf(false) }
    var isFlingEnabled by remember { mutableStateOf(false) }
    var isCameraIdleEnabled by remember { mutableStateOf(false) }
    var isCameraMoveStartedEnabled by remember { mutableStateOf(false) }
    var isMapClickEnabled by remember { mutableStateOf(false) }

    // Listeners
    val cameraMoveListener = remember {
        MaptecMap.OnCameraMoveListener {
            addLog("Camera Move")
        }
    }
    
    val mapLongClickListener = remember {
        object : MaptecMap.OnMapLongClickListener {
            override fun onMapLongClick(point: LatLng): Boolean {
                addLog("Map Long Click: $point")
                return false
            }
        }
    }
    
    val flingListener = remember {
        MaptecMap.OnFlingListener {
            addLog("Fling")
        }
    }
    
    val cameraIdleListener = remember {
        MaptecMap.OnCameraIdleListener {
            addLog("Camera Idle")
        }
    }
    
    val cameraMoveStartedListener = remember {
        MaptecMap.OnCameraMoveStartedListener { reason ->
            val reasonStr = when (reason) {
                MaptecMap.OnCameraMoveStartedListener.REASON_API_GESTURE -> "GESTURE"
                MaptecMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "DEVELOPER_ANIMATION"
                MaptecMap.OnCameraMoveStartedListener.REASON_API_ANIMATION -> "API_ANIMATION"
                else -> "UNKNOWN"
            }
            addLog("Camera Move Started: $reasonStr")
        }
    }
    
    val mapClickListener = remember {
        object : MaptecMap.OnMapClickListener {
            override fun onMapClick(point: LatLng): Boolean {
                addLog("Map Click: $point")
                return false
            }
        }
    }

    // Manage Listeners based on state
    LaunchedEffect(maptecMap, isCameraMoveEnabled) {
        maptecMap?.let { map ->
            if (isCameraMoveEnabled) map.addOnCameraMoveListener(cameraMoveListener)
            else map.removeOnCameraMoveListener(cameraMoveListener)
        }
    }

    LaunchedEffect(maptecMap, isMapLongClickEnabled) {
        maptecMap?.let { map ->
            if (isMapLongClickEnabled) map.addOnMapLongClickListener(mapLongClickListener)
            else map.removeOnMapLongClickListener(mapLongClickListener)
        }
    }

    LaunchedEffect(maptecMap, isFlingEnabled) {
        maptecMap?.let { map ->
            if (isFlingEnabled) map.addOnFlingListener(flingListener)
            else map.removeOnFlingListener(flingListener)
        }
    }

    LaunchedEffect(maptecMap, isCameraIdleEnabled) {
        maptecMap?.let { map ->
            if (isCameraIdleEnabled) map.addOnCameraIdleListener(cameraIdleListener)
            else map.removeOnCameraIdleListener(cameraIdleListener)
        }
    }

    LaunchedEffect(maptecMap, isCameraMoveStartedEnabled) {
        maptecMap?.let { map ->
            if (isCameraMoveStartedEnabled) map.addOnCameraMoveStartedListener(cameraMoveStartedListener)
            else map.removeOnCameraMoveStartedListener(cameraMoveStartedListener)
        }
    }

    LaunchedEffect(maptecMap, isMapClickEnabled) {
        maptecMap?.let { map ->
            if (isMapClickEnabled) map.addOnMapClickListener(mapClickListener)
            else map.removeOnMapClickListener(mapClickListener)
        }
    }

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.map_event_listener_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ControlItem(
                        labelResId = R.string.map_event_listener_camera_move,
                        testTag = "switch_camera_move",
                        checked = isCameraMoveEnabled,
                        onCheckedChange = { isCameraMoveEnabled = it }
                    )
                    ControlItem(
                        labelResId = R.string.map_event_listener_map_long_click,
                        testTag = "switch_map_long_click",
                        checked = isMapLongClickEnabled,
                        onCheckedChange = { isMapLongClickEnabled = it }
                    )
                    ControlItem(
                        labelResId = R.string.map_event_listener_fling,
                        testTag = "switch_fling",
                        checked = isFlingEnabled,
                        onCheckedChange = { isFlingEnabled = it }
                    )
                    ControlItem(
                        labelResId = R.string.map_event_listener_camera_idle,
                        testTag = "switch_camera_idle",
                        checked = isCameraIdleEnabled,
                        onCheckedChange = { isCameraIdleEnabled = it }
                    )
                    ControlItem(
                        labelResId = R.string.map_event_listener_camera_move_started,
                        testTag = "switch_camera_move_started",
                        checked = isCameraMoveStartedEnabled,
                        onCheckedChange = { isCameraMoveStartedEnabled = it }
                    )
                    ControlItem(
                        labelResId = R.string.map_event_listener_map_click,
                        testTag = "switch_map_click",
                        checked = isMapClickEnabled,
                        onCheckedChange = { isMapClickEnabled = it }
                    )
                }

                Divider()

                // Logs
                Text(
                    text = stringResource(R.string.map_event_listener_log_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                
                Button(
                    onClick = { logs.clear() },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("btn_clear_logs")
                ) {
                    Text(stringResource(R.string.map_event_listener_clear_log))
                }
            }
        }
    ) { padding ->
        Mapview(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onMapReady = { _, map ->
                maptecMap = map
            }
        )
    }
}

@Composable
private fun ControlItem(
    @StringRes labelResId: Int,
    testTag: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(labelResId))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
