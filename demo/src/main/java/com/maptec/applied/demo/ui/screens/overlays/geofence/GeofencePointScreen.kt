package com.maptec.applied.demo.ui.screens.overlays.geofence

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GeofencePointScreen(modifier: Modifier = Modifier) {
    GeofenceScreen(mode = GeofenceMode.POINT, modifier = modifier)
}
