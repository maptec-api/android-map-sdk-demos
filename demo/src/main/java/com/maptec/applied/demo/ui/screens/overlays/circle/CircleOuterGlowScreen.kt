package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.maptec.applied.demo.R
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleOuterGlowScreen() {
    val state = rememberBasicCircleState()
    var outerGlow by remember { mutableStateOf(true) }
    var glowColor by remember { mutableStateOf("#0066FF") }
    var glowRadius by remember { mutableStateOf("10") }
    val glowColorError = validateColor(glowColor)
    val glowRadiusError = validateGlowRadius(glowRadius)

    val isValid = state.isValid && (!outerGlow || (glowColorError == null && glowRadiusError == null))

    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    OutlinedTextField(
                        value = glowColor,
                        onValueChange = { glowColor = it },
                        label = { Text(stringResource(R.string.circle_glow_color)) },
                        supportingText = glowColorError?.let { { Text(it, color = Color.Red) } },
                        isError = glowColorError != null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .testTag("circle_input_glow_color"),
                    )
                    OutlinedTextField(
                        value = glowRadius,
                        onValueChange = { glowRadius = it },
                        label = { Text(stringResource(R.string.circle_glow_radius)) },
                        supportingText = glowRadiusError?.let { { Text(it, color = Color.Red) } },
                        isError = glowRadiusError != null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .testTag("circle_input_glow_radius"),
                    )
                }
            }
            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                enabled = isValid,
                buildOptions = {
                    val options = state.applyTo(CircleOptions())
                        .withOuterGlow(outerGlow)
                    if (outerGlow) {
                        options
                            .withGlowColor(glowColor)
                            .withGlowRadius(glowRadius.toFloatOrNull() ?: 10f)
                    } else options
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
