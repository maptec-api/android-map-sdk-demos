package com.maptec.applied.demo.ui.screens.web_services

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun WebServicesCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.catalog_text_search), CatalogRoutes.TEXT_SEARCH),
            ScreenItem(stringResource(R.string.catalog_nearby_search), CatalogRoutes.NEARBY_SEARCH),
            ScreenItem(stringResource(R.string.catalog_suggest), CatalogRoutes.SUGGEST),
            ScreenItem(stringResource(R.string.catalog_places), CatalogRoutes.PLACES),
            ScreenItem(stringResource(R.string.catalog_forward_geocode), CatalogRoutes.GEOCODE),
            ScreenItem(stringResource(R.string.catalog_reverse_geocode), CatalogRoutes.REVERSE_GEOCODE),
            ScreenItem(stringResource(R.string.screen_item_route), CatalogRoutes.ROUTE),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
