package com.maptec.applied.demo.ui.screens.catalog

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import com.maptec.applied.demo.ui.screens.common.ScreenItem

object CatalogExtras {
    @Composable
    fun mainCatalogItems(): List<ScreenItem> = emptyList()

    @Suppress("UNUSED_PARAMETER")
    fun NavGraphBuilder.registerExtraRoutes(
        registerNestedBackHandler: ((() -> Boolean)?) -> Unit,
        onCatalogItemClicked: (ScreenItem) -> Unit,
    ) {
    }

    @Composable
    fun extraTopBarTitle(route: String?): String? = null
}
