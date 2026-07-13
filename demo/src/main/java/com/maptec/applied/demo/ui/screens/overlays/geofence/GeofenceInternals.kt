@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays.geofence

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.data.GeofenceData
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
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
import com.maptec.applied.style.Property
import com.maptec.applied.utils.BitmapUtils
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point as GeoPoint
import org.maplibre.geojson.Polygon as GeoPolygon

private const val GEOFENCE_MARKER_ICON_ID = "geofence_default_pin"

private val GEOFENCE_POINT_CAMERA = LatLng(39.9087, 116.3975)
private val GEOFENCE_POLYGON_CAMERA = LatLng(39.9075, 116.3925)

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
                CircleMarkerGeofence(id, name, LatLng(geom.latitude(), geom.longitude()), radius),
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

internal enum class GeofenceMode {
    POINT,
    POLYGON,
}

@Composable
internal fun GeofenceScreen(
    mode: GeofenceMode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    val polygonFences = remember { mutableStateListOf<Pair<Fill, PolygonMarkerGeofence>>() }
    val circleFences = remember { mutableStateListOf<Pair<Circle, CircleMarkerGeofence>>() }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var statusText by remember { mutableStateOf("") }
    val defaultStatusText = stringResource(
        if (mode == GeofenceMode.POINT) {
            R.string.geofence_status_point_default
        } else {
            R.string.geofence_status_default
        },
    )
    var previousDefaultStatusText by remember { mutableStateOf(defaultStatusText) }
    LaunchedEffect(defaultStatusText) {
        if (statusText.isEmpty() || statusText == previousDefaultStatusText) {
            statusText = defaultStatusText
        }
        previousDefaultStatusText = defaultStatusText
    }
    var statusInside by remember { mutableStateOf<Boolean?>(null) }

    val loadPoint: () -> Unit = load@{
        val engine = mapRef?.getOverlayEngine() ?: return@load
        val parsed = parseGeofence(GeofenceData.POINT_GEOFENCE_JSON) as? ParsedMarkerGeofence.CircleMarkerType ?: return@load
        if (circleFences.any { it.second.id == parsed.data.id }) {
            Toast.makeText(context, context.getString(R.string.geofence_toast_point_already_loaded), Toast.LENGTH_SHORT).show()
            return@load
        }
        engine.addCircle(buildCircleOptions(parsed.data))?.let {
            circleFences.add(it to parsed.data)
            statusText = context.getString(R.string.geofence_status_point_loaded, parsed.data.name)
            statusInside = null
        }
    }

    val loadPolygon: () -> Unit = load@{
        val engine = mapRef?.getOverlayEngine() ?: return@load
        val parsed = parseGeofence(GeofenceData.POLYGON_GEOFENCE_JSON) as? ParsedMarkerGeofence.PolygonMarkerType ?: return@load
        if (polygonFences.any { it.second.id == parsed.data.id }) {
            Toast.makeText(context, context.getString(R.string.geofence_toast_polygon_already_loaded), Toast.LENGTH_SHORT).show()
            return@load
        }
        engine.addPolygon(buildFillOptions(parsed.data))?.let {
            polygonFences.add(it to parsed.data)
            statusText = context.getString(R.string.geofence_status_polygon_loaded, parsed.data.name)
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
        statusText = context.getString(R.string.geofence_status_cleared_all)
        statusInside = null
    }

    val clearMarker: () -> Unit = {
        currentMarker?.remove()
        currentMarker = null
        statusText = context.getString(R.string.geofence_status_clear_marker)
        statusInside = null
    }

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
            val msg = if (inside) {
                context.getString(R.string.geofence_marker_inside, hitName.orEmpty())
            } else {
                context.getString(R.string.geofence_marker_outside)
            }
            statusText = "${msg}：(${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)})"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            true
        }
        map.addOnMapClickListener(listener)
        onDispose {
            map.removeOnMapClickListener(listener)
        }
    }

    val configPanelController = LocalDemoConfigPanelController.current
    val collapseSheet = { configPanelController.close() }

    DemoPanelScaffold(
        modifier = Modifier.fillMaxSize().testTag("geofence_screen").then(modifier),
        sheetContent = {
            DemoPanelColumn {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (mode == GeofenceMode.POINT) {
                        DemoPanelButton(
                            onClick = { loadPoint(); collapseSheet() },
                            modifier = Modifier.fillMaxWidth().testTag("geofence_load_point"),
                        ) {
                            Text(stringResource(R.string.geofence_load_point))
                        }
                    }
                    if (mode == GeofenceMode.POLYGON) {
                        DemoPanelButton(
                            onClick = { loadPolygon(); collapseSheet() },
                            modifier = Modifier.fillMaxWidth().testTag("geofence_load_polygon"),
                        ) {
                            Text(stringResource(R.string.geofence_load_polygon))
                        }
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
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                Mapview(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { view, mapLibreMap ->
                        mapViewRef = view
                        mapLibreMap.getStyle { style ->
                            val engine: MapOverlayEngine = mapLibreMap.getOverlayEngine()
                            engine.deleteAllFills()
                            engine.deleteAllCircles()
                            polygonFences.clear()
                            circleFences.clear()

                            when (mode) {
                                GeofenceMode.POINT -> {
                                    (parseGeofence(GeofenceData.POINT_GEOFENCE_JSON) as? ParsedMarkerGeofence.CircleMarkerType)?.let { gf ->
                                        engine.addCircle(buildCircleOptions(gf.data))?.let {
                                            circleFences.add(it to gf.data)
                                        }
                                    }
                                }
                                GeofenceMode.POLYGON -> {
                                    (parseGeofence(GeofenceData.POLYGON_GEOFENCE_JSON) as? ParsedMarkerGeofence.PolygonMarkerType)?.let { gf ->
                                        engine.addPolygon(buildFillOptions(gf.data))?.let {
                                            polygonFences.add(it to gf.data)
                                        }
                                    }
                                }
                            }

                            mapLibreMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    when (mode) {
                                        GeofenceMode.POINT -> GEOFENCE_POINT_CAMERA
                                        GeofenceMode.POLYGON -> GEOFENCE_POLYGON_CAMERA
                                    },
                                    15.0,
                                ),
                            )

                            mapRef = mapLibreMap
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color(0xCCFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusText,
                        color = when (statusInside) {
                            true -> Color(0xFF1B7F2A)
                            false -> Color(0xFFC62828)
                            null -> Color.DarkGray
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("geofence_status"),
                    )
                    DemoPanelButton(
                        onClick = clearMarker,
                        modifier = Modifier
                            .height(36.dp)
                            .widthIn(min = 112.dp)
                            .testTag("geofence_clear_marker"),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(
                            text = stringResource(R.string.geofence_clear_marker),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        },
    )
}
