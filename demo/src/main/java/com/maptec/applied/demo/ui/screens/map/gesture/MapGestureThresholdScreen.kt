package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapGestureThresholdScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).build(),
) {
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
                    override fun onStyleLoaded(style: Style?) {
                    }

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(
                        style: Style?, message: String
                    ) {
                    }
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
            GestureConfigControlPanel(
                mapView = mapView, isStyleRendered = isStyleRendered
            )
        }) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { })
    }
}

@Composable
private fun GestureConfigControlPanel(
    mapView: MapView, isStyleRendered: Boolean
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // Thresholds (Pixels)
    var rotateThresholdPx by remember { mutableStateOf(15f) }
    var shoveThresholdPx by remember { mutableStateOf(15f) }
    var moveThresholdPx by remember { mutableStateOf(0f) }

    // Resistances
    var rotateResistance by remember { mutableStateOf(1f) }
    var shoveResistance by remember { mutableStateOf(1f) }

    // Initialize state from map
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            rotateThresholdPx = gestures.rotateGestureThreshold
            shoveThresholdPx = gestures.tiltGestureThreshold
            moveThresholdPx = gestures.scrollGestureThreshold
            rotateResistance = gestures.rotateGestureResistance
            shoveResistance = gestures.tiltGestureResistance
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.gesture_threshold_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // --- 平移配置 ---
        ConfigSectionTitle(stringResource(R.string.gesture_threshold_pan_title))
        ConfigSlider(
            label = stringResource(R.string.gesture_threshold_trigger_label),
            testTag = "slider_move_threshold",
            value = moveThresholdPx,
            onValueChange = { value ->
                moveThresholdPx = value
                mapView.getMapAsync { it.uiSettings.gestures.scrollGestureThreshold = value }
            },
            valueRange = 0f..50f,
            displayValue = context.getString(R.string.gesture_threshold_px_format, moveThresholdPx)
        )

        // --- 旋转配置 ---
        ConfigSectionTitle(stringResource(R.string.gesture_threshold_rotate_title))
        ConfigSlider(
            label = stringResource(R.string.gesture_threshold_trigger_label),
            testTag = "slider_rotate_threshold",
            value = rotateThresholdPx,
            onValueChange = { value ->
                rotateThresholdPx = value
                mapView.getMapAsync { it.uiSettings.gestures.rotateGestureThreshold = value }
            },
            valueRange = 5f..45f,
            displayValue = context.getString(R.string.gesture_threshold_px_format, rotateThresholdPx)
        )
        ConfigSlider(
            label = stringResource(R.string.gesture_threshold_resistance_label),
            testTag = "slider_rotate_resistance",
            value = rotateResistance,
            onValueChange = { value ->
                rotateResistance = value
                mapView.getMapAsync { it.uiSettings.gestures.rotateGestureResistance = value }
            },
            valueRange = 0.1f..5f,
            displayValue = context.getString(R.string.gesture_threshold_value_format, rotateResistance)
        )

        // --- 倾斜配置 ---
        ConfigSectionTitle(stringResource(R.string.gesture_threshold_shove_title))
        ConfigSlider(
            label = stringResource(R.string.gesture_threshold_trigger_label),
            testTag = "slider_shove_threshold",
            value = shoveThresholdPx,
            onValueChange = { value ->
                shoveThresholdPx = value
                mapView.getMapAsync { it.uiSettings.gestures.tiltGestureThreshold = value }
            },
            valueRange = 5f..45f,
            displayValue = context.getString(R.string.gesture_threshold_px_format, shoveThresholdPx)
        )
        ConfigSlider(
            label = stringResource(R.string.gesture_threshold_resistance_label),
            testTag = "slider_shove_resistance",
            value = shoveResistance,
            onValueChange = { value ->
                shoveResistance = value
                mapView.getMapAsync { it.uiSettings.gestures.tiltGestureResistance = value }
            },
            valueRange = 0.1f..5f,
            displayValue = context.getString(R.string.gesture_threshold_value_format, shoveResistance)
        )

        Text(
            text = stringResource(R.string.gesture_threshold_hint),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }
    }
}

@Composable
private fun ConfigSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    displayValue: String,
    testTag: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,

    ) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: $displayValue", style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
