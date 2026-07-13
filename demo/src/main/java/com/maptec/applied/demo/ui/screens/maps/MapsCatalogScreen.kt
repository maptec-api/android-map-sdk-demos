package com.maptec.applied.demo.ui.screens.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun MapsCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.map_item_map_render), CatalogRoutes.ADD_MAP),
            ScreenItem(stringResource(R.string.map_item_map_style), CatalogRoutes.MAP_STYLE),
            ScreenItem(stringResource(R.string.map_item_map_control), CatalogRoutes.CAMERA),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
