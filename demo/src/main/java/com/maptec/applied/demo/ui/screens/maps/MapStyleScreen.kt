package com.maptec.applied.demo.ui.screens.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

private data class DemoMapStyle(
    val id: String,
    val nameRes: Int,
)

@Composable
fun MapStyleScreen(modifier: Modifier = Modifier) {
    val supportedStyles = remember {
        listOf(
            DemoMapStyle("light", R.string.map_style_light),
            DemoMapStyle("dark", R.string.map_style_dark),
        )
    }
    var selectedStyleId by remember { mutableStateOf(supportedStyles.first().id) }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPosition = remember {
        CameraPosition.Builder().target(LatLng(1.360879, 103.732578)).zoom(16.0).build()
    }

    val switchStyle: (String) -> Unit = { styleId ->
        selectedStyleId = styleId
        isStyleRendered = false
        LoggerFactory.getLogger(LOG_MODULE).withTag("MapStyleScreen").d { "切换ID到: $styleId" }
        maptecMap?.setStyle(StyleOption(styleId), object : StyleStatusCallback {
            override fun onStyleLoaded(p0: Style?) {}
            override fun onStyleRendered(p0: Style?) {
                isStyleRendered = true
            }

            override fun onFailed(style: Style?, message: String) {
                showMapStyleLoadError(context, message)
            }
        })
    }

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            zoomButtonsEnabled(false)
            camera(cameraPosition)
        }
        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                maptecMap = map
                map.setStyle(StyleOption(selectedStyleId), object : StyleStatusCallback {
                    override fun onStyleLoaded(p0: Style?) {}
                    override fun onStyleRendered(p0: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: Style?, message: String) {
                        showMapStyleLoadError(context, message)
                    }
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = {},
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.map_style_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                supportedStyles.forEach { style ->
                    val selected = selectedStyleId == style.id
                    val content: @Composable () -> Unit = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(style.nameRes), fontSize = 16.sp)
                            Text(
                                text = style.id,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    if (selected) {
                        Button(
                            onClick = { switchStyle(style.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("style_${style.id}_button"),
                        ) {
                            content()
                        }
                    } else {
                        OutlinedButton(
                            onClick = { switchStyle(style.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("style_${style.id}_button"),
                        ) {
                            content()
                        }
                    }
                }
            }
        }

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
