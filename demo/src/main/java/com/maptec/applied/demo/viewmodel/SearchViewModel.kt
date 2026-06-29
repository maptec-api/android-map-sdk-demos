package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.R
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.NearbySearchRequest
import com.maptec.applied.search.model.request.SuggestRequest
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.PhotoResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.Suggestion
import com.maptec.applied.search.service.SearchService
import com.google.gson.GsonBuilder
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.style.layers.Property
import com.maptec.applied.utils.BitmapUtils
import android.graphics.Bitmap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchService: SearchService
) : ViewModel() {

    private val prettyGson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    // ==================== Tab / 视图模式 ====================

    enum class SearchTab(val label: String) {
        TEXT("文本搜索"),
        NEARBY("附近搜索"),
        SUGGEST("交互式搜索"),
        DETAIL("地点详情")
    }

    private val _currentTab = MutableStateFlow(SearchTab.TEXT)
    val currentTab: StateFlow<SearchTab> = _currentTab.asStateFlow()

    private val _tabApiResponses = MutableStateFlow<Map<SearchTab, Map<String, String>>>(emptyMap())

    private val _tabSelectedApiName = MutableStateFlow(
        SearchTab.entries.associateWith { tab -> TAB_API_NAMES[tab]?.firstOrNull().orEmpty() }
    )

    val currentTabSelectedApiName: StateFlow<String> =
        combine(_currentTab, _tabSelectedApiName) { tab, selected -> selected[tab].orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val currentTabSelectedResponseJson: StateFlow<String> =
        combine(_currentTab, currentTabSelectedApiName, _tabApiResponses) { tab, apiName, responses ->
            responses[tab]?.get(apiName).orEmpty()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    data class ApiSnapshot(
        val apiName: String,
        val responseJson: String
    )

    private val _placeDetailApiSnapshot = MutableStateFlow<ApiSnapshot?>(null)
    val placeDetailApiSnapshot: StateFlow<ApiSnapshot?> = _placeDetailApiSnapshot.asStateFlow()

    private val _showMap = MutableStateFlow(false)
    val showMap: StateFlow<Boolean> = _showMap.asStateFlow()

    fun switchTab(tab: SearchTab) {
        _currentTab.value = tab
        _showResults.value = false
        _showSuggestions.value = false
        _showPlaceDetail.value = false
    }


    private fun saveTabApiResponse(tab: SearchTab, apiName: String, response: Any) {
        val json = prettyGson.toJson(response)
        val tabMap = _tabApiResponses.value[tab].orEmpty()
        _tabApiResponses.value = _tabApiResponses.value + (tab to (tabMap + (apiName to json)))
        _tabSelectedApiName.value = _tabSelectedApiName.value + (tab to apiName)
    }

    private fun saveTabApiError(tab: SearchTab, apiName: String, message: String) {
        val json = """{"status":"ERROR","error":"${message.replace("\"", "\\\"")}"}"""
        val tabMap = _tabApiResponses.value[tab].orEmpty()
        _tabApiResponses.value = _tabApiResponses.value + (tab to (tabMap + (apiName to json)))
        _tabSelectedApiName.value = _tabSelectedApiName.value + (tab to apiName)
    }

    fun toggleMapView() {
        _showMap.value = !_showMap.value
    }

    // ==================== 通用查询 ====================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _languageString = MutableStateFlow("en")
    val languageString: StateFlow<String> = _languageString.asStateFlow()

    private val _regionString = MutableStateFlow("")
    val regionString: StateFlow<String> = _regionString.asStateFlow()

    // ==================== 地点详情参数 ====================

    private val _detailPlaceId = MutableStateFlow("")
    val detailPlaceId: StateFlow<String> = _detailPlaceId.asStateFlow()

    // ==================== 文本搜索参数 ====================

    private val _textType = MutableStateFlow("")
    val textType: StateFlow<String> = _textType.asStateFlow()

    private val _pageSizeString = MutableStateFlow("")
    val pageSizeString: StateFlow<String> = _pageSizeString.asStateFlow()

    private val _pageTokenString = MutableStateFlow("")
    val pageTokenString: StateFlow<String> = _pageTokenString.asStateFlow()

    private val _rankType = MutableStateFlow<RankType?>(null)
    val rankType: StateFlow<RankType?> = _rankType.asStateFlow()

    private val _locationMode = MutableStateFlow(LocationMode.NONE)
    val locationMode: StateFlow<LocationMode> = _locationMode.asStateFlow()

    private val _rectNorthEast = MutableStateFlow("")
    val rectNorthEast: StateFlow<String> = _rectNorthEast.asStateFlow()

    private val _rectSouthWest = MutableStateFlow("")
    val rectSouthWest: StateFlow<String> = _rectSouthWest.asStateFlow()

    private val _circleCenter = MutableStateFlow("")
    val circleCenter: StateFlow<String> = _circleCenter.asStateFlow()

    private val _circleRadius = MutableStateFlow("")
    val circleRadius: StateFlow<String> = _circleRadius.asStateFlow()

    // ==================== 附近搜索参数 ====================

    private val _nearbyCenterString = MutableStateFlow("1.4,103.75")
    val nearbyCenterString: StateFlow<String> = _nearbyCenterString.asStateFlow()

    private val _nearbyTypes = MutableStateFlow("")
    val nearbyTypes: StateFlow<String> = _nearbyTypes.asStateFlow()

    private val _nearbyRadius = MutableStateFlow("")
    val nearbyRadius: StateFlow<String> = _nearbyRadius.asStateFlow()

    private val _nearbyResultLimit = MutableStateFlow("")
    val nearbyResultLimit: StateFlow<String> = _nearbyResultLimit.asStateFlow()

    private val _nearbyRankType = MutableStateFlow<RankType?>(RankType.DISTANCE)
    val nearbyRankType: StateFlow<RankType?> = _nearbyRankType.asStateFlow()

    // ==================== 交互式搜索参数 ====================

    private val _suggestTypes = MutableStateFlow("")
    val suggestTypes: StateFlow<String> = _suggestTypes.asStateFlow()

    private val _suggestResultLimit = MutableStateFlow("")
    val suggestResultLimit: StateFlow<String> = _suggestResultLimit.asStateFlow()

    private val _suggestLocationMode = MutableStateFlow(LocationMode.NONE)
    val suggestLocationMode: StateFlow<LocationMode> = _suggestLocationMode.asStateFlow()

    private val _suggestRectNorthEast = MutableStateFlow("")
    val suggestRectNorthEast: StateFlow<String> = _suggestRectNorthEast.asStateFlow()

    private val _suggestRectSouthWest = MutableStateFlow("")
    val suggestRectSouthWest: StateFlow<String> = _suggestRectSouthWest.asStateFlow()

    private val _suggestCircleCenter = MutableStateFlow("")
    val suggestCircleCenter: StateFlow<String> = _suggestCircleCenter.asStateFlow()

    private val _suggestCircleRadius = MutableStateFlow("")
    val suggestCircleRadius: StateFlow<String> = _suggestCircleRadius.asStateFlow()

    // ==================== 搜索结果 ====================

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    private val _nextPageToken = MutableStateFlow<String?>(null)
    val nextPageToken: StateFlow<String?> = _nextPageToken.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _autoSuggestResultLimit = MutableStateFlow(10)
    val autoSuggestResultLimit: StateFlow<Int> = _autoSuggestResultLimit.asStateFlow()

    private val _isLoadingSuggestMore = MutableStateFlow(false)
    val isLoadingSuggestMore: StateFlow<Boolean> = _isLoadingSuggestMore.asStateFlow()

    // ==================== Toast 消息 ====================

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    // ==================== 地点详情 ====================

    private val _placeDetail = MutableStateFlow<Place?>(null)
    val placeDetail: StateFlow<Place?> = _placeDetail.asStateFlow()

    private val _showPlaceDetail = MutableStateFlow(false)
    val showPlaceDetail: StateFlow<Boolean> = _showPlaceDetail.asStateFlow()

    private val _isLoadingPlaceDetail = MutableStateFlow(false)
    val isLoadingPlaceDetail: StateFlow<Boolean> = _isLoadingPlaceDetail.asStateFlow()

    // ==================== 地图相关 ====================

    private val _mapCenter = MutableStateFlow<LatLng?>(null)
    val mapCenter: StateFlow<LatLng?> = _mapCenter.asStateFlow()

    private var mapLibreMapRef: MaptecMap? = null
    private var currentCircle: Circle? = null
    private var currentFill: Fill? = null
    private val markerIconBitmaps = mutableMapOf<String, Pair<Bitmap, Boolean>>()
    private val _placeMarkerMap = MutableStateFlow<MutableMap<String, Marker>>(mutableMapOf())
    private val _selectedPlaceId = MutableStateFlow<String?>(null)

    private var searchJob: Job? = null
    private var suggestJob: Job? = null
    private var placeDetailJob: Job? = null
    private var isProgrammaticSearch = false
    private var overlayInitialized = false
    private var lastTextQuery: String? = null

    // ==================== Setter 方法 ====================

    fun setSearchQuery(value: String) { _searchQuery.value = value }
    fun setLanguageString(value: String) { _languageString.value = value }
    fun setRegionString(value: String) { _regionString.value = value }
    fun setTextType(value: String) { _textType.value = value }
    fun setPageSizeString(value: String) { _pageSizeString.value = value }
    fun setPageTokenString(value: String) { _pageTokenString.value = value }
    fun setRankType(value: RankType?) { _rankType.value = value }
    fun setLocationMode(value: LocationMode) { _locationMode.value = value }
    fun setRectNorthEast(value: String) { _rectNorthEast.value = value }
    fun setRectSouthWest(value: String) { _rectSouthWest.value = value }
    fun setCircleCenter(value: String) { _circleCenter.value = value }
    fun setCircleRadius(value: String) { _circleRadius.value = value }
    fun setNearbyCenterString(value: String) { _nearbyCenterString.value = value }
    fun setNearbyTypes(value: String) { _nearbyTypes.value = value }
    fun setNearbyRadius(value: String) { _nearbyRadius.value = value }
    fun setNearbyResultLimit(value: String) { _nearbyResultLimit.value = value }
    fun setNearbyRankType(value: RankType?) { _nearbyRankType.value = value }
    fun setSuggestTypes(value: String) { _suggestTypes.value = value }
    fun setSuggestResultLimit(value: String) { _suggestResultLimit.value = value }
    fun setSuggestLocationMode(value: LocationMode) { _suggestLocationMode.value = value }
    fun setSuggestRectNorthEast(value: String) { _suggestRectNorthEast.value = value }
    fun setSuggestRectSouthWest(value: String) { _suggestRectSouthWest.value = value }
    fun setSuggestCircleCenter(value: String) { _suggestCircleCenter.value = value }
    fun setSuggestCircleRadius(value: String) { _suggestCircleRadius.value = value }
    fun setDetailPlaceId(value: String) { _detailPlaceId.value = value }

    // ==================== 图片 ====================

    private val _photoName = MutableStateFlow("")
    val photoName: StateFlow<String> = _photoName.asStateFlow()

    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri: StateFlow<String?> = _photoUri.asStateFlow()

    private val _isLoadingPhoto = MutableStateFlow(false)
    val isLoadingPhoto: StateFlow<Boolean> = _isLoadingPhoto.asStateFlow()

    fun setPhotoName(value: String) { _photoName.value = value }

    fun performPhotoSearch() {
        val name = _photoName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            _isLoadingPhoto.value = true
            _photoUri.value = null
            try {
                val result = searchService.getPhoto(name, maxWidthPx = 400, maxHeightPx = 400)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.DETAIL, "photo", response)
                    if (response.status == ResponseStatus.OK) {
                        _photoUri.value = response.photoData?.photoUri
                    } else {
                        _toastMessage.value = "获取图片失败: ${response.error?.message ?: response.status.name}"
                    }
                    _isLoadingPhoto.value = false
                }.onFailure {
                    _isLoadingPhoto.value = false
                    _toastMessage.value = "获取图片失败: ${it.message}"
                }
            } catch (e: Exception) {
                _isLoadingPhoto.value = false
                _toastMessage.value = "获取图片异常: ${e.message}"
            }
        }
    }

    // ==================== 搜索输入变化（含防抖 suggest） ====================

    fun onSearchQueryChange(newValue: String) {
        _searchQuery.value = newValue
        if (isProgrammaticSearch) return

        searchJob?.cancel()
        suggestJob?.cancel()

        if (newValue.isBlank()) {
            _suggestions.value = emptyList()
            _showSuggestions.value = false
            _places.value = emptyList()
            _showResults.value = false
            _nextPageToken.value = null
            _isLoadingMore.value = false
            _autoSuggestResultLimit.value = 10
            _isLoadingSuggestMore.value = false
        } else if (_currentTab.value == SearchTab.TEXT) {
            _autoSuggestResultLimit.value = 10
            _isLoadingSuggestMore.value = false
            suggestJob = viewModelScope.launch {
                delay(SUGGEST_DEBOUNCE_DELAY_MS)
                performAutoSuggest(newValue)
            }
        }
    }

    private fun performAutoSuggest(query: String) {
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            _showSuggestions.value = false
            return
        }
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            if (_autoSuggestResultLimit.value > 10) {
                _isLoadingSuggestMore.value = true
            }
            try {
                val request = SuggestRequest(
                    query = query,
                    resultLimit = _autoSuggestResultLimit.value,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE }
                )
                val result = searchService.suggest(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.TEXT, "autoSuggest", response)
                    if (response.status != ResponseStatus.OK) {
                        log.e { "autoSuggest: status: ${response.status}" }
                        response.error?.let { err ->
                            _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                        }
                        _suggestions.value = emptyList()
                        _showSuggestions.value = false
                        return@launch
                    }
                    val list = response.suggestions ?: emptyList()
                    _suggestions.value = list
                    _showSuggestions.value = list.isNotEmpty()
                    _isLoadingSuggestMore.value = false
                }.onFailure {
                    _suggestions.value = emptyList()
                    _showSuggestions.value = false
                    _isLoadingSuggestMore.value = false
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
                _showSuggestions.value = false
                _isLoadingSuggestMore.value = false
            }
        }
    }

    fun loadMoreAutoSuggest() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        if (_isLoadingSuggestMore.value) return
        if (_autoSuggestResultLimit.value >= 20) return
        _autoSuggestResultLimit.value = (_autoSuggestResultLimit.value + 10).coerceAtMost(20)
        performAutoSuggest(query)
    }

    fun selectSuggestion(suggestion: Suggestion) {
        isProgrammaticSearch = true
        val queryText = suggestion.text.text
        _searchQuery.value = queryText
        _showSuggestions.value = false
        suggestJob?.cancel()
        performTextSearch(queryText)
        viewModelScope.launch {
            delay(100)
            isProgrammaticSearch = false
        }
    }

    // ==================== 文本搜索 ====================

    fun performTextSearch(query: String) {
        if (query.isBlank()) {
            _places.value = emptyList()
            _showResults.value = false
            _nextPageToken.value = null
            _isLoadingMore.value = false
            return
        }
        searchJob?.cancel()
        _showSuggestions.value = false
        _suggestions.value = emptyList()
        _nextPageToken.value = null
        _isLoadingMore.value = false
        lastTextQuery = query

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val loc = buildLocation(_locationMode.value, _rectNorthEast.value, _rectSouthWest.value, _circleCenter.value, _circleRadius.value)
                val request = TextSearchRequest(
                    query = query,
                    type = _textType.value.trim().ifEmpty { null },
                    locationBias = loc.locationBias,
                    locationLimit = loc.locationLimit,
                    pageSize = _pageSizeString.value.trim().toIntOrNull()?.takeIf { it in 1..20 },
                    pageToken = _pageTokenString.value.trim().ifEmpty { null },
                    rank = _rankType.value,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.textSearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.TEXT, "textSearch", response)
                    if (response.status != ResponseStatus.OK) {
                        log.e { "textSearch: status: ${response.status}" }
                        _toastMessage.value = "文本搜索，服务器返回错误:${response.error?.code} ${response.error?.message ?: response.status.name}"
                        _places.value = emptyList()
                        _showResults.value = false
                        _isLoading.value = false
                        _nextPageToken.value = null
                        return@launch
                    }
                    val list = response.places ?: emptyList()
                    _places.value = list
                    _showResults.value = list.isNotEmpty()
                    _isLoading.value = false
                    _nextPageToken.value = response.nextPageToken?.trim()?.takeIf { it.isNotEmpty() }
                    drawAllMarkers(fitCameraToResults = true)
                }.onFailure {
                    _places.value = emptyList()
                    _showResults.value = false
                    _isLoading.value = false
                    _nextPageToken.value = null
                    val message = it.message ?: "未知错误"
                    _toastMessage.value = "文本搜索请求失败: $message"
                    saveTabApiError(SearchTab.TEXT, "textSearch", message)
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _places.value = emptyList()
                _showResults.value = false
                _isLoading.value = false
                _nextPageToken.value = null
                val message = e.message ?: "未知错误"
                _toastMessage.value = "文本搜索异常: $message"
                saveTabApiError(SearchTab.TEXT, "textSearch", message)
            }
        }
    }

    fun loadMoreTextSearch() {
        val token = _nextPageToken.value?.trim().orEmpty()
        if (token.isEmpty()) return
        if (_isLoadingMore.value) return
        val query = lastTextQuery ?: _searchQuery.value
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val loc = buildLocation(_locationMode.value, _rectNorthEast.value, _rectSouthWest.value, _circleCenter.value, _circleRadius.value)
                val request = TextSearchRequest(
                    query = query,
                    type = _textType.value.trim().ifEmpty { null },
                    locationBias = loc.locationBias,
                    locationLimit = loc.locationLimit,
                    pageSize = _pageSizeString.value.trim().toIntOrNull()?.takeIf { it in 1..20 },
                    pageToken = token,
                    rank = _rankType.value,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.textSearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.TEXT, "textSearch(loadMore)", response)
                    if (response.status != ResponseStatus.OK) {
                        response.error?.let { err ->
                            _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                        }
                        _isLoadingMore.value = false
                        return@launch
                    }
                    val list = response.places ?: emptyList()
                    _places.value = _places.value + list
                    _showResults.value = _places.value.isNotEmpty()
                    _nextPageToken.value = response.nextPageToken?.trim()?.takeIf { it.isNotEmpty() }
                    _isLoadingMore.value = false
                    drawAllMarkers(fitCameraToResults = true)
                }.onFailure {
                    _isLoadingMore.value = false
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
            }
        }
    }

    // ==================== 附近搜索 ====================

    fun performNearbySearch() {
        val radiusValue = _nearbyRadius.value.trim().toIntOrNull() ?: return
        if (radiusValue !in 0..50000) return

        val inputCenter = parseCoordinates(_nearbyCenterString.value)
        val coords = if (inputCenter != null) {
            inputCenter
        } else {
            val mc = _mapCenter.value ?: return
            GeoCoordinate(longitude = mc.longitude, latitude = mc.latitude)
        }

        searchJob?.cancel()
        _showResults.value = false

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val locationLimit = LocationLimit.Circle(
                    center = coords,
                    radius = radiusValue
                )
                val request = NearbySearchRequest(
                    locationLimit = locationLimit,
                    types = _nearbyTypes.value.trim().ifEmpty { null },
                    resultLimit = _nearbyResultLimit.value.trim().toIntOrNull()?.takeIf { it in 1..20 },
                    rank = _nearbyRankType.value,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.nearbySearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.NEARBY, "nearbySearch", response)
                    if (response.status != ResponseStatus.OK) {
                        log.e { "nearbySearch: status: ${response.status}" }
                        _toastMessage.value = "附近搜索失败，服务器返回错误:${response.error?.code}: ${response.error?.message ?: response.status.name}"
                        _places.value = emptyList()
                        _showResults.value = false
                        _isLoading.value = false
                        return@launch
                    }
                    val list = response.places ?: emptyList()
                    _places.value = list
                    _showResults.value = list.isNotEmpty()
                    _isLoading.value = false
                    drawAllMarkers(fitCameraToResults = true)
                }.onFailure {
                    _places.value = emptyList()
                    _showResults.value = false
                    _isLoading.value = false
                    val message = it.message ?: "未知错误"
                    _toastMessage.value = "附近搜索请求失败: $message"
                    saveTabApiError(SearchTab.NEARBY, "nearbySearch", message)
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _places.value = emptyList()
                _showResults.value = false
                _isLoading.value = false
                val message = e.message ?: "未知错误"
                _toastMessage.value = "附近搜索异常: $message"
                saveTabApiError(SearchTab.NEARBY, "nearbySearch", message)
            }
        }
    }

    // ==================== 交互式搜索 ====================

    fun performSuggestSearch(query: String) {
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            _showSuggestions.value = false
            return
        }
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val loc = buildLocation(_suggestLocationMode.value, _suggestRectNorthEast.value, _suggestRectSouthWest.value, _suggestCircleCenter.value, _suggestCircleRadius.value)
                val request = SuggestRequest(
                    query = query,
                    types = _suggestTypes.value.trim().ifEmpty { null },
                    locationBias = loc.locationBias,
                    locationLimit = loc.locationLimit,
                    resultLimit = _suggestResultLimit.value.trim().toIntOrNull()?.takeIf { it in 1..20 } ?: 10,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.suggest(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.SUGGEST, "suggestSearch", response)
                    if (response.status != ResponseStatus.OK) {
                        log.e { "suggestSearch: status: ${response.status}" }
                        _toastMessage.value = "交互式搜索失败，服务器返回错误:${response.error?.code}: ${response.error?.message ?: response.status.name}"
                        _suggestions.value = emptyList()
                        _showSuggestions.value = false
                        _isLoading.value = false
                        return@launch
                    }
                    val list = response.suggestions ?: emptyList()
                    _suggestions.value = list
                    _showSuggestions.value = list.isNotEmpty()
                    _showResults.value = false
                    _isLoading.value = false
                }.onFailure {
                    _suggestions.value = emptyList()
                    _showSuggestions.value = false
                    _isLoading.value = false
                    _toastMessage.value = "交互式搜索请求失败: ${it.message ?: "未知错误"}"
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _suggestions.value = emptyList()
                _showSuggestions.value = false
                _isLoading.value = false
                _toastMessage.value = "交互式搜索异常: ${e.message ?: "未知错误"}"
            }
        }
    }

    fun loadMoreSuggestSearch() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        if (_isLoading.value) return
        val current = _suggestResultLimit.value.trim().toIntOrNull()?.takeIf { it in 1..20 } ?: 10
        if (current >= 20) return
        _suggestResultLimit.value = (current + 10).coerceAtMost(20).toString()
        performSuggestSearch(query)
    }

    fun onSuggestResultClick(suggestion: Suggestion) {
        val placeId = suggestion.placeId
        if (!placeId.isNullOrBlank()) {
            loadPlaceDetail(placeId)
        }
    }

    // ==================== 地点详情 ====================

    private fun loadPlaceDetail(placeId: String) {
        placeDetailJob?.cancel()
        _isLoadingPlaceDetail.value = true
        _showPlaceDetail.value = true

        placeDetailJob = viewModelScope.launch {
            try {
                val result = searchService.getPlaceDetail(
                    placeId = placeId,
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null }
                )
                result.onSuccess { response ->
                    val snapshot = ApiSnapshot(apiName = "placeDetail", responseJson = prettyGson.toJson(response))
                    _placeDetailApiSnapshot.value = snapshot
                    saveTabApiResponse(SearchTab.DETAIL, "placeDetail", response)
                    if (response.status != ResponseStatus.OK) {
                        log.e { "placeDetail: status: ${response.status}" }
                        _toastMessage.value = "获取地点详情失败，服务器返回错误:${response.error?.code}: ${response.error?.message ?: response.status.name}"
                        _placeDetail.value = null
                        _isLoadingPlaceDetail.value = false
                        return@launch
                    }
                    _placeDetail.value = response.places
                    _isLoadingPlaceDetail.value = false
                }.onFailure {
                    _placeDetail.value = null
                    _isLoadingPlaceDetail.value = false
                    val message = it.message ?: "未知错误"
                    _toastMessage.value = "获取地点详情请求失败: $message"
                    saveTabApiError(SearchTab.DETAIL, "placeDetail", message)
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _placeDetail.value = null
                _isLoadingPlaceDetail.value = false
                val message = e.message ?: "未知错误"
                _toastMessage.value = "获取地点详情异常: $message"
                saveTabApiError(SearchTab.DETAIL, "placeDetail", message)
            }
        }
    }

    fun performPlaceDetailFromTab() {
        val placeId = _detailPlaceId.value.trim()
        if (placeId.isEmpty()) {
            _toastMessage.value = "placeId 不能为空"
            return
        }
        loadPlaceDetail(placeId)
    }

    fun closePlaceDetail() {
        _showPlaceDetail.value = false
        _placeDetail.value = null
    }

    // ==================== 地图交互 ====================

    fun onPlaceClick(place: Place, map: MaptecMap?) {
        val location = place.location
        if (location == null) {
            _toastMessage.value = "地点经纬度坐标为空"
            return
        }
        val latLng = LatLng(location.latitude, location.longitude)
//        map?.let { selectMarker(it, latLng, place.id) }
        selectMarkerFormList(place.id)
        _showResults.value = false
        _showSuggestions.value = false
    }

    fun initMapOverlays(context: Context, mapLibreMap: MaptecMap) {
        mapLibreMapRef = mapLibreMap
        if (overlayInitialized) return
        fun cacheMarkerIcon(drawableRes: Int, iconId: String) {
            val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return
            val bitmap = BitmapUtils.getBitmapFromDrawable(drawable) ?: return
            markerIconBitmaps[iconId] = bitmap to false
        }
        cacheMarkerIcon(R.drawable.marker_default, MARKER_ICON_ID)
        cacheMarkerIcon(R.drawable.marker_select, MARKER_SELECT_ICON_ID)
        overlayInitialized = true
    }

    fun updateMapCenter(center: LatLng?) {
//        _mapCenter.value = center
        if (center != null) {
            val newCenterString = "${center.latitude},${center.longitude}"
            if (_nearbyCenterString.value != newCenterString) {
//                _nearbyCenterString.value = newCenterString
            }
        }
    }

    fun drawAllMarkers(fitCameraToResults: Boolean = false) {
        val map = mapLibreMapRef ?: return
        // Overlay Marker 依赖地图 Style / Native 就绪，须等 getStyle 回调后再 addMarker
        map.getStyle { drawAllMarkersOnMapReady(map, fitCameraToResults) }
    }

    private fun drawAllMarkersOnMapReady(map: MaptecMap, fitCameraToResults: Boolean) {
        val engine = map.getOverlayEngine()
        val currentPlaces = _places.value
        val selectedId = _selectedPlaceId.value

        engine.deleteAllMarkers()
        _placeMarkerMap.value.clear()

        if (currentPlaces.isEmpty()) {
            _selectedPlaceId.value = null
            return
        }

        val newMap = mutableMapOf<String, Marker>()
        for (place in currentPlaces) {
            val loc = place.location ?: continue
            val latLng = LatLng(loc.latitude, loc.longitude)
            val iconId = if (place.id == selectedId) MARKER_SELECT_ICON_ID else MARKER_ICON_ID
            val cached = markerIconBitmaps[iconId] ?: continue
            val marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(latLng)
                    .withIcon(iconId, cached.first, cached.second)
                    .withIconSize(MARKER_ICON_SIZE_SCALE)
                    .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                    .withClickable(true)
                    .withDraggable(false)
                    .withVisible(true),
            )
            marker.addOnClickListener { clicked ->
                selectMarker(map, clicked.getLatLng(), place.id)
                true
            }
            newMap[place.id] = marker
        }
        _placeMarkerMap.value = newMap

        if (fitCameraToResults && newMap.isNotEmpty()) {
            fitCameraToPlaces(map, currentPlaces)
        }
    }

    /** 将相机移动到能尽量一屏展示全部搜索结果 marker 的范围。 */
    private fun fitCameraToPlaces(map: MaptecMap, places: List<Place>) {
        val points = places.mapNotNull { place ->
            place.location?.let { LatLng(it.latitude, it.longitude) }
        }
        if (points.isEmpty()) return

        if (points.size == 1) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(points.first(), SINGLE_RESULT_ZOOM),
                CAMERA_FIT_DURATION_MS,
            )
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_BOTTOM_PX,
            ),
            CAMERA_FIT_DURATION_MS,
        )
    }

    fun selectMarker(map: MaptecMap, latLng: LatLng, placeId: String) {
        if (mapLibreMapRef == null) return
        _selectedPlaceId.value = placeId
        drawAllMarkers()
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        loadPlaceDetail(placeId)
    }

    fun selectMarkerFormList(placeId: String) {
        _selectedPlaceId.value = placeId
        loadPlaceDetail(placeId)
    }

    fun deleteCircle() {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        currentCircle?.let { engine.deleteCircle(it) }
        currentCircle = null
    }

    fun updateCircle(center: LatLng?, radiusInMeters: Int, @Suppress("UNUSED_PARAMETER") mapLibreMap: MaptecMap) {
        center ?: return
        drawShapeCircle(center, radiusInMeters, mapLibreMap)
        _mapCenter.value = center
    }

    // ==================== 位置区域绘制 ====================

    fun clearLocationShapes() {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        currentCircle?.let { engine.deleteCircle(it) }
        currentCircle = null
        if (currentFill != null) {
            engine.deleteAllFills()
            currentFill = null
        }
    }

    fun drawLocationShape(mapLibreMap: MaptecMap) {
        clearLocationShapes()

        val mode: LocationMode
        val ne: String
        val sw: String
        val center: String
        val radius: String

        when (_currentTab.value) {
            SearchTab.TEXT -> {
                mode = _locationMode.value
                ne = _rectNorthEast.value
                sw = _rectSouthWest.value
                center = _circleCenter.value
                radius = _circleRadius.value
            }
            SearchTab.SUGGEST -> {
                mode = _suggestLocationMode.value
                ne = _suggestRectNorthEast.value
                sw = _suggestRectSouthWest.value
                center = _suggestCircleCenter.value
                radius = _suggestCircleRadius.value
            }
            SearchTab.NEARBY -> {
                val c = parseCoordinates(_nearbyCenterString.value)
                val r = _nearbyRadius.value.trim().toIntOrNull()
                if (c != null && r != null && r in 0..50000) {
                    val latLng = LatLng(c.latitude, c.longitude)
                    drawShapeCircle(latLng, r, mapLibreMap)
                }
                return
            }
            SearchTab.DETAIL -> {
                return
            }
        }

        when (mode) {
            LocationMode.NONE -> Unit
            LocationMode.BIAS_RECTANGLE, LocationMode.LIMIT_RECTANGLE -> {
                val neParsed = parseCoordinates(ne)
                val swParsed = parseCoordinates(sw)
                if (neParsed != null && swParsed != null) {
                    drawShapeRectangle(neParsed, swParsed)
                }
            }
            LocationMode.BIAS_CIRCLE, LocationMode.LIMIT_CIRCLE -> {
                val centerParsed = parseCoordinates(center)
                val r = radius.trim().toIntOrNull()?.takeIf { it in 0..50000 }
                if (centerParsed != null && r != null) {
                    val latLng = LatLng(centerParsed.latitude, centerParsed.longitude)
                    drawShapeCircle(latLng, r, mapLibreMap)
                }
            }
        }
    }

    private fun drawShapeCircle(center: LatLng, radiusInMeters: Int, @Suppress("UNUSED_PARAMETER") mapLibreMap: MaptecMap) {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        currentCircle?.let { engine.deleteCircle(it) }

        currentCircle = engine.addCircle(
            CircleOptions()
                .withCenter(center)
                .withRadius(radiusInMeters.toDouble())
                .withGeodesic(true)
                .withSegments(256)
                .withFillColor(CIRCLE_COLOR)
                .withFillOpacity(CIRCLE_OPACITY)
                .withStrokeColor(CIRCLE_STROKE_COLOR)
                .withStrokeWeight(CIRCLE_STROKE_WIDTH)
                .withStrokeOpacity(1f),
        )
    }

    private fun drawShapeRectangle(ne: GeoCoordinate, sw: GeoCoordinate) {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        if (currentFill != null) {
            engine.deleteAllFills()
            currentFill = null
        }

        val ring = listOf(
            LatLng(ne.latitude, ne.longitude),
            LatLng(ne.latitude, sw.longitude),
            LatLng(sw.latitude, sw.longitude),
            LatLng(sw.latitude, ne.longitude),
            LatLng(ne.latitude, ne.longitude),
        )

        currentFill = engine.addPolygon(
            FillOptions()
                .withLatLngs(listOf(ring))
                .withFillColor(RECT_FILL_COLOR)
                .withFillOpacity(RECT_FILL_OPACITY)
                .withStrokeColor(RECT_OUTLINE_COLOR)
                .withStrokeWeight(RECT_OUTLINE_WIDTH)
                .withStrokeOpacity(1f)
                .withFillAntialias(true),
        )
    }

    // ==================== 位置参数构建 ====================

    enum class LocationMode(val label: String) {
        NONE("不设置"),
        BIAS_RECTANGLE("locationBias - Rectangle"),
        BIAS_CIRCLE("locationBias - Circle"),
        LIMIT_RECTANGLE("locationLimit - Rectangle"),
        LIMIT_CIRCLE("locationLimit - Circle")
    }

    private data class LocationResult(
        val locationBias: LocationBias?,
        val locationLimit: LocationLimit?
    )

    private fun buildLocation(
        mode: LocationMode,
        ne: String, sw: String,
        center: String, radius: String
    ): LocationResult {
        return when (mode) {
            LocationMode.NONE -> LocationResult(null, null)
            LocationMode.BIAS_RECTANGLE -> {
                val neParsed = parseCoordinates(ne)
                val swParsed = parseCoordinates(sw)
                LocationResult(
                    locationBias = if (neParsed != null && swParsed != null) LocationBias.Rectangle(neParsed, swParsed) else null,
                    locationLimit = null
                )
            }
            LocationMode.LIMIT_RECTANGLE -> {
                val neParsed = parseCoordinates(ne)
                val swParsed = parseCoordinates(sw)
                LocationResult(
                    locationBias = null,
                    locationLimit = if (neParsed != null && swParsed != null) LocationLimit.Rectangle(neParsed, swParsed) else null
                )
            }
            LocationMode.BIAS_CIRCLE -> {
                val centerParsed = parseCoordinates(center)
                val r = radius.trim().toIntOrNull()?.takeIf { it in 0..50000 }
                LocationResult(
                    locationBias = if (centerParsed != null && r != null) LocationBias.Circle(centerParsed, r) else null,
                    locationLimit = null
                )
            }
            LocationMode.LIMIT_CIRCLE -> {
                val centerParsed = parseCoordinates(center)
                val r = radius.trim().toIntOrNull()?.takeIf { it in 0..50000 }
                LocationResult(
                    locationBias = null,
                    locationLimit = if (centerParsed != null && r != null) LocationLimit.Circle(centerParsed, r) else null
                )
            }
        }
    }

    private fun parseCoordinates(raw: String): GeoCoordinate? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        return GeoCoordinate(longitude = lng, latitude = lat)
    }

    companion object {
        private const val TAG = "SearchViewModel"
        private val log = LoggerFactory.getLogger(LOG_MODULE).withTag(TAG)
        private const val SUGGEST_DEBOUNCE_DELAY_MS = 300L
        private const val DEFAULT_LANGUAGE = "zh"
        private const val MARKER_ICON_SIZE_SCALE = 1.0f
        private const val SINGLE_RESULT_ZOOM = 14.0
        private const val CAMERA_FIT_DURATION_MS = 500
        private const val CAMERA_BOUNDS_PADDING_PX = 100
        /** 底部多留边距，避免 FAB / 控件遮挡 marker。 */
        private const val CAMERA_BOUNDS_PADDING_BOTTOM_PX = 180
        private const val MARKER_ICON_ID = "marker-icon"
        private const val MARKER_SELECT_ICON_ID = "marker-select-icon"
        private const val CIRCLE_COLOR = "#3A5EFB"
        private const val CIRCLE_STROKE_COLOR = "#FFFFFF"
        private const val CIRCLE_STROKE_WIDTH = 2.0f
        private const val CIRCLE_OPACITY = 0.3f
        private const val RECT_FILL_COLOR = "#3A5EFB"
        private const val RECT_FILL_OPACITY = 0.2f
        private const val RECT_OUTLINE_COLOR = "#3A5EFB"
        private const val RECT_OUTLINE_WIDTH = 2.0f

        val TAB_API_NAMES: Map<SearchTab, List<String>> = mapOf(
            SearchTab.TEXT to listOf("textSearch", "textSearch(loadMore)", "autoSuggest"),
            SearchTab.NEARBY to listOf("nearbySearch"),
            SearchTab.SUGGEST to listOf("suggestSearch"),
            SearchTab.DETAIL to listOf("placeDetail")
        )
    }
}

class SearchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            val searchService = SearchService.getInstance(context.applicationContext, null)
            return SearchViewModel(searchService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
