package com.maptec.applied.demo.ui.screens.map

import android.annotation.SuppressLint
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap

/**
 * CompassScreen - 指南针（CompassView）配置演示
 *
 * 用于 CompassView 等功能的开发用例，通过 map.uiSettings 控制：
 * - setCompassEnabled / isCompassEnabled
 * - setCompassGravity / getCompassGravity（位置：左上、右上、左下、右下等）
 * - setCompassMargins（left, top, right, bottom）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
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
            CompassControlPanel(
                mapView = mapView, maptecMap = maptecMap
            )
        }) { padding ->
        Mapview(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), onMapReady = { view, map ->
                mapView = view
                maptecMap = map
                maptecMap?.uiSettings?.setCompassFadeFacingNorth(false)
            })
    }
}

@SuppressLint("RtlHardcoded")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompassControlPanel(
    mapView: MapView?, maptecMap: MaptecMap?
) {
    val density = LocalDensity.current

    var compassEnabled by remember { mutableStateOf(true) }
    var fadeWhenFacingNorth by remember { mutableStateOf(false) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.TOP or Gravity.END) }
    var marginLeftDp by remember { mutableStateOf(10f) }
    var marginTopDp by remember { mutableStateOf(10f) }
    var marginRightDp by remember { mutableStateOf(10f) }
    var marginBottomDp by remember { mutableStateOf(10f) }
    var compassSizeDp by remember { mutableStateOf(48f) }

    val context = LocalContext.current
    val gravityOptions = listOf(
        context.getString(R.string.compass_gravity_top_end) to (Gravity.TOP or Gravity.END),
        context.getString(R.string.compass_gravity_top_start) to (Gravity.TOP or Gravity.START),
        context.getString(R.string.compass_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.compass_gravity_middle_left) to (Gravity.CENTER_VERTICAL or Gravity.LEFT),
        context.getString(R.string.compass_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.compass_gravity_middle_right) to (Gravity.CENTER_VERTICAL or Gravity.RIGHT),
        context.getString(R.string.compass_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
        context.getString(R.string.compass_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
        context.getString(R.string.compass_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    )

    maptecMap?.let { map ->
        val ui = map.uiSettings
        LaunchedEffect(Unit) {
            compassEnabled = ui.isCompassEnabled
            fadeWhenFacingNorth = ui.isCompassFadeWhenFacingNorth
            val g = ui.getCompassGravity()
            if (g >= 0) selectedGravity = g
            marginLeftDp = with(density) { ui.getCompassMarginLeft().toDp().value }
            marginTopDp = with(density) { ui.getCompassMarginTop().toDp().value }
            marginRightDp = with(density) { ui.getCompassMarginRight().toDp().value }
            marginBottomDp = with(density) { ui.getCompassMarginBottom().toDp().value }
            val widthPx = ui.getCompassViewWidth()
            if (widthPx > 0) {
                compassSizeDp = with(density) { widthPx.toDp().value }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 显示指南针 + 顺时针旋转 30° 同一行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.compass_show_compass),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = compassEnabled, onCheckedChange = { enabled ->
                        compassEnabled = enabled
                        maptecMap?.uiSettings?.setCompassEnabled(enabled)
                    }, enabled = maptecMap != null, modifier = Modifier.testTag("compass_switch")
                )
            }
            // 正北自动隐藏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.compass_fade_when_facing_north),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = fadeWhenFacingNorth,
                    onCheckedChange = {
                        fadeWhenFacingNorth = it
                        maptecMap?.uiSettings?.setCompassFadeFacingNorth(it)
                    },
                    enabled = maptecMap != null,
                    modifier = Modifier.testTag("compass_fade_switch")
                )
            }
        }

        // 指南针位置 (Gravity)
        var gravityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = gravityExpanded,
            onExpandedChange = { gravityExpanded = !gravityExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("compass_gravity_dropdown")
        ) {
            OutlinedTextField(
                value = gravityOptions.find { it.second == selectedGravity }?.first
                    ?: context.getString(R.string.compass_gravity_top_end),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.compass_gravity_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = maptecMap != null
            )
            ExposedDropdownMenu(
                expanded = gravityExpanded, onDismissRequest = { gravityExpanded = false }) {
                gravityOptions.forEach { (name, gravity) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        selectedGravity = gravity
                        maptecMap?.uiSettings?.setCompassGravity(gravity)
                        gravityExpanded = false
                    })
                }
            }
        }

        // 罗盘尺寸 setCompassViewSize (dp)
        Text(
            text = stringResource(R.string.compass_size_label, compassSizeDp.toInt()),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(
            value = compassSizeDp,
            onValueChange = { compassSizeDp = it },
            onValueChangeFinished = {
                maptecMap?.uiSettings?.setCompassViewSize(
                    with(density) { compassSizeDp.dp.roundToPx() },
                    with(density) { compassSizeDp.dp.roundToPx() },
                )
            },
            valueRange = 24f..120f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("compass_size_slider"),
            enabled = maptecMap != null
        )

        Button(
            onClick = {
                maptecMap?.let { map ->
                    val pos = map.cameraPosition
                    val newBearing = ((pos.bearing - 36) % 360 + 360) % 360
                    val update = CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder(pos).bearing(newBearing).build()
                    )
                    map.animateCamera(update, 300)
                }
            }, enabled = maptecMap != null, modifier = Modifier.testTag("compass_rotate_button")
        ) {
            Text(stringResource(R.string.compass_rotate_clockwise))
        }


        // 边距 setCompassMargins (px)
//        Text("左边距 (dp): ${marginLeftDp.toInt()}", modifier = Modifier.padding(bottom = 4.dp))
//        Slider(
//            value = marginLeftDp,
//            onValueChange = { marginLeftDp = it },
//            onValueChangeFinished = {
//                maptecMap?.uiSettings?.setCompassMargins(
//                    with(density) { marginLeftDp.dp.roundToPx() },
//                    with(density) { marginTopDp.dp.roundToPx() },
//                    with(density) { marginRightDp.dp.roundToPx() },
//                    with(density) { marginBottomDp.dp.roundToPx() }
//                )
//            },
//            valueRange = 0f..80f,
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
//            enabled = maptecMap != null
//        )
//        Text("上边距 (dp): ${marginTopDp.toInt()}", modifier = Modifier.padding(bottom = 4.dp))
//        Slider(
//            value = marginTopDp,
//            onValueChange = { marginTopDp = it },
//            onValueChangeFinished = {
//                maptecMap?.uiSettings?.setCompassMargins(
//                    with(density) { marginLeftDp.dp.roundToPx() },
//                    with(density) { marginTopDp.dp.roundToPx() },
//                    with(density) { marginRightDp.dp.roundToPx() },
//                    with(density) { marginBottomDp.dp.roundToPx() }
//                )
//            },
//            valueRange = 0f..80f,
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
//            enabled = maptecMap != null
//        )
//        Text("右边距 (dp): ${marginRightDp.toInt()}", modifier = Modifier.padding(bottom = 4.dp))
//        Slider(
//            value = marginRightDp,
//            onValueChange = { marginRightDp = it },
//            onValueChangeFinished = {
//                maptecMap?.uiSettings?.setCompassMargins(
//                    with(density) { marginLeftDp.dp.roundToPx() },
//                    with(density) { marginTopDp.dp.roundToPx() },
//                    with(density) { marginRightDp.dp.roundToPx() },
//                    with(density) { marginBottomDp.dp.roundToPx() }
//                )
//            },
//            valueRange = 0f..80f,
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
//            enabled = maptecMap != null
//        )
//        Text("下边距 (dp): ${marginBottomDp.toInt()}", modifier = Modifier.padding(bottom = 4.dp))
//        Slider(
//            value = marginBottomDp,
//            onValueChange = { marginBottomDp = it },
//            onValueChangeFinished = {
//                maptecMap?.uiSettings?.setCompassMargins(
//                    with(density) { marginLeftDp.dp.roundToPx() },
//                    with(density) { marginTopDp.dp.roundToPx() },
//                    with(density) { marginRightDp.dp.roundToPx() },
//                    with(density) { marginBottomDp.dp.roundToPx() }
//                )
//            },
//            valueRange = 0f..80f,
//            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
//            enabled = maptecMap != null
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
    }
}
