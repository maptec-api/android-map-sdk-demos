package com.maptec.applied.demo.ui.screens.overlays.polyline

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.javabase.log.LoggerFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.maptec.applied.demo.R
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
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

/** line-pattern 样式图 id，与 drawable 资源一一对应；bitmap 经 withLinePattern/setLinePattern 在 overlay 内注册。 */
private const val LINE_PATTERN_CLASSIC = "line_classic"
private const val LINE_PATTERN_CHEVRON = "line_chevron"
private const val LINE_PATTERN_TRIANGLE = "line_triangle"
private const val LINE_PATTERN_CUSTOM = "line_custom"

private const val LINE_CAP_CUSTOM = "custom"
private const val LINE_CAP_ARROW = "arrow"
// arrow_*.xml 底边过视口中心 (150,*)，底边 vp 高 140；屏幕底边高 = vp * iconSize，目标 2×线宽
private const val ARROW_TRIANGLE_BASE_VP = 140f
private const val ARROW_BASE_TO_STROKE_RATIO = 2f
// marker 中心跟线端点的像素间距
private const val LINE_CAP_CUSTOM_GAP_PX = 150f

/**
 * Arrow / Custom 用 Marker 画端帽时，折线本身必须用平切 [LineCapType.BUTT]，
 * 才能和三角形底边齐平。若传 [LineCapType.NONE]（空串），引擎会跳过不下发，
 * 保留上一次的 round，圆头会伸进三角形造成重叠。
 */
private fun engineCapForSelection(cap: String): String = when (cap) {
    LINE_CAP_ARROW, LINE_CAP_CUSTOM -> LineCapType.BUTT
    else -> cap
}

internal enum class LineMode {
    BASIC,
    GLOW,
    ARROWS,
    CAPS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LineScreen(
    mode: LineMode,
    modifier: Modifier = Modifier,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var previewLine by remember { mutableStateOf<Line?>(null) }
    var defaultLineAdded by remember { mutableStateOf(false) }

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                LineBottomDetailPanel(mapView, mode, previewLine) { previewLine = it }
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle {
                        if (!defaultLineAdded) {
                            defaultLineAdded = true
                            val points = parseLineLatLngs("[[1.360879,103.732578],[1.370879,103.742578],[1.380879,103.732578]]")
                            val options = LineOptions()
                                .withLatLngs(points)
                                .withStrokeColor("#00A63E")
                                .withStrokeWeight(5f)
                                .withStrokeOpacity(1f)
                            when (mode) {
                                LineMode.BASIC -> Unit
                                LineMode.GLOW -> options
                                    .withGlowEffect(true)
                                    .withGlowColor("#00A63E")
                                    .withGlowRadius(10f)
                                LineMode.ARROWS -> {
                                    linePatternBitmap(view.context, LINE_PATTERN_CLASSIC)?.let { bmp ->
                                        options.withLinePattern(bmp, LINE_PATTERN_CLASSIC)
                                    }
                                    options.withMaskColor("#000000")
                                }
                                LineMode.CAPS -> options
                                    .withLineCapStart(LineCapType.ROUND)
                                    .withLineCapEnd(LineCapType.ROUND)
                            }
                            previewLine = map.getOverlayEngine().addPolyline(options)
                            map.moveCameraToFitPolyline(points)
                        }
                    }
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LineBottomDetailPanel(
    mapView: MapView?,
    mode: LineMode,
    previewLine: Line? = null,
    onPreviewLineChanged: (Line) -> Unit = {},
) {
    var latLngs by remember { mutableStateOf("[[1.360879,103.732578],[1.370879,103.742578],[1.380879,103.732578]]") }
    var strokeColor by remember { mutableStateOf("#00A63E") }
    var strokeWeight by remember { mutableFloatStateOf(5f) }
    var strokeOpacity by remember { mutableFloatStateOf(1f) }
    var isClosed by remember { mutableStateOf(false) }
    var draggable by remember { mutableStateOf(false) }

    var dashArrayStr by remember { mutableStateOf("") }

    var flowAnimation by remember { mutableStateOf(false) }
    var flowSpeed by remember { mutableStateOf("1.0") }
    var flowColor by remember { mutableStateOf("#FFFFFF") }

    var glowEffect by remember(mode) { mutableStateOf(mode == LineMode.GLOW) }
    var glowColor by remember { mutableStateOf("#00A63E") }
    var glowRadius by remember { mutableStateOf("10") }
    val glowColorError = remember(glowColor) { validateColor(glowColor) }
    val glowRadiusError = remember(glowRadius) { validateGlowRadius(glowRadius) }

    var arrowsEnabled by remember(mode) { mutableStateOf(mode == LineMode.ARROWS) }
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

    var startCap by remember(mode) { mutableStateOf(if (mode == LineMode.CAPS) LineCapType.ROUND else LineCapType.NONE) }
    var endCap by remember(mode) { mutableStateOf(if (mode == LineMode.CAPS) LineCapType.ROUND else LineCapType.NONE) }
    var startMenuExpanded by remember { mutableStateOf(false) }
    var endMenuExpanded by remember { mutableStateOf(false) }
    val lineCapOptions = listOf(
        LineDropdownOption(LineCapType.NONE, "None"),
        LineDropdownOption(LineCapType.ROUND, "Round"),
        LineDropdownOption(LineCapType.BUTT, "Butt"),
        LineDropdownOption(LineCapType.SQUARE, "Square"),
        LineDropdownOption(LINE_CAP_ARROW, "Arrow"),
        LineDropdownOption(LINE_CAP_CUSTOM, "Custom"),
    )

    var customStartMarker by remember { mutableStateOf<Marker?>(null) }
    var customEndMarker by remember { mutableStateOf<Marker?>(null) }
    var arrowStartMarker by remember { mutableStateOf<Marker?>(null) }
    var arrowEndMarker by remember { mutableStateOf<Marker?>(null) }
    var capGeometryKey by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun requestMapRepaint() {
        mapView?.getMapAsync { map -> map.triggerRepaint() }
    }

    fun applyPreviewToLine(line: Line) {
        val points = parseLineLatLngs(latLngs)
        if (points.size >= 2) line.setLatLngs(points)
        runCatching { line.setStrokeColor(strokeColor) }
        line.setStrokeWeight(strokeWeight)
        line.setStrokeOpacity(strokeOpacity.coerceIn(0f, 1f))
        line.setClosed(isClosed)
        line.setStrokeDashArray(
            dashArrayStr.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray(),
        )
        line.setGlowEffectEnabled(glowEffect)
        runCatching { line.setGlowColor(glowColor) }
        glowRadius.toFloatOrNull()?.takeIf { it in 1f..30f }?.let(line::setGlowRadius)
        if (arrowsEnabled) {
            linePatternBitmap(context, arrowsPattern)?.let { bmp ->
                line.setLinePattern(bmp, arrowsPattern)
            }
            runCatching { line.setMaskColor(arrowsColor) }
        }
        line.setLineCapStart(engineCapForSelection(startCap))
        line.setLineCapEnd(engineCapForSelection(endCap))
        requestMapRepaint()
    }

    fun clearCapMarkers() {
        customStartMarker?.remove(); customStartMarker = null
        customEndMarker?.remove(); customEndMarker = null
        arrowStartMarker?.remove(); arrowStartMarker = null
        arrowEndMarker?.remove(); arrowEndMarker = null
    }

    fun capMarkersMatchSelection(): Boolean {
        val wantStartArrow = startCap == LINE_CAP_ARROW
        val wantEndArrow = endCap == LINE_CAP_ARROW
        val wantStartCustom = startCap == LINE_CAP_CUSTOM
        val wantEndCustom = endCap == LINE_CAP_CUSTOM
        return (wantStartArrow == (arrowStartMarker != null)) &&
            (wantEndArrow == (arrowEndMarker != null)) &&
            (wantStartCustom == (customStartMarker != null)) &&
            (wantEndCustom == (customEndMarker != null))
    }

    /** 滑块拖动时原地更新线帽 Marker 的颜色/透明度/尺寸。 */
    fun updateCapMarkersAppearance() {
        if (mode != LineMode.CAPS) return
        val tint = parseStrokeTint(strokeColor)
        val opacity = strokeOpacity.coerceIn(0f, 1f)
        val arrowSize = arrowIconSizeForStroke(strokeWeight)

        customStartMarker?.let { marker ->
            marker.setIconColor(tint)
            marker.setIconOpacity(opacity)
        }
        customEndMarker?.let { marker ->
            marker.setIconColor(tint)
            marker.setIconOpacity(opacity)
        }
        arrowStartMarker?.let { marker ->
            marker.setIconColor(tint)
            marker.setIconOpacity(opacity)
            marker.setIconSize(arrowSize)
        }
        arrowEndMarker?.let { marker ->
            marker.setIconColor(tint)
            marker.setIconOpacity(opacity)
            marker.setIconSize(arrowSize)
        }
        requestMapRepaint()
    }

    /** 线帽类型切换或尚未创建 Marker 时，全量重建端点标记。 */
    fun rebuildCapMarkers(points: List<LatLng>) {
        if (mode != LineMode.CAPS) return
        clearCapMarkers()
        if (points.size < 2) return
        mapView?.getMapAsync { map ->
            val engine = map.getOverlayEngine()
            val opacity = strokeOpacity.coerceIn(0f, 1f)
            if (startCap == LINE_CAP_CUSTOM) {
                customStartMarker = addCustomCapMarker(
                    engine, context,
                    anchor = points.first(), neighbor = points[1],
                    strokeColor = strokeColor,
                    strokeOpacity = opacity,
                )
            }
            if (endCap == LINE_CAP_CUSTOM) {
                customEndMarker = addCustomCapMarker(
                    engine, context,
                    anchor = points.last(), neighbor = points[points.size - 2],
                    strokeColor = strokeColor,
                    strokeOpacity = opacity,
                )
            }
            if (startCap == LINE_CAP_ARROW) {
                arrowStartMarker = addArrowCapMarker(
                    engine, context,
                    anchor = points.first(), neighbor = points[1],
                    strokeColor = strokeColor,
                    strokeOpacity = opacity,
                    strokePx = strokeWeight,
                    isStart = true,
                )
            }
            if (endCap == LINE_CAP_ARROW) {
                arrowEndMarker = addArrowCapMarker(
                    engine, context,
                    anchor = points.last(), neighbor = points[points.size - 2],
                    strokeColor = strokeColor,
                    strokeOpacity = opacity,
                    strokePx = strokeWeight,
                    isStart = false,
                )
            }
            map.triggerRepaint()
        }
    }

    LaunchedEffect(
        previewLine,
        latLngs,
        strokeColor,
        strokeWeight,
        strokeOpacity,
        isClosed,
        dashArrayStr,
        glowEffect,
        glowColor,
        glowRadius,
        arrowsEnabled,
        arrowsPattern,
        arrowsColor,
        startCap,
        endCap,
    ) {
        val line = previewLine ?: return@LaunchedEffect
        val points = parseLineLatLngs(latLngs)
        applyPreviewToLine(line)
        if (mode != LineMode.CAPS) {
            clearCapMarkers()
            return@LaunchedEffect
        }
        val hasCapMarker = startCap == LINE_CAP_ARROW || endCap == LINE_CAP_ARROW ||
            startCap == LINE_CAP_CUSTOM || endCap == LINE_CAP_CUSTOM
        if (!hasCapMarker) {
            clearCapMarkers()
            return@LaunchedEffect
        }
        if (points.size < 2) {
            clearCapMarkers()
            capGeometryKey = ""
            return@LaunchedEffect
        }
        val newGeometryKey = "$latLngs|$startCap|$endCap"
        val geometryChanged = newGeometryKey != capGeometryKey
        if (geometryChanged || !capMarkersMatchSelection()) {
            capGeometryKey = newGeometryKey
            rebuildCapMarkers(points)
        } else {
            updateCapMarkersAppearance()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = latLngs,
            onValueChange = { latLngs = it },
            label = { Text(stringResource(R.string.line_vertex_array)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_vertices"),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorPickerField(
                value = strokeColor,
                onValueChange = { strokeColor = it },
                label = stringResource(R.string.line_stroke_color),
                modifier = Modifier.fillMaxWidth(),
                testTag = "line_input_stroke_color",
            )
            DemoNumericSliderField(
                label = "${stringResource(R.string.line_opacity)}: ${formatDemoSliderValue(strokeOpacity)}",
                value = strokeOpacity,
                valueText = formatDemoSliderValue(strokeOpacity),
                onValueChange = { strokeOpacity = it.coerceIn(0f, 1f) },
                onValueTextChange = { value ->
                    value.toFloatOrNull()?.let { strokeOpacity = it.coerceIn(0f, 1f) }
                },
                valueRange = 0f..1f,
                rangeStartLabel = "0",
                rangeEndLabel = "1",
                testTag = "line_input_opacity",
            )
        }

        DemoNumericSliderField(
            label = "${stringResource(R.string.line_width)}: ${formatDemoSliderValue(strokeWeight)}",
            value = strokeWeight,
            valueText = formatDemoSliderValue(strokeWeight),
            onValueChange = { strokeWeight = it.coerceIn(1f, 30f) },
            onValueTextChange = { value ->
                value.toFloatOrNull()?.let { strokeWeight = it.coerceIn(1f, 30f) }
            },
            valueRange = 1f..30f,
            rangeStartLabel = "1",
            rangeEndLabel = "30",
            testTag = "line_input_width",
            modifier = Modifier.padding(bottom = 16.dp),
        )

        OutlinedTextField(
            value = dashArrayStr,
            onValueChange = { dashArrayStr = it },
            label = { Text(stringResource(R.string.line_dash_pattern)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_dash_pattern"),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.isClosed), modifier = Modifier.weight(1f))
            DemoPanelSwitch(
                checked = isClosed,
                onCheckedChange = { isClosed = it },
                modifier = Modifier.testTag("line_switch_closed"),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.line_draggable), modifier = Modifier.weight(1f))
            DemoPanelSwitch(
                checked = draggable,
                onCheckedChange = { draggable = it },
                modifier = Modifier.testTag("line_switch_draggable"),
            )
        }

        if (mode == LineMode.GLOW) {
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
                DemoPanelSwitch(
                    checked = glowEffect,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            if (glowEffect) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorPickerField(
                        value = glowColor,
                        onValueChange = { glowColor = it },
                        label = stringResource(R.string.line_glow_color),
                        supportingText = glowColorError?.let { { Text(it, color = Color.Red) } },
                        isError = glowColorError != null,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "line_input_glow_color",
                    )
                    DemoNumericSliderField(
                        label = stringResource(R.string.line_glow_radius),
                        value = glowRadius.toFloatOrNull() ?: 10f,
                        valueText = glowRadius,
                        onValueChange = { glowRadius = formatDemoSliderValue(it) },
                        onValueTextChange = { glowRadius = it },
                        valueRange = 1f..30f,
                        rangeStartLabel = "1",
                        rangeEndLabel = "30",
                        isError = glowRadiusError != null,
                        errorText = glowRadiusError,
                        testTag = "line_input_glow_radius",
                    )
                }
            }
        }

        if (mode == LineMode.ARROWS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.line_directional_arrows), modifier = Modifier.weight(1f))
                DemoPanelSwitch(
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
                            label = { Text(stringResource(R.string.line_arrow_style)) },
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
                    ColorPickerField(
                        value = arrowsColor,
                        onValueChange = { arrowsColor = it },
                        label = stringResource(R.string.line_arrow_color),
                        supportingText = arrowsColorError?.let { { Text(it, color = Color.Red) } },
                        isError = arrowsColorError != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (mode == LineMode.CAPS) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = startMenuExpanded,
                    onExpandedChange = { startMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = lineCapOptions.find { it.value == startCap }?.label ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.line_cap_start)) },
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = lineCapOptions.find { it.value == endCap }?.label ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.line_cap_end)) },
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
        }

        DemoPanelButton(
            onClick = {
                val pts = parseLineLatLngs(latLngs)
                if (pts.isEmpty()) return@DemoPanelButton
                if (arrowsEnabled && arrowsColorError != null) return@DemoPanelButton
                if (glowEffect && (glowColorError != null || glowRadiusError != null)) return@DemoPanelButton

                val hostView = mapView ?: return@DemoPanelButton
                hostView.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllLines()
                    clearCapMarkers()

                    val dashArray = if (dashArrayStr.isNotEmpty()) {
                        dashArrayStr.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
                    } else null

                    val effectiveStartCap = engineCapForSelection(startCap)
                    val effectiveEndCap = engineCapForSelection(endCap)

                    val options = LineOptions()
                        .withLatLngs(pts)
                        .withStrokeColor(strokeColor)
                        .withStrokeWeight(strokeWeight)
                        .withStrokeOpacity(strokeOpacity)
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
                                linePatternBitmap(hostView.context, arrowsPattern)?.let { bmp ->
                                    withLinePattern(bmp, arrowsPattern)
                                }
                                withMaskColor(arrowsColor.trim())
                            }
                        }

                    if (dashArray != null) {
                        options.withStrokeDashArray(dashArray)
                    }

                    val line = engine.addPolyline(options)
                    onPreviewLineChanged(line)
                    line.addOnDragListener(object : OnOverlayDragListener<Line> {
                        override fun onAnnotationDragStarted(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(line: Line) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(
                                mapView.context,
                                mapView.context.getString(R.string.line_toast_drag_finished, line.id),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    })
                    line.addOnMapClickListener(OnOverlayClickListener<Line> { ln ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onOverlayClick" }
                        Toast.makeText(
                            mapView.context,
                            mapView.context.getString(R.string.line_toast_click, ln.id),
                            Toast.LENGTH_SHORT,
                        ).show()
                        true
                    })
                    line.addOnMapLongClickListener(OnOverlayLongClickListener<Line> { ln ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("LineScreen").d { "onOverlayLongClick" }
                        Toast.makeText(
                            mapView.context,
                            mapView.context.getString(R.string.line_toast_long_click, ln.id),
                            Toast.LENGTH_SHORT,
                        ).show()
                        true
                    })

                    rebuildCapMarkers(pts)
                    map.moveCameraToFitPolyline(pts)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("line_button_draw"),
        ) {
            Text(stringResource(R.string.line_draw))
        }
    }
}

private fun linePatternBitmap(context: Context, imageId: String): Bitmap? =
    when (imageId) {
        LINE_PATTERN_CLASSIC -> loadDrawableBitmap(context, R.drawable.line_classic)
        LINE_PATTERN_CHEVRON -> loadDrawableBitmap(context, R.drawable.line_chevron)
        LINE_PATTERN_TRIANGLE -> loadDrawableBitmap(context, R.drawable.line_triangle)
        LINE_PATTERN_CUSTOM -> loadDrawableBitmap(context, R.drawable.harbor)
        else -> null
    }

private fun loadDrawableBitmap(context: Context, @DrawableRes res: Int): Bitmap? =
    ContextCompat.getDrawable(context, res)?.let(BitmapUtils::getBitmapFromDrawable)

private fun parseStrokeTint(strokeColor: String): Int {
    val c = strokeColor.trim()
    if (c.isEmpty()) return android.graphics.Color.BLACK
    return try {
        android.graphics.Color.parseColor(c)
    } catch (e: IllegalArgumentException) {
        android.graphics.Color.BLACK
    }
}

private fun addCustomCapMarker(
    engine: MapOverlayEngine,
    context: Context,
    anchor: LatLng,
    neighbor: LatLng,
    strokeColor: String,
    strokeOpacity: Float = 1f,
): Marker? {
    val tint = parseStrokeTint(strokeColor)

    val dLat = anchor.latitude - neighbor.latitude
    val dLng = anchor.longitude - neighbor.longitude
    val len = kotlin.math.sqrt(dLat * dLat + dLng * dLng)
    val (offsetX, offsetY) = if (len > 1e-9) {
        val nx = (dLng / len).toFloat()
        val ny = -(dLat / len).toFloat()
        nx * LINE_CAP_CUSTOM_GAP_PX to ny * LINE_CAP_CUSTOM_GAP_PX
    } else {
        0f to 0f
    }

    val opts = MarkerOptions()
        .withLatLng(anchor)
        .withIconResource(R.drawable.star, context, true)
        .withIconColor(tint)
        .withIconOpacity(strokeOpacity.coerceIn(0f, 1f))
        .withIconSize(0.15f)
        .withIconAnchor(MarkerAnchorType.CENTER)
        .withIconOffset(offsetX, offsetY)
        .withClickable(false)
        .withDraggable(false)
        .withVisible(true)
    return engine.addMarker(opts)
}

private fun addArrowCapMarker(
    engine: MapOverlayEngine,
    context: Context,
    anchor: LatLng,
    neighbor: LatLng,
    strokeColor: String,
    strokePx: Float,
    strokeOpacity: Float = 1f,
    isStart: Boolean,
): Marker? {
    val tint = parseStrokeTint(strokeColor)

    val dLat = anchor.latitude - neighbor.latitude
    val dLng = anchor.longitude - neighbor.longitude
    val len = kotlin.math.sqrt(dLat * dLat + dLng * dLng)
    val angleDeg = if (len > 1e-9) {
        Math.toDegrees(kotlin.math.atan2(-dLat, dLng)).toFloat()
    } else 0f

    val drawableRes = if (isStart) R.drawable.arrow_left else R.drawable.arrow_right
    val rotation = if (isStart) angleDeg - 180f else angleDeg
    val iconSize = arrowIconSizeForStroke(strokePx)

    val opts = MarkerOptions()
        .withLatLng(anchor)
        .withIconResource(drawableRes, context, true)
        .withIconColor(tint)
        .withIconOpacity(strokeOpacity.coerceIn(0f, 1f))
        .withIconSize(iconSize)
        .withIconAnchor(MarkerAnchorType.CENTER)
        .withIconRotation(rotation)
        .withClickable(false)
        .withDraggable(false)
        .withVisible(true)
    return engine.addMarker(opts)
}

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

private const val LINE_CAMERA_BOUNDS_PADDING_PX = 100
/** 将边界框按中心放大，使折线在屏幕中约占一半（scale=2 → 线宽/高约为视口 50%）。 */
private const val LINE_CAMERA_BOUNDS_SCALE = 2.0

/** 根据折线点集计算经纬度边界（min/max lat/lng）。 */
private fun buildLatLngBounds(points: List<LatLng>): LatLngBounds? {
    if (points.isEmpty()) return null
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }
    return LatLngBounds.Builder()
        .include(LatLng(minLat, minLng))
        .include(LatLng(maxLat, maxLng))
        .build()
}

/** 以中心为基准放大边界，避免折线贴满全屏。 */
private fun expandLatLngBounds(bounds: LatLngBounds, scaleFactor: Double): LatLngBounds {
    val centerLat = (bounds.northEast.latitude + bounds.southWest.latitude) / 2.0
    val centerLng = (bounds.northEast.longitude + bounds.southWest.longitude) / 2.0
    var latHalfSpan = (bounds.northEast.latitude - bounds.southWest.latitude) / 2.0
    var lngHalfSpan = (bounds.northEast.longitude - bounds.southWest.longitude) / 2.0
    if (latHalfSpan < 1e-6) latHalfSpan = 0.001
    if (lngHalfSpan < 1e-6) lngHalfSpan = 0.001
    latHalfSpan *= scaleFactor
    lngHalfSpan *= scaleFactor
    return LatLngBounds.Builder()
        .include(LatLng(centerLat - latHalfSpan, centerLng - lngHalfSpan))
        .include(LatLng(centerLat + latHalfSpan, centerLng + lngHalfSpan))
        .build()
}

/** 将相机移动至完整包含折线，折线约占视口一半，四周保留 [paddingPx] 像素边距。 */
private fun MaptecMap.moveCameraToFitPolyline(
    points: List<LatLng>,
    paddingPx: Int = LINE_CAMERA_BOUNDS_PADDING_PX,
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14.0))
        return
    }
    val bounds = buildLatLngBounds(points)?.let { expandLatLngBounds(it, LINE_CAMERA_BOUNDS_SCALE) } ?: return
    moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
}

