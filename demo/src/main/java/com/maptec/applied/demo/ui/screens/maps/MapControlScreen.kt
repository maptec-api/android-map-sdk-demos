package com.maptec.applied.demo.ui.screens.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

object MapCameraLimits {
    const val MIN_TILT = 0.0
    const val MAX_TILT_ABSOLUTE = 85.0
    const val MAX_TILT_DEFAULT = 60.0

    const val MIN_ZOOM = 0.0
    const val MAX_ZOOM = 20.0
}

@Composable
fun MapControlScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder()
        .target(LatLng(1.360879, 103.732578))
        .zoom(16.0)
        .build(),
) {
    var tilt by remember { mutableStateOf("40") }
    var bearing by remember { mutableStateOf("0") }
    var zoom by remember { mutableStateOf("14") }

    var minTilt by remember { mutableStateOf("0") }
    var maxTilt by remember { mutableStateOf("60") }
    var minZoom by remember { mutableStateOf("0") }
    var maxZoom by remember { mutableStateOf("20") }

    var isStyleRendered by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: com.maptec.applied.maps.Style?) {}
                    override fun onStyleRendered(style: com.maptec.applied.maps.Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: com.maptec.applied.maps.Style?, message: String) {}
                })

                map.addOnCameraMoveListener {
                    map.cameraPosition.let {
                        zoom = String.format("%.2f", it.zoom)
                        bearing = String.format("%.2f", it.bearing)
                        tilt = String.format("%.2f", it.tilt)
                    }
                }
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        LimitTextField(
                            value = tilt,
                            onValueChange = { tilt = it },
                            label = stringResource(R.string.map_camera_tilt),
                            testTag = "tilt",
                            maxValue = MapCameraLimits.MAX_TILT_ABSOLUTE,
                        )
                        LimitTextField(
                            value = bearing,
                            onValueChange = { bearing = it },
                            label = stringResource(R.string.map_camera_bearing),
                            testTag = "bearing",
                            allowNegative = true,
                        )
                        LimitTextField(
                            value = zoom,
                            onValueChange = { zoom = it },
                            label = stringResource(R.string.map_camera_zoom),
                            testTag = "zoom",
                            maxValue = MapCameraLimits.MAX_ZOOM,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        LimitTextField(
                            value = minTilt,
                            onValueChange = { minTilt = it },
                            label = stringResource(R.string.map_camera_min_tilt),
                            testTag = "minTilt",
                            maxValue = MapCameraLimits.MAX_TILT_DEFAULT,
                        )
                        LimitTextField(
                            value = maxTilt,
                            onValueChange = { maxTilt = it },
                            label = stringResource(R.string.map_camera_max_tilt),
                            testTag = "maxTilt",
                            maxValue = MapCameraLimits.MAX_TILT_ABSOLUTE,
                        )
                        LimitTextField(
                            value = minZoom,
                            onValueChange = { minZoom = it },
                            label = stringResource(R.string.map_camera_min_zoom),
                            testTag = "minZoom",
                            maxValue = MapCameraLimits.MAX_ZOOM,
                        )
                        LimitTextField(
                            value = maxZoom,
                            onValueChange = { maxZoom = it },
                            label = stringResource(R.string.map_camera_max_zoom),
                            testTag = "maxZoom",
                            maxValue = MapCameraLimits.MAX_ZOOM,
                        )
                    }
                }

                DemoPanelButton(
                    onClick = {
                        val tiltValue = if (tilt.isNotEmpty()) {
                            tilt.toDoubleOrNull()?.coerceIn(
                                MapCameraLimits.MIN_TILT, MapCameraLimits.MAX_TILT_ABSOLUTE,
                            )
                        } else null
                        val bearingValue = bearing.toDoubleOrNull()
                        val zoomValue = zoom.toDoubleOrNull()

                        mapView.getMapAsync { map ->
                            if (minTilt.isNotEmpty()) {
                                val v = (minTilt.toDoubleOrNull()
                                    ?: MapCameraLimits.MIN_TILT).coerceIn(
                                    MapCameraLimits.MIN_TILT,
                                    MapCameraLimits.MAX_TILT_DEFAULT,
                                )
                                map.setMinPitchPreference(v)
                            }
                            if (maxTilt.isNotEmpty()) {
                                val v = (maxTilt.toDoubleOrNull()
                                    ?: MapCameraLimits.MAX_TILT_DEFAULT).coerceIn(
                                    MapCameraLimits.MIN_TILT,
                                    MapCameraLimits.MAX_TILT_ABSOLUTE,
                                )
                                map.setMaxPitchPreference(v)
                            }
                            if (minZoom.isNotEmpty()) {
                                map.setMinZoomPreference(
                                    minZoom.toDoubleOrNull() ?: MapCameraLimits.MIN_ZOOM,
                                )
                            }
                            if (maxZoom.isNotEmpty()) {
                                map.setMaxZoomPreference(
                                    maxZoom.toDoubleOrNull() ?: MapCameraLimits.MAX_ZOOM,
                                )
                            }

                            val cur = map.cameraPosition
                            val builder = CameraPosition.Builder().target(cur.target)

                            if (tiltValue != null) builder.tilt(tiltValue)
                            if (bearingValue != null) builder.bearing(bearingValue)
                            if (zoomValue != null) builder.zoom(zoomValue)

                            map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()))

                            minTilt = String.format("%.2f", map.minPitch)
                            maxTilt = String.format("%.2f", map.maxPitch)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(50.dp)
                        .testTag("change_camera_button"),
                ) {
                    Text(
                        text = stringResource(id = R.string.change_camera),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("mapView"),
                )

                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

@Composable
fun LimitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    allowNegative: Boolean = false,
    maxValue: Double? = null,
) {
    TextField(
        value = value,
        onValueChange = { newValue ->
            val regex = if (allowNegative) Regex("^[-+]?\\d*\\.?\\d*$") else Regex("^\\d*\\.?\\d*$")
            if (newValue.isEmpty() || newValue.matches(regex)) {
                val num = newValue.toDoubleOrNull()
                if (num != null && maxValue != null && num > maxValue) {
                    onValueChange(maxValue.toString())
                } else {
                    onValueChange(newValue)
                }
            }
        },
        label = { Text(text = label) },
        singleLine = true,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag(testTag),
    )
}
