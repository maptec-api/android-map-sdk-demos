package com.maptec.applied.demo.ui.screens.overlays.heatmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.heatmap.HeatmapColorStop
import com.maptec.applied.maps.overlay.heatmap.HeatmapGradient
import com.maptec.applied.maps.overlay.heatmap.HeatmapPoint
import com.maptec.applied.maps.overlay.heatmap.HeatmapTile
import com.maptec.applied.maps.overlay.heatmap.HeatmapTileOptions
import kotlin.math.cos
import kotlin.math.sin
import org.json.JSONObject

internal const val HEATMAP_PROVIDER_ASSET = "heatmap_provider.json"

/** 与 JS 示例 `center: [103.82, 1.315]` 对齐。 */
internal val DEMO_HEATMAP_CENTER = LatLng(1.315, 103.82)
private const val DEMO_HEATMAP_ZOOM = 11.15

/** 与 JS `minimumWeight` / `maximumWeight` 对齐（逐点权重归一化区间）。 */
internal const val DEMO_HEATMAP_MIN_WEIGHT = 0f
internal const val DEMO_HEATMAP_MAX_WEIGHT = 8f

private data class HeatmapAnchor(
    val lng: Double,
    val lat: Double,
    val baseWeight: Double,
    val pointCount: Int,
)

/** 与 JS `createHeatmapPoints` 的 anchors 一致。 */
private val DEMO_HEATMAP_ANCHORS = listOf(
    HeatmapAnchor(103.8519, 1.2903, 7.2, 18),
    HeatmapAnchor(103.8318, 1.3048, 6.4, 16),
    HeatmapAnchor(103.8198, 1.3521, 5.7, 15),
    HeatmapAnchor(103.7764, 1.2966, 5.2, 15),
    HeatmapAnchor(103.7955, 1.3606, 4.8, 14),
    HeatmapAnchor(103.8750, 1.3250, 4.5, 14),
)

internal class HeatmapDemoState {
    var radius by mutableStateOf("34")
    var weight by mutableStateOf("1")
    var intensity by mutableStateOf("0.82")
    var opacity by mutableStateOf("0.82")
    var minZoom by mutableStateOf("0")
    var maxZoom by mutableStateOf("22")

    val radiusError: String? get() = validateRadius(radius)
    val weightError: String? get() = validateWeight(weight)
    val intensityError: String? get() = validateIntensity(intensity)
    val opacityError: String? get() = validateOpacity(opacity)
    val minZoomError: String? get() = validateMinZoom(minZoom, maxZoom)
    val maxZoomError: String? get() = validateMaxZoom(maxZoom, minZoom)

    val isValid: Boolean
        get() = radiusError == null && weightError == null && intensityError == null &&
            opacityError == null && minZoomError == null && maxZoomError == null
}

@Composable
internal fun rememberHeatmapDemoState(): HeatmapDemoState = remember { HeatmapDemoState() }

/**
 * 与 JS `Maptec.HeatmapOverlay` 示例 `createHeatmapPoints()` 一致：
 * 6 个锚点、螺旋分布采样，逐点 weight 落在 [minimumWeight, maximumWeight] 映射前的原始区间。
 */
internal fun generateDemoHeatmapPoints(): List<HeatmapPoint> {
    val points = ArrayList<HeatmapPoint>(92)
    DEMO_HEATMAP_ANCHORS.forEachIndexed { anchorIndex, anchor ->
        for (pointIndex in 0 until anchor.pointCount) {
            val ring = 0.35 + (pointIndex % 6) * 0.14
            val angle = pointIndex * 2.399 + anchorIndex * 0.72
            val longitudeOffset = cos(angle) * 0.0135 * ring
            val latitudeOffset = sin(angle) * 0.0102 * ring
            val weightDrop = (pointIndex % 7) * 0.34 + (pointIndex / 7) * 0.14
            val rawWeight = maxOf(1.2, anchor.baseWeight - weightDrop).toFloat()
            val normalizedWeight = ((rawWeight - DEMO_HEATMAP_MIN_WEIGHT) / DEMO_HEATMAP_MAX_WEIGHT)
                .coerceIn(0f, 1f)
            points.add(
                HeatmapPoint(
                    LatLng(anchor.lat + latitudeOffset, anchor.lng + longitudeOffset),
                    normalizedWeight,
                ),
            )
        }
    }
    return points
}

/** 与 JS 示例 `gradient` 对齐（产品验收色带）。 */
internal fun jsApprovedHeatmapGradient(): HeatmapGradient {
    return HeatmapGradient(
        HeatmapColorStop(0.0f, 59, 130, 246, 0f),
        HeatmapColorStop(0.2f, 59, 130, 246, 0.60f),
        HeatmapColorStop(0.4f, 34, 211, 238, 0.72f),
        HeatmapColorStop(0.6f, 34, 197, 94, 0.80f),
        HeatmapColorStop(0.8f, 250, 204, 21, 0.90f),
        HeatmapColorStop(1.0f, 239, 68, 68, 1f),
    )
}

internal fun loadHeatmapPointsFromAssets(
    context: android.content.Context,
    usePerPointWeight: Boolean = true,
): List<HeatmapPoint> {
    val json = context.assets.open(HEATMAP_PROVIDER_ASSET).bufferedReader().use { it.readText() }
    val features = JSONObject(json).getJSONArray("features")
    val points = ArrayList<HeatmapPoint>(features.length())
    for (i in 0 until features.length()) {
        val feature = features.getJSONObject(i)
        val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
        val lng = coords.getDouble(0)
        val lat = coords.getDouble(1)
        val properties = feature.optJSONObject("properties")
        val weight = properties?.optDouble("weight", Double.NaN)?.toFloat()
        val location = LatLng(lat, lng)
        points.add(
            if (usePerPointWeight && weight != null && !weight.isNaN()) {
                HeatmapPoint(location, weight)
            } else {
                HeatmapPoint(location)
            },
        )
    }
    return points
}

internal fun HeatmapDemoState.buildOptions(): HeatmapTileOptions? {
    return buildOptions(generateDemoHeatmapPoints())
}

internal fun HeatmapDemoState.buildOptions(points: List<HeatmapPoint>): HeatmapTileOptions? {
    if (points.isEmpty()) return null
    val radiusValue = radius.toFloatOrNull() ?: return null
    val weightValue = weight.toFloatOrNull() ?: return null
    val intensityValue = intensity.toFloatOrNull() ?: return null
    val opacityValue = opacity.toFloatOrNull() ?: return null
    val minZoomValue = minZoom.toFloatOrNull() ?: return null
    val maxZoomValue = maxZoom.toFloatOrNull() ?: return null

    var options = HeatmapTileOptions()
        .withPoints(points)
        .withHeatmapRadius(radiusValue)
        .withHeatmapWeight(weightValue)
        .withHeatmapIntensity(intensityValue)
        .withHeatmapOpacity(opacityValue)
        .withMinZoom(minZoomValue)
        .withMaxZoom(maxZoomValue)
        .withHeatmapGradient(jsApprovedHeatmapGradient())

    return options
}

internal fun applyHeatmapPropsToTile(tile: HeatmapTile, state: HeatmapDemoState) {
    state.radius.toFloatOrNull()?.let { tile.setHeatmapRadius(it) }
    state.weight.toFloatOrNull()?.let { tile.setHeatmapWeight(it) }
    state.intensity.toFloatOrNull()?.let { tile.setHeatmapIntensity(it) }
    state.opacity.toFloatOrNull()?.let { tile.setHeatmapOpacity(it) }
    state.minZoom.toFloatOrNull()?.let { tile.setMinZoom(it) }
    state.maxZoom.toFloatOrNull()?.let { tile.setMaxZoom(it) }
    tile.setHeatmapGradient(jsApprovedHeatmapGradient())
}

internal fun drawHeatmapOnMap(map: MaptecMap, options: HeatmapTileOptions): HeatmapTile {
    val engine = map.getOverlayEngine()
    engine.deleteAllTileOverlays()
    val tile = engine.addTileOverlay(options)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEMO_HEATMAP_CENTER, DEMO_HEATMAP_ZOOM))
    return tile
}

internal fun validateRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return when {
        value < 1f -> "半径应大于等于 1"
        value > 50f -> "半径应小于等于 50"
        else -> null
    }
}

internal fun validateWeight(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入权重"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return when {
        value < 0f -> "权重应大于等于 0"
        value > 1f -> "权重应小于等于 1"
        else -> null
    }
}

internal fun validateIntensity(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入强度"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return when {
        value < 0f -> "强度应大于等于 0"
        value > 10f -> "强度应小于等于 10"
        else -> null
    }
}

internal fun validateOpacity(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入透明度（0～1）"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..1f) null else "透明度应在 0～1 之间"
}

internal fun validateMinZoom(input: String, maxZoomInput: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入 minZoom"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    if (value !in 0f..22f) return "minZoom 应在 0～22 之间"
    val maxValue = maxZoomInput.toFloatOrNull()
    return if (maxValue != null && value > maxValue) "minZoom 不能大于 maxZoom" else null
}

internal fun validateMaxZoom(input: String, minZoomInput: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入 maxZoom"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    if (value !in 0f..22f) return "maxZoom 应在 0～22 之间"
    val minValue = minZoomInput.toFloatOrNull()
    return if (minValue != null && value < minValue) "maxZoom 不能小于 minZoom" else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeatmapScaffold(
    defaultOptions: (() -> HeatmapTileOptions?)? = null,
    onTileDrawn: ((HeatmapTile) -> Unit)? = null,
    sheetContent: @Composable (mapView: MapView?, lastTile: HeatmapTile?) -> Unit,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var defaultDrawn by remember { mutableStateOf(false) }
    var lastTile by remember { mutableStateOf<HeatmapTile?>(null) }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                sheetContent(mapView, lastTile)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle {
                        if (!defaultDrawn && defaultOptions != null) {
                            defaultOptions.invoke()?.let { options ->
                                defaultDrawn = true
                                lastTile = drawHeatmapOnMap(map, options)
                                onTileDrawn?.invoke(lastTile!!)
                            }
                        }
                    }
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeatmapParameterInputs(state: HeatmapDemoState) {
    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_radius_label),
        value = state.radius.toFloatOrNull() ?: 34f,
        valueText = state.radius,
        onValueChange = { state.radius = formatDemoSliderValue(it) },
        onValueTextChange = { state.radius = it },
        valueRange = 1f..50f,
        rangeStartLabel = "1",
        rangeEndLabel = "50",
        isError = state.radiusError != null,
        errorText = state.radiusError,
        testTag = "heatmap_slider_radius",
    )

    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_weight_label),
        value = state.weight.toFloatOrNull() ?: 1f,
        valueText = state.weight,
        onValueChange = { state.weight = formatDemoSliderValue(it) },
        onValueTextChange = { state.weight = it },
        valueRange = 0f..1f,
        rangeStartLabel = "0",
        rangeEndLabel = "1",
        isError = state.weightError != null,
        errorText = state.weightError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "heatmap_slider_weight",
    )

    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_intensity_label),
        value = state.intensity.toFloatOrNull() ?: 0.82f,
        valueText = state.intensity,
        onValueChange = { state.intensity = formatDemoSliderValue(it) },
        onValueTextChange = { state.intensity = it },
        valueRange = 0f..10f,
        rangeStartLabel = "0",
        rangeEndLabel = "10",
        isError = state.intensityError != null,
        errorText = state.intensityError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "heatmap_slider_intensity",
    )

    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_opacity_label),
        value = state.opacity.toFloatOrNull() ?: 0.82f,
        valueText = state.opacity,
        onValueChange = { state.opacity = formatDemoSliderValue(it) },
        onValueTextChange = { state.opacity = it },
        valueRange = 0f..1f,
        rangeStartLabel = "0",
        rangeEndLabel = "1",
        isError = state.opacityError != null,
        errorText = state.opacityError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "heatmap_slider_opacity",
    )

    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_min_zoom_label),
        value = state.minZoom.toFloatOrNull() ?: 0f,
        valueText = state.minZoom,
        onValueChange = { state.minZoom = formatDemoSliderValue(it) },
        onValueTextChange = { state.minZoom = it },
        valueRange = 0f..22f,
        rangeStartLabel = "0",
        rangeEndLabel = "22",
        isError = state.minZoomError != null,
        errorText = state.minZoomError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "heatmap_slider_min_zoom",
    )

    DemoNumericSliderField(
        label = stringResource(R.string.heatmap_input_max_zoom_label),
        value = state.maxZoom.toFloatOrNull() ?: 22f,
        valueText = state.maxZoom,
        onValueChange = { state.maxZoom = formatDemoSliderValue(it) },
        onValueTextChange = { state.maxZoom = it },
        valueRange = 0f..22f,
        rangeStartLabel = "0",
        rangeEndLabel = "22",
        isError = state.maxZoomError != null,
        errorText = state.maxZoomError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "heatmap_slider_max_zoom",
    )

}

@Composable
internal fun DrawHeatmapButton(
    state: HeatmapDemoState,
    mapView: MapView?,
    onTileDrawn: (HeatmapTile) -> Unit,
) {
    val configPanelController = LocalDemoConfigPanelController.current
    DemoPanelButton(
        onClick = {
            val options = state.buildOptions() ?: return@DemoPanelButton
            val view = mapView ?: return@DemoPanelButton
            view.getMapAsync { map ->
                onTileDrawn(drawHeatmapOnMap(map, options))
            }
            configPanelController.close()
        },
        enabled = state.isValid && mapView != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("heatmap_draw_button"),
    ) {
        Text(stringResource(R.string.heatmap_draw_button))
    }
}

@Composable
internal fun LoadExternalHeatmapButton(
    state: HeatmapDemoState,
    mapView: MapView?,
    onTileDrawn: (HeatmapTile) -> Unit,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    DemoPanelButton(
        onClick = {
            if (!state.isValid) return@DemoPanelButton
            val view = mapView ?: return@DemoPanelButton
            val points = try {
                loadHeatmapPointsFromAssets(context)
            } catch (_: Exception) {
                return@DemoPanelButton
            }
            val options = state.buildOptions(points) ?: return@DemoPanelButton
            view.getMapAsync { map ->
                onTileDrawn(drawHeatmapOnMap(map, options))
            }
            configPanelController.close()
        },
        enabled = state.isValid && mapView != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("heatmap_load_external_button"),
    ) {
        Text(stringResource(R.string.heatmap_load_external_button))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeatmapLivePreviewEffect(state: HeatmapDemoState, lastTile: HeatmapTile?) {
    // 不把 lastTile 作为 key：避免 addTileOverlay 刚完成时重复下发 native 属性，与 overlay 注册 onTick 竞态。
    LaunchedEffect(
        state.radius,
        state.weight,
        state.intensity,
        state.opacity,
        state.minZoom,
        state.maxZoom,
    ) {
        lastTile?.let { tile ->
            if (state.isValid) {
                applyHeatmapPropsToTile(tile, state)
            }
        }
    }
}
