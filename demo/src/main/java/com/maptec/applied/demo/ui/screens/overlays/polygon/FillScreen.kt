package com.maptec.applied.demo.ui.screens.overlays.polygon

import android.content.Context
import android.graphics.PointF
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.ui.screens.map.MapScreen
import com.maptec.applied.javabase.log.LoggerFactory
import kotlinx.coroutines.launch
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.google.gson.JsonParser
import com.maptec.applied.demo.R
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions as MapFillOptions
import com.maptec.applied.utils.BitmapUtils

private data class FillDropdownOption(val value: String, @StringRes val labelRes: Int)

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

/** pattern 为 image 时，与 addPresetIcon 注册的图片 ID 一致 */
private val fillPatternImageIdOptions = listOf(
    FillDropdownOption("garden", R.string.fill_pattern_image_garden),
    FillDropdownOption("harbor", R.string.fill_pattern_image_harbor),
)

/** fill-translate-anchor：留空表示不向 FillOptions 传入该属性 */
private val fillTranslateAnchorDropdownOptions = listOf(
    FillDropdownOption("map", R.string.fill_anchor_map),
    FillDropdownOption("viewport", R.string.fill_anchor_viewport),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillScreen() {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val collapseSheet: () -> Unit = {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // 默认展开 bottom sheet
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = {
            Box(modifier = Modifier.testTag("fill_sheet_drag_handle")) {
                BottomSheetDefaults.DragHandle()
            }
        },
        sheetContent = { 
            FillDetailPanel(mapView, collapseSheet)
        },
        content = { padding ->
            MapScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onMapReady = { view, mapLibreMap ->
                    mapView = view
                    mapLibreMap.getStyle { style ->
                        addPresetIcon(style,context,R.drawable.garden, "garden")
                        addPresetIcon(style,context,R.drawable.harbor, "harbor")
                    }
                }
            )
        }
    )
}

fun addPresetIcon(style: Style, context: Context, drawableRes: Int, iconId: String) {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return
    val bitmap = BitmapUtils.getBitmapFromDrawable(drawable) ?: return
    style.addImage(iconId, bitmap, false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillDetailPanel(
    mapView: MapView?,
    onDrawComplete: () -> Unit = {},
) {
    val mContext = LocalContext.current
    // 多边形坐标：[[外边线],[内边线1],[内边线2],...]，每边线须首尾闭合（首点=末点），格式 lat,lng;lat,lng;...（默认值为原范围缩小一半）
    var coordinates by remember {
        mutableStateOf(
            "[[1.425,103.775;1.475,103.775;1.475,103.825;1.425,103.825;1.425,103.775],[1.4575,103.79;1.4725,103.79;1.4725,103.81;1.4575,103.81;1.4575,103.79]]"
        )
    }
    var fillColor by remember { mutableStateOf("#00A63E") }
    var fillOpacity by remember { mutableStateOf("0.5") }
    var fillOutlineColor by remember { mutableStateOf("#FF0000") }
    var outlineLineWidth by remember { mutableStateOf("5") }
    var strokeOpacity by remember { mutableStateOf("1") }
    var strokeDashArray by remember { mutableStateOf("") }
    var fillPattern by remember { mutableStateOf(MapFillOptions.PATTERN_SOLID) }
    var fillPatternImageUrl by remember { mutableStateOf("garden") }
    var fillTranslateX by remember { mutableStateOf("") }
    var fillTranslateY by remember { mutableStateOf("") }
    var fillTranslateAnchor by remember { mutableStateOf("") }
    var fillAntialias by remember { mutableStateOf(true) }
    var isDraggable by remember { mutableStateOf(false) }
    var blendMode by remember { mutableStateOf(MapFillOptions.BLEND_MODE_NORMAL) }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 坐标输入：[[外边线],[内边线1],[内边线2],...]，每边线为 lat,lng;lat,lng;...
        OutlinedTextField(
            value = coordinates,
            onValueChange = { coordinates = it },
            label = { Text(stringResource(R.string.fill_input_coordinates_label)) },
            supportingText = coordinatesError?.let { { Text(it, color = Color.Red) } }
                ?: { Text("格式：[[外边线],[内边线],...]，边线首尾须闭合，lat,lng;lat,lng;...", color = Color.Gray) },
            isError = coordinatesError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("fill_input_coordinates")
        )

        // 填充颜色和填充透明度一行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = fillColor,
                onValueChange = { fillColor = it },
                label = { Text(stringResource(R.string.fill_color_label)) },
                supportingText = colorError?.let { { Text(it, color = Color.Red) } },
                isError = colorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("fill_input_color")
            )
            OutlinedTextField(
                value = fillOpacity,
                onValueChange = { fillOpacity = it },
                label = { Text(stringResource(R.string.fill_opacity_label)) },
                supportingText = opacityError?.let { { Text(it, color = Color.Red) } },
                isError = opacityError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("fill_input_opacity")
            )
        }

        // 描边颜色与边框宽度一行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = fillOutlineColor,
                onValueChange = { fillOutlineColor = it },
                label = { Text(stringResource(R.string.fill_stroke_color_label)) },
                supportingText = outlineColorError?.let { { Text(it, color = Color.Red) } },
                isError = outlineColorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("fill_input_outline_color")
            )
            OutlinedTextField(
                value = outlineLineWidth,
                onValueChange = { outlineLineWidth = it },
                label = { Text(stringResource(R.string.fill_stroke_width_label)) },
                supportingText = outlineWidthError?.let { { Text(it, color = Color.Red) } },
                isError = outlineWidthError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("fill_input_outline_width")
            )
        }

        // 描边透明度
        OutlinedTextField(
            value = strokeOpacity,
            onValueChange = { strokeOpacity = it },
            label = { Text(stringResource(R.string.fill_stroke_opacity_label)) },
            supportingText = strokeOpacityError?.let { { Text(it, color = Color.Red) } },
            isError = strokeOpacityError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("fill_input_stroke_opacity")
        )

        // 虚线描边：逗号分隔，留空为实线
        OutlinedTextField(
            value = strokeDashArray,
            onValueChange = { strokeDashArray = it },
            label = { Text(stringResource(R.string.fill_stroke_dasharray_label)) },
            supportingText = strokeDashError?.let { { Text(it, color = Color.Red) } }
                ?: { Text("可选，逗号分隔像素段长，如 2,4", color = Color.Gray) },
            isError = strokeDashError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("fill_input_dash_array")
        )

        // 填充图案与图案图 URL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = fillPatternMenuExpanded,
                onExpandedChange = { fillPatternMenuExpanded = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
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
                        .testTag("fill_dropdown_pattern")
                )
                ExposedDropdownMenu(
                    expanded = fillPatternMenuExpanded,
                    onDismissRequest = { fillPatternMenuExpanded = false }
                ) {
                    fillPatternDropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                fillPattern = option.value
                                fillPatternMenuExpanded = false
                            }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = fillPatternImageMenuExpanded,
                onExpandedChange = { fillPatternImageMenuExpanded = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                OutlinedTextField(
                    value = fillPatternImageIdOptions.find { it.value == fillPatternImageUrl }?.let { stringResource(it.labelRes) }
                        ?: fillPatternImageUrl,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.fill_pattern_image_label)) },
                    supportingText = { Text("pattern为image时使用，demo中的本地图片", color = Color.Gray) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fillPatternImageMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("fill_dropdown_pattern_image")
                )
                ExposedDropdownMenu(
                    expanded = fillPatternImageMenuExpanded,
                    onDismissRequest = { fillPatternImageMenuExpanded = false }
                ) {
                    fillPatternImageIdOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                fillPatternImageUrl = option.value
                                fillPatternImageMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 填充平移（像素）与锚点
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = fillTranslateX,
                onValueChange = { fillTranslateX = it },
                label = { Text(stringResource(R.string.fill_translate_x_label)) },
                supportingText = translatePairError?.let { { Text(it, color = Color.Red) } },
                isError = translatePairError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("fill_input_translate_x")
            )
            OutlinedTextField(
                value = fillTranslateY,
                onValueChange = { fillTranslateY = it },
                label = { Text(stringResource(R.string.fill_translate_y_label)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("fill_input_translate_y")
            )
        }
        ExposedDropdownMenuBox(
            expanded = fillTranslateAnchorMenuExpanded,
            onExpandedChange = { fillTranslateAnchorMenuExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = fillTranslateAnchorDropdownOptions.find { it.value == fillTranslateAnchor }?.let { stringResource(it.labelRes) }
                    ?: stringResource(fillTranslateAnchorDropdownOptions.first().labelRes),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.fill_translate_anchor_label)) },
                supportingText = { Text("地理平面和屏幕显示平面", color = Color.Gray) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = fillTranslateAnchorMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("fill_dropdown_translate_anchor")
            )
            ExposedDropdownMenu(
                expanded = fillTranslateAnchorMenuExpanded,
                onDismissRequest = { fillTranslateAnchorMenuExpanded = false }
            ) {
                fillTranslateAnchorDropdownOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            fillTranslateAnchor = option.value
                            fillTranslateAnchorMenuExpanded = false
                        }
                    )
                }
            }
        }

        // 抗锯齿、混合模式
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.fill_antialias_label), modifier = Modifier.weight(1f))
            Switch(
                checked = fillAntialias,
                onCheckedChange = { fillAntialias = it },
                modifier = Modifier.testTag("fill_switch_antialias")
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(stringResource(R.string.fill_draggable_label), modifier = Modifier.weight(1f))
            Switch(
                checked = isDraggable,
                onCheckedChange = { isDraggable = it },
                modifier = Modifier.testTag("fill_switch_draggable")
            )
        }
        ExposedDropdownMenuBox(
            expanded = blendModeMenuExpanded,
            onExpandedChange = { blendModeMenuExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
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
                    .testTag("fill_dropdown_blend_mode")
            )
            ExposedDropdownMenu(
                expanded = blendModeMenuExpanded,
                onDismissRequest = { blendModeMenuExpanded = false }
            ) {
                blendModeDropdownOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            blendMode = option.value
                            blendModeMenuExpanded = false
                        }
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fill_paste_button")
        ) {
            Text(stringResource(R.string.fill_paste_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val rings = parseCoordinates(coordinates) ?: return@Button

                mapView?.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllFills()
                    var fillOptions = MapFillOptions()
                        .withLatLngs(rings)
                        .withFillColor(fillColor)
                        .withFillOpacity(fillOpacity.toFloat())
                        .withStrokeColor(fillOutlineColor)
                        .withStrokeWeight(outlineLineWidth.toFloat())
                        .withStrokeOpacity(strokeOpacity.toFloat())
                        .withFillPatternMode(fillPattern)
                        .withBlendMode(blendMode)
                        .withFillAntialias(fillAntialias)
                        .withDraggable(isDraggable)
                    parseStrokeDashArray(strokeDashArray)?.let {
                        fillOptions = fillOptions.withStrokeDashArray(it)
                    }
                    if (fillPatternImageUrl.isNotBlank()) {
                        fillOptions = fillOptions.withFillPatternId(fillPatternImageUrl.trim())
                    }
                    val tx = fillTranslateX.trim()
                    val ty = fillTranslateY.trim()
                    if (tx.isNotEmpty() && ty.isNotEmpty()) {
                        val x = tx.toFloatOrNull()
                        val y = ty.toFloatOrNull()
                        if (x != null && y != null) {
                            fillOptions = fillOptions.withFillTranslate(PointF(x, y))
                        }
                    }
                    if (fillTranslateAnchor.isNotBlank()) {
                        fillOptions = fillOptions.withFillTranslateAnchor(fillTranslateAnchor)
                    }
                    if (customDataJson.isNotBlank()) {
                        runCatching {
                            fillOptions = fillOptions.withData(JsonParser.parseString(customDataJson.trim()))
                        }
                    }
                    val fill = engine.addPolygon(fillOptions)
                    fill.addOnDragListener(object : OnOverlayDragListener<Fill> {
                        override fun onAnnotationDragStarted(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(fill: Fill) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(mContext, "拖拽结束: ${fill.id}", Toast.LENGTH_SHORT).show()
                        }
                    })
                    fill.addOnMapClickListener(OnOverlayClickListener<Fill> { f ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onOverlayClick" }
                        Toast.makeText(mContext, "点击了面: ${f.id}", Toast.LENGTH_SHORT).show()
                        true
                    })
                    fill.addOnMapLongClickListener(OnOverlayLongClickListener<Fill> { f ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("FillScreen").d { "onOverlayLongClick" }
                        Toast.makeText(mContext, "长按了面: ${f.id}", Toast.LENGTH_SHORT).show()
                        true
                    })

                    // 移动相机到外环中心
                    val center = calculateCenter(rings.first())
                    map.moveCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(
                                latitude = center.latitude,
                                longitude = center.longitude
                            )
                        )
                    )
                    onDrawComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fill_draw_button"),
            enabled = isValid
        ) {
            Text(stringResource(R.string.fill_draw_button))
        }

        Spacer(modifier = Modifier.height(8.dp))
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

private fun validateOptionalJson(input: String): String? {
    val t = input.trim()
    if (t.isBlank()) return null
    return runCatching {
        JsonParser.parseString(t)
        null
    }.getOrElse { "不是合法 JSON" }
}

/** 解析单条边线：lat,lng;lat,lng;...。GeoJSON 要求环首尾闭合，未闭合时自动补首点。 */
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

/** GeoJSON Polygon 要求每个环首尾闭合，否则缩放/瓦片边界时填充异常。未闭合则补首点。 */
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

/**
 * 解析坐标字符串为 [[外边线],[内边线]]。
 * 格式：[[lat,lng;lat,lng;...],[lat,lng;lat,lng;...]]，内边线可选。
 */
private fun parseCoordinates(input: String): List<List<LatLng>>? {
    return try {
        var content = input.trim()
        if (content.isEmpty()) return null
        // 去掉首尾 [[ 和 ]]
        if (content.startsWith("[[")) content = content.drop(2)
        if (content.endsWith("]]")) content = content.dropLast(2)
        content = content.trim()
        if (content.isEmpty()) return null
        // 按 ],[ 拆成外边线和内边线
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

@Preview
@Composable
fun FillScreenPreview() {
    FillScreen()
}
