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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PlaceDetail 专项集成测试：通过文本搜索获取有效 placeId 后调用 SearchService.getPlaceDetail。
 */
@RunWith(AndroidJUnit4::class)
class PlaceDetailIntegrationTest {

    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private lateinit var searchService: SearchService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        searchService = SearchService.getInstance(
            context = context,
            apiKey = context.getString(R.string.maptec_apiKey),
            signatureSha1 = context.getString(R.string.signature_sha1)
        )
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
        scenario: String
    ): PlaceDetailResponse {
        val placeId = firstPlaceIdFromTextSearch(
            query = query,
            language = searchLanguage,
            region = searchRegion
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
        scenario: String
    ): PlaceDetailResponse {
        val request = placeDetailRequestMap(
            placeId = placeId,
            language = language,
            region = region
        )
        val response = measureNetworkRequest(scenario, "placeDetail") {
            searchService.getPlaceDetail(
                placeId = placeId,
                language = language,
                region = region
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
            scenario = "英文地址地点详情"
        )
        assertPlaceDetailHasFormattedAddress(response.places, "英文地址地点详情")
    }

    private suspend fun firstPlaceIdFromTextSearch(
        query: String = "hotel in Singapore",
        language: String = "en",
        region: String = "SG"
    ): String {
        val request = TextSearchRequest(
            query = query,
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 1,
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

        val place = response.places?.firstOrNull()
            ?: throw AssertionError("无法从文本搜索结果中获取有效 placeId，请配置 search.test.placeId。")

        return place.id
            ?: place.name?.removePrefix("places/")
            ?: throw AssertionError("文本搜索结果缺少 id/name，无法继续地点详情集成测试。")
    }

    private fun placeDetailRequestMap(
        placeId: String,
        language: String?,
        region: String?
    ): Map<String, String?> {
        return mapOf(
            "placeId" to placeId,
            "language" to language,
            "region" to region
        )
    }

    private fun assertPlaceDetailHasCoreFields(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse("$scenario 预期 displayName 不为空", actual.displayName.text.isBlank())
        assertFalse("$scenario 预期 location 不为空", actual.location == null)
        assertFalse("$scenario 预期 formattedAddress 不为空", actual.formattedAddress.isNullOrBlank())
    }

    private fun assertPlaceDetailHasDisplayNameAndLocation(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse("$scenario 预期 displayName 不为空", actual.displayName.text.isBlank())
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
