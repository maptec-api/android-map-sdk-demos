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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.ColorPickerField
import com.maptec.applied.demo.ui.screens.common.DemoNumericSliderField
import com.maptec.applied.demo.ui.screens.common.formatDemoSliderValue
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions

private const val INNER_SHADOW_OPACITY = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInnerShadowScreen() {
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
    var innerShadow by remember { mutableStateOf(true) }
    var innerShadowColor by remember { mutableStateOf("#000000") }
    var innerShadowBlur by remember { mutableStateOf("10") }

    var lastCircle by remember { mutableStateOf<Circle?>(null) }

    fun applyShadowProps(circle: Circle) {
        applyBasicCircleProps(circle, state)
        runCatching { circle.setInnerShadow(innerShadow) }
        if (innerShadow) {
            runCatching { circle.setInnerShadowColor(innerShadowColor) }
            runCatching { circle.setInnerShadowOpacity(INNER_SHADOW_OPACITY) }
            innerShadowBlur.toFloatOrNull()?.takeIf { it in 1f..50f }?.let { runCatching { circle.setInnerShadowBlur(it) } }
        }
    }

    CircleScaffold(
        defaultOptions = {
            state.applyTo(CircleOptions())
                .withInnerShadow(innerShadow)
                .withInnerShadowColor(innerShadowColor)
                .withInnerShadowOpacity(INNER_SHADOW_OPACITY)
                .withInnerShadowBlur(innerShadowBlur.toFloatOrNull() ?: 10f)
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
            innerShadow,
            innerShadowColor,
            innerShadowBlur,
        ) {
            lastCircle?.let { circle ->
                applyShadowProps(circle)
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
                Text(stringResource(R.string.circle_switch_inner_shadow), modifier = Modifier.weight(1f))
                Switch(
                    checked = innerShadow,
                    onCheckedChange = { innerShadow = it },
                    modifier = Modifier.testTag("circle_switch_inner_shadow"),
                )
            }

            if (innerShadow) {
                ColorPickerField(
                    value = innerShadowColor,
                    onValueChange = { innerShadowColor = it },
                    label = stringResource(R.string.circle_inner_shadow_color),
                    supportingText = validateColor(innerShadowColor)?.let { { Text(it, color = Color.Red) } },
                    isError = validateColor(innerShadowColor) != null,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "circle_input_inner_shadow_color",
                )
                DemoNumericSliderField(
                    label = "${stringResource(R.string.circle_inner_shadow_blur)}: $innerShadowBlur",
                    value = innerShadowBlur.toFloatOrNull()?.coerceIn(1f, 50f) ?: 10f,
                    valueText = innerShadowBlur,
                    onValueChange = { innerShadowBlur = formatDemoSliderValue(it) },
                    onValueTextChange = { innerShadowBlur = it },
                    valueRange = 1f..50f,
                    rangeStartLabel = "1",
                    rangeEndLabel = "50",
                    testTag = "circle_input_inner_shadow_blur",
                )
                Text(
                    stringResource(R.string.circle_inner_shadow_hint),
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                )
            }

            DrawCircleButton(
                state = state,
                mapView = mapView,
                buildOptions = {
                    val options = state.applyTo(CircleOptions())
                        .withInnerShadow(innerShadow)
                        .withInnerShadowColor(innerShadowColor)
                        .withInnerShadowOpacity(INNER_SHADOW_OPACITY)
                    if (innerShadow) {
                        options.withInnerShadowBlur(innerShadowBlur.toFloatOrNull() ?: 10f)
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
