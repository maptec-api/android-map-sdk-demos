package com.maptec.applied.demo.ui.screens.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.data.GeofenceData
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.fill.FillOptions
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Polygon as GeoPolygon

/**
 * Demo：[MaptecMap.setCameraTargetBounds] / [MaptecMap.getCameraTargetBounds]。
 *
 * 进入即绘制默认矩形区域；配置面板仅「设置 / 取消」边界限制。
 */
@Composable
fun CameraTargetBoundsScreen(
    modifier: Modifier = Modifier,
) {
    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var regionBounds by remember { mutableStateOf<LatLngBounds?>(null) }
    var cameraBoundsEnabled by remember { mutableStateOf(false) }

    fun setBoundsEnabled(enabled: Boolean) {
        val map = mapRef ?: return
        val bounds = regionBounds ?: return
        cameraBoundsEnabled = enabled
        map.setCameraTargetBounds(if (enabled) bounds else null)
    }

    DemoPanelScaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CameraTargetBoundsDemo.TAG_SCREEN)
            .then(modifier),
        sheetContent = {
            DemoPanelColumn {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.camera_target_bounds_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    DemoPanelButton(
                        onClick = { setBoundsEnabled(true) },
                        enabled = regionBounds != null && !cameraBoundsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CameraTargetBoundsDemo.TAG_ENABLE_BUTTON),
                    ) {
                        Text(stringResource(R.string.camera_target_bounds_set))
                    }
                    DemoPanelButton(
                        onClick = { setBoundsEnabled(false) },
                        enabled = regionBounds != null && cameraBoundsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CameraTargetBoundsDemo.TAG_DISABLE_BUTTON),
                    ) {
                        Text(stringResource(R.string.camera_target_bounds_cancel))
                    }
                }
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = CameraPosition.Builder()
                    .target(CameraTargetBoundsDemo.INITIAL_TARGET)
                    .zoom(15.0)
                    .build(),
                onMapReady = { _, map ->
                    map.getStyle {
                        val engine = map.getOverlayEngine()
                        engine.deleteAllFills()

                        parseRegionPolygon(GeofenceData.POLYGON_GEOFENCE_JSON)?.let { region ->
                            engine.addPolygon(buildFillOptions(region))
                            regionBounds = region.bounds
                            mapRef = map
                            // 默认开启边界限制
                            cameraBoundsEnabled = true
                            map.setCameraTargetBounds(region.bounds)
                        }
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                CameraTargetBoundsDemo.INITIAL_TARGET,
                                15.0,
                            ),
                        )
                        mapRef = map
                    }
                },
            )
        },
    )
}

private data class RegionPolygon(
    val rings: List<List<LatLng>>,
) {
    val bounds: LatLngBounds
        get() = LatLngBounds.Builder().includes(rings.first()).build()
}

private fun parseRegionPolygon(json: String): RegionPolygon? {
    val feature = runCatching { Feature.fromJson(json) }.getOrNull() ?: return null
    val geom = feature.geometry() as? GeoPolygon ?: return null
    val rings = geom.coordinates()?.map { ring ->
        ring.map { LatLng(it.latitude(), it.longitude()) }
    } ?: return null
    if (rings.isEmpty() || rings.first().size < 4) return null
    return RegionPolygon(rings)
}

private fun buildFillOptions(data: RegionPolygon): FillOptions =
    FillOptions()
        .withLatLngs(data.rings)
        .withFillColor("#3A5EFB")
        .withFillOpacity(0.25f)
        .withStrokeColor("#3A5EFB")
        .withStrokeWeight(3f)
        .withStrokeOpacity(1f)
        .withFillAntialias(true)

/** CameraTargetBoundsScreen 演示常量（测试与 UI 共用）。 */
object CameraTargetBoundsDemo {
    const val TAG_SCREEN = "camera_target_bounds_screen"
    const val TAG_ENABLE_BUTTON = "camera_target_bounds_enable_button"
    const val TAG_DISABLE_BUTTON = "camera_target_bounds_disable_button"

    /** 园区矩形中心附近。 */
    val INITIAL_TARGET = LatLng(39.9075, 116.3925)

    val bounds: LatLngBounds
        get() = parseRegionPolygon(GeofenceData.POLYGON_GEOFENCE_JSON)!!.bounds
}
