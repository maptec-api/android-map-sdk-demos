package com.maptec.applied.demo.ui.screens.overlays.point_cluster

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.maptec.applied.demo.R
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.cluster.Cluster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointClusterBasicScreen() {
    val context = LocalContext.current
    val state = rememberPointClusterDemoState()
    var lastOverlay by remember { mutableStateOf<Cluster?>(null) }
    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }

    val clusterClickListener = remember {
        Cluster.OnClickListener { _, event ->
            val hit = event.hit
            val feature = hit.featureData
            val clusterId = feature?.getValue("cluster_id")?.toString() ?: hit.id
            val pointCount = feature?.getValue("point_count")?.toString() ?: "-"
            val zoom = mapRef?.cameraPosition?.zoom ?: Double.NaN
            val mapMaxZoom = mapRef?.maxZoomLevel ?: Double.NaN
            val clusterMaxZoom = lastOverlay?.clusterMaxZoom
                ?: state.clusterMaxZoom.toDoubleOrNull()
                ?: 16.0
            val anchor = feature?.anchor
            val message = if (event.isExpansionLimited) {
                context.getString(
                    R.string.point_cluster_callback_cluster_expansion_limited,
                    zoom,
                    clusterMaxZoom,
                    mapMaxZoom,
                    clusterId,
                    pointCount,
                )
            } else {
                context.getString(
                    R.string.point_cluster_callback_cluster,
                    clusterId,
                    pointCount,
                    zoom,
                    anchor?.latitude ?: Double.NaN,
                    anchor?.longitude ?: Double.NaN,
                )
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            true
        }
    }

    val lifecycleListener = remember {
        Cluster.OnLifecycleListener { overlay, previousState, newState, error ->
            if (previousState == newState && newState == Cluster.LifecycleState.LOADING) {
                return@OnLifecycleListener
            }
            // 滑块实时改样式会频繁 LOADING/READY，抑制 Toast 刷屏；错误仍提示。
            if (state.suppressLifecycleToast) {
                when (newState) {
                    Cluster.LifecycleState.LOADING -> return@OnLifecycleListener
                    Cluster.LifecycleState.READY,
                    Cluster.LifecycleState.EMPTY,
                    -> {
                        state.suppressLifecycleToast = false
                        return@OnLifecycleListener
                    }
                    Cluster.LifecycleState.ERROR -> state.suppressLifecycleToast = false
                    Cluster.LifecycleState.REMOVED -> state.suppressLifecycleToast = false
                }
            }
            val message = when (newState) {
                Cluster.LifecycleState.LOADING ->
                    context.getString(R.string.point_cluster_lifecycle_loading)
                Cluster.LifecycleState.READY ->
                    context.getString(R.string.point_cluster_lifecycle_ready)
                Cluster.LifecycleState.EMPTY ->
                    context.getString(R.string.point_cluster_lifecycle_empty)
                Cluster.LifecycleState.ERROR ->
                    context.getString(
                        R.string.point_cluster_lifecycle_error_detail,
                        error?.code?.name ?: "-",
                        error?.message ?: "-",
                        error?.isRetryable == true,
                    )
                Cluster.LifecycleState.REMOVED ->
                    context.getString(R.string.point_cluster_lifecycle_removed)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val warningListener = remember {
        Cluster.OnWarningListener { _, warning ->
            Toast.makeText(
                context,
                context.getString(
                    R.string.point_cluster_warning_detail,
                    warning.code.name,
                    warning.message,
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun bindOverlay(overlay: Cluster, map: MaptecMap) {
        lastOverlay?.removeOnClickListener(clusterClickListener)
        lastOverlay?.removeOnLifecycleListener(lifecycleListener)
        lastOverlay?.removeOnWarningListener(warningListener)
        lastOverlay = overlay
        mapRef = map
        overlay.setClickable(true)
        overlay.addOnClickListener(clusterClickListener)
        overlay.addOnLifecycleListener(lifecycleListener)
        overlay.addOnWarningListener(warningListener)
    }

    fun unbindOverlay(overlay: Cluster) {
        overlay.removeOnClickListener(clusterClickListener)
        overlay.removeOnLifecycleListener(lifecycleListener)
        overlay.removeOnWarningListener(warningListener)
    }

    PointClusterScaffold(
        state = state,
        onOverlayDrawn = { overlay, map -> bindOverlay(overlay, map) },
        sheetContent = { mapView, scaffoldOverlay ->
            val activeOverlay = lastOverlay ?: scaffoldOverlay
            PointClusterParameterInputs(
                state = state,
                mapView = mapView,
                lastOverlay = activeOverlay,
                onOverlayDrawn = { overlay, map -> bindOverlay(overlay, map) },
            )
            DrawPointClusterButton(
                state = state,
                mapView = mapView,
                onOverlayDrawn = { overlay, map -> bindOverlay(overlay, map) },
            )
            LoadExternalPointClusterButton(
                state = state,
                mapView = mapView,
                lastOverlay = activeOverlay,
                onOverlayDrawn = { overlay, map -> bindOverlay(overlay, map) },
            )
            RemovePointClusterButton(
                mapView = mapView,
                lastOverlay = activeOverlay,
                onBeforeFirstRemove = { activeOverlay?.let { unbindOverlay(it) } },
            )
        },
    )
}
