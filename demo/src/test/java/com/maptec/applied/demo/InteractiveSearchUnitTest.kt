package com.maptec.applied.demo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.LocationMode
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.api.ApiClient
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.request.SuggestRequest
import com.maptec.applied.search.model.response.LocalizedText
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.SuggestResponse
import com.maptec.applied.search.model.response.Suggestion
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// ==================== 统一硬编码入参常量 ====================

private const val INTERACTIVE_QUERY = "hotel"
private const val INTERACTIVE_TYPES = "hotel,restaurant"
private const val INTERACTIVE_RESULT_LIMIT = "3"
private const val INTERACTIVE_LANGUAGE = "en"
private const val INTERACTIVE_REGION = "SG"
private const val INTERACTIVE_NE = "1.3600,103.8300"
private const val INTERACTIVE_SW = "1.3400,103.8000"
private const val INTERACTIVE_NE_LATITUDE = 1.3600
private const val INTERACTIVE_NE_LONGITUDE = 103.8300
private const val INTERACTIVE_SW_LATITUDE = 1.3400
private const val INTERACTIVE_SW_LONGITUDE = 103.8000

private const val TEST_SEARCH_BASE_URL = "http://127.0.0.1/"
private const val SUGGEST_SEARCH_ENDPOINT_PATH = "search/v1/suggest"

// ==================== 统一硬编码预期结果常量 ====================

private val EXPECTED_INTERACTIVE_RESPONSE = SuggestResponse(
    // 交互式搜索：校验响应状态必须是 OK。
    status = ResponseStatus.OK,
    // 交互式搜索：校验候选建议列表的 placeName、placeId 和 text 等关键字段。
    suggestions = listOf(
        Suggestion(
            placeName = "places/interactive_hotel_001",
            placeId = "interactive_hotel_001",
            text = LocalizedText(text = "MapTec Hotel", languageCode = INTERACTIVE_LANGUAGE)
        ),
        Suggestion(
            placeName = "places/interactive_restaurant_002",
            placeId = "interactive_restaurant_002",
            text = LocalizedText(text = "SDK Restaurant", languageCode = INTERACTIVE_LANGUAGE)
        )
    ),
    // 交互式搜索：校验 valid 字段与预期完全一致。
    valid = true,
    // 交互式搜索：校验错误对象为空。
    error = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class InteractiveSearchUnitTest {

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
        viewModel.switchTab(SearchViewModel.SearchTab.SUGGEST)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun interactiveSearch_viewportChanged_sendsRectangleBounds() = runTest(mainDispatcher) {
        logScenario("地图移动后交互式搜索")
        logRequestUrl("地图移动后交互式搜索-完整请求URL", suggestSearchRequestUrl())
        logJson("地图移动后交互式搜索-硬编码入参", interactiveViewportInput())

        // 测试场景：地图视口变化后，将视口边界作为 locationLimit.Rectangle 触发交互式搜索。
        viewModel.setSuggestLocationMode(LocationMode.LIMIT_RECTANGLE)
        viewModel.setSuggestRectNorthEast(INTERACTIVE_NE)
        viewModel.setSuggestRectSouthWest(INTERACTIVE_SW)
        viewModel.setSuggestTypes(INTERACTIVE_TYPES)
        viewModel.setSuggestResultLimit(INTERACTIVE_RESULT_LIMIT)
        viewModel.setLanguageString(INTERACTIVE_LANGUAGE)
        viewModel.setRegionString(INTERACTIVE_REGION)

        // 当前 ViewModel 中交互式搜索的业务方法名为 performSuggestSearch，对应 SearchScreen 的交互式搜索入口。
        viewModel.performSuggestSearch(INTERACTIVE_QUERY)
        advanceUntilIdle()

        // 关键字段校验：确认 SuggestRequest 携带了地图视口对应的东北/西南边界。
        val actualRequest = fakeSearchService.lastSuggestRequest
        val rectangle = actualRequest?.locationLimit as? LocationLimit.Rectangle
        logJson("地图移动后交互式搜索-实际请求对象", actualRequest)
        assertEquals(INTERACTIVE_QUERY, actualRequest?.query)
        assertEquals(INTERACTIVE_TYPES, actualRequest?.types)
        assertEquals(INTERACTIVE_RESULT_LIMIT.toInt(), actualRequest?.resultLimit)
        assertEquals(INTERACTIVE_LANGUAGE, actualRequest?.language)
        assertEquals(INTERACTIVE_REGION, actualRequest?.region)
        assertEquals(INTERACTIVE_NE_LATITUDE, rectangle?.northeast?.latitude)
        assertEquals(INTERACTIVE_NE_LONGITUDE, rectangle?.northeast?.longitude)
        assertEquals(INTERACTIVE_SW_LATITUDE, rectangle?.southwest?.latitude)
        assertEquals(INTERACTIVE_SW_LONGITUDE, rectangle?.southwest?.longitude)

        // 结果实体校验：从 ViewModel 保存的接口响应 JSON 还原实际返回实体，并与预期实体全量对比。
        val actualResponse = actualSuggestResponse()
        assertJsonEquals("地图移动后交互式搜索-完整返回实体", EXPECTED_INTERACTIVE_RESPONSE, actualResponse)
        assertJsonEquals("地图移动后交互式搜索-ViewModel候选列表", EXPECTED_INTERACTIVE_RESPONSE.suggestions, viewModel.suggestions.value)
        assertEquals(true, viewModel.showSuggestions.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun interactiveSearch_requestInFlight_setsLoadingTrue() = runTest(mainDispatcher) {
        logScenario("交互式搜索中状态")
        logRequestUrl("交互式搜索中状态-完整请求URL", suggestSearchRequestUrl())
        logJson("交互式搜索中状态-硬编码入参", mapOf("query" to INTERACTIVE_QUERY))

        // 测试场景：FakeService 暂停返回，验证请求发出后、响应完成前 isLoading 会变为 true。
        fakeSearchService.pendingSuggestResponse = CompletableDeferred()

        viewModel.performSuggestSearch(INTERACTIVE_QUERY)
        runCurrent()

        logJson(
            "交互式搜索中状态-实际状态对象",
            mapOf(
                "isLoading" to viewModel.isLoading.value,
                "lastRequest" to fakeSearchService.lastSuggestRequest
            )
        )
        assertEquals(true, viewModel.isLoading.value)
        assertEquals(INTERACTIVE_QUERY, fakeSearchService.lastSuggestRequest?.query)

        fakeSearchService.pendingSuggestResponse?.complete(EXPECTED_INTERACTIVE_RESPONSE)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertJsonEquals("交互式搜索中状态-完整返回实体", EXPECTED_INTERACTIVE_RESPONSE, actualSuggestResponse())
    }

    private fun actualSuggestResponse(): SuggestResponse {
        val actualResponseJson = viewModel.currentTabSelectedResponseJson.value
        return gson.fromJson(actualResponseJson, SuggestResponse::class.java)
    }

    private fun setSearchApiClientBaseUrlForUnitTest() {
        val baseUrlField = ApiClient::class.java.getDeclaredField("baseUrl")
        baseUrlField.isAccessible = true
        baseUrlField.set(ApiClient, TEST_SEARCH_BASE_URL)
    }

    private fun interactiveViewportInput(): Map<String, Any> {
        return mapOf(
            "query" to INTERACTIVE_QUERY,
            "types" to INTERACTIVE_TYPES,
            "resultLimit" to INTERACTIVE_RESULT_LIMIT,
            "language" to INTERACTIVE_LANGUAGE,
            "region" to INTERACTIVE_REGION,
            "viewport" to mapOf(
                "northeast" to INTERACTIVE_NE,
                "southwest" to INTERACTIVE_SW
            )
        )
    }

    private fun logScenario(name: String) {
        println("\n========== InteractiveSearchUnitTest: $name ==========")
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

    private fun suggestSearchRequestUrl(): String {
        return TEST_SEARCH_BASE_URL.trimEnd('/') + "/" + SUGGEST_SEARCH_ENDPOINT_PATH.trimStart('/')
    }

    private class FakeSearchService : SearchService(apiKey = null, signatureSha1 = null) {
        var lastSuggestRequest: SuggestRequest? = null
            private set

        var pendingSuggestResponse: CompletableDeferred<SuggestResponse>? = null

        override suspend fun suggest(request: SuggestRequest): Result<SuggestResponse> {
            lastSuggestRequest = request

            val response = pendingSuggestResponse?.await() ?: when (request.query) {
                INTERACTIVE_QUERY -> EXPECTED_INTERACTIVE_RESPONSE
                else -> SuggestResponse(
                    status = ResponseStatus.ZERO_RESULTS,
                    suggestions = emptyList(),
                    valid = true,
                    error = null
                )
            }

            return Result.success(response)
        }
    }
}
