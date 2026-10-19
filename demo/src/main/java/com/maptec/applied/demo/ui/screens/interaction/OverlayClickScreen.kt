package com.maptec.applied.demo.ui.screens.interaction

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.maps.MapHitItem
import com.maptec.applied.maps.MapHitItem.MapHitSourceType
import com.maptec.applied.maps.MapHitItem.MapQueryType
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong

/**
 * 覆盖物统一拾取 Demo。
 *
 * 使用 [MaptecMap.queryMapHit]（[MapQueryType.BUILDING]）查询点击处建筑。
 * 首项为建筑时调用 [MaptecMap.setBuildingHighlight] 高亮；SDK 要求相机 tilt > 0 才生效。
 */
@Composable
fun OverlayClickScreen() {
    val context = LocalContext.current
    val buildingHighlightColor = remember(context) {
        context.getColor(R.color.building_highlight)
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val latestHitRequest = remember { AtomicLong(0L) }
    val configPanelController = LocalDemoConfigPanelController.current
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var overlayEngine by remember { mutableStateOf<MapOverlayEngine?>(null) }
    var showMarker by remember { mutableStateOf(true) }
    var showPolyline by remember { mutableStateOf(true) }
    var showPolygon by remember { mutableStateOf(true) }
    var showCircle by remember { mutableStateOf(true) }
    var markerClickable by remember { mutableStateOf(true) }
    var lineClickable by remember { mutableStateOf(true) }
    var fillClickable by remember { mutableStateOf(true) }
    var circleClickable by remember { mutableStateOf(true) }
    var lastHitResult by remember { mutableStateOf<List<MapHitItem>?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var line by remember { mutableStateOf<Line?>(null) }
    var fill by remember { mutableStateOf<Fill?>(null) }
    var circle by remember { mutableStateOf<Circle?>(null) }
    var highlightedBuildingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { configPanelController.available }.first { it }
        if (!configPanelController.expanded) {
            configPanelController.toggle()
        }
    }

    LaunchedEffect(showMarker, showPolyline, showPolygon, showCircle, overlayEngine) {
        overlayEngine?.let { engine ->
            if (showMarker && marker == null) {
                val markerOptions = MarkerOptions()
                    .withLatLng(LatLng(1.3521, 103.8198))
                    .withText("Sample Marker")
                    .withIconResource(R.drawable.marker_default, context)
                    .withClickable(markerClickable)
                marker = engine.addMarker(markerOptions)
            } else if (!showMarker) {
                marker?.remove()
                marker = null
            }

            if (showPolyline && line == null) {
                val points = listOf(
                    LatLng(1.3550, 103.8100),
                    LatLng(1.3560, 103.8200),
                    LatLng(1.3540, 103.8250),
                )
                val lineOptions = LineOptions().withLatLngs(points)
                    .withStrokeWeight(25f)
                    .withStrokeColor("#0000FF")
                line = engine.addPolyline(lineOptions).also { it.setClickable(lineClickable) }
            } else if (!showPolyline) {
                line?.let { engine.deleteLine(it) }
                line = null
            }

            if (showPolygon && fill == null) {
                val points = listOf(
                    LatLng(1.3500, 103.8150),
                    LatLng(1.3510, 103.8200),
                    LatLng(1.3490, 103.8220),
                    LatLng(1.3480, 103.8180),
                )
                val fillOptions = FillOptions().withLatLngs(listOf(points))
                    .withFillColor("#8000FF00")
                    .withStrokeColor("#FF008800")
                    .withStrokeWeight(2f)
                fill = engine.addPolygon(fillOptions).also { it.setClickable(fillClickable) }
            } else if (!showPolygon) {
                fill?.let { engine.deleteFill(it) }
                fill = null
            }

            if (showCircle && circle == null) {
                val circleOptions = CircleOptions()
                    .withCenter(LatLng(1.3470, 103.8120))
                    .withRadius(200.0)
                    .withFillColor("#80FF0000")
                    .withStrokeColor("#FFFF0000")
                    .withStrokeWeight(2f)
                circle = engine.addCircle(circleOptions).also { it.setClickable(circleClickable) }
            } else if (!showCircle) {
                circle?.let { engine.deleteCircle(it) }
                circle = null
            }
        }
    }

    LaunchedEffect(markerClickable, marker) {
        marker?.setClickable(markerClickable)
    }
    LaunchedEffect(lineClickable, line) {
        line?.setClickable(lineClickable)
    }
    LaunchedEffect(fillClickable, fill) {
        fill?.setClickable(fillClickable)
    }
    LaunchedEffect(circleClickable, circle) {
        circle?.setClickable(circleClickable)
    }

    val currentMarker = rememberUpdatedState(marker)
    val currentLine = rememberUpdatedState(line)
    val currentFill = rememberUpdatedState(fill)
    val currentCircle = rememberUpdatedState(circle)
    val currentHighlightedBuildingId = rememberUpdatedState(highlightedBuildingId)
    val currentOverlayEngine = rememberUpdatedState(overlayEngine)
    val currentMap = rememberUpdatedState(maptecMap)

    DisposableEffect(Unit) {
        onDispose {
            currentMarker.value?.remove()
            currentLine.value?.let { currentOverlayEngine.value?.deleteLine(it) }
            currentFill.value?.let { currentOverlayEngine.value?.deleteFill(it) }
            currentCircle.value?.let { currentOverlayEngine.value?.deleteCircle(it) }
            currentHighlightedBuildingId.value?.let { id ->
                currentMap.value?.setBuildingHighlight(id, false, buildingHighlightColor)
            }
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    fun clearAllOverlays() {
        if (marker != null || line != null || fill != null || circle != null || highlightedBuildingId != null) {
            showMarker = false
            showPolyline = false
            showPolygon = false
            showCircle = false
            highlightedBuildingId?.let { id ->
                maptecMap?.setBuildingHighlight(id, false, buildingHighlightColor)
            }
            highlightedBuildingId = null
        }
        lastHitResult = null
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                OverlayClickControlPanel(
                    showMarker = showMarker,
                    showPolyline = showPolyline,
                    showPolygon = showPolygon,
                    showCircle = showCircle,
                    markerClickable = markerClickable,
                    lineClickable = lineClickable,
                    fillClickable = fillClickable,
                    circleClickable = circleClickable,
                    lastHitResult = lastHitResult,
                    onShowMarkerChanged = { isAdd ->
                        if (isAdd && marker == null) {
                            showMarker = true
                        } else if (!isAdd && marker != null) {
                            showMarker = false
                        }
                    },
                    onShowPolylineChanged = { isAdd ->
                        if (isAdd && line == null) {
                            showPolyline = true
                        } else if (!isAdd && line != null) {
                            showPolyline = false
                        }
                    },
                    onShowPolygonChanged = { isAdd ->
                        if (isAdd && fill == null) {
                            showPolygon = true
                        } else if (!isAdd && fill != null) {
                            showPolygon = false
                        }
                    },
                    onShowCircleChanged = { isAdd ->
                        if (isAdd && circle == null) {
                            showCircle = true
                        } else if (!isAdd && circle != null) {
                            showCircle = false
                        }
                    },
                    onMarkerClickableChanged = { markerClickable = it },
                    onLineClickableChanged = { lineClickable = it },
                    onFillClickableChanged = { fillClickable = it },
                    onCircleClickableChanged = { circleClickable = it },
                    onReset = { clearAllOverlays() },
                )
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = CameraPosition.Builder()
                    .target(LatLng(1.3521, 103.8198))
                    .zoom(16.0)
                    .tilt(45.0)
                    .build(),
                onMapReady = { _, map ->
                    maptecMap = map
                    map.addOnMapClickListener(object : MaptecMap.OnMapClickListener {
                        override fun onMapClick(point: LatLng): Boolean = false

                        override fun onMapClick(screen: PointF, point: LatLng): Boolean {
                            val requestId = latestHitRequest.incrementAndGet()
                            map.queryMapHit(screen, MapQueryType.BUILDING) { hits ->
                                // 拾取回调不保证主线程；Compose State / 高亮 API 需切回主线程。
                                mainHandler.post {
                                    if (requestId != latestHitRequest.get()) {
                                        return@post
                                    }
                                    lastHitResult = hits

                                    val firstHit = hits.firstOrNull()
                                    if (firstHit != null && firstHit.sourceType == MapHitSourceType.BUILDING) {
                                        val buildingId = firstHit.id
                                        if (highlightedBuildingId != buildingId) {
                                            highlightedBuildingId?.let {
                                                map.setBuildingHighlight(it, false, buildingHighlightColor)
                                            }
                                            highlightedBuildingId = buildingId
                                            map.setBuildingHighlight(buildingId, true, buildingHighlightColor)
                                        }
                                    } else {
                                        highlightedBuildingId?.let {
                                            map.setBuildingHighlight(it, false, buildingHighlightColor)
                                        }
                                        highlightedBuildingId = null
                                    }

                                    if (hits.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "命中 ${hits.size} 个元素",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                            return false
                        }
                    })
                },
                onStyleRendered = { _, map, _ ->
                    // 样式就绪后再挂 overlay，否则默认覆盖物进页不可见。
                    overlayEngine = map.overlayEngine
                },
            )
        },
    )
}

@Composable
private fun OverlayClickControlPanel(
    showMarker: Boolean,
    showPolyline: Boolean,
    showPolygon: Boolean,
    showCircle: Boolean,
    markerClickable: Boolean,
    lineClickable: Boolean,
    fillClickable: Boolean,
    circleClickable: Boolean,
    lastHitResult: List<MapHitItem>?,
    onShowMarkerChanged: (Boolean) -> Unit,
    onShowPolylineChanged: (Boolean) -> Unit,
    onShowPolygonChanged: (Boolean) -> Unit,
    onShowCircleChanged: (Boolean) -> Unit,
    onMarkerClickableChanged: (Boolean) -> Unit,
    onLineClickableChanged: (Boolean) -> Unit,
    onFillClickableChanged: (Boolean) -> Unit,
    onCircleClickableChanged: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var expandedIndices by remember(lastHitResult) { mutableStateOf(setOf<Int>()) }

    Text(
        text = stringResource(R.string.overlay_config),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoPanelButton(
            onClick = { onShowMarkerChanged(true) },
            modifier = Modifier.weight(1f),
        ) {
            Text("添加Marker")
        }
        DemoPanelButton(
            onClick = { onShowMarkerChanged(false) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除Marker")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.overlay_click_marker_clickable),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = markerClickable,
            onCheckedChange = onMarkerClickableChanged,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoPanelButton(
            onClick = { onShowPolylineChanged(true) },
            modifier = Modifier.weight(1f),
        ) {
            Text("添加折线")
        }
        DemoPanelButton(
            onClick = { onShowPolylineChanged(false) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除折线")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.overlay_click_line_clickable),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = lineClickable,
            onCheckedChange = onLineClickableChanged,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoPanelButton(
            onClick = { onShowPolygonChanged(true) },
            modifier = Modifier.weight(1f),
        ) {
            Text("添加多边形")
        }
        DemoPanelButton(
            onClick = { onShowPolygonChanged(false) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除多边形")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.overlay_click_fill_clickable),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = fillClickable,
            onCheckedChange = onFillClickableChanged,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoPanelButton(
            onClick = { onShowCircleChanged(true) },
            modifier = Modifier.weight(1f),
        ) {
            Text("添加圆")
        }
        DemoPanelButton(
            onClick = { onShowCircleChanged(false) },
            modifier = Modifier.weight(1f),
        ) {
            Text("删除圆")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.overlay_click_circle_clickable),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = circleClickable,
            onCheckedChange = onCircleClickableChanged,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    DemoPanelButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reset_overlays))
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.overlay_click_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    lastHitResult?.let { hits ->
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.last_hit_result),
            style = MaterialTheme.typography.titleSmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hits.isEmpty()) {
                Text(
                    text = stringResource(R.string.overlay_click_no_hit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                hits.forEachIndexed { index, hit ->
                    val isExpanded = expandedIndices.contains(index)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatHitSummary(hit),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (isExpanded) "收起" else "显示全部",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    expandedIndices = if (isExpanded) {
                                        expandedIndices - index
                                    } else {
                                        expandedIndices + index
                                    }
                                },
                            )
                        }
                        if (isExpanded) {
                            Text(
                                text = formatHitDetails(hit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHitSummary(hit: MapHitItem): String {
    val typed = hit.typedObjectType
    val name = hit.featureData?.getValue("name")
    return buildString {
        append("id=${hit.id}, source=${hit.sourceType}, type=$typed")
        if (name != null) {
            append(", name=$name")
        }
    }
}

private fun formatHitDetails(hit: MapHitItem): String {
    val feature = hit.featureData
    val lines = mutableListOf<String>()
    listOf("name", "poi_type", "class", "kind", "layer", "type").forEach { key ->
        val value = feature?.getValue(key) ?: return@forEach
        lines += "$key=$value"
    }
    val anchor = feature?.anchor
    lines += if (anchor != null) {
        "anchor=[lat=${anchor.latitude}, lng=${anchor.longitude}]"
    } else {
        "anchor=null"
    }
    if (feature != null) {
        lines += "feature=$feature"
    }
    return lines.joinToString("\n")
}

