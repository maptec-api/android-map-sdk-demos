package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.request.SuggestRequest
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.SuggestResponse
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Suggest 专项集成测试：直接调用 SearchService.suggest。
 */
@RunWith(AndroidJUnit4::class)
class SuggestSearchIntegrationTest {

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
     * 通用断言辅助方法：验证建议搜索返回 OK 且 suggestions 不为空。
     */
    private suspend fun suggestSearch_returnsOkAndNonEmptyPlaces(
        request: SuggestRequest,
        scenario: String
    ): SuggestResponse {
        val response = measureNetworkRequest(scenario, "suggest") {
            searchService.suggest(request).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        assertEquals(
            "$scenario 预期建议搜索 status 为 OK，实际=${response.status}",
            ResponseStatus.OK,
            response.status
        )
        assertFalse(
            "$scenario 预期建议搜索 suggestions 不为空，实际响应=${gson.toJson(response)}",
            response.suggestions.isNullOrEmpty()
        )

        return response
    }

    /**
     * 通用断言辅助方法：验证无效关键词建议搜索返回 ZERO_RESULTS 或空 suggestions。
     */
    private suspend fun suggestSearch_returnsZeroResultsOrEmpty(
        request: SuggestRequest,
        scenario: String
    ): SuggestResponse {
        val response = measureNetworkRequest(scenario, "suggest") {
            searchService.suggest(request).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        val isZeroResults = response.status == ResponseStatus.ZERO_RESULTS
        val isOkWithEmptySuggestions = response.status == ResponseStatus.OK && response.suggestions.isNullOrEmpty()
        assertFalse(
            "$scenario 预期建议搜索返回 ZERO_RESULTS，或 OK 且 suggestions 为空，实际响应=${gson.toJson(response)}",
            !(isZeroResults || isOkWithEmptySuggestions)
        )

        return response
    }

    // 测试目的：在新加坡搜索输入 hotel，验证成功返回建议地点列表。
    @Test
    fun suggestSearch_hotelInSingapore_returnsOkAndNonEmptyPlaces() = runTest {
        val request = SuggestRequest(
            query = "hotel",
            types = null,
            locationBias = null,
            locationLimit = null,
            resultLimit = null,
            language = "en",
            region = "SG"
        )
        suggestSearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "新加坡hotel建议搜索"
        )
    }

    // 测试目的：使用圆形限制半径 5000 米搜索 restaurant，验证接口响应 OK。
    @Test
    fun suggestSearch_restaurantInTokyo_withRadius_returnsOk() = runTest {
        val request = SuggestRequest(
            query = "restaurant",
            types = null,
            locationBias = null,
            locationLimit = LocationLimit.Circle(
                center = singaporeCenter(),
                radius = 5_000
            ),
            resultLimit = null,
            language = "en",
            region = "SG"
        )
        suggestSearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "restaurant半径限制建议搜索"
        )
    }

    // 测试目的：输入乱码关键词，验证 ZERO_RESULTS 或空 suggestions 的容错处理。
    @Test
    fun suggestSearch_invalidKeyword_returnsZeroResultsOrEmpty() = runTest {
        val request = SuggestRequest(
            query = "qzxjkvb123456",
            types = null,
            locationBias = null,
            locationLimit = null,
            resultLimit = null,
            language = "en",
            region = "SG"
        )
        suggestSearch_returnsZeroResultsOrEmpty(
            request = request,
            scenario = "乱码关键词建议搜索"
        )
    }

    // 测试目的：搜索 Star 并设置 cafe 类型过滤，验证建议搜索返回 OK。
    @Test
    fun suggestSearch_withCategoryFilter_returnsOk() = runTest {
        val request = SuggestRequest(
            query = "Star",
            types = "cafe",
            locationBias = null,
            locationLimit = null,
            resultLimit = null,
            language = "en",
            region = "SG"
        )
        suggestSearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Star cafe类型建议搜索"
        )
    }

    // 测试目的：模拟当前位置附近搜索 Mall，验证建议搜索返回 OK。
    @Test
    fun suggestSearch_nearCurrentLocation_returnsOk() = runTest {
        val request = SuggestRequest(
            query = "Mall",
            types = null,
            locationBias = LocationBias.Circle(
                center = GeoCoordinate(
                    longitude = 103.8519,
                    latitude = 1.2902
                ),
                radius = 5_000
            ),
            locationLimit = null,
            resultLimit = null,
            language = "en",
            region = "SG"
        )
        suggestSearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "当前位置Mall建议搜索"
        )
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
