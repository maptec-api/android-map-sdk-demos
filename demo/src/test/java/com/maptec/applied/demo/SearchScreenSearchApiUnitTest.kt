package com.maptec.applied.demo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.search.api.ApiClient
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.LocalizedText
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.TextSearchResponse
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

private const val NORMAL_KEYWORD = "coffee"
private const val NORMAL_LANGUAGE = "en"
private const val NORMAL_REGION = "SG"
private const val NORMAL_PAGE_SIZE = "2"

private const val EMPTY_KEYWORD = ""

private const val NO_MATCH_KEYWORD = "poi-that-should-not-match-000"
private const val NO_MATCH_LANGUAGE = "en"
private const val NO_MATCH_REGION = "SG"
private const val NO_MATCH_PAGE_SIZE = "5"

private const val TEST_SEARCH_BASE_URL = "http://127.0.0.1/"
private const val TEXT_SEARCH_ENDPOINT_PATH = "search/v1/text"

// ==================== 统一硬编码预期结果常量 ====================

private val EXPECTED_NORMAL_RESPONSE = TextSearchResponse(
    // 正常关键词搜索：校验响应状态必须是 OK。
    status = ResponseStatus.OK,
    // 正常关键词搜索：校验分页 token 与预设值完全一致。
    nextPageToken = null,
    // 正常关键词搜索：校验返回 POI 列表的 id、name、displayName、types、formattedAddress 等关键字段。
    places = listOf(
        Place(
            id = "place_coffee_001",
            name = "places/place_coffee_001",
            displayName = LocalizedText(text = "MapTec Coffee", languageCode = NORMAL_LANGUAGE),
            types = listOf("cafe", "food"),
            formattedAddress = "1 Demo Street, Singapore"
        ),
        Place(
            id = "place_coffee_002",
            name = "places/place_coffee_002",
            displayName = LocalizedText(text = "SDK Cafe", languageCode = NORMAL_LANGUAGE),
            types = listOf("cafe"),
            formattedAddress = "2 Demo Street, Singapore"
        )
    ),
    // 正常关键词搜索：校验 valid 字段与预期完全一致。
    valid = true,
    // 正常关键词搜索：校验错误对象为空。
    error = null
)

private val EXPECTED_EMPTY_PLACES = emptyList<Place>()
private const val EXPECTED_EMPTY_SHOW_RESULTS = false

private val EXPECTED_NO_MATCH_RESPONSE = TextSearchResponse(
    // 无匹配 POI 数据搜索：校验响应状态必须是 ZERO_RESULTS。
    status = ResponseStatus.ZERO_RESULTS,
    // 无匹配 POI 数据搜索：校验分页 token 为空。
    nextPageToken = null,
    // 无匹配 POI 数据搜索：校验 POI 列表为空。
    places = emptyList(),
    // 无匹配 POI 数据搜索：校验 valid 字段与预期完全一致。
    valid = true,
    // 无匹配 POI 数据搜索：校验错误对象为空。
    error = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenSearchApiUnitTest {

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun textSearch_normalKeyword_returnsExpectedResponseEntity() = runTest(mainDispatcher) {
        logScenario("正常关键词搜索")
        logRequestUrl("正常关键词搜索-完整请求URL", textSearchRequestUrl())
        logJson("正常关键词搜索-硬编码入参", normalKeywordInput())

        // 测试场景：正常关键词搜索，入参全部来自顶层硬编码常量。
        viewModel.setLanguageString(NORMAL_LANGUAGE)
        viewModel.setRegionString(NORMAL_REGION)
        viewModel.setPageSizeString(NORMAL_PAGE_SIZE)

        // 实际调用：调用 SearchScreen 页面实际绑定的 ViewModel 搜索封装方法，不启动 Compose UI。
        viewModel.performTextSearch(NORMAL_KEYWORD)
        advanceUntilIdle()

        // 关键字段校验：确认 ViewModel 传入 SearchService 的 request 与硬编码入参完全一致。
        val actualRequest = fakeSearchService.lastTextSearchRequest
        logJson("正常关键词搜索-实际请求对象", actualRequest)
        assertEquals(NORMAL_KEYWORD, actualRequest?.query)
        assertEquals(NORMAL_LANGUAGE, actualRequest?.language)
        assertEquals(NORMAL_REGION, actualRequest?.region)
        assertEquals(NORMAL_PAGE_SIZE.toInt(), actualRequest?.pageSize)

        // 结果实体校验：从 ViewModel 保存的接口响应 JSON 还原实际返回实体，并与预期实体全量对比。
        val actualResponse = actualTextSearchResponse()
        assertJsonEquals("正常关键词搜索-完整返回实体", EXPECTED_NORMAL_RESPONSE, actualResponse)
    }

    @Test
    fun textSearch_emptyKeyword_doesNotRequestApiAndClearsResultState() = runTest(mainDispatcher) {
        logScenario("空关键词搜索")
        logRequestUrl("空关键词搜索-完整请求URL", textSearchRequestUrl())
        logJson("空关键词搜索-硬编码入参", mapOf("query" to EMPTY_KEYWORD))

        // 测试场景：空关键词搜索，SearchScreen 对应的 ViewModel 封装会直接拦截空入参。
        viewModel.performTextSearch(EMPTY_KEYWORD)
        advanceUntilIdle()

        // 关键字段校验：空关键词不发起 SearchService 请求。
        logJson("空关键词搜索-实际请求对象", fakeSearchService.lastTextSearchRequest)
        assertEquals(null, fakeSearchService.lastTextSearchRequest)

        // 结果状态校验：空关键词应清空 POI 列表，并保持不展示搜索结果。
        logJson(
            "空关键词搜索-实际状态对象",
            mapOf(
                "places" to viewModel.places.value,
                "showResults" to viewModel.showResults.value
            )
        )
        assertEquals(EXPECTED_EMPTY_PLACES, viewModel.places.value)
        assertEquals(EXPECTED_EMPTY_SHOW_RESULTS, viewModel.showResults.value)
    }

    @Test
    fun textSearch_noMatchedPoi_returnsExpectedZeroResultsEntity() = runTest(mainDispatcher) {
        logScenario("无匹配POI数据搜索")
        logRequestUrl("无匹配POI数据搜索-完整请求URL", textSearchRequestUrl())
        logJson("无匹配POI数据搜索-硬编码入参", noMatchInput())

        // 测试场景：无匹配 POI 数据搜索，入参全部来自顶层硬编码常量。
        viewModel.setLanguageString(NO_MATCH_LANGUAGE)
        viewModel.setRegionString(NO_MATCH_REGION)
        viewModel.setPageSizeString(NO_MATCH_PAGE_SIZE)

        // 实际调用：调用 SearchScreen 页面实际绑定的 ViewModel 搜索封装方法，不启动 Compose UI。
        viewModel.performTextSearch(NO_MATCH_KEYWORD)
        advanceUntilIdle()

        // 关键字段校验：确认无匹配场景请求参数与硬编码入参完全一致。
        val actualRequest = fakeSearchService.lastTextSearchRequest
        logJson("无匹配POI数据搜索-实际请求对象", actualRequest)
        assertEquals(NO_MATCH_KEYWORD, actualRequest?.query)
        assertEquals(NO_MATCH_LANGUAGE, actualRequest?.language)
        assertEquals(NO_MATCH_REGION, actualRequest?.region)
        assertEquals(NO_MATCH_PAGE_SIZE.toInt(), actualRequest?.pageSize)

        // 结果实体校验：无匹配场景应返回 ZERO_RESULTS，且 places 为空列表。
        val actualResponse = actualTextSearchResponse()
        assertJsonEquals("无匹配POI数据搜索-完整返回实体", EXPECTED_NO_MATCH_RESPONSE, actualResponse)
    }

    private fun actualTextSearchResponse(): TextSearchResponse {
        val actualResponseJson = viewModel.currentTabSelectedResponseJson.value
        return gson.fromJson(actualResponseJson, TextSearchResponse::class.java)
    }

    private fun setSearchApiClientBaseUrlForUnitTest() {
        val baseUrlField = ApiClient::class.java.getDeclaredField("baseUrl")
        baseUrlField.isAccessible = true
        baseUrlField.set(ApiClient, TEST_SEARCH_BASE_URL)
    }

    private fun normalKeywordInput(): Map<String, Any> {
        return mapOf(
            "query" to NORMAL_KEYWORD,
            "language" to NORMAL_LANGUAGE,
            "region" to NORMAL_REGION,
            "pageSize" to NORMAL_PAGE_SIZE
        )
    }

    private fun noMatchInput(): Map<String, Any> {
        return mapOf(
            "query" to NO_MATCH_KEYWORD,
            "language" to NO_MATCH_LANGUAGE,
            "region" to NO_MATCH_REGION,
            "pageSize" to NO_MATCH_PAGE_SIZE
        )
    }

    private fun logScenario(name: String) {
        println("\n========== SearchScreenSearchApiUnitTest: $name ==========")
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

    private fun textSearchRequestUrl(): String {
        return TEST_SEARCH_BASE_URL.trimEnd('/') + "/" + TEXT_SEARCH_ENDPOINT_PATH.trimStart('/')
    }

    private class FakeSearchService : SearchService(apiKey = null, signatureSha1 = null) {
        var lastTextSearchRequest: TextSearchRequest? = null
            private set

        override suspend fun textSearch(request: TextSearchRequest): Result<TextSearchResponse> {
            lastTextSearchRequest = request

            val response = when (request.query) {
                NORMAL_KEYWORD -> EXPECTED_NORMAL_RESPONSE
                NO_MATCH_KEYWORD -> EXPECTED_NO_MATCH_RESPONSE
                else -> TextSearchResponse(
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
