package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun CircleOuterGlowScreen() {
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
    var outerGlow by remember { mutableStateOf(true) }
    var glowColor by remember { mutableStateOf("#0066FF") }
    var glowRadius by remember { mutableStateOf("10") }

    var lastCircle by remember { mutableStateOf<Circle?>(null) }

    fun applyGlowProps(circle: Circle) {
        applyBasicCircleProps(circle, state)
        runCatching { circle.setOuterGlow(outerGlow) }
        if (outerGlow) {
            runCatching { circle.setGlowColor(glowColor) }
            glowRadius.toFloatOrNull()?.takeIf { it in 1f..30f }?.let { runCatching { circle.setGlowRadius(it) } }
        }
    }

    CircleScaffold(
        defaultOptions = {
            state.applyTo(CircleOptions())
                .withOuterGlow(outerGlow)
                .withGlowColor(glowColor)
                .withGlowRadius(glowRadius.toFloatOrNull() ?: 10f)
        },
        onDefaultCircleAdded = { circle, _ -> lastCircle = circle },
    ) { mapView, _ ->
        LaunchedEffect(
            state.latLng,
            state.color,
            state.radius,
            state.opacity,
            state.strokeColor,
            state.strokeWidth,
            state.strokeOpacity,
            outerGlow,
            glowColor,
            glowRadius,
        ) {
            lastCircle?.let { circle ->
                applyGlowProps(circle)
                mapView?.getMapAsync { it.triggerRepaint() }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleCommonSliders(state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.circle_switch_outer_glow), modifier = Modifier.weight(1f))
                Switch(
                    checked = outerGlow,
                    onCheckedChange = { outerGlow = it },
                    modifier = Modifier.testTag("circle_switch_outer_glow"),
                )
            }

            if (outerGlow) {
                ColorPickerField(
                    value = glowColor,
                    onValueChange = { glowColor = it },
                    label = stringResource(R.string.circle_glow_color),
                    supportingText = validateColor(glowColor)?.let { { Text(it) } },
                    isError = validateColor(glowColor) != null,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "circle_input_glow_color",
                )
                DemoNumericSliderField(
                    label = "${stringResource(R.string.circle_glow_radius)}: $glowRadius",
                    value = glowRadius.toFloatOrNull()?.coerceIn(1f, 30f) ?: 10f,
                    valueText = glowRadius,
                    onValueChange = { glowRadius = formatDemoSliderValue(it) },
                    onValueTextChange = { glowRadius = it },
                    valueRange = 1f..30f,
                    rangeStartLabel = "1",
                    rangeEndLabel = "30",
                    isError = validateGlowRadius(glowRadius) != null,
                    errorText = validateGlowRadius(glowRadius),
                    testTag = "circle_input_glow_radius",
                )
            }

            DrawCircleButton(
                state = state,
                mapView = mapView,
                enabled = state.isValid && (!outerGlow || (validateColor(glowColor) == null && validateGlowRadius(glowRadius) == null)),
                buildOptions = {
                    val options = state.applyTo(CircleOptions()).withOuterGlow(outerGlow)
                    if (outerGlow) {
                        options
                            .withGlowColor(glowColor)
                            .withGlowRadius(glowRadius.toFloatOrNull() ?: 10f)
                    } else {
                        options
                    }
                },
                onCircleAdded = { circle, _ -> lastCircle = circle },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
