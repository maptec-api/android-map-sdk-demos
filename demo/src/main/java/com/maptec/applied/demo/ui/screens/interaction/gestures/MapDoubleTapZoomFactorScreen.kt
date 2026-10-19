package com.maptec.applied.demo.ui.screens.interaction.gestures

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.constants.Constants as SdkConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln

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
                    override fun onStyleLoaded(style: Style?) {}
                    override fun onStyleRendered(style: Style?) { isStyleRendered = true }
                    override fun onFailed(style: Style?, message: String) {}
                })

                lastZoomState.value = map.cameraPosition.zoom

                map.addOnCameraIdleListener {
                    val currentZoom = map.cameraPosition.zoom
                    val diff = currentZoom - lastZoomState.value
                    if (kotlin.math.abs(diff) > 0.01) {
                        addLog(
                            "Zoom 变化: ${
                                String.format(
                                    "%.2f", lastZoomState.value,
                                )
                            } -> ${String.format("%.2f", currentZoom)} (增量: ${
                                String.format(
                                    "%.2f", diff,
                                )
                            })",
                        )
                        lastZoomState.value = currentZoom
                    }
                }
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                DoubleTapZoomFactorControlPanel(
                    mapView = mapView,
                    logs = logs,
                    isStyleRendered = isStyleRendered,
                    onClearLogs = { logs.clear() },
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
private fun DoubleTapZoomFactorControlPanel(
    mapView: MapView,
    logs: List<String>,
    isStyleRendered: Boolean,
    onClearLogs: () -> Unit,
) {
    val context = LocalContext.current
    var zoomFactor by remember { mutableStateOf(SdkConstants.DEFAULT_DOUBLE_TAP_ZOOM_FACTOR) }
    var zoomLevel by remember { mutableStateOf(1.0) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            zoomFactor = map.uiSettings.gestures.doubleTapZoomFactor
            zoomLevel = ln(zoomFactor.toDouble()) / ln(2.0)
        }
    }

    Text(
        text = context.getString(R.string.double_tap_zoom_factor_label, zoomFactor),
        modifier = Modifier.testTag("text_zoom_factor"),
    )

    DemoPanelSlider(
        value = zoomFactor,
        onValueChange = { value ->
            zoomFactor = value
            mapView.getMapAsync { map ->
                map.uiSettings.gestures.doubleTapZoomFactor = zoomFactor
            }
            zoomLevel = ln(zoomFactor.toDouble()) / ln(2.0)
        },
        valueRange = SdkConstants.MIN_DOUBLE_TAP_ZOOM_FACTOR..SdkConstants.MAX_DOUBLE_TAP_ZOOM_FACTOR,
        showRangeLabels = false,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_zoom_factor"),
    )

    Text(
        text = context.getString(R.string.double_tap_zoom_factor_description),
        style = MaterialTheme.typography.bodySmall,
    )

    HorizontalDivider()

    Text(
        text = context.getString(R.string.double_tap_zoom_level_label, zoomLevel),
        modifier = Modifier.testTag("text_zoom_level"),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = context.getString(R.string.double_tap_zoom_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.double_tap_zoom_log_title),
            style = MaterialTheme.typography.titleSmall,
        )
        DemoPanelButton(onClick = onClearLogs, modifier = Modifier.testTag("button_clear_log")) {
            Text(context.getString(R.string.double_tap_zoom_clear_log))
        }
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

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
