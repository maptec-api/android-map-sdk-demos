package com.maptec.applied.demo.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.maptec.applied.maps.StyleStatusCallback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.demo.map.MapViewLifecycleEffect

// 1. 将魔法数字提取为顶部常量配置
object MapCameraLimits {
    const val MIN_TILT = 0.0
    const val MAX_TILT_ABSOLUTE = 85.0
    const val MAX_TILT_DEFAULT = 60.0

    const val MIN_ZOOM = 0.0
    const val MAX_ZOOM = 20.0
}

@Composable
fun MapControlScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(LatLng(1.4, 103.75)).zoom(16.0)
        .build(),
) {
    var tilt by remember { mutableStateOf("40") }
    var bearing by remember { mutableStateOf("0") }
    var zoom by remember { mutableStateOf("14") }

    var minTilt by remember { mutableStateOf("0") }
    var maxTilt by remember { mutableStateOf("60") }
    var minZoom by remember { mutableStateOf("0") }
    var maxZoom by remember { mutableStateOf("20") }

    var isStyleRendered by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }


    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: com.maptec.applied.maps.Style?) {}
                    override fun onStyleRendered(style: com.maptec.applied.maps.Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: com.maptec.applied.maps.Style?, message: String) {}
                })

                map.addOnCameraMoveListener {
                    map.cameraPosition.let {
                        zoom = String.format("%.2f", it.zoom)
                        bearing = String.format("%.2f", it.bearing)
                        tilt = String.format("%.2f", it.tilt)
                    }
                }
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    Box(modifier = Modifier.fillMaxSize()) {
        // 地图内容
        AndroidView(
            factory = { context -> mapView.apply { tag = "mapView" } },
            update = { mapView -> },
            modifier = Modifier
                .matchParentSize()
                .testTag("mapView")
        )

        if (isStyleRendered) {
            Box(modifier = Modifier.testTag("mapRendered"))
        }

        // 底部控制面板
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .let { if (isExpanded) it else it.height(60.dp) }
                .background(Color.White)) {
            // 展开/收起标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.LightGray)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "地图控制",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "收起" else "展开"
                )
            }

            // 展开内容
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row {
                        // 左侧控制列
                        Column(modifier = Modifier.weight(1f)) {
                            LimitTextField(
                                value = tilt,
                                onValueChange = { tilt = it },
                                label = "倾斜角度",
                                testTag = "tilt",
                                maxValue = MapCameraLimits.MAX_TILT_ABSOLUTE
                            )
                            LimitTextField(
                                value = bearing,
                                onValueChange = { bearing = it },
                                label = "旋转角度",
                                testTag = "bearing",
                                allowNegative = true
                            )
                            LimitTextField(
                                value = zoom,
                                onValueChange = { zoom = it },
                                label = "比例尺",
                                testTag = "zoom",
                                maxValue = MapCameraLimits.MAX_ZOOM
                            )
                        }

                        // 右侧控制列
                        Column(modifier = Modifier.weight(1f)) {
                            LimitTextField(
                                value = minTilt,
                                onValueChange = { minTilt = it },
                                label = "最小倾斜角",
                                testTag = "minTilt",
                                maxValue = MapCameraLimits.MAX_TILT_DEFAULT
                            )
                            LimitTextField(
                                value = maxTilt,
                                onValueChange = { maxTilt = it },
                                label = "最大倾斜角",
                                testTag = "maxTilt",
                                maxValue = MapCameraLimits.MAX_TILT_ABSOLUTE
                            )
                            LimitTextField(
                                value = minZoom,
                                onValueChange = { minZoom = it },
                                label = "最小比例尺",
                                testTag = "minZoom",
                                maxValue = MapCameraLimits.MAX_ZOOM
                            )
                            LimitTextField(
                                value = maxZoom,
                                onValueChange = { maxZoom = it },
                                label = "最大比例尺",
                                testTag = "maxZoom",
                                maxValue = MapCameraLimits.MAX_ZOOM
                            )
                        }
                    }

                    Button(
                        onClick = {
                            // 3. 点击时进行最终的兜底校验 (coerceIn)，确保安全性
                            val tiltValue = if (tilt.isNotEmpty()) {
                                tilt.toDoubleOrNull()?.coerceIn(
                                    MapCameraLimits.MIN_TILT, MapCameraLimits.MAX_TILT_ABSOLUTE
                                )
                            } else null
                            val bearingValue = bearing.toDoubleOrNull()
                            val zoomValue = zoom.toDoubleOrNull()

                            mapView.getMapAsync { map ->
                                if (minTilt.isNotEmpty()) {
                                    val v = (minTilt.toDoubleOrNull()
                                        ?: MapCameraLimits.MIN_TILT).coerceIn(
                                        MapCameraLimits.MIN_TILT,
                                        MapCameraLimits.MAX_TILT_DEFAULT
                                    )
                                    map.setMinPitchPreference(v)
                                }
                                if (maxTilt.isNotEmpty()) {
                                    val v = (maxTilt.toDoubleOrNull()
                                        ?: MapCameraLimits.MAX_TILT_DEFAULT).coerceIn(
                                        MapCameraLimits.MIN_TILT,
                                        MapCameraLimits.MAX_TILT_ABSOLUTE
                                    )
                                    map.setMaxPitchPreference(v)
                                }
                                if (minZoom.isNotEmpty()) {
                                    map.setMinZoomPreference(
                                        minZoom.toDoubleOrNull() ?: MapCameraLimits.MIN_ZOOM
                                    )
                                }
                                if (maxZoom.isNotEmpty()) {
                                    map.setMaxZoomPreference(
                                        maxZoom.toDoubleOrNull() ?: MapCameraLimits.MAX_ZOOM
                                    )
                                }

                                val cur = map.cameraPosition
                                val builder = CameraPosition.Builder().target(cur.target)

                                if (tiltValue != null) builder.tilt(tiltValue)
                                if (bearingValue != null) builder.bearing(bearingValue)
                                if (zoomValue != null) builder.zoom(zoomValue)

                                map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()))

                                // 回填当前生效的数值
                                minTilt = String.format("%.2f", map.minPitch)
                                maxTilt = String.format("%.2f", map.maxPitch)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp, 2.dp, 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.change_camera),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// 2. 抽取公用的带限制功能的 TextField 组件
@Composable
fun LimitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    allowNegative: Boolean = false,
    maxValue: Double? = null
) {
    TextField(
        value = value,
        onValueChange = { newValue ->
            // 正则校验合法数字（可选负数支持）
            val regex = if (allowNegative) Regex("^[-+]?\\d*\\.?\\d*$") else Regex("^\\d*\\.?\\d*$")
            if (newValue.isEmpty() || newValue.matches(regex)) {
                val num = newValue.toDoubleOrNull()
                // 输入时立即限制最大值：如果输入的值超过允许的最大值，直接回退并显示最大值
                if (num != null && maxValue != null && num > maxValue) {
                    onValueChange(maxValue.toString())
                } else {
                    onValueChange(newValue)
                }
            }
        },
        label = { Text(text = label) },
        singleLine = true,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag)
    )
}