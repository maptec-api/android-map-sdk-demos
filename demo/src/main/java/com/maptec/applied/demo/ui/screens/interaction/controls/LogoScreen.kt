package com.maptec.applied.demo.ui.screens.interaction.controls

import android.view.Gravity
import androidx.compose.foundation.layout.Box
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
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun LogoScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
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

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                LogoControlPanel(mapView = mapView)
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
private fun LogoControlPanel(
    mapView: MapView?,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var selectedGravity by remember { mutableIntStateOf(Gravity.BOTTOM or Gravity.START) }
    var marginLeftDp by remember { mutableStateOf(10f) }
    var marginTopDp by remember { mutableStateOf(10f) }
    var marginRightDp by remember { mutableStateOf(10f) }
    var marginBottomDp by remember { mutableStateOf(10f) }

    val gravityOptions = listOf(
        context.getString(R.string.logo_gravity_top_end) to (Gravity.TOP or Gravity.END),
        context.getString(R.string.logo_gravity_top_start) to (Gravity.TOP or Gravity.START),
        context.getString(R.string.logo_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
        context.getString(R.string.logo_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
        context.getString(R.string.logo_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.logo_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.logo_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
        context.getString(R.string.logo_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.logo_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END),
    )

    LaunchedEffect(Unit) {
        mapView?.getMapAsync { map ->
            val ui = map.uiSettings
            val g = ui.logoGravity
            if (g >= 0) selectedGravity = g
            marginLeftDp = with(density) { ui.logoMarginLeft.toDp().value }
            marginTopDp = with(density) { ui.logoMarginTop.toDp().value }
            marginRightDp = with(density) { ui.logoMarginRight.toDp().value }
            marginBottomDp = with(density) { ui.logoMarginBottom.toDp().value }
        }
    }

    var gravityExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = gravityExpanded,
        onExpandedChange = { gravityExpanded = !gravityExpanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("dropdown_logo_gravity"),
    ) {
        OutlinedTextField(
            value = gravityOptions.find { it.second == selectedGravity }?.first
                ?: context.getString(R.string.logo_gravity_top_end),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.logo_gravity_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_logo_gravity"),
        )
        ExposedDropdownMenu(
            expanded = gravityExpanded,
            onDismissRequest = { gravityExpanded = false },
            modifier = Modifier.testTag("menu_logo_gravity"),
        ) {
            gravityOptions.forEach { (name, gravity) ->
                DemoDropdownMenuItem(
                    text = name,
                    onClick = {
                        selectedGravity = gravity
                        mapView?.getMapAsync { maptecMap ->
                            maptecMap.uiSettings.logoGravity = gravity
                        }
                        gravityExpanded = false
                    },
                    modifier = Modifier.testTag("menu_item_$name"),
                )
            }
        }
    }
}
