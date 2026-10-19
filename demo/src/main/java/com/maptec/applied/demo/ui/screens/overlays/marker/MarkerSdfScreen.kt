package com.maptec.applied.demo.ui.screens.overlays.marker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MarkerSdfScreen(modifier: Modifier = Modifier) {
    MarkerLayerScreen(mode = MarkerLayerMode.SDF, modifier = modifier)
}
