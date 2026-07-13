package com.maptec.applied.demo.ui.screens.overlays.polyline

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PolylineGlowScreen(modifier: Modifier = Modifier) {
    LineScreen(mode = LineMode.GLOW, modifier = modifier)
}
