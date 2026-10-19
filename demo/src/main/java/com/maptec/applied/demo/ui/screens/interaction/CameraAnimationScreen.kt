package com.maptec.applied.demo.ui.screens.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

/**
 * Demo：对比 [MaptecMap.moveCamera]、[MaptecMap.easeCamera]、[MaptecMap.animateCamera]。
 *
 * 三者使用同一目标 [CameraUpdate]，最终视角一致；差异在过渡方式
 *（立即跳转 / 普通缓动 / 飞行动画）。「回到原位置」用 [MaptecMap.moveCamera] 复位。
 */
@Composable
fun CameraAnimationScreen(
    modifier: Modifier = Modifier,
) {
    var isStyleRendered by remember { mutableStateOf(false) }
    val idleStatus = stringResource(R.string.camera_anim_status_idle)
    val finishStatus = stringResource(R.string.camera_anim_callback_on_finish)
    val cancelStatus = stringResource(R.string.camera_anim_callback_on_cancel)
    val statusState = remember { mutableStateOf(idleStatus) }
    var callbackStatus by statusState

    val origin = remember {
        CameraPosition.Builder()
            .target(CameraAnimationDemo.ORIGIN)
            .zoom(CameraAnimationDemo.ORIGIN_ZOOM)
            .build()
    }
    val targetUpdate = remember {
        CameraUpdateFactory.newLatLngZoom(
            CameraAnimationDemo.TARGET,
            CameraAnimationDemo.TARGET_ZOOM,
        )
    }
    val originUpdate = remember {
        CameraUpdateFactory.newCameraPosition(origin)
    }
    val animationCallback = remember(statusState, finishStatus, cancelStatus) {
        object : MaptecMap.CancelableCallback {
            override fun onFinish() {
                statusState.value = finishStatus
            }

            override fun onCancel() {
                statusState.value = cancelStatus
            }
        }
    }

    val statusRunningMove = stringResource(R.string.camera_anim_status_running_move)
    val statusRunningEase = stringResource(R.string.camera_anim_status_running_ease)
    val statusRunningFly = stringResource(R.string.camera_anim_status_running_fly)
    val statusReset = stringResource(R.string.camera_anim_status_reset)

    val context = LocalContext.current
    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(origin)
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
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
                Text(
                    text = stringResource(R.string.camera_anim_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.camera_anim_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.camera_anim_callback_status, callbackStatus),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag(CameraAnimationDemo.TAG_CALLBACK_STATUS),
                )

                CameraAnimActionButton(
                    text = stringResource(R.string.camera_anim_move),
                    testTag = CameraAnimationDemo.TAG_MOVE_BUTTON,
                    onClick = {
                        runCameraAction(mapView, statusRunningMove, statusState) { map ->
                            map.moveCamera(targetUpdate, animationCallback)
                        }
                    },
                )
                CameraAnimActionButton(
                    text = stringResource(R.string.camera_anim_ease),
                    testTag = CameraAnimationDemo.TAG_EASE_BUTTON,
                    onClick = {
                        runCameraAction(mapView, statusRunningEase, statusState) { map ->
                            map.easeCamera(
                                targetUpdate,
                                CameraAnimationDemo.ANIMATION_DURATION_MS,
                                animationCallback,
                            )
                        }
                    },
                )
                CameraAnimActionButton(
                    text = stringResource(R.string.camera_anim_fly),
                    testTag = CameraAnimationDemo.TAG_FLY_BUTTON,
                    onClick = {
                        runCameraAction(mapView, statusRunningFly, statusState) { map ->
                            map.animateCamera(
                                targetUpdate,
                                CameraAnimationDemo.ANIMATION_DURATION_MS,
                                animationCallback,
                            )
                        }
                    },
                )
                CameraAnimActionButton(
                    text = stringResource(R.string.camera_anim_reset),
                    testTag = CameraAnimationDemo.TAG_RESET_BUTTON,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    onClick = {
                        runCameraAction(mapView, statusReset, statusState) { map ->
                            map.moveCamera(originUpdate, animationCallback)
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
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
        modifier = modifier,
    )
}

private fun runCameraAction(
    mapView: MapView,
    runningStatus: String,
    statusState: MutableState<String>,
    action: (MaptecMap) -> Unit,
) {
    statusState.value = runningStatus
    mapView.getMapAsync(action)
}

@Composable
private fun CameraAnimActionButton(
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

/** CameraAnimationScreen 演示常量（测试与 UI 共用）。 */
object CameraAnimationDemo {
    const val TAG_CALLBACK_STATUS = "camera_anim_callback_status"
    const val TAG_MOVE_BUTTON = "camera_anim_move_button"
    const val TAG_EASE_BUTTON = "camera_anim_ease_button"
    const val TAG_FLY_BUTTON = "camera_anim_fly_button"
    const val TAG_RESET_BUTTON = "camera_anim_reset_button"

    /** 新加坡市区（起始视角）。 */
    val ORIGIN = LatLng(1.3521, 103.8198)
    const val ORIGIN_ZOOM = 12.0

    /** 吉隆坡市中心（跨城目标，便于观察 flyTo 与 easeTo 差异）。 */
    val TARGET = LatLng(3.1390, 101.6869)
    const val TARGET_ZOOM = 12.0

    const val ANIMATION_DURATION_MS = 1500
}
