package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.util.SphericalUtil
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerCollisionBehavior
import com.maptec.applied.maps.overlay.marker.MarkerLayerOptions
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.random.Random

class MarkerCollisionViewModel : ViewModel() {

    private val _markers = MutableStateFlow<List<Marker>>(emptyList())
    val markers: StateFlow<List<Marker>> = _markers.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _collisionBehavior = MutableStateFlow(MarkerCollisionBehavior.REQUIRED)
    val collisionBehavior: StateFlow<MarkerCollisionBehavior> = _collisionBehavior.asStateFlow()

    private val _zOrder = MutableStateFlow(DEFAULT_Z_ORDER)
    val zOrder: StateFlow<Int> = _zOrder.asStateFlow()

    private var mapRef: MaptecMap? = null
    private var contextRef: Context? = null
    private var mapStyleReady = false
    private var markerLayerConfigured = false
    private var hub: LatLng? = null
    private var addCount = 0

    fun setCollisionBehavior(behavior: MarkerCollisionBehavior) {
        _collisionBehavior.value = behavior
    }

    fun setZOrder(value: Int) {
        _zOrder.value = value.coerceIn(0, 999)
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun initMap(context: Context, map: MaptecMap) {
        contextRef = context.applicationContext
        mapRef = map
        map.getStyle {
            mapStyleReady = true
            ensureMarkerLayerCollision(map)
            if (hub == null) {
                hub = POI_HUB
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(POI_HUB, DEMO_ZOOM))
            }
        }
    }

    fun addMarker() {
        val context = contextRef ?: run {
            _toastMessage.value = "地图未就绪"
            return
        }
        val map = mapRef ?: run {
            _toastMessage.value = "地图未就绪"
            return
        }
        if (!mapStyleReady) {
            _toastMessage.value = "地图样式加载中，请稍后再试"
            return
        }
        ensureMarkerLayerCollision(map)

        val center = hub ?: POI_HUB.also { hub = it }
        val index = addCount++
        val latLng = latLngForMarkerIndex(center, index)

        val behavior = _collisionBehavior.value
        val label = behaviorLabel(behavior, index)
        val opts = MarkerOptions()
            .withLatLng(latLng)
            .withIconResource(R.drawable.default_pin, context)
            .withIconSize(ICON_SIZE)
            .withIconAnchor(MarkerAnchorType.BOTTOM)
            .withText(label)
            .withTextSize(14)
            .withTextColor(AndroidColor.parseColor("#1A1A1A"))
            .withTextAnchor(MarkerAnchorType.TOP)
            .withTextOffset(0f, 0.5f)
            .withCollisionBehavior(behavior)
            .withZOrder(_zOrder.value)

        val marker = map.overlayEngine.addMarker(opts)
        _markers.value = _markers.value + marker
        map.triggerRepaint()
        _toastMessage.value = "已添加 $label @ zOrder=${_zOrder.value}"
    }

    fun clearAllMarkers() {
        val map = mapRef ?: return
        map.overlayEngine.deleteAllMarkers()
        _markers.value = emptyList()
        addCount = 0
        hub = POI_HUB
        _toastMessage.value = "已清除全部 Marker"
    }

    private fun ensureMarkerLayerCollision(map: MaptecMap) {
        if (markerLayerConfigured) return
        map.overlayEngine.configureMarkerLayer(
            MarkerLayerOptions()
                .withCollisionEnabled(true)
                .withVisible(true),
        )
        markerLayerConfigured = true
    }

    private fun behaviorLabel(behavior: MarkerCollisionBehavior, index: Int): String {
        val prefix = when (behavior) {
            MarkerCollisionBehavior.REQUIRED -> "REQ"
            MarkerCollisionBehavior.OPTIONAL_AND_HIDES_LOWER_PRIORITY -> "OPT"
            MarkerCollisionBehavior.REQUIRED_AND_HIDES_OPTIONAL -> "RHO"
        }
        return "$prefix-${index + 1}"
    }

    /**
     * 第 1 个 Marker 固定在 [hub]；之后每个点在 hub 为圆心、半径 [RANDOM_RADIUS_METERS] 的圆盘内随机一点
     *（均匀面积采样：r = sqrt(u)·R，θ ∈ [0, 360)）。
     */
    private fun latLngForMarkerIndex(hub: LatLng, index: Int): LatLng {
        if (index == 0) return hub
        val headingDeg = Random.nextDouble(0.0, 360.0)
        val distanceM = sqrt(Random.nextDouble()) * RANDOM_RADIUS_METERS
        return SphericalUtil.computeOffset(hub, distanceM, headingDeg)
    }

    companion object {
        /** 乌节路一带，底图 POI / 道路名称较密，便于观察与底图避让。 */
        val POI_HUB = LatLng(1.3048, 103.8319)
        private const val DEMO_ZOOM = 16.5
        private const val ICON_SIZE = 1.8f
        /** 第 2 个及以后 Marker 相对第一个点的最大距离（米）。 */
        private const val RANDOM_RADIUS_METERS = 1000.0
        private const val DEFAULT_Z_ORDER = 0
    }
}

class MarkerCollisionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkerCollisionViewModel::class.java)) {
            return MarkerCollisionViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
