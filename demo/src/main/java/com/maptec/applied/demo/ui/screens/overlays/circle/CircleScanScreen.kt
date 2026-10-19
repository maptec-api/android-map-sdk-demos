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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.maps.animation.Animation
import com.maptec.applied.maps.animation.AnimationStatus
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScanScreen() {
    val state = rememberBasicCircleState()
    val context = LocalContext.current
    val scanBaseImageBitmap = remember(context) {
        loadDrawableAsBitmap(context, R.drawable.circle_roate)
    }

    var scanSectorAngle by remember { mutableStateOf("60") }
    var scanSectorColor by remember { mutableStateOf("#E60012") }
    var scanSpeed by remember { mutableStateOf("3000") }
    var scanIconId by remember { mutableStateOf("scan_base_image") }

    val scanSectorAngleError = validateScanAngle(scanSectorAngle)
    val scanSectorColorError = validateColor(scanSectorColor)
    val scanSpeedError = validateAnimationLoop(scanSpeed)

    var lastCircle by remember { mutableStateOf<Circle?>(null) }
    var lastEngine by remember { mutableStateOf<MapOverlayEngine?>(null) }
    var animationHandle by remember { mutableStateOf<Animation?>(null) }

    fun isAnimating(): Boolean = animationHandle?.let {
        it.status != AnimationStatus.CANCELLED && it.status != AnimationStatus.COMPLETED
    } == true

    fun buildScanOptions(): CircleOptions {
        val options = state.applyTo(CircleOptions())
        return applyScanStyleToCircleOptions(
            options = options,
            scanEnabled = true,
            fillColor = state.color,
            scanSectorColorInput = scanSectorColor,
            scanSectorAngleInput = scanSectorAngle,
            scanBaseImageBitmap = scanBaseImageBitmap,
            scanBaseImageId = scanIconId.trim().ifEmpty { "scan_base_image" },
        )
    }

    fun applyBasicProps(circle: Circle) {
        if (!state.isValid) return
        parseCircleLatLng(state.latLng)?.let { runCatching { circle.setCenter(it) } }
        state.radius.toDoubleOrNull()?.let { runCatching { circle.setRadius(it) } }
        runCatching { circle.setFillColor(state.color) }
        state.opacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setFillOpacity(it) } }
        runCatching { circle.setStrokeColor(state.strokeColor) }
        state.strokeWidth.toFloatOrNull()?.let { runCatching { circle.setStrokeWeight(it) } }
        state.strokeOpacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setStrokeOpacity(it) } }
    }

    fun startScanAnimation(circle: Circle) {
        if (scanSpeedError != null) return
        animationHandle?.cancel()
        animationHandle = buildScanRotationAnimation(scanSpeed)?.let { circle.setAnimation(it) }
        animationHandle?.start()
    }

    fun rebuildScanCircle() {
        val engine = lastEngine ?: return
        if (!state.isValid || scanSectorAngleError != null || scanSectorColorError != null) return
        val keepAnimating = isAnimating()
        animationHandle?.cancel()
        lastCircle?.let { engine.deleteCircle(it) }
        val circle = engine.addCircle(buildScanOptions())
        attachDebugListeners(circle, context)
        lastCircle = circle
        if (keepAnimating) {
            startScanAnimation(circle)
        }
    }

    CircleScaffold(
        defaultOptions = { buildScanOptions() },
        onDefaultCircleAdded = { circle, engine ->
            lastCircle = circle
            lastEngine = engine
            animationHandle?.cancel()
            animationHandle = buildScanRotationAnimation(scanSpeed)?.let { circle.setAnimation(it) }
            animationHandle?.start()
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
            lastCircle?.let { applyBasicProps(it) }
        }

        LaunchedEffect(scanSectorAngle, scanSectorColor, scanIconId) {
            if (lastCircle != null) {
                rebuildScanCircle()
            }
        }

        LaunchedEffect(scanSpeed) {
            val circle = lastCircle ?: return@LaunchedEffect
            if (isAnimating()) {
                startScanAnimation(circle)
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
                label = "${stringResource(R.string.circle_scan_sector_angle)}: $scanSectorAngle",
                value = scanSectorAngle.toFloatOrNull()?.coerceIn(0f, 90f) ?: 60f,
                valueText = scanSectorAngle,
                onValueChange = { scanSectorAngle = formatDemoSliderValue(it) },
                onValueTextChange = { scanSectorAngle = it },
                valueRange = 0f..90f,
                rangeStartLabel = "0",
                rangeEndLabel = "90",
                isError = scanSectorAngleError != null,
                errorText = scanSectorAngleError,
                testTag = "circle_input_scan_sector_angle",
            )

            ColorPickerField(
                value = scanSectorColor,
                onValueChange = { scanSectorColor = it },
                label = stringResource(R.string.circle_scan_sector_color),
                supportingText = scanSectorColorError?.let { { Text(it, color = Color.Red) } },
                isError = scanSectorColorError != null,
                modifier = Modifier.fillMaxWidth(),
                testTag = "circle_input_scan_sector_color",
            )

            DemoNumericSliderField(
                label = "${stringResource(R.string.circle_scan_rotation_period)}: $scanSpeed",
                value = scanSpeed.toFloatOrNull()?.coerceIn(500f, 10000f) ?: 3000f,
                valueText = scanSpeed,
                onValueChange = { scanSpeed = it.toLong().toString() },
                onValueTextChange = { scanSpeed = it },
                valueRange = 500f..10000f,
                rangeStartLabel = "500",
                rangeEndLabel = "10000",
                isError = scanSpeedError != null,
                errorText = scanSpeedError,
                testTag = "circle_input_scan_speed",
            )

            OutlinedTextField(
                value = scanIconId,
                onValueChange = { scanIconId = it },
                label = { Text(stringResource(R.string.circle_scan_icon_url_id)) },
                supportingText = {
                    Text(
                        stringResource(R.string.circle_scan_icon_url_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("circle_input_scan_icon_id"),
            )

            DrawCircleButton(
                state = state,
                mapView = mapView,
                buildOptions = { buildScanOptions() },
                onCircleAdded = { circle, engine ->
                    animationHandle?.cancel()
                    lastCircle = circle
                    lastEngine = engine
                    animationHandle = buildScanRotationAnimation(scanSpeed)?.let { circle.setAnimation(it) }
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
