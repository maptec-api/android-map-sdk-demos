package com.maptec.applied.demo.ui.screens.interaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun InteractionCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.catalog_ui_controls), CatalogRoutes.UI_CONTROLS),
            ScreenItem(stringResource(R.string.map_item_map_gesture), CatalogRoutes.GESTURES),
            ScreenItem(stringResource(R.string.catalog_user_location), CatalogRoutes.USER_LOCATION),
            ScreenItem(stringResource(R.string.map_item_map_event_listener), CatalogRoutes.MAP_EVENTS),
            ScreenItem(stringResource(R.string.map_item_poi_click_center), CatalogRoutes.POI_QUERY),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
