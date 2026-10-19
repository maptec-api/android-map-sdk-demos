package com.maptec.applied.demo.ui.screens.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

/**
 * Demo：限制显示区域。
 *
 * 开启时：
 * 1. [CameraUpdateFactory.newLatLngBounds] 把视野 fit 到目标矩形（同类 fitBounds）
 * 2. [MaptecMap.setCameraTargetBounds] 引擎硬约束相机中心（撞墙，不可先出再弹回）
 * 3. 配合 minZoom，避免缩放过小导致约束失效感过强
 */
@Composable
fun LimitBoundsScreen(
    modifier: Modifier = Modifier,
) {
    var isBoundsLimited by remember { mutableStateOf(false) }
    var isStyleRendered by remember { mutableStateOf(false) }
    val bounds = remember { LimitBoundsDemo.bounds }

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(
                CameraPosition.Builder()
                    .target(LimitBoundsDemo.INITIAL_TARGET)
                    .zoom(LimitBoundsDemo.INITIAL_ZOOM)
                    .build(),
            )
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: com.maptec.applied.maps.Style?) {}
                    override fun onStyleRendered(style: com.maptec.applied.maps.Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: com.maptec.applied.maps.Style?, message: String) {}
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                Text(
                    text = stringResource(R.string.limit_bounds_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                DemoPanelButton(
                    onClick = {
                        mapView.getMapAsync { map ->
                            if (!isBoundsLimited) {
                                map.setMinZoomPreference(LimitBoundsDemo.LIMITED_MIN_ZOOM)
                                // fitBounds：先把视野收进限制矩形
                                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
                                // 引擎硬约束相机中心 → 拖到边界撞墙
                                map.setCameraTargetBounds(bounds)
                                isBoundsLimited = true
                            } else {
                                map.setCameraTargetBounds(null)
                                map.setMinZoomPreference(LimitBoundsDemo.UNLIMITED_MIN_ZOOM)
                                isBoundsLimited = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBoundsLimited) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(50.dp)
                        .testTag(LimitBoundsDemo.TAG_TOGGLE_BUTTON),
                ) {
                    Text(
                        text = if (isBoundsLimited) {
                            stringResource(R.string.limit_bounds_cancel)
                        } else {
                            stringResource(R.string.limit_bounds_set)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Text(
                    text = if (isBoundsLimited) {
                        stringResource(R.string.limit_bounds_status_enabled)
                    } else {
                        stringResource(R.string.limit_bounds_status_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .testTag(LimitBoundsDemo.TAG_STATUS),
                )
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("mapView"),
                )

                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
        modifier = modifier,
    )
}

/** LimitBoundsScreen 演示常量（测试与 UI 共用）。 */
object LimitBoundsDemo {
    const val TAG_TOGGLE_BUTTON = "limit_bounds_toggle_button"
    const val TAG_STATUS = "limit_bounds_status"

    val SOUTHWEST = LatLng(1.2, 103.6)
    val NORTHEAST = LatLng(1.5, 104.0)
    val INITIAL_TARGET = LatLng(1.35, 103.8)
    const val INITIAL_ZOOM = 11.0
    const val LIMITED_MIN_ZOOM = 10.0
    const val UNLIMITED_MIN_ZOOM = 0.0

    val bounds: LatLngBounds
        get() = LatLngBounds.Builder()
            .include(SOUTHWEST)
            .include(NORTHEAST)
            .build()
}
