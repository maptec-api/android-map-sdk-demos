package com.maptec.applied.demo.ui.screens.map

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.demo.map.MapViewLifecycleEffect

/**
 * 日夜模式演示：布局与「地图视角控制」一致——全屏地图 + 底部可折叠控制面板（带标题栏与展开/收起箭头）。
 */
@Composable
fun DayNightScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .build(),
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isMapReady by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context)
            .apply {
                dayNightModeEnabled(true)
                zoomButtonsEnabled(false)
                camera(cameraPosition)
            }

        MapView(context, options).apply {
            tag = "mapView"
            onCreate(null)
            getMapAsync { _ ->
                isMapReady = true
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            update = { },
            modifier = Modifier
                .matchParentSize()
                .testTag("mapView")
        )

        if (isMapReady) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (isExpanded) Modifier else Modifier.height(60.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.LightGray)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp)
                    .testTag("day_night_title_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.day_night_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) stringResource(R.string.day_night_collapse) else stringResource(R.string.day_night_expand)
                )
            }

            if (isExpanded) {
                DayNightControlPanel(mapView = mapView)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayNightControlPanel(
    mapView: MapView?
) {
    var dayNightEnabled by remember { mutableStateOf(true) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.TOP or Gravity.END) }

    val context = LocalContext.current
    val gravityOptions = listOf(
        context.getString(R.string.day_night_gravity_top_end) to (Gravity.TOP or Gravity.END),
        context.getString(R.string.day_night_gravity_top_start) to (Gravity.TOP or Gravity.START),
        context.getString(R.string.day_night_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
        context.getString(R.string.day_night_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
        context.getString(R.string.day_night_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.day_night_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.day_night_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
        context.getString(R.string.day_night_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.day_night_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END)
    )

    LaunchedEffect(Unit) {
        mapView?.getMapAsync { map ->
            val ui = map.uiSettings
            dayNightEnabled = ui.isDayNightModeEnabled
            val g = ui.dayNightModeGravity
            if (g >= 0) selectedGravity = g
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                Text(stringResource(R.string.day_night_enable_control), modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = dayNightEnabled,
                    onCheckedChange = { enabled ->
                        dayNightEnabled = enabled
                        mapView?.getMapAsync { map ->
                            map.uiSettings.setDayNightModeEnabled(enabled)
                        }
                    },
                    modifier = Modifier.testTag("switch_day_night_enabled")
                )
            }
        }

        var gravityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = gravityExpanded,
            onExpandedChange = { gravityExpanded = !gravityExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("dropdown_day_night_gravity")
        ) {
            OutlinedTextField(
                value = gravityOptions.find { it.second == selectedGravity }?.first
                    ?: context.getString(R.string.day_night_gravity_top_end),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.day_night_gravity_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("textfield_day_night_gravity")
            )
            ExposedDropdownMenu(
                expanded = gravityExpanded,
                onDismissRequest = { gravityExpanded = false },
                modifier = Modifier.testTag("menu_day_night_gravity")
            ) {
                gravityOptions.forEach { (name, gravity) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedGravity = gravity
                            mapView?.getMapAsync { maptecMap ->
                                maptecMap.uiSettings.setDayNightModeGravity(gravity)
                            }
                            gravityExpanded = false
                        },
                        modifier = Modifier.testTag("menu_item_$name")
                    )
                }
            }
        }
    }
}
