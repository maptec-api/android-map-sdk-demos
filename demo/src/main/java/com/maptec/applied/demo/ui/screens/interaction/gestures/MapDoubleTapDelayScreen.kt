package com.maptec.applied.demo.ui.screens.interaction.gestures

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
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
import androidx.compose.ui.graphics.Color
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
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val finalMsg = if (delta > 0 && delta < 2000) {
            context.getString(R.string.double_tap_tap_interval, msg, delta)
        } else {
            msg
        }
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
                    addLog(context.getString(R.string.double_tap_screen_down))
                }
                false
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                DoubleTapDelayControlPanel(
                    mapView = mapView,
                    logs = logs,
                    isStyleRendered = isStyleRendered,
                    onClearLogs = {
                        logs.clear()
                        lastClickTimeState.value = 0L
                    },
                )
            }
        },
        content = {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("mapView"),
                factory = { mapView.apply { tag = "mapView" } },
                update = {},
            )
        },
    )
}

@Composable
private fun DoubleTapDelayControlPanel(
    mapView: MapView,
    logs: List<String>,
    isStyleRendered: Boolean,
    onClearLogs: () -> Unit,
) {
    var isCustomDetectionEnabled by remember { mutableStateOf(true) }
    var doubleTapDelay by remember { mutableStateOf(300L) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            isCustomDetectionEnabled = map.uiSettings.gestures.isCustomDoubleTapDetectionEnabled
            doubleTapDelay = map.uiSettings.gestures.doubleTapDelay
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.enable_custom_double_tap))
        DemoPanelSwitch(
            checked = isCustomDetectionEnabled,
            onCheckedChange = { enabled ->
                isCustomDetectionEnabled = enabled
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.isCustomDoubleTapDetectionEnabled = enabled
                }
            },
            modifier = Modifier.testTag("switch_custom_double_tap"),
        )
    }

    Text(
        text = stringResource(R.string.double_tap_delay_label, doubleTapDelay),
        modifier = Modifier.testTag("text_double_tap_delay"),
    )

    DemoPanelSlider(
        value = doubleTapDelay.toFloat(),
        onValueChange = { value ->
            doubleTapDelay = value.toLong()
            mapView.getMapAsync { map ->
                map.uiSettings.gestures.doubleTapDelay = doubleTapDelay
            }
        },
        valueRange = 100f..1000f,
        steps = 18,
        showRangeLabels = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_double_tap_delay"),
    )

    Text(
        text = stringResource(R.string.double_tap_delay_hint),
        style = MaterialTheme.typography.bodySmall,
    )

    HorizontalDivider()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.double_tap_tap_log_title), style = MaterialTheme.typography.titleSmall)
        DemoPanelButton(onClick = onClearLogs) {
            Text(stringResource(R.string.double_tap_zoom_clear_log))
        }
    }

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.LightGray.copy(alpha = 0.2f)),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            items(logs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
