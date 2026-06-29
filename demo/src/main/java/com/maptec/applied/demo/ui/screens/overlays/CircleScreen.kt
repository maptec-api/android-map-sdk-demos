package com.maptec.applied.demo.ui.screens.overlays

import android.content.Context
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
import android.widget.Toast
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.animation.AlphaAnimation
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.AnimationCallbacks
import com.maptec.applied.maps.animation.AnimationSet
import com.maptec.applied.maps.animation.AnimationStatus
import com.maptec.applied.maps.animation.MapAnimationCurve
import com.maptec.applied.maps.animation.PulsatingAnimation
import com.maptec.applied.maps.animation.RadiusAnimation
import com.maptec.applied.maps.animation.RepeatMode
import com.maptec.applied.maps.animation.ScanningAnimation
import com.maptec.applied.demo.ui.screens.overlays.circle.addImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.maptec.applied.demo.R

private const val PULSE_OUTER_DELAY_MS = 0L
private const val PULSE_INNER_DELAY_MS = 500L
private const val PULSE_MIDDLE_DELAY_MS = 1000L

private data class CircleDropdownOption(val value: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    var mapStyle by remember { mutableStateOf<Style?>(null) }

    // 默认展开 bottom sheet
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 48.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            BottomDetailPanel(
                mapView = mapView,
                mapStyle = mapStyle,
                scaffoldState = scaffoldState,
            )
        },
        content = { padding ->
            Mapview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onMapReady = { view, mapLibreMap ->
                    mapView = view
                    mapLibreMap.getStyle { style ->
                        mapStyle = style
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomDetailPanel(
    mapView: MapView?,
    mapStyle: Style?,
    scaffoldState: androidx.compose.material3.BottomSheetScaffoldState,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var latLng by remember { mutableStateOf("1.4,103.75") }
    var color by remember { mutableStateOf("#3F7BF9") }
    var radius by remember { mutableStateOf("120") }
    var opacity by remember { mutableStateOf("0.5") }
    var strokeWidth by remember { mutableStateOf("2") }
    var strokeColor by remember { mutableStateOf("#FFFFFF") }
    var strokeOpacity by remember { mutableStateOf("0") }
    var geodesic by remember { mutableStateOf(false) }
    var draggable by remember { mutableStateOf(false) }
    
    // 内阴影
    var innerShadow by remember { mutableStateOf(false) }
    var innerShadowColor by remember { mutableStateOf("#000000") }
    var innerShadowOpacity by remember { mutableStateOf("0.5") }
    var innerShadowBlur by remember { mutableStateOf("10") }

    // 外发光
    var outerGlow by remember { mutableStateOf(false) }
    var glowColor by remember { mutableStateOf("#0066FF") }
    var glowRadius by remember { mutableStateOf("10") }

    // 动画
    var pulseAnimation by remember { mutableStateOf(false) }
    var pulseDuration by remember { mutableStateOf("2000") }
    val pulseDurationError = remember(pulseDuration) { validateAnimationLoop(pulseDuration) }
    var scanAnimation by remember { mutableStateOf(false) }
    var scanSpeed by remember { mutableStateOf("3000") }
    val scanSpeedError = remember(scanSpeed) { validateAnimationLoop(scanSpeed) }
    var scanSectorAngle by remember { mutableStateOf("60") }
    val scanSectorAngleError = remember(scanSectorAngle) { validateScanAngle(scanSectorAngle) }
    var scanSectorColor by remember { mutableStateOf("#E60012") }
    val scanSectorColorError = remember(scanSectorColor) { validateColor(scanSectorColor) }
    var scanIconUrlId by remember { mutableStateOf("scan_base_image") }
    /** 已通过 Style 注册的扫描底图 ID，绘制圆时写入 [CircleOptions.withScanBaseImage]。 */
    var scanBaseImageId by remember { mutableStateOf<String?>(null) }
    var radiusAnimation by remember { mutableStateOf(false) }
    var radiusAnimationRangeMin by remember { mutableStateOf("50") }
    var radiusAnimationRangeMax by remember { mutableStateOf("150") }
    var radiusDuration by remember { mutableStateOf("2000") }
    val radiusDurationError = remember(radiusDuration) { validateAnimationLoop(radiusDuration) }
    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var rippleCircle by remember { mutableStateOf<Circle?>(null) }
    var pulseMiddleCircle by remember { mutableStateOf<Circle?>(null) }
    var animationHandle by remember { mutableStateOf<Animation?>(null) }
    var rippleAnimationHandle by remember { mutableStateOf<Animation?>(null) }
    var pulseMiddleAnimationHandle by remember { mutableStateOf<Animation?>(null) }

    // 进阶属性
    var circleBlur by remember { mutableStateOf("0") }
    var circleTranslateX by remember { mutableStateOf("0") }
    var circleTranslateY by remember { mutableStateOf("0") }
    var circleTranslateAnchor by remember { mutableStateOf("map") }
    var circlePitchAlignment by remember { mutableStateOf("map") }
    var circlePitchScale by remember { mutableStateOf("map") }
    var circleSortKey by remember { mutableStateOf("0") }

    var translateAnchorMenuExpanded by remember { mutableStateOf(false) }
    var pitchAlignmentMenuExpanded by remember { mutableStateOf(false) }
    var pitchScaleMenuExpanded by remember { mutableStateOf(false) }

    val translateAnchorOptions = listOf(
        CircleDropdownOption("map", "地图 (map)"),
        CircleDropdownOption("viewport", "视口 (viewport)")
    )
    val alignmentOptions = listOf(
        CircleDropdownOption("map", "地图 (map)"),
        CircleDropdownOption("viewport", "视口 (viewport)")
    )
    val pitchScaleOptions = listOf(
        CircleDropdownOption("map", "地图 (map)"),
        CircleDropdownOption("viewport", "视口 (viewport)")
    )

    val latLngError = remember(latLng) { validateLatLng(latLng) }
    val colorError = remember(color) { validateColor(color) }
    val radiusError = remember(radius) { validateRadius(radius) }
    val opacityError = remember(opacity) { validateOpacity(opacity) }
    val strokeWidthError = remember(strokeWidth) { validateStrokeWidth(strokeWidth) }
    val strokeColorError = remember(strokeColor) { validateColor(strokeColor) }
    val strokeOpacityError = remember(strokeOpacity) { validateOpacity(strokeOpacity) }
    val glowColorError = remember(glowColor) { validateColor(glowColor) }
    val glowRadiusError = remember(glowRadius) { validateGlowRadius(glowRadius) }
    
    val isValid = latLngError == null && colorError == null && radiusError == null && 
                  opacityError == null  && strokeWidthError == null &&
                  strokeColorError == null && strokeOpacityError == null &&
                  (!outerGlow || (glowColorError == null && glowRadiusError == null))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 中心点
        OutlinedTextField(
            value = latLng,
            onValueChange = { latLng = it },
            label = { Text("中心点坐标 (lat,lng)") },
            supportingText = latLngError?.let { { Text(it, color = Color.Red) } },
            isError = latLngError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_latlng")
        )
        
        // 半径（米或像素由「等距模式」开关与引擎一致：关=像素半径，开=米）
        OutlinedTextField(
            value = radius,
            onValueChange = { radius = it },
            label = { Text(if (geodesic) "半径（米）" else "半径（像素）") },
            supportingText = radiusError?.let { { Text(it, color = Color.Red) } },
            isError = radiusError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_radius")
        )
        
        // 填充颜色和透明度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("填充颜色") },
                supportingText = colorError?.let { { Text(it, color = Color.Red) } },
                isError = colorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("circle_input_color")
            )
            OutlinedTextField(
                value = opacity,
                onValueChange = { opacity = it },
                label = { Text("填充透明度 (0~1)") },
                supportingText = opacityError?.let { { Text(it, color = Color.Red) } },
                isError = opacityError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("circle_input_opacity")
            )
        }
        
        // 描边颜色和宽度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = strokeColor,
                onValueChange = { strokeColor = it },
                label = { Text("描边颜色") },
                supportingText = strokeColorError?.let { { Text(it, color = Color.Red) } },
                isError = strokeColorError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("circle_input_stroke_color")
            )
            OutlinedTextField(
                value = strokeWidth,
                onValueChange = { strokeWidth = it },
                label = { Text("描边宽度") },
                supportingText = strokeWidthError?.let { { Text(it, color = Color.Red) } },
                isError = strokeWidthError != null,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .testTag("circle_input_stroke_width")
            )
        }

        // 描边透明度
        OutlinedTextField(
            value = strokeOpacity,
            onValueChange = { strokeOpacity = it },
            label = { Text("描边透明度 (0~1)") },
            supportingText = strokeOpacityError?.let { { Text(it, color = Color.Red) } },
            isError = strokeOpacityError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("circle_input_stroke_opacity")
        )

        // 开关选项：等距模式、可拖拽
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("等距模式 (Geodesic)", modifier = Modifier.weight(1f))
            Switch(
                checked = geodesic,
                onCheckedChange = { geodesic = it },
                modifier = Modifier.testTag("circle_switch_geodesic")
            )
        }
        Text(
            text = "提示：等距模式开启后，内阴影与外发光由引擎忽略，不会生效。",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("可拖拽 (Draggable)", modifier = Modifier.weight(1f))
            Switch(
                checked = draggable,
                onCheckedChange = { draggable = it },
                modifier = Modifier.testTag("circle_switch_draggable")
            )
        }

        // 内阴影设置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用内阴影", modifier = Modifier.weight(1f))
            Switch(
                checked = innerShadow,
                onCheckedChange = { innerShadow = it },
                modifier = Modifier.testTag("circle_switch_inner_shadow")
            )
        }
        if (innerShadow) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = innerShadowBlur,
                    onValueChange = { innerShadowBlur = it },
                    label = { Text("模糊度") },
                    supportingText = {
                        Text(
                            "说明：内阴影颜色与不透明度与上方「填充颜色」「填充透明度」一致。",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 外发光设置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用外发光", modifier = Modifier.weight(1f))
            Switch(
                checked = outerGlow,
                onCheckedChange = { outerGlow = it },
                modifier = Modifier.testTag("circle_switch_outer_glow")
            )
        }
        if (outerGlow) {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                OutlinedTextField(
                    value = glowColor,
                    onValueChange = { glowColor = it },
                    label = { Text("发光颜色") },
                    supportingText = glowColorError?.let { { Text(it, color = Color.Red) } },
                    isError = glowColorError != null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                OutlinedTextField(
                    value = glowRadius,
                    onValueChange = { glowRadius = it },
                    label = { Text("发光半径 (1～30)") },
                    supportingText = glowRadiusError?.let { { Text(it, color = Color.Red) } },
                    isError = glowRadiusError != null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }
        }

        // 动画设置

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = androidx . compose . ui . Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("扫描 (Scan)")
            Text("开始动画前再次点击绘制", fontSize = 11.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = scanAnimation,
                onCheckedChange = {
                    scanAnimation = it
                    if (it) {
                        pulseAnimation = false
                        radiusAnimation = false
                        geodesic = false
                    }
                },
                modifier = Modifier.testTag("circle_switch_scan"),
            )
        }
        if (scanAnimation) {
            OutlinedTextField(
                value = scanSectorAngle,
                onValueChange = { scanSectorAngle = it },
                label = { Text("扇形张角 (度, 0~90)") },
                supportingText = scanSectorAngleError?.let { { Text(it, color = Color.Red) } },
                isError = scanSectorAngleError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("circle_input_scan_sector_angle"),
            )
            OutlinedTextField(
                value = scanSectorColor,
                onValueChange = { scanSectorColor = it },
                label = { Text("扇形颜色 (空=填充色)") },
                isError = scanSectorColorError != null,
                supportingText = scanSectorColorError?.let { { Text(it, color = Color.Red) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("circle_input_scan_sector_color"),
            )
            OutlinedTextField(
                value = scanSpeed,
                onValueChange = { scanSpeed = it },
                label = { Text("旋转周期 (ms/圈)") },
                isError = scanSpeedError != null,
                supportingText = scanSpeedError?.let { { Text(it, color = Color.Red) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("circle_input_scan_speed"),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = scanIconUrlId,
                    onValueChange = { scanIconUrlId = it },
                    label = { Text("图标地址对应ID") },
                    supportingText = { Text("注册到 Style 的图标名", color = Color(0xFF666666)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("circle_input_scan_icon_url_id"),
                )
                Button(
                    onClick = {
                        val style = mapStyle
                        if (style == null) {
                            Toast.makeText(context, "地图样式未就绪", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val iconId = scanIconUrlId.trim().ifBlank { scanIconUrlId }
                        scope.launch {
                            withContext(Dispatchers.Main) {
                                try {
                                    if (style.getImage(iconId) == null) {
                                        addImage(context,R.drawable.circle_roate, iconId, style)
                                    }
                                    scanBaseImageId = iconId
                                    Toast.makeText(
                                        context,
                                        "底图已注册：$iconId",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "图标添加失败：${e.message ?: ""}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .testTag("circle_btn_scan_add_by_url"),
                    enabled = mapStyle != null,
                ) {
                    Text("按地址添加")
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = androidx . compose . ui . Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("脉动 (Pulse)")
            Text("开始动画前再次点击绘制", fontSize = 11.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = pulseAnimation, onCheckedChange = {
                pulseAnimation = it
                if (it) { scanAnimation = false; radiusAnimation = false }
            },modifier = Modifier.testTag("circle_switch_pulse"))
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp).
            fillMaxWidth().padding(vertical = 4.dp)) {
            Text("半径呼吸 (Radius)")
            Text("开始动画前再次点击绘制", fontSize = 11.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = radiusAnimation, onCheckedChange = { 
                radiusAnimation = it 
                if (it) { pulseAnimation = false; scanAnimation = false }
            },modifier = Modifier.testTag("circle_switch_radius"))
        }
        if (radiusAnimation || pulseAnimation || scanAnimation) {
            if(radiusAnimation || pulseAnimation){
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = radiusAnimationRangeMin,
                        onValueChange = { radiusAnimationRangeMin = it },
                        label = { Text("起始半径") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .testTag("circle_input_anim_min")
                    )
                    OutlinedTextField(
                        value = radiusAnimationRangeMax,
                        onValueChange = { radiusAnimationRangeMax = it },
                        label = { Text("目标半径") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("circle_input_anim_max")
                    )
                    OutlinedTextField(
                        value = if (pulseAnimation) pulseDuration else radiusDuration,
                        onValueChange = {
                            if (pulseAnimation) pulseDuration = it else radiusDuration = it
                        },
                        label = { Text("周期 (ms)") },
                        supportingText = (if (pulseAnimation) pulseDurationError else radiusDurationError)?.let { { Text(it, color = Color.Red)}
                                                                  },
                        isError = (if (pulseAnimation) pulseDurationError else radiusDurationError) != null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .testTag("circle_input_anim_duration")
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = {
                        val circle = lastCircle
                        if (circle == null || mapView == null) {
                            Toast.makeText(
                                mapView?.context,
                                "请先点击「绘制」添加圆",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        mapView.getMapAsync { map ->
                            val engine = map.getOverlayEngine()
                            animationHandle?.cancel()
                            rippleAnimationHandle?.cancel()
                            pulseMiddleAnimationHandle?.cancel()

                            if (scanAnimation) {
                                val scanAnim = buildScanRotationAnimation(scanSpeed)
                                    ?: run {
                                        Toast.makeText(mapView.context, "请填写有效的旋转周期", Toast.LENGTH_SHORT).show()
                                        return@getMapAsync
                                    }
                                if (scanSpeedError != null) {
                                    Toast.makeText(mapView.context, "请填写有效的旋转周期", Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                if (scanSectorAngleError != null) {
                                    Toast.makeText(mapView.context, "请填写有效的扇形张角", Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                animationHandle?.cancel()
                                animationHandle = circle.setAnimation(scanAnim)
                                animationHandle?.start()
                            } else if (pulseAnimation) {
                                val min = radiusAnimationRangeMin.toDoubleOrNull()
                                val max = radiusAnimationRangeMax.toDoubleOrNull()
                                if (min == null || max == null) {
                                    Toast.makeText(mapView.context, "请填写有效的半径范围", Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                if (pulseDurationError != null) {
                                    Toast.makeText(mapView.context, pulseDurationError, Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                val center = circle.getCenter()
                                val strokeW = strokeWidth.toFloatOrNull() ?: 2f
                                val strokeOp = strokeOpacity.toFloatOrNull() ?: 1f
                                rippleCircle?.let { engine.deleteCircle(it) }
                                pulseMiddleCircle?.let { engine.deleteCircle(it) }
                                rippleCircle = engine.addCircle(
                                    pulseLayerCircleOptions(
                                        center = center,
                                        startRadius = min,
                                        fillColor = color,
                                        fillOpacity = 0.15f,
                                        strokeColor = strokeColor,
                                        strokeWeight = strokeW,
                                        strokeOpacity = strokeOp,
                                        geodesic = geodesic,
                                    )
                                )
                                pulseMiddleCircle = engine.addCircle(
                                    pulseLayerCircleOptions(
                                        center = center,
                                        startRadius = min,
                                        fillColor = color,
                                        fillOpacity = 0.3f,
                                        strokeColor = strokeColor,
                                        strokeWeight = strokeW,
                                        strokeOpacity = strokeOp,
                                        geodesic = geodesic,
                                    )
                                )
                                val durationMs = pulseDuration.toLongOrNull() ?: 2000L
                                val outerPulse = buildPulseAnimation(min, max, durationMs, "pulse-outer")
                                    .apply { setDelayMs(PULSE_OUTER_DELAY_MS) }
                                val innerPulse = buildPulseAnimation(min, max, durationMs, "pulse-inner")
                                    .apply { setDelayMs(PULSE_INNER_DELAY_MS) }
                                val middlePulse = buildPulseAnimation(min, max, durationMs, "pulse-middle")
                                    .apply { setDelayMs(PULSE_MIDDLE_DELAY_MS) }
                                rippleAnimationHandle = rippleCircle?.setAnimation(outerPulse)
                                rippleAnimationHandle?.start()
                                animationHandle = circle.setAnimation(innerPulse)
                                animationHandle?.start()
                                pulseMiddleAnimationHandle = pulseMiddleCircle?.setAnimation(middlePulse)
                                pulseMiddleAnimationHandle?.start()
                            } else {
                                rippleCircle?.let { engine.deleteCircle(it) }
                                pulseMiddleCircle?.let { engine.deleteCircle(it) }
                                rippleCircle = null
                                pulseMiddleCircle = null
                                rippleAnimationHandle = null
                                pulseMiddleAnimationHandle = null
                                val min = radiusAnimationRangeMin.toDoubleOrNull()
                                val max = radiusAnimationRangeMax.toDoubleOrNull()
                                if (min == null || max == null) {
                                    Toast.makeText(mapView.context, "请填写有效的半径范围", Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                if (radiusDurationError != null) {
                                    Toast.makeText(mapView.context, radiusDurationError, Toast.LENGTH_SHORT).show()
                                    return@getMapAsync
                                }
                                val animation = buildCircleAnimation(
                                    pulseAnimation = false,
                                    pulseDuration = pulseDuration,
                                    scanAnimation = false,
                                    scanSpeed = scanSpeed,
                                    radiusAnimation = radiusAnimation,
                                    radiusAnimationRangeMin = radiusAnimationRangeMin,
                                    radiusAnimationRangeMax = radiusAnimationRangeMax,
                                    radiusDuration = radiusDuration,
                                ) ?: return@getMapAsync
                                animationHandle?.cancel()
                                animationHandle = circle.setAnimation(animation)
                                animationHandle?.start()
                            }

                        }
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    enabled = mapView != null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text("开始动画")
                }
                Button(
                    onClick = {
                        animationHandle?.cancel()
                        rippleAnimationHandle?.cancel()
                        pulseMiddleAnimationHandle?.cancel()
                        mapView?.getMapAsync { map ->
                            val engine = map.getOverlayEngine()
                            rippleCircle?.let { engine.deleteCircle(it) }
                            pulseMiddleCircle?.let { engine.deleteCircle(it) }
                            rippleCircle = null
                            pulseMiddleCircle = null
                            rippleAnimationHandle = null
                            pulseMiddleAnimationHandle = null
                        }
                    },
                    enabled = sequenceOf(
                        animationHandle,
                        rippleAnimationHandle,
                        pulseMiddleAnimationHandle,
                    ).any { handle ->
                        handle != null
                                && handle.status != AnimationStatus.CANCELLED
                                && handle.status != AnimationStatus.COMPLETED
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text("结束动画")
                }
            }
        }

        // 进阶渲染属性
//        Text("进阶渲染属性", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
//
//        OutlinedTextField(
//            value = circleBlur,
//            onValueChange = { circleBlur = it },
//            label = { Text("圆模糊度 (0-1)") },
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
//        )
//
//        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
//            OutlinedTextField(
//                value = circleTranslateX,
//                onValueChange = { circleTranslateX = it },
//                label = { Text("偏移 X") },
//                modifier = Modifier.weight(1f).padding(end = 4.dp)
//            )
//            OutlinedTextField(
//                value = circleTranslateY,
//                onValueChange = { circleTranslateY = it },
//                label = { Text("偏移 Y") },
//                modifier = Modifier.weight(1f).padding(start = 4.dp)
//            )
//        }
//
//        // Translate Anchor Dropdown
//        ExposedDropdownMenuBox(
//            expanded = translateAnchorMenuExpanded,
//            onExpandedChange = { translateAnchorMenuExpanded = it },
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
//        ) {
//            OutlinedTextField(
//                value = translateAnchorOptions.find { it.value == circleTranslateAnchor }?.label ?: circleTranslateAnchor,
//                onValueChange = {},
//                readOnly = true,
//                label = { Text("偏移锚点") },
//                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = translateAnchorMenuExpanded) },
//                modifier = Modifier.fillMaxWidth().menuAnchor()
//            )
//            ExposedDropdownMenu(
//                expanded = translateAnchorMenuExpanded,
//                onDismissRequest = { translateAnchorMenuExpanded = false }
//            ) {
//                translateAnchorOptions.forEach { option ->
//                    DropdownMenuItem(
//                        text = { Text(option.label) },
//                        onClick = {
//                            circleTranslateAnchor = option.value
//                            translateAnchorMenuExpanded = false
//                        }
//                    )
//                }
//            }
//        }
//
//        // Pitch Alignment Dropdown
//        ExposedDropdownMenuBox(
//            expanded = pitchAlignmentMenuExpanded,
//            onExpandedChange = { pitchAlignmentMenuExpanded = it },
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
//        ) {
//            OutlinedTextField(
//                value = alignmentOptions.find { it.value == circlePitchAlignment }?.label ?: circlePitchAlignment,
//                onValueChange = {},
//                readOnly = true,
//                label = { Text("俯仰对齐") },
//                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pitchAlignmentMenuExpanded) },
//                modifier = Modifier.fillMaxWidth().menuAnchor()
//            )
//            ExposedDropdownMenu(
//                expanded = pitchAlignmentMenuExpanded,
//                onDismissRequest = { pitchAlignmentMenuExpanded = false }
//            ) {
//                alignmentOptions.forEach { option ->
//                    DropdownMenuItem(
//                        text = { Text(option.label) },
//                        onClick = {
//                            circlePitchAlignment = option.value
//                            pitchAlignmentMenuExpanded = false
//                        }
//                    )
//                }
//            }
//        }
//
//        // Pitch Scale Dropdown
//        ExposedDropdownMenuBox(
//            expanded = pitchScaleMenuExpanded,
//            onExpandedChange = { pitchScaleMenuExpanded = it },
//            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
//        ) {
//            OutlinedTextField(
//                value = pitchScaleOptions.find { it.value == circlePitchScale }?.label ?: circlePitchScale,
//                onValueChange = {},
//                readOnly = true,
//                label = { Text("俯仰缩放") },
//                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pitchScaleMenuExpanded) },
//                modifier = Modifier.fillMaxWidth().menuAnchor()
//            )
//            ExposedDropdownMenu(
//                expanded = pitchScaleMenuExpanded,
//                onDismissRequest = { pitchScaleMenuExpanded = false }
//            ) {
//                pitchScaleOptions.forEach { option ->
//                    DropdownMenuItem(
//                        text = { Text(option.label) },
//                        onClick = {
//                            circlePitchScale = option.value
//                            pitchScaleMenuExpanded = false
//                        }
//                    )
//                }
//            }
//        }
//
//        OutlinedTextField(
//            value = circleSortKey,
//            onValueChange = { circleSortKey = it },
//            label = { Text("排序权重 (Sort Key)") },
//            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
//        )

        Button(
            onClick = {
                val center = parseCircleLatLng(latLng) ?: return@Button
                mapView?.getMapAsync { map ->
                    val engine: MapOverlayEngine = map.getOverlayEngine()
                    engine.deleteAllCircles()

                    val options = CircleOptions()
                        .withCenter(center)
                        .withRadius(radius.toDouble())
                        .withFillColor(color)
                        .withFillOpacity(opacity.toFloat())
                        .withStrokeColor(strokeColor)
                        .withStrokeWeight(strokeWidth.toFloat())
                        .withStrokeOpacity(strokeOpacity.toFloat())
                        .withGeodesic(geodesic)
                        .withDraggable(draggable)
                        .withInnerShadow(innerShadow)
                        .withInnerShadowColor(innerShadowColor)
                        .withInnerShadowOpacity(innerShadowOpacity.toFloat())
                        .withInnerShadowBlur(innerShadowBlur.toFloat())
                        .withOuterGlow(outerGlow)
                        .withGlowColor(glowColor)
                        .withGlowRadius(glowRadius.toFloat())
                        .withCircleBlur(circleBlur.toFloat())
                        .withCircleTranslate(android.graphics.PointF(circleTranslateX.toFloat(), circleTranslateY.toFloat()))
                        .withCircleTranslateAnchor(circleTranslateAnchor)
                        .withCirclePitchAlignment(circlePitchAlignment)
                        .withCirclePitchScale(circlePitchScale)
                        .withCircleSortKey(circleSortKey.toFloat())
                    applyScanStyleToCircleOptions(
                        options = options,
                        scanEnabled = scanAnimation,
                        fillColor = color,
                        scanSectorColorInput = scanSectorColor,
                        scanSectorAngleInput = scanSectorAngle,
                        scanBaseImageId = scanBaseImageId,
                    )

                    animationHandle?.cancel()
                    rippleAnimationHandle?.cancel()
                    pulseMiddleAnimationHandle?.cancel()
                    rippleCircle = null
                    pulseMiddleCircle = null
                    rippleAnimationHandle = null
                    pulseMiddleAnimationHandle = null
                    animationHandle = null

                    val circle = engine.addCircle(options)
                    lastCircle = circle
                    animationHandle = buildCircleAnimation(
                        pulseAnimation = pulseAnimation,
                        pulseDuration = pulseDuration,
                        scanAnimation = scanAnimation,
                        scanSpeed = scanSpeed,
                        radiusAnimation = radiusAnimation,
                        radiusAnimationRangeMin = radiusAnimationRangeMin,
                        radiusAnimationRangeMax = radiusAnimationRangeMax,
                        radiusDuration = radiusDuration,
                    )?.let { circle.setAnimation(it) }

                    circle.addOnDragListener(object : OnOverlayDragListener<Circle> {
                        override fun onAnnotationDragStarted(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDragStarted" }
                        }

                        override fun onAnnotationDrag(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDrag" }
                        }

                        override fun onAnnotationDragFinished(circle: Circle) {
                            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onAnnotationDragFinished" }
                            Toast.makeText(mapView.context, "拖拽结束: ${circle.id}", Toast.LENGTH_SHORT).show()
                        }
                    })
                    circle.addOnMapClickListener(OnOverlayClickListener<Circle> { c ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onOverlayClick" }
                        Toast.makeText(mapView.context, "点击了圆: ${c.id}", Toast.LENGTH_SHORT).show()
                        true
                    })
                    circle.addOnMapLongClickListener(OnOverlayLongClickListener<Circle> { c ->
                        LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "onOverlayLongClick" }
                        Toast.makeText(mapView.context, "长按了圆: ${c.id}", Toast.LENGTH_SHORT).show()
                        true
                    })

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude = center.latitude, longitude = center.longitude),
                            15.0
                        )
                    )
                }
                scope.launch {
                    scaffoldState.bottomSheetState.partialExpand()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("circle_draw_button"),
            enabled = isValid
        ) {
            Text("绘制")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun circleAnimationCallbacks(label: String): AnimationCallbacks =
    object : AnimationCallbacks {
        override fun onStart() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onStart" }
        }

        override fun onUpdate(progress: Float) {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onUpdate: $progress" }
        }

        override fun onPause() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onPause" }
        }

        override fun onResume() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onResume" }
        }

        override fun onComplete(finished: Boolean) {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onComplete finished=$finished" }
        }

        override fun onCancel() {
            LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").d { "$label animation onCancel" }
        }
    }

/**
 * 静态扇形样式写入 [CircleOptions]（引擎 {@code ScanAnimation}）；
 * 旋转由 [ScanningAnimation] 驱动。
 */
private fun applyScanStyleToCircleOptions(
    options: CircleOptions,
    scanEnabled: Boolean,
    fillColor: String,
    scanSectorColorInput: String,
    scanSectorAngleInput: String,
    scanBaseImageId: String?,
): CircleOptions {
    if (!scanEnabled) {
        return options
    }
    val sectorAngle = scanSectorAngleInput.toFloatOrNull()?.takeIf { it > 0f } ?: 60f
    val sectorColor = scanSectorColorInput.trim().ifEmpty { fillColor.trim() }
    var result = options
        .withScanEnabled(scanEnabled)
        .withScanSectorAngle(sectorAngle)
        .withScanSectorColor(sectorColor)
        .withScanRotationAngle(0f)
    val baseId = scanBaseImageId?.trim().orEmpty()
    if (baseId.isNotEmpty()) {
        result = result.withScanBaseImage(baseId)
    }
    return result
}

/**
 * 扫描旋转：native {@code scaningAnimation} 为 0°→360°。
 * */
private fun buildScanRotationAnimation(scanSpeed: String): Animation? {
    val durationMs = scanSpeed.toLongOrNull()?.takeIf { it > 0L } ?: return null
    return ScanningAnimation(durationMs).apply {
        setListener(circleAnimationCallbacks("scan"))
    }
}


private fun buildCircleAnimation(
    pulseAnimation: Boolean,
    pulseDuration: String,
    scanAnimation: Boolean,
    scanSpeed: String,
    radiusAnimation: Boolean,
    radiusAnimationRangeMin: String,
    radiusAnimationRangeMax: String,
    radiusDuration: String,
): Animation? {
    if (scanAnimation) {
        return buildScanRotationAnimation(scanSpeed)
    }
    val min = radiusAnimationRangeMin.toDoubleOrNull() ?: return null
    val max = radiusAnimationRangeMax.toDoubleOrNull() ?: return null
    return when {
        pulseAnimation -> buildPulseAnimation(
            min,
            max,
            pulseDuration.toLongOrNull() ?: 2000L,
            "pulse",
        )
        radiusAnimation -> RadiusAnimation(min, max).apply {
            setDurationMs(radiusDuration.toLongOrNull() ?: 2000L)
            setRepeatCount(-1)
            setRepeatMode(RepeatMode.REVERSE)
            setListener(circleAnimationCallbacks("radius"))
        }
        else -> null
    }
}

/** 与 {@link #buildPulseAnimation} 配套的原始脉动动画（半径 + 透明度，REVERSE 无限循环）。 */
private fun buildPulseAnimation(
    min: Double,
    max: Double,
    durationMs: Long,
    label: String,
): Animation {
    val pulsating = PulsatingAnimation(min, max, durationMs)
    return AnimationSet().apply {
        setDurationMs(durationMs)
        // Native 只读根节点 getCurve()；必须把 PulsatingAnimation 上的 curve 设到 AnimationSet。
        setCurve(MapAnimationCurve.EASE_IN_OUT)
        setRepeatMode(RepeatMode.REVERSE)
        setRepeatCount(-1)
        addAnimation(pulsating)
        addAnimation(
            AlphaAnimation(0.5f, 0f).apply {
                setDurationMs(durationMs)
            }
        )
        setListener(circleAnimationCallbacks(label))
    }
}

/** 脉动附加层：与主圆同心，通过 fillOpacity 区分层次。 */
private fun pulseLayerCircleOptions(
    center: LatLng,
    startRadius: Double,
    fillColor: String,
    fillOpacity: Float,
    strokeColor: String,
    strokeWeight: Float,
    strokeOpacity: Float,
    geodesic: Boolean,
): CircleOptions {
    return CircleOptions()
        .withCenter(center)
        .withRadius(startRadius)
        .withFillColor(fillColor)
        .withFillOpacity(fillOpacity)
        .withStrokeColor(strokeColor)
        .withStrokeWeight(strokeWeight)
        .withStrokeOpacity(strokeOpacity * 0f)
        .withGeodesic(geodesic)
        .withDraggable(false)
}

private fun validateLatLng(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入中心点坐标"
    val point = parseCircleLatLng(trimmed) ?: return "格式错误，示例：1.4, 103.75"
    return when {
        point.latitude < -90 || point.latitude > 90 -> "纬度应在 -90～90 之间"
        point.longitude < -180 || point.longitude > 180 -> "经度应在 -180～180 之间"
        else -> null
    }
}

private fun validateColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入颜色"
    val pattern = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    return if (pattern.matches(trimmed)) null else "格式错误，示例：#00A63E 或 #RRGGBB"
}

private fun validateRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return when {
        value <= 0 -> "半径必须大于 0"
        value > 1_000_000 -> "半径不宜超过 1000000 "
        else -> null
    }
}

private fun validateOpacity(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入透明度（0～1）"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..1f) null else "透明度应在 0～1 之间"
}

private fun validateStrokeWidth(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null // 描边宽度可选
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value >= 0f) null else "描边宽度应大于等于 0"
}

private fun validateGlowRadius(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入发光半径"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 1f..30f) null else "发光半径应在 1～30 之间"
}

private fun validateAnimationLoop(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入动画周期"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 500f..10000f) null else "动画周期应在 500～10000 之间"
}

private fun validateScanAngle(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入扇形角度"
    val value = trimmed.toFloatOrNull() ?: return "请输入有效数字"
    return if (value in 0f..90f) null else "扇形角度应在 0～90 之间"
}

private fun parseCircleLatLng(input: String): LatLng? {
    return try {
        val content = input.trim()
        if (content.isEmpty()) return null

        val coords = content.split(",").map { it.trim().toDouble() }
        if (coords.size >= 2) LatLng(coords[0], coords[1]) else null
    } catch (e: Exception) {
        LoggerFactory.getLogger(LOG_MODULE).withTag("CircleScreen").e { "Failed to parse coordinates: ${e.message}" }
        null
    }
}

@Preview
@Composable
fun CircleScreenPreview() {
    CircleScreen()
}
