package com.maptec.applied.demo.ui.screens.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun ToolsCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Items(
        items = listOf(
            ScreenItem(stringResource(R.string.catalog_geometry_demo), CatalogRoutes.GEOMETRY_DEMO),
        ),
        onItemClicked = onItemClicked,
        modifier = modifier,
    )
}
