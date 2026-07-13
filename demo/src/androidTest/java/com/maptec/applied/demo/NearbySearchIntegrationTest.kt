package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.NearbySearchRequest
import com.maptec.applied.search.model.response.NearbySearchResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NearbySearch 专项集成测试：直接调用 SearchService.nearbySearch。
 */
@RunWith(AndroidJUnit4::class)
class NearbySearchIntegrationTest {

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
     * 通用断言辅助方法：执行附近搜索，打印请求/响应 JSON，并验证 status == OK、places 非空。
     */
    private suspend fun nearbySearch_returnsOkAndNonEmptyPlaces(
        request: NearbySearchRequest,
        scenario: String,
        expectedType: String
    ): NearbySearchResponse {
        val response = measureNetworkRequest(scenario, "nearbySearch") {
            searchService.nearbySearch(request).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        assertEquals(
            "$scenario 预期附近搜索 status 为 OK，实际=${response.status}",
            ResponseStatus.OK,
            response.status
        )
        assertFalse(
            "$scenario 预期附近搜索 places 不为空，实际响应=${gson.toJson(response)}",
            response.places.isNullOrEmpty()
        )
        request.resultLimit?.let { limit ->
            assertFalse(
                "$scenario 预期返回数量不超过 resultLimit=$limit，实际=${response.places.orEmpty().size}",
                response.places.orEmpty().size > limit
            )
        }
        assertNearbyPlacesContainType(
            response = response,
            expectedType = expectedType,
            scenario = scenario
        )
        return response
    }

    private fun assertNearbyPlacesContainType(
        response: NearbySearchResponse,
        expectedType: String,
        scenario: String
    ) {
        response.places.orEmpty().forEach { place ->
            val containsType = place.types.any { type ->
                type.contains(expectedType, ignoreCase = true) ||
                        expectedType.contains(type, ignoreCase = true)
            }
            assertFalse(
                "$scenario 预期 place=${place.id} 的 types 包含 $expectedType，实际=${place.types}",
                !containsType
            )
        }
    }

    // 测试目的：验证 types=Chinese_Cuisine 时，附近搜索能返回酒店类型地点。
    @Test
    fun nearbySearch_hotelsType_returnsHotelPlaces() = runTest {
        val request = nearbySearchRequest(types = "Chinese_Cuisine")
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Chinese_Cuisine类型附近搜索",
            expectedType = "Chinese_Cuisine"
        )
    }

    // 测试目的：验证 resultLimit=5 时，附近搜索返回数量不超过 5 条。
    @Test
    fun nearbySearch_hotelsWithResultLimitFive_returnsAtMostFivePlaces() = runTest {
        val request = nearbySearchRequest(
            types = "Chinese_Cuisine",
            resultLimit = 5
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Chinese_Cuisine限制5条附近搜索",
            expectedType = "Chinese_Cuisine"
        )
    }

    // 测试目的：验证 rank=DISTANCE 时，附近搜索能按距离排序场景返回有效地点。
    @Test
    fun nearbySearch_hotelsWithDistanceRank_returnsPlaces() = runTest {
        val request = nearbySearchRequest(
            types = "Chinese_Cuisine",
            rank = RankType.DISTANCE
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Chinese_Cuisine距离排序附近搜索",
            expectedType = "Chinese_Cuisine"
        )
    }

    // 测试目的：验证 language=en 时，附近搜索返回英文场景下的有效地点数据。
    @Test
    fun nearbySearch_hotelsWithEnglishLanguage_returnsPlaces() = runTest {
        val request = nearbySearchRequest(
            types = "Chinese_Cuisine",
            language = "en"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Chinese_Cuisine英文附近搜索",
            expectedType = "Chinese_Cuisine"
        )
    }

    // 测试目的：验证 types=Hotels、resultLimit=5、rank=DISTANCE、language=en 的组合请求。
    @Test
    fun nearbySearch_hotelsWithLimitDistanceRankAndEnglish_returnsPlaces() = runTest {
        val request = nearbySearchRequest(
            types = "Chinese_Cuisine",
            resultLimit = 5,
            rank = RankType.DISTANCE,
            language = "en"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Chinese_Cuisine限制5条距离排序英文附近搜索",
            expectedType = "Chinese_Cuisine"
        )
    }

    private fun nearbySearchRequest(
        types: String,
        resultLimit: Int? = null,
        rank: RankType? = null,
        language: String = "en"
    ): NearbySearchRequest {
        return NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            types = types,
            resultLimit = resultLimit,
            rank = rank,
            language = language,
            region = "SG"
        )
    }

    private fun singaporeCenter(): GeoCoordinate {
        return GeoCoordinate(
            longitude = 103.8198,
            latitude = 1.3521
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
