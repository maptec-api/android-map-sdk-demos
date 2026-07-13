package com.maptec.applied.demo.ui.screens.interaction.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LocationLayerScreen(modifier: Modifier = Modifier) {
    LocationScreen(mode = LocationMode.LAYER, modifier = modifier)
}
