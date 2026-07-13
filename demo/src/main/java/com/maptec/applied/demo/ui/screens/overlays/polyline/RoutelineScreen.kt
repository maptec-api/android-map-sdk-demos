package com.maptec.applied.demo.ui.screens.overlays.polyline

import android.widget.Toast
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.javabase.log.LoggerFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions

private data class RoutelineDropdownOption(val value: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutelineScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            RoutelineBottomDetailPanel(mapView)
        },
        content = { padding ->
            com.maptec.applied.demo.map.Mapview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onMapReady = { view, _ ->
                    mapView = view
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutelineBottomDetailPanel(
    mapView: MapView?,
) {
    // 基础线参数
    var latLngs by remember { mutableStateOf("[[1.4,103.75],[1.41,103.76],[1.42,103.75],[1.43,103.77]]") }
    var strokeColor by remember { mutableStateOf("#0066FF") }
    var strokeWeight by remember { mutableStateOf("6") }
    var strokeOpacity by remember { mutableStateOf("1.0") }

    // 鱼骨线（沿线方向箭头）参数
    var fishboneEnabled by remember { mutableStateOf(true) }
    var fishboneType by remember { mutableStateOf(LineOptions.DIRECTIONAL_ARROWS_TYPE_CLASSIC) }
    var fishboneSpacing by remember { mutableStateOf("60") }
    var fishboneSizeW by remember { mutableStateOf("12") }
    var fishboneSizeH by remember { mutableStateOf("20") }
    var fishboneColor by remember { mutableStateOf("#FFFFFF") }
    var fishboneRotate by remember { mutableStateOf(LineOptions.DIRECTIONAL_ARROWS_ROTATE_ALONG) }
    var fishboneScaleWithZoom by remember { mutableStateOf(false) }

    var fishboneTypeMenuExpanded by remember { mutableStateOf(false) }
    var fishboneRotateMenuExpanded by remember { mutableStateOf(false) }

    val fishboneTypeOptions = listOf(
        RoutelineDropdownOption(LineOptions.DIRECTIONAL_ARROWS_TYPE_CLASSIC, "经典 (classic)"),
        RoutelineDropdownOption(LineOptions.DIRECTIONAL_ARROWS_TYPE_CHEVRON, "V型 (chevron)"),
        RoutelineDropdownOption(LineOptions.DIRECTIONAL_ARROWS_TYPE_TRIANGLE, "三角形 (triangle)")
    )

    val fishboneRotateOptions = listOf(
        RoutelineDropdownOption(LineOptions.DIRECTIONAL_ARROWS_ROTATE_ALONG, "沿线方向 (along)"),
        RoutelineDropdownOption(LineOptions.DIRECTIONAL_ARROWS_ROTATE_FIXED, "固定方向 (fixed)")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = latLngs,
            onValueChange = { latLngs = it },
            label = { Text("顶点数组 [[lat,lng],...]") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = strokeColor,
                onValueChange = { strokeColor = it },
                label = { Text("线颜色") },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = strokeOpacity,
                onValueChange = { strokeOpacity = it },
                label = { Text("不透明度") },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        OutlinedTextField(
            value = strokeWeight,
            onValueChange = { strokeWeight = it },
            label = { Text("线宽 (px)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // 鱼骨线开关
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("启用鱼骨线", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = fishboneEnabled, onCheckedChange = { fishboneEnabled = it })
        }

        if (fishboneEnabled) {
            // 样式
            ExposedDropdownMenuBox(
                expanded = fishboneTypeMenuExpanded,
                onExpandedChange = { fishboneTypeMenuExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = fishboneTypeOptions.find { it.value == fishboneType }?.label ?: fishboneType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("样式") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fishboneTypeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = fishboneTypeMenuExpanded,
                    onDismissRequest = { fishboneTypeMenuExpanded = false }
                ) {
                    fishboneTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                fishboneType = option.value
                                fishboneTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 旋转模式
            ExposedDropdownMenuBox(
                expanded = fishboneRotateMenuExpanded,
                onExpandedChange = { fishboneRotateMenuExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = fishboneRotateOptions.find { it.value == fishboneRotate }?.label ?: fishboneRotate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("旋转模式") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fishboneRotateMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = fishboneRotateMenuExpanded,
                    onDismissRequest = { fishboneRotateMenuExpanded = false }
                ) {
                    fishboneRotateOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                fishboneRotate = option.value
                                fishboneRotateMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 间距 / 颜色
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedTextField(
                    value = fishboneSpacing,
                    onValueChange = { fishboneSpacing = it },
                    label = { Text("间距 (20~500)") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                OutlinedTextField(
                    value = fishboneColor,
                    onValueChange = { fishboneColor = it },
                    label = { Text("颜色") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            // 尺寸 宽 / 高
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedTextField(
                    value = fishboneSizeW,
                    onValueChange = { fishboneSizeW = it },
                    label = { Text("宽") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                OutlinedTextField(
                    value = fishboneSizeH,
                    onValueChange = { fishboneSizeH = it },
                    label = { Text("高") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            // 缩放时行为：true = 大小随地图缩放，false = 大小固定
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("缩放时行为")
                    Text(
                        if (fishboneScaleWithZoom) "大小随地图缩放" else "大小固定",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = fishboneScaleWithZoom,
                    onCheckedChange = { fishboneScaleWithZoom = it }
                )
            }
        }

        Button(
            onClick = {
                val pts = parseRoutelineLatLngs(latLngs)
                if (pts.isEmpty()) return@Button

                mapView?.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllLines()

                    val options = LineOptions()
                        .withLatLngs(pts)
                        .withStrokeColor(strokeColor)
                        .withStrokeWeight(strokeWeight.toFloatOrNull() ?: 6f)
                        .withStrokeOpacity(strokeOpacity.toFloatOrNull() ?: 1f)
                        .withDirectionalArrowsEnabled(fishboneEnabled)
                        .withDirectionalArrowsType(fishboneType)
                        .withDirectionalArrowsColor(fishboneColor)
                        .withDirectionalArrowsSpacing(fishboneSpacing.toFloatOrNull() ?: 60f)
                        .withDirectionalArrowsSize(
                            floatArrayOf(
                                fishboneSizeW.toFloatOrNull() ?: 12f,
                                fishboneSizeH.toFloatOrNull() ?: 20f
                            )
                        )
                        .withDirectionalArrowsRotate(fishboneRotate)
                        .withDirectionalArrowsScaleWithZoom(fishboneScaleWithZoom)

                    val line = engine.addPolyline(options)
                    line.addOnDragListener(object : OnOverlayDragListener<Line> {
                        override fun onAnnotationDragStarted(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("RoutelineScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("RoutelineScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("RoutelineScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(mapView.context, "拖拽结束: ${line.id}", Toast.LENGTH_SHORT).show()
                        }
                    })
                    line.addOnMapClickListener(OnOverlayClickListener<Line> { ln ->
                        Toast.makeText(mapView.context, "点击了鱼骨线: ${ln.id}", Toast.LENGTH_SHORT).show()
                        true
                    })
                    line.addOnMapLongClickListener(OnOverlayLongClickListener<Line> { ln ->
                        Toast.makeText(mapView.context, "长按了鱼骨线: ${ln.id}", Toast.LENGTH_SHORT).show()
                        true
                    })

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14.0))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("绘制")
        }
    }
}

private fun parseRoutelineLatLngs(input: String): List<LatLng> {
    return try {
        val json = input.trim()
        if (json.isEmpty()) return emptyList()
        val pairs = json.replace("[", "").replace("]", "").split(",")
        val result = mutableListOf<LatLng>()
        for (i in pairs.indices step 2) {
            if (i + 1 < pairs.size) {
                result.add(LatLng(pairs[i].toDouble(), pairs[i + 1].toDouble()))
            }
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}

@Preview
@Composable
fun RoutelineScreenPreview() {
    RoutelineScreen()
}
