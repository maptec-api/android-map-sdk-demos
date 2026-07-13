package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.maps.MaptecMap

@Composable
fun PlaceDetailScreen() {
    SearchScreenScaffold(SearchTab.DETAIL) { viewModel, mapRef ->
        PlaceDetailListMode(viewModel, mapRef)
    }
}

@Composable
private fun PlaceDetailListMode(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
) {
    val isLoadingPlaceDetail by viewModel.isLoadingPlaceDetail.collectAsState()

    SearchListModeLayout(
        viewModel = viewModel,
        mapLibreMapRef = mapLibreMapRef,
        panelContent = {
            PlaceDetailPanel(viewModel, isLoadingPlaceDetail)
        },
        showResultsList = false,
    )
}
