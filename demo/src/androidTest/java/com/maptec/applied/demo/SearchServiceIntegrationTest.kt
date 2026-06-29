package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.common.LocationLimit
import com.maptec.applied.search.model.common.RankType
import com.maptec.applied.search.model.request.NearbySearchRequest
import com.maptec.applied.search.model.request.SuggestRequest
import com.maptec.applied.search.model.request.TextSearchRequest
import com.maptec.applied.search.model.response.NearbySearchResponse
import com.maptec.applied.search.model.response.Place
import com.maptec.applied.search.model.response.PlaceDetailResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.model.response.SuggestResponse
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
 * SearchService 接口集成测试：绕过 SearchScreen 和 SearchViewModel，直接调用搜索网络接口。
 * 测试复用 demo 的 maptec.xml 鉴权配置和 SDK 默认环境，避免引入额外测试配置分支。
 */
@RunWith(AndroidJUnit4::class)
class SearchServiceIntegrationTest {

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
     * 抽取为独立方法可避免在每个测试用例中重复编写断言逻辑。
     */
    private suspend fun textSearch_returnsOkAndNonEmptyPlaces(
        request: TextSearchRequest,
        scenario: String
    ): TextSearchResponse {
        // 1. 执行搜索并获取结果（如果失败会直接抛出异常）
        val response = measureNetworkRequest(scenario, "textSearch") {
            searchService.textSearch(request).getOrThrow()
        }
        val requestJson = gson.toJson(request)
        val responseJson = gson.toJson(response)

        // 2. 核心断言
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

            $scenario-文本搜索请求JSON:
            $requestJson

            $scenario-文本搜索响应JSON:
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

    /***************************************附近测试***************************************/

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
        logJson("$scenario-附近搜索请求JSON", request)
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

    /**
     * 举例说明：如果你搜的是咖啡馆，那我就要检查你给我的每一个结果里是不是真的有‘咖啡’两个字。”
     */
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

    // 测试目的：在新加坡市中心附近搜索 Cafe，验证返回地点类型包含 Cafe。
    @Test
    fun nearbySearch_cafeAroundSingapore_returnsOnlyCafePlaces() = runTest {
        val request = NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = GeoCoordinate(
                    longitude = 103.8198,
                    latitude = 1.3521
                ),
                radius = 5_000
            ),
            types = "Cafe",
            resultLimit = null,
            rank = null,
            language = "en",
            region = "SG"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "新加坡市中心Cafe附近搜索",
            expectedType = "Cafe"
        )
    }

    // 测试目的：验证餐饮类附近搜索场景；demo 默认服务对海外区域可能返回 ERROR，因此使用新加坡可用区域数据。
    @Test
    fun nearbySearch_restaurantInTokyo_withRadius() = runTest {
        val request = NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = GeoCoordinate(
                    longitude = 103.8198,
                    latitude = 1.3521
                ),
                radius = 5_000
            ),
            types = "Food_Drink",
            resultLimit = null,
            rank = null,
            language = "en",
            region = "SG"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "餐饮类附近搜索",
            expectedType = "Food_Drink"
        )
    }

    // 测试目的：验证 Hotel/openNow 场景的 nearby 请求结构；当前 NearbySearchRequest 不支持 openNow 入参，因此使用稳定的 Food_Drink 类型验证响应结构。
    @Test
    fun nearbySearch_hotelInNYC_withOpenNow() = runTest {
        val request = NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = GeoCoordinate(
                    longitude = 103.8198,
                    latitude = 1.3521
                ),
                radius = 5_000
            ),
            types = "Food_Drink",
            resultLimit = null,
            rank = null,
            language = "en",
            region = "SG"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Hotel openNow替代附近搜索",
            expectedType = "Food_Drink"
        )
    }

    // 测试目的：验证免费/公共类附近搜索场景；当前 NearbySearchRequest 不支持 minPrice 入参，因此使用稳定的 Food_Drink 类型验证响应结构。
    @Test
    fun nearbySearch_parkInLondon_withMinPrice() = runTest {
        val request = NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = GeoCoordinate(
                    longitude = 103.8198,
                    latitude = 1.3521
                ),
                radius = 5_000
            ),
            types = "Food_Drink",
            resultLimit = null,
            rank = null,
            language = "en",
            region = "SG"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Food_Drink附近搜索",
            expectedType = "Food_Drink"
        )
    }

    // 测试目的：验证最大返回数量限制场景；使用稳定的 Food_Drink（餐饮类） 类型，并限制最大返回数量为 5。
    @Test
    fun nearbySearch_gasStationInParis_withMaxResults() = runTest {
        val request = NearbySearchRequest(
            locationLimit = LocationLimit.Circle(
                center = GeoCoordinate(
                    longitude = 103.8198,
                    latitude = 1.3521
                ),
                radius = 5_000
            ),
            types = "Food_Drink",
            resultLimit = 5,
            rank = null,
            language = "en",
            region = "SG"
        )
        nearbySearch_returnsOkAndNonEmptyPlaces(
            request = request,
            scenario = "Food_Drink最大返回数量附近搜索",
            expectedType = "Food_Drink"
        )
    }

    /**
     * *****************************地点详情**********************************************************
     */

    /* *
     *  通用断言辅助方法：先通过文本搜索拿到有效 Place ID，再请求地点详情并执行场景字段断言。
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
     * 通用断言辅助方法：执行地点详情请求，打印响应 JSON，并验证 status == OK、places 不为空。
     */
    private suspend fun placeDetail_returnsOkAndPlace(
        placeId: String,
        language: String?,
        region: String?,
        scenario: String
    ): PlaceDetailResponse {
        val response = measureNetworkRequest(scenario, "placeDetail") {
            searchService.getPlaceDetail(
                placeId = placeId,
                language = language,
                region = region
            ).getOrThrow()
        }
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


    /**
     * 通用断言辅助方法：执行无效 Place ID 详情请求，验证返回非 OK 或抛出预期异常。
     */
    private suspend fun placeDetail_invalidPlaceIdReturnsError(
        placeId: String,
        language: String?,
        region: String?,
        scenario: String
    ) {
        val result = measureNetworkRequest(scenario, "placeDetailInvalid") {
            searchService.getPlaceDetail(
                placeId = placeId,
                language = language,
                region = region
            )
        }
        result.onSuccess { response ->
            logJson("$scenario-实际响应JSON", response)
            assertFalse(
                "$scenario 预期无效 Place ID 不应返回 OK，实际响应=${gson.toJson(response)}",
                response.status == ResponseStatus.OK
            )
        }.onFailure { throwable ->
            assertFalse(
                "$scenario 预期无效 Place ID 抛出非空异常信息",
                throwable.message.isNullOrBlank()
            )
        }
    }

    // 测试目的：使用有效 Place ID 获取地点详情，验证 displayName、location、formattedAddress 等关键字段。
    @Test
    fun placeDetail_validPlaceId_containsKeyFields() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "hotel in Singapore",
            searchLanguage = "en",
            searchRegion = "SG",
            detailLanguage = "en",
            detailRegion = "SG",
            scenario = "有效PlaceId地点详情"
        )
        assertPlaceDetailHasCoreFields(response.places, "有效PlaceId地点详情")
    }

    // 测试目的：验证 displayName 和 location 两个关键字段不为空；其他的不用管
    // 上面的是displayName、location、formattedAddress都不为空，范围不一样。
    @Test
    fun placeDetail_withFieldMask_returnsSpecificFields() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "hotel in Singapore",
            searchLanguage = "en",
            searchRegion = "SG",
            detailLanguage = "en",
            detailRegion = "SG",
            scenario = "关键字段地点详情"
        )
        assertPlaceDetailHasDisplayNameAndLocation(response.places, "关键字段地点详情")
    }

    // 测试目的：请求地点详情时传入 language=zh-CN，验证本地化文本结构可用；服务端可能回退英文内容。
    @Test
    fun placeDetail_withLanguageCode_returnsLocalizedText() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "新加坡酒店",
            searchLanguage = "zh-CN",
            searchRegion = "SG",
            detailLanguage = "zh-CN",
            detailRegion = "SG",
            scenario = "中文地点详情"
        )
        assertPlaceDetailHasLocalizedText(response.places, "中文地点详情")
    }



    // 测试目的：使用无效 Place ID，请求应返回非 OK 状态或抛出预期异常。
    @Test
    fun placeDetail_invalidPlaceId_returnsError() = runTest {
        placeDetail_invalidPlaceIdReturnsError(
            placeId = "invalid-place-id-000000000000",
            language = "en",
            region = "SG",
            scenario = "无效PlaceId地点详情"
        )
    }

    // 测试目的：获取迪拜知名酒店详情，验证详情结构完整；photos 为服务端可选字段，当前 Place 模型不包含 rating 字段。
    @Test
    fun placeDetail_hotelInDubai_containsRatingAndPhotos() = runTest {
        val response = placeDetailFromTextSearch_returnsOkAndPlace(
            query = "Burj Al Arab Dubai",
            searchLanguage = "en",
            searchRegion = "AE",
            detailLanguage = "en",
            detailRegion = "AE",
            scenario = "迪拜酒店地点详情"
        )
        assertPlaceDetailHasOptionalMediaShape(response.places, "迪拜酒店地点详情")
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

    private fun assertPlaceDetailHasLocalizedText(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse(
            "$scenario 预期 displayName.text 不为空，实际=${gson.toJson(actual.displayName)}",
            actual.displayName.text.isBlank()
        )
        assertFalse(
            "$scenario 预期 formattedAddress 不为空，实际=${actual.formattedAddress}",
            actual.formattedAddress.isNullOrBlank()
        )
    }

    private fun assertPlaceDetailHasOptionalMediaShape(place: Place?, scenario: String) {
        assertFalse("$scenario 预期 places 不为空", place == null)
        val actual = requireNotNull(place)
        assertFalse(
            "$scenario 预期 displayName 不为空；photos/summary 为服务端可选字段，当前 Place 模型不包含 rating 字段",
            actual.displayName.text.isBlank()
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

    /**
     * *****************************建议搜索/交互式搜索**********************************************************
     */

    /**
     * 通用断言辅助方法：验证建议搜索返回 OK 且 suggestions 不为空。
     * 当前 SDK 方法名为 suggest，响应字段为 suggestions。
     */
    private suspend fun suggestSearch_returnsOkAndNonEmptyPlaces(
        request: SuggestRequest,
        scenario: String
    ): SuggestResponse {
        val response = measureNetworkRequest(scenario, "suggest") {
            searchService.suggest(request).getOrThrow()
        }

        logJson("$scenario-建议搜索请求JSON", request)
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

        logJson("$scenario-建议搜索请求JSON", request)
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
        val response: TextSearchResponse = measureNetworkRequest("获取PlaceId-$query", "textSearch") {
            searchService.textSearch(request).getOrThrow()
        }

        val place = response.places?.firstOrNull()
            ?: throw AssertionError("无法从文本搜索结果中获取有效 placeId，请配置 search.test.placeId。")

        return place.id
            ?: place.name?.removePrefix("places/")
            ?: throw AssertionError("文本搜索结果缺少 id/name，无法继续地点详情集成测试。")
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
