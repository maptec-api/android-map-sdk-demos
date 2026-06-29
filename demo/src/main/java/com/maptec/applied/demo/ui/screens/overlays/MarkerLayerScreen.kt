@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.MarkerLayerViewModel
import com.maptec.applied.demo.viewmodel.MarkerLayerViewModelFactory
import com.maptec.applied.style.layers.Property

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerLayerScreen(
    viewModel: MarkerLayerViewModel = viewModel(factory = MarkerLayerViewModelFactory())
) {
    val context = LocalContext.current
    val markers by viewModel.markers.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
    val dragCompletedCount by viewModel.dragCompletedCount.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            MarkerLayerDetailPanel(viewModel = viewModel)
        },
        content = { padding ->
            Mapview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onMapReady = { mapView, mapLibreMap ->
                    mapLibreMap.getStyle { style ->
                        viewModel.initSymbolManager(context, mapView, mapLibreMap, style)
                    }
                }
            )
        }
    )

    if (markers.isNotEmpty()) {
        Box(Modifier.size(1.dp).testTag("symbol_layer_has_markers"))
    }
    if (selectedMarkerId != null) {
        Box(Modifier.size(1.dp).testTag("symbol_layer_marker_selected"))
    }
    if (isDragging) {
        Box(Modifier.size(1.dp).testTag("symbol_layer_marker_dragging"))
    }
    if (dragCompletedCount > 0) {
        Box(Modifier.size(1.dp).testTag("symbol_layer_marker_drag_completed"))
    }
}

private val ICON_ANCHOR_OPTIONS = listOf(
    Property.ICON_ANCHOR_BOTTOM to "底部",
    Property.ICON_ANCHOR_CENTER to "中心",
    Property.ICON_ANCHOR_TOP to "顶部",
    Property.ICON_ANCHOR_LEFT to "左",
    Property.ICON_ANCHOR_RIGHT to "右",
    Property.ICON_ANCHOR_TOP_LEFT to "左上",
    Property.ICON_ANCHOR_TOP_RIGHT to "右上",
    Property.ICON_ANCHOR_BOTTOM_LEFT to "左下",
    Property.ICON_ANCHOR_BOTTOM_RIGHT to "右下",
)

private val FIELD_PAIR_PADDING = 8.dp

@Composable
fun MarkerLayerDetailPanel(viewModel: MarkerLayerViewModel) {
    val scrollState = rememberScrollState()

    // 公共参数
    val defaultLatLng by viewModel.defaultLatLng.collectAsState()
    val iconOpacity by viewModel.iconOpacity.collectAsState()
    val iconAnchor by viewModel.iconAnchor.collectAsState()
    val iconScaleWithZoom by viewModel.iconScaleWithZoom.collectAsState()
    val iconSize by viewModel.iconSize.collectAsState()
    val minScale by viewModel.minScale.collectAsState()
    val minZoom by viewModel.minZoom.collectAsState()
    val maxZoom by viewModel.maxZoom.collectAsState()

    // 按地址添加
    val iconUrl by viewModel.iconUrl.collectAsState()
    val iconUrlId by viewModel.iconUrlId.collectAsState()

    // 按类型添加
    val iconType by viewModel.iconType.collectAsState()

    // SDF
    val sdfIconColor by viewModel.sdfIconColor.collectAsState()

    // 文本状态
    var iconOpacityStr by remember(iconOpacity) { mutableStateOf(iconOpacity.toString()) }
    var iconSizeStr by remember(iconSize) { mutableStateOf(iconSize.toString()) }
    var minScaleStr by remember(minScale) { mutableStateOf(minScale.toString()) }
    var minZoomStr by remember(minZoom) { mutableStateOf(minZoom.toString()) }
    var maxZoomStr by remember(maxZoom) { mutableStateOf(maxZoom.toString()) }

    var anchorExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val iconSizeError = remember(iconSizeStr) {
        iconSizeStr.toFloatOrNull()?.takeIf { it in 0.1f..5f } == null && iconSizeStr.isNotBlank()
    }
    val opacityError = remember(iconOpacityStr) {
        iconOpacityStr.toFloatOrNull()?.takeIf { it in 0f..1f } == null && iconOpacityStr.isNotBlank()
    }
    val colorError = remember(sdfIconColor) { validateColor(sdfIconColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // ==================== 公共参数 ====================
        SectionTitle("公共参数")

        LatLngOutlinedTextField(
            value = defaultLatLng,
            onValueChange = { viewModel.setDefaultLatLng(it) },
            label = "位置坐标",
            hint = "1.45,103.80",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            testTag = "symbol_input_default_latlng"
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
        ) {
            OutlinedTextField(
                value = iconOpacityStr,
                onValueChange = { iconOpacityStr = it; it.toFloatOrNull()?.let { f -> viewModel.setIconOpacity(f) } },
                label = { Text("图标透明度") },
                supportingText = { Text("0～1", color = Color.Gray) },
                isError = opacityError,
                modifier = Modifier.weight(1f).testTag("symbol_input_icon_opacity")
            )
            ExposedDropdownMenuBox(
                expanded = anchorExpanded,
                onExpandedChange = { anchorExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = ICON_ANCHOR_OPTIONS.find { it.first == iconAnchor }?.second ?: iconAnchor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("图标锚点") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = anchorExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = anchorExpanded,
                    onDismissRequest = { anchorExpanded = false }
                ) {
                    ICON_ANCHOR_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setIconAnchor(value)
                                anchorExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
            ) {
                Text("随缩放变化大小")
                Switch(
                    checked = iconScaleWithZoom,
                    onCheckedChange = { viewModel.setIconScaleWithZoom(it) },
                    modifier = Modifier.testTag("symbol_switch_icon_scale_with_zoom"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
        ) {
            OutlinedTextField(
                value = iconSizeStr,
                onValueChange = { iconSizeStr = it; it.toFloatOrNull()?.let { f -> viewModel.setIconSize(f) } },
                label = { Text("图标大小(最大比例)") },
                supportingText = { Text("0.1～5", color = Color.Gray) },
                isError = iconSizeError,
                modifier = Modifier.weight(1f).testTag("symbol_input_icon_size")
            )
            OutlinedTextField(
                value = minScaleStr,
                onValueChange = { minScaleStr = it; it.toFloatOrNull()?.let { v -> viewModel.setMinScale(v) } },
                label = { Text("最小比例") },
                modifier = Modifier.weight(1f)
            )
        }

        if (iconScaleWithZoom) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
            ) {
                OutlinedTextField(
                    value = minZoomStr,
                    onValueChange = { minZoomStr = it; it.toIntOrNull()?.let { v -> viewModel.setMinZoom(v) } },
                    label = { Text("最小比例对应Zoom") },
                    modifier = Modifier.weight(1f).testTag("symbol_input_min_zoom"),
                )
                OutlinedTextField(
                    value = maxZoomStr,
                    onValueChange = { maxZoomStr = it; it.toIntOrNull()?.let { v -> viewModel.setMaxZoom(v) } },
                    label = { Text("最大比例对应Zoom") },
                    modifier = Modifier.weight(1f).testTag("symbol_input_max_zoom"),
                )
            }
        }

        SectionDivider()

        // ==================== 按图标地址添加 Marker ====================
        SectionTitle("按图标地址添加 Marker")

        OutlinedTextField(
            value = iconUrl,
            onValueChange = { viewModel.setIconUrl(it) },
            label = { Text("图标地址") },
            supportingText = { Text("图片URL", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("symbol_input_icon_url")
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
        ) {
            OutlinedTextField(
                value = iconUrlId,
                onValueChange = { viewModel.setIconUrlId(it) },
                label = { Text("图标地址对应ID") },
                supportingText = { Text("注册到 Style 的图标名", color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("symbol_input_icon_url_id")
            )
            Button(
                onClick = { viewModel.addMarkerByUrl() },
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .testTag("symbol_btn_add_by_url"),
                enabled = validateLatLng(viewModel.defaultLatLng.collectAsState().value) == null
            ) {
                Text("按地址添加")
            }
        }

        SectionDivider()

        // ==================== 按图标类型添加 Marker ====================
        SectionTitle("按图标类型添加 Marker")

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
        ) {
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = MarkerLayerViewModel.ICON_TYPE_OPTIONS.find { it.first == iconType }?.second ?: iconType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("图标类型") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    MarkerLayerViewModel.ICON_TYPE_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setIconType(value)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                onClick = { viewModel.addMarkerByType() },
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .testTag("symbol_btn_add_by_type"),
                enabled = validateLatLng(viewModel.defaultLatLng.collectAsState().value) == null
            ) {
                Text("按类型添加")
            }
        }

        SectionDivider()

        // ==================== 添加 SDF 可修改颜色 Marker ====================
        SectionTitle("添加 SDF 可修改颜色 Marker")

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(FIELD_PAIR_PADDING)
        ) {
            OutlinedTextField(
                value = sdfIconColor,
                onValueChange = { viewModel.setSdfIconColor(it) },
                label = { Text("图标颜色") },
                supportingText = colorError?.let { { Text(it, color = Color.Red) } }
                    ?: { Text("图标ID: marker-icon", color = Color.Gray) },
                isError = colorError != null,
                modifier = Modifier.weight(1f).testTag("symbol_input_sdf_icon_color")
            )
            Button(
                onClick = { viewModel.addMarkerBySdf() },
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .testTag("symbol_btn_add_sdf"),
                enabled = validateLatLng(viewModel.defaultLatLng.collectAsState().value) == null
            ) {
                Text("添加SDF")
            }
        }

        SectionDivider()

        // ==================== 清除所有图标 ====================
        Button(
            onClick = { viewModel.clearMarkers() },
            modifier = Modifier.fillMaxWidth().testTag("symbol_btn_clear_all"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("清除所有图标")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = Color.LightGray
    )
}

private fun validateColor(input: String): String? {
    val t = input.trim()
    if (t.isBlank()) return null
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(t)) null else "格式错误，示例：#3A5EFB"
}
