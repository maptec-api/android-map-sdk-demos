package com.maptec.applied.demo.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun Mapview(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(12.0)
        .build(),
    onMapReady: (MapView, MaptecMap) -> Unit = { _, _ -> },
    onStyleRendered: (MapView, MaptecMap, Style) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    var isStyleRendered by remember(cameraPosition) { mutableStateOf(false) }

    val mapView = remember(cameraPosition) {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
        }
        MapView(context, options).apply {
            onCreate(null)
            tag = "mapView"
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                        if (style != null) {
                            onStyleRendered(this@apply, map, style)
                        }
                    }

                    override fun onFailed(style: Style?, message: String) {}
                })
                onMapReady(this, map)
            }
        }
    }

    MapViewLifecycleEffect(mapView)

    AndroidView(
        factory = { mapView },
        modifier = modifier.testTag("mapView"),
    )

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
