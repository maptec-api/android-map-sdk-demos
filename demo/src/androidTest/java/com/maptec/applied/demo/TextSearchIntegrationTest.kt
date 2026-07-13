package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.TextSearchResponse
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TextSearch 专项集成测试：直接调用 SearchService.textSearch。
 */
@RunWith(AndroidJUnit4::class)
class TextSearchIntegrationTest {

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
     * 通用断言辅助方法：验证文本搜索返回 OK 且 places 不为空。
     */
    private suspend fun textSearch_returnsOkAndNonEmptyPlaces(
        request: TextSearchRequest,
        scenario: String
    ): TextSearchResponse {
        val response = measureNetworkRequest(scenario, "textSearch") {
            searchService.textSearch(request).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        val requestJson = gson.toJson(request)
        val responseJson = gson.toJson(response)
        assertTrue(
            textSearchAssertionMessage(
                scenario = scenario,
                reason = "文本搜索 status 应为 OK，实际为 ${response.status}",
                requestJson = requestJson,
                responseJson = responseJson
            ),
            response.status == ResponseStatus.OK
        )
        assertFalse(
            textSearchAssertionMessage(
                scenario = scenario,
                reason = "文本搜索应返回至少一个 place",
                requestJson = requestJson,
                responseJson = responseJson
            ),
            response.places.isNullOrEmpty()
        )
        val pageSize = request.pageSize
        assertTrue(
            textSearchAssertionMessage(
                scenario = scenario,
                reason = "文本搜索返回数量不应超过请求 pageSize=$pageSize，实际=${response.places.orEmpty().size}",
                requestJson = requestJson,
                responseJson = responseJson
            ),
            pageSize == null || response.places.orEmpty().size <= pageSize
        )

        return response
    }

    private fun textSearchAssertionMessage(
        scenario: String,
        reason: String,
        requestJson: String,
        responseJson: String
    ): String {
        return """
            $scenario 断言失败：$reason

            $scenario-请求JSON:
            $requestJson

            $scenario-实际响应JSON:
            $responseJson
        """.trimIndent()
    }

    // 测试目的：验证搜索“新加坡酒店”能正常返回结果，并校验返回地点包含名称和地址等基础字段。
    @Test
    fun textSearch_hotelInSingapore_returnsOkAndNonEmptyPlaces() = runTest {
        val request = TextSearchRequest(
            query = "hotel in Singapore",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 1,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "新加坡酒店基础搜索")
        assertPlaceHasNameAndAddress(result)
    }

    // 测试目的：验证 pageSize=5 时服务端最多返回 5 条酒店结果。
    @Test
    fun textSearch_hotelInSingapore_pageSizeFive_returnsAtMostFivePlaces() = runTest {
        val request = TextSearchRequest(
            query = "hotel in Singapore",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 5,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "新加坡酒店pageSize=5")
        assertTrue("pageSize=5 时返回结果不应超过 5 条", result.places.orEmpty().size <= 5)
    }

    // 测试目的：验证类别 type=cafe 结合 Singapore 文本搜索能返回 cafe 相关结果。
    @Test
    fun textSearch_cafeWithType_returnsCafeRelatedPlaces() = runTest {
        val request = TextSearchRequest(
            query = "cafe in Singapore",
            type = "cafe",
            locationBias = null,
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "新加坡cafe类型搜索")
        assertAnyPlaceTypeContains(result, "cafe")
    }

    // 测试目的：验证 locationBias 圆形偏向参数能用于搜索 Singapore 附近酒店。
    @Test
    fun textSearch_hotelWithCircleLocationBias_returnsPlaces() = runTest {
        val request = TextSearchRequest(
            query = "hotel",
            type = null,
            locationBias = LocationBias.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "圆形位置偏向酒店搜索")
        assertPlaceHasNameAndAddress(result)
    }

    // 测试目的：验证 locationBias 矩形偏向参数能用于搜索 Singapore 范围内餐厅。
    @Test
    fun textSearch_restaurantWithRectangleLocationBias_returnsPlaces() = runTest {
        val request = TextSearchRequest(
            query = "restaurant",
            type = null,
            locationBias = singaporeRectangleBias(),
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "矩形位置偏向餐厅搜索")
        assertPlaceHasNameAndAddress(result)
    }

    // 测试目的：验证 locationLimit 圆形限制参数能限制搜索 Singapore 附近 cafe。
    @Test
    fun textSearch_cafeWithCircleLocationLimit_returnsCafeRelatedPlaces() = runTest {
        val request = TextSearchRequest(
            query = "cafe",
            type = "cafe",
            locationBias = null,
            locationLimit = LocationLimit.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "圆形位置限制cafe搜索")
        assertAnyPlaceTypeContains(result, "cafe")
    }

    // 测试目的：验证 locationLimit 矩形限制参数能限制 Singapore 范围内酒店搜索。
    @Test
    fun textSearch_hotelWithRectangleLocationLimit_returnsPlaces() = runTest {
        val request = TextSearchRequest(
            query = "hotel",
            type = null,
            locationBias = null,
            locationLimit = singaporeRectangleLimit(),
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "矩形位置限制酒店搜索")
        assertPlaceHasNameAndAddress(result)
    }

    // 测试目的：验证 rank=RELEVANCE 时类别搜索能返回相关餐厅结果。
    @Test
    fun textSearch_restaurantWithRelevanceRank_returnsRestaurantRelatedPlaces() = runTest {
        val request = TextSearchRequest(
            query = "restaurant",
            type = "restaurant",
            locationBias = LocationBias.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = RankType.RELEVANCE,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "相关性排序餐厅搜索")
        assertAnyPlaceTypeContains(result, "restaurant")
    }

    // 测试目的：验证 rank=DISTANCE 时类别搜索能按距离场景返回 Singapore 附近 cafe。
    @Test
    fun textSearch_cafeWithDistanceRank_returnsCafeRelatedPlaces() = runTest {
        val request = TextSearchRequest(
            query = "cafe",
            type = "cafe",
            locationBias = LocationBias.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = RankType.DISTANCE,
            language = "en",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "距离排序cafe搜索")
        assertAnyPlaceTypeContains(result, "cafe")
    }

    // 测试目的：验证 pageToken 翻页参数能在存在下一页 token 时继续获取后续结果。
    @Test
    fun textSearch_hotelWithPageToken_returnsNextPagePlacesWhenTokenExists() = runTest {
        val firstPageRequest = TextSearchRequest(
            query = "hotel in Singapore",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 1,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG",
        )
        val firstPage = textSearch_returnsOkAndNonEmptyPlaces(firstPageRequest, "酒店搜索第一页")
        val nextPageToken = firstPage.nextPageToken?.trim().orEmpty()
        assertFalse(
            "pageSize=1 的酒店搜索应返回 nextPageToken 以验证 pageToken 参数",
            nextPageToken.isEmpty()
        )

        val secondPageRequest = TextSearchRequest(
            query = "hotel in Singapore",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 1,
            pageToken = nextPageToken,
            rank = null,
            language = "en",
            region = "SG",
        )
        val secondPage = textSearch_returnsOkAndNonEmptyPlaces(secondPageRequest, "酒店搜索第二页")
        assertFalse(
            "第二页首个地点不应与第一页首个地点相同",
            firstPage.places?.firstOrNull()?.id == secondPage.places?.firstOrNull()?.id
        )
    }

    // 测试目的：验证中文 language 参数下搜索“新加坡酒店”仍能返回结构完整的数据。
    @Test
    fun textSearch_hotelWithChineseLanguage_returnsLocalizedPlaces() = runTest {
        val request = TextSearchRequest(
            query = "新加坡酒店",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 3,
            pageToken = null,
            rank = null,
            language = "zh-CN",
            region = "SG",
        )
        val result = textSearch_returnsOkAndNonEmptyPlaces(request, "中文新加坡酒店搜索")
        assertPlaceHasNameAndAddress(result)
    }

    // 测试目的：无结果关键词不应返回地点；兼容服务端用 ZERO_RESULTS 或 OK + 空 places 表达空结果。
    @Test
    fun textSearch_impossibleQuery_returnsZeroResults() = runTest {
        val request = TextSearchRequest(
            query = "qzxjkvbnonexistentpoi000000000000",
            type = null,
            locationBias = null,
            locationLimit = null,
            pageSize = 5,
            pageToken = null,
            rank = null,
            language = "en",
            region = "SG"
        )
        val response = measureNetworkRequest("文本搜索无结果", "textSearch") {
            searchService.textSearch(request).getOrThrow()
        }

        logJson("文本搜索无结果-请求JSON", request)
        logJson("文本搜索无结果-实际响应JSON", response)
        assertNoTextSearchResults(response)
    }

    private fun assertAnyPlaceTypeContains(response: TextSearchResponse, expectedType: String) {
        val hasExpectedType = response.places.orEmpty().any { place ->
            place.types.orEmpty().any { type ->
                type.contains(expectedType, ignoreCase = true)
            }
        }
        assertTrue(
            "至少一个搜索结果的 types 应包含 $expectedType，实际 places=${gson.toJson(response.places)}",
            hasExpectedType
        )
    }

    private fun assertNoTextSearchResults(response: TextSearchResponse) {
        val isZeroResults = response.status == ResponseStatus.ZERO_RESULTS
        val isOkWithEmptyPlaces =
            response.status == ResponseStatus.OK && response.places.isNullOrEmpty()
        assertTrue(
            """
            无结果文本搜索应返回 ZERO_RESULTS，或返回 OK 且 places 为空。
            Actual:
            ${gson.toJson(response)}
            """.trimIndent(),
            isZeroResults || isOkWithEmptyPlaces
        )
    }

    private fun assertPlaceHasNameAndAddress(response: TextSearchResponse) {
        response.places.orEmpty().forEach { place ->
            assertFalse("place id=${place.id} 的 name 不应为空", place.name.isNullOrBlank())
            assertFalse(
                "place id=${place.id} 的 formattedAddress 不应为空",
                place.formattedAddress.isNullOrBlank()
            )
        }
    }

    private fun singaporeCenter(): GeoCoordinate {
        return GeoCoordinate(
            longitude = 103.8198,
            latitude = 1.3521
        )
    }

    private fun singaporeRectangleBias(): LocationBias.Rectangle {
        return LocationBias.Rectangle(
            northeast = GeoCoordinate(longitude = 104.05, latitude = 1.48),
            southwest = GeoCoordinate(longitude = 103.60, latitude = 1.20)
        )
    }

    private fun singaporeRectangleLimit(): LocationLimit.Rectangle {
        return LocationLimit.Rectangle(
            northeast = GeoCoordinate(longitude = 104.05, latitude = 1.48),
            southwest = GeoCoordinate(longitude = 103.60, latitude = 1.20)
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
