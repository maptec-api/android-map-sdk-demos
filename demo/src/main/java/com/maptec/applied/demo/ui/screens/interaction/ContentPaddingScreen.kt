package com.maptec.applied.demo.ui.screens.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSlider
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import androidx.compose.material3.ButtonColors

/**
 * Demo：配置并读回 [MaptecMap.setContentPadding] / [MaptecMap.getContentPadding]。
 *
 * 左/上/右/下为屏幕像素；应用后四边半透明遮罩与偏移一致，并可选把相机移到标记点以观察有效区域中心。
 */
@Composable
fun ContentPaddingScreen(
    modifier: Modifier = Modifier,
) {
    var isStyleRendered by remember { mutableStateOf(false) }
    var leftPx by remember { mutableFloatStateOf(0f) }
    var topPx by remember { mutableFloatStateOf(0f) }
    var rightPx by remember { mutableFloatStateOf(0f) }
    var bottomPx by remember { mutableFloatStateOf(0f) }
    var appliedLeft by remember { mutableFloatStateOf(0f) }
    var appliedTop by remember { mutableFloatStateOf(0f) }
    var appliedRight by remember { mutableFloatStateOf(0f) }
    var appliedBottom by remember { mutableFloatStateOf(0f) }
    var paddingReadout by remember { mutableStateOf("[0.0, 0.0, 0.0, 0.0]") }

    val density = LocalDensity.current
    val target = remember { ContentPaddingDemo.TARGET }

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(
                CameraPosition.Builder()
                    .target(target)
                    .zoom(ContentPaddingDemo.TARGET_ZOOM)
                    .build(),
            )
        }
        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: com.maptec.applied.maps.Style?) {}
                    override fun onStyleRendered(style: com.maptec.applied.maps.Style?) {
                        map.getOverlayEngine().addMarker(
                            MarkerOptions().withLatLng(target).withText("Center"),
                        )
                        paddingReadout = formatPadding(map.getContentPadding())
                        isStyleRendered = true
                    }

                    override fun onFailed(style: com.maptec.applied.maps.Style?, message: String) {}
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    fun syncFromMap(map: MaptecMap) {
        val pad = map.getContentPadding()
        paddingReadout = formatPadding(pad)
        appliedLeft = pad[0].toFloat()
        appliedTop = pad[1].toFloat()
        appliedRight = pad[2].toFloat()
        appliedBottom = pad[3].toFloat()
    }

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                Text(
                    text = stringResource(R.string.content_padding_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.content_padding_readout, paddingReadout),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag(ContentPaddingDemo.TAG_READOUT),
                )

                PaddingSliderRow(
                    label = stringResource(R.string.content_padding_left),
                    value = leftPx,
                    onValueChange = { leftPx = it },
                    testTag = ContentPaddingDemo.TAG_LEFT_SLIDER,
                )
                PaddingSliderRow(
                    label = stringResource(R.string.content_padding_top),
                    value = topPx,
                    onValueChange = { topPx = it },
                    testTag = ContentPaddingDemo.TAG_TOP_SLIDER,
                )
                PaddingSliderRow(
                    label = stringResource(R.string.content_padding_right),
                    value = rightPx,
                    onValueChange = { rightPx = it },
                    testTag = ContentPaddingDemo.TAG_RIGHT_SLIDER,
                )
                PaddingSliderRow(
                    label = stringResource(R.string.content_padding_bottom),
                    value = bottomPx,
                    onValueChange = { bottomPx = it },
                    testTag = ContentPaddingDemo.TAG_BOTTOM_SLIDER,
                )

                ContentPaddingActionButton(
                    text = stringResource(R.string.content_padding_apply),
                    testTag = ContentPaddingDemo.TAG_APPLY_BUTTON,
                    onClick = {
                        mapView.getMapAsync { map ->
                            map.setContentPadding(
                                leftPx.toDouble(),
                                topPx.toDouble(),
                                rightPx.toDouble(),
                                bottomPx.toDouble(),
                            )
                            // 业务侧显式移动相机后，目标点会落在 padding 后的有效区域中心
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    target,
                                    ContentPaddingDemo.TARGET_ZOOM,
                                ),
                            )
                            syncFromMap(map)
                        }
                    },
                )
                ContentPaddingActionButton(
                    text = stringResource(R.string.content_padding_clear),
                    testTag = ContentPaddingDemo.TAG_CLEAR_BUTTON,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    onClick = {
                        leftPx = 0f
                        topPx = 0f
                        rightPx = 0f
                        bottomPx = 0f
                        mapView.getMapAsync { map ->
                            map.setContentPadding(0.0, 0.0, 0.0, 0.0)
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    target,
                                    ContentPaddingDemo.TARGET_ZOOM,
                                ),
                            )
                            syncFromMap(map)
                        }
                    },
                )
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("mapView"),
                )
                ContentPaddingOverlays(
                    leftPx = appliedLeft,
                    topPx = appliedTop,
                    rightPx = appliedRight,
                    bottomPx = appliedBottom,
                    density = density,
                )
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun PaddingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    testTag: String,
) {
    Text(
        text = stringResource(R.string.content_padding_slider_label, label, value.toInt()),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    DemoPanelSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..ContentPaddingDemo.SLIDER_MAX_PX,
        steps = (ContentPaddingDemo.SLIDER_MAX_PX / 10f).toInt() - 1,
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun ContentPaddingOverlays(
    leftPx: Float,
    topPx: Float,
    rightPx: Float,
    bottomPx: Float,
    density: Density,
) {
    val overlay = Color(0x662196F3)
    Box(modifier = Modifier.fillMaxSize()) {
        with(density) {
            if (topPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topPx.toDp())
                        .align(Alignment.TopCenter)
                        .background(overlay)
                        .testTag(ContentPaddingDemo.TAG_OVERLAY_TOP),
                )
            }
            if (bottomPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomPx.toDp())
                        .align(Alignment.BottomCenter)
                        .background(overlay)
                        .testTag(ContentPaddingDemo.TAG_OVERLAY_BOTTOM),
                )
            }
            if (leftPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(leftPx.toDp())
                        .align(Alignment.CenterStart)
                        .background(overlay)
                        .testTag(ContentPaddingDemo.TAG_OVERLAY_LEFT),
                )
            }
            if (rightPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(rightPx.toDp())
                        .align(Alignment.CenterEnd)
                        .background(overlay)
                        .testTag(ContentPaddingDemo.TAG_OVERLAY_RIGHT),
                )
            }
        }
    }
}

@Composable
private fun ContentPaddingActionButton(
    text: String,
    testTag: String,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
) {
    DemoPanelButton(
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(48.dp)
            .testTag(testTag),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatPadding(padding: DoubleArray): String {
    return padding.joinToString(prefix = "[", postfix = "]") { String.format("%.1f", it) }
}

/** ContentPaddingScreen 演示常量（测试与 UI 共用）。 */
object ContentPaddingDemo {
    const val TAG_READOUT = "content_padding_readout"
    const val TAG_LEFT_SLIDER = "content_padding_left_slider"
    const val TAG_TOP_SLIDER = "content_padding_top_slider"
    const val TAG_RIGHT_SLIDER = "content_padding_right_slider"
    const val TAG_BOTTOM_SLIDER = "content_padding_bottom_slider"
    const val TAG_APPLY_BUTTON = "content_padding_apply_button"
    const val TAG_CLEAR_BUTTON = "content_padding_clear_button"
    const val TAG_OVERLAY_LEFT = "content_padding_overlay_left"
    const val TAG_OVERLAY_TOP = "content_padding_overlay_top"
    const val TAG_OVERLAY_RIGHT = "content_padding_overlay_right"
    const val TAG_OVERLAY_BOTTOM = "content_padding_overlay_bottom"

    val TARGET = LatLng(1.3521, 103.8198)
    const val TARGET_ZOOM = 13.0
    const val SLIDER_MAX_PX = 400f
}
