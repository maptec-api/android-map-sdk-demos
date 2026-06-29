package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.graphics.Color as AndroidColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.R
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.animation.AlphaAnimation
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.MapAnimationCurve
import com.maptec.applied.maps.animation.OffsetAnimation
import com.maptec.applied.maps.animation.RepeatMode
import com.maptec.applied.maps.animation.ScaleAnimation
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OverlayKind
import com.maptec.applied.maps.overlay.marker.MarkLayerPlacementMode
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.NativeMarkLayerOverlay
import com.maptec.applied.maps.overlay.marker.OnMarkerDragListener
import com.maptec.applied.style.layers.Property
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MarkerAnimationViewModel —— marker 动画演示页：
 *
 * 在基础 marker 功能（default_pin 添加 / 清除）之上，演示三类动画：
 *   1. 进入动画（none / fadeIn / drop / bounce）+ duration，开始 / 结束按钮，作用于全部 marker
 *   2. 点选动画（none / bounce）：点击地图空白处添加 Marker 并播放对应动画
 *   3. 消失动画（none / fadeOut / scaleDown）+ duration，执行一次，播完后自动移除全部 marker
 *
 * 顶部「开始 / 结束」按钮共用，抽屉内开关切换进入 / 消失模式（互斥）。
 */
class MarkerAnimationViewModel : ViewModel() {

    // ========== 标记管理状态 ==========
    private val _markers = MutableStateFlow<List<Marker>>(emptyList())
    val markers: StateFlow<List<Marker>> = _markers.asStateFlow()

    private val _selectedMarkerId = MutableStateFlow<String?>(null)
    val selectedMarkerId: StateFlow<String?> = _selectedMarkerId.asStateFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ========== 默认坐标 ==========
    private val _defaultLatLng = MutableStateFlow(DEFAULT_LAT_LNG)
    val defaultLatLng: StateFlow<String> = _defaultLatLng.asStateFlow()

    // ========== 动画选择状态 ==========
    private val _enterType = MutableStateFlow(NONE)
    val enterType: StateFlow<String> = _enterType.asStateFlow()
    private val _enterDurationMs = MutableStateFlow(800L)
    val enterDurationMs: StateFlow<Long> = _enterDurationMs.asStateFlow()

    private val _selectType = MutableStateFlow(NONE)
    val selectType: StateFlow<String> = _selectType.asStateFlow()
    private val _selectDurationMs = MutableStateFlow(500L)
    val selectDurationMs: StateFlow<Long> = _selectDurationMs.asStateFlow()

    private val _disappearType = MutableStateFlow(NONE)
    val disappearType: StateFlow<String> = _disappearType.asStateFlow()
    private val _disappearDurationMs = MutableStateFlow(600L)
    val disappearDurationMs: StateFlow<Long> = _disappearDurationMs.asStateFlow()

    /** 顶部开始/结束按钮当前控制的动画模式（进入 / 消失，互斥）。 */
    private val _mainAnimationMode = MutableStateFlow(MainAnimationMode.ENTER)
    val mainAnimationMode: StateFlow<MainAnimationMode> = _mainAnimationMode.asStateFlow()

    private var mapLibreMapRef: MaptecMap? = null
    private var mapViewRef: MapView? = null
    private var contextRef: Context? = null
    private var mapStyleReady = false

    // ========== 运行中的动画句柄 ==========
    /** 每个 marker 独立的进入动画句柄。 */
    private val enterHandles = mutableMapOf<String, Animation>()
    /** 每个 marker 独立的消失动画句柄。 */
    private val disappearHandles = mutableMapOf<String, Animation>()
    /** 每个 marker 独立的点选弹跳动画句柄。 */
    private val selectHandles = mutableMapOf<String, Animation>()

    private var mapClickListener: MaptecMap.OnMapClickListener? = null

    // 记录是否有图层级别的动画 BEGIN 过（用于判断是否需要 END）
    private var hasActiveLayerAnimation = false

    private val mainHandler = Handler(Looper.getMainLooper())
    /** 消失动画完成后移除 marker 的延迟任务（不依赖 native AnimationCallbacks，避免 JNI 回调崩溃）。 */
    private var disappearRemovalRunnable: Runnable? = null

    companion object {
        private const val TAG = "MarkerAnimationVM"
        private val LOG = LoggerFactory.getLogger(LOG_MODULE).withTag(TAG)
        private const val DEFAULT_PIN = "default_pin"
        private const val MARKER_OVERLAY_ID = "marker_overlay_main"
        private const val DEFAULT_LAT_LNG = "1.4, 103.75"
        private const val DEFAULT_ICON_SIZE = 2.0f
        private const val DEFAULT_ICON_OPACITY = 1.0f

        const val NONE = "none"

        /** 引擎 marker 动画属性名（与 markerOverlayImpl hashMap 一致）。 */
        private const val PROP_ICON_OFFSET = "iconOffset"
        private const val PROP_ICON_OPACITY = "iconOpacity"
        private const val PROP_ICON_SIZE = "iconSize"

        private val MARKER_ANIM_PROPS = listOf(PROP_ICON_OFFSET, PROP_ICON_OPACITY, PROP_ICON_SIZE)

        /** icon-offset 单位为 em；-120 会飞出屏幕。drop 下落幅度较小；bounce 需更明显。 */
        private const val ENTER_DROP_OFFSET_FROM = -3.0f
        private const val ENTER_BOUNCE_OFFSET = -8.0f
        private const val SELECT_BOUNCE_OFFSET_TO = -8.0f
        private const val MIN_ICON_SIZE = 0.05f

        /** 进入动画类型选项：value to 显示名。 */
        val ENTER_OPTIONS = listOf(
            NONE to "无",
            "fadeIn" to "淡入 fadeIn",
            "drop" to "下落 drop",
            "bounce" to "弹跳 bounce",
        )

        /** 点选动画类型选项。 */
        val SELECT_OPTIONS = listOf(
            NONE to "无",
            "bounce" to "弹跳 bounce",
        )

        /** 消失动画类型选项。 */
        val DISAPPEAR_OPTIONS = listOf(
            NONE to "无",
            "fadeOut" to "淡出 fadeOut",
            "scaleDown" to "缩小 scaleDown",
        )
    }

    /** 顶部按钮控制的动画模式。 */
    enum class MainAnimationMode {
        ENTER,
        DISAPPEAR,
    }

    // ========== 动画选择 Setter ==========
    fun setEnterType(value: String) { _enterType.value = value }
    fun setEnterDurationMs(value: Long) { _enterDurationMs.value = value.coerceIn(100L, 10_000L) }
    fun setSelectType(value: String) { _selectType.value = value }
    fun setSelectDurationMs(value: Long) { _selectDurationMs.value = value.coerceIn(100L, 10_000L) }
    fun setDisappearType(value: String) { _disappearType.value = value }
    fun setDisappearDurationMs(value: Long) { _disappearDurationMs.value = value.coerceIn(100L, 10_000L) }

    fun setMainAnimationMode(mode: MainAnimationMode) {
        if (_mainAnimationMode.value == mode) return
        when (_mainAnimationMode.value) {
            MainAnimationMode.ENTER -> if (enterHandles.isNotEmpty()) endEnterAnimation()
            MainAnimationMode.DISAPPEAR -> if (disappearHandles.isNotEmpty()) endDisappearAnimation()
        }
        _mainAnimationMode.value = mode
    }

    /** 顶部「开始动画」：按当前模式启动进入或消失动画。 */
    fun startMainAnimation() {
        when (_mainAnimationMode.value) {
            MainAnimationMode.ENTER -> {
                if (disappearHandles.isNotEmpty()) endDisappearAnimation()
                startEnterAnimation()
            }
            MainAnimationMode.DISAPPEAR -> {
                if (enterHandles.isNotEmpty()) endEnterAnimation()
                startDisappearAnimation()
            }
        }
    }

    /** 顶部「结束动画」：按当前模式结束进入或消失动画。 */
    fun endMainAnimation() {
        when (_mainAnimationMode.value) {
            MainAnimationMode.ENTER -> endEnterAnimation()
            MainAnimationMode.DISAPPEAR -> endDisappearAnimation()
        }
    }

    // ========== Marker 添加 ==========

    /** 点击地图空白处：在点击位置添加 Marker 并启动点选动画。 */
    fun onMapClickAddMarker(latLng: LatLng) {
        val context = contextRef ?: run {
            _toastMessage.value = "地图未就绪"
            return
        }
        addMarkerInternal(
            latLng,
            defaultPinOptions(context),
            applySelectAnimation = true,
        )
    }

    /** 添加 Marker —— 使用默认设置，按 default_pin 图标类型添加。 */
    fun addMarker() {
        val context = contextRef ?: run { _toastMessage.value = "地图未就绪"; return }
        val latLng = parseLatLng(_defaultLatLng.value)
        if (latLng == null) { _toastMessage.value = "请输入有效的位置坐标 (lat,lng)"; return }
        addMarkerInternal(
            latLng,
            defaultPinOptions(context),
            applySelectAnimation = false,
        )
    }

    private fun defaultPinOptions(context: Context): MarkerOptions =
        MarkerOptions().withIconResource(R.drawable.default_pin, context)

    /** 通用 add：补齐 size/opacity/anchor/text + click/drag listener + 加进 markers 列表。 */
    private fun addMarkerInternal(
        latLng: LatLng,
        baseOpts: MarkerOptions,
        applySelectAnimation: Boolean,
    ) {
        val map = mapLibreMapRef ?: run { _toastMessage.value = "地图未就绪"; return }
        if (!mapStyleReady) {
            _toastMessage.value = "地图样式加载中，请稍后再试"
            return
        }
        if (baseOpts.iconId == null) {
            _toastMessage.value = "图标加载失败，请重试"
            return
        }
        val opts = baseOpts
            .withLatLng(latLng)
            .withIconSize(DEFAULT_ICON_SIZE)
            .withIconOpacity(DEFAULT_ICON_OPACITY)
            .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
            // 文本：注意 textSize/textColor 缺一就显示不出来
            .withText("Marker ${_markers.value.size + 1}")
            .withTextSize(16)
            .withTextColor(AndroidColor.parseColor("#000000"))
            .withTextOffset(0f, 1f)
            .withTextAnchor(MarkerAnchorType.TOP)
            .withVisible(true)
            .withDraggable(true)
            .withClickable(true)

        val marker = map.getOverlayEngine().addMarker(opts)
        configureMarkerOverlayForDemo(map)
        wireMarkerListeners(marker)
        ensureMarkerDisplayState(marker)
        map.triggerRepaint()
        _markers.value = _markers.value + marker
        if (applySelectAnimation && _selectType.value != NONE) {
            startSelectAnimation(marker)
            _selectedMarkerId.value = marker.id
            val animLabel = SELECT_OPTIONS.find { it.first == _selectType.value }?.second ?: _selectType.value
            _toastMessage.value = "已在 (${"%.4f".format(latLng.latitude)}, ${"%.4f".format(latLng.longitude)}) " +
                "添加 Marker，点选动画：$animLabel"
        } else {
            _toastMessage.value = if (applySelectAnimation) {
                "已在 (${"%.4f".format(latLng.latitude)}, ${"%.4f".format(latLng.longitude)}) 添加 Marker"
            } else {
                "已添加 Marker: ${marker.id.take(8)}"
            }
        }
    }

    /** 新 Marker 不受进行中的图层级动画影响；无动画时不派发 END，避免干扰 native 默认渲染。 */
    private fun ensureMarkerDisplayState(marker: Marker) {
        marker.iconSize = DEFAULT_ICON_SIZE
        marker.iconOpacity = DEFAULT_ICON_OPACITY

    }

    /** Demo：关闭 MarkLayer 符号碰撞，避免 pin 被底图 POI 挤掉而只剩文字。 */
    private fun configureMarkerOverlayForDemo(map: MaptecMap) {
        val engine = map.getOverlayEngine()
        val nativePtr = map.nativeMapPtr
        if (nativePtr == 0L) return
        val layerGroupId = engine.layerGroupId(OverlayKind.MARKER)
        NativeMarkLayerOverlay.nativeEnsureCreated(
            nativePtr,
            layerGroupId,
            MARKER_OVERLAY_ID,
            MarkLayerPlacementMode.POINT,
        )
        NativeMarkLayerOverlay.nativeSetOverlayOption(
            nativePtr,
            layerGroupId,
            MARKER_OVERLAY_ID,
            true,
            true,
            false,
            true,
        )
    }

    /** 给 marker 挂 click / drag listener。 */
    private fun wireMarkerListeners(marker: Marker) {
        marker.addOnClickListener { m ->
            if (_selectType.value == NONE) {
                _selectedMarkerId.value = m.id
                return@addOnClickListener true
            }
            startSelectAnimation(m)
            _selectedMarkerId.value = m.id
            _toastMessage.value = "已重新触发点选动画: ${m.id.take(8)}"
            true
        }
        marker.addOnDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStarted(m: Marker) {
                _isDragging.value = true
                _toastMessage.value = "开始拖动 Marker: ${m.id.take(8)}"
            }
            override fun onMarkerDrag(m: Marker) {}
            override fun onMarkerDragFinished(m: Marker) {
                _isDragging.value = false
                _toastMessage.value = "拖动完成 Marker: ${m.id.take(8)} " +
                    "(${"%.4f".format(m.latLng.latitude)}, ${"%.4f".format(m.latLng.longitude)})"
            }
        })
    }

    // ========== 进入动画 ==========

    fun startEnterAnimation() {
        if (_markers.value.isEmpty()) { _toastMessage.value = "请先添加 Marker"; return }
        buildEnterAnimation() ?: run { _toastMessage.value = "进入动画为「无」"; return }
        val prop = enterAnimProperty() ?: return
        cancelEnterAnimations()

        LOG.i { "startEnterAnimation type=${_enterType.value} prop=$prop dur=${_enterDurationMs.value} markers=${_markers.value.size}" }
        var started = 0
        for (m in _markers.value) {
            val anim = buildEnterAnimation() ?: continue
            val handle = m.startAnimation(anim, 0L) ?: continue
            enterHandles[m.id] = handle
            started++
        }
        if (started == 0) {
            LOG.w { "startAnimation 失败：动画为空或 overlay 未找到" }
            _toastMessage.value = "动画启动失败，请 Clean 后重新编译运行"
            return
        }
        _toastMessage.value = "开始进入动画：${_enterType.value}"
    }

    fun endEnterAnimation() {
        cancelEnterAnimations()
        _markers.value.forEach { ensureMarkerDisplayState(it) }
        _toastMessage.value = "结束进入动画"
    }

    private fun cancelEnterAnimations() {
        val engine = overlayEngine()
        enterHandles.values.forEach { engine?.cancelAnimation(it) }
        enterHandles.clear()
    }

    private fun buildEnterAnimation(): Animation? {
        val dur = _enterDurationMs.value
        return when (_enterType.value) {
            "fadeIn" -> AlphaAnimation(0f, 1f).apply {
                durationMs = dur
                curve = MapAnimationCurve.LINEAR
                repeatCount = -1
                repeatMode = RepeatMode.RESTART
            }
            "drop" -> OffsetAnimation(ENTER_DROP_OFFSET_FROM, 0f).apply {
                durationMs = dur
                curve = MapAnimationCurve.LINEAR
                repeatCount = -1
                repeatMode = RepeatMode.RESTART
            }
            "bounce" -> OffsetAnimation(ENTER_BOUNCE_OFFSET, 0f).apply {
                durationMs = dur
                curve = MapAnimationCurve.EASE_IN_OUT
                repeatCount = -1
                repeatMode = RepeatMode.REVERSE
            }
            else -> null
        }
    }

    // ========== 点选动画 ==========

    private fun startSelectAnimation(m: Marker) {
        when (_selectType.value) {
            NONE -> stopSelectAnimation(m)
            else -> {
                val anim = buildSelectAnimation() ?: return
                val prop = selectAnimProperty() ?: return
                val engine = overlayEngine() ?: return
                stopBounceAnimation(m)
                LOG.i { "startSelectAnimation type=${_selectType.value} prop=$prop marker=${m.id.take(8)}" }
                val handle = m.startAnimation(anim, 0L) ?: return
                selectHandles[m.id] = handle
            }
        }
    }

    private fun stopSelectAnimation(m: Marker) {
        stopBounceAnimation(m)
        m.iconSize = DEFAULT_ICON_SIZE
        m.iconOpacity = DEFAULT_ICON_OPACITY
    }

    private fun stopBounceAnimation(m: Marker) {
        val handle = selectHandles.remove(m.id)
        val prop = selectAnimProperty()
        val engine = overlayEngine()
        if (handle != null && prop != null && engine != null) {
            engine.cancelAnimation(handle)
        }
    }

    private fun stopAllSelectAnimations() {
        val engine = overlayEngine()
        val bounceHandles = selectHandles.values.toList()
        selectHandles.clear()
        bounceHandles.forEach { handle -> engine?.cancelAnimation(handle) }
        _markers.value.forEach { marker ->
            marker.iconSize = DEFAULT_ICON_SIZE
            marker.iconOpacity = DEFAULT_ICON_OPACITY
        }
    }

    private fun buildSelectAnimation(): Animation? {
        return when (_selectType.value) {
            "bounce" -> OffsetAnimation(0f, SELECT_BOUNCE_OFFSET_TO).apply {
                durationMs = _selectDurationMs.value
                curve = MapAnimationCurve.LINEAR
                repeatCount = -1
                repeatMode = RepeatMode.REVERSE
            }
            else -> null
        }
    }

    // ========== 消失动画 ==========

    fun startDisappearAnimation() {
        val markerList = _markers.value
        if (markerList.isEmpty()) {
            _toastMessage.value = "请先添加 Marker"
            return
        }
        if (disappearHandles.isNotEmpty()) {
            _toastMessage.value = "消失动画进行中，请等待完成或点「结束动画」取消"
            return
        }
        // 「无」：无过渡动画，直接移除全部 marker
        if (_disappearType.value == NONE) {
            removeAllMarkersNow("已直接移除")
            return
        }
        val prop = disappearAnimProperty() ?: return
        val durationMs = _disappearDurationMs.value
        cancelDisappearRemovalTask()
        buildDisappearAnimation() ?: return

        LOG.i {
            "startDisappearAnimation type=${_disappearType.value} prop=$prop dur=$durationMs markers=${markerList.size}"
        }
        var started = 0
        for (m in markerList) {
            val anim = buildDisappearAnimation() ?: continue
            val handle = m.startAnimation(anim, durationMs) ?: continue
            disappearHandles[m.id] = handle
            started++
        }
        if (started == 0) {
            _toastMessage.value = "动画启动失败"
            return
        }
        scheduleDisappearRemoval(durationMs)
        _toastMessage.value = "开始消失动画：${_disappearType.value}（完成后自动移除 Marker）"
    }

    fun endDisappearAnimation() {
        if (disappearHandles.isEmpty()) {
            _toastMessage.value = "当前没有进行中的消失动画"
            return
        }
        cancelDisappearRemovalTask()
        cancelDisappearAnimations()
        _markers.value.forEach { ensureMarkerDisplayState(it) }
        _toastMessage.value = "已取消消失动画"
    }

    private fun cancelDisappearAnimations() {
        val engine = overlayEngine()
        disappearHandles.values.forEach { engine?.cancelAnimation(it) }
        disappearHandles.clear()
    }

    private fun scheduleDisappearRemoval(durationMs: Long) {
        val task = Runnable {
            disappearRemovalRunnable = null
            removeMarkersAfterDisappearAnimation()
        }
        disappearRemovalRunnable = task
        // 略大于动画时长，确保最后一帧渲染完成后再删 marker
        mainHandler.postDelayed(task, durationMs + 100L)
    }

    private fun cancelDisappearRemovalTask() {
        disappearRemovalRunnable?.let { mainHandler.removeCallbacks(it) }
        disappearRemovalRunnable = null
    }

    /** 消失动画自然播完后：清理句柄并从地图移除全部 marker。 */
    private fun removeMarkersAfterDisappearAnimation() {
        if (disappearHandles.isEmpty()) return
        disappearHandles.clear()
        if (enterHandles.isEmpty()) {
            hasActiveLayerAnimation = false
        }
        removeAllMarkersNow("消失动画完成，已移除")
    }

    /** 从地图删除全部 marker 并清空状态（无动画 / 动画结束后共用）。 */
    private fun removeAllMarkersNow(toastPrefix: String) {
        cancelDisappearRemovalTask()
        cancelEnterAnimations()
        cancelDisappearAnimations()
        // 所有 marker 都要被删除了，清除图层动画标记
        hasActiveLayerAnimation = false
        stopAllSelectAnimations()
        val engine = overlayEngine() ?: return
        val removedCount = _markers.value.size
        if (removedCount > 0) {
            engine.deleteAllMarkers()
        }
        _markers.value = emptyList()
        _selectedMarkerId.value = null
        _isDragging.value = false
        _toastMessage.value = "$toastPrefix $removedCount 个 Marker"
    }

    private fun buildDisappearAnimation(): Animation? {
        val dur = _disappearDurationMs.value
        return when (_disappearType.value) {
            "fadeOut" -> AlphaAnimation(1f, 0f).apply {
                durationMs = dur
                curve = MapAnimationCurve.LINEAR
                repeatCount = 0
                setNeedResetAnimationState(false)
            }
            "scaleDown" -> ScaleAnimation(DEFAULT_ICON_SIZE, MIN_ICON_SIZE).apply {
                durationMs = dur
                curve = MapAnimationCurve.LINEAR
                repeatCount = 0
                setNeedResetAnimationState(false)
            }
            else -> null
        }
    }

    private fun enterAnimProperty(): String? = when (_enterType.value) {
        "fadeIn" -> PROP_ICON_OPACITY
        "drop", "bounce" -> PROP_ICON_OFFSET
        else -> null
    }

    private fun selectAnimProperty(): String? = when (_selectType.value) {
        "bounce" -> PROP_ICON_OFFSET
        else -> null
    }

    private fun disappearAnimProperty(): String? = when (_disappearType.value) {
        "fadeOut" -> PROP_ICON_OPACITY
        "scaleDown" -> PROP_ICON_SIZE
        else -> null
    }

    private fun overlayEngine(): MapOverlayEngine? {
        val map = mapLibreMapRef ?: run {
            _toastMessage.value = "地图未就绪"
            return null
        }
        return try {
            map.getOverlayEngine()
        } catch (_: IllegalStateException) {
            null
        }
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

    fun initSymbolManager(
        context: Context,
        mapView: MapView,
        mapLibreMap: MaptecMap,
        @Suppress("UNUSED_PARAMETER") style: Style,
    ) {
        val isNewMap = mapLibreMapRef !== mapLibreMap
        mapLibreMapRef = mapLibreMap
        mapViewRef = mapView
        contextRef = context

        if (isNewMap) {
            cancelAllAnimations()
            _markers.value = emptyList()
            _selectedMarkerId.value = null
            _isDragging.value = false
            mapStyleReady = false
        }

        mapStyleReady = true

        mapClickListener?.let { mapLibreMap.removeOnMapClickListener(it) }
        val listener = MaptecMap.OnMapClickListener { latLng ->
            onMapClickAddMarker(latLng)
            true
        }
        mapClickListener = listener
        mapLibreMap.addOnMapClickListener(listener)
    }

    fun clearMarkers() {
        cancelAllAnimations()
        val map = mapLibreMapRef ?: return
        map.getOverlayEngine().deleteAllMarkers()
        _markers.value = emptyList()
        _selectedMarkerId.value = null
        _isDragging.value = false
    }

    private fun cancelAllAnimations() {
        cancelDisappearRemovalTask()
        cancelEnterAnimations()
        cancelDisappearAnimations()
        hasActiveLayerAnimation = false
        stopAllSelectAnimations()
    }

    fun clearToastMessage() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        mapClickListener?.let { listener ->
            mapLibreMapRef?.removeOnMapClickListener(listener)
        }
        mapClickListener = null
        cancelAllAnimations()
        mapStyleReady = false
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

class MarkerAnimationViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkerAnimationViewModel::class.java)) {
            return MarkerAnimationViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
