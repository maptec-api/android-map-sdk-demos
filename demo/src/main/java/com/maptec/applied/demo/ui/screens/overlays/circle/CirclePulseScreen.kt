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
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

private const val PULSE_OUTER_DELAY_MS = 0L
private const val PULSE_INNER_DELAY_MS = 500L
private const val PULSE_MIDDLE_DELAY_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclePulseScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current

    var rangeMin by remember { mutableStateOf("50") }
    var rangeMax by remember { mutableStateOf("150") }
    var pulseDuration by remember { mutableStateOf("2000") }
    val pulseDurationError = validateAnimationLoop(pulseDuration)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var lastEngine by remember { mutableStateOf<MapOverlayEngine?>(null) }
    var rippleCircle by remember { mutableStateOf<Circle?>(null) }
    var middleCircle by remember { mutableStateOf<Circle?>(null) }
    var innerHandle by remember { mutableStateOf<Animation?>(null) }
    var rippleHandle by remember { mutableStateOf<Animation?>(null) }
    var middleHandle by remember { mutableStateOf<Animation?>(null) }

    fun stopAnimation(deleteLayers: Boolean) {
        innerHandle?.let { lastEngine?.cancelAnimation(it) }
        rippleHandle?.let { lastEngine?.cancelAnimation(it) }
        middleHandle?.let { lastEngine?.cancelAnimation(it) }
        if (deleteLayers) {
            lastEngine?.let { engine ->
                rippleCircle?.let { engine.deleteCircle(it) }
                middleCircle?.let { engine.deleteCircle(it) }
            }
            rippleCircle = null
            middleCircle = null
        }
        innerHandle = null
        rippleHandle = null
        middleHandle = null
    }

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
                    value = pulseDuration,
                    onValueChange = { pulseDuration = it },
                    label = { Text(stringResource(R.string.circle_radius_duration)) },
                    isError = pulseDurationError != null,
                    supportingText = pulseDurationError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("circle_input_anim_duration"),
                )
            }

            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = { state.applyTo(CircleOptions()) },
                onCircleAdded = { circle, engine ->
                    stopAnimation(deleteLayers = true)
                    lastCircle = circle
                    lastEngine = engine
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Button(
                    onClick = {
                        val circle = lastCircle
                        val engine = lastEngine
                        if (circle == null || engine == null) {
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
                        if (pulseDurationError != null) {
                            Toast.makeText(context, pulseDurationError, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        stopAnimation(deleteLayers = true)

                        val center = circle.getCenter()
                        val strokeW = state.strokeWidth.toFloatOrNull() ?: 2f
                        val strokeOp = state.strokeOpacity.toFloatOrNull() ?: 1f

                        rippleCircle = engine.addCircle(
                            pulseLayerCircleOptions(
                                center = center,
                                startRadius = min,
                                fillColor = state.color,
                                fillOpacity = 0.15f,
                                strokeColor = state.strokeColor,
                                strokeWeight = strokeW,
                                strokeOpacity = strokeOp,
                                geodesic = false,
                            )
                        )
                        middleCircle = engine.addCircle(
                            pulseLayerCircleOptions(
                                center = center,
                                startRadius = min,
                                fillColor = state.color,
                                fillOpacity = 0.3f,
                                strokeColor = state.strokeColor,
                                strokeWeight = strokeW,
                                strokeOpacity = strokeOp,
                                geodesic = false,
                            )
                        )

                        val durationMs = pulseDuration.toLongOrNull() ?: 2000L
                        val outer = buildPulseAnimation(min, max, durationMs, "pulse-outer")
                            .apply { setDelayMs(PULSE_OUTER_DELAY_MS) }
                        val inner = buildPulseAnimation(min, max, durationMs, "pulse-inner")
                            .apply { setDelayMs(PULSE_INNER_DELAY_MS) }
                        val middle = buildPulseAnimation(min, max, durationMs, "pulse-middle")
                            .apply { setDelayMs(PULSE_MIDDLE_DELAY_MS) }

                        rippleHandle = rippleCircle?.startAnimation(outer)
                        innerHandle = circle.startAnimation(inner)
                        middleHandle = middleCircle?.startAnimation(middle)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .testTag("circle_btn_animation_start"),
                ) { Text(stringResource(R.string.circle_animation_start)) }
                Button(
                    onClick = { stopAnimation(deleteLayers = true) },
                    enabled = sequenceOf(innerHandle, rippleHandle, middleHandle).any { handle ->
                        handle != null &&
                            handle.status != AnimationStatus.CANCELLED &&
                            handle.status != AnimationStatus.COMPLETED
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .testTag("circle_btn_animation_stop"),
                ) { Text(stringResource(R.string.circle_animation_stop)) }
            }
        }
    }
}
