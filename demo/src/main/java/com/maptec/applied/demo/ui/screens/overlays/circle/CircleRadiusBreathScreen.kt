package com.maptec.applied.demo.ui.screens.overlays.circle

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.AnimationStatus
import com.maptec.applied.maps.animation.RadiusAnimation
import com.maptec.applied.maps.animation.RepeatMode
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

private fun validateBreathDuration(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "请输入动画周期"
    val value = trimmed.toLongOrNull() ?: return "请输入有效数字"
    return if (value in 500L..3000L) null else "动画周期应在 500～3000 之间"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleRadiusBreathScreen() {
    val state = remember {
        BasicCircleState().apply {
            latLng = "1.4, 103.75"
            color = "#3F7BF9"
            radius = "120"
            opacity = "0.5"
            strokeColor = "#FFFFFF"
            strokeWidth = "2"
            strokeOpacity = "0.8"
        }
    }
    val context = LocalContext.current

    var rangeMin by remember { mutableStateOf("50") }
    var rangeMax by remember { mutableStateOf("150") }
    var duration by remember { mutableStateOf("2000") }
    val durationError = validateBreathDuration(duration)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var animationHandle by remember { mutableStateOf<Animation?>(null) }

    fun restartBreathAnimation(circle: Circle) {
        val min = rangeMin.toDoubleOrNull()
        val max = rangeMax.toDoubleOrNull()
        if (min == null || max == null || durationError != null) return

        animationHandle?.cancel()
        val anim = RadiusAnimation(min, max).apply {
            setDurationMs(duration.toLongOrNull() ?: 2000L)
            setRepeatCount(-1)
            setRepeatMode(RepeatMode.REVERSE)
            setListener(circleAnimationCallbacks("radius"))
        }
        animationHandle = circle.setAnimation(anim)
        animationHandle?.start()
    }

    CircleScaffold(
        defaultOptions = { state.applyTo(CircleOptions()) },
        onDefaultCircleAdded = { circle, _ ->
            lastCircle = circle
            restartBreathAnimation(circle)
        },
    ) { mapView, _ ->
        LaunchedEffect(
            state.latLng,
            state.color,
            state.radius,
            state.opacity,
            state.strokeColor,
            state.strokeWidth,
            state.strokeOpacity,
        ) {
            lastCircle?.let { circle ->
                applyBasicCircleProps(circle, state)
                mapView?.getMapAsync { it.triggerRepaint() }
            }
        }

        LaunchedEffect(rangeMin, rangeMax, duration) {
            lastCircle?.let { restartBreathAnimation(it) }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleCommonSliders(state)

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_range_min)}: $rangeMin",
                value = rangeMin.toFloatOrNull()?.coerceIn(1f, 100f) ?: 50f,
                valueText = rangeMin,
                onValueChange = { rangeMin = formatDemoSliderValue(it) },
                onValueTextChange = { rangeMin = it },
                valueRange = 1f..100f,
                rangeStartLabel = "1",
                rangeEndLabel = "100",
                testTag = "circle_input_anim_min",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_range_max)}: $rangeMax",
                value = rangeMax.toFloatOrNull()?.coerceIn(1f, 200f) ?: 150f,
                valueText = rangeMax,
                onValueChange = { rangeMax = formatDemoSliderValue(it) },
                onValueTextChange = { rangeMax = it },
                valueRange = 1f..200f,
                rangeStartLabel = "1",
                rangeEndLabel = "200",
                testTag = "circle_input_anim_max",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_duration)}: $duration",
                value = duration.toFloatOrNull()?.coerceIn(500f, 3000f) ?: 2000f,
                valueText = duration,
                onValueChange = { duration = it.toLong().toString() },
                onValueTextChange = { duration = it },
                valueRange = 500f..3000f,
                rangeStartLabel = "500",
                rangeEndLabel = "3000",
                isError = durationError != null,
                errorText = durationError,
                testTag = "circle_input_anim_duration",
            )

            DrawCircleButton(
                state = state,
                mapView = mapView,
                buildOptions = { state.applyTo(CircleOptions()) },
                onCircleAdded = { circle, _ ->
                    lastCircle = circle
                    restartBreathAnimation(circle)
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
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
                        if (rangeMin.toDoubleOrNull() == null || rangeMax.toDoubleOrNull() == null) {
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
                        restartBreathAnimation(circle)
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
