@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens.web_services.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.AdvancedFieldsSurface
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.NearbyAdvancedSummary
import com.maptec.applied.demo.ui.screens.common.WebServiceApiResponseCard
import com.maptec.applied.demo.ui.screens.common.WebServicePanel
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.LocationMode
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.demo.viewmodel.SearchViewModelFactory
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.Suggestion

@Composable
internal fun SearchScreenScaffold(
    tab: SearchTab,
    listContent: @Composable (viewModel: SearchViewModel, mapLibreMapRef: MaptecMap?) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(
        factory = remember(context) { SearchViewModelFactory(context.applicationContext) },
    )

    val showMap by viewModel.showMap.collectAsState()
    val places by viewModel.places.collectAsState()
    val placeDetail by viewModel.placeDetail.collectAsState()
    val showPlaceDetail by viewModel.showPlaceDetail.collectAsState()
    val isLoadingPlaceDetail by viewModel.isLoadingPlaceDetail.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var mapLibreMapRef: MaptecMap? by remember { mutableStateOf(null) }

    LaunchedEffect(tab) {
        viewModel.switchTab(tab)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    LaunchedEffect(showMap, places) {
        if (showMap && places.isNotEmpty()) {
            viewModel.drawAllMarkers(fitCameraToResults = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var mapInitialized by remember { mutableStateOf(false) }
        if (showMap) mapInitialized = true

        if (mapInitialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showMap) 1f else 0f),
            ) {
                SearchMapModeContent(
                    viewModel = viewModel,
                    places = places,
                    mapCenter = mapCenter,
                    showPlaceDetail = showPlaceDetail,
                    placeDetail = placeDetail,
                    isLoadingPlaceDetail = isLoadingPlaceDetail,
                    onMapRefChanged = { mapLibreMapRef = it },
                    nearbyRadius = viewModel.nearbyRadius.collectAsState().value,
                    currentTab = tab,
                )
            }
        }

        if (!showMap) {
            listContent(viewModel, mapLibreMapRef)
        }

        FloatingActionButton(
            onClick = { viewModel.toggleMapView() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = if (showMap) Icons.Default.List else Icons.Default.LocationOn,
                contentDescription = if (showMap) {
                    stringResource(R.string.search_list_view)
                } else {
                    stringResource(R.string.search_map_view)
                },
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
internal fun SearchListModeLayout(
    viewModel: SearchViewModel,
    mapLibreMapRef: MaptecMap?,
    panelContent: @Composable () -> Unit,
    panelFooter: @Composable () -> Unit = {},
    showResultsList: Boolean = true,
    canLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val places by viewModel.places.collectAsState()
    val showResults by viewModel.showResults.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val selectedApiName by viewModel.currentTabSelectedApiName.collectAsState()
    val selectedResponseJson by viewModel.currentTabSelectedResponseJson.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (selectedResponseJson.isNotBlank()) {
            WebServiceApiResponseCard(
                titlePrefixRes = R.string.search_api_response_title,
                selectedApiName = selectedApiName,
                responseJson = selectedResponseJson,
            )
        }

        val hasResults = showResults && places.isNotEmpty()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = if (hasResults) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                },
            ) {
                WebServicePanel {
                    panelContent()
                    panelFooter()
                }
            }

            if (hasResults && showResultsList) {
                SearchResultsList(
                    places = places,
                    onPlaceClick = { place ->
                        mapLibreMapRef?.let { viewModel.onPlaceClick(place, it) }
                            ?: viewModel.onPlaceClick(place, null)
                    },
                    modifier = Modifier.weight(1f),
                    canLoadMore = canLoadMore,
                    isLoadingMore = isLoadingMore,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
internal fun SearchMapModeContent(
    viewModel: SearchViewModel,
    places: List<Place>,
    mapCenter: com.maptec.applied.geometry.LatLng?,
    showPlaceDetail: Boolean,
    placeDetail: Place?,
    isLoadingPlaceDetail: Boolean,
    onMapRefChanged: (MaptecMap) -> Unit,
    nearbyRadius: String,
    currentTab: SearchTab,
) {
    val context = LocalContext.current
    var mapLibreMapRef: MaptecMap? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Mapview(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { _, mapLibreMap ->
                mapLibreMapRef = mapLibreMap
                onMapRefChanged(mapLibreMap)

                mapLibreMap.uiSettings.setZoomButtonsEnabled(false)

                viewModel.initMapOverlays(context, mapLibreMap)

                mapLibreMap.addOnCameraMoveListener {
                    val pos = mapLibreMap.cameraPosition
                    viewModel.updateMapCenter(pos.target)
                }

                val initialCenter = mapLibreMap.cameraPosition.target
                viewModel.updateMapCenter(initialCenter)

                mapLibreMap.getStyle {
                    viewModel.drawAllMarkers(fitCameraToResults = true)
                    viewModel.drawLocationShape(mapLibreMap)
                }
            },
        )

        if (showPlaceDetail) {
            PlaceDetailBottomSheet(
                place = placeDetail,
                isLoading = isLoadingPlaceDetail,
                onDismiss = { viewModel.closePlaceDetail() },
            )
        }
    }

    LaunchedEffect(places) {
        viewModel.drawAllMarkers(fitCameraToResults = true)
    }

    val textLocationMode by viewModel.locationMode.collectAsState()
    val textRectNE by viewModel.rectNorthEast.collectAsState()
    val textRectSW by viewModel.rectSouthWest.collectAsState()
    val textCircleCenter by viewModel.circleCenter.collectAsState()
    val textCircleRadius by viewModel.circleRadius.collectAsState()
    val suggestLocationMode by viewModel.suggestLocationMode.collectAsState()
    val suggestRectNE by viewModel.suggestRectNorthEast.collectAsState()
    val suggestRectSW by viewModel.suggestRectSouthWest.collectAsState()
    val suggestCircleCenter by viewModel.suggestCircleCenter.collectAsState()
    val suggestCircleRadius by viewModel.suggestCircleRadius.collectAsState()
    val nearbyCenterString by viewModel.nearbyCenterString.collectAsState()

    LaunchedEffect(
        currentTab,
        mapCenter,
        nearbyRadius,
        nearbyCenterString,
        textLocationMode,
        textRectNE,
        textRectSW,
        textCircleCenter,
        textCircleRadius,
        suggestLocationMode,
        suggestRectNE,
        suggestRectSW,
        suggestCircleCenter,
        suggestCircleRadius,
    ) {
        mapLibreMapRef?.let { map ->
            viewModel.drawLocationShape(map)
        }
    }
}

@Composable
internal fun TextSearchPanel(
    viewModel: SearchViewModel,
    searchQuery: String,
    isLoading: Boolean,
) {
    val typeString by viewModel.textType.collectAsState()
    val languageString by viewModel.languageString.collectAsState()
    val regionString by viewModel.regionString.collectAsState()
    val pageSizeString by viewModel.pageSizeString.collectAsState()
    val pageTokenString by viewModel.pageTokenString.collectAsState()
    val rankType by viewModel.rankType.collectAsState()
    val locationMode by viewModel.locationMode.collectAsState()
    val rectNorthEast by viewModel.rectNorthEast.collectAsState()
    val rectSouthWest by viewModel.rectSouthWest.collectAsState()
    val circleCenter by viewModel.circleCenter.collectAsState()
    val circleRadius by viewModel.circleRadius.collectAsState()

    var advancedExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }

    QueryInputBar(
        searchQuery = searchQuery,
        isLoading = isLoading,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearchClick = { viewModel.performTextSearch(searchQuery) },
        placeholder = stringResource(R.string.search_placeholder),
    )

    OutlinedTextField(
        value = typeString,
        onValueChange = viewModel::setTextType,
        label = { Text(stringResource(R.string.search_label_type)) },
        supportingText = { Text(stringResource(R.string.search_hint_type), color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )

    NearbyAdvancedSummary(
        expanded = advancedExpanded,
        resultLimit = pageSizeString,
        rank = rankType?.name,
        language = languageString,
        region = regionString,
        onToggle = { advancedExpanded = !advancedExpanded },
    )

    if (advancedExpanded) {
        AdvancedFieldsSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = languageString,
                    onValueChange = viewModel::setLanguageString,
                    label = { Text(stringResource(R.string.search_label_language)) },
                    supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = regionString,
                    onValueChange = viewModel::setRegionString,
                    label = { Text(stringResource(R.string.search_label_region)) },
                    supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_region"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = pageSizeString,
                    onValueChange = viewModel::setPageSizeString,
                    label = { Text(stringResource(R.string.search_label_pageSize)) },
                    supportingText = { Text(stringResource(R.string.search_hint_page_size), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_page_size"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = rankType?.name ?: "",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.search_label_rank)) },
                        supportingText = { Text(stringResource(R.string.search_rank_default_hint), color = Color.Gray) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = rankExpanded,
                        onDismissRequest = { rankExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.search_not_set)) },
                            onClick = { viewModel.setRankType(null); rankExpanded = false },
                        )
                        RankType.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { viewModel.setRankType(item); rankExpanded = false },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = pageTokenString,
                onValueChange = viewModel::setPageTokenString,
                label = { Text(stringResource(R.string.search_label_pageToken)) },
                supportingText = { Text(stringResource(R.string.search_hint_page_token), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            LocationModeSelector(
                locationMode = locationMode,
                onLocationModeChange = viewModel::setLocationMode,
                rectNorthEast = rectNorthEast,
                onRectNorthEastChange = viewModel::setRectNorthEast,
                rectSouthWest = rectSouthWest,
                onRectSouthWestChange = viewModel::setRectSouthWest,
                circleCenter = circleCenter,
                onCircleCenterChange = viewModel::setCircleCenter,
                circleRadius = circleRadius,
                onCircleRadiusChange = viewModel::setCircleRadius,
            )
        }
    }
}

@Composable
internal fun NearbySearchPanel(
    viewModel: SearchViewModel,
    isLoading: Boolean,
) {
    val nearbyCenterString by viewModel.nearbyCenterString.collectAsState()
    val nearbyTypes by viewModel.nearbyTypes.collectAsState()
    val nearbyRadius by viewModel.nearbyRadius.collectAsState()
    val nearbyResultLimit by viewModel.nearbyResultLimit.collectAsState()
    val nearbyRankType by viewModel.nearbyRankType.collectAsState()
    val languageString by viewModel.languageString.collectAsState()
    val regionString by viewModel.regionString.collectAsState()

    var advancedExpanded by remember { mutableStateOf(false) }
    var rankExpanded by remember { mutableStateOf(false) }
    val radiusError = nearbyRadius.isNotEmpty() &&
        (nearbyRadius.toIntOrNull() == null || nearbyRadius.toIntOrNull() !in 0..50000)
    val canSearch = !isLoading
        && nearbyRadius.isNotEmpty()
        && nearbyRadius.toIntOrNull()?.let { it in 0..50000 } == true
        && (nearbyCenterString.isBlank() || validateLatLng(nearbyCenterString, required = false) == null)

    Text(
        text = stringResource(R.string.search_nearby_tip),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF718096),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LatLngOutlinedTextField(
            value = nearbyCenterString,
            onValueChange = viewModel::setNearbyCenterString,
            label = stringResource(R.string.search_label_center),
            hint = stringResource(R.string.search_center_hint),
            required = false,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = nearbyRadius,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    viewModel.setNearbyRadius(newValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_nearby_radius"),
            placeholder = { Text(stringResource(R.string.search_radius_placeholder)) },
            label = { Text(stringResource(R.string.search_label_radius)) },
            singleLine = true,
            isError = radiusError,
            supportingText = {
                val v = nearbyRadius.toIntOrNull()
                when {
                    nearbyRadius.isBlank() -> Text(stringResource(R.string.search_radius_required_hint), color = Color.Gray)
                    v == null -> Text(stringResource(R.string.search_error_invalid_number), color = MaterialTheme.colorScheme.error)
                    v !in 0..50000 -> Text(stringResource(R.string.search_error_range_0_50000), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PoiCategoryDropdown(
            selectedValue = nearbyTypes,
            onValueChange = viewModel::setNearbyTypes,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.performNearbySearch() },
            enabled = canSearch,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .testTag("search_nearby_submit_button"),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_action_search))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.search_action_search))
            }
        }
    }

    NearbyAdvancedSummary(
        expanded = advancedExpanded,
        resultLimit = nearbyResultLimit,
        rank = nearbyRankType?.name,
        language = languageString,
        region = regionString,
        onToggle = { advancedExpanded = !advancedExpanded },
    )

    if (advancedExpanded) {
        AdvancedFieldsSurface {
            OutlinedTextField(
                value = nearbyResultLimit,
                onValueChange = viewModel::setNearbyResultLimit,
                label = { Text(stringResource(R.string.search_label_result_limit)) },
                supportingText = { Text(stringResource(R.string.search_hint_result_limit), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().testTag("search_nearby_result_limit"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            ExposedDropdownMenuBox(
                expanded = rankExpanded,
                onExpandedChange = { rankExpanded = !rankExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = nearbyRankType?.name ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.search_label_rank)) },
                    supportingText = { Text(stringResource(R.string.search_rank_default_hint), color = Color.Gray) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(
                    expanded = rankExpanded,
                    onDismissRequest = { rankExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_not_set)) },
                        onClick = { viewModel.setNearbyRankType(null); rankExpanded = false },
                    )
                    RankType.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = { viewModel.setNearbyRankType(item); rankExpanded = false },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = languageString,
                    onValueChange = viewModel::setLanguageString,
                    label = { Text(stringResource(R.string.search_label_language)) },
                    supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = regionString,
                    onValueChange = viewModel::setRegionString,
                    label = { Text(stringResource(R.string.search_label_region)) },
                    supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

@Composable
internal fun SuggestSearchPanel(
    viewModel: SearchViewModel,
    searchQuery: String,
    isLoading: Boolean,
) {
    val suggestTypes by viewModel.suggestTypes.collectAsState()
    val suggestResultLimit by viewModel.suggestResultLimit.collectAsState()
    val suggestLocationMode by viewModel.suggestLocationMode.collectAsState()
    val suggestRectNorthEast by viewModel.suggestRectNorthEast.collectAsState()
    val suggestRectSouthWest by viewModel.suggestRectSouthWest.collectAsState()
    val suggestCircleCenter by viewModel.suggestCircleCenter.collectAsState()
    val suggestCircleRadius by viewModel.suggestCircleRadius.collectAsState()
    val languageString by viewModel.languageString.collectAsState()
    val regionString by viewModel.regionString.collectAsState()

    var advancedExpanded by remember { mutableStateOf(false) }

    QueryInputBar(
        searchQuery = searchQuery,
        isLoading = isLoading,
        onQueryChange = viewModel::setSearchQuery,
        onSearchClick = { viewModel.performSuggestSearch(searchQuery) },
        placeholder = stringResource(R.string.search_suggest_placeholder),
    )

    OutlinedTextField(
        value = suggestTypes,
        onValueChange = viewModel::setSuggestTypes,
        label = { Text(stringResource(R.string.search_label_suggest_types)) },
        supportingText = { Text(stringResource(R.string.search_hint_suggest_types), color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().testTag("search_input_suggest_types"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )

    NearbyAdvancedSummary(
        expanded = advancedExpanded,
        resultLimit = suggestResultLimit,
        rank = null,
        language = languageString,
        region = regionString,
        onToggle = { advancedExpanded = !advancedExpanded },
    )

    if (advancedExpanded) {
        AdvancedFieldsSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = suggestResultLimit,
                    onValueChange = viewModel::setSuggestResultLimit,
                    label = { Text(stringResource(R.string.search_label_result_limit)) },
                    supportingText = { Text(stringResource(R.string.search_hint_result_limit), color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = languageString,
                    onValueChange = viewModel::setLanguageString,
                    label = { Text(stringResource(R.string.search_label_language)) },
                    supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            OutlinedTextField(
                value = regionString,
                onValueChange = viewModel::setRegionString,
                label = { Text(stringResource(R.string.search_label_region)) },
                supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            LocationModeSelector(
                locationMode = suggestLocationMode,
                onLocationModeChange = viewModel::setSuggestLocationMode,
                rectNorthEast = suggestRectNorthEast,
                onRectNorthEastChange = viewModel::setSuggestRectNorthEast,
                rectSouthWest = suggestRectSouthWest,
                onRectSouthWestChange = viewModel::setSuggestRectSouthWest,
                circleCenter = suggestCircleCenter,
                onCircleCenterChange = viewModel::setSuggestCircleCenter,
                circleRadius = suggestCircleRadius,
                onCircleRadiusChange = viewModel::setSuggestCircleRadius,
            )
        }
    }
}

@Composable
internal fun PlaceDetailPanel(
    viewModel: SearchViewModel,
    isLoadingPlaceDetail: Boolean,
) {
    val placeId by viewModel.detailPlaceId.collectAsState()
    val languageString by viewModel.languageString.collectAsState()
    val regionString by viewModel.regionString.collectAsState()
    var advancedExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = placeId,
        onValueChange = viewModel::setDetailPlaceId,
        label = { Text(stringResource(R.string.search_label_place_id)) },
        supportingText = { Text(stringResource(R.string.search_hint_place_id), color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().testTag("search_input_place_id"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )

    NearbyAdvancedSummary(
        expanded = advancedExpanded,
        resultLimit = "",
        rank = null,
        language = languageString,
        region = regionString,
        onToggle = { advancedExpanded = !advancedExpanded },
    )

    if (advancedExpanded) {
        AdvancedFieldsSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = languageString,
                    onValueChange = viewModel::setLanguageString,
                    label = { Text(stringResource(R.string.search_label_language)) },
                    supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = regionString,
                    onValueChange = viewModel::setRegionString,
                    label = { Text(stringResource(R.string.search_label_region)) },
                    supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_input_region"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }

    Button(
        onClick = viewModel::performPlaceDetailFromTab,
        enabled = !isLoadingPlaceDetail,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            if (isLoadingPlaceDetail) {
                stringResource(R.string.search_loading)
            } else {
                stringResource(R.string.search_action_get_detail)
            },
        )
    }
}

@Composable
internal fun PlaceDetailInlineCard(
    viewModel: SearchViewModel,
) {
    val placeDetail by viewModel.placeDetail.collectAsState()
    val isLoadingPlaceDetail by viewModel.isLoadingPlaceDetail.collectAsState()
    val showPlaceDetail by viewModel.showPlaceDetail.collectAsState()

    if (showPlaceDetail) {
        Spacer(modifier = Modifier.height(12.dp))
        PlaceDetailCard(
            placeDetail = placeDetail,
            isLoading = isLoadingPlaceDetail,
            onDismiss = viewModel::closePlaceDetail,
        )
    }
}

@Composable
internal fun TextAutoSuggestionsFooter(viewModel: SearchViewModel) {
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val showResults by viewModel.showResults.collectAsState()
    val autoSuggestLimit by viewModel.autoSuggestResultLimit.collectAsState()
    val isLoadingSuggestMore by viewModel.isLoadingSuggestMore.collectAsState()

    if (showSuggestions && suggestions.isNotEmpty() && !showResults) {
        Spacer(modifier = Modifier.height(12.dp))
        val canLoadMoreSuggestions = autoSuggestLimit < 20 && suggestions.size >= autoSuggestLimit
        SuggestionsList(
            suggestions = suggestions,
            onSuggestionClick = viewModel::selectSuggestion,
            canLoadMore = canLoadMoreSuggestions,
            isLoadingMore = isLoadingSuggestMore,
            onLoadMore = viewModel::loadMoreAutoSuggest,
        )
    }
}

@Composable
internal fun SuggestResultsFooter(viewModel: SearchViewModel) {
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val suggestResultLimitString by viewModel.suggestResultLimit.collectAsState()

    if (showSuggestions && suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        val suggestLimit = suggestResultLimitString.trim().toIntOrNull()?.takeIf { it in 1..20 } ?: 10
        val canLoadMoreSuggestions = suggestLimit < 20 && suggestions.size >= suggestLimit
        SuggestionsList(
            suggestions = suggestions,
            onSuggestionClick = viewModel::onSuggestResultClick,
            canLoadMore = canLoadMoreSuggestions,
            isLoadingMore = isLoading,
            onLoadMore = viewModel::loadMoreSuggestSearch,
        )
    }
}

@Composable
private fun QueryInputBar(
    searchQuery: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    placeholder: String,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
    }

    LaunchedEffect(searchQuery) {
        if (textFieldValue.text != searchQuery) {
            textFieldValue = TextFieldValue(searchQuery, TextRange(searchQuery.length))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onQueryChange(newValue.text)
            },
            modifier = Modifier.weight(1f).testTag("search_query_input"),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_action_search)) },
            singleLine = true,
        )
        Button(
            onClick = onSearchClick,
            enabled = !isLoading && searchQuery.isNotBlank(),
            modifier = Modifier.testTag("search_submit_button"),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.search_action_search))
            }
        }
    }
}

@Composable
private fun LocationModeSelector(
    locationMode: LocationMode,
    onLocationModeChange: (LocationMode) -> Unit,
    rectNorthEast: String,
    onRectNorthEastChange: (String) -> Unit,
    rectSouthWest: String,
    onRectSouthWestChange: (String) -> Unit,
    circleCenter: String,
    onCircleCenterChange: (String) -> Unit,
    circleRadius: String,
    onCircleRadiusChange: (String) -> Unit,
) {
    var locationExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = locationExpanded,
        onExpandedChange = { locationExpanded = !locationExpanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = locationMode.label,
            onValueChange = { },
            label = { Text(stringResource(R.string.search_location_label)) },
            supportingText = { Text(stringResource(R.string.search_location_hint), color = Color.Gray) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = locationExpanded,
            onDismissRequest = { locationExpanded = false },
        ) {
            LocationMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onLocationModeChange(mode)
                        locationExpanded = false
                    },
                )
            }
        }
    }

    when (locationMode) {
        LocationMode.NONE -> Unit
        LocationMode.BIAS_RECTANGLE,
        LocationMode.LIMIT_RECTANGLE,
        -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LatLngOutlinedTextField(
                    value = rectNorthEast,
                    onValueChange = onRectNorthEastChange,
                    label = stringResource(R.string.search_label_northeast),
                    modifier = Modifier.weight(1f),
                )
                LatLngOutlinedTextField(
                    value = rectSouthWest,
                    onValueChange = onRectSouthWestChange,
                    label = stringResource(R.string.search_label_southwest),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        LocationMode.BIAS_CIRCLE,
        LocationMode.LIMIT_CIRCLE,
        -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LatLngOutlinedTextField(
                    value = circleCenter,
                    onValueChange = onCircleCenterChange,
                    label = stringResource(R.string.search_label_circle_center),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = circleRadius,
                    onValueChange = onCircleRadiusChange,
                    label = { Text(stringResource(R.string.search_label_radius)) },
                    supportingText = { Text(stringResource(R.string.search_hint_circle_radius), color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("search_location_circle_radius"),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailCard(
    placeDetail: Place?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("place_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.search_detail_title), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.search_close))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (placeDetail != null) {
                PlaceDetailFields(place = placeDetail)
            } else {
                Text(
                    text = stringResource(R.string.search_detail_loading_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailFields(place: Place) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (place.displayName.text.isNotEmpty()) {
            DetailRow(label = stringResource(R.string.search_detail_name), value = place.displayName.text)
        }
        place.formattedAddress?.takeIf { it.isNotEmpty() }?.let {
            DetailRow(label = stringResource(R.string.search_detail_address), value = it)
        }
        place.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
            DetailRow(label = stringResource(R.string.search_detail_phone), value = it)
        }
        if (place.types.isNotEmpty()) {
            DetailRow(label = stringResource(R.string.search_detail_type_label), value = place.types.joinToString(", "))
        }
        place.location?.let { location ->
            DetailRow(
                label = stringResource(R.string.search_detail_coordinate),
                value = "纬度: ${location.latitude}, 经度: ${location.longitude}",
            )
        } ?: DetailRow(
            label = stringResource(R.string.search_detail_coordinate),
            value = stringResource(R.string.search_detail_coordinate_none),
        )
        place.summary?.text?.takeIf { it.isNotEmpty() }?.let {
            DetailRow(label = stringResource(R.string.search_detail_summary), value = it)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
