@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.overlays

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.viewmodel.MarkerAnimationViewModel
import com.maptec.applied.demo.viewmodel.MarkerAnimationViewModel.MainAnimationMode
import com.maptec.applied.demo.viewmodel.MarkerAnimationViewModelFactory

private val FIELD_PADDING = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerAnimationScreen(
    viewModel: MarkerAnimationViewModel = viewModel(factory = MarkerAnimationViewModelFactory())
) {
    val context = LocalContext.current
    val markers by viewModel.markers.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val selectedMarkerId by viewModel.selectedMarkerId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
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
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.testTag("anim_sheet_drag_handle"),
            )
        },
        sheetContent = {
            MarkerAnimationDetailPanel(viewModel = viewModel)
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Mapview(
                    modifier = Modifier.fillMaxSize(),
                    onStyleRendered = { mapView, mapLibreMap, style ->
                        viewModel.initSymbolManager(context, mapView, mapLibreMap, style)
                    },
                )
                MainAnimationTopBar(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
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
}

/** 地图顶部：共用开始 / 结束动画按钮。 */
@Composable
private fun MainAnimationTopBar(
    viewModel: MarkerAnimationViewModel,
    modifier: Modifier = Modifier,
) {
    val mode by viewModel.mainAnimationMode.collectAsState()
    val modeLabel = when (mode) {
        MainAnimationMode.ENTER -> "进入动画"
        MainAnimationMode.DISAPPEAR -> "消失动画"
    }

    Row(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("anim_top_bar"),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = modeLabel,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { viewModel.startMainAnimation() },
            modifier = Modifier
                .weight(1.2f)
                .testTag(
                    when (mode) {
                        MainAnimationMode.ENTER -> "anim_btn_enter_start"
                        MainAnimationMode.DISAPPEAR -> "anim_btn_disappear_start"
                    }
                ),
        ) {
            Text("开始动画")
        }
        OutlinedButton(
            onClick = { viewModel.endMainAnimation() },
            modifier = Modifier
                .weight(1.2f)
                .testTag(
                    when (mode) {
                        MainAnimationMode.ENTER -> "anim_btn_enter_end"
                        MainAnimationMode.DISAPPEAR -> "anim_btn_disappear_end"
                    }
                ),
        ) {
            Text("结束动画")
        }
    }
}

@Composable
fun MarkerAnimationDetailPanel(viewModel: MarkerAnimationViewModel) {
    val scrollState = rememberScrollState()
    val mode by viewModel.mainAnimationMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==================== 基础操作 ====================
        Button(
            onClick = { viewModel.addMarker() },
            modifier = Modifier.fillMaxWidth().testTag("symbol_btn_add_marker")
        ) {
            Text("添加 Marker")
        }

        Button(
            onClick = { viewModel.clearMarkers() },
            modifier = Modifier.fillMaxWidth().testTag("symbol_btn_clear_all"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("清除所有图标")
        }

        SectionDivider()

        // ==================== 进入 / 消失模式开关 ====================
        MainAnimationModeSwitch(
            selected = mode,
            onSelected = { viewModel.setMainAnimationMode(it) },
        )

        SectionDivider()

        // ==================== 进入动画配置 ====================
        EnterAnimationSection(
            viewModel = viewModel,
            enabled = mode == MainAnimationMode.ENTER,
        )

        SectionDivider()

        // ==================== 点选动画 ====================
        SelectAnimationSection(viewModel)

        SectionDivider()

        // ==================== 消失动画配置 ====================
        DisappearAnimationSection(
            viewModel = viewModel,
            enabled = mode == MainAnimationMode.DISAPPEAR,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MainAnimationModeSwitch(
    selected: MainAnimationMode,
    onSelected: (MainAnimationMode) -> Unit,
) {
    SectionTitle("顶部按钮控制")
    Text(
        text = "进入动画与消失动画互斥，切换后顶部「开始 / 结束」作用于所选模式。",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth().testTag("anim_mode_switch"),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
    ) {
        FilterChip(
            selected = selected == MainAnimationMode.ENTER,
            onClick = { onSelected(MainAnimationMode.ENTER) },
            label = { Text("进入动画") },
            modifier = Modifier.weight(1f).testTag("anim_mode_enter"),
        )
        FilterChip(
            selected = selected == MainAnimationMode.DISAPPEAR,
            onClick = { onSelected(MainAnimationMode.DISAPPEAR) },
            label = { Text("消失动画") },
            modifier = Modifier.weight(1f).testTag("anim_mode_disappear"),
        )
    }
}

@Composable
private fun EnterAnimationSection(
    viewModel: MarkerAnimationViewModel,
    enabled: Boolean,
) {
    val type by viewModel.enterType.collectAsState()
    val duration by viewModel.enterDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    SectionTitle("进入动画")
    if (!enabled) {
        Text(
            text = "当前未选中，请在上方切换为「进入动画」。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimationTypeDropdown(
            label = "进入动画类型",
            options = MarkerAnimationViewModel.ENTER_OPTIONS,
            selected = type,
            onSelected = { viewModel.setEnterType(it) },
            modifier = Modifier.weight(1f),
            testTag = "anim_dropdown_enter",
            enabled = enabled,
        )
        OutlinedTextField(
            value = durationStr,
            onValueChange = {
                durationStr = it
                it.toLongOrNull()?.let { v -> viewModel.setEnterDurationMs(v) }
            },
            label = { Text("duration(ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f).testTag("anim_input_enter_duration")
        )
    }
}

@Composable
private fun SelectAnimationSection(viewModel: MarkerAnimationViewModel) {
    val type by viewModel.selectType.collectAsState()
    val duration by viewModel.selectDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    SectionTitle("点选动画")
    Text(
        text = "选择类型后，点击地图空白处添加 Marker；「无」与普通添加一致，bounce 会播放弹跳点选动画。",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimationTypeDropdown(
            label = "点选动画类型",
            options = MarkerAnimationViewModel.SELECT_OPTIONS,
            selected = type,
            onSelected = { viewModel.setSelectType(it) },
            modifier = Modifier.weight(1f),
            testTag = "anim_dropdown_select"
        )
        OutlinedTextField(
            value = durationStr,
            onValueChange = {
                durationStr = it
                it.toLongOrNull()?.let { v -> viewModel.setSelectDurationMs(v) }
            },
            label = { Text("duration(ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("anim_input_select_duration")
        )
    }
}

@Composable
private fun DisappearAnimationSection(
    viewModel: MarkerAnimationViewModel,
    enabled: Boolean,
) {
    val type by viewModel.disappearType.collectAsState()
    val duration by viewModel.disappearDurationMs.collectAsState()
    var durationStr by remember(duration) { mutableStateOf(duration.toString()) }

    SectionTitle("消失动画")
    if (!enabled) {
        Text(
            text = "当前未选中，请在上方切换为「消失动画」。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FIELD_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimationTypeDropdown(
            label = "消失动画类型",
            options = MarkerAnimationViewModel.DISAPPEAR_OPTIONS,
            selected = type,
            onSelected = { viewModel.setDisappearType(it) },
            modifier = Modifier.weight(1f),
            testTag = "anim_dropdown_disappear",
            enabled = enabled,
        )
        OutlinedTextField(
            value = durationStr,
            onValueChange = {
                durationStr = it
                it.toLongOrNull()?.let { v -> viewModel.setDisappearDurationMs(v) }
            },
            label = { Text("duration(ms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f).testTag("anim_input_disappear_duration")
        )
    }
}

@Composable
private fun AnimationTypeDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.find { it.first == selected }?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(testTag),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = Color.LightGray)
}
