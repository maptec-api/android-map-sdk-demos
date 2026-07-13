package com.maptec.applied.demo.ui.screens.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun MainCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        ScreenItem(stringResource(R.string.screen_item_attention), CatalogRoutes.AUTH),
        ScreenItem(stringResource(R.string.catalog_main_maps), CatalogRoutes.MAPS),
        ScreenItem(stringResource(R.string.catalog_main_annotations), CatalogRoutes.ANNOTATIONS),
        ScreenItem(stringResource(R.string.catalog_main_interaction), CatalogRoutes.INTERACTION),
        ScreenItem(stringResource(R.string.catalog_main_web_services), CatalogRoutes.WEB_SERVICES),
    )
    Items(items, onItemClicked, modifier)
}
