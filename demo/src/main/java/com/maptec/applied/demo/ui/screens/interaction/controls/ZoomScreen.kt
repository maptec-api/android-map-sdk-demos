package com.maptec.applied.demo.ui.screens.interaction.controls

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoDropdownMenuItem
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.maps.ZoomButtonsView

@Composable
fun ZoomScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .build(),
) {
    val context = LocalContext.current
    var isStyleRendered by remember { mutableStateOf(false) }

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

                    override fun onFailed(style: Style?, message: String) {
                    }
                })
                map.uiSettings.setZoomButtonsEnabled(true)
                map.uiSettings.setZoomLevelVisible(true)
                map.uiSettings.zoomButtonsGravity = Gravity.BOTTOM or Gravity.START
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                ZoomControlPanel(mapView = mapView)
            }
        },
        content = {
            Box(modifier = modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mapView"),
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                )
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoomControlPanel(
    mapView: MapView?,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var zoomButtonsEnabled by remember { mutableStateOf(true) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.BOTTOM or Gravity.START) }
    var marginLeftDp by remember { mutableStateOf(10f) }
    var marginTopDp by remember { mutableStateOf(10f) }
    var marginRightDp by remember { mutableStateOf(10f) }
    var marginBottomDp by remember { mutableStateOf(10f) }
    var buttonSizeDp by remember { mutableStateOf(32f) }
    var zoomPrecision by remember { mutableIntStateOf(1) }
    var zoomLevelPosition by remember { mutableIntStateOf(ZoomButtonsView.ZOOM_LEVEL_POSITION_MIDDLE) }
    var zoomLevelVisible by remember { mutableStateOf(true) }

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
            zoomButtonsEnabled = ui.isZoomButtonsEnabled
            val g = ui.zoomButtonsGravity
            if (g >= 0) selectedGravity = g
            marginLeftDp = with(density) { ui.zoomButtonsMarginLeft.toDp().value }
            marginTopDp = with(density) { ui.zoomButtonsMarginTop.toDp().value }
            marginRightDp = with(density) { ui.zoomButtonsMarginRight.toDp().value }
            marginBottomDp = with(density) { ui.zoomButtonsMarginBottom.toDp().value }
            val sz = ui.zoomButtonsSize
            if (sz > 0) buttonSizeDp = with(density) { sz.toDp().value }
            zoomPrecision = parsePrecision(ui.zoomLevelFormat)
            zoomLevelPosition = ui.zoomLevelPosition
            zoomLevelVisible = ui.isZoomLevelVisible
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.zoom_show_buttons), modifier = Modifier.weight(1f))
        DemoPanelSwitch(
            checked = zoomButtonsEnabled,
            onCheckedChange = { enabled ->
                zoomButtonsEnabled = enabled
                mapView?.getMapAsync { map ->
                    map.uiSettings.setZoomButtonsEnabled(enabled)
                }
            },
            modifier = Modifier.testTag("switch_zoom_enabled"),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.zoom_show_level), modifier = Modifier.weight(1f))
        DemoPanelSwitch(
            checked = zoomLevelVisible,
            onCheckedChange = { visible ->
                zoomLevelVisible = visible
                mapView?.getMapAsync { map ->
                    map.uiSettings.setZoomLevelVisible(visible)
                }
            },
            modifier = Modifier.testTag("switch_zoom_level_visible"),
        )
    }

    var gravityExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = gravityExpanded,
        onExpandedChange = { gravityExpanded = !gravityExpanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("dropdown_zoom_gravity"),
    ) {
        OutlinedTextField(
            value = gravityOptions.find { it.second == selectedGravity }?.first
                ?: context.getString(R.string.zoom_gravity_bottom_start),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.zoom_gravity_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_zoom_gravity"),
        )
        ExposedDropdownMenu(
            expanded = gravityExpanded,
            onDismissRequest = { gravityExpanded = false },
            modifier = Modifier.testTag("menu_zoom_gravity"),
        ) {
            gravityOptions.forEach { (name, gravity) ->
                DemoDropdownMenuItem(
                    text = name,
                    modifier = Modifier.testTag("zoom_menu_item_$name"),
                    onClick = {
                        selectedGravity = gravity
                        mapView?.getMapAsync { maptecMap ->
                            maptecMap.uiSettings.zoomButtonsGravity = gravity
                        }
                        gravityExpanded = false
                    },
                )
            }
        }
    }

    Text(
        text = "${stringResource(R.string.zoom_button_size_label)}: ${buttonSizeDp.toInt()}dp",
        modifier = Modifier.padding(bottom = 4.dp),
    )
    DemoPanelSlider(
        value = buttonSizeDp,
        onValueChange = { newValue ->
            buttonSizeDp = newValue
            val px = with(density) { newValue.dp.toPx().toInt() }
            mapView?.getMapAsync { map ->
                map.uiSettings.setZoomButtonsSize(px)
            }
        },
        valueRange = 24f..60f,
        steps = (60 - 24) - 1,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_zoom_button_size"),
    )

    var precisionExpanded by remember { mutableStateOf(false) }
    val precisionOptions = listOf(0, 1, 2, 3)
    ExposedDropdownMenuBox(
        expanded = precisionExpanded && zoomLevelVisible,
        onExpandedChange = {
            if (zoomLevelVisible) precisionExpanded = !precisionExpanded
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("dropdown_zoom_precision"),
    ) {
        OutlinedTextField(
            value = "$zoomPrecision " + stringResource(R.string.zoom_precision_decimal_unit) +
                "  (\"${formatForPrecision(zoomPrecision)}\")",
            onValueChange = {},
            readOnly = true,
            enabled = zoomLevelVisible,
            label = { Text(stringResource(R.string.zoom_precision_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = precisionExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_zoom_precision"),
        )
        ExposedDropdownMenu(
            expanded = precisionExpanded,
            onDismissRequest = { precisionExpanded = false },
            modifier = Modifier.testTag("menu_zoom_precision"),
        ) {
            precisionOptions.forEach { p ->
                DemoDropdownMenuItem(
                    text = "$p " + stringResource(R.string.zoom_precision_decimal_unit) +
                        "  (\"${formatForPrecision(p)}\")",
                    modifier = Modifier.testTag("zoom_precision_item_$p"),
                    onClick = {
                        zoomPrecision = p
                        mapView?.getMapAsync { map ->
                            map.uiSettings.setZoomLevelFormat(formatForPrecision(p))
                        }
                        precisionExpanded = false
                    },
                )
            }
        }
    }

    var positionExpanded by remember { mutableStateOf(false) }
    val positionOptions = listOf(
        stringResource(R.string.zoom_level_position_top) to ZoomButtonsView.ZOOM_LEVEL_POSITION_TOP,
        stringResource(R.string.zoom_level_position_middle) to ZoomButtonsView.ZOOM_LEVEL_POSITION_MIDDLE,
        stringResource(R.string.zoom_level_position_bottom) to ZoomButtonsView.ZOOM_LEVEL_POSITION_BOTTOM,
    )
    ExposedDropdownMenuBox(
        expanded = positionExpanded && zoomLevelVisible,
        onExpandedChange = {
            if (zoomLevelVisible) positionExpanded = !positionExpanded
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("dropdown_zoom_level_position"),
    ) {
        OutlinedTextField(
            value = positionOptions.find { it.second == zoomLevelPosition }?.first
                ?: stringResource(R.string.zoom_level_position_middle),
            onValueChange = {},
            readOnly = true,
            enabled = zoomLevelVisible,
            label = { Text(stringResource(R.string.zoom_level_position_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_zoom_level_position"),
        )
        ExposedDropdownMenu(
            expanded = positionExpanded,
            onDismissRequest = { positionExpanded = false },
            modifier = Modifier.testTag("menu_zoom_level_position"),
        ) {
            positionOptions.forEach { (name, pos) ->
                DemoDropdownMenuItem(
                    text = name,
                    modifier = Modifier.testTag("zoom_level_position_item_$name"),
                    onClick = {
                        zoomLevelPosition = pos
                        mapView?.getMapAsync { map ->
                            map.uiSettings.setZoomLevelPosition(pos)
                        }
                        positionExpanded = false
                    },
                )
            }
        }
    }
}

private fun formatForPrecision(precision: Int): String =
    "%.${precision}f"

private fun parsePrecision(format: String): Int {
    val match = Regex("""%\.(\d+)f""").find(format)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
}
