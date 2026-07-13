package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.OnMarkerDragListener
import com.maptec.applied.style.Property
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MarkerAnimationViewModel —— marker 动画演示页：
 *
 * 在基础 marker 功能（default_pin 添加 / 清除）之上，演示三类动画：
 *   1. 进入动画（none / fadeIn / drop / bounce）+ duration，开始 / 结束按钮，作用于全部 marker
 *   2. 点选动画（none / bounce）：点击已有 Marker 触发；空白处点击仅添加 Marker
 *   3. 消失动画（none / fadeOut / scaleDown）+ duration，执行一次，播完后自动移除全部 marker
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
    private val defaultLatLngText = DEFAULT_LAT_LNG

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

    private var mapLibreMapRef: MaptecMap? = null
    private var contextRef: Context? = null
    private var mapStyleReady = false
    private var markerOverlayConfigured = false

    // ========== 运行中的动画句柄 ==========
    /** 每个 marker 独立的进入动画句柄。 */
    private val enterHandles = mutableMapOf<String, Animation>()
    /** 每个 marker 独立的消失动画句柄。 */
    private val disappearHandles = mutableMapOf<String, Animation>()
    /** 每个 marker 独立的点选弹跳动画句柄。 */
    private val selectHandles = mutableMapOf<String, Animation>()

    private var mapClickListener: MaptecMap.OnMapClickListener? = null

    /** 防止地图点击与 Marker 点击在同一手势内重复触发点选动画。 */
    private var lastSelectHandledMarkerId: String? = null
    private var lastSelectHandledTimeMs: Long = 0L
    /** 地图点击添加 Marker 后，同一次手势可能误触发新 Marker 的 onClick。 */
    private var justAddedFromMapClickMarkerId: String? = null
    private var justAddedFromMapClickTimeMs: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    /** 消失动画完成后移除 marker 的延迟任务 。 */
    private var disappearRemovalRunnable: Runnable? = null

    companion object {
        private const val TAG = "MarkerAnimationVM"
        private val LOG = LoggerFactory.getLogger(LOG_MODULE).withTag(TAG)
        private const val DEFAULT_LAT_LNG = "1.4, 103.75"
        private const val DEFAULT_ICON_SIZE = 2.0f
        private const val DEFAULT_ICON_OPACITY = 1.0f

        /** default_pin 矢量图尺寸（与 res/drawable/default_pin.xml 一致）。 */
        private const val MARKER_ICON_DP = 40f
        /** 与 addMarkerInternal 中 withTextSize(16) 一致。 */
        private const val MARKER_TEXT_OFFSET_DP = 16f
        /** 单行文本大致高度。 */
        private const val MARKER_TEXT_HEIGHT_DP = 18f
        /** 在图标/文字矩形外四周扩展的热区（dp）。 */
        private const val MARKER_HIT_PADDING_DP = 16f
        private const val SELECT_DEDUP_WINDOW_MS = 300L
        /** 地图点击刚添加的 Marker 会同步收到 click，需短暂忽略以免误播点选动画。 */
        private const val JUST_ADDED_SELECT_SUPPRESS_MS = 400L

        const val NONE = "none"

        /** 引擎 marker 动画属性名（与 markerOverlayImpl hashMap 一致）。 */
        private const val PROP_ICON_OFFSET = "iconOffset"
        private const val PROP_ICON_OPACITY = "iconOpacity"
        private const val PROP_ICON_SIZE = "iconSize"

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

    // ========== 动画选择 Setter ==========
    fun setEnterType(value: String) { _enterType.value = value }
    fun setEnterDurationMs(value: Long) { _enterDurationMs.value = value.coerceIn(100L, 10_000L) }
    fun setSelectType(value: String) { _selectType.value = value }
    fun setSelectDurationMs(value: Long) { _selectDurationMs.value = value.coerceIn(100L, 10_000L) }
    fun setDisappearType(value: String) { _disappearType.value = value }
    fun setDisappearDurationMs(value: Long) { _disappearDurationMs.value = value.coerceIn(100L, 10_000L) }

    // ========== Marker 添加 ==========

    /** 点击地图空白处：在点击位置添加 Marker（不触发点选动画）。 */
    private fun onMapClickAddMarker(latLng: LatLng) {
        val context = contextRef ?: run {
            _toastMessage.value = "地图未就绪"
            return
        }
        addMarkerInternal(
            latLng,
            defaultPinOptions(context),
            fromMapClick = true,
        )
    }

    /** 添加 Marker —— 使用默认设置，按 default_pin 图标类型添加。 */
    fun addMarker() {
        val context = contextRef ?: run { _toastMessage.value = "地图未就绪"; return }
        val latLng = parseLatLng(defaultLatLngText)
        if (latLng == null) { _toastMessage.value = "请输入有效的位置坐标 (lat,lng)"; return }
        addMarkerInternal(
            latLng,
            defaultPinOptions(context),
        )
    }

    private fun defaultPinOptions(context: Context): MarkerOptions =
        MarkerOptions().withIconResource(R.drawable.default_pin, context)

    /** 通用 add：补齐 size/opacity/anchor/text + click/drag listener + 加进 markers 列表。 */
    private fun addMarkerInternal(
        latLng: LatLng,
        baseOpts: MarkerOptions,
        fromMapClick: Boolean = false,
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
            .withIconAnchor(MarkerAnchorType.BOTTOM)
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
        wireMarkerListeners(marker)
        ensureMarkerDisplayState(marker)
        map.triggerRepaint()
        requestRepaintIfAnimationsActive()
        _markers.value = _markers.value + marker
        if (fromMapClick) {
            justAddedFromMapClickMarkerId = marker.id
            justAddedFromMapClickTimeMs = SystemClock.uptimeMillis()
        }
        _toastMessage.value = if (fromMapClick) {
            "已在 (${"%.4f".format(latLng.latitude)}, ${"%.4f".format(latLng.longitude)}) 添加 Marker"
        } else {
            "已添加 Marker: ${marker.id.take(8)}"
        }
    }

    /** Demo：关闭 MarkLayer 符号碰撞，避免 pin 被底图 POI 挤掉而只剩文字。仅初始化一次。 */
    private fun ensureMarkerOverlayConfigured(map: MaptecMap) {
        if (markerOverlayConfigured) return
        map.getOverlayEngine().setMarkerLayerCollisionEnabled(false)
        markerOverlayConfigured = true
    }

    /** 有动画进行时额外补一帧重绘，避免 addMarker 的 GeoJSON 更新被动画帧排队拖后。 */
    private fun requestRepaintIfAnimationsActive() {
        if (enterHandles.isEmpty() && selectHandles.isEmpty() && disappearHandles.isEmpty()) return
        mainHandler.post { mapLibreMapRef?.triggerRepaint() }
    }

    /** 新 Marker 不受进行中的图层级动画影响 */
    private fun ensureMarkerDisplayState(marker: Marker) {
        marker.iconSize = DEFAULT_ICON_SIZE
        marker.iconOpacity = DEFAULT_ICON_OPACITY
    }

    /** 给 marker 挂 click / drag listener。 */
    private fun wireMarkerListeners(marker: Marker) {
        marker.addOnClickListener { m ->
            onMarkerTappedForSelect(m)
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

    /** 停止单个 Marker 上正在进行的进入 / 消失 / 点选动画，并恢复默认显示状态。 */
    private fun stopMarkerAnimations(marker: Marker) {
        val engine = overlayEngine() ?: return
        val id = marker.id
        enterHandles.remove(id)?.let { engine.cancelAnimation(it) }
        disappearHandles.remove(id)?.let { engine.cancelAnimation(it) }
        cancelSelectAnimation(marker)
        ensureMarkerDisplayState(marker)
    }

    private fun startSelectAnimation(m: Marker) {
        when (_selectType.value) {
            NONE -> stopSelectAnimation(m)
            else -> {
                val anim = buildSelectAnimation() ?: return
                val prop = selectAnimProperty() ?: return
                LOG.i { "startSelectAnimation type=${_selectType.value} prop=$prop marker=${m.id.take(8)}" }
                val handle = m.startAnimation(anim, 0L) ?: return
                selectHandles[m.id] = handle
            }
        }
    }

    private fun stopSelectAnimation(marker: Marker) {
        cancelSelectAnimation(marker)
        ensureMarkerDisplayState(marker)
    }

    private fun cancelSelectAnimation(marker: Marker) {
        val handle = selectHandles.remove(marker.id) ?: return
        overlayEngine()?.cancelAnimation(handle)
    }

    private fun stopAllSelectAnimations() {
        val engine = overlayEngine() ?: return
        val handles = selectHandles.values.toList()
        selectHandles.clear()
        handles.forEach { engine.cancelAnimation(it) }
        _markers.value.forEach { ensureMarkerDisplayState(it) }
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
        removeAllMarkersNow("消失动画完成，已移除")
    }

    /** 从地图删除全部 marker 并清空状态（无动画 / 动画结束后共用）。 */
    private fun removeAllMarkersNow(toastPrefix: String) {
        cancelDisappearRemovalTask()
        cancelEnterAnimations()
        cancelDisappearAnimations()
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

    /** 点击 Marker：先停该 Marker 当前动画，再按配置播放点选动画。 */
    private fun onMarkerTappedForSelect(marker: Marker) {
        val now = SystemClock.uptimeMillis()
        if (marker.id == justAddedFromMapClickMarkerId &&
            now - justAddedFromMapClickTimeMs < JUST_ADDED_SELECT_SUPPRESS_MS
        ) {
            return
        }
        if (marker.id == lastSelectHandledMarkerId && now - lastSelectHandledTimeMs < SELECT_DEDUP_WINDOW_MS) {
            return
        }
        lastSelectHandledMarkerId = marker.id
        lastSelectHandledTimeMs = now

        _selectedMarkerId.value = marker.id
        stopMarkerAnimations(marker)
        if (_selectType.value == NONE) {
            _toastMessage.value = "已停止 Marker 动画: ${marker.id.take(8)}"
            return
        }
        startSelectAnimation(marker)
        _toastMessage.value = "已触发点选动画: ${marker.id.take(8)}"
    }

    /**
     * 以底部锚点计算单个 Marker 的点击矩形：整图标 + 下方文本，四周略扩。
     * default_pin 为 40dp 正方形，锚点在底部中心。
     */
    private fun markerHitRect(map: MaptecMap, marker: Marker): RectF {
        val density = contextRef?.resources?.displayMetrics?.density ?: 1f
        val scale = marker.iconSize / DEFAULT_ICON_SIZE
        val padding = MARKER_HIT_PADDING_DP * density
        val iconW = MARKER_ICON_DP * density * scale
        val iconH = MARKER_ICON_DP * density * scale
        val textBelow = (MARKER_TEXT_OFFSET_DP + MARKER_TEXT_HEIGHT_DP) * density

        val anchor = map.projection.toScreenLocation(marker.latLng)
        return RectF(
            anchor.x - iconW / 2f - padding,
            anchor.y - iconH - padding,
            anchor.x + iconW / 2f + padding,
            anchor.y + textBelow + padding,
        )
    }

    private fun findMarkerAtScreen(map: MaptecMap, screen: PointF): Marker? {
        for (marker in _markers.value.asReversed()) {
            if (!marker.isClickable) continue
            if (markerHitRect(map, marker).contains(screen.x, screen.y)) {
                return marker
            }
        }
        return null
    }

    /** 空白处点击添加 Marker；命中已有 Marker 区域时触发点选动画，不新增。 */
    private fun handleMapClickForAddMarker(map: MaptecMap, screen: PointF, latLng: LatLng): Boolean {
        findMarkerAtScreen(map, screen)?.let { marker ->
            onMarkerTappedForSelect(marker)
            return true
        }
        onMapClickAddMarker(latLng)
        return true
    }

    fun initSymbolManager(
        context: Context,
        @Suppress("UNUSED_PARAMETER") mapView: MapView,
        mapTecMap: MaptecMap,
        @Suppress("UNUSED_PARAMETER") style: Style,
    ) {
        val isNewMap = mapLibreMapRef !== mapTecMap
        mapLibreMapRef = mapTecMap
        contextRef = context

        if (isNewMap) {
            cancelAllAnimations()
            _markers.value = emptyList()
            _selectedMarkerId.value = null
            _isDragging.value = false
            mapStyleReady = false
            markerOverlayConfigured = false
        }

        mapStyleReady = true
        ensureMarkerOverlayConfigured(mapTecMap)

        mapClickListener?.let { mapTecMap.removeOnMapClickListener(it) }
        val listener = object : MaptecMap.OnMapClickListener {
            override fun onMapClick(point: LatLng): Boolean {
                val screen = mapTecMap.projection.toScreenLocation(point)
                return handleMapClickForAddMarker(mapTecMap, screen, point)
            }

            override fun onMapClick(screen: PointF, point: LatLng): Boolean {
                return handleMapClickForAddMarker(mapTecMap, screen, point)
            }
        }
        mapClickListener = listener
        mapTecMap.addOnMapClickListener(listener)
    }

    /** 停止全部 Marker 的进入 / 消失 / 点选动画，不删除 Marker。 */
    fun stopAllAnimations() {
        val hasActiveAnimations = enterHandles.isNotEmpty() ||
            disappearHandles.isNotEmpty() ||
            selectHandles.isNotEmpty() ||
            disappearRemovalRunnable != null
        if (!hasActiveAnimations) {
            _toastMessage.value = "当前没有进行中的动画"
            return
        }
        cancelAllAnimations()
        _markers.value.forEach { ensureMarkerDisplayState(it) }
        _toastMessage.value = "已停止全部动画"
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
