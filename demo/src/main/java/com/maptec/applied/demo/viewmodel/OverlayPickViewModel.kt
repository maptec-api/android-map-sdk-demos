package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.OverlayHit
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.OnMarkerClickListener
import com.maptec.applied.maps.overlay.marker.OnMarkerDragListener
import com.maptec.applied.style.layers.Property
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class OverlayPickViewModel : ViewModel() {

    data class PickSnapshot(
        val tap: LatLng,
        val screen: PointF,
        val gesture: String,
        val exHits: List<String>,
        val resolvedMarker: String?,
    )

    private val _pickSnapshot = MutableStateFlow<PickSnapshot?>(null)
    val pickSnapshot: StateFlow<PickSnapshot?> = _pickSnapshot.asStateFlow()

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog.asStateFlow()

    private val _lineClickable = MutableStateFlow(true)
    val lineClickable: StateFlow<Boolean> = _lineClickable.asStateFlow()

    private val _markerClickable = MutableStateFlow(true)
    val markerClickable: StateFlow<Boolean> = _markerClickable.asStateFlow()

    private val _lineConsume = MutableStateFlow(true)
    val lineConsume: StateFlow<Boolean> = _lineConsume.asStateFlow()

    private val _markerConsume = MutableStateFlow(true)
    val markerConsume: StateFlow<Boolean> = _markerConsume.asStateFlow()

    private var mapRef: MaptecMap? = null
    private var appContext: Context? = null
    private var testLine: Line? = null
    private var testMarker: Marker? = null

    private var pickClickListener: MaptecMap.OnMapClickListener? = null
    private var pickLongClickListener: MaptecMap.OnMapLongClickListener? = null

    fun attachMap(map: MaptecMap, context: Context) {
        if (mapRef === map && testMarker != null) return
        detachMap()
        mapRef = map
        appContext = context.applicationContext

        val clickListener = object : MaptecMap.OnMapClickListener {
            override fun onMapClick(point: LatLng) = onMapClickPick(map, point)

            override fun onMapClick(screen: PointF, point: LatLng): Boolean {
                performExPickAtScreen(map, screen, point, gesture = "单击")
                return false
            }
        }
        val longClickListener = object : MaptecMap.OnMapLongClickListener {
            override fun onMapLongClick(point: LatLng) = onMapLongClickPick(map, point)

            override fun onMapLongClick(screen: PointF, point: LatLng): Boolean {
                performExPickAtScreen(map, screen, point, gesture = "长按")
                return false
            }
        }
        map.addOnMapClickListener(clickListener)
        map.addOnMapLongClickListener(longClickListener)
        pickClickListener = clickListener
        pickLongClickListener = longClickListener

        setupTestScene(map, context)
    }

    fun detachMap() {
        val map = mapRef ?: return
        pickClickListener?.let { map.removeOnMapClickListener(it) }
        pickLongClickListener?.let { map.removeOnMapLongClickListener(it) }
        pickClickListener = null
        pickLongClickListener = null
        clearScene()
        mapRef = null
        appContext = null
    }

    fun setLineClickable(enabled: Boolean) {
        _lineClickable.value = enabled
        testLine?.isClickable = enabled
    }

    fun setMarkerClickable(enabled: Boolean) {
        _markerClickable.value = enabled
        testMarker?.isClickable = enabled
    }

    fun setLineConsume(enabled: Boolean) {
        _lineConsume.value = enabled
    }

    fun setMarkerConsume(enabled: Boolean) {
        _markerConsume.value = enabled
    }

    fun pickAtMarker() {
        val map = mapRef ?: return
        val marker = testMarker ?: return
        val screen = map.projection.toScreenLocation(marker.latLng)
        // bottom anchor：向上偏移到 pin 图标主体，避免只命中线
        screen.offset(0f, -MARKER_PICK_SCREEN_OFFSET_Y)
        performExPickAtScreen(
            map,
            screen,
            map.projection.fromScreenLocation(screen),
            gesture = "Marker位置",
        )
    }

    fun resetScene(context: Context) {
        val map = mapRef ?: return
        clearScene()
        setupTestScene(map, context.applicationContext)
        appendLog("场景已重置")
    }

    fun clearLog() {
        _eventLog.value = emptyList()
    }

    override fun onCleared() {
        detachMap()
        super.onCleared()
    }

    private fun setupTestScene(map: MaptecMap, context: Context) {
        val engine = map.getOverlayEngine()

        // 跨 kind：后 add 的 layer group 在渲染 / Ex 拾取栈顶（先 Marker 后 Line → Line 在上）。
        val marker = engine.addMarker(
            MarkerOptions()
                .withLatLng(LINE_CENTER)
                .withIconResource(R.drawable.default_pin, context)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withIconSize(2.2f)
                .withClickable(_markerClickable.value)
                .withDraggable(true)
        )
        marker.addOnClickListener(OnMarkerClickListener { m ->
            appendLog("Marker 点击 listener（consume=${_markerConsume.value}） id=${m.id}")
            _markerConsume.value
        })
        marker.addOnDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStarted(m: Marker) {
                appendLog("Marker 拖拽开始 id=${m.id}")
            }

            override fun onMarkerDrag(m: Marker) {
                appendLog("Marker 拖拽中 id=${m.id}")
            }

            override fun onMarkerDragFinished(m: Marker) {
                appendLog("Marker 拖拽结束 id=${m.id}")
            }
        })
        testMarker = marker

        val linePoints = listOf(
            LatLng(LINE_CENTER.latitude - 0.005, LINE_CENTER.longitude - 0.008),
            LatLng(LINE_CENTER.latitude + 0.005, LINE_CENTER.longitude + 0.008),
        )
        val line = engine.addPolyline(
            LineOptions()
                .withLatLngs(linePoints)
                .withStrokeWeight(36f)
                .withStrokeColor("#E53935")
                .withStrokeOpacity(0.95f)
                .withDraggable(true)
        )
        line.isClickable = _lineClickable.value
        line.addOnMapClickListener(OnOverlayClickListener<Line> { ln ->
            appendLog("Line 点击 listener（consume=${_lineConsume.value}） id=${ln.id}")
            _lineConsume.value
        })
        line.addOnMapLongClickListener(OnOverlayLongClickListener<Line> { ln ->
            appendLog("Line 长按 listener id=${ln.id}")
            true
        })
        line.addOnDragListener(object : OnOverlayDragListener<Line> {
            override fun onAnnotationDragStarted(overlay: Line) {
                appendLog("Line 拖拽开始 id=${overlay.id}")
            }

            override fun onAnnotationDrag(overlay: Line) {
                appendLog("Line 拖拽中 id=${overlay.id}")
            }

            override fun onAnnotationDragFinished(overlay: Line) {
                appendLog("Line 拖拽结束 id=${overlay.id}")
            }
        })
        testLine = line

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LINE_CENTER, 13.5))
        appendLog("测试场景：Marker 先加、Line 后加（Line 在 Marker 上层）；两对象均 consume 时优先 Line listener")
    }

    private fun onMapClickPick(map: MaptecMap, point: LatLng): Boolean {
        performExPick(map, point, gesture = "单击")
        return false
    }

    private fun onMapLongClickPick(map: MaptecMap, point: LatLng): Boolean {
        performExPick(map, point, gesture = "长按")
        return false
    }

    private fun performExPickAtScreen(
        map: MaptecMap,
        screen: PointF,
        latLng: LatLng,
        gesture: String,
    ) {
        val engine = map.getOverlayEngine()
        val hits = engine.queryOverlayHitsFromPoint(screen)
        val resolvedMarker = resolveFirstMarker(engine, hits)

        _pickSnapshot.value = PickSnapshot(
            tap = latLng,
            screen = PointF(screen.x, screen.y),
            gesture = gesture,
            exHits = hits.mapIndexed { index, hit -> formatHit(index, hit) },
            resolvedMarker = resolvedMarker?.let { "Marker id=${it.id}" },
        )

        appendLog(
            "$gesture @ (${formatCoord(latLng.latitude)}, ${formatCoord(latLng.longitude)}) " +
                "screen=(${formatCoord(screen.x.toDouble())}, ${formatCoord(screen.y.toDouble())}) " +
                "→ Ex ${hits.size} hit(s)" +
                (resolvedMarker?.let { ", marker=${it.id}" } ?: "")
        )
        logListenerDispatchTrace(engine, hits, gesture)
    }

    /**
     * 说明引擎 MapClickResolver 的分发结果（与 Ex 命中数可不一致：首个 consume 会终止链）。
     */
    private fun logListenerDispatchTrace(
        engine: MapOverlayEngine,
        hits: List<OverlayHit>,
        gesture: String,
    ) {
        if (hits.isEmpty()) {
            appendLog("$gesture 分发：无命中")
            return
        }
        appendLog("$gesture 引擎 listener 分发（Ex 自上而下，首个 consume 终止）：")
        for ((index, hit) in hits.withIndex()) {
            when (val target = engine.resolveHitTarget(hit)) {
                is Marker -> {
                    val clickable = _markerClickable.value
                    val consume = _markerConsume.value
                    appendLog("  ${index + 1}. Marker clickable=$clickable consume=$consume")
                    if (!clickable) {
                        appendLog("     → skip（clickable=false），继续")
                        continue
                    }
                    appendLog("     → 触发 Marker listener" + if (consume) "，consume=true 终止" else "，consume=false 继续")
                    if (consume) return
                }
                is Line -> {
                    val clickable = _lineClickable.value
                    val consume = _lineConsume.value
                    appendLog("  ${index + 1}. Line clickable=$clickable consume=$consume")
                    if (!clickable) {
                        appendLog("     → skip（clickable=false），继续")
                        continue
                    }
                    appendLog("     → 触发 Line listener" + if (consume) "，consume=true 终止" else "，consume=false 继续")
                    if (consume) return
                }
                else -> appendLog("  ${index + 1}. ${hit.type.name} → 非本页测试对象，继续")
            }
        }
        appendLog("  全部分发完毕（均无 consume）")
    }

    /** 从 Ex hits 自上而下解析第一个 Marker（与 MapClickResolver 一致）。 */
    private fun resolveFirstMarker(
        engine: MapOverlayEngine,
        hits: List<OverlayHit>,
    ): Marker? {
        for (hit in hits) {
            when (val target = engine.resolveHitTarget(hit)) {
                is Marker -> return target
            }
        }
        return null
    }

    private fun clearScene() {
        val map = mapRef ?: return
        val engine = runCatching { map.getOverlayEngine() }.getOrNull() ?: return
        testLine?.let { engine.deleteLine(it) }
        testMarker?.remove()
        testLine = null
        testMarker = null
    }

    private fun performExPick(map: MaptecMap, latLng: LatLng, gesture: String) {
        val screen = map.projection.toScreenLocation(latLng)
        performExPickAtScreen(map, screen, latLng, gesture)
    }

    private fun appendLog(message: String) {
        val next = (_eventLog.value + message).takeLast(MAX_LOG_LINES)
        _eventLog.value = next
    }

    private fun formatHit(index: Int, hit: OverlayHit): String {
        val sub = hit.subId ?: "-"
        return "${index + 1}. ${hit.type.name} overlayId=${hit.overlayId} subId=$sub"
    }

    private fun formatCoord(value: Double): String =
        String.format(Locale.US, "%.5f", value)

    companion object {
        private val LINE_CENTER = LatLng(1.4, 103.75)
        /** bottom anchor 时向上偏移到 pin 图标主体，便于程序化拾取。 */
        private const val MARKER_PICK_SCREEN_OFFSET_Y = 56f
        private const val MAX_LOG_LINES = 30
    }
}

class OverlayPickViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayPickViewModel::class.java)) {
            return OverlayPickViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
