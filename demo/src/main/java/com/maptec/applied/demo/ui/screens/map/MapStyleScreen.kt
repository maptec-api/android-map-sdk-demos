package com.maptec.applied.demo.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@Composable
fun MapStyleScreen(modifier: Modifier = Modifier) {
    var styleIdInput by remember { mutableStateOf("style_00001") }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPosition = remember {
        CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0).build()
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
                map.setStyle(StyleOption(styleIdInput), object : StyleStatusCallback {
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
            update = {}
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = styleIdInput,
                onValueChange = { styleIdInput = it },
                label = { Text(stringResource(R.string.map_style_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("style_id_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        maptecMap?.preLoadStyles(
                            listOf(StyleOption(styleIdInput))
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preload_button")
                ) {
                    Text(text = stringResource(R.string.map_style_preload), fontSize = 16.sp)
                }
                Button(
                    onClick = {
                        isStyleRendered = false
                        LoggerFactory.getLogger(LOG_MODULE).withTag("MapStyleScreen").d { "切换ID到: $styleIdInput" }
                        maptecMap?.setStyle(StyleOption(styleIdInput), object : StyleStatusCallback {
                            override fun onStyleLoaded(p0: Style?) {}
                            override fun onStyleRendered(p0: Style?) {
                                isStyleRendered = true
                            }

                            override fun onFailed(style: Style?, message: String) {
                                showMapStyleLoadError(context, message)
                            }
                        })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("switch_button")
                ) {
                    Text(text = stringResource(R.string.map_style_switch), fontSize = 16.sp)
                }
            }
        }

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}
