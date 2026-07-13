package com.maptec.applied.demo.ui.screens.interaction

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MapEventListenerScreen - 地图事件监听演示
 */
@Composable
fun MapEventListenerScreen() {
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }

    val logs = remember { mutableStateListOf<String>() }
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
        if (logs.size > 100) logs.removeLast()
    }

    var isCameraMoveEnabled by remember { mutableStateOf(true) }
    var isMapLongClickEnabled by remember { mutableStateOf(true) }
    var isFlingEnabled by remember { mutableStateOf(true) }
    var isCameraIdleEnabled by remember { mutableStateOf(true) }
    var isCameraMoveStartedEnabled by remember { mutableStateOf(true) }
    var isMapClickEnabled by remember { mutableStateOf(true) }

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

    DemoPanelScaffold(
        sheetPanelHeight = 520.dp,
        sheetContent = {
            DemoPanelColumn {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            EventSectionTitle(stringResource(R.string.map_event_listener_sheet_title))
                            Spacer(Modifier.height(12.dp))
                            ControlItem(
                                labelResId = R.string.map_event_listener_camera_move,
                                testTag = "switch_camera_move",
                                checked = isCameraMoveEnabled,
                                onCheckedChange = { isCameraMoveEnabled = it },
                            )
                            ControlItem(
                                labelResId = R.string.map_event_listener_map_long_click,
                                testTag = "switch_map_long_click",
                                checked = isMapLongClickEnabled,
                                onCheckedChange = { isMapLongClickEnabled = it },
                            )
                            ControlItem(
                                labelResId = R.string.map_event_listener_fling,
                                testTag = "switch_fling",
                                checked = isFlingEnabled,
                                onCheckedChange = { isFlingEnabled = it },
                            )
                            ControlItem(
                                labelResId = R.string.map_event_listener_camera_idle,
                                testTag = "switch_camera_idle",
                                checked = isCameraIdleEnabled,
                                onCheckedChange = { isCameraIdleEnabled = it },
                            )
                            ControlItem(
                                labelResId = R.string.map_event_listener_camera_move_started,
                                testTag = "switch_camera_move_started",
                                checked = isCameraMoveStartedEnabled,
                                onCheckedChange = { isCameraMoveStartedEnabled = it },
                            )
                            ControlItem(
                                labelResId = R.string.map_event_listener_map_click,
                                testTag = "switch_map_click",
                                checked = isMapClickEnabled,
                                onCheckedChange = { isMapClickEnabled = it },
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            EventSectionTitle(stringResource(R.string.map_event_listener_log_title))
                            Spacer(Modifier.height(12.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                    .padding(12.dp),
                            ) {
                                items(logs) { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                            DemoPanelButton(
                                onClick = { logs.clear() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag("btn_clear_logs"),
                            ) {
                                Text(
                                    text = stringResource(R.string.map_event_listener_clear_log),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { _, map ->
                    maptecMap = map
                },
            )
        },
    )
}

@Composable
private fun EventSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ControlItem(
    @StringRes labelResId: Int,
    testTag: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
