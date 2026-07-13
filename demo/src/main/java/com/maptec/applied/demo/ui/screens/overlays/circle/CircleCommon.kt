package com.maptec.applied.demo.ui.screens.overlays.circle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.animation.AlphaAnimation
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.AnimationCallbacks
import com.maptec.applied.maps.animation.AnimationSet
import com.maptec.applied.maps.animation.MapAnimationCurve
import com.maptec.applied.maps.animation.RadiusAnimation
import com.maptec.applied.maps.animation.ScanningAnimation
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.utils.BitmapUtils

internal class BasicCircleState {
    var latLng by mutableStateOf("1.4,103.75")
    var color by mutableStateOf("#3F7BF9")
    var radius by mutableStateOf("120")
    var opacity by mutableStateOf("0.5")
    var strokeWidth by mutableStateOf("2")
    var strokeColor by mutableStateOf("#FFFFFF")
    var strokeOpacity by mutableStateOf("0")

    val latLngError: String? get() = validateLatLng(latLng)
    val colorError: String? get() = validateColor(color)
    val radiusError: String? get() = validateRadius(radius)
    val opacityError: String? get() = validateOpacity(opacity)
    val strokeWidthError: String? get() = validateStrokeWidth(strokeWidth)
    val strokeColorError: String? get() = validateColor(strokeColor)
    val strokeOpacityError: String? get() = validateOpacity(strokeOpacity)

    val isValid: Boolean
        get() = latLngError == null && colorError == null && radiusError == null &&
            opacityError == null && strokeWidthError == null &&
            strokeColorError == null && strokeOpacityError == null
}

@Composable
internal fun rememberBasicCircleState(): BasicCircleState = remember { BasicCircleState() }

/** 将 [state] 的基础样式实时应用到已有 [circle]（用于滑块实时预览）。 */
internal fun applyBasicCircleProps(circle: Circle, state: BasicCircleState) {
    if (!state.isValid) return
    parseCircleLatLng(state.latLng)?.let { runCatching { circle.setCenter(it) } }
    state.radius.toDoubleOrNull()?.let { runCatching { circle.setRadius(it) } }
    runCatching { circle.setFillColor(state.color) }
    state.opacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setFillOpacity(it) } }
    runCatching { circle.setStrokeColor(state.strokeColor) }
    state.strokeWidth.toFloatOrNull()?.let { runCatching { circle.setStrokeWeight(it) } }
    state.strokeOpacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setStrokeOpacity(it) } }
}

/**
 * 通用基础参数手动排版（坐标输入 + 半径/填充/描边滑块 + 颜色选择器）。
 *
 * 滑块范围：半径 1~500，填充/描边透明度 0~1，描边宽度 0~20。
 */
@Composable
internal fun CircleCommonSliders(
    state: BasicCircleState,
    radiusLabelRes: Int = R.string.circle_input_radius_pixel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.latLng,
            onValueChange = { state.latLng = it },
            label = { Text(stringResource(R.string.circle_input_latlng_label)) },
            supportingText = state.latLngError?.let { { Text(it, color = Color.Red) } },
            isError = state.latLngError != null,
            modifier = Modifier.fillMaxWidth().testTag("circle_input_latlng"),
        )

        DemoNumericSliderField(
            label = "${stringResource(radiusLabelRes)}: ${state.radius}",
            value = state.radius.toFloatOrNull()?.coerceIn(1f, 500f) ?: 120f,
            valueText = state.radius,
            onValueChange = { state.radius = formatDemoSliderValue(it) },
            onValueTextChange = { state.radius = it },
            valueRange = 1f..500f,
            rangeStartLabel = "1",
            rangeEndLabel = "500",
            isError = state.radiusError != null,
            errorText = state.radiusError,
            testTag = "circle_input_radius",
        )

        ColorPickerField(
            value = state.color,
            onValueChange = { state.color = it },
            label = stringResource(R.string.circle_input_fill_color),
            supportingText = state.colorError?.let { { Text(it, color = Color.Red) } },
            isError = state.colorError != null,
            modifier = Modifier.fillMaxWidth(),
            testTag = "circle_input_color",
        )

        DemoNumericSliderField(
            label = "${stringResource(R.string.circle_input_fill_opacity)}: ${state.opacity}",
            value = state.opacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f,
            valueText = state.opacity,
            onValueChange = { state.opacity = formatDemoSliderValue(it) },
            onValueTextChange = { state.opacity = it },
            valueRange = 0f..1f,
            rangeStartLabel = "0",
            rangeEndLabel = "1",
            isError = state.opacityError != null,
            errorText = state.opacityError,
            testTag = "circle_input_opacity",
        )

        ColorPickerField(
            value = state.strokeColor,
            onValueChange = { state.strokeColor = it },
            label = stringResource(R.string.circle_input_stroke_color),
            supportingText = state.strokeColorError?.let { { Text(it, color = Color.Red) } },
            isError = state.strokeColorError != null,
            modifier = Modifier.fillMaxWidth(),
            testTag = "circle_input_stroke_color",
        )

        DemoNumericSliderField(
            label = "${stringResource(R.string.circle_input_stroke_width)}: ${state.strokeWidth}",
            value = state.strokeWidth.toFloatOrNull()?.coerceIn(0f, 20f) ?: 2f,
            valueText = state.strokeWidth,
            onValueChange = { state.strokeWidth = formatDemoSliderValue(it) },
            onValueTextChange = { state.strokeWidth = it },
            valueRange = 0f..20f,
            rangeStartLabel = "0",
            rangeEndLabel = "20",
            isError = state.strokeWidthError != null,
            errorText = state.strokeWidthError,
            testTag = "circle_input_stroke_width",
        )

        DemoNumericSliderField(
            label = "${stringResource(R.string.circle_input_stroke_opacity)}: ${state.strokeOpacity}",
            value = state.strokeOpacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.8f,
            valueText = state.strokeOpacity,
            onValueChange = { state.strokeOpacity = formatDemoSliderValue(it) },
            onValueTextChange = { state.strokeOpacity = it },
            valueRange = 0f..1f,
            rangeStartLabel = "0",
            rangeEndLabel = "1",
            isError = state.strokeOpacityError != null,
            errorText = state.strokeOpacityError,
            testTag = "circle_input_stroke_opacity",
        )
    }
}

@Composable
internal fun BasicCircleInputs(
    state: BasicCircleState,
    geodesic: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.latLng,
            onValueChange = { state.latLng = it },
            label = { Text(stringResource(R.string.circle_input_latlng_label)) },
            supportingText = state.latLngError?.let { { Text(it, color = Color.Red) } },
            isError = state.latLngError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_latlng")
        )

        OutlinedTextField(
            value = state.radius,
            onValueChange = { state.radius = it },
            label = {
                Text(
                    if (geodesic) stringResource(R.string.circle_input_radius_meter)
                    else stringResource(R.string.circle_input_radius_pixel)
                )
            },
            supportingText = state.radiusError?.let { { Text(it, color = Color.Red) } },
            isError = state.radiusError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_radius")
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = state.color,
                onValueChange = { state.color = it },
                label = { Text(stringResource(R.string.circle_input_fill_color)) },
                supportingText = state.colorError?.let { { Text(it, color = Color.Red) } },
                isError = state.colorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("circle_input_color")
            )
            OutlinedTextField(
                value = state.opacity,
                onValueChange = { state.opacity = it },
                label = { Text(stringResource(R.string.circle_input_fill_opacity)) },
                supportingText = state.opacityError?.let { { Text(it, color = Color.Red) } },
                isError = state.opacityError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("circle_input_opacity")
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = state.strokeColor,
                onValueChange = { state.strokeColor = it },
                label = { Text(stringResource(R.string.circle_input_stroke_color)) },
                supportingText = state.strokeColorError?.let { { Text(it, color = Color.Red) } },
                isError = state.strokeColorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("circle_input_stroke_color")
            )
            OutlinedTextField(
                value = state.strokeWidth,
                onValueChange = { state.strokeWidth = it },
                label = { Text(stringResource(R.string.circle_input_stroke_width)) },
                supportingText = state.strokeWidthError?.let { { Text(it, color = Color.Red) } },
                isError = state.strokeWidthError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("circle_input_stroke_width")
            )
        }

        OutlinedTextField(
            value = state.strokeOpacity,
            onValueChange = { state.strokeOpacity = it },
            label = { Text(stringResource(R.string.circle_input_stroke_opacity)) },
            supportingText = state.strokeOpacityError?.let { { Text(it, color = Color.Red) } },
            isError = state.strokeOpacityError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_stroke_opacity")
        )
    }
}

internal fun BasicCircleState.applyTo(
    options: CircleOptions,
    geodesic: Boolean = false,
    draggable: Boolean = false,
): CircleOptions {
    val center = parseCircleLatLng(latLng) ?: return options
    return options
        .withCenter(center)
        .withRadius(radius.toDouble())
        .withFillColor(color)
        .withFillOpacity(opacity.toFloat())
        .withStrokeColor(strokeColor)
        .withStrokeWeight(strokeWidth.toFloat())
        .withStrokeOpacity(strokeOpacity.toFloat())
        .withGeodesic(geodesic)
        .withDraggable(draggable)
}

@Composable
internal fun CircleScaffold(
    defaultOptions: (() -> CircleOptions?)? = null,
    onDefaultCircleAdded: (Circle, MapOverlayEngine) -> Unit = { _, _ -> },
    sheetContent: @Composable (mapView: MapView?, mapStyle: Style?) -> Unit,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    var defaultDrawn by remember { mutableStateOf(false) }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                sheetContent(mapView, mapStyle)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle { style ->
                        mapStyle = style
                        if (!defaultDrawn && defaultOptions != null) {
                            defaultOptions.invoke()?.let { options ->
                                defaultDrawn = true
                                val engine = map.getOverlayEngine()
                                engine.deleteAllCircles()
                                val circle = engine.addCircle(options)
                                attachDebugListeners(circle, view.context)
                                onDefaultCircleAdded(circle, engine)
                                options.center?.let { center ->
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(center, 15.0),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    )
}

@Composable
internal fun CirclePanelColumn(content: @Composable () -> Unit) {
    content()
}

/**
 * 通用「绘制」按钮：清空旧圆 → 用 [buildOptions] 构造选项 → 落点居中 → 关闭配置面板。
 *
 * @param buildOptions 返回完整 [CircleOptions]；若为 null 跳过绘制（用于校验失败提示）。
 * @param onCircleAdded 绘制成功后回调，便于注册动画或额外监听。
 */
@Composable
internal fun DrawCircleButton(
    state: BasicCircleState,
    mapView: MapView?,
    enabled: Boolean = state.isValid,
    buildOptions: () -> CircleOptions? = { state.applyTo(CircleOptions()) },
    onCircleAdded: (Circle, MapOverlayEngine) -> Unit = { _, _ -> },
) {
    val configPanelController = LocalDemoConfigPanelController.current
    val context = LocalContext.current
    Button(
        onClick = {
            val center = parseCircleLatLng(state.latLng) ?: return@Button
            val options = buildOptions() ?: return@Button
            val view = mapView ?: return@Button
            view.getMapAsync { map ->
                val engine = map.getOverlayEngine()
                engine.deleteAllCircles()
                val circle = engine.addCircle(options)
                attachDebugListeners(circle, context)
                onCircleAdded(circle, engine)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(center.latitude, center.longitude),
                        15.0,
                    ),
                )
            }
            configPanelController.close()
        },
        enabled = enabled && mapView != null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("circle_draw_button"),
    ) {
        Text(stringResource(R.string.circle_draw))
    }
}

@SuppressLint("StringFormatMatches")
internal fun attachDebugListeners(circle: Circle, context: Context) {
    circle.addOnMapClickListener(OnOverlayClickListener<Circle> { c ->
        LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onOverlayClick id=${c.id}" }
        Toast.makeText(context, context.getString(R.string.circle_toast_click_overlay, c.id), Toast.LENGTH_SHORT).show()
        true
    })
    circle.addOnMapLongClickListener(OnOverlayLongClickListener<Circle> { c ->
        LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onOverlayLongClick id=${c.id}" }
        Toast.makeText(context, context.getString(R.string.circle_toast_long_click_overlay, c.id), Toast.LENGTH_SHORT).show()
        true
    })
}

internal fun applyScanStyleToCircleOptions(
    options: CircleOptions,
    scanEnabled: Boolean,
    fillColor: String,
    scanSectorColorInput: String,
    scanSectorAngleInput: String,
    scanBaseImageBitmap: android.graphics.Bitmap?,
    scanBaseImageId: String = "scan_base_image",
): CircleOptions {
    if (!scanEnabled) return options
    val sectorAngle = scanSectorAngleInput.toFloatOrNull()?.takeIf { it > 0f } ?: 60f
    val sectorColor = scanSectorColorInput.trim().ifEmpty { fillColor.trim() }
    var result = options
        .withScanEnabled(true)
        .withScanSectorAngle(sectorAngle)
        .withScanSectorColor(sectorColor)
        .withScanRotationAngle(0f)
    if (scanBaseImageBitmap != null) {
        result = result.withScanBaseImage(scanBaseImageBitmap, scanBaseImageId)
    }
    return result
}

internal fun buildScanRotationAnimation(scanSpeed: String): Animation? {
    val durationMs = scanSpeed.toLongOrNull()?.takeIf { it > 0L } ?: return null
    return ScanningAnimation(durationMs).apply {
        setListener(circleAnimationCallbacks("scan"))
    }
}

internal fun buildPulseAnimation(
    min: Double,
    max: Double,
    expandDurationMs: Long,
    label: String,
    onWaveComplete: ((Boolean) -> Unit)? = null,
): Animation {
    val fadeDurationMs = (expandDurationMs * 0.55f).toLong().coerceIn(1L, expandDurationMs)
    val radiusAnim = RadiusAnimation(min, max).apply { setDurationMs(expandDurationMs) }
    val baseListener = circleAnimationCallbacks(label)
    return AnimationSet().apply {
        setDurationMs(expandDurationMs)
        // Native 只读根节点 getCurve()；必须把 curve 设到 AnimationSet。
        setCurve(MapAnimationCurve.LINEAR)
        setRepeatCount(0)
        addAnimation(radiusAnim)
        addAnimation(AlphaAnimation(0.35f, 0f).apply { setDurationMs(fadeDurationMs) })
        setListener(
            object : AnimationCallbacks {
                override fun onStart() = baseListener.onStart()
                override fun onUpdate(progress: Float) = baseListener.onUpdate(progress)
                override fun onPause() = baseListener.onPause()
                override fun onResume() = baseListener.onResume()
                override fun onComplete(finished: Boolean) {
                    baseListener.onComplete(finished)
                    onWaveComplete?.invoke(finished)
                }
                override fun onCancel() = baseListener.onCancel()
            },
        )
    }
}

internal fun pulseLayerCircleOptions(
    center: LatLng,
    startRadius: Double,
    fillColor: String,
    fillOpacity: Float,
    strokeColor: String,
    strokeWeight: Float,
    strokeOpacity: Float,
    geodesic: Boolean,
): CircleOptions = CircleOptions()
    .withCenter(center)
    .withRadius(startRadius)
    .withFillColor(fillColor)
    .withFillOpacity(fillOpacity)
    .withStrokeColor(strokeColor)
    .withStrokeWeight(strokeWeight)
    .withStrokeOpacity(strokeOpacity * 0f)
    .withGeodesic(geodesic)
    .withDraggable(false)

internal fun circleAnimationCallbacks(label: String): AnimationCallbacks =
    object : AnimationCallbacks {
        override fun onStart() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onStart" }
        }
        override fun onUpdate(progress: Float) {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onUpdate: $progress" }
        }
        override fun onPause() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onPause" }
        }
        override fun onResume() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onResume" }
        }
        override fun onComplete(finished: Boolean) {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onComplete finished=$finished" }
        }
        override fun onCancel() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onCancel" }
        }
    }

internal fun loadDrawableAsBitmap(context: Context, drawableRes: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return null
    return drawableToBitmap(drawable)
}

/**
 * 更好的 Drawable 转 Bitmap 方法，支持 VectorDrawable
 */
private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    // 如果已经是 BitmapDrawable，直接返回
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    // 获取 drawable 的固有尺寸
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 256
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 256

    // 创建 Bitmap 和 Canvas
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 设置 drawable 的边界并绘制
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

internal fun parseCircleLatLng(input: String): LatLng? = try {
    val content = input.trim()
    if (content.isEmpty()) null
    else {
        val coords = content.split(",").map { it.trim().toDouble() }
        if (coords.size >= 2) LatLng(coords[0], coords[1]) else null
    }
} catch (e: Exception) {
    LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").e { "Failed to parse coordinates: ${e.message}" }
    null
}

internal fun validateLatLng(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入中心点坐标"
    val point = parseCircleLatLng(trimmed) ?: return "格式错误，示例：1.4, 103.75"
    return when {
        point.latitude < -90 || point.latitude > 90 -> "纬度应在 -90～90 之间"
        point.longitude < -180 || point.longitude > 180 -> "经度应在 -180～180 之间"
        else -> null
    }
}

internal fun validateColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入颜色"
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(trimmed)) null else "格式错误，示例：#00A63E 或 #RRGGBB"
}

internal fun validateRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return when {
        value <= 0 -> "半径必须大于 0"
        value > 1_000_000 -> "半径不宜超过 1000000 "
        else -> null
    }
}

internal fun validateOpacity(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入透明度（0～1）"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..1f) null else "透明度应在 0～1 之间"
}

internal fun validateStrokeWidth(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value >= 0f) null else "描边宽度应大于等于 0"
}

internal fun validateGlowRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入发光半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 1f..30f) null else "发光半径应在 1～30 之间"
}

internal fun validateAnimationLoop(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入动画周期"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 500f..10000f) null else "动画周期应在 500～10000 之间"
}

internal fun validateScanAngle(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入扇形角度"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..90f) null else "扇形角度应在 0～90 之间"
}
