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
import com.maptec.applied.maps.animation.RadiusAnimation
import com.maptec.applied.maps.animation.RepeatMode
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleRadiusBreathScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current

    var rangeMin by remember { mutableStateOf("50") }
    var rangeMax by remember { mutableStateOf("150") }
    var duration by remember { mutableStateOf("2000") }
    val durationError = validateAnimationLoop(duration)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var animationHandle by remember { mutableStateOf<Animation?>(null) }

    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                OutlinedTextField(
                    value = rangeMin,
                    onValueChange = { rangeMin = it },
                    label = { Text(stringResource(R.string.circle_radius_range_min)) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("circle_input_anim_min"),
                )
                OutlinedTextField(
                    value = rangeMax,
                    onValueChange = { rangeMax = it },
                    label = { Text(stringResource(R.string.circle_radius_range_max)) },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("circle_input_anim_max"),
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text(stringResource(R.string.circle_radius_duration)) },
                    isError = durationError != null,
                    supportingText = durationError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("circle_input_anim_duration"),
                )
            }

            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = { state.applyTo(CircleOptions()) },
                onCircleAdded = { circle, _ ->
                    animationHandle?.cancel()
                    lastCircle = circle
                    animationHandle = null
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
                        val min = rangeMin.toDoubleOrNull()
                        val max = rangeMax.toDoubleOrNull()
                        if (min == null || max == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_invalid_range),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@Button
                        }
                        if (durationError != null) {
                            Toast.makeText(context, durationError, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val anim = RadiusAnimation(min, max).apply {
                            setDurationMs(duration.toLongOrNull() ?: 2000L)
                            setRepeatCount(-1)
                            setRepeatMode(RepeatMode.REVERSE)
                            setListener(circleAnimationCallbacks("radius"))
                        }
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
