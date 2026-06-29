package com.maptec.applied.demo.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.viewmodel.LocationScreenUiState
import com.maptec.applied.demo.viewmodel.LocationScreenViewModel
import com.maptec.applied.demo.viewmodel.LocationScreenViewModelFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.location.LocationComponent
import com.maptec.applied.location.LocationComponentActivationOptions
import com.maptec.applied.location.LocationComponentOptions
import com.maptec.applied.location.OnCameraTrackingChangedListener
import com.maptec.applied.location.OnLocationClickListener
import com.maptec.applied.location.OnLocationStaleListener
import com.maptec.applied.location.OnRenderModeChangedListener
import com.maptec.applied.location.modes.CameraMode
import com.maptec.applied.location.modes.RenderMode
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style

/**
 * LocationScreen - 定位组件配置演示
 *
 * 展示 LocationComponent 的各种功能：
 * 1. 定位图标自定义（前景、背景、方向箭头）
 * 2. 相机跟踪模式（CameraMode）
 * 3. 渲染模式（RenderMode）
 * 4. 精度圈样式
 * 5. 脉冲动画
 * 6. 过期状态
 * 7. 手动位置更新
 * 8. 事件监听
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen() {
    val context = LocalContext.current

    val viewModel: LocationScreenViewModel = viewModel(
        factory = LocationScreenViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var locationComponent by remember { mutableStateOf<LocationComponent?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()

    // 检查定位权限
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true)
                || (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        viewModel.setPermissionGranted(granted)
    }

    // 检查权限状态
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val granted = fineLocation || coarseLocation
        viewModel.setPermissionGranted(granted)
        if (!granted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle(modifier = Modifier.testTag("bottom_sheet_drag_handle")) },
        sheetContent = {
            Column(
                modifier = Modifier.testTag("location_bottom_sheet")
            ) {
                LocationControlPanel(
                    locationComponent = locationComponent,
                    mapView = mapView,
                    maptecMap = maptecMap,
                    uiState = uiState,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Mapview(
                onMapReady = { view, map ->
                mapView = view
                maptecMap = map

                map.uiSettings.setLocationViewEnabled(true)
                map.uiSettings.getLocationView()?.setOnClickListener {
                    locationComponent?.lastKnownLocation?.let { location ->
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                16.0
                            ),
                            1000
                        )
                    } ?: run {
                        Toast.makeText(context, context.getString(R.string.location_no_location), Toast.LENGTH_SHORT).show()
                    }
                }

                map.getStyle { style ->
                    if (uiState.permissionGranted) {
                        initLocationComponent(
                            context = context,
                            map = map,
                            style = style,
                            onLocationComponentReady = { lc ->
                                locationComponent = lc
                                viewModel.setLocationComponent(lc)
                            }
                        )
                    }
                }
            }
        )
        }
    }

    // 当权限状态变化且地图已准备好时，初始化定位组件
    LaunchedEffect(uiState.permissionGranted, maptecMap) {
        if (uiState.permissionGranted && maptecMap != null && locationComponent == null) {
            maptecMap?.getStyle { style ->
                initLocationComponent(
                    context = context,
                    map = maptecMap!!,
                    style = style,
                    onLocationComponentReady = { lc ->
                        locationComponent = lc
                        viewModel.setLocationComponent(lc)
                    }
                )
            }
        }
    }

}

@SuppressLint("MissingPermission")
private fun initLocationComponent(
    context: android.content.Context,
    map: MaptecMap,
    style: Style,
    onLocationComponentReady: (LocationComponent) -> Unit
) {
    val locationComponent = map.locationComponent

    // 自定义定位图标样式
    val options = LocationComponentOptions.builder(context)
        .foregroundDrawable(R.drawable.ic_map_foreground_location)
        .backgroundDrawable(R.drawable.ic_background_location)
        .bearingDrawable(R.drawable.ic_map_compass)
        .gpsDrawable(R.drawable.ic_gps_location)
        .foregroundDrawableStale(R.drawable.ic_map_foreground_stale_location)
        .elevation(0f)
        .accuracyColor(Color.parseColor("#3A5EFB")) // 半透明蓝色
        .accuracyAlpha(0.3f)
        .pulseEnabled(true)
        .pulseFadeEnabled(true)
        .pulseColor(Color.parseColor("#2196F3"))
        .pulseMaxRadius(50f)
        .pulseSingleDuration(2000f)
        .pulseAlpha(0.5f)
        .enableStaleState(true)
        .staleStateTimeout(30000) // 30秒无更新视为过期
        .maxZoomIconScale(1.2f)
        .minZoomIconScale(0.6f)
        .trackingGesturesManagement(true)
        .build()

    // 激活定位组件
    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(options)
            .build()
    )
    // 启用定位组件
    locationComponent.setLocationComponentVisibility(true)
    locationComponent.cameraMode = CameraMode.TRACKING
    locationComponent.renderMode = RenderMode.COMPASS

//    locationComponent.applyStyle(options)
    // 添加事件监听
    locationComponent.addOnLocationClickListener(object : OnLocationClickListener {
        override fun onLocationComponentClick() {
//            Toast.makeText(context, "点击了定位图标", Toast.LENGTH_SHORT).show()
        }
    })

    locationComponent.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingDismissed() {
//            Toast.makeText(context, "相机跟踪已取消", Toast.LENGTH_SHORT).show()
        }

        override fun onCameraTrackingChanged(currentMode: Int) {
//            Toast.makeText(context, "相机模式: $currentMode", Toast.LENGTH_SHORT).show()
        }
    })

    locationComponent.addOnLocationStaleListener(object : OnLocationStaleListener {
        override fun onStaleStateChange(isStale: Boolean) {
//            Toast.makeText(context, if (isStale) "位置已过期" else "位置已更新", Toast.LENGTH_SHORT).show()
        }
    })

    locationComponent.addOnRenderModeChangedListener(object : OnRenderModeChangedListener {
        override fun onRenderModeChanged(currentMode: Int) {
//            Toast.makeText(context, "渲染模式: $currentMode", Toast.LENGTH_SHORT).show()
        }
    })

    onLocationComponentReady(locationComponent)
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationControlPanel(
    locationComponent: LocationComponent?,
    mapView: MapView?,
    maptecMap: MaptecMap?,
    uiState: LocationScreenUiState,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    // 状态
    var isEnabled by remember { mutableStateOf(true) }
    var selectedCameraMode by remember { mutableStateOf(CameraMode.TRACKING) }
    var selectedRenderMode by remember { mutableStateOf(RenderMode.COMPASS) }
    var pulseEnabled by remember { mutableStateOf(true) }
    var accuracyAlpha by remember { mutableFloatStateOf(0.3f) }
    var useCustomEngine by remember { mutableStateOf(false) }

    // 相机模式选项
    val cameraModeOptions = listOf(
        "NONE" to CameraMode.NONE,
        "NONE_COMPASS" to CameraMode.NONE_COMPASS,
        "NONE_GPS" to CameraMode.NONE_GPS,
        "TRACKING" to CameraMode.TRACKING,
        "TRACKING_COMPASS" to CameraMode.TRACKING_COMPASS,
        "TRACKING_GPS" to CameraMode.TRACKING_GPS,
        "TRACKING_GPS_NORTH" to CameraMode.TRACKING_GPS_NORTH
    )

    // 渲染模式选项
    val renderModeOptions = listOf(
        "NORMAL (圆点)" to RenderMode.NORMAL,
        "COMPASS (指南针)" to RenderMode.COMPASS,
        "GPS (导航箭头)" to RenderMode.GPS
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 定位组件开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(context.getString(R.string.location_enable_component))
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    isEnabled = enabled
                    locationComponent?.isLocationComponentEnabled = enabled
                },
                enabled = locationComponent != null,
                modifier = Modifier.testTag("switch_location_component_enabled")
            )
        }

        LocationViewControlSection(mapView = mapView)

        Spacer(modifier = Modifier.height(8.dp))

        // 精度圈透明度
        Text(context.getString(R.string.location_accuracy_format, String.format("%.2f", accuracyAlpha)))
        Slider(
            value = accuracyAlpha,
            onValueChange = { accuracyAlpha = it },
            onValueChangeFinished = {
                locationComponent?.let { lc ->
                    val newOptions = lc.locationComponentOptions.toBuilder()
                    .accuracyAlpha(accuracyAlpha).build()
                    lc.applyStyle(newOptions)
                }
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("slider_accuracy_alpha"),
            enabled = locationComponent != null
        )
        // 脉冲动画开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(context.getString(R.string.location_pulse_animation))
            Switch(
                checked = pulseEnabled,
                onCheckedChange = { enabled ->
                    pulseEnabled = enabled
                    // 需要重新应用样式来更新脉冲动画
                    locationComponent?.let { lc ->
                        val newOptions = lc.locationComponentOptions.toBuilder()
                            .pulseEnabled(enabled)
                            .pulseFadeEnabled(enabled)
                            .build()
                        lc.applyStyle(newOptions)
                    }
                },
                enabled = locationComponent != null,
                modifier = Modifier.testTag("switch_pulse_animation")
            )
        }


        // 相机模式下拉框
        var cameraModeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = cameraModeExpanded,
            onExpandedChange = { cameraModeExpanded = !cameraModeExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("dropdown_camera_mode")
        ) {
            OutlinedTextField(
                value = cameraModeOptions.find { it.second == selectedCameraMode }?.first ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(context.getString(R.string.location_camera_mode)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cameraModeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = locationComponent != null
            )
            ExposedDropdownMenu(
                expanded = cameraModeExpanded,
                onDismissRequest = { cameraModeExpanded = false }
            ) {
                cameraModeOptions.forEach { (name, mode) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedCameraMode = mode
                            locationComponent?.cameraMode = mode
                            cameraModeExpanded = false
                        }
                    )
                }
            }
        }

        // 渲染模式下拉框
        var renderModeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = renderModeExpanded,
            onExpandedChange = { renderModeExpanded = !renderModeExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("dropdown_render_mode")
        ) {
            OutlinedTextField(
                value = renderModeOptions.find { it.second == selectedRenderMode }?.first ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(context.getString(R.string.location_render_mode)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = renderModeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = locationComponent != null
            )
            ExposedDropdownMenu(
                expanded = renderModeExpanded,
                onDismissRequest = { renderModeExpanded = false }
            ) {
                renderModeOptions.forEach { (name, mode) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedRenderMode = mode
                            locationComponent?.renderMode = mode
                            renderModeExpanded = false
                        }
                    )
                }
            }
        }


        // 移动到当前位置按钮
        Button(
            onClick = {
                locationComponent?.lastKnownLocation?.let { location ->
                    maptecMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            16.0
                        ),
                        1000
                    )
                } ?: run {
                    Toast.makeText(context, context.getString(R.string.location_no_location), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("button_move_to_current"),
            enabled = locationComponent != null
        ) {
            Text(context.getString(R.string.location_move_to_current))
        }


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationViewControlSection(
    mapView: MapView?
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var locationViewEnabled by remember { mutableStateOf(true) }
    var selectedGravity by remember { mutableIntStateOf(Gravity.BOTTOM or Gravity.END) }
    var marginLeftDp by remember { mutableStateOf(10f) }
    var marginTopDp by remember { mutableStateOf(10f) }
    var marginRightDp by remember { mutableStateOf(10f) }
    var marginBottomDp by remember { mutableStateOf(10f) }
    var locationViewSizeDp by remember { mutableStateOf(40f) }

    val gravityOptions = listOf(
        context.getString(R.string.location_gravity_top_end) to (Gravity.TOP or Gravity.END),
        context.getString(R.string.location_gravity_top_start) to (Gravity.TOP or Gravity.START),
        context.getString(R.string.location_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
        context.getString(R.string.location_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
        context.getString(R.string.location_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.location_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.location_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
        context.getString(R.string.location_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
        context.getString(R.string.location_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END)
    )

    LaunchedEffect(Unit) {
        mapView?.getMapAsync { map ->
            val ui = map.uiSettings
            locationViewEnabled = ui.isLocationViewEnabled
            val g = ui.locationViewGravity
            if (g >= 0) selectedGravity = g
            marginLeftDp = with(density) { ui.locationViewMarginLeft.toDp().value }
            marginTopDp = with(density) { ui.locationViewMarginTop.toDp().value }
            marginRightDp = with(density) { ui.locationViewMarginRight.toDp().value }
            marginBottomDp = with(density) { ui.locationViewMarginBottom.toDp().value }
            val widthPx = ui.locationViewWidth
            if (widthPx > 0) {
                locationViewSizeDp = with(density) { widthPx.toDp().value }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(context.getString(R.string.location_show_button))
        Switch(
            checked = locationViewEnabled,
            onCheckedChange = { enabled ->
                locationViewEnabled = enabled
                mapView?.getMapAsync { map ->
                    map.uiSettings.setLocationViewEnabled(enabled)
                }
            },
            modifier = Modifier.testTag("switch_location_view_enabled")
        )
    }

    var gravityExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = gravityExpanded,
        onExpandedChange = { gravityExpanded = !gravityExpanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("dropdown_location_view_gravity")
    ) {
        OutlinedTextField(
            value = gravityOptions.find { it.second == selectedGravity }?.first
                ?: context.getString(R.string.location_gravity_bottom_end),
            onValueChange = {},
            readOnly = true,
            label = { Text(context.getString(R.string.location_button_gravity)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gravityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("textfield_location_view_gravity")
        )
        ExposedDropdownMenu(
            expanded = gravityExpanded,
            onDismissRequest = { gravityExpanded = false },
            modifier = Modifier.testTag("menu_location_view_gravity")
        ) {
            gravityOptions.forEach { (name, gravity) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        selectedGravity = gravity
                        mapView?.getMapAsync { maptecMap ->
                            maptecMap.uiSettings.setLocationViewGravity(gravity)
                        }
                        gravityExpanded = false
                    },
                    modifier = Modifier.testTag("menu_item_$name")
                )
            }
        }
    }

    Text(
        text = context.getString(R.string.location_view_size_label, locationViewSizeDp.toInt()),
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Slider(
        value = locationViewSizeDp,
        onValueChange = { locationViewSizeDp = it },
        onValueChangeFinished = {
            mapView?.getMapAsync { map ->
                val sizePx = with(density) { locationViewSizeDp.dp.roundToPx() }
                map.uiSettings.setLocationViewSize(sizePx, sizePx)
            }
        },
        valueRange = 30f..60f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("slider_location_view_size")
    )
}
