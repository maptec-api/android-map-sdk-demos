package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.maps.MaptecMap

@Composable
fun AlongRouteSearchScreen(
    registerNestedBackHandler: ((() -> Boolean)?) -> Unit = {},
) {
    SearchScreenScaffold(
        tab = SearchTab.ALONG_ROUTE,
        registerNestedBackHandler = registerNestedBackHandler,
    ) { viewModel, mapRef ->
        AlongRouteSearchListMode(viewModel, mapRef)
    }
}

@Composable
private fun AlongRouteSearchListMode(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val alongRouteResults by viewModel.alongRouteResults.collectAsState()
    val showResults by viewModel.showResults.collectAsState()

    SearchListModeLayout(
        viewModel = viewModel,
        mapLibreMapRef = mapLibreMapRef,
        showResultsList = false,
        panelContent = {
            AlongRouteSearchPanel(viewModel, isLoading)
        },
        externalContent = {
            if (showResults && alongRouteResults.isNotEmpty()) {
                AlongRouteResultsList(
                    results = alongRouteResults,
                    onResultClick = { result ->
                        mapLibreMapRef?.let { viewModel.onPlaceClick(result.place, it) }
                            ?: viewModel.onPlaceClick(result.place, null)
                    },
                )
            }
        },
    )
}
