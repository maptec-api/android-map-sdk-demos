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
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitchRow
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
import com.maptec.applied.maps.overlay.line.LineGradientStop
import com.maptec.applied.maps.overlay.line.LineJoinType
import com.maptec.applied.maps.overlay.line.LineOptions

private data class LineDropdownOption(val value: String, val label: String)

/** line-pattern 样式图 id，与 drawable 资源一一对应；bitmap 经 withLinePattern/setLinePattern 在 overlay 内注册。 */
private const val LINE_PATTERN_CLASSIC = "line_classic"
private const val LINE_PATTERN_CHEVRON = "line_chevron"
private const val LINE_PATTERN_TRIANGLE = "line_triangle"
private const val LINE_PATTERN_CUSTOM = "line_custom"

internal enum class LineMode {
    BASIC,
    GLOW,
    ARROWS,
    CAPS,
    GRADIENT,
}

private data class GradientStopUi(
    val position: Float,
    val color: String,
)

/** 与各折线 Demo 页共用默认路径，避免渐变页 fit 相机范围过大、瓦片加载偏慢。 */
private const val DEFAULT_LINE_PATH =
    "[[1.360879,103.732578],[1.370879,103.742578],[1.380879,103.732578]]"

private const val DEFAULT_GRADIENT_STROKE_WEIGHT = 8f
private const val DEFAULT_LINE_STROKE_WEIGHT = 5f

private fun defaultGradientStops(): List<GradientStopUi> = listOf(
    GradientStopUi(0f, "#E63946"),
    GradientStopUi(0.5f, "#F4A261"),
    GradientStopUi(1f, "#2A9D8F"),
)

private fun buildLineGradientStops(stops: List<GradientStopUi>): List<LineGradientStop> =
    stops.map { LineGradientStop(it.position, it.color) }

private fun validateGradientStopsUi(stops: List<GradientStopUi>): String? {
    if (stops.size < 2) return "至少需要 2 个渐变节点"
    val sorted = stops.sortedBy { it.position }
    if (sorted.first().position != 0f) return "首节点 position 须为 0"
    if (sorted.last().position != 1f) return "末节点 position 须为 1"
    var previous = -1f
    for (stop in sorted) {
        if (stop.position < 0f || stop.position > 1f) return "position 须在 [0, 1]"
        if (validateColor(stop.color) != null) return "渐变颜色格式无效"
        if (stop.position <= previous) return "position 须严格递增"
        previous = stop.position
    }
    return null
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
                            val points = parseLineLatLngs(DEFAULT_LINE_PATH)
                            val options = LineOptions()
                                .withLatLngs(points)
                                .withStrokeColor("#00A63E")
                                .withStrokeWeight(
                                    if (mode == LineMode.GRADIENT) DEFAULT_GRADIENT_STROKE_WEIGHT
                                    else DEFAULT_LINE_STROKE_WEIGHT,
                                )
                                .withStrokeOpacity(1f)
                            when (mode) {
                                LineMode.GRADIENT -> {
                                    val gradientStops = buildLineGradientStops(defaultGradientStops())
                                    options
                                        .withGradientEnabled(true)
                                        .withGradientStops(gradientStops)
                                }
                                LineMode.BASIC -> options.withLineJoin(LineJoinType.ROUND)
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
    var latLngs by remember(mode) { mutableStateOf(DEFAULT_LINE_PATH) }
    var strokeColor by remember { mutableStateOf("#00A63E") }
    var strokeWeight by remember(mode) {
        mutableFloatStateOf(
            if (mode == LineMode.GRADIENT) DEFAULT_GRADIENT_STROKE_WEIGHT else DEFAULT_LINE_STROKE_WEIGHT,
        )
    }
    var strokeOpacity by remember { mutableFloatStateOf(1f) }
    var isClosed by remember { mutableStateOf(false) }
    var draggable by remember { mutableStateOf(false) }

    var dashArrayStr by remember { mutableStateOf("") }

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
    var arrowSpacingCustom by remember { mutableStateOf(false) }
    var arrowSpacing by remember { mutableFloatStateOf(5f) }
    /** 滑块拖动中的展示值；提交到 [arrowSpacing] 仅在松手时，避免列表滚动误触滑块。 */
    var arrowSpacingSliderUi by remember { mutableFloatStateOf(5f) }

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
        LineDropdownOption(LineCapType.ARROW, "Arrow"),
    )

    var lineJoin by remember(mode) {
        mutableStateOf(if (mode == LineMode.BASIC) LineJoinType.ROUND else LineJoinType.NONE)
    }
    var miterLimit by remember(mode) { mutableFloatStateOf(2f) }
    var joinMenuExpanded by remember { mutableStateOf(false) }
    val lineJoinOptions = listOf(
        LineDropdownOption(LineJoinType.ROUND, "Round"),
        LineDropdownOption(LineJoinType.BEVEL, "Bevel"),
        LineDropdownOption(LineJoinType.MITER, "Miter"),
    )
    val miterLimitError = remember(miterLimit, lineJoin) {
        if (lineJoin != LineJoinType.MITER) null
        else if (miterLimit >= 1f) null else "斜接限制须 ≥ 1"
    }

    var gradientEnabled by remember(mode) { mutableStateOf(mode == LineMode.GRADIENT) }
    var gradientStartColor by remember { mutableStateOf("#E63946") }
    var gradientMidColor by remember { mutableStateOf("#F4A261") }
    var gradientEndColor by remember { mutableStateOf("#2A9D8F") }
    var gradientMidPosition by remember { mutableFloatStateOf(0.5f) }
    val gradientStopsUi = remember(gradientStartColor, gradientMidColor, gradientEndColor, gradientMidPosition) {
        listOf(
            GradientStopUi(0f, gradientStartColor),
            GradientStopUi(gradientMidPosition.coerceIn(0.01f, 0.99f), gradientMidColor),
            GradientStopUi(1f, gradientEndColor),
        )
    }
    val gradientStopsError = remember(gradientStopsUi, gradientEnabled) {
        if (!gradientEnabled) null else validateGradientStopsUi(gradientStopsUi)
    }

    val context = LocalContext.current

    fun requestMapRepaint() {
        mapView?.getMapAsync { map -> map.triggerRepaint() }
    }

    fun buildPanelLineOptions(hostContext: Context, points: List<LatLng>): LineOptions {
        val options = LineOptions()
            .withLatLngs(points)
            .withStrokeColor(strokeColor)
            .withStrokeWeight(strokeWeight)
            .withStrokeOpacity(strokeOpacity)
            .withIsClosed(isClosed)
            .withDraggable(draggable)
            .withGlowEffect(glowEffect)
            .withGlowColor(glowColor)
            .withGlowRadius(glowRadius.toFloat())
            .withLineCapStart(startCap)
            .withLineCapEnd(endCap)
            .apply {
                if (arrowsEnabled) {
                    linePatternBitmap(hostContext, arrowsPattern)?.let { bmp ->
                        withLinePattern(bmp, arrowsPattern)
                    }
                    withMaskColor(arrowsColor.trim())
                    if (arrowSpacingCustom) {
                        withArrowSpacing(arrowSpacing.coerceAtLeast(1f))
                    }
                }
                if (lineJoin.isNotEmpty()) {
                    withLineJoin(lineJoin)
                    if (lineJoin == LineJoinType.MITER) {
                        withMiterLimit(miterLimit.coerceAtLeast(1f))
                    }
                }
                if (mode == LineMode.GRADIENT) {
                    withGradientEnabled(gradientEnabled)
                    if (gradientEnabled && gradientStopsError == null) {
                        withGradientStops(buildLineGradientStops(gradientStopsUi))
                    }
                }
            }
        parseLineDashArray(dashArrayStr)?.let { options.withStrokeDashArray(it) }
        return options
    }

    fun syncGradientPreviewLine() {
        if (gradientStopsError != null) return
        val hostView = mapView ?: return
        hostView.getMapAsync { map ->
            val pts = parseLineLatLngs(latLngs)
            if (pts.size < 2) return@getMapAsync
            val engine = map.getOverlayEngine()
            engine.deleteAllLines()
            val line = engine.addPolyline(buildPanelLineOptions(hostView.context, pts))
            onPreviewLineChanged(line)
            map.triggerRepaint()
        }
    }

    fun applyPreviewToLine(line: Line) {
        val points = parseLineLatLngs(latLngs)
        if (points.size >= 2) line.setLatLngs(points)
        runCatching { line.setStrokeColor(strokeColor) }
        line.setStrokeWeight(strokeWeight)
        line.setStrokeOpacity(strokeOpacity.coerceIn(0f, 1f))
        line.setClosed(isClosed)
        runCatching { line.setDraggable(draggable) }
        parseLineDashArray(dashArrayStr)?.let { line.setStrokeDashArray(it) }
            ?: line.setStrokeDashArray(floatArrayOf())
        line.setGlowEffectEnabled(glowEffect)
        runCatching { line.setGlowColor(glowColor) }
        glowRadius.toFloatOrNull()?.takeIf { it in 1f..30f }?.let(line::setGlowRadius)
        if (arrowsEnabled) {
            linePatternBitmap(context, arrowsPattern)?.let { bmp ->
                line.setLinePattern(bmp, arrowsPattern)
            }
            runCatching { line.setMaskColor(arrowsColor) }
            if (arrowSpacingCustom) {
                line.setArrowSpacing(arrowSpacing.coerceAtLeast(1f))
            } else {
                line.setArrowSpacing(null)
            }
        }
        line.setLineCapStart(startCap)
        line.setLineCapEnd(endCap)
        if (lineJoin.isNotEmpty()) {
            line.setLineJoin(lineJoin)
            if (lineJoin == LineJoinType.MITER && miterLimitError == null) {
                line.setMiterLimit(miterLimit.coerceAtLeast(1f))
            }
        }
        line.setGradientEnabled(gradientEnabled)
        if (gradientEnabled && gradientStopsError == null) {
            runCatching {
                line.setGradientStops(buildLineGradientStops(gradientStopsUi))
            }.onFailure {
                Toast.makeText(context, it.message ?: "渐变参数无效", Toast.LENGTH_SHORT).show()
            }
        }
        requestMapRepaint()
    }

    LaunchedEffect(arrowSpacingCustom) {
        if (arrowSpacingCustom) {
            arrowSpacingSliderUi = arrowSpacing
        }
    }

    LaunchedEffect(
        mapView,
        mode,
        gradientEnabled,
        latLngs,
        strokeColor,
        strokeWeight,
        strokeOpacity,
        isClosed,
        dashArrayStr,
        draggable,
        gradientStartColor,
        gradientMidColor,
        gradientEndColor,
        gradientMidPosition,
        startCap,
        endCap,
        gradientStopsError,
    ) {
        if (mode != LineMode.GRADIENT || !gradientEnabled) return@LaunchedEffect
        syncGradientPreviewLine()
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
        arrowSpacingCustom,
        arrowSpacing,
        startCap,
        endCap,
        lineJoin,
        miterLimit,
        gradientEnabled,
        gradientStartColor,
        gradientMidColor,
        gradientEndColor,
        gradientMidPosition,
        draggable,
        mode,
    ) {
        if (mode == LineMode.GRADIENT && gradientEnabled) return@LaunchedEffect
        val line = previewLine ?: return@LaunchedEffect
        applyPreviewToLine(line)
    }

    LaunchedEffect(previewLine, mapView) {
        val line = previewLine ?: return@LaunchedEffect
        val hostView = mapView ?: return@LaunchedEffect
        attachLineOverlayListeners(line, hostView)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = latLngs,
            onValueChange = { latLngs = it },
            label = { Text(stringResource(R.string.line_vertex_array)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("line_input_vertices"),
        )

        if (mode == LineMode.GRADIENT) {
            DemoPanelSwitchRow(
                label = stringResource(R.string.line_gradient_enabled),
                checked = gradientEnabled,
                onCheckedChange = { gradientEnabled = it },
                modifier = Modifier.padding(bottom = 8.dp),
                testTag = "line_switch_gradient",
            )
            if (!gradientEnabled) {
                ColorPickerField(
                    value = strokeColor,
                    onValueChange = { strokeColor = it },
                    label = stringResource(R.string.line_stroke_color),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    testTag = "line_input_stroke_color",
                )
            }
            if (gradientEnabled) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorPickerField(
                        value = gradientStartColor,
                        onValueChange = { gradientStartColor = it },
                        label = stringResource(R.string.line_gradient_start_color),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "line_input_gradient_start_color",
                    )
                    ColorPickerField(
                        value = gradientMidColor,
                        onValueChange = { gradientMidColor = it },
                        label = stringResource(R.string.line_gradient_mid_color),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "line_input_gradient_mid_color",
                    )
                    DemoNumericSliderField(
                        label = "${stringResource(R.string.line_gradient_mid_position)}: ${
                            formatDemoSliderValue(gradientMidPosition)
                        }",
                        value = gradientMidPosition,
                        valueText = formatDemoSliderValue(gradientMidPosition),
                        onValueChange = { gradientMidPosition = it.coerceIn(0.01f, 0.99f) },
                        onValueTextChange = { value ->
                            value.toFloatOrNull()?.let { gradientMidPosition = it.coerceIn(0.01f, 0.99f) }
                        },
                        valueRange = 0.01f..0.99f,
                        rangeStartLabel = "0.01",
                        rangeEndLabel = "0.99",
                        testTag = "line_input_gradient_mid_position",
                    )
                    ColorPickerField(
                        value = gradientEndColor,
                        onValueChange = { gradientEndColor = it },
                        label = stringResource(R.string.line_gradient_end_color),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "line_input_gradient_end_color",
                    )
                    if (gradientStopsError != null) {
                        Text(gradientStopsError, color = Color.Red)
                    }
                    DemoPanelButton(
                        onClick = {
                            val reversed = parseLineLatLngs(latLngs).asReversed()
                            if (reversed.size >= 2) {
                                latLngs = reversed.joinToString(prefix = "[", postfix = "]") { point ->
                                    "[${point.latitude},${point.longitude}]"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("line_button_reverse_path"),
                    ) {
                        Text(stringResource(R.string.line_gradient_reverse_path))
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (mode != LineMode.GRADIENT) {
                ColorPickerField(
                    value = strokeColor,
                    onValueChange = { strokeColor = it },
                    label = stringResource(R.string.line_stroke_color),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "line_input_stroke_color",
                )
            }
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

        if (mode == LineMode.BASIC) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = joinMenuExpanded,
                    onExpandedChange = { joinMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = lineJoinOptions.find { it.value == lineJoin }?.label ?: "Round",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.line_join)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = joinMenuExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("line_dropdown_line_join"),
                    )
                    ExposedDropdownMenu(
                        expanded = joinMenuExpanded,
                        onDismissRequest = { joinMenuExpanded = false },
                    ) {
                        lineJoinOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    lineJoin = opt.value
                                    joinMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (lineJoin == LineJoinType.MITER) {
                    DemoNumericSliderField(
                        label = "${stringResource(R.string.line_miter_limit)}: ${formatDemoSliderValue(miterLimit)}",
                        value = miterLimit,
                        valueText = formatDemoSliderValue(miterLimit),
                        onValueChange = { miterLimit = it.coerceIn(1f, 10f) },
                        onValueTextChange = { value ->
                            value.toFloatOrNull()?.let { miterLimit = it.coerceIn(1f, 10f) }
                        },
                        valueRange = 1f..10f,
                        rangeStartLabel = "1",
                        rangeEndLabel = "10",
                        isError = miterLimitError != null,
                        errorText = miterLimitError,
                        testTag = "line_input_miter_limit",
                    )
                }
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.line_arrow_spacing_custom),
                            modifier = Modifier.weight(1f),
                        )
                        DemoPanelSwitch(
                            checked = arrowSpacingCustom,
                            onCheckedChange = { arrowSpacingCustom = it },
                            modifier = Modifier.testTag("line_switch_arrow_spacing_custom"),
                        )
                    }
                    if (arrowSpacingCustom) {
                        DemoNumericSliderField(
                            label = "${stringResource(R.string.line_arrow_spacing)}: ${
                                formatDemoSliderValue(arrowSpacingSliderUi)
                            }",
                            value = arrowSpacingSliderUi,
                            valueText = formatDemoSliderValue(arrowSpacingSliderUi),
                            onValueChange = { arrowSpacingSliderUi = it.coerceIn(1f, 30f) },
                            onValueChangeFinished = {
                                arrowSpacing = arrowSpacingSliderUi.coerceIn(1f, 30f)
                            },
                            onValueTextChange = { value ->
                                value.toFloatOrNull()?.let { parsed ->
                                    val coerced = parsed.coerceIn(1f, 30f)
                                    arrowSpacing = coerced
                                    arrowSpacingSliderUi = coerced
                                }
                            },
                            valueRange = 1f..30f,
                            rangeStartLabel = "1",
                            rangeEndLabel = "30",
                            testTag = "line_input_arrow_spacing",
                        )
                    }
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
                if (lineJoin == LineJoinType.MITER && miterLimitError != null) return@DemoPanelButton
                if (gradientEnabled && gradientStopsError != null) return@DemoPanelButton

                val hostView = mapView ?: return@DemoPanelButton
                hostView.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllLines()

                    val line = engine.addPolyline(buildPanelLineOptions(hostView.context, pts))
                    onPreviewLineChanged(line)

                    map.moveCameraToFitPolyline(pts)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("line_button_draw"),
        ) {
            Text(stringResource(R.string.line_draw))
        }
    }
}

private fun attachLineOverlayListeners(line: Line, mapView: MapView) {
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

/** 至少 2 个有效数值才视为完整虚线参数，避免输入过程中把线刷没。 */
private fun parseLineDashArray(input: String): FloatArray? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val values = trimmed.split(",").mapNotNull { it.trim().toFloatOrNull() }
    if (values.size < 2) return null
    return values.toFloatArray()
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

