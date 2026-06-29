package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRotateBearingRangeScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL)
        .bearing(20.0)
        .build(),
) {
    var isStyleRendered by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context)
            .apply {
                camera(cameraPosition)
            }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}
                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }
                    override fun onFailed(style: Style?, message: String) {}
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            RotateBearingRangePanel(
                mapView = mapView,
                isStyleRendered = isStyleRendered,
            )
        }
    ) { _ ->
        AndroidView(
            modifier = modifier.fillMaxSize().testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { }
        )
    }
}

@Composable
private fun RotateBearingRangePanel(
    mapView: MapView,
    isStyleRendered: Boolean,
) {
    var minBearing by remember { mutableStateOf(0f) }
    var maxBearing by remember { mutableStateOf(360f) }
    var currentBearing by remember { mutableStateOf(0.0) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map: MaptecMap ->
            val gestures = map.uiSettings.gestures
            minBearing = gestures.rotateGestureMinBearing.toFloat()
            maxBearing = gestures.rotateGestureMaxBearing.toFloat()
            currentBearing = map.cameraPosition.bearing
            map.addOnCameraMoveListener {
                currentBearing = map.cameraPosition.bearing
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.rotate_bearing_range_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.rotate_bearing_range_current, currentBearing),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BearingSliderRow(
            label = stringResource(R.string.rotate_bearing_range_min),
            value = minBearing,
            onValueChange = { v ->
                minBearing = v
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.setRotateGestureBearingRange(v.toDouble(), maxBearing.toDouble())
                }
            },
            testTag = "slider_min_bearing"
        )
        BearingSliderRow(
            label = stringResource(R.string.rotate_bearing_range_max),
            value = maxBearing,
            onValueChange = { v ->
                maxBearing = v
                mapView.getMapAsync { map ->
                    map.uiSettings.gestures.setRotateGestureBearingRange(minBearing.toDouble(), v.toDouble())
                }
            },
            testTag = "slider_max_bearing"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.rotate_bearing_range_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}

@Composable
private fun BearingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    testTag: String,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.rotate_bearing_range_slider_label, label, value),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth().testTag(testTag)
        )
    }
}
