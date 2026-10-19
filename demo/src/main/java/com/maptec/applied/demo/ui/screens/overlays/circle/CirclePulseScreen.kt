package com.maptec.applied.demo.ui.screens.overlays.circle

import android.os.Handler
import android.os.Looper
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

/** 中心锚点半径相对动画最小半径的比例（小圆点，小于涟漪起始半径） */
private const val CENTER_ANCHOR_RADIUS_RATIO = 0.35f

private fun centerAnchorRadius(min: Double): Double =
    (min * CENTER_ANCHOR_RADIUS_RATIO).coerceIn(8.0, (min - 2.0).coerceAtLeast(8.0))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclePulseScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current
    val pulseHandler = remember { Handler(Looper.getMainLooper()) }

    var rangeMin by remember { mutableStateOf("30") }
    var rangeMax by remember { mutableStateOf("150") }
    var pulseDuration by remember { mutableStateOf("2000") }
    val pulseDurationError = validateAnimationLoop(pulseDuration)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var lastEngine by remember { mutableStateOf<MapOverlayEngine?>(null) }
    val activeRipples = remember { mutableStateListOf<Circle>() }
    var pulseEmitterRunnable by remember { mutableStateOf<Runnable?>(null) }
    var pulseRunning by remember { mutableStateOf(false) }

    fun isAnimating(): Boolean = pulseRunning

    fun clearActiveRipples() {
        lastEngine?.let { engine ->
            activeRipples.forEach { ripple -> runCatching { engine.deleteCircle(ripple) } }
        }
        activeRipples.clear()
    }

    fun stopAnimation(deleteRipples: Boolean) {
        pulseRunning = false
        pulseEmitterRunnable?.let { pulseHandler.removeCallbacks(it) }
        pulseEmitterRunnable = null
        if (deleteRipples) {
            clearActiveRipples()
        }
    }

    fun applyCenterAnchorProps(circle: Circle, min: Double) {
        if (!state.isValid) return
        val anchorRadius = centerAnchorRadius(min)
        parseCircleLatLng(state.latLng)?.let { runCatching { circle.setCenter(it) } }
        runCatching { circle.setRadius(anchorRadius) }
        runCatching { circle.setFillColor(state.color) }
        state.opacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setFillOpacity(it) } }
        runCatching { circle.setStrokeColor(state.strokeColor) }
        state.strokeWidth.toFloatOrNull()?.let { runCatching { circle.setStrokeWeight(it) } }
        state.strokeOpacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setStrokeOpacity(it) } }
    }

    fun emitRippleWave(min: Double, max: Double, expandMs: Long) {
        val engine = lastEngine ?: return
        val anchor = lastCircle ?: return
        if (!pulseRunning) return

        val center = anchor.getCenter()
        val strokeW = state.strokeWidth.toFloatOrNull() ?: 2f
        val strokeOp = state.strokeOpacity.toFloatOrNull() ?: 1f

        val ripple = engine.addCircle(
            pulseLayerCircleOptions(
                center = center,
                startRadius = min,
                fillColor = state.color,
                fillOpacity = 0.15f,
                strokeColor = state.strokeColor,
                strokeWeight = strokeW,
                strokeOpacity = strokeOp,
                geodesic = false,
            ),
        )
        activeRipples.add(ripple)

        val anim = buildPulseAnimation(min, max, expandMs, "pulse-wave") { finished ->
            if (finished && pulseRunning) {
                runCatching { engine.deleteCircle(ripple) }
                activeRipples.remove(ripple)
            }
        }
        ripple.startAnimation(anim, expandMs)
        pulseHandler.postDelayed({
            if (activeRipples.remove(ripple)) {
                runCatching { engine.deleteCircle(ripple) }
            }
        }, expandMs + 200L)
    }

    fun startPulseAnimation(): Boolean {
        val circle = lastCircle ?: return false
        val engine = lastEngine ?: return false
        val min = rangeMin.toDoubleOrNull() ?: return false
        val max = rangeMax.toDoubleOrNull() ?: return false
        if (pulseDurationError != null) return false

        stopAnimation(deleteRipples = true)
        applyCenterAnchorProps(circle, min)

        val expandMs = pulseDuration.toLongOrNull() ?: 2000L
        val emitIntervalMs = expandMs / 2

        pulseRunning = true
        val emitter = object : Runnable {
            override fun run() {
                if (!pulseRunning) return
                emitRippleWave(min, max, expandMs)
                pulseHandler.postDelayed(this, emitIntervalMs)
            }
        }
        pulseEmitterRunnable = emitter
        emitter.run()
        return true
    }

    CircleScaffold(
        defaultOptions = { state.applyTo(CircleOptions()) },
        onDefaultCircleAdded = { circle, engine ->
            lastCircle = circle
            lastEngine = engine
            startPulseAnimation()
        },
    ) { mapView, _ ->
        LaunchedEffect(
            state.latLng,
            state.color,
            state.opacity,
            state.strokeColor,
            state.strokeWidth,
            state.strokeOpacity,
            rangeMin,
            rangeMax,
            pulseDuration,
        ) {
            if (lastCircle != null && lastEngine != null) {
                startPulseAnimation()
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.latLng,
                onValueChange = { state.latLng = it },
                label = { Text(stringResource(R.string.circle_input_latlng_label)) },
                supportingText = state.latLngError?.let { { Text(it, color = Color.Red) } },
                isError = state.latLngError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("circle_input_latlng"),
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_input_radius_pixel)}: ${state.radius}",
                value = state.radius.toFloatOrNull()?.coerceIn(1f, 500f) ?: 120f,
                valueText = state.radius,
                onValueChange = { state.radius = formatDemoSliderValue(it) },
                onValueTextChange = { state.radius = it },
                valueRange = 1f..500f,
                rangeStartLabel = "1",
                rangeEndLabel = "500",
                isError = state.radiusError != null,
                errorText = state.radiusError,
                testTag = "circle_input_radius",
            )

            ColorPickerField(
                value = state.color,
                onValueChange = { state.color = it },
                label = stringResource(R.string.circle_input_fill_color),
                supportingText = state.colorError?.let { { Text(it, color = Color.Red) } },
                isError = state.colorError != null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "circle_input_color",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_input_fill_opacity)}: ${state.opacity}",
                value = state.opacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f,
                valueText = state.opacity,
                onValueChange = { state.opacity = formatDemoSliderValue(it) },
                onValueTextChange = { state.opacity = it },
                valueRange = 0f..1f,
                rangeStartLabel = "0",
                rangeEndLabel = "1",
                isError = state.opacityError != null,
                errorText = state.opacityError,
                testTag = "circle_input_opacity",
            )

            ColorPickerField(
                value = state.strokeColor,
                onValueChange = { state.strokeColor = it },
                label = stringResource(R.string.circle_input_stroke_color),
                supportingText = state.strokeColorError?.let { { Text(it, color = Color.Red) } },
                isError = state.strokeColorError != null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "circle_input_stroke_color",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_input_stroke_width)}: ${state.strokeWidth}",
                value = state.strokeWidth.toFloatOrNull()?.coerceIn(0f, 20f) ?: 2f,
                valueText = state.strokeWidth,
                onValueChange = { state.strokeWidth = formatDemoSliderValue(it) },
                onValueTextChange = { state.strokeWidth = it },
                valueRange = 0f..20f,
                rangeStartLabel = "0",
                rangeEndLabel = "20",
                isError = state.strokeWidthError != null,
                errorText = state.strokeWidthError,
                testTag = "circle_input_stroke_width",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_input_stroke_opacity)}: ${state.strokeOpacity}",
                value = state.strokeOpacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
                valueText = state.strokeOpacity,
                onValueChange = { state.strokeOpacity = formatDemoSliderValue(it) },
                onValueTextChange = { state.strokeOpacity = it },
                valueRange = 0f..1f,
                rangeStartLabel = "0",
                rangeEndLabel = "1",
                isError = state.strokeOpacityError != null,
                errorText = state.strokeOpacityError,
                testTag = "circle_input_stroke_opacity",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_range_min)}: $rangeMin",
                value = rangeMin.toFloatOrNull()?.coerceIn(1f, 500f) ?: 30f,
                valueText = rangeMin,
                onValueChange = { rangeMin = formatDemoSliderValue(it) },
                onValueTextChange = { rangeMin = it },
                valueRange = 1f..500f,
                rangeStartLabel = "1",
                rangeEndLabel = "500",
                testTag = "circle_input_anim_min",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_range_max)}: $rangeMax",
                value = rangeMax.toFloatOrNull()?.coerceIn(1f, 500f) ?: 150f,
                valueText = rangeMax,
                onValueChange = { rangeMax = formatDemoSliderValue(it) },
                onValueTextChange = { rangeMax = it },
                valueRange = 1f..500f,
                rangeStartLabel = "1",
                rangeEndLabel = "500",
                testTag = "circle_input_anim_max",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_radius_duration)}: $pulseDuration",
                value = pulseDuration.toFloatOrNull()?.coerceIn(500f, 10000f) ?: 2000f,
                valueText = pulseDuration,
                onValueChange = { pulseDuration = it.toLong().toString() },
                onValueTextChange = { pulseDuration = it },
                valueRange = 500f..10000f,
                rangeStartLabel = "500",
                rangeEndLabel = "10000",
                isError = pulseDurationError != null,
                errorText = pulseDurationError,
                testTag = "circle_input_anim_duration",
            )

            DrawCircleButton(
                state = state,
                mapView = mapView,
                buildOptions = { state.applyTo(CircleOptions()) },
                onCircleAdded = { circle, engine ->
                    stopAnimation(deleteRipples = true)
                    lastCircle = circle
                    lastEngine = engine
                    startPulseAnimation()
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (lastCircle == null || lastEngine == null) {
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
                        if (!startPulseAnimation()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.circle_toast_draw_first),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .testTag("circle_btn_animation_start"),
                ) { Text(stringResource(R.string.circle_animation_start)) }
                Button(
                    onClick = { stopAnimation(deleteRipples = true) },
                    enabled = isAnimating(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .testTag("circle_btn_animation_stop"),
                ) { Text(stringResource(R.string.circle_animation_stop)) }
            }
        }
    }
}
