package com.maptec.applied.demo.ui.screens.overlays.circle

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.AnimationStatus
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScanScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanSectorAngle by remember { mutableStateOf("60") }
    var scanSectorColor by remember { mutableStateOf("#E60012") }
    var scanSpeed by remember { mutableStateOf("3000") }
    var scanBaseImageId by remember { mutableStateOf<String?>(null) }
    val scanSectorAngleError = validateScanAngle(scanSectorAngle)
    val scanSectorColorError = validateColor(scanSectorColor)
    val scanSpeedError = validateAnimationLoop(scanSpeed)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var animationHandle by remember { mutableStateOf<Animation?>(null) }

    CircleScaffold { mapView, mapStyle, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)

            OutlinedTextField(
                value = scanSectorAngle,
                onValueChange = { scanSectorAngle = it },
                label = { Text(stringResource(R.string.circle_scan_sector_angle)) },
                isError = scanSectorAngleError != null,
                supportingText = scanSectorAngleError?.let { { Text(it, color = Color.Red) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("circle_input_scan_sector_angle"),
            )
            OutlinedTextField(
                value = scanSectorColor,
                onValueChange = { scanSectorColor = it },
                label = { Text(stringResource(R.string.circle_scan_sector_color)) },
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
                label = { Text(stringResource(R.string.circle_scan_rotation_period)) },
                isError = scanSpeedError != null,
                supportingText = scanSpeedError?.let { { Text(it, color = Color.Red) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("circle_input_scan_speed"),
            )

            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = {
                    // 先添加图片再绘制圆
                    val iconId = "scan_base_image"
                    if (mapStyle != null) {
                        try {
                            if (mapStyle.getImage(iconId) == null) {
                                addImage(context, R.drawable.circle_roate, iconId, mapStyle)
                            }
                            scanBaseImageId = iconId
                        } catch (e: Exception) {
                            // 图片添加失败也继续绘制圆
                        }
                    }

                    val options = state.applyTo(CircleOptions())
                    applyScanStyleToCircleOptions(
                        options = options,
                        scanEnabled = true,
                        fillColor = state.color,
                        scanSectorColorInput = scanSectorColor,
                        scanSectorAngleInput = scanSectorAngle,
                        scanBaseImageId = scanBaseImageId,
                    )
                },
                onCircleAdded = { circle, _ ->
                    animationHandle?.cancel()
                    lastCircle = circle
                    animationHandle = buildScanRotationAnimation(scanSpeed)?.let { circle.setAnimation(it) }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Button(
                    onClick = {
                        val circle = lastCircle
                        if (circle == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_draw_first),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@Button
                        }
                        if (scanSpeedError != null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_invalid_scan_speed),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@Button
                        }
                        if (scanSectorAngleError != null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_invalid_scan_angle),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@Button
                        }
                        val anim = buildScanRotationAnimation(scanSpeed) ?: return@Button
                        animationHandle?.cancel()
                        animationHandle = circle.setAnimation(anim)
                        animationHandle?.start()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .testTag("circle_btn_animation_start"),
                ) { Text(stringResource(R.string.circle_animation_start)) }
                Button(
                    onClick = { animationHandle?.cancel() },
                    enabled = animationHandle?.let {
                        it.status != AnimationStatus.CANCELLED && it.status != AnimationStatus.COMPLETED
                    } == true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .testTag("circle_btn_animation_stop"),
                ) { Text(stringResource(R.string.circle_animation_stop)) }
            }
        }
    }
}
