package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun CircleGeodesicScreen() {
    val state = rememberBasicCircleState()
    var geodesic by remember { mutableStateOf(false) }
    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state, geodesic = geodesic)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.circle_switch_geodesic), modifier = Modifier.weight(1f))
                Switch(
                    checked = geodesic,
                    onCheckedChange = { geodesic = it },
                    modifier = Modifier.testTag("circle_switch_geodesic"),
                )
            }
            Text(
                text = stringResource(R.string.circle_geodesic_hint),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                fontSize = 12.sp,
                color = Color(0xFF666666),
            )
            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = { state.applyTo(CircleOptions(), geodesic = geodesic) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
