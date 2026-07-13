package com.maptec.applied.demo.ui.screens.interaction.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LocationButtonScreen(modifier: Modifier = Modifier) {
    LocationScreen(mode = LocationMode.BUTTON, modifier = modifier)
}
