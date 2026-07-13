package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.maps.MaptecMap

@Composable
fun SuggestSearchScreen() {
    SearchScreenScaffold(SearchTab.SUGGEST) { viewModel, mapRef ->
        SuggestSearchListMode(viewModel, mapRef)
    }
}

@Composable
private fun SuggestSearchListMode(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    SearchListModeLayout(
        viewModel = viewModel,
        mapLibreMapRef = mapLibreMapRef,
        panelContent = {
            SuggestSearchPanel(viewModel, searchQuery, isLoading)
        },
        panelFooter = {
            PlaceDetailInlineCard(viewModel)
        },
        externalContent = {
            SuggestResultsFooter(viewModel)
        },
        showResultsList = false,
    )
}
