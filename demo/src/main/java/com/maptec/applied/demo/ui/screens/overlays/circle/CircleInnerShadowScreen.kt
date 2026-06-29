package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.maptec.applied.demo.R
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleInnerShadowScreen() {
    val state = rememberBasicCircleState()
    var innerShadow by remember { mutableStateOf(true) }
    var innerShadowBlur by remember { mutableStateOf("10") }

    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    OutlinedTextField(
                        value = innerShadowBlur,
                        onValueChange = { innerShadowBlur = it },
                        label = { Text(stringResource(R.string.circle_inner_shadow_blur)) },
                        supportingText = {
                            Text(
                                stringResource(R.string.circle_inner_shadow_hint),
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("circle_input_inner_shadow_blur"),
                    )
                }
            }
            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = {
                    val options = state.applyTo(CircleOptions())
                        .withInnerShadow(innerShadow)
                        .withInnerShadowColor("#000000")
                        .withInnerShadowOpacity(0.5f)
                    if (innerShadow) {
                        options.withInnerShadowBlur(innerShadowBlur.toFloatOrNull() ?: 10f)
                    } else options
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
