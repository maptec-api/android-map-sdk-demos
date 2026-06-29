package com.maptec.applied.demo.ui.screens.map

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.widgets.ScaleView
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .build(),
) {
    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context)
            .apply {
                scaleBarEnabled(true)
                camera(cameraPosition)
            }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID),
                    null
                )
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
            ScaleControlPanel(mapView = mapView)
        }
    ) { padding ->
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleControlPanel(
    mapView: MapView?
) {
    var scaleEnabled by remember { mutableStateOf(true) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.BOTTOM or Gravity.START) }
    var scaleBarMaxWidthPx by remember {
        mutableFloatStateOf(ScaleView.SCALE_BAR_MAX_WIDTH_DEFAULT_PX.toFloat())
    }

    val gravityOptions = listOf(
        "右上 (TOP|END)" to (Gravity.TOP or Gravity.END),
        "左上 (TOP|START)" to (Gravity.TOP or Gravity.START),
        "右下 (BOTTOM|END)" to (Gravity.BOTTOM or Gravity.END),
        "左下 (BOTTOM|START)" to (Gravity.BOTTOM or Gravity.START),
        "顶部居中 (TOP|CENTER)" to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        "底部居中 (BOTTOM|CENTER)" to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        "居中靠左 (Middle|START)" to (Gravity.CENTER_VERTICAL or Gravity.START),
        "居中 (CENTER)" to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        "居中靠右 (Middle|END)" to (Gravity.CENTER_VERTICAL or Gravity.END)
    )

    LaunchedEffect(Unit) {
        mapView?.getMapAsync { map ->
            val ui = map.uiSettings
            scaleEnabled = ui.isScaleBarEnabled
            val g = ui.scaleBarGravity
            if (g >= 0) selectedGravity = g
//            ui.scaleView?.let { sv ->
//                scaleBarMaxWidthPx = sv.scaleBarMaxWidthPx.toFloat()
//            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示比例尺", modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = scaleEnabled,
                    onCheckedChange = { enabled ->
                        scaleEnabled = enabled
                        mapView?.getMapAsync { map ->
                            map.uiSettings.isScaleBarEnabled = enabled
                        }
                    },
                    modifier = Modifier.testTag("switch_scale_enabled")
                )
            }
        }

        Text(
            text = "比例尺最大宽度 (px): ${scaleBarMaxWidthPx.roundToInt()} " +
                "[${ScaleView.SCALE_BAR_MAX_WIDTH_MIN_PX}, ${ScaleView.SCALE_BAR_MAX_WIDTH_MAX_PX}]",
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("text_scale_bar_max_width_label")
        )
        Slider(
            value = scaleBarMaxWidthPx,
            onValueChange = { scaleBarMaxWidthPx = it },
            onValueChangeFinished = {
                val v = scaleBarMaxWidthPx.roundToInt()
                mapView?.getMapAsync { map ->
                    map.uiSettings.scaleView?.let { sv ->
                        sv.setScaleBarMaxWidthPx(v)
                        sv.refreshScaleView(map)
                    }
                }
            },
            valueRange =
                ScaleView.SCALE_BAR_MAX_WIDTH_MIN_PX.toFloat()..
                    ScaleView.SCALE_BAR_MAX_WIDTH_MAX_PX.toFloat(),
            steps = ScaleView.SCALE_BAR_MAX_WIDTH_MAX_PX - ScaleView.SCALE_BAR_MAX_WIDTH_MIN_PX - 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("slider_scale_bar_max_width")
        )

        var gravityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = gravityExpanded,
            onExpandedChange = { gravityExpanded = !gravityExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("dropdown_scale_gravity")
        ) {
            OutlinedTextField(
                value = gravityOptions.find { it.second == selectedGravity }?.first
                    ?: "左下 (BOTTOM|START)",
                onValueChange = {},
                readOnly = true,
                label = { Text("比例尺位置 (setScaleBarGravity)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("textfield_scale_gravity")
            )
            ExposedDropdownMenu(
                expanded = gravityExpanded,
                onDismissRequest = { gravityExpanded = false },
                modifier = Modifier.testTag("menu_scale_gravity")
            ) {
                gravityOptions.forEach { (name, gravity) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedGravity = gravity
                            mapView?.getMapAsync { maptecMap ->
                                maptecMap.uiSettings.scaleBarGravity = gravity
                            }
                            gravityExpanded = false
                        },
                        modifier = Modifier.testTag("menu_item_scale_$name")
                    )
                }
            }
        }
    }
}
