package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.maps.MaptecMap

@Composable
fun TextSearchScreen(
    registerNestedBackHandler: ((() -> Boolean)?) -> Unit = {},
) {
    SearchScreenScaffold(
        tab = SearchTab.TEXT,
        registerNestedBackHandler = registerNestedBackHandler,
    ) { viewModel, mapRef ->
        TextSearchListMode(viewModel, mapRef)
    }
}

@Composable
private fun TextSearchListMode(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val nextPageToken by viewModel.nextPageToken.collectAsState()

    SearchListModeLayout(
        viewModel = viewModel,
        mapLibreMapRef = mapLibreMapRef,
        panelContent = {
            TextSearchPanel(viewModel, searchQuery, isLoading)
        },
        panelFooter = {
            TextAutoSuggestionsFooter(viewModel)
        },
        canLoadMore = !nextPageToken.isNullOrBlank(),
        onLoadMore = viewModel::loadMoreTextSearch,
    )
}
