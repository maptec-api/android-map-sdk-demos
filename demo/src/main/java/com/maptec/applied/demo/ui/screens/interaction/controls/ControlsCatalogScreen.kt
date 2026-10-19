package com.maptec.applied.demo.ui.screens.interaction.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun ControlsCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.map_item_zoom), CatalogRoutes.ZOOM),
            ScreenItem(stringResource(R.string.map_item_compass), CatalogRoutes.COMPASS),
            ScreenItem(stringResource(R.string.map_item_scale_bar), CatalogRoutes.SCALE_BAR),
            ScreenItem(stringResource(R.string.map_item_day_night_mode), CatalogRoutes.DAY_NIGHT),
            ScreenItem(stringResource(R.string.map_item_logo), CatalogRoutes.LOGO),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
