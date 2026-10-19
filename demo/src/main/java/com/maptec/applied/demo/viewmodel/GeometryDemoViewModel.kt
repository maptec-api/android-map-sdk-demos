package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoCapability
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoColors
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoMapLayer
import com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoTab
import com.maptec.applied.demo.ui.screens.tools.geometry.buildClearSimplifyExample
import com.maptec.applied.demo.ui.screens.tools.geometry.buildRightTriangle
import com.maptec.applied.demo.ui.screens.tools.geometry.densifyGreatCircle
import com.maptec.applied.demo.ui.screens.tools.geometry.offsetPath
import com.maptec.applied.demo.ui.screens.tools.geometry.pathMidpoint
import com.maptec.applied.demo.ui.screens.tools.geometry.polygonCentroid
import com.maptec.applied.demo.ui.screens.tools.geometry.sameLatLng
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.util.GeometryException
import com.maptec.applied.geometry.util.PolyUtil
import com.maptec.applied.geometry.util.SphericalUtil
import com.maptec.applied.maps.MaptecMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
data class GeometryDemoUiState(
    val tab: GeometryDemoTab = GeometryDemoTab.Spherical,
    val capability: GeometryDemoCapability = GeometryDemoCapability.Distance,
    val statusLine: String = "",
    val panelDetail: String = "",
    val offsetDistanceM: Float = 500f,
    val offsetHeadingDeg: Float = 45f,
    val originDistanceM: Float = 5000f,
    val originHeadingDeg: Float = 45f,
    val interpolateFraction: Float = 0.5f,
    val simplifyToleranceM: Float = 30f,
    val pathClosedForIndex: Boolean = true,
    val spatialToleranceM: Float = 25f,
    val nextClickIsTestPoint: Boolean = false,
    val encodedPreview: String = "",
    val decodedCount: Int = 0,
)

class GeometryDemoViewModel : ViewModel() {
    private val layer = GeometryDemoMapLayer()
    private var displayContext: Context? = null

    fun setDisplayContext(context: Context) {
        displayContext = context
    }

    private fun capLabel(cap: GeometryDemoCapability): String {
        return displayContext?.getString(cap.chipLabelRes).orEmpty()
    }

    private fun txt(@androidx.annotation.StringRes id: Int, vararg args: Any): String {
        val ctx = displayContext ?: return ""
        return ctx.getString(id, *args)
    }
    private val vertices = mutableListOf<LatLng>()
    private var pointA: LatLng? = null
    private var pointB: LatLng? = null
    private var segmentStart: LatLng? = null
    private var segmentEnd: LatLng? = null
    private var testPoint: LatLng? = null
    private var referencePoint: LatLng? = null
    private var targetTo: LatLng? = null

    private val _uiState = MutableStateFlow(GeometryDemoUiState())
    val uiState: StateFlow<GeometryDemoUiState> = _uiState.asStateFlow()

    fun attachMap(map: MaptecMap, context: Context, bottomPaddingPx: Int) {
        layer.attach(map, context)
        map.setContentPadding(0.0, 0.0, 0.0, bottomPaddingPx.toDouble())
        redraw()
    }

    fun selectTab(tab: GeometryDemoTab) {
        if (_uiState.value.tab == tab) return
        resetPoints()
        val cap = GeometryDemoCapability.defaultForTab.getValue(tab)
        _uiState.update {
            it.copy(
                tab = tab,
                capability = cap,
                nextClickIsTestPoint = false,
            )
        }
        redraw()
    }

    fun selectCapability(capability: GeometryDemoCapability) {
        if (_uiState.value.capability == capability) return
        resetPoints()
        _uiState.update {
            it.copy(
                capability = capability,
                tab = capability.tab,
                nextClickIsTestPoint = false,
            )
        }
        redraw()
    }

    fun clearPoints() {
        resetPoints()
        redraw()
    }

    fun setNextClickIsTestPoint(enabled: Boolean) {
        _uiState.update { it.copy(nextClickIsTestPoint = enabled) }
    }

    fun updateOffsetDistance(value: Float) {
        _uiState.update { it.copy(offsetDistanceM = value) }
        if (_uiState.value.capability == GeometryDemoCapability.Offset) redraw()
    }

    fun updateOffsetHeading(value: Float) {
        _uiState.update { it.copy(offsetHeadingDeg = value) }
        if (_uiState.value.capability == GeometryDemoCapability.Offset) redraw()
    }

    fun updateOriginDistance(value: Float) {
        _uiState.update { it.copy(originDistanceM = value) }
        if (_uiState.value.capability == GeometryDemoCapability.OffsetOrigin) redraw()
    }

    fun updateOriginHeading(value: Float) {
        _uiState.update { it.copy(originHeadingDeg = value) }
        if (_uiState.value.capability == GeometryDemoCapability.OffsetOrigin) redraw()
    }

    fun updateInterpolateFraction(value: Float) {
        _uiState.update { it.copy(interpolateFraction = value) }
        if (_uiState.value.capability == GeometryDemoCapability.Interpolate) redraw()
    }

    fun updateSimplifyTolerance(value: Float) {
        _uiState.update { it.copy(simplifyToleranceM = value) }
        if (_uiState.value.capability == GeometryDemoCapability.Path) redraw()
    }

    fun updatePathClosedForIndex(value: Boolean) {
        _uiState.update { it.copy(pathClosedForIndex = value) }
        if (_uiState.value.capability == GeometryDemoCapability.SegmentIndex) redraw()
    }

    fun fillSimplifyExample() {
        resetPoints()
        vertices.clear()
        vertices += buildClearSimplifyExample()
        _uiState.update { it.copy(simplifyToleranceM = 55f) }
        redraw()
    }

    fun fillSignedAreaCounterClockwise() {
        resetPoints()
        vertices.clear()
        vertices += buildRightTriangle(clockwise = false)
        redraw()
    }

    fun fillSignedAreaClockwise() {
        resetPoints()
        vertices.clear()
        vertices += buildRightTriangle(clockwise = true)
        redraw()
    }

    fun fillClosedPolygonExample() {
        resetPoints()
        vertices.clear()
        val tri = buildRightTriangle(clockwise = false)
        vertices += tri
        vertices += tri.first()
        redraw()
    }

    fun triggerPoleNoSolution() {
        targetTo = LatLng(90.0, 103.8198)
        redraw()
    }

    fun closePolygonExplicitly() {
        if (_uiState.value.capability != GeometryDemoCapability.IsClosed) return
        if (vertices.size < 3) return
        if (PolyUtil.isClosedPolygon(vertices)) return
        vertices += vertices.first()
        redraw()
    }

    fun onMapClick(point: LatLng) {
        when (_uiState.value.capability) {
            GeometryDemoCapability.Distance -> onClickTwoPointAb(point, maxPoints = 2)
            GeometryDemoCapability.Angle -> onClickTwoPointAb(point, maxPoints = 2)
            GeometryDemoCapability.Area -> onClickPolygonWithOptionalTest(point)
            GeometryDemoCapability.SignedArea -> onClickAppendVertex(point)
            GeometryDemoCapability.Offset -> {
                referencePoint = point
                redraw()
            }
            GeometryDemoCapability.OffsetOrigin -> {
                targetTo = point
                redraw()
            }
            GeometryDemoCapability.Interpolate -> onClickInterpolate(point)
            GeometryDemoCapability.Path -> onClickPath(point)
            GeometryDemoCapability.DistanceToSegment -> onClickDistanceToSegment(point)
            GeometryDemoCapability.SegmentIndex -> onClickSegmentIndex(point)
            GeometryDemoCapability.IsClosed -> onClickIsClosed(point)
            GeometryDemoCapability.EncodeDecode -> onClickAppendVertex(point)
        }
    }

    private fun onClickTwoPointAb(point: LatLng, maxPoints: Int) {
        if (vertices.size >= maxPoints) {
            vertices.clear()
        }
        vertices += point
        redraw()
    }

    private fun onClickAppendVertex(point: LatLng) {
        vertices += point
        redraw()
    }

    private fun onClickPolygonWithOptionalTest(point: LatLng) {
        if (_uiState.value.nextClickIsTestPoint && vertices.size >= 3) {
            testPoint = point
            _uiState.update { it.copy(nextClickIsTestPoint = false) }
            redraw()
            return
        }
        vertices += point
        testPoint = null
        redraw()
    }

    private fun onClickInterpolate(point: LatLng) {
        when {
            pointA == null -> pointA = point
            pointB == null -> pointB = point
            else -> {
                pointA = point
                pointB = null
            }
        }
        redraw()
    }

    private fun onClickPath(point: LatLng) {
        if (_uiState.value.nextClickIsTestPoint && vertices.size >= 2) {
            testPoint = point
            _uiState.update { it.copy(nextClickIsTestPoint = false) }
            redraw()
            return
        }
        vertices += point
        testPoint = null
        redraw()
    }

    private fun onClickDistanceToSegment(point: LatLng) {
        when {
            segmentStart == null -> segmentStart = point
            segmentEnd == null -> segmentEnd = point
            testPoint == null -> testPoint = point
            else -> {
                segmentStart = point
                segmentEnd = null
                testPoint = null
            }
        }
        redraw()
    }

    private fun onClickSegmentIndex(point: LatLng) {
        if (_uiState.value.nextClickIsTestPoint && vertices.size >= 2) {
            testPoint = point
            _uiState.update { it.copy(nextClickIsTestPoint = false) }
            redraw()
            return
        }
        if (testPoint != null) {
            testPoint = point
            redraw()
            return
        }
        vertices += point
        redraw()
    }

    private fun onClickIsClosed(point: LatLng) {
        val closed = vertices.size >= 2 && PolyUtil.isClosedPolygon(vertices)
        if (closed) {
            resetPoints()
            vertices += point
            redraw()
            return
        }
        if (vertices.size >= 3) {
            val first = vertices.first()
            val dist = SphericalUtil.computeDistanceBetween(point, first)
            if (dist <= 80.0 && !sameLatLng(point, first)) {
                vertices += first
                redraw()
                return
            }
        }
        vertices += point
        redraw()
    }

    private fun resetPoints() {
        vertices.clear()
        pointA = null
        pointB = null
        segmentStart = null
        segmentEnd = null
        testPoint = null
        referencePoint = null
        targetTo = null
    }

    fun redraw() {
        layer.clearOverlays()
        val state = _uiState.value
        val cap = state.capability
        var fitPoints: List<LatLng> = emptyList()
        var status = capLabel(cap)
        var panel = ""
        var encodedPreview = _uiState.value.encodedPreview
        var decodedCount = _uiState.value.decodedCount

        try {
            when (cap) {
                GeometryDemoCapability.Distance -> {
                    val (s, p, f) = drawDistance()
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.Angle -> {
                    val (s, p, f) = drawAngle()
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.Area -> {
                    val (s, p, f) = drawArea(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.SignedArea -> {
                    val (s, p, f) = drawSignedArea()
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.Offset -> {
                    val (s, p, f) = drawOffset(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.OffsetOrigin -> {
                    val (s, p, f) = drawOffsetOrigin(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.Interpolate -> {
                    val (s, p, f) = drawInterpolate(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.Path -> {
                    val (s, p, f) = drawPath(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.SegmentIndex -> {
                    val (s, p, f) = drawSegmentIndex(state)
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.DistanceToSegment -> {
                    val (s, p, f) = drawDistanceToSegment()
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.IsClosed -> {
                    val (s, p, f) = drawIsClosed()
                    status = s; panel = p; fitPoints = f
                }
                GeometryDemoCapability.EncodeDecode -> {
                    val result = drawEncode()
                    status = result.status
                    panel = result.panel
                    fitPoints = result.fitPoints
                    encodedPreview = result.encoded
                    decodedCount = result.decodedSize
                }
            }
        } catch (e: GeometryException) {
            status = "${capLabel(cap)} · ${e.errorCode}"
            panel = e.message.orEmpty()
        }

        _uiState.update {
            it.copy(
                statusLine = status,
                panelDetail = panel,
                encodedPreview = if (cap == GeometryDemoCapability.EncodeDecode) encodedPreview else "",
                decodedCount = if (cap == GeometryDemoCapability.EncodeDecode) decodedCount else 0,
            )
        }

        if (fitPoints.isNotEmpty() && shouldAutoFit(cap, fitPoints)) {
            layer.fitTo(fitPoints)
        }
    }

    private fun shouldAutoFit(cap: GeometryDemoCapability, points: List<LatLng>): Boolean {
        return when (cap) {
            GeometryDemoCapability.Distance,
            GeometryDemoCapability.Angle,
            GeometryDemoCapability.Offset,
            GeometryDemoCapability.OffsetOrigin,
            GeometryDemoCapability.Interpolate,
            GeometryDemoCapability.Area,
            GeometryDemoCapability.SignedArea,
            GeometryDemoCapability.IsClosed,
            -> points.size >= 2
            GeometryDemoCapability.Path,
            GeometryDemoCapability.EncodeDecode,
            -> points.size >= 3
            else -> points.size >= 2
        }
    }

    private fun drawDistance(): Triple<String, String, List<LatLng>> {
        vertices.forEachIndexed { index, p ->
            layer.addLabeledPoint(p, if (index == 0) "A" else "B")
        }
        if (vertices.size == 2) {
            layer.addGeodesicLine(vertices[0], vertices[1], GeometryDemoColors.BLUE, 10f)
        } else if (vertices.size > 2) {
            layer.addGeodesicLine(vertices[0], vertices[1], GeometryDemoColors.BLUE, 10f)
        }
        if (vertices.size < 2) {
            val need = 2 - vertices.size
            return Triple(
                "测距 · 已 ${vertices.size} 点，还需 $need 点",
                "",
                vertices.toList(),
            )
        }
        val a = vertices[0]
        val b = vertices[1]
        val dist = SphericalUtil.computeDistanceBetween(a, b)
        val heading = SphericalUtil.computeHeading(a, b)
        val mid = SphericalUtil.interpolate(a, b, 0.5)
        layer.addBubble(mid, "距离 ${"%.1f".format(dist)} m", "方位角 ${"%.1f".format(heading)}°")
        return Triple(
            "测距 · Distance = ${"%.1f".format(dist)} m · Heading = ${"%.1f".format(heading)}°",
            "computeDistanceBetween = ${"%.2f".format(dist)} m\ncomputeHeading = ${"%.2f".format(heading)}°",
            listOf(a, b),
        )
    }

    private fun drawAngle(): Triple<String, String, List<LatLng>> {
        vertices.forEachIndexed { index, p ->
            layer.addLabeledPoint(p, if (index == 0) "A" else "B", R.drawable.geometry_point_purple)
        }
        if (vertices.size == 2) {
            layer.addGeodesicLine(vertices[0], vertices[1], GeometryDemoColors.PURPLE, 8f)
            layer.addGeodesicLine(vertices[0], vertices[1], GeometryDemoColors.BLUE, 4f)
        }
        if (vertices.size < 2) {
            return Triple("夹角 · 已 ${vertices.size} 点，还需 ${2 - vertices.size} 点", "", vertices.toList())
        }
        val rad = SphericalUtil.computeAngleBetween(vertices[0], vertices[1])
        val deg = Math.toDegrees(rad)
        val mid = SphericalUtil.interpolate(vertices[0], vertices[1], 0.5)
        layer.addBubble(mid, "${"%.4f".format(rad)} rad", "${"%.2f".format(deg)}°")
        return Triple(
            "夹角 · computeAngleBetween = ${"%.6f".format(rad)} rad",
            "Radians = ${"%.6f".format(rad)}\nDegrees = ${"%.2f".format(deg)}°",
            vertices.take(2),
        )
    }

    private fun drawArea(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        vertices.forEach { layer.addDot(it, GeometryDemoColors.BLUE) }
        if (vertices.size >= 2) {
            layer.addLine(vertices, GeometryDemoColors.BLUE, 4f)
        }
        if (vertices.size >= 3) {
            layer.addPolygon(vertices, GeometryDemoColors.RED, GeometryDemoColors.RED_FILL, 3f)
            val area = SphericalUtil.computeArea(vertices)
            val centroid = polygonCentroid(vertices)
            layer.addBubble(centroid, "区域面积 ${"%.1f".format(area)} m²")
            testPoint?.let { q ->
                val inside = PolyUtil.containsLocation(q, vertices, geodesic = true)
                val onEdge = PolyUtil.isLocationOnEdge(q, vertices, geodesic = true, toleranceMeters = state.spatialToleranceM.toDouble())
                val color = when {
                    onEdge -> GeometryDemoColors.ORANGE
                    inside -> GeometryDemoColors.GREEN
                    else -> GeometryDemoColors.ORANGE
                }
                layer.addDot(q, color, 8.0)
                layer.addBubble(
                    q,
                    "在多边形内 = $inside",
                    if (onEdge) "在边上" else "",
                )
            }
            return Triple(
                "面积 · computeArea = ${"%.1f".format(area)} m²",
                "computeArea = ${"%.1f".format(area)} m²",
                vertices + listOfNotNull(testPoint),
            )
        }
        val need = (3 - vertices.size).coerceAtLeast(0)
        return Triple(
            "面积 · 已 ${vertices.size} 点，还需 ≥$need 点",
            if (vertices.isEmpty()) "" else "继续点击添加顶点",
            vertices.toList(),
        )
    }

    private fun drawSignedArea(): Triple<String, String, List<LatLng>> {
        vertices.forEachIndexed { index, p -> layer.addNumberedPoint(p, index + 1) }
        if (vertices.size >= 2) {
            layer.addLine(vertices, GeometryDemoColors.BLUE, 4f)
        }
        if (vertices.size >= 3) {
            val signed = SphericalUtil.computeSignedArea(vertices)
            val positive = signed >= 0
            val fill = if (positive) GeometryDemoColors.GREEN_FILL else GeometryDemoColors.RED_FILL
            val stroke = if (positive) GeometryDemoColors.GREEN else GeometryDemoColors.RED
            layer.addPolygon(vertices, stroke, fill, 3f)
            val centroid = polygonCentroid(vertices)
            val orient = if (positive) "正（多为逆时针）" else "负（多为顺时针）"
            layer.addBubble(centroid, "带符号面积 ${"%.1f".format(signed)} m²", orient)
            return Triple(
                "带符号面积 · computeSignedArea = ${"%.1f".format(signed)} m² · $orient",
                "computeSignedArea = ${"%.1f".format(signed)} m²\n$orient",
                vertices.toList(),
            )
        }
        return Triple(
            "带符号面积 · 已 ${vertices.size} 点，还需 ≥${(3 - vertices.size).coerceAtLeast(0)} 点",
            "",
            vertices.toList(),
        )
    }

    private fun drawOffset(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        val a = referencePoint ?: return Triple("偏移 · 请选择参考点 A", "", emptyList())
        layer.addLabeledPoint(a, "A")
        val b = SphericalUtil.computeOffset(a, state.offsetDistanceM.toDouble(), state.offsetHeadingDeg.toDouble())
        layer.addLabeledPoint(b, "B", R.drawable.geometry_point)
        layer.addGeodesicLine(a, b, GeometryDemoColors.BLUE, 8f)
        val mid = SphericalUtil.interpolate(a, b, 0.5)
        layer.addBubble(
            mid,
            "Offset ${state.offsetDistanceM.toInt()} m / ${state.offsetHeadingDeg.toInt()}°",
        )
        return Triple(
            "偏移 · 从 A 沿 ${state.offsetHeadingDeg.toInt()}° 走 ${state.offsetDistanceM.toInt()} m → B",
            "computeOffset → (${"%.5f".format(b.latitude)}, ${"%.5f".format(b.longitude)})",
            listOf(a, b),
        )
    }

    private fun drawOffsetOrigin(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        val to = targetTo ?: return Triple("反推原点 · 请选择目标 To", "", emptyList())
        layer.addDot(to, GeometryDemoColors.GREEN, 10.0)
        layer.addCaptionLabel(to, "To")
        val origin = SphericalUtil.computeOffsetOrigin(
            to,
            state.originDistanceM.toDouble(),
            state.originHeadingDeg.toDouble(),
        )
        if (origin == null) {
            layer.addBubble(to, "无解（null）")
            return Triple(
                "反推原点 · computeOffsetOrigin = null",
                "computeOffsetOrigin = null",
                listOf(to),
            )
        }
        layer.addLabeledPoint(origin, "From")
        layer.addGeodesicLine(origin, to, GeometryDemoColors.PURPLE, 8f)
        val mid = SphericalUtil.interpolate(origin, to, 0.5)
        layer.addBubble(
            mid,
            "From 反推",
            "d=${state.originDistanceM.toInt()} m θ=${state.originHeadingDeg.toInt()}°",
        )
        return Triple(
            "反推原点 · From = (${"%.5f".format(origin.latitude)}, ${"%.5f".format(origin.longitude)})",
            "computeOffsetOrigin 有解",
            listOf(origin, to),
        )
    }

    private fun drawInterpolate(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        val a = pointA ?: return Triple("插值 · 请选择 A", "", emptyList())
        layer.addLabeledPoint(a, "A")
        val b = pointB ?: return Triple("插值 · 请选择 B", "", listOf(a))
        layer.addLabeledPoint(b, "B")
        layer.addGeodesicLine(a, b, GeometryDemoColors.BLUE, 8f)
        val mid = SphericalUtil.interpolate(a, b, state.interpolateFraction.toDouble())
        layer.addDot(mid, GeometryDemoColors.ORANGE, 10.0)
        val pct = (state.interpolateFraction * 100).toInt()
        layer.addBubble(mid, "Interpolate $pct%")
        return Triple(
            "插值 · fraction = ${"%.2f".format(state.interpolateFraction)}",
            "interpolate → (${"%.5f".format(mid.latitude)}, ${"%.5f".format(mid.longitude)})",
            listOf(a, b, mid),
        )
    }

    private fun drawPath(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        if (vertices.size < 3) {
            layer.addLine(vertices, GeometryDemoColors.BLUE, 5f)
            vertices.forEach { layer.addDot(it, GeometryDemoColors.BLUE) }
            return Triple(
                "路径 · 已 ${vertices.size} 点（≥3 可抽稀）",
                "",
                vertices.toList(),
            )
        }
        val simplified = PolyUtil.simplify(vertices.toList(), state.simplifyToleranceM.toDouble())
        val offsetSimplified = offsetPath(simplified, 55.0)
        val perp = SphericalUtil.computeHeading(vertices[0], vertices[1]) + 90.0

        layer.addLine(vertices, GeometryDemoColors.BLUE, 5f)
        vertices.forEach { point ->
            val kept = simplified.any { sameLatLng(point, it) }
            layer.addDot(point, if (kept) GeometryDemoColors.GREEN else GeometryDemoColors.BLUE)
        }
        layer.addLine(offsetSimplified, GeometryDemoColors.GREEN, 12f)
        offsetSimplified.forEachIndexed { index, point ->
            layer.addDot(point, GeometryDemoColors.GREEN)
            layer.addCaptionLabel(point, "保${index + 1}")
        }
        val originalCaptionAt = SphericalUtil.computeOffset(pathMidpoint(vertices), 90.0, perp + 180.0)
        val simplifiedCaptionAt = SphericalUtil.computeOffset(pathMidpoint(offsetSimplified), 90.0, perp)
        layer.addCaptionLabel(originalCaptionAt, "蓝细线=原 ${vertices.size} 点（绿圆=保留）")
        layer.addCaptionLabel(simplifiedCaptionAt, "绿粗线=抽稀后 ${simplified.size} 点（已侧移）")

        testPoint?.let { q ->
            layer.addDot(q, GeometryDemoColors.PURPLE, 8.0)
            val onPath = PolyUtil.isLocationOnPath(q, vertices, geodesic = true, toleranceMeters = state.spatialToleranceM.toDouble())
            val index = PolyUtil.locationIndexOnPath(q, vertices, geodesic = true, toleranceMeters = state.spatialToleranceM.toDouble())
            layer.addBubble(q, "在路径上 = $onPath", "段索引 = $index")
        }

        return Triple(
            "路径 · simplify ${vertices.size} → ${simplified.size} 点",
            "PolyUtil.simplify(tol=${state.simplifyToleranceM.toInt()} m)",
            vertices + offsetSimplified + listOfNotNull(testPoint),
        )
    }

    private fun drawSegmentIndex(state: GeometryDemoUiState): Triple<String, String, List<LatLng>> {
        if (vertices.size >= 2) {
            layer.addLine(vertices, GeometryDemoColors.BLUE, 5f)
        }
        vertices.forEach { layer.addDot(it, GeometryDemoColors.BLUE) }
        if (state.pathClosedForIndex && vertices.size >= 3) {
            layer.addGeodesicLine(vertices.last(), vertices.first(), GeometryDemoColors.ORANGE, 6f)
            val mid = SphericalUtil.interpolate(vertices.last(), vertices.first(), 0.5)
            layer.addCaptionLabel(mid, "闭合边")
        }
        val q = testPoint
        if (q != null && vertices.size >= 2) {
            layer.addDot(q, GeometryDemoColors.PURPLE, 8.0)
            val idx = PolyUtil.locationIndexOnEdgeOrPath(
                q,
                vertices,
                state.pathClosedForIndex,
                geodesic = true,
                toleranceMeters = state.spatialToleranceM.toDouble(),
            )
            layer.addBubble(q, "段/边索引 = $idx")
            return Triple(
                "闭合段索引 · locationIndexOnEdgeOrPath = $idx",
                "closed = ${state.pathClosedForIndex}\nindex = $idx",
                vertices + listOf(q),
            )
        }
        return Triple(
            "${capLabel(GeometryDemoCapability.SegmentIndex)} · ${vertices.size} pts",
            txt(R.string.geometry_demo_panel_segment_index) + "\nclosed = ${state.pathClosedForIndex}",
            vertices.toList(),
        )
    }

    private fun drawDistanceToSegment(): Triple<String, String, List<LatLng>> {
        segmentStart?.let { layer.addLabeledPoint(it, "S") }
        segmentEnd?.let { layer.addLabeledPoint(it, "E") }
        if (segmentStart != null && segmentEnd != null) {
            layer.addLine(listOf(segmentStart!!, segmentEnd!!), GeometryDemoColors.BLUE, 6f)
        }
        val q = testPoint
        if (segmentStart != null && segmentEnd != null && q != null) {
            layer.addDot(q, GeometryDemoColors.ORANGE, 8.0)
            val dist = PolyUtil.distanceToLine(q, segmentStart!!, segmentEnd!!)
            layer.addBubble(q, "距离 ${"%.1f".format(dist)} m")
            return Triple(
                "点到线段 · distanceToLine = ${"%.1f".format(dist)} m",
                "PolyUtil.distanceToLine = ${"%.2f".format(dist)} m",
                listOf(segmentStart!!, segmentEnd!!, q),
            )
        }
        val step = when {
            segmentStart == null -> "起点 S"
            segmentEnd == null -> "终点 E"
            else -> "待测点"
        }
        return Triple(
            "${capLabel(GeometryDemoCapability.DistanceToSegment)} · $step",
            txt(R.string.geometry_demo_panel_distance_to_segment),
            listOfNotNull(segmentStart, segmentEnd, q),
        )
    }

    private fun drawIsClosed(): Triple<String, String, List<LatLng>> {
        val closed = vertices.size >= 2 && PolyUtil.isClosedPolygon(vertices)
        val ring = if (closed && vertices.size >= 2) vertices.dropLast(1) else vertices

        ring.forEachIndexed { index, p -> layer.addNumberedPoint(p, index + 1) }
        if (ring.size >= 2) {
            layer.addLine(if (closed) vertices else ring, GeometryDemoColors.BLUE, 5f)
        }
        if (closed && ring.size >= 3) {
            layer.addPolygon(ring, GeometryDemoColors.GREEN, GeometryDemoColors.GREEN_FILL, 3f)
            layer.addBubble(polygonCentroid(ring), "isClosedPolygon = true")
            return Triple(
                "是否闭合 · isClosedPolygon = true",
                "PolyUtil.isClosedPolygon = true",
                vertices.toList(),
            )
        }
        if (vertices.size == 1) {
            return Triple("是否闭合 · 继续加点…", "点数=1，不算闭合", vertices.toList())
        }
        if (vertices.size >= 3 && !closed) {
            layer.addSnapCircle(vertices.first(), 80.0, GeometryDemoColors.GREEN)
        }
        return Triple(
            "${capLabel(GeometryDemoCapability.IsClosed)} · ${vertices.size} pts",
            txt(R.string.geometry_demo_panel_is_closed) + "\nisClosedPolygon = false",
            vertices.toList(),
        )
    }

    private data class EncodeDrawResult(
        val status: String,
        val panel: String,
        val fitPoints: List<LatLng>,
        val encoded: String,
        val decodedSize: Int,
    )

    private fun drawEncode(): EncodeDrawResult {
        layer.addLine(vertices, GeometryDemoColors.BLUE, 5f)
        vertices.forEach { layer.addDot(it, GeometryDemoColors.BLUE) }
        if (vertices.size < 2) {
            return EncodeDrawResult("编解码 · 至少 2 点", "", vertices.toList(), "", 0)
        }
        val encoded = PolyUtil.encodePath(vertices)
        val decoded = PolyUtil.decodePath(encoded)
        val preview = if (encoded.length > 48) encoded.take(48) + "…" else encoded
        return EncodeDrawResult(
            status = "编解码 · Encoded = $preview",
            panel = "Decoded 点数 = ${decoded.size}",
            fitPoints = vertices.toList(),
            encoded = encoded,
            decodedSize = decoded.size,
        )
    }
}
