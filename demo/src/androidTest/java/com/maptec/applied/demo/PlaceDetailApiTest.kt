package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.PlaceDetailResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.TextSearchResponse
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PlaceDetail 专项集成测试：通过文本搜索获取有效 placeId 后调用 SearchService.getPlaceDetail。
 */
@RunWith(AndroidJUnit4::class)
class PlaceDetailApiTest {

    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private lateinit var searchService: SearchService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        searchService = SearchService.getInstance(context)
    }

    /**
     * 通用断言辅助方法：先通过文本搜索拿到有效 Place ID，再请求地点详情。
     */
    private suspend fun placeDetailFromTextSearch_returnsOkAndPlace(
        query: String,
        searchLanguage: String,
        searchRegion: String,
        detailLanguage: String?,
        detailRegion: String?,
        scenario: String,
        searchType: String? = null,
    ): PlaceDetailResponse {
        val placeId = firstPlaceIdFromTextSearch(
            query = query,
            language = searchLanguage,
            region = searchRegion,
            type = searchType,
        )
        return placeDetail_returnsOkAndPlace(
            placeId = placeId,
            language = detailLanguage,
            region = detailRegion,
            scenario = scenario
        )
    }

    /**
     * 通用断言辅助方法：执行地点详情请求，打印请求/响应 JSON，并验证 status == OK、places 不为空。
     */
    private suspend fun placeDetail_returnsOkAndPlace(
        placeId: String,
        language: String?,
        region: String?,
        scenario: String,
        extraFields: List<String>? = null,
    ): PlaceDetailResponse {
        val request = placeDetailRequestMap(
            placeId = placeId,
            language = language,
            region = region,
            extraFields = extraFields,
        )
        val response = measureNetworkRequest(scenario, "placeDetail") {
            searchService.getPlaceDetail(
                placeId = placeId,
                language = language,
                region = region,
                extraFields = extraFields,
            ).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        assertEquals(
            "$scenario 预期地点详情 status 为 OK，实际=${response.status}",
            ResponseStatus.OK,
            response.status
        )
        assertFalse(
            "$scenario 预期地点详情 places 不为空，实际响应=${gson.toJson(response)}",
            response.places == null
        )
        return response
    }

    // 测试目的：先通过文本搜索获取有效 placeId，再请求地点详情，验证核心字段完整。
    @Test
    fun placeDetail_fromTextSearchPlaceId_containsCoreFields() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "hotel in Singapore",
            searchLanguage = "en",
            searchRegion = "SG",
            detailLanguage = null,
            detailRegion = "SG",
            scenario = "PlaceId地点详情"
        )
        assertPlaceDetailHasCoreFields(response.places, "PlaceId地点详情")
    }

    // 测试目的：请求地点详情时传入 language=en，验证英文场景下 displayName 和 location 可用。
    @Test
    fun placeDetail_withEnglishLanguage_containsDisplayNameAndLocation() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "hotel in Singapore",
            searchLanguage = "en",
            searchRegion = "SG",
            detailLanguage = "en",
            detailRegion = "SG",
            scenario = "英文地点详情"
        )
        assertPlaceDetailHasDisplayNameAndLocation(response.places, "英文地点详情")
    }

    // 测试目的：使用另一个文本搜索结果的 placeId，并传入 language=en，验证地址文本字段可用。
    @Test
    fun placeDetail_withEnglishLanguage_containsFormattedAddress() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "cafe in Singapore",
            searchLanguage = "en",
            searchRegion = "SG",
            detailLanguage = "en",
            detailRegion = "SG",
            scenario = "英文地址地点详情",
            searchType = "Cafe_Tea_Dessert",
        )
        assertPlaceDetailHasFormattedAddress(response.places, "英文地址地点详情")
    }

    // 测试目的：带全量 extraFields 请求地点详情，验证 query 编码与扩展字段可解析。
    @Test
    fun placeDetail_withExtraFields_parsesExtendedPlaceFields() = runTest {
        val extraFields = listOf(
            "aoi",
            "summary",
            "openingHours",
            "phoneNumbers",
            "emailAddresses",
            "websiteUris",
            "addressComponents",
            "timeZone",
        )
        val placeId = firstPlaceIdFromTextSearch(
            query = "Anglo-Chinese School",
            language = "en",
            region = "SG",
        )
        val response = placeDetail_returnsOkAndPlace(
            placeId = placeId,
            language = "en",
            region = "SG",
            scenario = "地点详情extraFields",
            extraFields = extraFields,
        )
        val request = placeDetailRequestMap(placeId, "en", "SG", extraFields)
        val requestJson = gson.toJson(request)
        assertTrue(
            "请求应记录 extraFields 逗号串：$requestJson",
            requestJson.contains(extraFields.joinToString(",")),
        )

        val place = requireNotNull(response.places)
        assertPlaceDetailHasCoreFields(place, "地点详情extraFields")
        place.phoneNumbers?.forEach {
            assertFalse("phoneNumbers 元素不应为 blank", it.isNullOrBlank())
        }
        place.emailAddresses?.forEach {
            assertFalse("emailAddresses 元素不应为 blank", it.isNullOrBlank())
        }
        place.websiteUris?.forEach {
            assertFalse("websiteUris 元素不应为 blank", it.isNullOrBlank())
        }
        place.aoi?.let { aoi ->
            assertFalse("aoi.id 不应为空", aoi.id.isNullOrBlank())
            assertFalse("aoi.name 不应为空", aoi.name.isNullOrBlank())
        }
        place.addressComponents?.forEach { component ->
            assertFalse(
                "addressComponents.longName/shortName 不应同时为空",
                component.longName.isNullOrBlank() && component.shortName.isNullOrBlank(),
            )
        }
    }

    // 测试目的：extraFields 为空列表时行为与不传一致（不附带 query）。
    @Test
    fun placeDetail_withEmptyExtraFields_returnsOk() = runTest {
        val placeId = firstPlaceIdFromTextSearch()
        val response = placeDetail_returnsOkAndPlace(
            placeId = placeId,
            language = "en",
            region = "SG",
            scenario = "地点详情空extraFields",
            extraFields = emptyList(),
        )
        assertPlaceDetailHasCoreFields(response.places, "地点详情空extraFields")
    }

    private suspend fun firstPlaceIdFromTextSearch(
        query: String = "hotel in Singapore",
        language: String = "en",
        region: String = "SG",
        type: String? = null,
    ): String {
        val primary = textSearchForPlaceId(query, language, region, type)
        val fallbackQuery = "hotel in Singapore"
        val fallback = if (firstValidPlace(primary) == null && query != fallbackQuery) {
            textSearchForPlaceId(fallbackQuery, language, region, type = null)
        } else {
            null
        }
        val place = firstValidPlace(primary) ?: firstValidPlace(fallback)
        val placeId = place?.id?.takeIf { it.isNotBlank() }
            ?: place?.name?.removePrefix("places/")?.takeIf { it.isNotBlank() }
            ?: InstrumentationRegistry.getArguments().getString("search.test.placeId")
                ?.takeIf { it.isNotBlank() }

        return placeId ?: throw AssertionError(
            "无法从文本搜索结果中获取有效 placeId。" +
                "query=$query, type=$type, status=${primary.status}, " +
                "response=${gson.toJson(primary)}。" +
                "可配置 instrumentation 参数 search.test.placeId 作为兜底。"
        )
    }

    private suspend fun textSearchForPlaceId(
        query: String,
        language: String,
        region: String,
        type: String?,
    ): TextSearchResponse {
        val request = TextSearchRequest(
            query = query,
            type = type,
            locationBias = null,
            locationLimit = null,
            pageSize = 5,
            pageToken = null,
            rank = null,
            language = language,
            region = region
        )
        val scenario = "获取PlaceId-$query"
        val response: TextSearchResponse = measureNetworkRequest(scenario, "textSearch") {
            searchService.textSearch(request).getOrThrow()
        }
        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)
        return response
    }

    private fun firstValidPlace(response: TextSearchResponse?): Place? {
        return response?.places.orEmpty().firstOrNull { place ->
            !place.id.isNullOrBlank() || !place.name.isNullOrBlank()
        }
    }

    private fun placeDetailRequestMap(
        placeId: String,
        language: String?,
        region: String?,
        extraFields: List<String>? = null,
    ): Map<String, String?> {
        return mapOf(
            "placeId" to placeId,
            "language" to language,
            "region" to region,
            "extraFields" to extraFields?.takeIf { it.isNotEmpty() }?.joinToString(","),
        )
    }

    private fun assertPlaceDetailHasCoreFields(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse("$scenario 预期 displayName 不为空", actual.displayName?.text.isNullOrBlank())
        assertFalse("$scenario 预期 location 不为空", actual.location == null)
        assertFalse("$scenario 预期 formattedAddress 不为空", actual.formattedAddress.isNullOrBlank())
    }

    private fun assertPlaceDetailHasDisplayNameAndLocation(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse("$scenario 预期 displayName 不为空", actual.displayName?.text.isNullOrBlank())
        assertFalse("$scenario 预期 location 不为空", actual.location == null)
    }

    private fun assertPlaceDetailHasFormattedAddress(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse(
            "$scenario 预期 formattedAddress 不为空，实际=${actual.formattedAddress}",
            actual.formattedAddress.isNullOrBlank()
        )
    }

    private fun logJson(label: String, value: Any?) {
        println("[$label]")
        println(gson.toJson(value))
    }

    private suspend fun <T> measureNetworkRequest(
        scenario: String,
        apiName: String,
        requestBlock: suspend () -> T
    ): T {
        val startMs = SystemClock.elapsedRealtime()
        return try {
            requestBlock()
        } finally {
            val elapsedMs = SystemClock.elapsedRealtime() - startMs
            println("[$scenario-$apiName-接口耗时]")
            println("${elapsedMs}ms")
        }
    }
}
