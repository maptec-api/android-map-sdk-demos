package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.OnMarkerDragListener
import com.maptec.applied.style.layers.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

/**
 * MarkerLayerViewModel —— 基于 SDK 新分层的 marker API：
 *
 *   map.getOverlayEngine().addMarker(MarkerOptions...) → Marker
 *   marker.addOnClickListener / addOnDragListener / setIconSize / remove()
 */
class MarkerLayerViewModel : ViewModel() {

    // ========== 标记管理状态 ==========
    private val _markers = MutableStateFlow<List<Marker>>(emptyList())
    val markers: StateFlow<List<Marker>> = _markers.asStateFlow()

    private val _selectedMarkerId = MutableStateFlow<String?>(null)
    val selectedMarkerId: StateFlow<String?> = _selectedMarkerId.asStateFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _dragCompletedCount = MutableStateFlow(0)
    val dragCompletedCount: StateFlow<Int> = _dragCompletedCount.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ========== 公共参数 ==========
    private val _defaultLatLng = MutableStateFlow("1.4, 103.75")
    val defaultLatLng: StateFlow<String> = _defaultLatLng.asStateFlow()

    private val _iconOpacity = MutableStateFlow(DEFAULT_ICON_OPACITY)
    val iconOpacity: StateFlow<Float> = _iconOpacity.asStateFlow()

    private val _iconAnchor = MutableStateFlow(Property.ICON_ANCHOR_BOTTOM)
    val iconAnchor: StateFlow<String> = _iconAnchor.asStateFlow()

    private val _iconScaleWithZoom = MutableStateFlow(false)
    val iconScaleWithZoom: StateFlow<Boolean> = _iconScaleWithZoom.asStateFlow()

    private val _iconSize = MutableStateFlow(DEFAULT_ICON_SIZE)
    val iconSize: StateFlow<Float> = _iconSize.asStateFlow()

    private val _minScale = MutableStateFlow(0.1f)
    val minScale: StateFlow<Float> = _minScale.asStateFlow()

    private val _minZoom = MutableStateFlow(8)
    val minZoom: StateFlow<Int> = _minZoom.asStateFlow()

    private val _maxZoom = MutableStateFlow(16)
    val maxZoom: StateFlow<Int> = _maxZoom.asStateFlow()

    // ========== 按图标地址添加 Marker ==========
    private val _iconUrl = MutableStateFlow("https://hellorfimg.zcool.cn/preview260/1291884748.jpg?x-image-process=image/resize,h_380")
    val iconUrl: StateFlow<String> = _iconUrl.asStateFlow()

    private val _iconUrlId = MutableStateFlow("url_icon")
    val iconUrlId: StateFlow<String> = _iconUrlId.asStateFlow()

    // ========== 按图标类型添加 Marker ==========
    private val _iconType = MutableStateFlow(DEFAULT_PIN)
    val iconType: StateFlow<String> = _iconType.asStateFlow()

    // ========== SDF 可修改颜色 Marker ==========
    private val _sdfIconColor = MutableStateFlow(DEFAULT_ICON_COLOR)
    val sdfIconColor: StateFlow<String> = _sdfIconColor.asStateFlow()

    private var mapLibreMapRef: MaptecMap? = null
    private var mapViewRef: MapView? = null
    private var contextRef: Context? = null
    /** 类型预置图缓存 —— addMarker 时通过 MarkerOptions.withIcon 一次塞进 SDK image registry。 */
    private val typePresetBitmaps = mutableMapOf<String, Pair<Bitmap, Boolean>>()
    private var zoomListener: MaptecMap.OnCameraMoveListener? = null

    companion object {
        private const val TAG = "MarkerLayerVM"
        private const val MARKER_ICON_ID = "marker-icon"
        private const val DEFAULT_PIN = "default_pin"
        private const val BLUE_DOT = "blue_dot"
        private const val RED_DOT = "red_dot"
        private const val YELLOW_DOT = "yellow_dot"
        private const val GREEN_DOT = "green_dot"
        private const val DEFAULT_ICON_SIZE = 2.0f
        private const val SELECTED_ICON_SIZE = 3.0f
        private const val DEFAULT_ICON_OPACITY = 1.0f
        private const val SELECTED_ICON_OPACITY = 0.5f
        private const val DEFAULT_ICON_COLOR = "#3A5EFB"
        private const val SELECTED_ICON_COLOR = "#FF0000"

        val ICON_TYPE_OPTIONS = listOf(
            DEFAULT_PIN to "default_pin",
            BLUE_DOT to "blue_dot",
            RED_DOT to "red_dot",
            YELLOW_DOT to "yellow_dot",
            GREEN_DOT to "green_dot",
        )
    }

    // ========== 公共参数 Setter ==========
    fun setDefaultLatLng(value: String) { _defaultLatLng.value = value }
    fun setIconOpacity(value: Float) { _iconOpacity.value = value.coerceIn(0f, 1f) }
    fun setIconAnchor(value: String) { _iconAnchor.value = value.ifBlank { Property.ICON_ANCHOR_BOTTOM } }
    fun setIconSize(value: Float) { _iconSize.value = value.coerceIn(0.1f, 5f) }

    fun setIconScaleWithZoom(enabled: Boolean) {
        _iconScaleWithZoom.value = enabled
        applyIconSizeForCurrentZoom()
    }

    fun setMinZoom(value: Int)  { _minZoom.value = value;  applyIconSizeForCurrentZoom() }
    fun setMaxZoom(value: Int)  { _maxZoom.value = value;  applyIconSizeForCurrentZoom() }
    fun setMinScale(value: Float) { _minScale.value = value; applyIconSizeForCurrentZoom() }

    fun setIconUrl(value: String) { _iconUrl.value = value }
    fun setIconUrlId(value: String) { _iconUrlId.value = value.ifBlank { "url_icon" } }
    fun setIconType(value: String) { _iconType.value = value.ifBlank { DEFAULT_PIN } }
    fun setSdfIconColor(value: String) { _sdfIconColor.value = value.ifBlank { DEFAULT_ICON_COLOR } }

    // ========== Marker 添加 ==========

    fun addMarkerByUrl() {
        val latLng = parseLatLng(_defaultLatLng.value)
        if (latLng == null) { _toastMessage.value = "请输入有效的位置坐标 (lat,lng)"; return }
        val url = _iconUrl.value.trim()
        if (url.isEmpty()) { _toastMessage.value = "请输入图标地址"; return }
        val id = _iconUrlId.value.ifBlank { "url_icon" }

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUrl(url) }
            if (bitmap == null) {
                _toastMessage.value = "无法根据地址获取图片，请检查图标地址"
                return@launch
            }
            withContext(Dispatchers.Main) {
                addMarkerInternal(latLng, MarkerOptions().withIcon(id, bitmap, /* sdf = */ false))
            }
        }
    }

    fun addMarkerByType() {
        val latLng = parseLatLng(_defaultLatLng.value)
        if (latLng == null) { _toastMessage.value = "请输入有效的位置坐标 (lat,lng)"; return }
        val iconId = _iconType.value.ifBlank { DEFAULT_PIN }
        val cached = typePresetBitmaps[iconId] ?: run {
            _toastMessage.value = "图标未注册：$iconId"; return
        }
        addMarkerInternal(latLng, MarkerOptions().withIcon(iconId, cached.first, cached.second))
    }

    fun addMarkerBySdf() {
        val latLng = parseLatLng(_defaultLatLng.value)
        if (latLng == null) { _toastMessage.value = "请输入有效的位置坐标 (lat,lng)"; return }
        val cached = typePresetBitmaps[MARKER_ICON_ID] ?: run {
            _toastMessage.value = "SDF 图标未注册"; return
        }
        val color = parseColorOrNull(_sdfIconColor.value)
        val opts = MarkerOptions().withIcon(MARKER_ICON_ID, cached.first, /* sdf = */ true)
        if (color != null) opts.withIconColor(color)
        addMarkerInternal(latLng, opts)
    }

    /** 通用 add：补齐 size/opacity/anchor/text + click/drag listener + 加进 markers 列表。 */
    private fun addMarkerInternal(latLng: LatLng, baseOpts: MarkerOptions) {
        val map = mapLibreMapRef ?: run { _toastMessage.value = "地图未就绪"; return }
        val sizeForAdd = if (_iconScaleWithZoom.value) sizeForZoom(map.cameraPosition.zoom) else _iconSize.value
        val opts = baseOpts
            .withLatLng(latLng)
            .withIconSize(sizeForAdd)
            .withIconOpacity(_iconOpacity.value)
            .withIconAnchor(_iconAnchor.value)
            // 文本：跟老实现一致。注意 textSize/textColor 缺一就显示不出来
            .withText("Marker ${_markers.value.size + 1}")
            .withTextSize(16)
            .withTextColor(AndroidColor.parseColor("#000000"))
            .withTextOffset(0f, 1f)
            .withTextAnchor(MarkerAnchorType.TOP)
            .withVisible(true)
            .withDraggable(true)
            .withClickable(true)

        val marker = map.getOverlayEngine().addMarker(opts)
        wireMarkerListeners(marker)
        _markers.value = _markers.value + marker
        _toastMessage.value = "已添加 Marker: ${marker.id.take(8)}"
    }

    /** 给 marker 挂 click / drag listener。 */
    private fun wireMarkerListeners(marker: Marker) {
        marker.addOnClickListener { m ->
            val current = _selectedMarkerId.value
            if (current == m.id) {
                // 再点取消
                m.iconSize = DEFAULT_ICON_SIZE
                m.iconOpacity = DEFAULT_ICON_OPACITY
                _selectedMarkerId.value = null
                _toastMessage.value = "已取消选中 Marker: ${m.id.take(8)}"
            } else {
                // 还原之前选中的
                current?.let { id ->
                    _markers.value.firstOrNull { it.id == id }?.let { prev ->
                        prev.iconSize = DEFAULT_ICON_SIZE
                        prev.iconOpacity = DEFAULT_ICON_OPACITY
                    }
                }
                m.iconSize = SELECTED_ICON_SIZE
                m.iconOpacity = SELECTED_ICON_OPACITY
                _selectedMarkerId.value = m.id
                _toastMessage.value = "已选中 Marker: ${m.id.take(8)}"
                mapLibreMapRef?.moveCamera(CameraUpdateFactory.newLatLng(m.latLng))
            }
            true
        }
        marker.addOnDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStarted(m: Marker) {
                _isDragging.value = true
                _toastMessage.value = "开始拖动 Marker: ${m.id.take(8)}"
            }
            override fun onMarkerDrag(m: Marker) { /* no-op，frame 太多 */ }
            override fun onMarkerDragFinished(m: Marker) {
                _isDragging.value = false
                _dragCompletedCount.value += 1
                _toastMessage.value = "拖动完成 Marker: ${m.id.take(8)} " +
                    "(${"%.4f".format(m.latLng.latitude)}, ${"%.4f".format(m.latLng.longitude)})"
            }
        })
    }

    private fun parseLatLng(input: String): LatLng? {
        val parts = input.trim().split(",").map { it.trim().toDoubleOrNull() }
        if (parts.size >= 2 && parts[0] != null && parts[1] != null) {
            val lat = parts[0]!!
            val lng = parts[1]!!
            if (lat in -90.0..90.0 && lng in -180.0..180.0) return LatLng(lat, lng)
        }
        return null
    }

    private fun parseColorOrNull(hex: String): Int? {
        val trimmed = hex.trim()
        if (trimmed.isEmpty()) return null
        return try { AndroidColor.parseColor(trimmed) } catch (e: IllegalArgumentException) { null }
    }

    private fun loadBitmapFromUrl(urlString: String): Bitmap? = try {
        val connection = URL(urlString).openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.getInputStream().use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }

    fun initSymbolManager(
        context: Context,
        mapView: MapView,
        mapLibreMap: MaptecMap,
        @Suppress("UNUSED_PARAMETER") style: Style,
    ) {
        mapLibreMapRef = mapLibreMap
        mapViewRef = mapView
        contextRef = context

        // 把预置 drawable 转 bitmap 缓存起来，addMarker 时用 MarkerOptions.withIcon
        // 注：bitmap 不必预先 overlayManagerAddImage —— MarkerOptions.withIcon 第一次用时
        // SDK 内部 MarkerImageRegistry 会自动注册
        fun cachePresetIcon(drawableRes: Int, iconId: String, sdf: Boolean) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableRes) ?: return
            val bmp = com.maptec.applied.utils.BitmapUtils.getBitmapFromDrawable(drawable) ?: return
            typePresetBitmaps[iconId] = bmp to sdf
        }
        cachePresetIcon(R.drawable.marker_default, MARKER_ICON_ID, sdf = true)
        cachePresetIcon(R.drawable.default_pin, DEFAULT_PIN, sdf = false)
        cachePresetIcon(R.drawable.blue_dot, BLUE_DOT, sdf = false)
        cachePresetIcon(R.drawable.red_dot, RED_DOT, sdf = false)
        cachePresetIcon(R.drawable.green_dot, GREEN_DOT, sdf = false)
        cachePresetIcon(R.drawable.yellow_dot, YELLOW_DOT, sdf = false)

        // zoom 联动：仍然业务侧批量重设 iconSize（后续应下沉到引擎）
        val l = MaptecMap.OnCameraMoveListener { applyIconSizeForCurrentZoom() }
        mapLibreMap.addOnCameraMoveListener(l)
        zoomListener = l
    }

    fun clearMarkers() {
        val map = mapLibreMapRef ?: return
        map.getOverlayEngine().deleteAllMarkers()
        _markers.value = emptyList()
        _selectedMarkerId.value = null
        _isDragging.value = false
    }

    private fun sizeForZoom(zoom: Double): Float {
        val minZ = _minZoom.value.toDouble()
        val maxZ = _maxZoom.value.toDouble().coerceAtLeast(minZ + 0.0001)
        val minS = _minScale.value
        val maxS = _iconSize.value
        val t = ((zoom - minZ) / (maxZ - minZ)).coerceIn(0.0, 1.0).toFloat()
        return minS + (maxS - minS) * t
    }

    private fun applyIconSizeForCurrentZoom() {
        val map = mapLibreMapRef ?: return
        val markers = _markers.value
        if (markers.isEmpty()) return
        val size = if (_iconScaleWithZoom.value) sizeForZoom(map.cameraPosition.zoom) else _iconSize.value
        for (m in markers) m.iconSize = size
    }

    fun clearToastMessage() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        zoomListener?.let { mapLibreMapRef?.removeOnCameraMoveListener(it) }
        zoomListener = null
        typePresetBitmaps.clear()
        contextRef = null
        mapLibreMapRef = null
        mapViewRef = null
    }
}

// Kotlin 扩展：让 marker.iconSize = ... 这种语法可以工作
private var Marker.iconSize: Float
    get() = getIconSize()
    set(v) { setIconSize(v) }

private var Marker.iconOpacity: Float
    get() = getIconOpacity()
    set(v) { setIconOpacity(v) }

private val Marker.id: String get() = getId()
private val Marker.latLng: LatLng get() = getLatLng()

class MarkerLayerViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkerLayerViewModel::class.java)) {
            return MarkerLayerViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
