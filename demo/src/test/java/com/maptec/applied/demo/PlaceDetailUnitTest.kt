package com.maptec.applied.demo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.demo.viewmodel.SearchViewModel
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.api.ApiClient
import com.maptec.applied.search.model.response.ErrorObject
import com.maptec.applied.search.model.response.LocalizedText
import com.maptec.applied.search.model.response.OpeningHours
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.PlaceDetailResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ==================== 统一硬编码入参常量 ====================

private const val NORMAL_PLACE_ID = "detail_place_001"
private const val ERROR_PLACE_ID = "detail_place_error"
private const val DETAIL_LANGUAGE = "en"
private const val DETAIL_REGION = "SG"

private const val EXPECTED_PHONE_NUMBER = "+65 6000 0001"
private const val EXPECTED_FORMATTED_ADDRESS = "88 Detail Road, Singapore"
private const val EXPECTED_WEEKDAY_TEXT = "Monday: 09:00-18:00"

private const val TEST_SEARCH_BASE_URL = "http://127.0.0.1/"
private const val PLACE_DETAIL_ENDPOINT_PATH = "search/v1/place"

// ==================== 统一硬编码预期结果常量 ====================

private val EXPECTED_PLACE_DETAIL_RESPONSE = PlaceDetailResponse(
    // 地点详情成功：校验响应状态必须是 OK。
    status = ResponseStatus.OK,
    // 地点详情成功：校验电话、地址、营业时间、坐标和摘要等字段能被正确解析并保存。
    places = Place(
        id = NORMAL_PLACE_ID,
        name = "places/$NORMAL_PLACE_ID",
        displayName = LocalizedText(text = "MapTec Detail Store", languageCode = DETAIL_LANGUAGE),
        types = listOf("store", "point_of_interest"),
        formattedAddress = EXPECTED_FORMATTED_ADDRESS,
        phoneNumber = EXPECTED_PHONE_NUMBER,
        location = GeoCoordinate(longitude = 103.8519, latitude = 1.2903),
        openingHours = OpeningHours(
            openNow = true,
            weekdayText = listOf(EXPECTED_WEEKDAY_TEXT)
        ),
        summary = LocalizedText(text = "A mocked place detail for unit testing.", languageCode = DETAIL_LANGUAGE)
    ),
    // 地点详情成功：校验 valid 字段与预期完全一致。
    valid = true,
    // 地点详情成功：校验错误对象为空。
    error = null
)

private val EXPECTED_PLACE_DETAIL_ERROR_RESPONSE = PlaceDetailResponse(
    // 地点详情失败：校验响应状态必须是 ERROR。
    status = ResponseStatus.ERROR,
    // 地点详情失败：校验地点详情为空。
    places = null,
    // 地点详情失败：校验 valid 字段与预期完全一致。
    valid = false,
    // 地点详情失败：校验错误码和错误消息与预设值完全一致。
    error = ErrorObject(
        code = "PLACE_NOT_FOUND",
        message = "Place detail is not available."
    )
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceDetailUnitTest {

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
        viewModel.switchTab(SearchViewModel.SearchTab.DETAIL)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun placeDetail_success_savesExpectedDetailFields() = runTest(mainDispatcher) {
        logScenario("获取地点详情成功")
        logRequestUrl("获取地点详情成功-完整请求URL", placeDetailRequestUrl(NORMAL_PLACE_ID))
        logJson("获取地点详情成功-硬编码入参", normalPlaceDetailInput())

        // 测试场景：给定地点 ID，ViewModel 应透传 placeId/language/region，并保存完整地点详情。
        viewModel.setDetailPlaceId(NORMAL_PLACE_ID)
        viewModel.setLanguageString(DETAIL_LANGUAGE)
        viewModel.setRegionString(DETAIL_REGION)

        // 当前 ViewModel 的详情入口来自地点详情 Tab，对应业务内部的 loadPlaceDetail 调用。
        viewModel.performPlaceDetailFromTab()
        advanceUntilIdle()

        // 关键字段校验：确认地点详情请求参数与硬编码入参完全一致。
        logJson("获取地点详情成功-实际请求对象", fakeSearchService.lastPlaceDetailRequest())
        assertEquals(NORMAL_PLACE_ID, fakeSearchService.lastPlaceId)
        assertEquals(DETAIL_LANGUAGE, fakeSearchService.lastLanguage)
        assertEquals(DETAIL_REGION, fakeSearchService.lastRegion)

        // 结果实体校验：从 ViewModel 保存的接口响应 JSON 还原实际返回实体，并与预期实体全量对比。
        val actualResponse = actualPlaceDetailResponse()
        assertJsonEquals("获取地点详情成功-完整返回实体", EXPECTED_PLACE_DETAIL_RESPONSE, actualResponse)
        assertJsonEquals("获取地点详情成功-ViewModel地点详情", EXPECTED_PLACE_DETAIL_RESPONSE.places, viewModel.placeDetail.value)
        assertEquals(EXPECTED_PHONE_NUMBER, viewModel.placeDetail.value?.phoneNumber)
        assertEquals(EXPECTED_FORMATTED_ADDRESS, viewModel.placeDetail.value?.formattedAddress)
        assertEquals(EXPECTED_WEEKDAY_TEXT, viewModel.placeDetail.value?.openingHours?.weekdayText?.firstOrNull())
        assertEquals(true, viewModel.showPlaceDetail.value)
        assertEquals(false, viewModel.isLoadingPlaceDetail.value)
    }

    @Test
    fun placeDetail_errorResponse_clearsDetailAndSetsToast() = runTest(mainDispatcher) {
        logScenario("获取地点详情失败")
        logRequestUrl("获取地点详情失败-完整请求URL", placeDetailRequestUrl(ERROR_PLACE_ID))
        logJson("获取地点详情失败-硬编码入参", errorPlaceDetailInput())

        // 测试场景：服务端返回 ERROR 时，ViewModel 应清空详情、结束 loading，并设置错误提示。
        viewModel.setDetailPlaceId(ERROR_PLACE_ID)
        viewModel.setLanguageString(DETAIL_LANGUAGE)
        viewModel.setRegionString(DETAIL_REGION)

        viewModel.performPlaceDetailFromTab()
        advanceUntilIdle()

        logJson("获取地点详情失败-实际请求对象", fakeSearchService.lastPlaceDetailRequest())
        assertEquals(ERROR_PLACE_ID, fakeSearchService.lastPlaceId)
        assertEquals(DETAIL_LANGUAGE, fakeSearchService.lastLanguage)
        assertEquals(DETAIL_REGION, fakeSearchService.lastRegion)

        val actualResponse = actualPlaceDetailResponse()
        assertJsonEquals("获取地点详情失败-完整返回实体", EXPECTED_PLACE_DETAIL_ERROR_RESPONSE, actualResponse)
        assertNull(viewModel.placeDetail.value)
        assertEquals(true, viewModel.showPlaceDetail.value)
        assertEquals(false, viewModel.isLoadingPlaceDetail.value)
        assertNotNull(viewModel.toastMessage.value)
        assertTrue(viewModel.toastMessage.value.orEmpty().contains("PLACE_NOT_FOUND"))
    }

    private fun actualPlaceDetailResponse(): PlaceDetailResponse {
        val actualResponseJson = viewModel.placeDetailApiSnapshot.value?.responseJson.orEmpty()
        return gson.fromJson(actualResponseJson, PlaceDetailResponse::class.java)
    }

    private fun setSearchApiClientBaseUrlForUnitTest() {
        val baseUrlField = ApiClient::class.java.getDeclaredField("baseUrl")
        baseUrlField.isAccessible = true
        baseUrlField.set(ApiClient, TEST_SEARCH_BASE_URL)
    }

    private fun normalPlaceDetailInput(): Map<String, Any> {
        return mapOf(
            "placeId" to NORMAL_PLACE_ID,
            "language" to DETAIL_LANGUAGE,
            "region" to DETAIL_REGION
        )
    }

    private fun errorPlaceDetailInput(): Map<String, Any> {
        return mapOf(
            "placeId" to ERROR_PLACE_ID,
            "language" to DETAIL_LANGUAGE,
            "region" to DETAIL_REGION
        )
    }

    private fun logScenario(name: String) {
        println("\n========== PlaceDetailUnitTest: $name ==========")
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

    private fun placeDetailRequestUrl(placeId: String): String {
        return TEST_SEARCH_BASE_URL.trimEnd('/') + "/" + PLACE_DETAIL_ENDPOINT_PATH.trimStart('/') + "/" + placeId
    }

    private class FakeSearchService : SearchService(apiKey = null, signatureSha1 = null) {
        var lastPlaceId: String? = null
            private set
        var lastLanguage: String? = null
            private set
        var lastRegion: String? = null
            private set

        override suspend fun getPlaceDetail(
            placeId: String,
            language: String?,
            region: String?
        ): Result<PlaceDetailResponse> {
            lastPlaceId = placeId
            lastLanguage = language
            lastRegion = region

            val response = when (placeId) {
                NORMAL_PLACE_ID -> EXPECTED_PLACE_DETAIL_RESPONSE
                ERROR_PLACE_ID -> EXPECTED_PLACE_DETAIL_ERROR_RESPONSE
                else -> EXPECTED_PLACE_DETAIL_ERROR_RESPONSE
            }

            return Result.success(response)
        }

        fun lastPlaceDetailRequest(): Map<String, String?> {
            return mapOf(
                "placeId" to lastPlaceId,
                "language" to lastLanguage,
                "region" to lastRegion
            )
        }
    }
}
