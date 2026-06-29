@file:OptIn(ExperimentalMaterial3Api::class)

package com.maptec.applied.demo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.maptec.applied.demo.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.LatLngOutlinedTextField
import com.maptec.applied.demo.ui.screens.common.validateLatLng
import com.maptec.applied.demo.ui.screens.search.PlaceDetailBottomSheet
import com.maptec.applied.demo.ui.screens.search.PoiCategoryDropdown
import com.maptec.applied.demo.ui.screens.search.SearchResultsList
import com.maptec.applied.demo.ui.screens.search.SuggestionsList
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import com.maptec.applied.demo.viewmodel.SearchViewModel.LocationMode
import com.maptec.applied.demo.viewmodel.SearchViewModelFactory
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.Suggestion

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(
        factory = remember(context) { SearchViewModelFactory(context.applicationContext) }
    )

    // 暴露给测试，绕过 UI 点击直接切换 tab
    remember { searchScreenViewModel = viewModel }

    val currentTab by viewModel.currentTab.collectAsState()
    val showMap by viewModel.showMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val places by viewModel.places.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showResults by viewModel.showResults.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val placeDetail by viewModel.placeDetail.collectAsState()
    val showPlaceDetail by viewModel.showPlaceDetail.collectAsState()
    val isLoadingPlaceDetail by viewModel.isLoadingPlaceDetail.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var mapLibreMapRef: MaptecMap? by remember { mutableStateOf(null) }
    var mapZoom by remember { mutableStateOf(0.0) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // 列表模式先搜索再切地图：补绘 marker（drawAllMarkers 内部会等 getStyle）
    LaunchedEffect(showMap, places) {
        if (showMap && places.isNotEmpty()) {
            viewModel.drawAllMarkers(fitCameraToResults = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // MapView 创建代价高昂（GL 上下文、瓦片加载），首次显示后常驻组合树，
        // 通过 alpha 切换可见性，避免反复销毁重建导致 ANR
        var mapInitialized by remember { mutableStateOf(false) }
        if (showMap) mapInitialized = true

        if (mapInitialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showMap) 1f else 0f)
            ) {
                MapModeContent(
                    viewModel = viewModel,
                    places = places,
                    mapCenter = mapCenter,
                    showPlaceDetail = showPlaceDetail,
                    placeDetail = placeDetail,
                    isLoadingPlaceDetail = isLoadingPlaceDetail,
                    onMapRefChanged = { mapLibreMapRef = it },
                    onZoomChanged = { mapZoom = it },
                    nearbyRadius = viewModel.nearbyRadius.collectAsState().value,
                    currentTab = currentTab
                )
            }
        }

        if (!showMap) {
            ListModeContent(
                viewModel = viewModel,
                currentTab = currentTab,
                searchQuery = searchQuery,
                isLoading = isLoading,
                showResults = showResults,
                places = places,
                showSuggestions = showSuggestions,
                suggestions = suggestions,
                showPlaceDetail = showPlaceDetail,
                placeDetail = placeDetail,
                isLoadingPlaceDetail = isLoadingPlaceDetail,
                mapLibreMapRef = mapLibreMapRef
            )
        }

        // ========== 悬浮切换按钮 ==========
        FloatingActionButton(
            onClick = { viewModel.toggleMapView() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (showMap) Icons.Default.List else Icons.Default.LocationOn,
                contentDescription = if (showMap) stringResource(R.string.search_list_view) else stringResource(R.string.search_map_view),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** 暴露给测试，绕过 UI 点击直接切换 tab */
@Volatile
var searchScreenViewModel: SearchViewModel? = null

// ==================== 列表模式 ====================

@Composable
private fun ListModeContent(
    viewModel: SearchViewModel,
    currentTab: SearchTab,
    searchQuery: String,
    isLoading: Boolean,
    showResults: Boolean,
    places: List<Place>,
    showSuggestions: Boolean,
    suggestions: List<Suggestion>,
    showPlaceDetail: Boolean,
    placeDetail: Place?,
    isLoadingPlaceDetail: Boolean,
    mapLibreMapRef: MaptecMap?
) {
    val scrollState = rememberScrollState()
    val nextPageToken by viewModel.nextPageToken.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val autoSuggestLimit by viewModel.autoSuggestResultLimit.collectAsState()
    val isLoadingSuggestMore by viewModel.isLoadingSuggestMore.collectAsState()
    val suggestResultLimitString by viewModel.suggestResultLimit.collectAsState()
    val selectedApiName by viewModel.currentTabSelectedApiName.collectAsState()
    val selectedResponseJson by viewModel.currentTabSelectedResponseJson.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Tab 栏
        SearchTabRow(
            currentTab = currentTab,
            onTabSelected = viewModel::switchTab
        )

        // 搜索区域 + 参数
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedResponseJson.isNotBlank()) {
                ApiResponseDebugCard(
                    selectedApiName = selectedApiName,
                    responseJson = selectedResponseJson
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            when (currentTab) {
                SearchTab.TEXT -> TextSearchContent(viewModel, searchQuery, isLoading)
                SearchTab.NEARBY -> NearbySearchContent(
                    viewModel, isLoading, showResults, places, showPlaceDetail,
                    placeDetail, isLoadingPlaceDetail, mapLibreMapRef
                )
                SearchTab.SUGGEST -> SuggestSearchContent(viewModel, searchQuery, isLoading)
                SearchTab.DETAIL -> PlaceDetailTabContent(viewModel, isLoadingPlaceDetail)
            }

            // 文本搜索自动提示
            if (currentTab == SearchTab.TEXT && showSuggestions && suggestions.isNotEmpty() && !showResults) {
                Spacer(modifier = Modifier.height(8.dp))
                val canLoadMoreSuggestions = autoSuggestLimit < 20 && suggestions.size >= autoSuggestLimit
                SuggestionsList(
                    suggestions = suggestions,
                    onSuggestionClick = viewModel::selectSuggestion,
                    canLoadMore = canLoadMoreSuggestions,
                    isLoadingMore = isLoadingSuggestMore,
                    onLoadMore = viewModel::loadMoreAutoSuggest
                )
            }

            // 交互式搜索提示结果
            if (currentTab == SearchTab.SUGGEST && showSuggestions && suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val suggestLimit = suggestResultLimitString.trim().toIntOrNull()?.takeIf { it in 1..20 } ?: 10
                val canLoadMoreSuggestions = suggestLimit < 20 && suggestions.size >= suggestLimit
                SuggestionsList(
                    suggestions = suggestions,
                    onSuggestionClick = viewModel::onSuggestResultClick,
                    canLoadMore = canLoadMoreSuggestions,
                    isLoadingMore = isLoading,
                    onLoadMore = viewModel::loadMoreSuggestSearch
                )
            }

            // 搜索结果列表（文本搜索和交互式搜索，附近搜索已在其内容中展示）
            if (currentTab != SearchTab.NEARBY && showResults && places.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SearchResultsList(
                    places = places,
                    onPlaceClick = { place ->
                        mapLibreMapRef?.let { viewModel.onPlaceClick(place, it) }
                            ?: viewModel.onPlaceClick(place, null)
                    },
                    canLoadMore = currentTab == SearchTab.TEXT && !nextPageToken.isNullOrBlank(),
                    isLoadingMore = isLoadingMore,
                    onLoadMore = viewModel::loadMoreTextSearch
                )
            }

            // 地点详情卡片（文本搜索和交互式搜索，附近搜索已在其内容中展示）
            if (currentTab != SearchTab.NEARBY && showPlaceDetail) {
                Spacer(modifier = Modifier.height(12.dp))
                PlaceDetailCard(
                    placeDetail = placeDetail,
                    isLoading = isLoadingPlaceDetail,
                    onDismiss = viewModel::closePlaceDetail
                )
            }
        }
    }
}

// ==================== 地图模式 ====================

@Composable
private fun MapModeContent(
    viewModel: SearchViewModel,
    places: List<Place>,
    mapCenter: com.maptec.applied.geometry.LatLng?,
    showPlaceDetail: Boolean,
    placeDetail: Place?,
    isLoadingPlaceDetail: Boolean,
    onMapRefChanged: (MaptecMap) -> Unit,
    onZoomChanged: (Double) -> Unit,
    nearbyRadius: String,
    currentTab: SearchTab
) {
    val context = LocalContext.current
    var mapLibreMapRef: MaptecMap? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Mapview(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { mapView, mapLibreMap ->
                mapLibreMapRef = mapLibreMap
                onMapRefChanged(mapLibreMap)

                mapLibreMap.uiSettings.setZoomButtonsEnabled(false)

                viewModel.initMapOverlays(context, mapLibreMap)

                mapLibreMap.addOnCameraMoveListener {
                    val pos = mapLibreMap.cameraPosition
                    viewModel.updateMapCenter(pos.target)
                    onZoomChanged(pos.zoom)
                }

                val initialCenter = mapLibreMap.cameraPosition.target
                viewModel.updateMapCenter(initialCenter)
                onZoomChanged(mapLibreMap.cameraPosition.zoom)

                mapLibreMap.getStyle {
                    viewModel.drawAllMarkers(fitCameraToResults = true)
                    viewModel.drawLocationShape(mapLibreMap)
                }
            }
        )

        if (showPlaceDetail) {
            PlaceDetailBottomSheet(
                place = placeDetail,
                isLoading = isLoadingPlaceDetail,
                onDismiss = { viewModel.closePlaceDetail() }
            )
        }
    }

    LaunchedEffect(places) {
        viewModel.drawAllMarkers(fitCameraToResults = true)
    }

    // 文本搜索位置参数
    val textLocationMode by viewModel.locationMode.collectAsState()
    val textRectNE by viewModel.rectNorthEast.collectAsState()
    val textRectSW by viewModel.rectSouthWest.collectAsState()
    val textCircleCenter by viewModel.circleCenter.collectAsState()
    val textCircleRadius by viewModel.circleRadius.collectAsState()
    // 交互式搜索位置参数
    val suggestLocationMode by viewModel.suggestLocationMode.collectAsState()
    val suggestRectNE by viewModel.suggestRectNorthEast.collectAsState()
    val suggestRectSW by viewModel.suggestRectSouthWest.collectAsState()
    val suggestCircleCenter by viewModel.suggestCircleCenter.collectAsState()
    val suggestCircleRadius by viewModel.suggestCircleRadius.collectAsState()
    // 附近搜索位置参数
    val nearbyCenterString by viewModel.nearbyCenterString.collectAsState()

    LaunchedEffect(
        currentTab, mapCenter,
        nearbyRadius, nearbyCenterString,
        textLocationMode, textRectNE, textRectSW, textCircleCenter, textCircleRadius,
        suggestLocationMode, suggestRectNE, suggestRectSW, suggestCircleCenter, suggestCircleRadius
    ) {
        mapLibreMapRef?.let { map ->
            viewModel.drawLocationShape(map)
        }
    }
}

// ==================== Tab 栏 ====================

@Composable
private fun SearchTabRow(
    currentTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit
) {
    val tabs = SearchTab.entries
    TabRow(selectedTabIndex = tabs.indexOf(currentTab)) {
        tabs.forEach { tab ->
            Tab(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.testTag("search_tab_${tab.name.lowercase()}"),
                text = { Text(tab.label) }
            )
        }
    }
}

// ==================== 文本搜索内容 ====================

@Composable
private fun TextSearchContent(
    viewModel: SearchViewModel,
    searchQuery: String,
    isLoading: Boolean
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
    var locationExpanded by remember { mutableStateOf(false) }

    QueryInputBar(
        searchQuery = searchQuery,
        isLoading = isLoading,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearchClick = { viewModel.performTextSearch(searchQuery) },
        placeholder = stringResource(R.string.search_placeholder)
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = typeString,
            onValueChange = viewModel::setTextType,
            label = { Text(stringResource(R.string.search_label_type)) },
            supportingText = { Text(stringResource(R.string.search_hint_type), color = Color.Gray) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Button(
            onClick = { advancedExpanded = !advancedExpanded },
            modifier = Modifier.heightIn(min = 56.dp)
        ) {
            Text(if (advancedExpanded) stringResource(R.string.search_advanced_less) else stringResource(R.string.search_advanced_more))
        }
    }

    if (advancedExpanded) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = languageString,
                onValueChange = viewModel::setLanguageString,
                label = { Text(stringResource(R.string.search_label_language)) },
                supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_input_language"),
                singleLine = true
            )
            OutlinedTextField(
                value = regionString,
                onValueChange = viewModel::setRegionString,
                label = { Text(stringResource(R.string.search_label_region)) },
                supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_input_region"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = pageSizeString,
                onValueChange = viewModel::setPageSizeString,
                label = { Text(stringResource(R.string.search_label_pageSize)) },
                supportingText = { Text(stringResource(R.string.search_hint_page_size), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_input_page_size"),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = rankExpanded,
                onExpandedChange = { rankExpanded = !rankExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = rankType?.name ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.search_label_rank)) },
                    supportingText = { Text(stringResource(R.string.search_hint_rank), color = Color.Gray) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = rankExpanded,
                    onDismissRequest = { rankExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("不设置") },
                        onClick = { viewModel.setRankType(null); rankExpanded = false }
                    )
                    RankType.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = { viewModel.setRankType(item); rankExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pageTokenString,
            onValueChange = viewModel::setPageTokenString,
            label = { Text(stringResource(R.string.search_label_pageToken)) },
            supportingText = { Text(stringResource(R.string.search_hint_page_token), color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
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
            onCircleRadiusChange = viewModel::setCircleRadius
        )
    }
}

// ==================== 附近搜索内容 ====================

@Composable
private fun NearbySearchContent(
    viewModel: SearchViewModel,
    isLoading: Boolean,
    showResults: Boolean,
    places: List<Place>,
    showPlaceDetail: Boolean,
    placeDetail: Place?,
    isLoadingPlaceDetail: Boolean,
    mapLibreMapRef: MaptecMap?
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

    Text(
        text = stringResource(R.string.search_nearby_tip),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LatLngOutlinedTextField(
            value = nearbyCenterString,
            onValueChange = viewModel::setNearbyCenterString,
            label = stringResource(R.string.search_label_center),
            hint = stringResource(R.string.search_center_hint),
            required = false,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = nearbyRadius,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    viewModel.setNearbyRadius(newValue)
                }
            },
            modifier = Modifier.weight(1f).testTag("search_nearby_radius"),
            placeholder = { Text(stringResource(R.string.search_radius_placeholder)) },
            label = { Text(stringResource(R.string.search_label_radius)) },
            singleLine = true,
            isError = nearbyRadius.isNotEmpty() && (nearbyRadius.toIntOrNull() == null || nearbyRadius.toIntOrNull() !in 0..50000),
            supportingText = {
                if (nearbyRadius.isNotEmpty()) {
                    val v = nearbyRadius.toIntOrNull()
                    when {
                        v == null -> Text(stringResource(R.string.search_error_invalid_number), color = MaterialTheme.colorScheme.error)
                        v !in 0..50000 -> Text(stringResource(R.string.search_error_range_0_50000), color = MaterialTheme.colorScheme.error)
                        else -> null
                    }
                } else null
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        PoiCategoryDropdown(
            selectedValue = nearbyTypes,
            onValueChange = viewModel::setNearbyTypes,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = { viewModel.performNearbySearch() },
            enabled = !isLoading
                && nearbyRadius.isNotEmpty()
                && nearbyRadius.toIntOrNull()?.let { it in 0..50000 } == true
                && (nearbyCenterString.isBlank() || validateLatLng(nearbyCenterString, required = false) == null),
            modifier = Modifier.testTag("search_nearby_submit_button"),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_action_search))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.search_action_search))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { advancedExpanded = !advancedExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (advancedExpanded) stringResource(R.string.search_advanced_less) else stringResource(R.string.search_advanced_more))
    }

    if (advancedExpanded) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nearbyResultLimit,
                onValueChange = viewModel::setNearbyResultLimit,
                label = { Text(stringResource(R.string.search_label_result_limit)) },
                supportingText = { Text(stringResource(R.string.search_hint_result_limit), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_nearby_result_limit"),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = rankExpanded,
                onExpandedChange = { rankExpanded = !rankExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = nearbyRankType?.name ?: "",
                    onValueChange = { },
                    label = { Text("rank") },
                    supportingText = { Text("POPULARITY 默认", color = Color.Gray) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = rankExpanded,
                    onDismissRequest = { rankExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("不设置") },
                        onClick = { viewModel.setNearbyRankType(null); rankExpanded = false }
                    )
                    RankType.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = { viewModel.setNearbyRankType(item); rankExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = languageString,
                onValueChange = viewModel::setLanguageString,
                label = { Text(stringResource(R.string.search_label_language)) },
                supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_input_language"),
                singleLine = true
            )
            OutlinedTextField(
                value = regionString,
                onValueChange = viewModel::setRegionString,
                label = { Text(stringResource(R.string.search_label_region)) },
                supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }

    // 附近搜索结果列表
    if (showResults && places.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        SearchResultsList(
            places = places,
            onPlaceClick = { place ->
                mapLibreMapRef?.let { viewModel.onPlaceClick(place, it) }
                    ?: viewModel.onPlaceClick(place, null)
            }
        )
    }

    // 附近搜索地点详情
    if (showPlaceDetail) {
        Spacer(modifier = Modifier.height(12.dp))
        PlaceDetailCard(
            placeDetail = placeDetail,
            isLoading = isLoadingPlaceDetail,
            onDismiss = viewModel::closePlaceDetail
        )
    }
}

// ==================== 交互式搜索内容 ====================

@Composable
private fun SuggestSearchContent(
    viewModel: SearchViewModel,
    searchQuery: String,
    isLoading: Boolean
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
        placeholder = stringResource(R.string.search_suggest_placeholder)
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = suggestTypes,
            onValueChange = viewModel::setSuggestTypes,
            label = { Text(stringResource(R.string.search_label_suggest_types)) },
            supportingText = { Text(stringResource(R.string.search_hint_suggest_types), color = Color.Gray) },
            modifier = Modifier.weight(1f).testTag("search_input_suggest_types"),
            singleLine = true
        )
        Button(
            onClick = { advancedExpanded = !advancedExpanded },
            modifier = Modifier.heightIn(min = 56.dp)
        ) {
            Text(if (advancedExpanded) stringResource(R.string.search_advanced_less) else stringResource(R.string.search_advanced_more))
        }
    }

    if (advancedExpanded) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = suggestResultLimit,
                onValueChange = viewModel::setSuggestResultLimit,
                label = { Text(stringResource(R.string.search_label_result_limit)) },
                supportingText = { Text(stringResource(R.string.search_hint_result_limit), color = Color.Gray) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = languageString,
                onValueChange = viewModel::setLanguageString,
                label = { Text(stringResource(R.string.search_label_language)) },
                supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_input_language"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = regionString,
            onValueChange = viewModel::setRegionString,
            label = { Text(stringResource(R.string.search_label_region)) },
            supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
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
            onCircleRadiusChange = viewModel::setSuggestCircleRadius
        )
    }
}

// ==================== 地点详情内容 ====================

@Composable
private fun PlaceDetailTabContent(
    viewModel: SearchViewModel,
    isLoadingPlaceDetail: Boolean
) {
    val placeId by viewModel.detailPlaceId.collectAsState()
    val languageString by viewModel.languageString.collectAsState()
    val regionString by viewModel.regionString.collectAsState()

    OutlinedTextField(
        value = placeId,
        onValueChange = viewModel::setDetailPlaceId,
        label = { Text(stringResource(R.string.search_label_place_id)) },
        supportingText = { Text(stringResource(R.string.search_hint_place_id), color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().testTag("search_input_place_id"),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = languageString,
            onValueChange = viewModel::setLanguageString,
            label = { Text(stringResource(R.string.search_label_language)) },
            supportingText = { Text(stringResource(R.string.search_hint_language), color = Color.Gray) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = regionString,
            onValueChange = viewModel::setRegionString,
            label = { Text(stringResource(R.string.search_label_region)) },
            supportingText = { Text(stringResource(R.string.search_hint_region), color = Color.Gray) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = viewModel::performPlaceDetailFromTab,
        enabled = !isLoadingPlaceDetail,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isLoadingPlaceDetail) stringResource(R.string.search_loading) else stringResource(R.string.search_action_get_detail))
    }

}

// ==================== 通用组件 ====================

@Composable
private fun QueryInputBar(
    searchQuery: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    placeholder: String
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
        verticalAlignment = Alignment.CenterVertically
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
            singleLine = true
        )
        Button(
            onClick = onSearchClick,
            enabled = !isLoading && searchQuery.isNotBlank(),
            modifier = Modifier.testTag("search_submit_button"),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
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
    onCircleRadiusChange: (String) -> Unit
) {
    var locationExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = locationExpanded,
        onExpandedChange = { locationExpanded = !locationExpanded },
        modifier = Modifier.fillMaxWidth()
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
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = locationExpanded,
            onDismissRequest = { locationExpanded = false }
        ) {
            LocationMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onLocationModeChange(mode)
                        locationExpanded = false
                    }
                )
            }
        }
    }

    when (locationMode) {
        LocationMode.NONE -> Unit
        LocationMode.BIAS_RECTANGLE,
        LocationMode.LIMIT_RECTANGLE -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LatLngOutlinedTextField(
                    value = rectNorthEast,
                    onValueChange = onRectNorthEastChange,
                    label = stringResource(R.string.search_label_northeast),
                    modifier = Modifier.weight(1f)
                )
                LatLngOutlinedTextField(
                    value = rectSouthWest,
                    onValueChange = onRectSouthWestChange,
                    label = stringResource(R.string.search_label_southwest),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        LocationMode.BIAS_CIRCLE,
        LocationMode.LIMIT_CIRCLE -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LatLngOutlinedTextField(
                    value = circleCenter,
                    onValueChange = onCircleCenterChange,
                    label = stringResource(R.string.search_label_circle_center),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = circleRadius,
                    onValueChange = onCircleRadiusChange,
                    label = { Text(stringResource(R.string.search_label_radius)) },
                    supportingText = { Text(stringResource(R.string.search_hint_circle_radius), color = Color.Gray) },
                modifier = Modifier.weight(1f).testTag("search_location_circle_radius"),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailCard(
    placeDetail: Place?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("place_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (placeDetail != null) {
                PlaceDetailFields(place = placeDetail)
            } else {
                Text(
                    text = stringResource(R.string.search_detail_loading_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlaceDetailFields(place: Place) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                value = "纬度: ${location.latitude}, 经度: ${location.longitude}"
            )
        } ?: DetailRow(label = stringResource(R.string.search_detail_coordinate), value = stringResource(R.string.search_detail_coordinate_none))
        place.summary?.text?.takeIf { it.isNotEmpty() }?.let {
            DetailRow(label = stringResource(R.string.search_detail_summary), value = it)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ApiResponseDebugCard(
    selectedApiName: String,
    responseJson: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("api_response_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${stringResource(R.string.search_api_response_title)}($selectedApiName)", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) stringResource(R.string.search_action_collapse) else stringResource(R.string.search_action_expand))
                }
            }
            if (expanded) {
                val scroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(scroll)
                ) {
                    Text(
                        text = responseJson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
