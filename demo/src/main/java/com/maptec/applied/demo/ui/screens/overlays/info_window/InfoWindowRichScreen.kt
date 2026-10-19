package com.maptec.applied.demo.ui.screens.overlays.info_window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun InfoWindowRichScreen(modifier: Modifier = Modifier) {
    InfoWindowScreen(mode = InfoWindowMode.RICH_TEXT, modifier = modifier)
}
