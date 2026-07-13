package com.maptec.applied.demo.ui.screens.overlays.geofence

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GeofencePolygonScreen(modifier: Modifier = Modifier) {
    GeofenceScreen(mode = GeofenceMode.POLYGON, modifier = modifier)
}
