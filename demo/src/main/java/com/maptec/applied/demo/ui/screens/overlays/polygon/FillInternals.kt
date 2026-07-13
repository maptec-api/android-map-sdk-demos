package com.maptec.applied.demo.ui.screens.overlays.polygon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.google.gson.JsonParser
import androidx.annotation.StringRes
import com.maptec.applied.demo.R
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions as MapFillOptions
import com.maptec.applied.utils.BitmapUtils

private data class FillDropdownOption(val value: String, @StringRes val labelRes: Int)

private const val DEFAULT_FILL_COORDINATES = "[[1.323600,103.814100;1.323200,103.817100;1.321500,103.819600;1.318800,103.819800;1.316200,103.818800;1.313200,103.819400;1.310500,103.818300;1.307700,103.817300;1.305700,103.815500;1.306300,103.813700;1.308500,103.812400;1.311600,103.812000;1.315500,103.812600;1.319000,103.813100;1.321800,103.813500;1.323600,103.814100]]"

internal enum class FillMode {
    BASIC,
    PATTERN,
    TRANSLATE,
    INTERACTION,
}

private val fillPatternDropdownOptions = listOf(
    FillDropdownOption(MapFillOptions.PATTERN_SOLID, R.string.fill_pattern_solid),
    FillDropdownOption(MapFillOptions.PATTERN_HATCH, R.string.fill_pattern_hatch),
    FillDropdownOption(MapFillOptions.PATTERN_CROSS, R.string.fill_pattern_cross),
    FillDropdownOption(MapFillOptions.PATTERN_DOT, R.string.fill_pattern_dot),
    FillDropdownOption(MapFillOptions.PATTERN_IMAGE, R.string.fill_pattern_image),
)

private val blendModeDropdownOptions = listOf(
    FillDropdownOption(MapFillOptions.BLEND_MODE_NORMAL, R.string.fill_blend_normal),
    FillDropdownOption(MapFillOptions.BLEND_MODE_MULTIPLY, R.string.fill_blend_multiply),
    FillDropdownOption(MapFillOptions.BLEND_MODE_SCREEN, R.string.fill_blend_screen),
    FillDropdownOption(MapFillOptions.BLEND_MODE_OVERLAY, R.string.fill_blend_overlay),
)

private val fillPatternImageIdOptions = listOf(
    FillDropdownOption("garden", R.string.fill_pattern_image_garden),
    FillDropdownOption("harbor", R.string.fill_pattern_image_harbor),
)

private val fillTranslateAnchorDropdownOptions = listOf(
    FillDropdownOption("map", R.string.fill_anchor_map),
    FillDropdownOption("viewport", R.string.fill_anchor_viewport),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FillScreen(
    mode: FillMode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var defaultFillAdded by remember { mutableStateOf(false) }
    var previewFill by remember { mutableStateOf<Fill?>(null) }
    val configPanelController = LocalDemoConfigPanelController.current
    val collapseSheet: () -> Unit = { configPanelController.close() }

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                FillDetailPanel(mapView, collapseSheet, mode, previewFill) { previewFill = it }
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle {
                        if (!defaultFillAdded) {
                            defaultFillAdded = true
                            val rings = parseCoordinates(DEFAULT_FILL_COORDINATES).orEmpty()
                            if (rings.isNotEmpty()) {
                                val options = MapFillOptions()
                                    .withLatLngs(rings)
                                    .withFillColor("#2F80ED")
                                    .withFillOpacity(0.28f)
                                    .withStrokeColor("#0B5CAD")
                                    .withStrokeWeight(3f)
                                    .withStrokeOpacity(0.9f)
                                    .withFillPatternMode(MapFillOptions.PATTERN_SOLID)
                                    .withBlendMode(MapFillOptions.BLEND_MODE_NORMAL)
                                    .withFillAntialias(true)
                                when (mode) {
                                    FillMode.BASIC -> Unit
                                    FillMode.PATTERN -> options.withFillPatternMode(MapFillOptions.PATTERN_HATCH)
                                    FillMode.TRANSLATE -> options
                                        .withTranslate(PointF(16f, 16f), "map")
                                    FillMode.INTERACTION -> options
                                        .withDraggable(true)
                                        .withBlendMode(MapFillOptions.BLEND_MODE_MULTIPLY)
                                }
                                previewFill = map.getOverlayEngine().addPolygon(options)
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(calculateCenter(rings.first()), 14.5),
                                )
                            }
                        }
                    }
                },
            )
        },
    )
}

private fun fillPatternBitmap(context: Context, imageId: String): Bitmap? =
    when (imageId) {
        "garden" -> loadDrawableBitmap(context, R.drawable.garden)
        "harbor" -> loadDrawableBitmap(context, R.drawable.harbor)
        else -> null
    }

private fun loadDrawableBitmap(context: Context, @DrawableRes res: Int): Bitmap? =
    ContextCompat.getDrawable(context, res)?.let(BitmapUtils::getBitmapFromDrawable)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FillDetailPanel(
    mapView: MapView?,
    onDrawComplete: () -> Unit = {},
    mode: FillMode = FillMode.BASIC,
    previewFill: Fill? = null,
    onPreviewFillChanged: (Fill) -> Unit = {},
) {
    val mContext = LocalContext.current
    var coordinates by remember { mutableStateOf(DEFAULT_FILL_COORDINATES) }
    var fillColor by remember { mutableStateOf("#2F80ED") }
    var fillOpacity by remember { mutableStateOf("0.28") }
    var fillOutlineColor by remember { mutableStateOf("#0B5CAD") }
    var outlineLineWidth by remember { mutableStateOf("3") }
    var strokeOpacity by remember { mutableStateOf("0.9") }
    var strokeDashArray by remember(mode) { mutableStateOf(if (mode == FillMode.INTERACTION) "4,2" else "") }
    var fillPattern by remember(mode) { mutableStateOf(if (mode == FillMode.PATTERN) MapFillOptions.PATTERN_HATCH else MapFillOptions.PATTERN_SOLID) }
    var fillPatternImageUrl by remember { mutableStateOf("garden") }
    var fillTranslateX by remember(mode) { mutableStateOf(if (mode == FillMode.TRANSLATE) "16" else "") }
    var fillTranslateY by remember(mode) { mutableStateOf(if (mode == FillMode.TRANSLATE) "16" else "") }
    var fillTranslateAnchor by remember(mode) { mutableStateOf(if (mode == FillMode.TRANSLATE) "map" else "") }
    var fillAntialias by remember { mutableStateOf(true) }
    var isDraggable by remember(mode) { mutableStateOf(mode == FillMode.INTERACTION) }
    var blendMode by remember(mode) { mutableStateOf(if (mode == FillMode.INTERACTION) MapFillOptions.BLEND_MODE_MULTIPLY else MapFillOptions.BLEND_MODE_NORMAL) }
    var customDataJson by remember { mutableStateOf("") }
    var fillPatternMenuExpanded by remember { mutableStateOf(false) }
    var fillPatternImageMenuExpanded by remember { mutableStateOf(false) }
    var blendModeMenuExpanded by remember { mutableStateOf(false) }
    var fillTranslateAnchorMenuExpanded by remember { mutableStateOf(false) }

    val coordinatesError = remember(coordinates) { validateCoordinates(coordinates) }
    val colorError = remember(fillColor) { validateColor(fillColor) }
    val opacityError = remember(fillOpacity) { validateOpacity(fillOpacity) }
    val outlineColorError = remember(fillOutlineColor) { validateColor(fillOutlineColor) }
    val outlineWidthError = remember(outlineLineWidth) { validateOutlineWidth(outlineLineWidth) }
    val strokeOpacityError = remember(strokeOpacity) { validateOpacity(strokeOpacity) }
    val strokeDashError = remember(strokeDashArray) { validateStrokeDashArray(strokeDashArray) }
    val translatePairError = remember(fillTranslateX, fillTranslateY) {
        validateTranslatePair(fillTranslateX, fillTranslateY)
    }
    val isValid = coordinatesError == null && colorError == null && opacityError == null &&
        outlineColorError == null && outlineWidthError == null &&
        strokeOpacityError == null && strokeDashError == null &&
        translatePairError == null

    val fillOpacityValue = fillOpacity.toFloatOrNull() ?: 0f
    val outlineLineWidthValue = outlineLineWidth.toFloatOrNull() ?: 0f
    val strokeOpacityValue = strokeOpacity.toFloatOrNull() ?: 0f
    val fillTranslateXValue = fillTranslateX.toFloatOrNull()
    val fillTranslateYValue = fillTranslateY.toFloatOrNull()

    // 记录已应用的填充类型，用于检测“填充类型”切换后需要重建 Fill 对象。
    var lastAppliedPattern by remember { mutableStateOf<String?>(null) }

    fun buildFillOptions(rings: List<List<LatLng>>): MapFillOptions {
        var options = MapFillOptions()
            .withLatLngs(rings)
            .withFillColor(fillColor)
            .withFillOpacity(fillOpacityValue.coerceIn(0f, 1f))
            .withStrokeColor(fillOutlineColor)
            .withStrokeWeight(outlineLineWidthValue.coerceIn(0f, 20f))
            .withStrokeOpacity(strokeOpacityValue.coerceIn(0f, 1f))
            .withFillPatternMode(fillPattern)
            .withBlendMode(blendMode)
            .withFillAntialias(fillAntialias)
            .withDraggable(isDraggable)
        parseStrokeDashArray(strokeDashArray)?.let { options = options.withStrokeDashArray(it) }
        if (fillPattern == MapFillOptions.PATTERN_IMAGE && fillPatternImageUrl.isNotBlank()) {
            fillPatternBitmap(mContext, fillPatternImageUrl.trim())?.let { bmp ->
                options = options.withFillPatternId(bmp, fillPatternImageUrl.trim())
            }
        }
        if (fillTranslateXValue != null && fillTranslateYValue != null && fillTranslateAnchor.isNotBlank()) {
            options = options.withTranslate(PointF(fillTranslateXValue, fillTranslateYValue), fillTranslateAnchor)
        }
        return options
    }

    LaunchedEffect(
        previewFill,
        coordinates,
        fillColor,
        fillOpacityValue,
        fillOutlineColor,
        outlineLineWidthValue,
        strokeOpacityValue,
        strokeDashArray,
        fillPattern,
        fillPatternImageUrl,
        fillTranslateXValue,
        fillTranslateYValue,
        fillTranslateAnchor,
        fillAntialias,
        blendMode,
    ) {
        val fill = previewFill ?: return@LaunchedEffect

        // 填充类型切换：部分引擎不支持在已有对象上动态切换 pattern，需销毁旧对象后重建。
        if (mode == FillMode.PATTERN && lastAppliedPattern != null && lastAppliedPattern != fillPattern) {
            val rings = parseCoordinates(coordinates)?.takeIf { it.isNotEmpty() }
            if (rings != null) {
                mapView?.getMapAsync { map ->
                    val engine = map.getOverlayEngine()
                    engine.deleteAllFills()
                    val newFill = engine.addPolygon(buildFillOptions(rings))
                    onPreviewFillChanged(newFill)
                    map.triggerRepaint()
                }
            }
            lastAppliedPattern = fillPattern
            return@LaunchedEffect
        }

        parseCoordinates(coordinates)?.takeIf { it.isNotEmpty() }?.let(fill::setLatLngs)
        runCatching { fill.setFillColor(fillColor) }
        fillOpacityValue.takeIf { it in 0f..1f }?.let(fill::setFillOpacity)
        runCatching { fill.setStrokeColor(fillOutlineColor) }
        outlineLineWidthValue.takeIf { it in 0f..20f }?.let(fill::setStrokeWeight)
        strokeOpacityValue.takeIf { it in 0f..1f }?.let(fill::setStrokeOpacity)
        if (mode == FillMode.INTERACTION) {
            parseStrokeDashArray(strokeDashArray)?.let { dashArray ->
                runCatching { fill.setStrokeDashArray(dashArray) }
            }
            runCatching { fill.setFillAntialias(fillAntialias) }
            runCatching { fill.setBlendMode(blendMode) }
        }
        if (mode == FillMode.PATTERN) {
            runCatching { fill.setFillPatternMode(fillPattern) }
            if (fillPattern == MapFillOptions.PATTERN_IMAGE && fillPatternImageUrl.isNotBlank()) {
                val bmp = fillPatternBitmap(mContext, fillPatternImageUrl.trim())
                if (bmp != null) {
                    runCatching { fill.setFillPatternId(bmp, fillPatternImageUrl.trim()) }
                } else {
                    LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen")
                        .e { "Fill pattern bitmap is null for id=$fillPatternImageUrl" }
                }
            }
            lastAppliedPattern = fillPattern
        }
        if (mode == FillMode.TRANSLATE) {
            if (fillTranslateXValue != null && fillTranslateYValue != null && fillTranslateAnchor.isNotBlank()) {
                runCatching {
                    fill.setTranslate(PointF(fillTranslateXValue, fillTranslateYValue), fillTranslateAnchor)
                }
            }
        }
        mapView?.getMapAsync { it.triggerRepaint() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = coordinates,
            onValueChange = { coordinates = it },
            label = { Text(stringResource(R.string.fill_input_coordinates_label)) },
            supportingText = coordinatesError?.let { { Text(it, color = Color.Red) } }
                ?: { Text(stringResource(R.string.fill_coordinates_hint), color = Color.Gray) },
            isError = coordinatesError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("fill_input_coordinates"),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorPickerField(
                value = fillColor,
                onValueChange = { fillColor = it },
                label = stringResource(R.string.fill_color_label),
                supportingText = colorError?.let { { Text(it, color = Color.Red) } },
                isError = colorError != null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "fill_input_color",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.fill_opacity_label),
                value = fillOpacity.toFloatOrNull() ?: 0.35f,
                valueText = fillOpacity,
                onValueChange = { fillOpacity = formatDemoSliderValue(it) },
                onValueTextChange = { fillOpacity = it },
                valueRange = 0f..1f,
                rangeStartLabel = "0",
                rangeEndLabel = "1",
                isError = opacityError != null,
                errorText = opacityError,
                testTag = "fill_input_opacity",
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorPickerField(
                value = fillOutlineColor,
                onValueChange = { fillOutlineColor = it },
                label = stringResource(R.string.fill_stroke_color_label),
                supportingText = outlineColorError?.let { { Text(it, color = Color.Red) } },
                isError = outlineColorError != null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "fill_input_outline_color",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.fill_stroke_width_label),
                value = outlineLineWidth.toFloatOrNull() ?: 2f,
                valueText = outlineLineWidth,
                onValueChange = { outlineLineWidth = formatDemoSliderValue(it) },
                onValueTextChange = { outlineLineWidth = it },
                valueRange = 0f..20f,
                rangeStartLabel = "0",
                rangeEndLabel = "20",
                isError = outlineWidthError != null,
                errorText = outlineWidthError,
                testTag = "fill_input_outline_width",
            )
        }

        DemoNumericSliderField(
            label = stringResource(R.string.fill_stroke_opacity_label),
            value = strokeOpacity.toFloatOrNull() ?: 1f,
            valueText = strokeOpacity,
            onValueChange = { strokeOpacity = formatDemoSliderValue(it) },
            onValueTextChange = { strokeOpacity = it },
            valueRange = 0f..1f,
            rangeStartLabel = "0",
            rangeEndLabel = "1",
            isError = strokeOpacityError != null,
            errorText = strokeOpacityError,
            testTag = "fill_input_stroke_opacity",
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (mode == FillMode.INTERACTION) {
            OutlinedTextField(
                value = strokeDashArray,
                onValueChange = { strokeDashArray = it },
                label = { Text(stringResource(R.string.fill_stroke_dasharray_label)) },
                supportingText = strokeDashError?.let { { Text(it, color = Color.Red) } }
                    ?: { Text(stringResource(R.string.fill_stroke_dash_hint), color = Color.Gray) },
                isError = strokeDashError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("fill_input_dash_array"),
            )
        }

        if (mode == FillMode.PATTERN) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = fillPatternMenuExpanded,
                    onExpandedChange = { fillPatternMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = fillPatternDropdownOptions.find { it.value == fillPattern }?.let { stringResource(it.labelRes) }
                            ?: fillPattern,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.fill_pattern_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fillPatternMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("fill_dropdown_pattern"),
                    )
                    ExposedDropdownMenu(
                        expanded = fillPatternMenuExpanded,
                        onDismissRequest = { fillPatternMenuExpanded = false },
                    ) {
                        fillPatternDropdownOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    fillPattern = option.value
                                    fillPatternMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = fillPatternImageMenuExpanded,
                    onExpandedChange = { fillPatternImageMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = fillPatternImageIdOptions.find { it.value == fillPatternImageUrl }?.let { stringResource(it.labelRes) }
                            ?: fillPatternImageUrl,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.fill_pattern_image_label)) },
                        supportingText = { Text(stringResource(R.string.fill_pattern_image_hint), color = Color.Gray) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fillPatternImageMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("fill_dropdown_pattern_image"),
                    )
                    ExposedDropdownMenu(
                        expanded = fillPatternImageMenuExpanded,
                        onDismissRequest = { fillPatternImageMenuExpanded = false },
                    ) {
                        fillPatternImageIdOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    fillPatternImageUrl = option.value
                                    fillPatternImageMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        if (mode == FillMode.TRANSLATE) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = fillTranslateX,
                    onValueChange = { fillTranslateX = it },
                    label = { Text(stringResource(R.string.fill_translate_x_label)) },
                    supportingText = translatePairError?.let { { Text(it, color = Color.Red) } },
                    isError = translatePairError != null,
                    modifier = Modifier.fillMaxWidth().testTag("fill_input_translate_x"),
                )
                OutlinedTextField(
                    value = fillTranslateY,
                    onValueChange = { fillTranslateY = it },
                    label = { Text(stringResource(R.string.fill_translate_y_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("fill_input_translate_y"),
                )
            }
            ExposedDropdownMenuBox(
                expanded = fillTranslateAnchorMenuExpanded,
                onExpandedChange = { fillTranslateAnchorMenuExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                OutlinedTextField(
                    value = fillTranslateAnchorDropdownOptions.find { it.value == fillTranslateAnchor }?.let { stringResource(it.labelRes) }
                        ?: stringResource(fillTranslateAnchorDropdownOptions.first().labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.fill_translate_anchor_label)) },
                    supportingText = { Text(stringResource(R.string.fill_translate_anchor_hint), color = Color.Gray) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fillTranslateAnchorMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("fill_dropdown_translate_anchor"),
                )
                ExposedDropdownMenu(
                    expanded = fillTranslateAnchorMenuExpanded,
                    onDismissRequest = { fillTranslateAnchorMenuExpanded = false },
                ) {
                    fillTranslateAnchorDropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                fillTranslateAnchor = option.value
                                fillTranslateAnchorMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (mode == FillMode.INTERACTION) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FillSwitchRow(
                    label = stringResource(R.string.fill_antialias_label),
                    checked = fillAntialias,
                    onCheckedChange = { fillAntialias = it },
                    modifier = Modifier.testTag("fill_switch_antialias"),
                )
                FillSwitchRow(
                    label = stringResource(R.string.fill_draggable_label),
                    checked = isDraggable,
                    onCheckedChange = { isDraggable = it },
                    modifier = Modifier.testTag("fill_switch_draggable"),
                )
            }
            ExposedDropdownMenuBox(
                expanded = blendModeMenuExpanded,
                onExpandedChange = { blendModeMenuExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                OutlinedTextField(
                    value = blendModeDropdownOptions.find { it.value == blendMode }?.let { stringResource(it.labelRes) }
                        ?: blendMode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.fill_blend_mode_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = blendModeMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("fill_dropdown_blend_mode"),
                )
                ExposedDropdownMenu(
                    expanded = blendModeMenuExpanded,
                    onDismissRequest = { blendModeMenuExpanded = false },
                ) {
                    blendModeDropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                blendMode = option.value
                                blendModeMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val clipboard = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val pastedText = clip.getItemAt(0).text?.toString()
                    if (!pastedText.isNullOrBlank()) {
                        coordinates = pastedText
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("fill_paste_button"),
        ) {
            Text(stringResource(R.string.fill_paste_button))
        }
        Spacer(modifier = Modifier.height(8.dp))

        DemoPanelButton(
            onClick = {
                val rings = parseCoordinates(coordinates) ?: return@DemoPanelButton

                mapView?.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllFills()
                    var fillOptions = buildFillOptions(rings)
                    if (customDataJson.isNotBlank()) {
                        runCatching {
                            fillOptions = fillOptions.withData(JsonParser.parseString(customDataJson.trim()))
                        }
                    }
                    val fill = engine.addPolygon(fillOptions)
                    onPreviewFillChanged(fill)
                    lastAppliedPattern = fillPattern
                    fill.addOnDragListener(object : OnOverlayDragListener<Fill> {
                        override fun onAnnotationDragStarted(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(
                                mContext,
                                mContext.getString(R.string.fill_toast_drag_finished, fill.id),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    })
                    fill.addOnMapClickListener(OnOverlayClickListener<Fill> { f ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onOverlayClick" }
                        Toast.makeText(
                            mContext,
                            mContext.getString(R.string.fill_toast_click, f.id),
                            Toast.LENGTH_SHORT,
                        ).show()
                        true
                    })
                    fill.addOnMapLongClickListener(OnOverlayLongClickListener<Fill> { f ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onOverlayLongClick" }
                        Toast.makeText(
                            mContext,
                            mContext.getString(R.string.fill_toast_long_click, f.id),
                            Toast.LENGTH_SHORT,
                        ).show()
                        true
                    })

                    val center = calculateCenter(rings.first())
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(center.latitude, center.longitude),
                            14.5,
                        ),
                    )
                    onDrawComplete()
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("fill_draw_button"),
            enabled = isValid,
        ) {
            Text(stringResource(R.string.fill_draw_button))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FillSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        DemoPanelSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
        )
    }
}

private fun validateCoordinates(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入坐标"
    val rings = parseCoordinates(trimmed) ?: return "格式错误，边线须首尾闭合，示例：[[1.4,103.75;1.5,103.75;1.5,103.85;1.4,103.85;1.4,103.75],[...]]"
    if (rings.isEmpty()) return "至少需要外边线"
    val outer = rings.first()
    if (outer.size < 3) return "外边线至少需要3个点"
    rings.drop(1).forEachIndexed { index, ring ->
        if (ring.size < 3) return "内边线${index + 1}至少需要3个点"
    }
    return null
}

private fun validateColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入颜色"
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(trimmed)) null else "格式错误，示例：#00A63E 或 #RRGGBB"
}

private fun validateOpacity(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入透明度（0～1）"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..1f) null else "透明度应在 0～1 之间"
}

private fun validateOutlineWidth(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入边框宽度"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..20f) null else "边框宽度应在 0～20 之间（与 FillOptions 一致）"
}

private fun validateStrokeDashArray(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "格式：逗号分隔数字，如 2,4"
    parts.forEach {
        if (it.toFloatOrNull() == null) return "虚线数组须为有效数字"
    }
    return null
}

private fun parseStrokeDashArray(input: String): Array<Float>? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val floats = parts.mapNotNull { it.toFloatOrNull() }
    if (floats.size != parts.size) return null
    return floats.toTypedArray()
}

private fun validateTranslatePair(x: String, y: String): String? {
    val xs = x.trim()
    val ys = y.trim()
    if (xs.isEmpty() && ys.isEmpty()) return null
    if (xs.isEmpty() || ys.isEmpty()) return "平移 X、Y 须同时填写或同时留空"
    if (xs.toFloatOrNull() == null || ys.toFloatOrNull() == null) return "平移须为有效数字"
    return null
}

private fun parseRing(ringStr: String): List<LatLng>? {
    val trimmed = ringStr.trim()
    if (trimmed.isEmpty()) return null
    val points = trimmed.split(";").mapNotNull { pointStr ->
        val coords = pointStr.trim().split(",").map { it.trim().toDoubleOrNull() }
        if (coords.size >= 2 && coords[0] != null && coords[1] != null) {
            val lat = coords[0]!!
            val lng = coords[1]!!
            if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                LatLng(lat, lng)
            } else null
        } else null
    }
    if (points.isEmpty()) return null
    return closeRing(points)
}

private fun closeRing(ring: List<LatLng>): List<LatLng> {
    if (ring.size < 2) return ring
    val first = ring.first()
    val last = ring.last()
    return if (first.latitude == last.latitude && first.longitude == last.longitude) {
        ring
    } else {
        ring + first
    }
}

private fun parseCoordinates(input: String): List<List<LatLng>>? {
    return try {
        var content = input.trim()
        if (content.isEmpty()) return null
        if (content.startsWith("[[")) content = content.drop(2)
        if (content.endsWith("]]")) content = content.dropLast(2)
        content = content.trim()
        if (content.isEmpty()) return null
        val ringStrings = content.split("],[")
        val rings = ringStrings.mapNotNull { parseRing(it.trim()) }
        if (rings.isEmpty()) return null
        rings
    } catch (e: Exception) {
        LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").e { "Failed to parse coordinates: ${e.message}" }
        null
    }
}

private fun calculateCenter(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return LatLng(0.0, 0.0)
    val avgLat = points.map { it.latitude }.average()
    val avgLng = points.map { it.longitude }.average()
    return LatLng(avgLat, avgLng)
}

