package com.maptec.applied.demo.ui.screens.overlays.point_cluster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.maptec.applied.demo.R
import com.maptec.applied.maps.overlay.cluster.util.ClusterGeoJson
import com.maptec.applied.maps.overlay.cluster.ClusterLifecycleError
import com.maptec.applied.maps.overlay.cluster.ClusterPoint

internal sealed class ClusterGeoJsonParseResult {
    data class Ok(val points: List<ClusterPoint>) : ClusterGeoJsonParseResult()
    data class Err(val error: ClusterLifecycleError) : ClusterGeoJsonParseResult()
}

/**
 * 从 assets 加载外部点聚合 GeoJSON；校验失败返回 SDK 标准错误摘要，由调用方保留旧数据。
 */
internal fun loadClusterPointsFromAssets(context: Context): ClusterGeoJsonParseResult {
    val json = try {
        context.assets.open(CLUSTER_SAMPLE_ASSET).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return ClusterGeoJsonParseResult.Err(
            ClusterLifecycleError.of(
                ClusterLifecycleError.Code.CLUSTER_INVALID_GEOJSON,
                context.getString(R.string.point_cluster_error_geojson_read),
            ),
        )
    }
    val leafIcon = loadAssetLeafIcon(context, R.drawable.default_pin)
    return when (val result = ClusterGeoJson.parse(json, leafIcon, ClusterGeoJson.DEFAULT_MAX_FEATURES)) {
        is ClusterGeoJson.Result.Success -> ClusterGeoJsonParseResult.Ok(result.points)
        is ClusterGeoJson.Result.Failure -> ClusterGeoJsonParseResult.Err(result.error)
        else -> ClusterGeoJsonParseResult.Err(
            ClusterLifecycleError.of(
                ClusterLifecycleError.Code.CLUSTER_INTERNAL_ERROR,
                context.getString(R.string.point_cluster_error_geojson_unknown),
            ),
        )
    }
}

private fun loadAssetLeafIcon(context: Context, @DrawableRes res: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, res) ?: return null
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
