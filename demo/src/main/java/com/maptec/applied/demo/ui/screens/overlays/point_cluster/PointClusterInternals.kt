@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.point_cluster

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.graphics.Color as AndroidColor
import kotlin.math.roundToInt
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.maps.overlay.cluster.Cluster
import com.maptec.applied.maps.overlay.cluster.ClusterAdvancedStyle
import com.maptec.applied.maps.overlay.cluster.ClusterClickBehavior
import com.maptec.applied.maps.overlay.cluster.ClusterSimpleStyle
import com.maptec.applied.maps.overlay.cluster.ClusterOptions
import com.maptec.applied.maps.overlay.cluster.ClusterPoint
import com.maptec.applied.utils.BitmapUtils
import kotlin.math.cos
import kotlin.math.sin

internal const val CLUSTER_SAMPLE_ASSET = "cluster_sample_points.json"
internal const val CLUSTER_MAP_STYLE_LIGHT = Constants.DEFAULT_STYLE_ID
internal const val CLUSTER_MAP_STYLE_DARK = "dark"

internal val DEMO_CLUSTER_CENTER = LatLng(1.31, 103.8125)
private const val DEMO_CLUSTER_ZOOM = 11.15

private data class ClusterAnchor(val lng: Double, val lat: Double, val pointCount: Int)

/** 默认内存点：两簇锚点，约 80 点，进入页面即可看到聚合。 */
private val DEMO_CLUSTER_ANCHORS = listOf(
    ClusterAnchor(103.8519, 1.2903, 48),
    ClusterAnchor(103.8318, 1.3048, 32),
)

/** 单点「统一图标」关：默认针图 Fixed id；开：红点 Fixed id。均为 Options 级一份图，避免按点挂 Bitmap OOM。 */
private const val DEMO_LEAF_ICON_ID = "demo_leaf"
private const val DEMO_LEAF_FIXED_ICON_ID = "demo_leaf_fixed"

/** 单点图标模式：关「统一图标」= 针图 Fixed；开 = 红点 Fixed（非大点数按点不同图）。 */
internal enum class LeafIconMode { BY_FEATURE, FIXED }

/** 叶子文字：ByFeature 读各点 text；Fixed 全员同一文案。 */
internal enum class LeafTextMode { BY_FEATURE, FIXED }

/** 聚合点图标类型：SDF 可填色/描边；自定义则隐藏填色描边。 */
internal enum class ClusterSimpleStyleIconType { SDF, CUSTOM }

/** 自定义聚合图标：统一一张 / 按数量分档。 */
internal enum class ClusterCustomIconMode { UNIFIED, STEPPED }

/** 聚合点视觉半径：固定 / 自适应。 */
internal enum class ClusterVisualRadiusMode { FIXED, ADAPTIVE }

/** Demo 三条样式路径：SDK 默认 / ClusterSimpleStyle / ClusterAdvancedStyle 自定义位图。 */
internal enum class ClusterSimpleStyleDemoMode { DEFAULT, SIMPLE, ICON_STEPS }

internal class PointClusterDemoState {
    var clusterRadius by mutableStateOf("50")
    var clusterMaxZoom by mutableStateOf("16")
    var clusterMinPoints by mutableStateOf("2")
    var clusterEnable by mutableStateOf(true)
    var countAbbreviate by mutableStateOf(true)
    var visible by mutableStateOf(true)
    var clusterClickBehavior by mutableStateOf(ClusterClickBehavior.CALLBACK_ONLY)
    /** 地图底图样式：默认浅色 `light`，可切 `dark`。 */
    var mapStyleId by mutableStateOf(CLUSTER_MAP_STYLE_LIGHT)
    var isMapStyleSwitching by mutableStateOf(false)

    // ---- 聚合点样式 ----
    /** DEFAULT=SDK 内置；SIMPLE=ClusterSimpleStyle；ICON_STEPS=ClusterAdvancedStyle 自定义位图。 */
    var clusterSimpleStyleDemoMode by mutableStateOf(ClusterSimpleStyleDemoMode.DEFAULT)
    var clusterSimpleStyleIconType by mutableStateOf(ClusterSimpleStyleIconType.SDF)
    var clusterCustomIconMode by mutableStateOf(ClusterCustomIconMode.UNIFIED)
    var clusterFillColor by mutableStateOf("#2F80ED")
    var clusterFillOpacity by mutableStateOf("1.0")
    var clusterStrokeColor by mutableStateOf("#FFFFFF")
    var clusterStrokeWidth by mutableStateOf("1.0")
    var clusterTextSize by mutableStateOf("12")
    var clusterTextColor by mutableStateOf("#FFFFFF")
    var clusterVisualRadiusMode by mutableStateOf(ClusterVisualRadiusMode.ADAPTIVE)
    /** 视觉半径 dp（经 [ClusterOptions.IconRadius.dpToIconSize] 换算后下发）。 */
    var clusterVisualRadius by mutableStateOf("24")
    var clusterMinVisualRadius by mutableStateOf("18")
    var clusterMaxVisualRadius by mutableStateOf("30")
    var clusterRadiusMaxReference by mutableStateOf("100")
    var clusterLayerMinZoom by mutableStateOf("0")
    var clusterLayerMaxZoom by mutableStateOf("16")

    // ---- 单点样式 ----
    /** 关「统一图标」= 针图 Fixed；开 = 红点 Fixed。 */
    var leafIconMode by mutableStateOf(LeafIconMode.BY_FEATURE)
    /** 图标大小比例，>0。 */
    var leafIconSize by mutableStateOf("1.0")
    /** 图标旋转角度（度）。 */
    var leafIconRotation by mutableStateOf("0")
    /** 图标透明度 0～1。 */
    var leafIconOpacity by mutableStateOf("1.0")
    /** 是否显示叶子文字。 */
    var leafShowText by mutableStateOf(false)
    /** 开「显示文字」时：ByFeature=各点 id；Fixed=统一文案。 */
    var leafTextMode by mutableStateOf(LeafTextMode.BY_FEATURE)
    /** Fixed 模式统一文案。 */
    var leafFixedText by mutableStateOf("统一文案")
    /** 单点文字大小（sp）。 */
    var leafTextSize by mutableStateOf("12")
    /** 单点可见最小 zoom（point-layer-min-zoom）。 */
    var pointMinZoom by mutableStateOf("0")
    /** 单点可见最大 zoom（point-layer-max-zoom）。 */
    var pointMaxZoom by mutableStateOf("22")
    /**
     * 单点样式实时下发时抑制 LOADING/READY Toast，避免滑块拖动刷屏；
     * 在生命周期落到 READY/EMPTY/ERROR 后自动清零。
     */
    var suppressLifecycleToast by mutableStateOf(false)

    val radiusError: String? get() = validateClusterRadius(clusterRadius)
    val maxZoomError: String? get() = validateClusterMaxZoom(clusterMaxZoom)
    val minPointsError: String? get() = validateClusterMinPoints(clusterMinPoints)
    val leafIconSizeError: String? get() = validatePositiveFloat(leafIconSize, "iconSize")
    val leafIconOpacityError: String? get() = validateRangeFloat(leafIconOpacity, 0f, 1f, "opacity")
    val leafTextSizeError: String? get() = validatePositiveFloat(leafTextSize, "textSize")
    val pointMinZoomError: String? get() = validateZoomFloat(pointMinZoom, "point-layer-min-zoom")
    val pointMaxZoomError: String? get() = validateZoomFloat(pointMaxZoom, "point-layer-max-zoom")
    val clusterFillColorError: String?
        get() = if (clusterSimpleStyleIconType == ClusterSimpleStyleIconType.SDF) {
            validateClusterColor(clusterFillColor)
        } else null
    val clusterFillOpacityError: String?
        get() = if (clusterSimpleStyleIconType == ClusterSimpleStyleIconType.SDF) {
            validateRangeFloat(clusterFillOpacity, 0f, 1f, "fillOpacity")
        } else null
    val clusterStrokeColorError: String?
        get() = if (clusterSimpleStyleIconType == ClusterSimpleStyleIconType.SDF) {
            validateClusterColor(clusterStrokeColor)
        } else null
    val clusterStrokeWidthError: String?
        get() = if (clusterSimpleStyleIconType == ClusterSimpleStyleIconType.SDF) {
            validateNonNegativeFloat(clusterStrokeWidth, "strokeWidth")
        } else null
    val clusterTextSizeError: String? get() = validatePositiveFloat(clusterTextSize, "textSize")
    val clusterTextColorError: String? get() = validateClusterColor(clusterTextColor)
    val clusterVisualRadiusError: String?
        get() = if (clusterVisualRadiusMode == ClusterVisualRadiusMode.FIXED) {
            validatePositiveFloat(clusterVisualRadius, "radius")
        } else null
    val clusterMinVisualRadiusError: String?
        get() = if (clusterVisualRadiusMode == ClusterVisualRadiusMode.ADAPTIVE) {
            validatePositiveFloat(clusterMinVisualRadius, "minRadius")
        } else null
    val clusterMaxVisualRadiusError: String?
        get() = if (clusterVisualRadiusMode == ClusterVisualRadiusMode.ADAPTIVE) {
            validateAdaptiveMaxRadius(clusterMinVisualRadius, clusterMaxVisualRadius)
        } else null
    val clusterRadiusMaxReferenceError: String?
        get() = if (clusterVisualRadiusMode == ClusterVisualRadiusMode.ADAPTIVE) {
            validateMaxReference(clusterRadiusMaxReference, clusterMinPoints)
        } else null
    val clusterLayerMinZoomError: String? get() = validateZoomFloat(clusterLayerMinZoom, "cluster-layer-min-zoom")
    val clusterLayerMaxZoomError: String? get() = validateZoomFloat(clusterLayerMaxZoom, "cluster-layer-max-zoom")

    val isDarkMapStyle: Boolean
        get() = mapStyleId == CLUSTER_MAP_STYLE_DARK

    val isValid: Boolean
        get() = radiusError == null && maxZoomError == null && minPointsError == null
                && leafIconSizeError == null && leafIconOpacityError == null
                && leafTextSizeError == null && pointMinZoomError == null && pointMaxZoomError == null
                && clusterFillColorError == null && clusterFillOpacityError == null
                && clusterStrokeColorError == null && clusterStrokeWidthError == null
                && clusterTextSizeError == null && clusterTextColorError == null
                && clusterVisualRadiusError == null && clusterMinVisualRadiusError == null
                && clusterMaxVisualRadiusError == null && clusterRadiusMaxReferenceError == null
                && clusterLayerMinZoomError == null && clusterLayerMaxZoomError == null
}

@Composable
internal fun rememberPointClusterDemoState(): PointClusterDemoState =
    remember { PointClusterDemoState() }

/**
 * 生成演示用单点。
 *
 * 「统一图标」开/关在 Demo 中均为 Options 级 [ClusterOptions.PointIconImage.fixed]
 * （红点 / 默认针图），点上不挂 Bitmap，避免大点数 setPoints 时 JNI/引擎按点注册图 OOM。
 */
internal fun generateDemoClusterPoints(
    context: Context,
    state: PointClusterDemoState = PointClusterDemoState(),
): List<ClusterPoint> {
    val iconSize = state.leafIconSize.toFloatOrNull()?.takeIf { it > 0f }
    val iconRotation = state.leafIconRotation.toFloatOrNull()
    val iconOpacity = state.leafIconOpacity.toFloatOrNull()?.coerceIn(0f, 1f)
    val textSize = state.leafTextSize.toFloatOrNull()?.takeIf { it > 0f }

    val points = ArrayList<ClusterPoint>(DEMO_CLUSTER_ANCHORS.sumOf { it.pointCount })
    DEMO_CLUSTER_ANCHORS.forEachIndexed { anchorIndex, anchor ->
        for (pointIndex in 0 until anchor.pointCount) {
            val ring = 0.35 + (pointIndex % 6) * 0.14
            val angle = pointIndex * 2.399 + anchorIndex * 0.72
            val lng = anchor.lng + cos(angle) * 0.0135 * ring
            val lat = anchor.lat + sin(angle) * 0.0102 * ring
            val id = "demo-${anchorIndex + 1}-${pointIndex + 1}"
            val builder = ClusterPoint.builder(LatLng(lat, lng)).withId(id)
            iconSize?.let { builder.withIconSize(it) }
            iconRotation?.let { builder.withIconRotation(it) }
            iconOpacity?.let { builder.withIconOpacity(it) }
            if (state.leafShowText) {
                if (state.leafTextMode == LeafTextMode.BY_FEATURE) {
                    builder.withText(id)
                }
                textSize?.let { builder.withTextSize(it) }
                builder.withTextOffset(0f, 1.5f)
            }
            points.add(builder.build())
        }
    }
    return points
}

/**
 * 按当前面板参数重写单点样式（保留坐标；图标由 PointIconImage.fixed 统一提供）。
 */
internal fun restyleClusterPoints(
    context: Context,
    source: List<ClusterPoint>,
    state: PointClusterDemoState,
): List<ClusterPoint> {
    val iconSize = state.leafIconSize.toFloatOrNull()?.takeIf { it > 0f }
    val iconRotation = state.leafIconRotation.toFloatOrNull()
    val iconOpacity = state.leafIconOpacity.toFloatOrNull()?.coerceIn(0f, 1f)
    val textSize = state.leafTextSize.toFloatOrNull()?.takeIf { it > 0f }

    return source.map { point ->
        val builder = ClusterPoint.builder(point.location).withId(point.id)
        // 开/关「统一图标」均不在点上挂 Bitmap，只刷新 size / opacity / text 等
        iconSize?.let { builder.withIconSize(it) }
        iconRotation?.let { builder.withIconRotation(it) }
        iconOpacity?.let { builder.withIconOpacity(it) }
        if (state.leafShowText) {
            if (state.leafTextMode == LeafTextMode.BY_FEATURE) {
                val text = point.id ?: point.text
                if (text != null) {
                    builder.withText(text)
                }
            }
            textSize?.let { builder.withTextSize(it) }
            builder.withTextOffset(0f, 1.5f)
        }
        builder.build()
    }
}

/**
 * 将单点样式下发到已有 Overlay。
 *
 * 叶子图通过 [Cluster.setPointIconImage] Fixed 一份下发；[Cluster.setPoints] 只更新点属性。
 * 避免按点挂唯一 iconId+Bitmap 导致大点数下 syncImages/addImage OOM。
 */
internal fun PointClusterDemoState.resolvePointTextField(): ClusterOptions.PointTextField {
    if (!leafShowText) {
        return ClusterOptions.PointTextField.fixed("")
    }
    return when (leafTextMode) {
        LeafTextMode.BY_FEATURE -> ClusterOptions.PointTextField.byFeature("text")
        LeafTextMode.FIXED -> ClusterOptions.PointTextField.fixed(leafFixedText)
    }
}

internal fun applyLeafStyleLive(
    context: Context,
    overlay: Cluster?,
    state: PointClusterDemoState,
    applyPoints: Boolean = true,
    applyMinZoom: Boolean = false,
    applyMaxZoom: Boolean = false,
    updateIconMode: Boolean = false,
) {
    val target = overlay?.takeIf { !it.isRemoved } ?: return

    if (applyMinZoom && state.pointMinZoomError == null) {
        state.pointMinZoom.toFloatOrNull()?.let { target.setPointLayerMinZoom(it) }
    }
    if (applyMaxZoom && state.pointMaxZoomError == null) {
        state.pointMaxZoom.toFloatOrNull()?.let { target.setPointLayerMaxZoom(it) }
    }

    if (!applyPoints) return
    if (state.leafIconSizeError != null ||
        state.leafIconOpacityError != null ||
        (state.leafShowText && state.leafTextSizeError != null)
    ) {
        return
    }

    state.suppressLifecycleToast = true
    if (updateIconMode) {
        when (state.leafIconMode) {
            // 开：红点 Fixed；关：默认针图 Fixed（勿 byFeature + 按点挂图，大点数会 OOM）
            LeafIconMode.FIXED -> {
                val fixedIcon = loadDrawableBitmap(context, R.drawable.red_dot)
                if (fixedIcon != null) {
                    target.setPointIconImage(
                        ClusterOptions.PointIconImage.fixed(DEMO_LEAF_FIXED_ICON_ID, fixedIcon, false),
                    )
                }
            }
            LeafIconMode.BY_FEATURE -> {
                val pinIcon = loadDrawableBitmap(context, R.drawable.default_pin)
                if (pinIcon != null) {
                    target.setPointIconImage(
                        ClusterOptions.PointIconImage.fixed(DEMO_LEAF_ICON_ID, pinIcon, false),
                    )
                }
            }
        }
    }
    target.setPointTextField(state.resolvePointTextField())
    target.setPoints(restyleClusterPoints(context, target.points, state))
}

internal fun PointClusterDemoState.buildOptions(
    context: Context,
    points: List<ClusterPoint> = generateDemoClusterPoints(context, this),
): ClusterOptions? {
    if (!isValid || points.isEmpty()) return null
    val radius = clusterRadius.toIntOrNull() ?: return null
    val maxZoom = clusterMaxZoom.toDoubleOrNull() ?: return null
    val minPoints = clusterMinPoints.toIntOrNull() ?: return null
    val pMinZoom = pointMinZoom.toFloatOrNull() ?: return null
    val pMaxZoom = pointMaxZoom.toFloatOrNull() ?: return null
    val cMinZoom = clusterLayerMinZoom.toFloatOrNull() ?: return null
    val cMaxZoom = clusterLayerMaxZoom.toFloatOrNull() ?: return null
    val leafIcon = loadDrawableBitmap(context, R.drawable.default_pin) ?: return null
    val clusterIcon = loadDrawableBitmap(context, R.drawable.blue_dot) ?: return null
    // 开「统一图标」= 红点 Fixed；关 = 默认针图 Fixed（勿按点挂图，大点数会 OOM）
    val fixedLeafIcon = loadDrawableBitmap(context, R.drawable.red_dot) ?: leafIcon

    val pointIconImage = when (leafIconMode) {
        LeafIconMode.FIXED ->
            ClusterOptions.PointIconImage.fixed(DEMO_LEAF_FIXED_ICON_ID, fixedLeafIcon, false)
        LeafIconMode.BY_FEATURE ->
            ClusterOptions.PointIconImage.fixed(DEMO_LEAF_ICON_ID, leafIcon, false)
    }

    // 未自定义时不传样式/半径，由 SDK ClusterSimpleStyle 默认样式提供默认三色 + 18～30dp 自适应
    var options = ClusterOptions()
        .withPoints(points)
        .withDefaultIcons(leafIcon, clusterIcon)
        .withPointIconImage(pointIconImage)
        .withPointTextField(resolvePointTextField())
        .withClusterEnabled(clusterEnable)
        .withClusterRadius(radius)
        .withClusterMaxZoom(maxZoom)
        .withClusterMinPoints(minPoints.toLong())
        .withClusterCountAbbreviate(countAbbreviate)
        .withClusterLayerMinZoom(cMinZoom)
        .withClusterLayerMaxZoom(cMaxZoom)
        .withPointLayerMinZoom(pMinZoom)
        .withPointLayerMaxZoom(pMaxZoom)
        .withVisible(visible)
        .withClusterClickBehavior(clusterClickBehavior)

    when (clusterSimpleStyleDemoMode) {
        ClusterSimpleStyleDemoMode.DEFAULT -> Unit
        ClusterSimpleStyleDemoMode.SIMPLE -> {
            buildClusterSimpleStyle()?.let { options = options.withClusterSimpleStyle(it) }
        }
        ClusterSimpleStyleDemoMode.ICON_STEPS -> {
            buildClusterAdvancedStyles(context, this)?.let {
                options = options.withClusterAdvancedStyles(it)
            }
            buildClusterIconRadius(context, this)?.let { options = options.withClusterIconRadius(it) }
        }
    }
    return options
}

/** 简单层：ClusterSimpleStyle，覆盖圆点填色/描边/文字/视觉半径。 */
internal fun PointClusterDemoState.buildClusterSimpleStyle(): ClusterSimpleStyle? {
    val fill = parseClusterColor(clusterFillColor) ?: return null
    val opacity = clusterFillOpacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: return null
    val stroke = parseClusterColor(clusterStrokeColor) ?: return null
    val strokeWidth = clusterStrokeWidth.toFloatOrNull()?.coerceAtLeast(0f) ?: return null
    val textSize = clusterTextSize.toFloatOrNull()?.takeIf { it > 0f } ?: return null
    val textColor = parseClusterColor(clusterTextColor) ?: return null

    val builder = ClusterSimpleStyle.builder()
        .withFillOpacity(opacity)
        .withStrokeColor(stroke)
        .withStrokeWidthDp(strokeWidth)
        .withTextSize(textSize)
        .withTextColor(textColor)

    if (clusterFillColor.trim().equals("#2F80ED", ignoreCase = true)) {
        builder.withTieredFillColors(
            ClusterSimpleStyle.COLOR_LOW,
            ClusterSimpleStyle.COLOR_MID,
            ClusterSimpleStyle.COLOR_HIGH,
        )
    } else {
        builder.withUnifiedFillColor(fill)
    }

    return when (clusterVisualRadiusMode) {
        ClusterVisualRadiusMode.FIXED -> {
            val radius = clusterVisualRadius.toFloatOrNull()?.takeIf { it > 0f } ?: return null
            builder.withFixedRadiusDp(radius).build()
        }
        ClusterVisualRadiusMode.ADAPTIVE -> {
            val minR = clusterMinVisualRadius.toFloatOrNull()?.takeIf { it > 0f } ?: return null
            val maxR = clusterMaxVisualRadius.toFloatOrNull()?.takeIf { it >= minR } ?: return null
            val ref = clusterRadiusMaxReference.toFloatOrNull()?.toLong()?.takeIf { it > 0L }
                ?: return null
            builder.withAdaptiveRadiusDp(minR, maxR, ref).build()
        }
    }
}

/**
 * 自定义位图分档：走 SDK [ClusterAdvancedStyle]（按数量换图，并带上数量文字大小/颜色）。
 *
 * 不用 [com.maptec.applied.maps.overlay.cluster.ClusterIconStep]，因其不支持文字样式。
 */
internal fun buildClusterAdvancedStyles(
    context: Context,
    state: PointClusterDemoState,
): List<ClusterAdvancedStyle>? {
    val textSize = state.clusterTextSize.toFloatOrNull()?.takeIf { it > 0f } ?: return null
    val textColor = parseClusterColor(state.clusterTextColor) ?: return null
    return when (state.clusterSimpleStyleIconType) {
        ClusterSimpleStyleIconType.SDF -> null // 圆点走 SIMPLE，不应进入 ICON_STEPS
        ClusterSimpleStyleIconType.CUSTOM -> when (state.clusterCustomIconMode) {
            ClusterCustomIconMode.UNIFIED -> {
                val bmp = loadDrawableBitmap(context, R.drawable.blue_dot) ?: return null
                listOf(
                    buildClusterAdvancedStyle(2, 99L, "demo_cluster_uni_s", bmp, 1.5f, textSize, textColor),
                    buildClusterAdvancedStyle(100, 999L, "demo_cluster_uni_m", bmp, 1.8f, textSize, textColor),
                    buildClusterAdvancedStyle(1000, null, "demo_cluster_uni_l", bmp, 2.1f, textSize, textColor),
                )
            }
            ClusterCustomIconMode.STEPPED -> {
                val small = loadDrawableBitmap(context, R.drawable.yellow_dot) ?: return null
                val medium = loadDrawableBitmap(context, R.drawable.green_dot) ?: return null
                val large = loadDrawableBitmap(context, R.drawable.red_dot) ?: return null
                listOf(
                    buildClusterAdvancedStyle(2, 99L, "demo_cluster_step_s", small, 1.5f, textSize, textColor),
                    buildClusterAdvancedStyle(100, 999L, "demo_cluster_step_m", medium, 1.8f, textSize, textColor),
                    buildClusterAdvancedStyle(1000, null, "demo_cluster_step_l", large, 2.1f, textSize, textColor),
                )
            }
        }
    }
}

internal fun buildClusterIconRadius(
    context: Context,
    state: PointClusterDemoState,
): ClusterOptions.IconRadius? {
    val icon = loadDrawableBitmap(context, R.drawable.blue_dot) ?: return null
    return when (state.clusterVisualRadiusMode) {
        ClusterVisualRadiusMode.FIXED -> {
            val radius = state.clusterVisualRadius.toFloatOrNull()?.takeIf { it > 0f } ?: return null
            ClusterOptions.IconRadius.fixedDp(radius, icon)
        }
        ClusterVisualRadiusMode.ADAPTIVE -> {
            val minR = state.clusterMinVisualRadius.toFloatOrNull()?.takeIf { it > 0f } ?: return null
            val maxR = state.clusterMaxVisualRadius.toFloatOrNull()?.takeIf { it >= minR } ?: return null
            val ref = state.clusterRadiusMaxReference.toFloatOrNull()?.toLong()?.takeIf { it > 0L }
                ?: return null
            ClusterOptions.IconRadius.adaptiveDp(minR, maxR, ref, icon)
        }
    }
}

/** 下发聚合点样式。拖动滑块时节流调用，避免高频 applyOption；松手时再强制下发一次。 */
internal fun applyClusterSimpleStyleLive(
    context: Context,
    overlay: Cluster?,
    state: PointClusterDemoState,
    applyStyles: Boolean = true,
    applyRadius: Boolean = true,
    applyZoom: Boolean = false,
    applyMinZoom: Boolean = false,
    applyMaxZoom: Boolean = false,
) {
    val target = overlay?.takeIf { !it.isRemoved } ?: return
    if (!state.isValid) return

    if (applyStyles || applyRadius) {
        when {
            state.clusterSimpleStyleIconType == ClusterSimpleStyleIconType.CUSTOM -> {
                state.clusterSimpleStyleDemoMode = ClusterSimpleStyleDemoMode.ICON_STEPS
                buildClusterAdvancedStyles(context, state)?.let { target.setClusterAdvancedStyles(it) }
                if (applyRadius) {
                    buildClusterIconRadius(context, state)?.let { target.setClusterIconRadius(it) }
                }
            }
            else -> {
                state.clusterSimpleStyleDemoMode = ClusterSimpleStyleDemoMode.SIMPLE
                state.buildClusterSimpleStyle()?.let { target.setClusterSimpleStyle(it) }
            }
        }
    }
    if (applyZoom || applyMinZoom) {
        state.clusterLayerMinZoom.toFloatOrNull()?.let { target.setClusterLayerMinZoom(it) }
    }
    if (applyZoom || applyMaxZoom) {
        state.clusterLayerMaxZoom.toFloatOrNull()?.let { target.setClusterLayerMaxZoom(it) }
    }
}

private fun buildClusterAdvancedStyle(
    minCount: Long,
    maxCount: Long?,
    iconId: String,
    bitmap: Bitmap,
    iconSize: Float,
    textSize: Float,
    textColor: Int,
): ClusterAdvancedStyle = ClusterAdvancedStyle.builder(minCount)
    .withMaxCount(maxCount)
    .withIcon(iconId, bitmap, false)
    .withIconSize(iconSize)
    .withTextSize(textSize)
    .withTextColor(textColor)
    .build()

internal fun parseClusterColor(input: String): Int? = try {
    AndroidColor.parseColor(input.trim())
} catch (_: IllegalArgumentException) {
    null
}

internal fun validateClusterColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入颜色"
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(trimmed)) null else "格式错误，示例：#51BBD6"
}

internal fun validateNonNegativeFloat(input: String, label: String): String? {
    val v = input.trim().toFloatOrNull() ?: return "$label 请输入有效数字"
    return if (v < 0f) "$label 应大于等于 0" else null
}

internal fun validateAdaptiveMaxRadius(minInput: String, maxInput: String): String? {
    val minR = minInput.trim().toFloatOrNull() ?: return "minRadius 请输入有效数字"
    val maxR = maxInput.trim().toFloatOrNull() ?: return "maxRadius 请输入有效数字"
    return when {
        maxR <= 0f -> "maxRadius 应大于 0"
        maxR < minR -> "maxRadius 不能小于 minRadius"
        else -> null
    }
}

internal fun validateMaxReference(input: String, minPointsInput: String): String? {
    val ref = input.trim().toFloatOrNull()?.toLong() ?: return "maxReference 请输入有效整数"
    val minPoints = minPointsInput.trim().toLongOrNull() ?: 2L
    return when {
        ref <= 0L -> "maxReference 应大于 0"
        ref <= minPoints -> "maxReference 应大于 minPoints"
        else -> null
    }
}

internal fun drawPointClusterOnMap(
    context: Context,
    map: MaptecMap,
    state: PointClusterDemoState,
    points: List<ClusterPoint>? = null,
    resetCamera: Boolean = true,
): Cluster? {
    val engine = map.getOverlayEngine()
    engine.deleteAllClusterOverlays()
    val options = state.buildOptions(context, points ?: generateDemoClusterPoints(context, state))
        ?: return null
    val overlay = engine.addClusterOverlay(options)
    if (resetCamera) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEMO_CLUSTER_CENTER, DEMO_CLUSTER_ZOOM))
    }
    return overlay
}

/**
 * 切换浅色 / 深色底图。
 *
 * SDK：`setStyle` 会清掉 Overlay，因此渲染完成后必须重新 add。
 * Demo：保留切换前相机与点集，避免视角被拉回初始中心、散开状态因 zoom 重置而重新聚合。
 */
internal fun switchPointClusterMapStyle(
    context: Context,
    map: MaptecMap,
    state: PointClusterDemoState,
    dark: Boolean,
    existingOverlay: Cluster? = null,
    onOverlayDrawn: (Cluster, MaptecMap) -> Unit,
) {
    val styleId = if (dark) CLUSTER_MAP_STYLE_DARK else CLUSTER_MAP_STYLE_LIGHT
    if (state.mapStyleId == styleId || state.isMapStyleSwitching) {
        return
    }
    val preservedCamera = map.cameraPosition
    val preservedPoints = existingOverlay
        ?.takeIf { !it.isRemoved }
        ?.points
        ?.takeIf { it.isNotEmpty() }
    state.mapStyleId = styleId
    state.isMapStyleSwitching = true
    map.setStyle(StyleOption(styleId), object : StyleStatusCallback {
        override fun onStyleLoaded(style: Style?) {}

        override fun onStyleRendered(style: Style?) {
            // native onStyleRendered 在渲染线程回调；Compose / addClusterOverlay 必须回主线程。
            runOnMainThread {
                state.isMapStyleSwitching = false
                drawPointClusterOnMap(
                    context = context,
                    map = map,
                    state = state,
                    points = preservedPoints,
                    resetCamera = false,
                )?.let { overlay ->
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(preservedCamera))
                    onOverlayDrawn(overlay, map)
                }
            }
        }

        override fun onFailed(style: Style?, message: String) {
            runOnMainThread {
                state.isMapStyleSwitching = false
                showMapStyleLoadError(context, message)
            }
        }
    })
}

private fun runOnMainThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        Handler(Looper.getMainLooper()).post(block)
    }
}

internal fun validateClusterRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入聚合半径"
    val value = trimmed.toIntOrNull() ?: return "请输入有效整数"
    return when {
        value < 0 -> "半径应大于等于 0"
        value > 512 -> "半径应小于等于 512"
        else -> null
    }
}

internal fun validateClusterMaxZoom(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入 clusterMaxZoom"
    val value = trimmed.toDoubleOrNull() ?: return "请输入有效数字"
    return when {
        value < 0.0 -> "clusterMaxZoom 应大于等于 0"
        value > 22.0 -> "clusterMaxZoom 应小于等于 22"
        else -> null
    }
}

internal fun validateClusterMinPoints(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入 minPoints"
    val value = trimmed.toIntOrNull() ?: return "请输入有效整数"
    return when {
        value < 2 -> "minPoints 应大于等于 2"
        else -> null
    }
}

internal fun validatePositiveFloat(input: String, label: String): String? {
    val v = input.trim().toFloatOrNull() ?: return "$label 请输入有效数字"
    return if (v <= 0f) "$label 应大于 0" else null
}

internal fun validateRangeFloat(input: String, min: Float, max: Float, label: String): String? {
    val v = input.trim().toFloatOrNull() ?: return "$label 请输入有效数字"
    return if (v < min || v > max) "$label 应在 $min～$max" else null
}

internal fun validateZoomFloat(input: String, label: String): String? {
    val v = input.trim().toFloatOrNull() ?: return "$label 请输入有效数字"
    return if (v < 0f || v > 22f) "$label 应在 0～22" else null
}

private fun loadDrawableBitmap(context: Context, @DrawableRes res: Int): Bitmap? =
    ContextCompat.getDrawable(context, res)?.let(BitmapUtils::getBitmapFromDrawable)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PointClusterScaffold(
    state: PointClusterDemoState,
    onOverlayDrawn: ((Cluster, MaptecMap) -> Unit)? = null,
    sheetContent: @Composable (
        mapView: MapView?,
        lastOverlay: Cluster?,
    ) -> Unit,
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var defaultDrawn by remember { mutableStateOf(false) }
    var lastOverlay by remember { mutableStateOf<Cluster?>(null) }

    val handleOverlayDrawn: (Cluster, MaptecMap) -> Unit = { overlay, map ->
        lastOverlay = overlay
        onOverlayDrawn?.invoke(overlay, map)
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                sheetContent(mapView, lastOverlay)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, map ->
                    mapView = view
                    map.getStyle {
                        if (!defaultDrawn) {
                            defaultDrawn = true
                            drawPointClusterOnMap(context, map, state)?.let { overlay ->
                                handleOverlayDrawn(overlay, map)
                            }
                        }
                    }
                },
            )
        },
    )
}

@Composable
internal fun PointClusterParameterInputs(
    state: PointClusterDemoState,
    mapView: MapView? = null,
    lastOverlay: Cluster? = null,
    onOverlayDrawn: ((Cluster, MaptecMap) -> Unit)? = null,
) {
    val context = LocalContext.current
    val applyLeafPoints: () -> Unit = {
        applyLeafStyleLive(context, lastOverlay, state, applyPoints = true)
    }
    val applyLeafIconMode: () -> Unit = {
        applyLeafStyleLive(
            context,
            lastOverlay,
            state,
            applyPoints = true,
            updateIconMode = true,
        )
    }
    val applyLeafMinZoom: () -> Unit = {
        applyLeafStyleLive(
            context,
            lastOverlay,
            state,
            applyPoints = false,
            applyMinZoom = true,
        )
    }
    val applyLeafMaxZoom: () -> Unit = {
        applyLeafStyleLive(
            context,
            lastOverlay,
            state,
            applyPoints = false,
            applyMaxZoom = true,
        )
    }
    val liveOverlay: () -> Cluster? = {
        lastOverlay?.takeIf { !it.isRemoved }
    }
    val applyClusterRadiusLive: () -> Unit = {
        if (state.radiusError == null) {
            state.clusterRadius.toIntOrNull()?.let { liveOverlay()?.setClusterRadius(it) }
        }
    }
    val applyClusterMaxZoomLive: () -> Unit = {
        if (state.maxZoomError == null) {
            state.clusterMaxZoom.toDoubleOrNull()?.let { liveOverlay()?.setClusterMaxZoom(it) }
        }
    }
    val applyClusterMinPointsLive: () -> Unit = {
        if (state.minPointsError == null) {
            state.clusterMinPoints.toIntOrNull()?.let { liveOverlay()?.setClusterMinPoints(it.toLong()) }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.point_cluster_input_map_style_label,
                stringResource(
                    if (state.isDarkMapStyle) {
                        R.string.map_style_dark
                    } else {
                        R.string.map_style_light
                    },
                ),
            ),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.isDarkMapStyle,
            onCheckedChange = { dark ->
                val view = mapView ?: return@Switch
                val redraw = onOverlayDrawn ?: return@Switch
                view.getMapAsync { map ->
                    switchPointClusterMapStyle(
                        context = context,
                        map = map,
                        state = state,
                        dark = dark,
                        existingOverlay = lastOverlay,
                        onOverlayDrawn = redraw,
                    )
                }
            },
            enabled = mapView != null && onOverlayDrawn != null && !state.isMapStyleSwitching,
            modifier = Modifier.testTag("point_cluster_switch_map_style"),
        )
    }

    DemoNumericSliderField(
        label = stringResource(R.string.point_cluster_input_radius_label),
        value = state.clusterRadius.toFloatOrNull() ?: 50f,
        valueText = state.clusterRadius,
        onValueChange = { state.clusterRadius = it.roundToInt().toString() },
        onValueTextChange = {
            state.clusterRadius = it
            applyClusterRadiusLive()
        },
        valueRange = 0f..512f,
        rangeStartLabel = "0",
        rangeEndLabel = "512",
        isError = state.radiusError != null,
        errorText = state.radiusError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "point_cluster_slider_radius",
        onValueChangeFinished = applyClusterRadiusLive,
        steps = 512,
    )

    DemoNumericSliderField(
        label = stringResource(R.string.point_cluster_input_max_zoom_label),
        value = state.clusterMaxZoom.toFloatOrNull() ?: 16f,
        valueText = state.clusterMaxZoom,
        onValueChange = {
            state.clusterMaxZoom = formatDemoSliderValue(it)
            if (state.maxZoomError == null) applyClusterMaxZoomLive()
        },
        onValueTextChange = {
            state.clusterMaxZoom = it
            applyClusterMaxZoomLive()
        },
        valueRange = 0f..22f,
        rangeStartLabel = "0",
        rangeEndLabel = "22",
        isError = state.maxZoomError != null,
        errorText = state.maxZoomError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "point_cluster_slider_max_zoom",
        onValueChangeFinished = applyClusterMaxZoomLive,
    )

    DemoNumericSliderField(
        label = stringResource(R.string.point_cluster_input_min_points_label),
        value = state.clusterMinPoints.toFloatOrNull() ?: 2f,
        valueText = state.clusterMinPoints,
        onValueChange = { state.clusterMinPoints = it.roundToInt().toString() },
        onValueTextChange = {
            state.clusterMinPoints = it
            applyClusterMinPointsLive()
        },
        valueRange = 2f..50f,
        rangeStartLabel = "2",
        rangeEndLabel = "50",
        isError = state.minPointsError != null,
        errorText = state.minPointsError,
        modifier = Modifier.padding(top = 12.dp),
        testTag = "point_cluster_slider_min_points",
        onValueChangeFinished = applyClusterMinPointsLive,
        steps = 48,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.point_cluster_input_enable_label),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.clusterEnable,
            onCheckedChange = { checked ->
                state.clusterEnable = checked
                lastOverlay?.takeIf { !it.isRemoved }?.setClusterEnabled(checked)
            },
            modifier = Modifier.testTag("point_cluster_switch_enable"),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.point_cluster_input_count_abbreviate_label),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.countAbbreviate,
            onCheckedChange = { checked ->
                state.countAbbreviate = checked
                liveOverlay()?.setClusterCountAbbreviate(checked)
            },
            modifier = Modifier.testTag("point_cluster_switch_abbreviate"),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.point_cluster_input_visible_label),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.visible,
            onCheckedChange = { checked ->
                state.visible = checked
                lastOverlay?.takeIf { !it.isRemoved }?.setVisible(checked)
            },
            enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
            modifier = Modifier.testTag("point_cluster_switch_visible"),
        )
    }

    var clickBehaviorExpanded by remember { mutableStateOf(false) }
    val clickBehaviors = listOf(
        ClusterClickBehavior.CALLBACK_ONLY,
        ClusterClickBehavior.ZOOM_TO_EXPANSION,
        ClusterClickBehavior.NONE,
    )
    Text(
        text = stringResource(R.string.point_cluster_input_click_behavior_label),
        modifier = Modifier.padding(top = 8.dp),
    )
    ExposedDropdownMenuBox(
        expanded = clickBehaviorExpanded,
        onExpandedChange = { clickBehaviorExpanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("point_cluster_click_behavior"),
    ) {
        OutlinedTextField(
            value = state.clusterClickBehavior.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clickBehaviorExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("point_cluster_click_behavior_field"),
        )
        ExposedDropdownMenu(
            expanded = clickBehaviorExpanded,
            onDismissRequest = { clickBehaviorExpanded = false },
        ) {
            clickBehaviors.forEach { behavior ->
                DropdownMenuItem(
                    text = { Text(behavior.name) },
                    onClick = {
                        state.clusterClickBehavior = behavior
                        lastOverlay?.takeIf { !it.isRemoved }?.setClusterClickBehavior(behavior)
                        clickBehaviorExpanded = false
                    },
                    modifier = Modifier.testTag("point_cluster_click_behavior_option_${behavior.name}"),
                )
            }
        }
    }

    // ---- 聚合点样式（默认收起）----
    var clusterSimpleStyleExpanded by remember { mutableStateOf(false) }
    val applyClusterSimpleStyles: () -> Unit = {
        applyClusterSimpleStyleLive(context, lastOverlay, state, applyStyles = true, applyRadius = false)
    }
    val applyClusterRadiusCfg: () -> Unit = {
        applyClusterSimpleStyleLive(context, lastOverlay, state, applyStyles = false, applyRadius = true)
    }
    val applyClusterMinZoom: () -> Unit = {
        applyClusterSimpleStyleLive(
            context,
            lastOverlay,
            state,
            applyStyles = false,
            applyRadius = false,
            applyMinZoom = true,
        )
    }
    val applyClusterMaxZoom: () -> Unit = {
        applyClusterSimpleStyleLive(
            context,
            lastOverlay,
            state,
            applyStyles = false,
            applyRadius = false,
            applyMaxZoom = true,
        )
    }
    fun throttleClusterLiveApply(apply: () -> Unit) {
        apply()
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { clusterSimpleStyleExpanded = !clusterSimpleStyleExpanded }
            .testTag("point_cluster_cluster_style_toggle"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.point_cluster_cluster_style_section),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                if (clusterSimpleStyleExpanded) R.string.panel_collapse else R.string.panel_expand,
            ),
        )
    }

    if (clusterSimpleStyleExpanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.point_cluster_input_cluster_custom_icon_label),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.clusterSimpleStyleIconType == ClusterSimpleStyleIconType.CUSTOM,
                onCheckedChange = { custom ->
                    state.clusterSimpleStyleIconType =
                        if (custom) ClusterSimpleStyleIconType.CUSTOM else ClusterSimpleStyleIconType.SDF
                    state.clusterSimpleStyleDemoMode =
                        if (custom) ClusterSimpleStyleDemoMode.ICON_STEPS else ClusterSimpleStyleDemoMode.SIMPLE
                    // 自定义位图多为浅色中心/彩色针图，默认用黑字；圆点默认用白字
                    state.clusterTextColor = if (custom) "#000000" else "#FFFFFF"
                    applyClusterSimpleStyles()
                },
                enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                modifier = Modifier.testTag("point_cluster_switch_cluster_custom_icon"),
            )
        }

        if (state.clusterSimpleStyleIconType == ClusterSimpleStyleIconType.SDF) {
            ColorPickerField(
                value = state.clusterFillColor,
                onValueChange = {
                    state.clusterFillColor = it
                    if (state.clusterFillColorError == null) applyClusterSimpleStyles()
                },
                label = stringResource(R.string.point_cluster_input_cluster_fill_color_label),
                isError = state.clusterFillColorError != null,
                supportingText = state.clusterFillColorError?.let { { Text(it) } },
                allowManualInput = false,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_cluster_fill_color",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_fill_opacity_label),
                value = state.clusterFillOpacity.toFloatOrNull() ?: 1f,
                valueText = state.clusterFillOpacity,
                onValueChange = {
                    state.clusterFillOpacity = formatDemoSliderValue(it)
                    if (state.clusterFillOpacityError == null) {
                        throttleClusterLiveApply(applyClusterSimpleStyles)
                    }
                },
                onValueTextChange = {
                    state.clusterFillOpacity = it
                    if (state.clusterFillOpacityError == null) applyClusterSimpleStyles()
                },
                onValueChangeFinished = applyClusterSimpleStyles,
                valueRange = 0f..1f,
                rangeStartLabel = "0",
                rangeEndLabel = "1",
                isError = state.clusterFillOpacityError != null,
                errorText = state.clusterFillOpacityError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_fill_opacity",
            )
            ColorPickerField(
                value = state.clusterStrokeColor,
                onValueChange = {
                    state.clusterStrokeColor = it
                    if (state.clusterStrokeColorError == null) applyClusterSimpleStyles()
                },
                label = stringResource(R.string.point_cluster_input_cluster_stroke_color_label),
                isError = state.clusterStrokeColorError != null,
                supportingText = state.clusterStrokeColorError?.let { { Text(it) } },
                allowManualInput = false,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_cluster_stroke_color",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_stroke_width_label),
                value = state.clusterStrokeWidth.toFloatOrNull() ?: 1f,
                valueText = state.clusterStrokeWidth,
                onValueChange = {
                    state.clusterStrokeWidth = formatDemoSliderValue(it)
                    if (state.clusterStrokeWidthError == null) {
                        throttleClusterLiveApply(applyClusterSimpleStyles)
                    }
                },
                onValueTextChange = {
                    state.clusterStrokeWidth = it
                    if (state.clusterStrokeWidthError == null) applyClusterSimpleStyles()
                },
                onValueChangeFinished = applyClusterSimpleStyles,
                valueRange = 0f..8f,
                rangeStartLabel = "0",
                rangeEndLabel = "8",
                isError = state.clusterStrokeWidthError != null,
                errorText = state.clusterStrokeWidthError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_stroke_width",
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.point_cluster_input_cluster_icon_steps_label),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.clusterCustomIconMode == ClusterCustomIconMode.STEPPED,
                    onCheckedChange = { stepped ->
                        state.clusterCustomIconMode =
                            if (stepped) ClusterCustomIconMode.STEPPED else ClusterCustomIconMode.UNIFIED
                        applyClusterSimpleStyles()
                    },
                    enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                    modifier = Modifier.testTag("point_cluster_switch_cluster_icon_steps"),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.point_cluster_input_cluster_adaptive_radius_label),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.clusterVisualRadiusMode == ClusterVisualRadiusMode.ADAPTIVE,
                onCheckedChange = { adaptive ->
                    state.clusterVisualRadiusMode =
                        if (adaptive) ClusterVisualRadiusMode.ADAPTIVE else ClusterVisualRadiusMode.FIXED
                    applyClusterRadiusCfg()
                },
                enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                modifier = Modifier.testTag("point_cluster_switch_cluster_adaptive_radius"),
            )
        }
        Text(
            text = stringResource(
                if (state.clusterVisualRadiusMode == ClusterVisualRadiusMode.ADAPTIVE) {
                    R.string.point_cluster_adaptive_radius_hint
                } else {
                    R.string.point_cluster_fixed_radius_hint
                },
            ),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.clusterVisualRadiusMode == ClusterVisualRadiusMode.FIXED) {
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_visual_radius_label),
                value = state.clusterVisualRadius.toFloatOrNull() ?: 24f,
                valueText = state.clusterVisualRadius,
                onValueChange = {
                    state.clusterVisualRadius = formatDemoSliderValue(it)
                    if (state.clusterVisualRadiusError == null) {
                        throttleClusterLiveApply(applyClusterRadiusCfg)
                    }
                },
                onValueTextChange = {
                    state.clusterVisualRadius = it
                    if (state.clusterVisualRadiusError == null) applyClusterRadiusCfg()
                },
                onValueChangeFinished = applyClusterRadiusCfg,
                valueRange = 8f..48f,
                rangeStartLabel = "8",
                rangeEndLabel = "48",
                isError = state.clusterVisualRadiusError != null,
                errorText = state.clusterVisualRadiusError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_visual_radius",
            )
        } else {
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_min_radius_label),
                value = state.clusterMinVisualRadius.toFloatOrNull() ?: 18f,
                valueText = state.clusterMinVisualRadius,
                onValueChange = {
                    state.clusterMinVisualRadius = formatDemoSliderValue(it)
                    if (state.clusterMinVisualRadiusError == null &&
                        state.clusterMaxVisualRadiusError == null
                    ) {
                        throttleClusterLiveApply(applyClusterRadiusCfg)
                    }
                },
                onValueTextChange = {
                    state.clusterMinVisualRadius = it
                    if (state.clusterMinVisualRadiusError == null &&
                        state.clusterMaxVisualRadiusError == null
                    ) {
                        applyClusterRadiusCfg()
                    }
                },
                onValueChangeFinished = applyClusterRadiusCfg,
                valueRange = 8f..48f,
                rangeStartLabel = "8",
                rangeEndLabel = "48",
                isError = state.clusterMinVisualRadiusError != null,
                errorText = state.clusterMinVisualRadiusError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_min_radius",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_max_radius_label),
                value = state.clusterMaxVisualRadius.toFloatOrNull() ?: 30f,
                valueText = state.clusterMaxVisualRadius,
                onValueChange = {
                    state.clusterMaxVisualRadius = formatDemoSliderValue(it)
                    if (state.clusterMaxVisualRadiusError == null) {
                        throttleClusterLiveApply(applyClusterRadiusCfg)
                    }
                },
                onValueTextChange = {
                    state.clusterMaxVisualRadius = it
                    if (state.clusterMaxVisualRadiusError == null) applyClusterRadiusCfg()
                },
                onValueChangeFinished = applyClusterRadiusCfg,
                valueRange = 8f..48f,
                rangeStartLabel = "8",
                rangeEndLabel = "48",
                isError = state.clusterMaxVisualRadiusError != null,
                errorText = state.clusterMaxVisualRadiusError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_max_radius",
            )
            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_cluster_max_reference_label),
                value = state.clusterRadiusMaxReference.toFloatOrNull() ?: 1000f,
                valueText = state.clusterRadiusMaxReference,
                onValueChange = {
                    state.clusterRadiusMaxReference = formatDemoSliderValue(it)
                    if (state.clusterRadiusMaxReferenceError == null) {
                        throttleClusterLiveApply(applyClusterRadiusCfg)
                    }
                },
                onValueTextChange = {
                    state.clusterRadiusMaxReference = it
                    if (state.clusterRadiusMaxReferenceError == null) applyClusterRadiusCfg()
                },
                onValueChangeFinished = applyClusterRadiusCfg,
                valueRange = 10f..5000f,
                rangeStartLabel = "10",
                rangeEndLabel = "5000",
                isError = state.clusterRadiusMaxReferenceError != null,
                errorText = state.clusterRadiusMaxReferenceError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_cluster_max_reference",
            )
        }

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_cluster_text_size_label),
            value = state.clusterTextSize.toFloatOrNull() ?: 12f,
            valueText = state.clusterTextSize,
            onValueChange = {
                state.clusterTextSize = formatDemoSliderValue(it)
                if (state.clusterTextSizeError == null) {
                    throttleClusterLiveApply(applyClusterSimpleStyles)
                }
            },
            onValueTextChange = {
                state.clusterTextSize = it
                if (state.clusterTextSizeError == null) applyClusterSimpleStyles()
            },
            onValueChangeFinished = applyClusterSimpleStyles,
            valueRange = 8f..32f,
            rangeStartLabel = "8",
            rangeEndLabel = "32",
            isError = state.clusterTextSizeError != null,
            errorText = state.clusterTextSizeError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_cluster_text_size",
        )
        ColorPickerField(
            value = state.clusterTextColor,
            onValueChange = {
                state.clusterTextColor = it
                if (state.clusterTextColorError == null) applyClusterSimpleStyles()
            },
            label = stringResource(R.string.point_cluster_input_cluster_text_color_label),
            isError = state.clusterTextColorError != null,
            supportingText = state.clusterTextColorError?.let { { Text(it) } },
            allowManualInput = false,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_cluster_text_color",
        )

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_cluster_layer_min_zoom_label),
            value = state.clusterLayerMinZoom.toFloatOrNull() ?: 0f,
            valueText = state.clusterLayerMinZoom,
            onValueChange = {
                state.clusterLayerMinZoom = formatDemoSliderValue(it)
                if (state.clusterLayerMinZoomError == null) {
                    throttleClusterLiveApply(applyClusterMinZoom)
                }
            },
            onValueTextChange = {
                state.clusterLayerMinZoom = it
                if (state.clusterLayerMinZoomError == null) applyClusterMinZoom()
            },
            onValueChangeFinished = applyClusterMinZoom,
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            isError = state.clusterLayerMinZoomError != null,
            errorText = state.clusterLayerMinZoomError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_cluster_layer_min_zoom",
        )
        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_cluster_layer_max_zoom_label),
            value = state.clusterLayerMaxZoom.toFloatOrNull() ?: 16f,
            valueText = state.clusterLayerMaxZoom,
            onValueChange = {
                state.clusterLayerMaxZoom = formatDemoSliderValue(it)
                if (state.clusterLayerMaxZoomError == null) {
                    throttleClusterLiveApply(applyClusterMaxZoom)
                }
            },
            onValueTextChange = {
                state.clusterLayerMaxZoom = it
                if (state.clusterLayerMaxZoomError == null) applyClusterMaxZoom()
            },
            onValueChangeFinished = applyClusterMaxZoom,
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            isError = state.clusterLayerMaxZoomError != null,
            errorText = state.clusterLayerMaxZoomError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_cluster_layer_max_zoom",
        )
    }

    // ---- 单点样式（默认收起）----
    var leafStyleExpanded by remember { mutableStateOf(false) }
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { leafStyleExpanded = !leafStyleExpanded }
            .testTag("point_cluster_leaf_style_toggle"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.point_cluster_leaf_style_section),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                if (leafStyleExpanded) R.string.panel_collapse else R.string.panel_expand,
            ),
        )
    }

    if (leafStyleExpanded) {
        // 统一图标：关=针图 Fixed / 开=红点 Fixed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.point_cluster_input_leaf_icon_mode_label),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.leafIconMode == LeafIconMode.FIXED,
                onCheckedChange = { fixed ->
                    state.leafIconMode = if (fixed) LeafIconMode.FIXED else LeafIconMode.BY_FEATURE
                    applyLeafIconMode()
                },
                enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                modifier = Modifier.testTag("point_cluster_switch_leaf_icon_mode"),
            )
        }

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_leaf_icon_size_label),
            value = state.leafIconSize.toFloatOrNull() ?: 1f,
            valueText = state.leafIconSize,
            onValueChange = { state.leafIconSize = formatDemoSliderValue(it) },
            onValueTextChange = {
                state.leafIconSize = it
                if (state.leafIconSizeError == null) applyLeafPoints()
            },
            onValueChangeFinished = applyLeafPoints,
            valueRange = 0.1f..4f,
            rangeStartLabel = "0.1",
            rangeEndLabel = "4",
            isError = state.leafIconSizeError != null,
            errorText = state.leafIconSizeError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_leaf_icon_size",
        )

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_leaf_icon_rotation_label),
            value = state.leafIconRotation.toFloatOrNull() ?: 0f,
            valueText = state.leafIconRotation,
            onValueChange = { state.leafIconRotation = formatDemoSliderValue(it) },
            onValueTextChange = {
                state.leafIconRotation = it
                if (it.toFloatOrNull() != null) applyLeafPoints()
            },
            onValueChangeFinished = applyLeafPoints,
            valueRange = 0f..360f,
            rangeStartLabel = "0",
            rangeEndLabel = "360",
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_leaf_icon_rotation",
        )

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_leaf_icon_opacity_label),
            value = state.leafIconOpacity.toFloatOrNull() ?: 1f,
            valueText = state.leafIconOpacity,
            onValueChange = { state.leafIconOpacity = formatDemoSliderValue(it) },
            onValueTextChange = {
                state.leafIconOpacity = it
                if (state.leafIconOpacityError == null) applyLeafPoints()
            },
            onValueChangeFinished = applyLeafPoints,
            valueRange = 0f..1f,
            rangeStartLabel = "0",
            rangeEndLabel = "1",
            isError = state.leafIconOpacityError != null,
            errorText = state.leafIconOpacityError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_leaf_icon_opacity",
        )

        // 显示文字（ByFeature=点 id；Fixed=统一文案）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.point_cluster_input_leaf_show_text_label),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.leafShowText,
                onCheckedChange = {
                    state.leafShowText = it
                    applyLeafPoints()
                },
                enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                modifier = Modifier.testTag("point_cluster_switch_leaf_show_text"),
            )
        }

        if (state.leafShowText) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.point_cluster_input_leaf_text_mode_label),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.leafTextMode == LeafTextMode.FIXED,
                    onCheckedChange = { fixed ->
                        state.leafTextMode =
                            if (fixed) LeafTextMode.FIXED else LeafTextMode.BY_FEATURE
                        applyLeafPoints()
                    },
                    enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                    modifier = Modifier.testTag("point_cluster_switch_leaf_text_mode"),
                )
            }

            if (state.leafTextMode == LeafTextMode.FIXED) {
                OutlinedTextField(
                    value = state.leafFixedText,
                    onValueChange = {
                        state.leafFixedText = it
                        applyLeafPoints()
                    },
                    label = {
                        Text(stringResource(R.string.point_cluster_input_leaf_fixed_text_label))
                    },
                    singleLine = true,
                    enabled = lastOverlay != null && lastOverlay.isRemoved.not(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("point_cluster_input_leaf_fixed_text"),
                )
            }

            DemoNumericSliderField(
                label = stringResource(R.string.point_cluster_input_leaf_text_size_label),
                value = state.leafTextSize.toFloatOrNull() ?: 12f,
                valueText = state.leafTextSize,
                onValueChange = { state.leafTextSize = formatDemoSliderValue(it) },
                onValueTextChange = {
                    state.leafTextSize = it
                    if (state.leafTextSizeError == null) applyLeafPoints()
                },
                onValueChangeFinished = applyLeafPoints,
                valueRange = 8f..32f,
                rangeStartLabel = "8",
                rangeEndLabel = "32",
                isError = state.leafTextSizeError != null,
                errorText = state.leafTextSizeError,
                modifier = Modifier.padding(top = 12.dp),
                testTag = "point_cluster_slider_leaf_text_size",
            )
        }

        // 单点可见 zoom：松手后再 applyOption，避免拖动时每帧 nativeSetOption 触发 bad_alloc
        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_point_min_zoom_label),
            value = state.pointMinZoom.toFloatOrNull() ?: 0f,
            valueText = state.pointMinZoom,
            onValueChange = { state.pointMinZoom = formatDemoSliderValue(it) },
            onValueTextChange = {
                state.pointMinZoom = it
                if (state.pointMinZoomError == null) applyLeafMinZoom()
            },
            onValueChangeFinished = applyLeafMinZoom,
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            isError = state.pointMinZoomError != null,
            errorText = state.pointMinZoomError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_point_min_zoom",
        )

        DemoNumericSliderField(
            label = stringResource(R.string.point_cluster_input_point_max_zoom_label),
            value = state.pointMaxZoom.toFloatOrNull() ?: 22f,
            valueText = state.pointMaxZoom,
            onValueChange = { state.pointMaxZoom = formatDemoSliderValue(it) },
            onValueTextChange = {
                state.pointMaxZoom = it
                if (state.pointMaxZoomError == null) applyLeafMaxZoom()
            },
            onValueChangeFinished = applyLeafMaxZoom,
            valueRange = 0f..22f,
            rangeStartLabel = "0",
            rangeEndLabel = "22",
            isError = state.pointMaxZoomError != null,
            errorText = state.pointMaxZoomError,
            modifier = Modifier.padding(top = 12.dp),
            testTag = "point_cluster_slider_point_max_zoom",
        )
    }
}

@Composable
internal fun DrawPointClusterButton(
    state: PointClusterDemoState,
    mapView: MapView?,
    onOverlayDrawn: (Cluster, MaptecMap) -> Unit,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    DemoPanelButton(
        onClick = {
            if (!state.isValid) return@DemoPanelButton
            val view = mapView ?: return@DemoPanelButton
            view.getMapAsync { map ->
                drawPointClusterOnMap(context, map, state)?.let { onOverlayDrawn(it, map) }
            }
            configPanelController.close()
        },
        enabled = state.isValid && mapView != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("point_cluster_draw_button"),
    ) {
        Text(stringResource(R.string.point_cluster_draw_button))
    }
}

@Composable
internal fun LoadExternalPointClusterButton(
    state: PointClusterDemoState,
    mapView: MapView?,
    lastOverlay: Cluster?,
    onOverlayDrawn: (Cluster, MaptecMap) -> Unit,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    DemoPanelButton(
        onClick = {
            if (!state.isValid) return@DemoPanelButton
            val view = mapView ?: return@DemoPanelButton
            val loadResult = loadClusterPointsFromAssets(context)
            val points = when (loadResult) {
                is ClusterGeoJsonParseResult.Ok -> loadResult.points
                is ClusterGeoJsonParseResult.Err -> {
                    val err = loadResult.error
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.point_cluster_load_external_failed_detail,
                            err.code.name,
                            err.message,
                            err.invalidFeatureCount ?: 0,
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@DemoPanelButton
                }
            }
            view.getMapAsync { map ->
                val overlay = lastOverlay
                if (overlay != null) {
                    overlay.setPoints(points)
                    overlay.setClusterEnabled(state.clusterEnable)
                    state.clusterRadius.toIntOrNull()?.let { overlay.setClusterRadius(it) }
                    state.clusterMaxZoom.toDoubleOrNull()?.let { overlay.setClusterMaxZoom(it) }
                    state.clusterMinPoints.toIntOrNull()?.let { overlay.setClusterMinPoints(it.toLong()) }
                    overlay.setClusterCountAbbreviate(state.countAbbreviate)
                    onOverlayDrawn(overlay, map)
                } else {
                    drawPointClusterOnMap(context, map, state, points)?.let {
                        onOverlayDrawn(it, map)
                    }
                }
            }
            configPanelController.close()
        },
        enabled = state.isValid && mapView != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("point_cluster_load_external_button"),
    ) {
        Text(stringResource(R.string.point_cluster_load_external_button))
    }
}

@Composable
internal fun RemovePointClusterButton(
    mapView: MapView?,
    lastOverlay: Cluster?,
    onBeforeFirstRemove: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    DemoPanelButton(
        onClick = {
            val view = mapView ?: return@DemoPanelButton
            val overlay = lastOverlay ?: return@DemoPanelButton
            view.getMapAsync { map ->
                val wasRemoved = overlay.isRemoved
                if (!wasRemoved) {
                    onBeforeFirstRemove?.invoke()
                }
                map.getOverlayEngine().deleteClusterOverlay(overlay)
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.point_cluster_remove_result,
                        overlay.lifecycleState.name,
                        wasRemoved,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            configPanelController.close()
        },
        enabled = mapView != null && lastOverlay != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("point_cluster_remove_button"),
    ) {
        Text(stringResource(R.string.point_cluster_remove_button))
    }
}
