@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.data.GeofenceData
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.overlays.buildCircleOptions
import com.maptec.applied.demo.ui.screens.overlays.buildFillOptions
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.style.layers.Property
import com.maptec.applied.utils.BitmapUtils
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point as GeoPoint
import org.maplibre.geojson.Polygon as GeoPolygon

private const val GEOFENCE_MARKER_ICON_ID = "geofence_default_pin"

private val GEOFENCE_CAMERA_TARGET = LatLng(39.908, 116.394)

private data class CircleMarkerGeofence(
    val id: String,
    val name: String,
    val center: LatLng,
    val radiusMeters: Double,
)

private data class PolygonMarkerGeofence(
    val id: String,
    val name: String,
    val rings: List<List<LatLng>>,
)

private sealed interface ParsedMarkerGeofence {
    val id: String
    val name: String
    data class CircleMarkerType(val data: CircleMarkerGeofence) : ParsedMarkerGeofence {
        override val id get() = data.id
        override val name get() = data.name
    }
    data class PolygonMarkerType(val data: PolygonMarkerGeofence) : ParsedMarkerGeofence {
        override val id get() = data.id
        override val name get() = data.name
    }
}

private fun parseGeofence(json: String): ParsedMarkerGeofence? {
    val feature = runCatching { Feature.fromJson(json) }.getOrNull() ?: return null
    val geom = feature.geometry() ?: return null
    val id = feature.id().orEmpty()
    val name = feature.getStringProperty("name").orEmpty()
    return when (geom) {
        is GeoPoint -> {
            val radius = feature.getNumberProperty("radius")?.toDouble() ?: return null
            ParsedMarkerGeofence.CircleMarkerType(
                CircleMarkerGeofence(id, name, LatLng(geom.latitude(), geom.longitude()), radius)
            )
        }
        is GeoPolygon -> {
            val rings = geom.coordinates()?.map { ring ->
                ring.map { LatLng(it.latitude(), it.longitude()) }
            } ?: return null
            if (rings.isEmpty() || rings.first().size < 4) return null
            ParsedMarkerGeofence.PolygonMarkerType(PolygonMarkerGeofence(id, name, rings))
        }
        else -> null
    }
}

private fun buildCircleOptions(data: CircleMarkerGeofence): CircleOptions =
    CircleOptions()
        .withCenter(data.center)
        .withRadius(data.radiusMeters)
        .withGeodesic(true)
        // 默认段数偏少，描边会出现明显锯齿，提升到 256 段让圆周更平顺
        .withSegments(256)
        .withFillColor("#3A5EFB")
        .withFillOpacity(0.25f)
        .withStrokeColor("#3A5EFB")
        .withStrokeWeight(3f)
        .withStrokeOpacity(1f)

private fun buildFillOptions(data: PolygonMarkerGeofence): FillOptions =
    FillOptions()
        .withLatLngs(data.rings)
        .withFillColor("#3A5EFB")
        .withFillOpacity(0.25f)
        .withStrokeColor("#3A5EFB")
        .withStrokeWeight(3f)
        .withStrokeOpacity(1f)
        .withFillAntialias(true)


@Composable
fun GeofenceMarkerScreen() {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var defaultFencesLoaded by remember { mutableStateOf(false) }
    val polygonFences = remember { mutableStateListOf<Pair<Fill, PolygonMarkerGeofence>>() }
    val circleFences = remember { mutableStateListOf<Pair<Circle, CircleMarkerGeofence>>() }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var statusText by remember { mutableStateOf("") }
    val defaultStatusText = stringResource(R.string.geofence_status_default)
    LaunchedEffect(defaultStatusText) {
        if (statusText.isEmpty()) {
            statusText = defaultStatusText
        }
    }
    var statusInside by remember { mutableStateOf<Boolean?>(null) }

    val loadPoint: () -> Unit = load@{
        val engine = mapRef?.getOverlayEngine() ?: return@load
        val parsed = parseGeofence(GeofenceData.POINT_GEOFENCE_JSON) as? ParsedMarkerGeofence.CircleMarkerType ?: return@load
        if (circleFences.any { it.second.id == parsed.data.id }) {
            Toast.makeText(context, "Point 围栏已加载", Toast.LENGTH_SHORT).show()
            return@load
        }
        engine.addCircle(buildCircleOptions(parsed.data))?.let {
            circleFences.add(it to parsed.data)
            statusText = "已加载 Point 围栏「${parsed.data.name}」"
            statusInside = null
        }
    }

    val loadPolygon: () -> Unit = load@{
        val engine = mapRef?.getOverlayEngine() ?: return@load
        val parsed = parseGeofence(GeofenceData.POLYGON_GEOFENCE_JSON) as? ParsedMarkerGeofence.PolygonMarkerType ?: return@load
        if (polygonFences.any { it.second.id == parsed.data.id }) {
            Toast.makeText(context, "Polygon 围栏已加载", Toast.LENGTH_SHORT).show()
            return@load
        }
        engine.addPolygon(buildFillOptions(parsed.data))?.let {
            polygonFences.add(it to parsed.data)
            statusText = "已加载 Polygon 围栏「${parsed.data.name}」"
            statusInside = null
        }
    }

    val clearAll: () -> Unit = clear@{
        val engine = mapRef?.getOverlayEngine() ?: return@clear
        engine.deleteAllCircles()
        engine.deleteAllFills()
        circleFences.clear()
        polygonFences.clear()
        currentMarker?.remove()
        currentMarker = null
        statusText = "已清除所有覆盖物"
        statusInside = null
    }

    val clearMarker: () -> Unit = {
        currentMarker?.remove()
        currentMarker = null
        statusText = "已清除 Marker，请重新点击地图"
        statusInside = null
    }

    // 注册地图点击：放置/移动单个 Marker，并判断是否落在任一围栏内
    DisposableEffect(mapRef) {
        val map = mapRef
        if (map == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = MaptecMap.OnMapClickListener { point ->
            currentMarker?.remove()
            val engine = map.getOverlayEngine()
            
            val drawable = ContextCompat.getDrawable(context, R.drawable.red_dot)
            val bitmap = drawable?.let { BitmapUtils.getBitmapFromDrawable(it) }
            
            val options = MarkerOptions()
                .withLatLng(point)
                .withIcon(GEOFENCE_MARKER_ICON_ID, bitmap!!, false)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withIconSize(2.0f)
            currentMarker = engine.addMarker(options)

            val hitName = polygonFences.firstOrNull { (fill, _) -> fill.contains(point) }?.second?.name
                ?: circleFences.firstOrNull { (_, c) ->
                    c.center.distanceTo(point) <= c.radiusMeters
                }?.second?.name
            val inside = hitName != null
            statusInside = inside
            val msg = if (inside) "Marker 在围栏「$hitName」内" else "Marker 不在任何围栏内"
            statusText = "${msg}：(${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)})"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            true
        }
        map.addOnMapClickListener(listener)
        onDispose {
            map.removeOnMapClickListener(listener)
        }
    }

    val sheetState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val collapseSheet = {
        scope.launch { sheetState.bottomSheetState.partialExpand() }
        Unit
    }
    BottomSheetScaffold(
        scaffoldState = sheetState,
        // 收起时只露出抓手，所有 button 都在抽屉里看不到
        sheetPeekHeight = 56.dp,
        modifier = Modifier.fillMaxSize().testTag("geofence_screen"),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { loadPoint(); collapseSheet() },
                    modifier = Modifier.fillMaxWidth().testTag("geofence_load_point"),
                ) {
                    Text(stringResource(R.string.geofence_load_point))
                }
                Button(
                    onClick = { loadPolygon(); collapseSheet() },
                    modifier = Modifier.fillMaxWidth().testTag("geofence_load_polygon"),
                ) {
                    Text(stringResource(R.string.geofence_load_polygon))
                }
                OutlinedButton(
                    onClick = { clearAll(); collapseSheet() },
                    modifier = Modifier.fillMaxWidth().testTag("geofence_clear_all"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.geofence_clear_all))
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { view, mapLibreMap ->
                    mapViewRef = view
                    mapRef = mapLibreMap
                    mapLibreMap.getStyle {
                        if (defaultFencesLoaded) return@getStyle
                        defaultFencesLoaded = true

                        val engine: MapOverlayEngine = mapLibreMap.getOverlayEngine()
                        engine.deleteAllFills()
                        engine.deleteAllCircles()
                        polygonFences.clear()
                        circleFences.clear()

                        // 默认只加载 Polygon 围栏
                        (parseGeofence(GeofenceData.POLYGON_GEOFENCE_JSON) as? ParsedMarkerGeofence.PolygonMarkerType)?.let { gf ->
                            engine.addPolygon(buildFillOptions(gf.data))?.let {
                                polygonFences.add(it to gf.data)
                            }
                        }

                        mapLibreMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(GEOFENCE_CAMERA_TARGET, 15.0)
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xCCFFFFFF))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = statusText,
                    color = when (statusInside) {
                        true -> Color(0xFF1B7F2A)
                        false -> Color(0xFFC62828)
                        null -> Color.DarkGray
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("geofence_status"),
                )
                Button(
                    onClick = clearMarker,
                    modifier = Modifier.fillMaxWidth().testTag("geofence_clear_marker"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.geofence_clear_marker))
                }
            }
        }
    }
}
