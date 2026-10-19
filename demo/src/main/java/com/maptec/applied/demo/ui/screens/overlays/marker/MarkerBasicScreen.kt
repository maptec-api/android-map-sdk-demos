package com.maptec.applied.demo.ui.screens.overlays.marker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MarkerBasicScreen(modifier: Modifier = Modifier) {
    MarkerLayerScreen(mode = MarkerLayerMode.BASIC, modifier = modifier)
}
