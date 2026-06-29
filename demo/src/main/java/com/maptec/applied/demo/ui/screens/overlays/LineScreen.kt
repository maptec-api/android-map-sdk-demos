package com.maptec.applied.demo.ui.screens.overlays

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.javabase.log.LoggerFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.maptec.applied.demo.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.utils.BitmapUtils
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineCapType
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerOptions

private data class LineDropdownOption(val value: String, val label: String)

/** MapOverlayEngine.addImage 注册的 line-pattern 名称，与 drawable 资源一一对应。 */
private const val LINE_PATTERN_CLASSIC = "line_classic"
private const val LINE_PATTERN_CHEVRON = "line_chevron"
private const val LINE_PATTERN_TRIANGLE = "line_triangle"
private const val LINE_PATTERN_CUSTOM = "line_custom"


private const val LINE_CAP_CUSTOM = "custom"
private const val LINE_CAP_ARROW = "arrow"
// arrow_*.xml 底边中心在 (150,150)，底边 vp 高 100；屏幕底边高 = vp * iconSize，目标 2×线宽
private const val ARROW_TRIANGLE_BASE_VP = 100f
private const val ARROW_BASE_TO_STROKE_RATIO = 2f
// marker 中心跟线端点的像素间距
private const val LINE_CAP_CUSTOM_GAP_PX = 150f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()

    // 默认展开 bottom sheet
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            LineBottomDetailPanel(mapView)
        },
        content = { padding ->
            Mapview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle {
                        registerLinePatternImages(map, view.context)
                        // 端点 marker 不再预先 setup —— addMarker 时 SDK lazy init
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineBottomDetailPanel(
    mapView: MapView?,
) {
    var latLngs by remember { mutableStateOf("[[1.4,103.75],[1.41,103.76],[1.42,103.75]]") }
    var strokeColor by remember { mutableStateOf("#00A63E") }
    var strokeWeight by remember { mutableStateOf("5") }
    var strokeOpacity by remember { mutableStateOf("1.0") }
    var isClosed by remember { mutableStateOf(false) }
    var draggable by remember { mutableStateOf(false) }

    // 虚线
    var dashArrayStr by remember { mutableStateOf("") }

    // 流动动画
    var flowAnimation by remember { mutableStateOf(false) }
    var flowSpeed by remember { mutableStateOf("1.0") }
    var flowColor by remember { mutableStateOf("#FFFFFF") }

    // 发光
    var glowEffect by remember { mutableStateOf(false) }
    var glowColor by remember { mutableStateOf("#00A63E") }
    var glowRadius by remember { mutableStateOf("10") }
    val glowColorError = remember(glowColor) { validateColor(glowColor) }
    val glowRadiusError = remember(glowRadius) { validateGlowRadius(glowRadius) }

    // 箭头线（line-pattern + mask-color）
    var arrowsEnabled by remember { mutableStateOf(false) }
    var arrowsPattern by remember { mutableStateOf(LINE_PATTERN_CLASSIC) }
    var arrowsColor by remember { mutableStateOf("#000000") }
    val arrowsColorError = remember(arrowsColor) { validateColor(arrowsColor) }

    var arrowsTypeMenuExpanded by remember { mutableStateOf(false) }

    val arrowsTypeOptions = listOf(
        LineDropdownOption(LINE_PATTERN_CLASSIC, "Classic"),
        LineDropdownOption(LINE_PATTERN_CHEVRON, "Chevron"),
        LineDropdownOption(LINE_PATTERN_TRIANGLE, "Triangle"),
        LineDropdownOption(LINE_PATTERN_CUSTOM, "Custom"),
    )

    // 起点/终点线帽（lineCapStart/End）。默认 None = 不画端帽
    var startCap by remember { mutableStateOf(LineCapType.NONE) }
    var endCap by remember { mutableStateOf(LineCapType.NONE) }
    var startMenuExpanded by remember { mutableStateOf(false) }
    var endMenuExpanded by remember { mutableStateOf(false) }
    val lineCapOptions = listOf(
        LineDropdownOption(LineCapType.NONE,   "None"),
        LineDropdownOption(LineCapType.ROUND,  "Round"),
        LineDropdownOption(LineCapType.BUTT,   "Butt"),
        LineDropdownOption(LineCapType.SQUARE, "Square"),
        LineDropdownOption(LINE_CAP_ARROW,     "Arrow"),
        LineDropdownOption(LINE_CAP_CUSTOM,    "Custom"),
    )

    // 选 Custom / Arrow 时画的 marker，每次重绘前先清掉
    var customStartMarker by remember { mutableStateOf<Marker?>(null) }
    var customEndMarker by remember { mutableStateOf<Marker?>(null) }
    var arrowStartMarker by remember { mutableStateOf<Marker?>(null) }
    var arrowEndMarker by remember { mutableStateOf<Marker?>(null) }

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
            label = { Text(stringResource(R.string.line_vertex_array)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_vertices")
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = strokeColor,
                onValueChange = { strokeColor = it },
                label = { Text(stringResource(R.string.line_stroke_color)) },
                modifier = Modifier.weight(1f).padding(end = 8.dp).testTag("line_input_stroke_color")
            )
            OutlinedTextField(
                value = strokeOpacity,
                onValueChange = { strokeOpacity = it },
                label = { Text(stringResource(R.string.line_opacity)) },
                modifier = Modifier.weight(1f).padding(start = 8.dp).testTag("line_input_opacity")
            )
        }

        OutlinedTextField(
            value = strokeWeight,
            onValueChange = { strokeWeight = it },
            label = { Text(stringResource(R.string.line_width)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_width")
        )

        OutlinedTextField(
            value = dashArrayStr,
            onValueChange = { dashArrayStr = it },
            label = { Text(stringResource(R.string.line_dash_pattern)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_dash_pattern")
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.isClosed), modifier = Modifier.weight(1f))
            Switch(checked = isClosed, onCheckedChange = { isClosed = it }, modifier = Modifier.testTag("line_switch_closed"))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.line_draggable), modifier = Modifier.weight(1f))
            Switch(checked = draggable, onCheckedChange = { draggable = it }, modifier = Modifier.testTag("line_switch_draggable"))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = glowEffect,
                    role = Role.Switch,
                    onValueChange = { glowEffect = it },
                )
                .testTag("line_switch_glow"),
        ) {
            Text(stringResource(R.string.line_glow_enabled), modifier = Modifier.weight(1f))
            Switch(
                checked = glowEffect,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        if (glowEffect) {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                OutlinedTextField(
                    value = glowColor,
                    onValueChange = { glowColor = it },
                    label = { Text(stringResource(R.string.line_glow_color)) },
                    supportingText = glowColorError?.let { { Text(it, color = Color.Red) }},
                    isError = glowColorError != null,
                    modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("line_input_glow_color")
                )
                OutlinedTextField(
                    value = glowRadius,
                    onValueChange = { glowRadius = it },
                    label = { Text(stringResource(R.string.line_glow_radius)) },
                    supportingText = glowRadiusError?.let { { Text(it, color = Color.Red)} },
                    isError = glowRadiusError != null,
                    modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("line_input_glow_radius")
                )
            }
        }

        // 流动
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Text("流动动画", modifier = Modifier.weight(1f))
//            Switch(checked = flowAnimation, onCheckedChange = { flowAnimation = it })
//        }
//        if (flowAnimation) {
//            Row(modifier = Modifier.padding(bottom = 16.dp)) {
//                OutlinedTextField(
//                    value = flowColor,
//                    onValueChange = { flowColor = it },
//                    label = { Text("流动颜色") },
//                    modifier = Modifier.weight(1f).padding(end = 4.dp)
//                )
//                OutlinedTextField(
//                    value = flowSpeed,
//                    onValueChange = { flowSpeed = it },
//                    label = { Text("速度") },
//                    modifier = Modifier.weight(1f).padding(start = 4.dp)
//                )
//            }
//        }

        // 方向箭头
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("方向箭头", modifier = Modifier.weight(1f))
            Switch(
                checked = arrowsEnabled,
                onCheckedChange = { arrowsEnabled = it },
                modifier = Modifier.testTag("line_switch_directional_arrows"),
            )
        }
        if (arrowsEnabled) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = arrowsTypeMenuExpanded,
                    onExpandedChange = { arrowsTypeMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    OutlinedTextField(
                        value = arrowsTypeOptions.find { it.value == arrowsPattern }?.label ?: arrowsPattern,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("箭头样式") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = arrowsTypeMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = arrowsTypeMenuExpanded,
                        onDismissRequest = { arrowsTypeMenuExpanded = false },
                    ) {
                        arrowsTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    arrowsPattern = option.value
                                    arrowsTypeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = arrowsColor,
                    onValueChange = { arrowsColor = it },
                    label = { Text("箭头颜色") },
                    supportingText = arrowsColorError?.let { { Text(it, color = Color.Red) } },
                    isError = arrowsColorError != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 起点 / 终点样式（一行并排两个下拉）
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExposedDropdownMenuBox(
                expanded = startMenuExpanded,
                onExpandedChange = { startMenuExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = lineCapOptions.find { it.value == startCap }?.label ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("起点线帽") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = startMenuExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("line_dropdown_start_cap"),
                )
                ExposedDropdownMenu(
                    expanded = startMenuExpanded,
                    onDismissRequest = { startMenuExpanded = false },
                ) {
                    lineCapOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.label) },
                            onClick = {
                                startCap = opt.value
                                startMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = endMenuExpanded,
                onExpandedChange = { endMenuExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = lineCapOptions.find { it.value == endCap }?.label ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("终点线帽") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = endMenuExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("line_dropdown_end_cap"),
                )
                ExposedDropdownMenu(
                    expanded = endMenuExpanded,
                    onDismissRequest = { endMenuExpanded = false },
                ) {
                    lineCapOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.label) },
                            onClick = {
                                endCap = opt.value
                                endMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val pts = parseLineLatLngs(latLngs)
                if (pts.isEmpty()) return@Button
                if (arrowsEnabled && arrowsColorError != null) return@Button
                if (glowEffect && (glowColorError != null || glowRadiusError != null)) return@Button

                val hostView = mapView ?: return@Button
                hostView.getMapAsync { map ->
                    map.getStyle {
                        registerLinePatternImages(map, hostView.context)
                    }

                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllLines()
                    // 清掉上次 Custom / Arrow 选项画的 marker
                    customStartMarker?.remove(); customStartMarker = null
                    customEndMarker?.remove();   customEndMarker = null
                    arrowStartMarker?.remove();  arrowStartMarker = null
                    arrowEndMarker?.remove();    arrowEndMarker = null

                    val dashArray = if (dashArrayStr.isNotEmpty()) {
                        dashArrayStr.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
                    } else null

                    // Custom / Arrow 不下发引擎线帽，按 None 处理；其它值正常透传
                    val effectiveStartCap = when (startCap) {
                        LINE_CAP_CUSTOM, LINE_CAP_ARROW -> LineCapType.NONE
                        else -> startCap
                    }
                    val effectiveEndCap = when (endCap) {
                        LINE_CAP_CUSTOM, LINE_CAP_ARROW -> LineCapType.NONE
                        else -> endCap
                    }

                    val options = LineOptions()
                        .withLatLngs(pts)
                        .withStrokeColor(strokeColor)
                        .withStrokeWeight(strokeWeight.toFloat())
                        .withStrokeOpacity(strokeOpacity.toFloat())
                        .withIsClosed(isClosed)
                        .withDraggable(draggable)
                        .withGlowEffect(glowEffect)
                        .withGlowColor(glowColor)
                        .withGlowRadius(glowRadius.toFloat())
                        .withFlowAnimation(flowAnimation)
                        .withFlowColor(flowColor)
                        .withFlowSpeed(flowSpeed.toFloat())
                        .withLineCapStart(effectiveStartCap)
                        .withLineCapEnd(effectiveEndCap)
                        .apply {
                            if (arrowsEnabled) {
                                withLinePattern(arrowsPattern)
                                withMaskColor(arrowsColor.trim())
                            }
                        }

                    if (dashArray != null) {
                        options.withStrokeDashArray(dashArray)
                    }

                    val line = engine.addPolyline(options)
                    line.addOnDragListener(object : OnOverlayDragListener<Line> {
                        override fun onAnnotationDragStarted(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(mapView.context, "拖拽结束: ${line.id}", Toast.LENGTH_SHORT).show()
                        }
                    })
                    line.addOnMapClickListener(OnOverlayClickListener<Line> { ln ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onOverlayClick" }
                        Toast.makeText(mapView.context, "点击了线: ${ln.id}", Toast.LENGTH_SHORT).show()
                        true
                    })
                    line.addOnMapLongClickListener(OnOverlayLongClickListener<Line> { ln ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onOverlayLongClick" }
                        Toast.makeText(mapView.context, "长按了线: ${ln.id}", Toast.LENGTH_SHORT).show()
                        true
                    })

                    // Custom 模式：用 SDF star marker 在端点放一颗跟线同色的星
                    if (startCap == LINE_CAP_CUSTOM && pts.size >= 2) {
                        customStartMarker = addCustomCapMarker(
                            engine, hostView.context,
                            anchor = pts.first(), neighbor = pts[1],
                            strokeColor = strokeColor,
                        )
                    }
                    if (endCap == LINE_CAP_CUSTOM && pts.size >= 2) {
                        customEndMarker = addCustomCapMarker(
                            engine, hostView.context,
                            anchor = pts.last(), neighbor = pts[pts.size - 2],
                            strokeColor = strokeColor,
                        )
                    }

                    // Arrow 模式：起点 arrow_left / 终点 arrow_right，尺寸随线宽放大并外推至线端外侧
                    val strokePx = strokeWeight.trim().toFloatOrNull() ?: 5f
                    if (startCap == LINE_CAP_ARROW && pts.size >= 2) {
                        arrowStartMarker = addArrowCapMarker(
                            engine, hostView.context,
                            anchor = pts.first(), neighbor = pts[1],
                            strokeColor = strokeColor,
                            strokePx = strokePx,
                            isStart = true,
                        )
                    }
                    if (endCap == LINE_CAP_ARROW && pts.size >= 2) {
                        arrowEndMarker = addArrowCapMarker(
                            engine, hostView.context,
                            anchor = pts.last(), neighbor = pts[pts.size - 2],
                            strokeColor = strokeColor,
                            strokePx = strokePx,
                            isStart = false,
                        )
                    }

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14.0))
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("line_button_draw")
        ) {
            Text(stringResource(R.string.line_draw))
        }
    }
}

private fun registerLinePatternImages(map: MaptecMap, context: Context) {
    addLinePatternImage(map, context, R.drawable.line_classic, LINE_PATTERN_CLASSIC)
    addLinePatternImage(map, context, R.drawable.line_chevron, LINE_PATTERN_CHEVRON)
    addLinePatternImage(map, context, R.drawable.line_triangle, LINE_PATTERN_TRIANGLE)
    addLinePatternImage(map, context, R.drawable.harbor, LINE_PATTERN_CUSTOM)
}

private fun addLinePatternImage(map: MaptecMap, context: Context, drawableRes: Int, imageId: String) {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return
    val bitmap = BitmapUtils.getBitmapFromDrawable(drawable) ?: return
    map.getOverlayEngine().addImage(imageId, bitmap, false)
}

/**
 * Custom 线帽：在 [anchor] 处放一颗 SDF 五角星，颜色跟随线色，并用 iconOffset 把图标
 * 沿线段方向推离端点，避免跟线端贴在一起。
 *
 * @param engine     overlay 引擎
 * @param context    用于把 vector drawable 转 bitmap
 * @param anchor     端点位置（line 的起点或终点）
 * @param neighbor   线段中相邻的下一个点（起点用 pts[1]、终点用 pts[n-2]），用于求方向向量
 * @param strokeColor 当前线的描边颜色（"#RRGGBB" 或 "#AARRGGBB"），用于 SDF 上色
 */
private fun addCustomCapMarker(
    engine: com.maptec.applied.maps.overlay.MapOverlayEngine,
    context: android.content.Context,
    anchor: LatLng,
    neighbor: LatLng,
    strokeColor: String,
): Marker? {
    // 颜色解析失败 → 用 Color.BLACK 兜底，避免 SDF 渲染成透明
    val tint = try {
        android.graphics.Color.parseColor(strokeColor.trim())
    } catch (e: IllegalArgumentException) {
        android.graphics.Color.BLACK
    }

    // 沿"neighbor → anchor"方向单位向量，把 marker 在屏幕坐标里向"远离线"方向推 LINE_CAP_CUSTOM_GAP_PX
    // 注：iconOffset 单位是像素，符号约定 (+x = 向右, +y = 向下)
    val dLat = anchor.latitude - neighbor.latitude
    val dLng = anchor.longitude - neighbor.longitude
    val len = kotlin.math.sqrt(dLat * dLat + dLng * dLng)
    val (offsetX, offsetY) = if (len > 1e-9) {
        // 把 lat/lng 方向归一化（粗糙近似，端点附近足够准），
        // 屏幕 y 朝下而 lat 朝北 → 取反
        val nx = (dLng / len).toFloat()
        val ny = -(dLat / len).toFloat()
        nx * LINE_CAP_CUSTOM_GAP_PX to ny * LINE_CAP_CUSTOM_GAP_PX
    } else {
        0f to 0f
    }

    val opts = MarkerOptions()
        .withLatLng(anchor)
        .withIconResource(R.drawable.star, context, /* sdf = */ true)
        .withIconColor(tint)                      // 跟线色一致
        .withIconSize(0.15f)                      // star.xml 是 300x300dp，缩到能看的尺寸
        .withIconAnchor(MarkerAnchorType.CENTER)
        .withIconOffset(offsetX, offsetY)        // 推离线
        .withClickable(false)
        .withDraggable(false)
        .withVisible(true)
    return engine.addMarker(opts)
}

/**
 * Arrow 线帽：起点用 {@code arrow_left}、终点用 {@code arrow_right}，整体旋转到沿线方向，
 * 三角底边屏幕高度 = 2×[strokePx]；底边中心与 viewport 中心重合，CENTER 锚点落在端点、尖端朝外。
 *
 * 旋转推导：屏幕坐标 CW-from-east 角度 = {@code atan2(-dLat, dLng)}（lat 朝北，screen y 朝下）。
 *   - 终点：方向 = anchor - prev（沿线 forward），arrow_right 原生朝东 → rotation = angleDeg
 *   - 起点：方向 = anchor - next（反 line direction，指向"线外"），arrow_left 原生朝西 → rotation = angleDeg - 180
 *
 * @param isStart true=起点（用 arrow_left）；false=终点（用 arrow_right）
 */
private fun addArrowCapMarker(
    engine: com.maptec.applied.maps.overlay.MapOverlayEngine,
    context: android.content.Context,
    anchor: LatLng,
    neighbor: LatLng,
    strokeColor: String,
    strokePx: Float,
    isStart: Boolean,
): Marker? {
    val tint = try {
        android.graphics.Color.parseColor(strokeColor.trim())
    } catch (e: IllegalArgumentException) {
        android.graphics.Color.BLACK
    }

    val dLat = anchor.latitude - neighbor.latitude
    val dLng = anchor.longitude - neighbor.longitude
    val len = kotlin.math.sqrt(dLat * dLat + dLng * dLng)
    val angleDeg = if (len > 1e-9) {
        Math.toDegrees(kotlin.math.atan2(-dLat, dLng)).toFloat()
    } else 0f

    // arrow_right native pointing = east (0°)；arrow_left native pointing = west (180°)
    val drawableRes = if (isStart) R.drawable.arrow_left else R.drawable.arrow_right
    val rotation = if (isStart) angleDeg - 180f else angleDeg

    // 底边像素高 = ARROW_TRIANGLE_BASE_VP * iconSize，令之等于 2 * strokePx
    val iconSize = arrowIconSizeForStroke(strokePx)

    val opts = MarkerOptions()
        .withLatLng(anchor)
        .withIconResource(drawableRes, context, /* sdf = */ true)
        .withIconColor(tint)
        .withIconSize(iconSize)
        .withIconAnchor(MarkerAnchorType.CENTER)
        .withIconRotation(rotation)
        .withClickable(false)
        .withDraggable(false)
        .withVisible(true)
    return engine.addMarker(opts)
}

/** 三角底边（垂直于线方向）屏幕高度 = [ARROW_BASE_TO_STROKE_RATIO] × 线宽。 */
private fun arrowIconSizeForStroke(strokePx: Float): Float {
    val targetBasePx = strokePx * ARROW_BASE_TO_STROKE_RATIO
    return (targetBasePx / ARROW_TRIANGLE_BASE_VP).coerceIn(0.03f, 1.5f)
}

private fun validateColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入颜色"
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(trimmed)) null else "格式错误，示例：#00A63E 或 #RRGGBB"
}

private fun validateGlowRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入发光半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 1f..30f) null else "发光半径应在 1～30 之间"
}

private fun parseLineLatLngs(input: String): List<LatLng> {
    return try {
        val json = input.trim()
        if (json.isEmpty()) return emptyList()
        // 简单解析 [[lat,lng],...]
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
fun LineScreenPreview() {
    LineScreen()
}
