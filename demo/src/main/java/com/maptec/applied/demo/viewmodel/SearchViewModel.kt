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
import com.maptec.applied.search.model.common.AlongRouteRankType
import com.maptec.applied.search.model.common.EvaluationMode
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.AlongRouteSearchRequest
import com.maptec.applied.search.model.request.NearbySearchRequest
import com.maptec.applied.search.model.request.RouteGeometry
import com.maptec.applied.search.model.request.SuggestRequest
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.AlongRouteResult
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.PhotoResponse
import com.maptec.applied.search.model.response.ErrorObject
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
import com.maptec.applied.maps.overlay.navigationline.NavigationLine
import com.maptec.applied.maps.overlay.navigationline.NavigationLineOptions
import com.maptec.applied.style.Property
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
import org.maplibre.geojson.utils.PolylineUtils

class SearchViewModel(
    private val searchService: SearchService,
    context: Context
) : ViewModel() {

    private val prettyGson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val appContext = context.applicationContext
    private val lineArrowBitmap: Bitmap? =
        ContextCompat.getDrawable(appContext, R.drawable.line_arrow)?.let { BitmapUtils.getBitmapFromDrawable(it) }

    // ==================== Tab / 视图模式 ====================

    enum class SearchTab(val label: String) {
        TEXT("文本搜索"),
        NEARBY("附近搜索"),
        SUGGEST("交互式搜索"),
        ALONG_ROUTE("沿途搜索"),
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
        _showMapPlaceSheet.value = false
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

    /** ZERO_RESULTS 表示无匹配结果，按空列表处理，不弹错误 Toast。 */
    private fun isEmptySearchResult(status: ResponseStatus): Boolean =
        status == ResponseStatus.ZERO_RESULTS

    private fun showSearchStatusErrorToast(
        tag: String,
        status: ResponseStatus,
        error: ErrorObject?,
        toastPrefix: String,
    ) {
        log.e { "$tag: status: $status" }
        _toastMessage.value = "$toastPrefix${error?.code}: ${error?.message ?: status.name}"
    }

    fun toggleMapView() {
        if (_showMap.value) {
            exitMapView()
            return
        }
        // 仅地点详情 FAB 进地图时画 AOI；文本/附近 FAB 仍只切地图
        if (_currentTab.value == SearchTab.DETAIL) {
            enterDetailMapView()
        } else {
            _showMap.value = true
        }
    }

    /**
     * 从地图返回列表/表单页。
     * @return true 表示已处理（调用方不应再 navigateUp 到 Web 服务目录）
     */
    fun exitMapView(): Boolean {
        if (!_showMap.value) return false
        // 先关地图底栏再关地图，避免 BottomSheet 出树时 onDismiss 清掉详情数据
        _showMapPlaceSheet.value = false
        _showMap.value = false
        clearAoiPolygon()
        when (_currentTab.value) {
            SearchTab.TEXT, SearchTab.NEARBY, SearchTab.ALONG_ROUTE -> {
                if (_places.value.isNotEmpty() || _alongRouteResults.value.isNotEmpty()) {
                    _showResults.value = true
                }
                closePlaceDetail()
            }
            SearchTab.DETAIL -> {
                // 回到详情表单，保留 placeDetail / showPlaceDetail 给内联卡片
            }
            else -> Unit
        }
        return true
    }

    /** 地点详情 FAB：进地图并用当前详情的 AOI 画多边形。 */
    private fun enterDetailMapView() {
        val place = _placeDetail.value
        if (place != null) {
            if (_places.value.none { it.id == place.id }) {
                _places.value = listOf(place)
            }
            _selectedPlaceId.value = place.id
            _showPlaceDetail.value = true
            _showMapPlaceSheet.value = true
            pendingAoiPlace = place
        }
        _showMap.value = true
    }

    // ==================== 通用查询 ====================

    private val _searchQuery = MutableStateFlow("Anglo-Chinese School")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _languageString = MutableStateFlow("en")
    val languageString: StateFlow<String> = _languageString.asStateFlow()

    private val _regionString = MutableStateFlow("")
    val regionString: StateFlow<String> = _regionString.asStateFlow()

    // ==================== 地点详情参数 ====================

    private val _detailPlaceId = MutableStateFlow("244737191329156445")
    val detailPlaceId: StateFlow<String> = _detailPlaceId.asStateFlow()

    /** 地点详情 extraFields，逗号分隔；清空表示不传。 */
    private val _detailExtraFields = MutableStateFlow(DEFAULT_DETAIL_EXTRA_FIELDS)
    val detailExtraFields: StateFlow<String> = _detailExtraFields.asStateFlow()

    // ==================== 文本搜索参数 ====================

    private val _textType = MutableStateFlow("")
    val textType: StateFlow<String> = _textType.asStateFlow()

    /** 文本搜索 extraFields，逗号分隔；清空表示不传。 */
    private val _textExtraFields = MutableStateFlow(DEFAULT_TEXT_EXTRA_FIELDS)
    val textExtraFields: StateFlow<String> = _textExtraFields.asStateFlow()

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

    private val _nearbyCenterString = MutableStateFlow("1.360879,103.732578")
    val nearbyCenterString: StateFlow<String> = _nearbyCenterString.asStateFlow()

    private val _nearbyTypes = MutableStateFlow("")
    val nearbyTypes: StateFlow<String> = _nearbyTypes.asStateFlow()

    private val _nearbyRadius = MutableStateFlow("")
    val nearbyRadius: StateFlow<String> = _nearbyRadius.asStateFlow()

    /** 附近搜索 extraFields，逗号分隔；清空表示不传。 */
    private val _nearbyExtraFields = MutableStateFlow(DEFAULT_NEARBY_EXTRA_FIELDS)
    val nearbyExtraFields: StateFlow<String> = _nearbyExtraFields.asStateFlow()

    private val _nearbyResultLimit = MutableStateFlow("")
    val nearbyResultLimit: StateFlow<String> = _nearbyResultLimit.asStateFlow()

    private val _nearbyRankType = MutableStateFlow<RankType?>(RankType.DISTANCE)
    val nearbyRankType: StateFlow<RankType?> = _nearbyRankType.asStateFlow()

    // ==================== 沿途搜索参数 ====================

    private val _alongRouteQuery = MutableStateFlow("")
    val alongRouteQuery: StateFlow<String> = _alongRouteQuery.asStateFlow()

    private val _alongRouteTypes = MutableStateFlow("")
    val alongRouteTypes: StateFlow<String> = _alongRouteTypes.asStateFlow()

//    private val _encodedPolyline = MutableStateFlow("kwyFkh|xR_g@~u@kf@fTsl@jgB")
    private val _encodedPolyline = MutableStateFlow("qoyF_k|xRRFbCr@XT??HFBDDDBPUb@Ud@OKBER]?OAGY]YKc@Mw@Uk@OoCe@_CY_CSo@Eo@CiC@cLK{BAgLGu@AiAG_AM}@Qs@Uw@]cAu@{@{@o@_AUe@[{@SaAQaAOiAo@cGYyBqDcYSeAM_AWoEMqF_@sJMs@Qe@W_@WUSMQM[Ke@OcBg@aBi@cBi@eA]_AYsAa@a@Me@Oa@Ua@Ki@OcAWq@Qm@Kg@Ic@EOAQAYC[Aa@A[?Y@]?Q@U@Q@YBYDYDYDYDYFi@Ny@TkAZ}Ab@{Ab@uBl@cAXsAZq@TkBj@??gAXe@Nk@N??UF??MDe@La@J??WF]H]H]Ha@He@JSDc@HG@i@Jg@Hi@Jm@Hg@Hm@Hi@Hs@Hi@Fg@FeAJc@Bg@Dm@Fg@BWBa@Be@Be@Bg@BWBk@BeAHk@BQ@a@Bg@Bc@Bc@@a@@a@?[?i@Ag@Ag@Ca@Cc@Cc@Ee@Gc@Gc@Ia@Ia@Ma@MSIMEUMIE??GCUM_@USOc@]WWSSWYOWQUU][i@MYKUISO]ISQa@IQMYGMQc@Sc@Ui@Qa@KSGOGQMYO[MYKWIQGMKWQe@IQISUo@Ma@Qm@Qk@GSI]IUIWI[Ma@Ok@K[I[GSSo@EMK[Qc@KWISGKKSIMIKIMGKQQIMMKIKKKKIOMYSMIKGKGOIMGSKMEOGSIMESEMEOEKCOCOCQCSCOAMCI?IAIAQASCQASAQAYAQAOCK?SAQAMAMAG?GAI?OASAG?IAI?MAIAE?O?EAK?MAK?OAM?E?GAI?OAQ?QAS?QAG?O?OAS?Q?WAO?YAQ?MASAIAOAKAMAOCGAMCMCICICMEMEOGWMMG[QSOUSOOKKKMOQKQQYIQIOKWSe@O[M[EKQ_@ISUe@_@w@Yi@Wg@[m@Ye@O[QYKSIOQYQ[ISUa@IQKUO[IQIOEMKUGOKWSg@ACMg@MWEMGQGWGOIWEOOg@K[MYK]MYQe@a@}@Ug@Ue@Q[OUO_@KOGMCEOSQOKSYc@]g@a@k@Ya@W]KQs@y@o@u@Y[QSUUKIKKAC]YSSOMYWOKWS_@[c@[MIWQYS]SQKYQg@YUMc@U[QUKUK[MQIGCEC]MYMe@Oi@O_@Kw@Q??m@QmAY_@ISEQEQCSEQCOCMCOASCUCKAOAMAWCOASAa@AUAU?U?S?_@?Y@Q?YB[@a@Da@DK@M@MBM@OBMBWDQDyAZSBODUDOBMBQ@MBM@QBM@C?UBI@G?O@I?M@K?M@U@_@?_@?m@AS?MAWAUCYCa@EUCSC_@G[GYGMCUGICKEOEmBk@oFcBw@Uy@W{@WaJkC_@MUIe@O]M_@OUI_@Os@[mGwCgMoF_Ac@uBeA{BcAsFeCsFgCuF{By@YcA]eA[gCm@eGoA]Mc@KiAYmCm@yGyAeMuC}A_@_GmAiA[qAc@mAg@{@a@KGOIoB_BqAoAuAkBqA}B}C{Ee@u@m@s@y@o@}@a@a@Og@Ku@K[C_@Ay@Aa@E_BKO@}@I_AMqAYuAUSCcAIs@@o@Bs@Fu@JoA\\k@RULkAl@m@\\o@b@s@f@WNKFOBk@d@yIdHqB`BwAhAwOjMMLaBxAgCzBqCvC{@hAk@n@eAlAaAjAw@dA_D`FuCnFGb@Sn@e@tA_@`ASf@GR@V{@fCqBlHAf@m@lEEb@EZMdAA@UrB?h@B\\P^XXVVrElAD?nBh@bAVdBb@^J^HVHTFVFzBn@tAZb@JfDv@\\H\\HVHIVI^I^_A|DCLA@W`ACJQ|@Gb@Q`BVj@RXjAHfCVFYj@eCLk@b@kBFIJAf@J")
    val encodedPolyline: StateFlow<String> = _encodedPolyline.asStateFlow()

    private val _routeBufferMeters = MutableStateFlow("1000")
    val routeBufferMeters: StateFlow<String> = _routeBufferMeters.asStateFlow()

    private val _evaluationMode = MutableStateFlow(EvaluationMode.NONE)
    val evaluationMode: StateFlow<EvaluationMode> = _evaluationMode.asStateFlow()

    private val _alongRouteRank = MutableStateFlow<AlongRouteRankType?>(null)
    val alongRouteRank: StateFlow<AlongRouteRankType?> = _alongRouteRank.asStateFlow()

    private val _alongRouteResultLimit = MutableStateFlow("")
    val alongRouteResultLimit: StateFlow<String> = _alongRouteResultLimit.asStateFlow()

    private val _alongRouteExtraFields = MutableStateFlow(DEFAULT_ALONG_ROUTE_EXTRA_FIELDS)
    val alongRouteExtraFields: StateFlow<String> = _alongRouteExtraFields.asStateFlow()

    private val _alongRouteResults = MutableStateFlow<List<AlongRouteResult>>(emptyList())
    val alongRouteResults: StateFlow<List<AlongRouteResult>> = _alongRouteResults.asStateFlow()

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

    /**
     * 地图上的地点详情 ModalBottomSheet 是否展示。
     * 与 [showPlaceDetail] 分开：退出地图时 BottomSheet 出树会触发 onDismissRequest，
     * 若此时走 closePlaceDetail 会清掉表单内联卡片的数据。
     */
    private val _showMapPlaceSheet = MutableStateFlow(false)
    val showMapPlaceSheet: StateFlow<Boolean> = _showMapPlaceSheet.asStateFlow()

    private val _isLoadingPlaceDetail = MutableStateFlow(false)
    val isLoadingPlaceDetail: StateFlow<Boolean> = _isLoadingPlaceDetail.asStateFlow()

    // ==================== 地图相关 ====================

    private val _mapCenter = MutableStateFlow<LatLng?>(null)
    val mapCenter: StateFlow<LatLng?> = _mapCenter.asStateFlow()

    private var mapLibreMapRef: MaptecMap? = null
    private var currentCircle: Circle? = null
    private var currentFill: Fill? = null
    private var aoiFill: Fill? = null
    private var routeNavigationLine: NavigationLine? = null
    /** 列表点击后待绘制的地点；地图尚未就绪时暂存，就绪后再画 AOI。 */
    private var pendingAoiPlace: Place? = null
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
    fun setAlongRouteQuery(value: String) { _alongRouteQuery.value = value }
    fun setAlongRouteTypes(value: String) { _alongRouteTypes.value = value }
    fun setEncodedPolyline(value: String) { _encodedPolyline.value = value }
    fun setRouteBufferMeters(value: String) { _routeBufferMeters.value = value }
    fun setEvaluationMode(value: EvaluationMode) {
        _evaluationMode.value = value
        val rank = _alongRouteRank.value
        if (value == EvaluationMode.NONE &&
            (rank == AlongRouteRankType.ROUTE_DURATION || rank == AlongRouteRankType.ROUTE_DISTANCE)
        ) {
            _alongRouteRank.value = null
        }
    }
    fun setAlongRouteRank(value: AlongRouteRankType?) { _alongRouteRank.value = value }
    fun setAlongRouteResultLimit(value: String) { _alongRouteResultLimit.value = value }
    fun setAlongRouteExtraFields(value: String) { _alongRouteExtraFields.value = value }
    fun setSuggestTypes(value: String) { _suggestTypes.value = value }
    fun setSuggestResultLimit(value: String) { _suggestResultLimit.value = value }
    fun setSuggestLocationMode(value: LocationMode) { _suggestLocationMode.value = value }
    fun setSuggestRectNorthEast(value: String) { _suggestRectNorthEast.value = value }
    fun setSuggestRectSouthWest(value: String) { _suggestRectSouthWest.value = value }
    fun setSuggestCircleCenter(value: String) { _suggestCircleCenter.value = value }
    fun setSuggestCircleRadius(value: String) { _suggestCircleRadius.value = value }
    fun setDetailPlaceId(value: String) { _detailPlaceId.value = value }
    fun setTextExtraFields(value: String) { _textExtraFields.value = value }
    fun setNearbyExtraFields(value: String) { _nearbyExtraFields.value = value }
    fun setDetailExtraFields(value: String) { _detailExtraFields.value = value }

    private fun parseExtraFields(raw: String): List<String>? =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }

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
                        if (!isEmptySearchResult(response.status)) {
                            log.e { "autoSuggest: status: ${response.status}" }
                            response.error?.let { err ->
                                _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                            }
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
                    extraFields = parseExtraFields(_textExtraFields.value),
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.textSearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.TEXT, "textSearch", response)
                    if (response.status != ResponseStatus.OK) {
                        if (!isEmptySearchResult(response.status)) {
                            showSearchStatusErrorToast(
                                tag = "textSearch",
                                status = response.status,
                                error = response.error,
                                toastPrefix = "文本搜索，服务器返回错误:",
                            )
                        }
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
                    extraFields = parseExtraFields(_textExtraFields.value),
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.textSearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.TEXT, "textSearch(loadMore)", response)
                    if (response.status != ResponseStatus.OK) {
                        if (!isEmptySearchResult(response.status)) {
                            response.error?.let { err ->
                                _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                            }
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
                    extraFields = parseExtraFields(_nearbyExtraFields.value),
                    region = _regionString.value.trim().ifEmpty { null }
                )
                val result = searchService.nearbySearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.NEARBY, "nearbySearch", response)
                    if (response.status != ResponseStatus.OK) {
                        if (!isEmptySearchResult(response.status)) {
                            showSearchStatusErrorToast(
                                tag = "nearbySearch",
                                status = response.status,
                                error = response.error,
                                toastPrefix = "附近搜索失败，服务器返回错误:",
                            )
                        }
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

    // ==================== 沿途搜索 ====================

    fun performAlongRouteSearch() {
        val query = _alongRouteQuery.value.trim().ifEmpty { null }
        val types = _alongRouteTypes.value.trim().ifEmpty { null }
        val polyline = _encodedPolyline.value.trim()
        if (query == null && types == null) {
            _toastMessage.value = appContext.getString(R.string.search_toast_along_query_types_required)
            return
        }
        if (polyline.isEmpty()) {
            _toastMessage.value = appContext.getString(R.string.search_toast_along_polyline_required)
            return
        }

        searchJob?.cancel()
        _showResults.value = false

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = AlongRouteSearchRequest(
                    query = query,
                    types = types,
                    route = RouteGeometry(encodedPolyline = polyline),
                    routeBufferMeters = _routeBufferMeters.value.trim().toIntOrNull()
                        ?.takeIf { it in 100..50000 },
                    evaluationMode = _evaluationMode.value,
                    rank = _alongRouteRank.value,
                    resultLimit = _alongRouteResultLimit.value.trim().toIntOrNull()
                        ?.takeIf { it in 1..20 },
                    language = _languageString.value.trim().ifEmpty { DEFAULT_LANGUAGE },
                    region = _regionString.value.trim().ifEmpty { null },
                    extraFields = parseExtraFields(_alongRouteExtraFields.value),
                )
                val result = searchService.alongRouteSearch(request)
                result.onSuccess { response ->
                    saveTabApiResponse(SearchTab.ALONG_ROUTE, "alongRouteSearch", response)
                    if (response.status != ResponseStatus.OK) {
                        if (!isEmptySearchResult(response.status)) {
                            showSearchStatusErrorToast(
                                tag = "alongRouteSearch",
                                status = response.status,
                                error = response.error,
                                toastPrefix = appContext.getString(
                                    R.string.search_toast_along_status_error_prefix,
                                ),
                            )
                        }
                        _alongRouteResults.value = emptyList()
                        _places.value = emptyList()
                        _showResults.value = false
                        _isLoading.value = false
                        return@launch
                    }
                    val list = response.results ?: emptyList()
                    _alongRouteResults.value = list
                    _places.value = list.map { it.place }
                    _showResults.value = list.isNotEmpty()
                    _isLoading.value = false
                    drawRoutePolyline()
                    drawAllMarkers(fitCameraToResults = true)
                }.onFailure {
                    _alongRouteResults.value = emptyList()
                    _places.value = emptyList()
                    _showResults.value = false
                    _isLoading.value = false
                    val message = it.message
                        ?: appContext.getString(R.string.search_toast_unknown_error)
                    _toastMessage.value = appContext.getString(
                        R.string.search_toast_along_request_failed,
                        message,
                    )
                    saveTabApiError(SearchTab.ALONG_ROUTE, "alongRouteSearch", message)
                    log.e { it.toString() }
                }
            } catch (e: Exception) {
                _alongRouteResults.value = emptyList()
                _places.value = emptyList()
                _showResults.value = false
                _isLoading.value = false
                val message = e.message
                    ?: appContext.getString(R.string.search_toast_unknown_error)
                _toastMessage.value = appContext.getString(
                    R.string.search_toast_along_exception,
                    message,
                )
                saveTabApiError(SearchTab.ALONG_ROUTE, "alongRouteSearch", message)
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
                        if (!isEmptySearchResult(response.status)) {
                            showSearchStatusErrorToast(
                                tag = "suggestSearch",
                                status = response.status,
                                error = response.error,
                                toastPrefix = "交互式搜索失败，服务器返回错误:",
                            )
                        }
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
                    region = _regionString.value.trim().ifEmpty { null },
                    extraFields = parseExtraFields(_detailExtraFields.value),
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
        _showMapPlaceSheet.value = false
        _placeDetail.value = null
    }

    /** 仅关闭地图上的详情底栏，不清空请求结果（供表单内联卡片继续展示）。 */
    fun dismissMapPlaceSheet() {
        _showMapPlaceSheet.value = false
    }

    // ==================== 地图交互 ====================

    fun onPlaceClick(place: Place, map: MaptecMap?) {
        // 文本 / 附近 / 沿途搜索：不进详情，切回地图并用 addPolygon 绘制 AOI
        if (_currentTab.value == SearchTab.TEXT ||
            _currentTab.value == SearchTab.NEARBY ||
            _currentTab.value == SearchTab.ALONG_ROUTE
        ) {
            onSearchListItemSelected(place)
            return
        }
        val location = place.location
        if (location == null) {
            _toastMessage.value = "地点经纬度坐标为空"
            return
        }
        selectMarkerFormList(place.id)
        _showResults.value = false
        _showSuggestions.value = false
    }

    /**
     * 文本 / 附近搜索列表点击：切到地图、画 AOI，并用搜索结果直接展示地图上的地点详情底栏
     *（不再额外请求 placeDetail 接口）。
     */
    fun onSearchListItemSelected(place: Place) {
        placeDetailJob?.cancel()
        _isLoadingPlaceDetail.value = false
        _showResults.value = false
        _showSuggestions.value = false
        _selectedPlaceId.value = place.id
        _placeDetail.value = place
        _showPlaceDetail.value = true
        _showMapPlaceSheet.value = true
        // 先挂 pending，再切地图；由 LaunchedEffect / onMapReady 统一 consume，避免与 drawAllMarkers 抢跑
        pendingAoiPlace = place
        _showMap.value = true
    }

    /**
     * 地图 Style 就绪后调用：若有待处理的列表选中项，则绘制 marker + AOI。
     * @return true 表示已消费待处理选中项（调用方不必再 fit 全量结果）
     */
    fun consumePendingAoiSelection(): Boolean {
        val place = pendingAoiPlace ?: return false
        val map = mapLibreMapRef ?: return false
        pendingAoiPlace = null
        map.getStyle {
            drawAllMarkersOnMapReady(map, fitCameraToResults = false)
            drawAoiPolygon(place)
        }
        return true
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

        if (_currentTab.value == SearchTab.ALONG_ROUTE) {
            drawRoutePolyline()
        }

        if (currentPlaces.isEmpty()) {
            _selectedPlaceId.value = null
            if (_currentTab.value == SearchTab.ALONG_ROUTE) {
                fitCameraToRouteAndPlaces(map, emptyList())
            }
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
            if (_currentTab.value == SearchTab.ALONG_ROUTE) {
                fitCameraToRouteAndPlaces(map, currentPlaces)
            } else {
                fitCameraToPlaces(map, currentPlaces)
            }
        }
    }

    private fun decodeRouteLatLngs(): List<LatLng> {
        val polyline = _encodedPolyline.value.trim()
        if (polyline.isEmpty()) return emptyList()
        return try {
            PolylineUtils.decode(polyline, 5).map {
                LatLng(latitude = it.latitude(), longitude = it.longitude())
            }
        } catch (e: Exception) {
            log.e { "decodeRouteLatLngs: ${e.message}" }
            emptyList()
        }
    }

    fun drawRoutePolyline() {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        val points = decodeRouteLatLngs()
        if (points.size < 2) return

        // 替换普通 Polyline：使用 NavigationLine（同色描边 + 方向箭头）
        routeNavigationLine?.let { engine.deleteNavigationLine(it) }
        routeNavigationLine = null

        val outlineColor = "#000000"
        var opts = NavigationLineOptions()
            .withLatLngs(points)
            .withLineColor(ROUTE_LINE_COLOR)
            .withLineWidth(ROUTE_LINE_WIDTH)
            .withLineJoin(Property.LINE_JOIN_ROUND)
            .withLineCap(Property.LINE_CAP_ROUND)
            .withOutlineColor(outlineColor)
            .withOutlineWidth(1.0f)

        lineArrowBitmap?.let { bitmap ->
            opts = opts.withLinePattern(bitmap, "line-arrow")
        }

        routeNavigationLine = engine.addNavigationLine(opts)
    }

    private fun clearRoutePolyline() {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        routeNavigationLine?.let { engine.deleteNavigationLine(it) }
        routeNavigationLine = null
    }

    private fun fitCameraToRouteAndPlaces(map: MaptecMap, places: List<Place>) {
        val points = decodeRouteLatLngs().toMutableList()
        places.mapNotNull { place ->
            place.location?.let { LatLng(it.latitude, it.longitude) }
        }.let { points.addAll(it) }
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
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                boundsBuilder.build(),
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_PX,
                CAMERA_BOUNDS_PADDING_BOTTOM_PX,
            ),
            CAMERA_FIT_DURATION_MS,
        )
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

        // 文本 / 附近 / 沿途搜索：与列表点击一致，直接用搜索结果展示地图底栏，不再请求 placeDetail
        if (_currentTab.value == SearchTab.TEXT ||
            _currentTab.value == SearchTab.NEARBY ||
            _currentTab.value == SearchTab.ALONG_ROUTE
        ) {
            val place = _places.value.find { it.id == placeId } ?: return
            placeDetailJob?.cancel()
            _isLoadingPlaceDetail.value = false
            _placeDetail.value = place
            _showPlaceDetail.value = true
            _showMapPlaceSheet.value = true
            drawAoiPolygon(place)
            return
        }

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
        currentFill?.let { engine.deleteFill(it) }
        currentFill = null
        clearRoutePolyline()
    }

    fun clearAoiPolygon() {
        val engine = mapLibreMapRef?.getOverlayEngine() ?: return
        aoiFill?.let { engine.deleteFill(it) }
        aoiFill = null
    }

    /**
     * 将 Place.aoi.geojson（GeoJSON Polygon，坐标为 [lng, lat]）转为 Fill 并 addPolygon 到地图。
     */
    private fun drawAoiPolygon(place: Place) {
        val map = mapLibreMapRef ?: return
        val engine = map.getOverlayEngine()
        clearAoiPolygon()

        val geo = place.aoi?.geojson
        if (geo == null || !geo.type.equals("Polygon", ignoreCase = true)) {
            if (place.aoi != null) {
                _toastMessage.value = "AOI 几何类型不支持：${geo?.type}"
            }
            place.location?.let { loc ->
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(loc.latitude, loc.longitude),
                        SINGLE_RESULT_ZOOM,
                    ),
                    CAMERA_FIT_DURATION_MS,
                )
            }
            return
        }

        val rings = geo.coordinates.mapNotNull { ring ->
            val latLngs = ring.mapNotNull { pt ->
                if (pt.size < 2) null else LatLng(pt[1], pt[0])
            }.toMutableList()
            if (latLngs.size < 3) return@mapNotNull null
            if (latLngs.first() != latLngs.last()) {
                latLngs.add(latLngs.first())
            }
            latLngs.toList()
        }
        if (rings.isEmpty()) {
            _toastMessage.value = "AOI 坐标无效"
            return
        }

        aoiFill = engine.addPolygon(
            FillOptions()
                .withLatLngs(rings)
                .withFillColor(AOI_FILL_COLOR)
                .withFillOpacity(AOI_FILL_OPACITY)
                .withStrokeColor(AOI_STROKE_COLOR)
                .withStrokeWeight(AOI_STROKE_WIDTH)
                .withStrokeOpacity(1f)
                .withFillAntialias(true),
        )

        val builder = LatLngBounds.Builder()
        var hasPoint = false
        for (ring in rings) {
            for (ll in ring) {
                builder.include(ll)
                hasPoint = true
            }
        }
        if (hasPoint) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    builder.build(),
                    CAMERA_BOUNDS_PADDING_PX,
                    CAMERA_BOUNDS_PADDING_PX,
                    CAMERA_BOUNDS_PADDING_PX,
                    CAMERA_BOUNDS_PADDING_BOTTOM_PX,
                ),
                CAMERA_FIT_DURATION_MS,
            )
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
            SearchTab.ALONG_ROUTE -> {
                drawRoutePolyline()
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
        currentFill?.let { engine.deleteFill(it) }
        currentFill = null

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

    override fun onCleared() {
        super.onCleared()
        mapLibreMapRef = null
        aoiFill = null
        pendingAoiPlace = null
        overlayInitialized = false
        markerIconBitmaps.clear()
    }

    companion object {
        private const val TAG = "SearchViewModel"
        private val log = LoggerFactory.getLogger(LOG_MODULE).withTag(TAG)
        private const val SUGGEST_DEBOUNCE_DELAY_MS = 300L
        private const val DEFAULT_LANGUAGE = "zh"
        private const val DEFAULT_TEXT_EXTRA_FIELDS =
            "aoi,phoneNumbers,emailAddresses,websiteUris,summary,openingHours"
        private const val DEFAULT_NEARBY_EXTRA_FIELDS =
            "phoneNumbers,emailAddresses,websiteUris,aoi"
        private const val DEFAULT_ALONG_ROUTE_EXTRA_FIELDS =
            "openingHours,phoneNumbers,aoi"
        private const val DEFAULT_DETAIL_EXTRA_FIELDS =
            "aoi,summary,openingHours,phoneNumbers,emailAddresses,websiteUris,addressComponents,timeZone"
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
        private const val ROUTE_LINE_COLOR = "#1677FF"
        private const val ROUTE_LINE_WIDTH = 10.0f
        private const val AOI_FILL_COLOR = "#E85D04"
        private const val AOI_FILL_OPACITY = 0.35f
        private const val AOI_STROKE_COLOR = "#E85D04"
        private const val AOI_STROKE_WIDTH = 2.0f

        val TAB_API_NAMES: Map<SearchTab, List<String>> = mapOf(
            SearchTab.TEXT to listOf("textSearch", "textSearch(loadMore)", "autoSuggest"),
            SearchTab.NEARBY to listOf("nearbySearch"),
            SearchTab.SUGGEST to listOf("suggestSearch"),
            SearchTab.ALONG_ROUTE to listOf("alongRouteSearch"),
            SearchTab.DETAIL to listOf("placeDetail")
        )
    }
}

class SearchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            val searchService = SearchService.getInstance(context.applicationContext)
            return SearchViewModel(searchService, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
