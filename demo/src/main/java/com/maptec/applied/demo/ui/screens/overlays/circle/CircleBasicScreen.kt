package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.maptec.applied.maps.overlay.circle.CircleOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleBasicScreen() {
    val state = rememberBasicCircleState()
    CircleScaffold { mapView, _, scaffoldState ->
        CirclePanelColumn {
            BasicCircleInputs(state)
            DrawCircleButton(
                state = state,
                mapView = mapView,
                scaffoldState = scaffoldState,
                buildOptions = { state.applyTo(CircleOptions()) },
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
        }
    }
}
