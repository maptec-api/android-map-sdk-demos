package com.maptec.applied.demo.ui.screens.overlays.polyline

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PolylineGradientScreen(modifier: Modifier = Modifier) {
    LineScreen(mode = LineMode.GRADIENT, modifier = modifier)
}
