package com.maptec.applied.demo.ui.screens.tools.geometry

import android.content.Context
import android.graphics.Color as AndroidColor
import android.widget.FrameLayout
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.view.BubbleCalloutView
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.geometry.util.SphericalUtil
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.infowindow.InfoWindow
import com.maptec.applied.maps.overlay.infowindow.InfoWindowAnchor
import com.maptec.applied.maps.overlay.infowindow.InfoWindowOptions
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions

internal class GeometryDemoMapLayer {
    private var engine: MapOverlayEngine? = null
    private var map: MaptecMap? = null
    private var appContext: Context? = null
    private val lines = mutableListOf<Line>()
    private val fills = mutableListOf<Fill>()
    private val circles = mutableListOf<Circle>()
    private val markers = mutableListOf<Marker>()
    private val infoWindows = mutableListOf<InfoWindow>()

    fun attach(map: MaptecMap, context: Context) {
        this.map = map
        engine = map.overlayEngine
        appContext = context.applicationContext
        engine?.setMarkerLayerCollisionEnabled(false)
    }

    fun clearOverlays() {
        val overlayEngine = engine ?: return
        lines.forEach { overlayEngine.deleteLine(it) }
        fills.forEach { overlayEngine.deleteFill(it) }
        circles.forEach { overlayEngine.deleteCircle(it) }
        markers.forEach { overlayEngine.deleteMarker(it) }
        infoWindows.forEach { runCatching { it.remove() } }
        lines.clear()
        fills.clear()
        circles.clear()
        markers.clear()
        infoWindows.clear()
    }

    fun addGeodesicLine(a: LatLng, b: LatLng, color: String, width: Float) {
        addLine(densifyGreatCircle(a, b), color, width)
    }

    fun addLine(points: List<LatLng>, color: String, width: Float) {
        val overlayEngine = engine ?: return
        if (points.size < 2) return
        lines += overlayEngine.addPolyline(
            LineOptions().withLatLngs(ArrayList(points)).withStrokeColor(color).withStrokeWeight(width),
        )
    }

    fun addPolygon(points: List<LatLng>, strokeColor: String, fillColor: String, strokeWeight: Float = 2f) {
        val overlayEngine = engine ?: return
        if (points.size < 3) return
        fills += overlayEngine.addPolygon(
            FillOptions()
                .withLatLngs(listOf(ArrayList(points)))
                .withFillColor(fillColor)
                .withStrokeColor(strokeColor)
                .withStrokeWeight(strokeWeight),
        )
    }

    fun addDot(point: LatLng, colorHex: String, radiusMeters: Double = 6.0) {
        val overlayEngine = engine ?: return
        circles += overlayEngine.addCircle(
            CircleOptions()
                .withCenter(point)
                .withRadius(radiusMeters)
                .withFillColor(colorHex)
                .withFillOpacity(1f)
                .withStrokeColor(colorHex)
                .withStrokeWeight(1f),
        )
    }

    fun addSnapCircle(center: LatLng, radiusMeters: Double, colorHex: String) {
        val overlayEngine = engine ?: return
        circles += overlayEngine.addCircle(
            CircleOptions()
                .withCenter(center)
                .withRadius(radiusMeters)
                .withFillColor(colorHex)
                .withFillOpacity(0.15f)
                .withStrokeColor(colorHex)
                .withStrokeWeight(2f),
        )
    }

    fun addLabeledPoint(point: LatLng, label: String, iconRes: Int = R.drawable.geometry_point) {
        val overlayEngine = engine ?: return
        val context = appContext ?: return
        markers += overlayEngine.addMarker(
            MarkerOptions()
                .withLatLng(point)
                .withIconResource(iconRes, context)
                .withText(label)
                .withTextSize(12)
                .withTextColor(AndroidColor.parseColor("#1B1B1B"))
                .withClickable(false),
        )
    }

    fun addNumberedPoint(point: LatLng, index: Int) {
        addLabeledPoint(point, index.toString())
    }

    fun addCaptionLabel(point: LatLng, text: String, iconScale: Float = 0.45f) {
        val overlayEngine = engine ?: return
        val context = appContext ?: return
        runCatching {
            markers += overlayEngine.addMarker(
                MarkerOptions()
                    .withLatLng(point)
                    .withIconResource(R.drawable.geometry_point, context)
                    .withIconSize(iconScale)
                    .withText(text)
                    .withTextSize(11)
                    .withTextColor(AndroidColor.parseColor("#1B1B1B"))
                    .withClickable(false),
            )
        }
    }

    fun addBubble(point: LatLng, line1: String, line2: String = "") {
        val overlayEngine = engine ?: return
        val context = appContext ?: return
        runCatching {
            val view = BubbleCalloutView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
                line1Text = line1
                line2Text = line2
                isFixedWidth = false
                maxWidthPx = (280 * context.resources.displayMetrics.density).toInt()
            }
            infoWindows += overlayEngine.addInfoWindow(
                InfoWindowOptions()
                    .withLatLng(point)
                    .withContentView(view)
                    .withAnchor(InfoWindowAnchor.BOTTOM)
                    .withAutoCloseOnMapClick(false),
            )
        }
    }

    fun fitTo(points: List<LatLng>, paddingPx: Int = 72) {
        val maptecMap = map ?: return
        if (points.isEmpty()) return
        if (points.size == 1) {
            maptecMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15.0))
            return
        }
        val bounds = LatLngBounds.Builder().also { builder ->
            points.forEach { builder.include(it) }
        }.build()
        maptecMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
    }
}

internal fun densifyGreatCircle(a: LatLng, b: LatLng): List<LatLng> {
    return (0..16).map { SphericalUtil.interpolate(a, b, it / 16.0) }
}

internal fun offsetPath(points: List<LatLng>, meters: Double): List<LatLng> {
    if (points.size < 2) return points.toList()
    val heading = SphericalUtil.computeHeading(points.first(), points[1])
    val perp = heading + 90.0
    return points.map { SphericalUtil.computeOffset(it, meters, perp) }
}

internal fun pathMidpoint(points: List<LatLng>): LatLng {
    if (points.size <= 2) return points[points.size / 2]
    return points[points.size / 2]
}

internal fun polygonCentroid(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return LatLng(0.0, 0.0)
    val lat = points.sumOf { it.latitude } / points.size
    val lng = points.sumOf { it.longitude } / points.size
    return LatLng(lat, lng)
}

internal fun sameLatLng(a: LatLng, b: LatLng): Boolean {
    return kotlin.math.abs(a.latitude - b.latitude) < 1e-8 &&
        kotlin.math.abs(a.longitude - b.longitude) < 1e-8
}

private const val SIMPLIFY_EXAMPLE_SPACING_METERS = 200.0
private const val SIMPLIFY_EXAMPLE_JITTER_METERS = 12.0
internal val SIMPLIFY_EXAMPLE_ORIGIN = LatLng(1.3485, 103.8148)

/** 大 L 形：点距约 200m、轻微抖动，抽稀后通常 8→3。 */
internal fun buildClearSimplifyExample(origin: LatLng = SIMPLIFY_EXAMPLE_ORIGIN): List<LatLng> {
    val east = 90.0
    val north = 0.0
    val alongEast: (Int) -> LatLng = { i ->
        SphericalUtil.computeOffset(origin, SIMPLIFY_EXAMPLE_SPACING_METERS * i, east)
    }
    val corner = alongEast(3)
    val alongNorth: (Int) -> LatLng = { i ->
        SphericalUtil.computeOffset(corner, SIMPLIFY_EXAMPLE_SPACING_METERS * i, north)
    }
    return listOf(
        origin,
        SphericalUtil.computeOffset(alongEast(1), SIMPLIFY_EXAMPLE_JITTER_METERS, north),
        SphericalUtil.computeOffset(alongEast(2), SIMPLIFY_EXAMPLE_JITTER_METERS, 180.0),
        corner,
        SphericalUtil.computeOffset(alongNorth(1), SIMPLIFY_EXAMPLE_JITTER_METERS, east),
        SphericalUtil.computeOffset(alongNorth(2), SIMPLIFY_EXAMPLE_JITTER_METERS, -90.0),
        SphericalUtil.computeOffset(alongNorth(3), SIMPLIFY_EXAMPLE_JITTER_METERS, east),
        alongNorth(4),
    )
}

internal fun buildRightTriangle(clockwise: Boolean, anchor: LatLng = LatLng(1.3498, 103.8165)): List<LatLng> {
    val a = anchor
    val b = SphericalUtil.computeOffset(anchor, 300.0, 90.0)
    val c = SphericalUtil.computeOffset(anchor, 300.0, 0.0)
    return if (clockwise) listOf(a, c, b) else listOf(a, b, c)
}
