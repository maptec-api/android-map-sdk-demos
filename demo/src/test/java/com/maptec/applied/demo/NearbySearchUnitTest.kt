package com.maptec.applied.demo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.api.ApiClient
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.NearbySearchRequest
import com.maptec.applied.search.model.response.LocalizedText
import com.maptec.applied.search.model.response.NearbySearchResponse
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// ==================== 统一硬编码入参常量 ====================

private const val NORMAL_CENTER = "1.3521,103.8198"
private const val NORMAL_LATITUDE = 1.3521
private const val NORMAL_LONGITUDE = 103.8198
private const val NORMAL_RADIUS = "3000"
private const val NORMAL_TYPES = "cafe,restaurant"
private const val NORMAL_RESULT_LIMIT = "2"
private const val NORMAL_LANGUAGE = "en"
private const val NORMAL_REGION = "SG"

private const val EMPTY_CENTER = "1.4000,103.7500"
private const val EMPTY_RADIUS = "1500"
private const val EMPTY_TYPES = "poi-that-should-not-match-000"

private const val TEST_SEARCH_BASE_URL = "http://127.0.0.1/"
private const val NEARBY_SEARCH_ENDPOINT_PATH = "search/v1/nearby"

// ==================== 统一硬编码预期结果常量 ====================

private val EXPECTED_NEARBY_NORMAL_RESPONSE = NearbySearchResponse(
    // 正常附近搜索：校验响应状态必须是 OK。
    status = ResponseStatus.OK,
    // 正常附近搜索：校验分页 token 与预设值完全一致。
    nextPageToken = null,
    // 正常附近搜索：校验返回 POI 列表的 id、name、displayName、types、formattedAddress 等关键字段。
    places = listOf(
        Place(
            id = "nearby_cafe_001",
            name = "places/nearby_cafe_001",
            displayName = LocalizedText(text = "MapTec Nearby Coffee", languageCode = NORMAL_LANGUAGE),
            types = listOf("cafe", "food"),
            formattedAddress = "10 Nearby Street, Singapore",
            location = GeoCoordinate(longitude = 103.8199, latitude = 1.3522)
        ),
        Place(
            id = "nearby_food_002",
            name = "places/nearby_food_002",
            displayName = LocalizedText(text = "SDK Nearby Kitchen", languageCode = NORMAL_LANGUAGE),
            types = listOf("restaurant"),
            formattedAddress = "11 Nearby Street, Singapore",
            location = GeoCoordinate(longitude = 103.8201, latitude = 1.3524)
        )
    ),
    // 正常附近搜索：校验 valid 字段与预期完全一致。
    valid = true,
    // 正常附近搜索：校验错误对象为空。
    error = null
)

private val EXPECTED_NEARBY_ZERO_RESULTS_RESPONSE = NearbySearchResponse(
    // 空结果附近搜索：校验响应状态必须是 ZERO_RESULTS。
    status = ResponseStatus.ZERO_RESULTS,
    // 空结果附近搜索：校验分页 token 为空。
    nextPageToken = null,
    // 空结果附近搜索：校验 POI 列表为空。
    places = emptyList(),
    // 空结果附近搜索：校验 valid 字段与预期完全一致。
    valid = true,
    // 空结果附近搜索：校验错误对象为空。
    error = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NearbySearchUnitTest {

    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var fakeSearchService: FakeSearchService
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        setSearchApiClientBaseUrlForUnitTest()
        fakeSearchService = FakeSearchService()
        viewModel = SearchViewModel(fakeSearchService)
        viewModel.switchTab(SearchViewModel.SearchTab.NEARBY)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nearbySearch_normalRadiusAndCenter_returnsExpectedPlaces() = runTest(mainDispatcher) {
        logScenario("正常附近搜索")
        logRequestUrl("正常附近搜索-完整请求URL", nearbySearchRequestUrl())
        logJson("正常附近搜索-硬编码入参", normalNearbyInput())

        // 测试场景：给定经纬度、半径、类型和分页上限，ViewModel 应完整透传到 SearchService。
        viewModel.setNearbyCenterString(NORMAL_CENTER)
        viewModel.setNearbyRadius(NORMAL_RADIUS)
        viewModel.setNearbyTypes(NORMAL_TYPES)
        viewModel.setNearbyResultLimit(NORMAL_RESULT_LIMIT)
        viewModel.setNearbyRankType(RankType.DISTANCE)
        viewModel.setLanguageString(NORMAL_LANGUAGE)
        viewModel.setRegionString(NORMAL_REGION)

        viewModel.performNearbySearch()
        advanceUntilIdle()

        // 关键字段校验：确认附近搜索请求中的圆形区域和高级参数与硬编码入参完全一致。
        val actualRequest = fakeSearchService.lastNearbySearchRequest
        val actualLocationLimit = actualRequest?.locationLimit
        logJson("正常附近搜索-实际请求对象", actualRequest)
        assertEquals(NORMAL_LATITUDE, actualLocationLimit?.center?.latitude)
        assertEquals(NORMAL_LONGITUDE, actualLocationLimit?.center?.longitude)
        assertEquals(NORMAL_RADIUS.toInt(), actualLocationLimit?.radius)
        assertEquals(NORMAL_TYPES, actualRequest?.types)
        assertEquals(NORMAL_RESULT_LIMIT.toInt(), actualRequest?.resultLimit)
        assertEquals(RankType.DISTANCE, actualRequest?.rank)
        assertEquals(NORMAL_LANGUAGE, actualRequest?.language)
        assertEquals(NORMAL_REGION, actualRequest?.region)

        // 结果实体校验：从 ViewModel 保存的接口响应 JSON 还原实际返回实体，并与预期实体全量对比。
        val actualResponse = actualNearbySearchResponse()
        assertJsonEquals("正常附近搜索-完整返回实体", EXPECTED_NEARBY_NORMAL_RESPONSE, actualResponse)
        assertJsonEquals("正常附近搜索-ViewModel地点列表", EXPECTED_NEARBY_NORMAL_RESPONSE.places, viewModel.places.value)
        assertEquals(true, viewModel.showResults.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun nearbySearch_zeroResults_clearsResultState() = runTest(mainDispatcher) {
        logScenario("附近搜索空结果")
        logRequestUrl("附近搜索空结果-完整请求URL", nearbySearchRequestUrl())
        logJson("附近搜索空结果-硬编码入参", zeroResultsNearbyInput())

        // 测试场景：服务端返回 ZERO_RESULTS 时，ViewModel 应清空 POI 列表并隐藏结果区域。
        viewModel.setNearbyCenterString(EMPTY_CENTER)
        viewModel.setNearbyRadius(EMPTY_RADIUS)
        viewModel.setNearbyTypes(EMPTY_TYPES)
        viewModel.setLanguageString(NORMAL_LANGUAGE)
        viewModel.setRegionString(NORMAL_REGION)

        viewModel.performNearbySearch()
        advanceUntilIdle()

        val actualRequest = fakeSearchService.lastNearbySearchRequest
        logJson("附近搜索空结果-实际请求对象", actualRequest)
        assertEquals(EMPTY_TYPES, actualRequest?.types)
        assertEquals(EMPTY_RADIUS.toInt(), actualRequest?.locationLimit?.radius)

        val actualResponse = actualNearbySearchResponse()
        assertJsonEquals("附近搜索空结果-完整返回实体", EXPECTED_NEARBY_ZERO_RESULTS_RESPONSE, actualResponse)
        assertEquals(emptyList<Place>(), viewModel.places.value)
        assertEquals(false, viewModel.showResults.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    private fun actualNearbySearchResponse(): NearbySearchResponse {
        val actualResponseJson = viewModel.currentTabSelectedResponseJson.value
        return gson.fromJson(actualResponseJson, NearbySearchResponse::class.java)
    }

    private fun setSearchApiClientBaseUrlForUnitTest() {
        val baseUrlField = ApiClient::class.java.getDeclaredField("baseUrl")
        baseUrlField.isAccessible = true
        baseUrlField.set(ApiClient, TEST_SEARCH_BASE_URL)
    }

    private fun normalNearbyInput(): Map<String, Any> {
        return mapOf(
            "center" to NORMAL_CENTER,
            "radius" to NORMAL_RADIUS,
            "types" to NORMAL_TYPES,
            "resultLimit" to NORMAL_RESULT_LIMIT,
            "rank" to RankType.DISTANCE.name,
            "language" to NORMAL_LANGUAGE,
            "region" to NORMAL_REGION
        )
    }

    private fun zeroResultsNearbyInput(): Map<String, Any> {
        return mapOf(
            "center" to EMPTY_CENTER,
            "radius" to EMPTY_RADIUS,
            "types" to EMPTY_TYPES,
            "language" to NORMAL_LANGUAGE,
            "region" to NORMAL_REGION
        )
    }

    private fun logScenario(name: String) {
        println("\n========== NearbySearchUnitTest: $name ==========")
    }

    private fun logRequestUrl(label: String, url: String) {
        println("[$label]")
        println(url)
    }

    private fun logJson(label: String, value: Any?) {
        println("[$label]")
        println(toPrettyJson(value))
    }

    private fun assertJsonEquals(label: String, expected: Any?, actual: Any?) {
        val expectedJson = toPrettyJson(expected)
        val actualJson = toPrettyJson(actual)

        logJson("$label-预期JSON", expected)
        logJson("$label-实际JSON", actual)

        assertEquals(
            buildJsonCompareMessage(label, expectedJson, actualJson),
            expectedJson,
            actualJson
        )
    }

    private fun buildJsonCompareMessage(label: String, expectedJson: String, actualJson: String): String {
        if (expectedJson == actualJson) {
            return "$label JSON completely matched."
        }

        val expectedLines = expectedJson.lines()
        val actualLines = actualJson.lines()
        val maxLineCount = maxOf(expectedLines.size, actualLines.size)
        val firstDiffIndex = (0 until maxLineCount).firstOrNull { index ->
            expectedLines.getOrNull(index) != actualLines.getOrNull(index)
        }

        val firstDiffText = firstDiffIndex?.let { index ->
            """
            First different line: ${index + 1}
            Expected: ${expectedLines.getOrNull(index).orEmpty()}
            Actual  : ${actualLines.getOrNull(index).orEmpty()}
            """.trimIndent()
        }.orEmpty()

        return """
            $label JSON mismatch.
            $firstDiffText

            Expected JSON:
            $expectedJson

            Actual JSON:
            $actualJson
        """.trimIndent()
    }

    private fun toPrettyJson(value: Any?): String {
        return gson.toJson(value)
    }

    private fun nearbySearchRequestUrl(): String {
        return TEST_SEARCH_BASE_URL.trimEnd('/') + "/" + NEARBY_SEARCH_ENDPOINT_PATH.trimStart('/')
    }

    private class FakeSearchService : SearchService(apiKey = null, signatureSha1 = null) {
        var lastNearbySearchRequest: NearbySearchRequest? = null
            private set

        override suspend fun nearbySearch(request: NearbySearchRequest): Result<NearbySearchResponse> {
            lastNearbySearchRequest = request

            val response = when (request.types) {
                NORMAL_TYPES -> EXPECTED_NEARBY_NORMAL_RESPONSE
                EMPTY_TYPES -> EXPECTED_NEARBY_ZERO_RESULTS_RESPONSE
                else -> NearbySearchResponse(
                    status = ResponseStatus.ZERO_RESULTS,
                    nextPageToken = null,
                    places = emptyList(),
                    valid = true,
                    error = null
                )
            }

            return Result.success(response)
        }
    }
}
