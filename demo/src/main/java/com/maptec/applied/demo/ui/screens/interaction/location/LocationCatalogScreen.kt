package com.maptec.applied.demo.ui.screens.interaction.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun LocationCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.map_item_location_layer), CatalogRoutes.LOCATION_LAYER),
            ScreenItem(stringResource(R.string.map_item_location_button), CatalogRoutes.LOCATION_BUTTON),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
