package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleBasicScreen() {
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

    var lastCircle by remember { mutableStateOf<Circle?>(null) }

    fun applyPropsToCircle(circle: Circle) {
        if (!state.isValid) return
        parseCircleLatLng(state.latLng)?.let { runCatching { circle.setCenter(it) } }
        state.radius.toDoubleOrNull()?.let { runCatching { circle.setRadius(it) } }
        runCatching { circle.setFillColor(state.color) }
        state.opacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setFillOpacity(it) } }
        runCatching { circle.setStrokeColor(state.strokeColor) }
        state.strokeWidth.toFloatOrNull()?.let { runCatching { circle.setStrokeWeight(it) } }
        state.strokeOpacity.toFloatOrNull()?.takeIf { it in 0f..1f }?.let { runCatching { circle.setStrokeOpacity(it) } }
    }

    CircleScaffold(
        defaultOptions = { state.applyTo(CircleOptions()) },
        onDefaultCircleAdded = { circle, _ ->
            lastCircle = circle
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
            lastCircle?.let { applyPropsToCircle(it) }
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
                value = state.radius.toFloatOrNull()?.coerceIn(1f, 1000f) ?: 120f,
                valueText = state.radius,
                onValueChange = { state.radius = formatDemoSliderValue(it) },
                onValueTextChange = { state.radius = it },
                valueRange = 1f..1000f,
                rangeStartLabel = "1",
                rangeEndLabel = "1000",
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
                value = state.strokeOpacity.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.8f,
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

            DrawCircleButton(
                state = state,
                mapView = mapView,
                buildOptions = { state.applyTo(CircleOptions()) },
                onCircleAdded = { circle, _ ->
                    lastCircle = circle
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
