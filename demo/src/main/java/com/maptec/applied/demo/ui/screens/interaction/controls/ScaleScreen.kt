package com.maptec.applied.demo.ui.screens.interaction.controls

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoDropdownMenuItem
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.widgets.ScaleView
import kotlin.math.roundToInt

@Composable
fun ScaleScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .build(),
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                ScaleControlPanel(mapView = mapView)
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                cameraPosition = cameraPosition,
                onMapReady = { view, map ->
                    mapView = view
                    map.uiSettings.isScaleBarEnabled = true
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleControlPanel(
    mapView: MapView?,
) {
    val context = LocalContext.current

    var scaleEnabled by remember { mutableStateOf(true) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.BOTTOM or Gravity.START) }
    var scaleBarMaxWidthPx by remember {
        mutableFloatStateOf(ScaleView.SCALE_BAR_MAX_WIDTH_DEFAULT_PX.toFloat())
    }

    val gravityOptions = listOf(
        context.getString(R.string.zoom_gravity_top_end) to (Gravity.TOP or Gravity.END),
        context.getString(R.string.zoom_gravity_top_start) to (Gravity.TOP or Gravity.START),
        context.getString(R.string.zoom_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
        context.getString(R.string.zoom_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
        context.getString(R.string.zoom_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.zoom_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.zoom_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
        context.getString(R.string.zoom_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.zoom_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END),
    )

    LaunchedEffect(Unit) {
        mapView?.getMapAsync { map ->
            val ui = map.uiSettings
            scaleEnabled = ui.isScaleBarEnabled
            val g = ui.scaleBarGravity
            if (g >= 0) selectedGravity = g
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.scale_show_bar), modifier = Modifier.weight(1f))
        DemoPanelSwitch(
            checked = scaleEnabled,
            onCheckedChange = { enabled ->
                scaleEnabled = enabled
                mapView?.getMapAsync { map ->
                    map.uiSettings.isScaleBarEnabled = enabled
                }
            },
            modifier = Modifier.testTag("switch_scale_enabled"),
        )
    }

    Text(
        text = stringResource(
            R.string.scale_bar_max_width_label,
            scaleBarMaxWidthPx.roundToInt(),
            ScaleView.SCALE_BAR_MAX_WIDTH_MIN_PX,
            ScaleView.SCALE_BAR_MAX_WIDTH_MAX_PX,
        ),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("text_scale_bar_max_width_label"),
    )
    DemoPanelSlider(
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
            .testTag("slider_scale_bar_max_width"),
    )

    var gravityExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = gravityExpanded,
        onExpandedChange = { gravityExpanded = !gravityExpanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("dropdown_scale_gravity"),
    ) {
        OutlinedTextField(
            value = gravityOptions.find { it.second == selectedGravity }?.first
                ?: context.getString(R.string.zoom_gravity_bottom_start),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.scale_bar_gravity_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_scale_gravity"),
        )
        ExposedDropdownMenu(
            expanded = gravityExpanded,
            onDismissRequest = { gravityExpanded = false },
            modifier = Modifier.testTag("menu_scale_gravity"),
        ) {
            gravityOptions.forEach { (name, gravity) ->
                DemoDropdownMenuItem(
                    text = name,
                    onClick = {
                        selectedGravity = gravity
                        mapView?.getMapAsync { maptecMap ->
                            maptecMap.uiSettings.scaleBarGravity = gravity
                        }
                        gravityExpanded = false
                    },
                    modifier = Modifier.testTag("menu_item_scale_$name"),
                )
            }
        }
    }
}
