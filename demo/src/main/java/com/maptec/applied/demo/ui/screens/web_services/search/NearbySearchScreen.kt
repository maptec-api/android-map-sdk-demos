package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.maps.MaptecMap

@Composable
fun NearbySearchScreen() {
    SearchScreenScaffold(SearchTab.NEARBY) { viewModel, mapRef ->
        NearbySearchListMode(viewModel, mapRef)
    }
}

@Composable
private fun NearbySearchListMode(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
) {
    val isLoading by viewModel.isLoading.collectAsState()

    SearchListModeLayout(
        viewModel = viewModel,
        mapLibreMapRef = mapLibreMapRef,
        panelContent = {
            NearbySearchPanel(viewModel, isLoading)
        },
        panelFooter = {
            PlaceDetailInlineCard(viewModel)
        },
    )
}
