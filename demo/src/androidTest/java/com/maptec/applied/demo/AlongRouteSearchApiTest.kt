package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.search.model.common.AlongRouteRankType
import com.maptec.applied.search.model.common.EvaluationMode
import com.maptec.applied.search.model.request.AlongRouteSearchRequest
import com.maptec.applied.search.model.request.RouteGeometry
import com.maptec.applied.search.model.response.AlongRouteSearchResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 沿途搜索专项集成测试：直接调用 SearchService.alongRouteSearch。
 */
@RunWith(AndroidJUnit4::class)
class AlongRouteSearchApiTest {

    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private lateinit var searchService: SearchService

    @Before
    fun setUp() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        searchService = SearchService.getInstance(context)
    }

    private suspend fun alongRouteSearch_returnsOkAndNonEmptyResults(
        request: AlongRouteSearchRequest,
        scenario: String,
    ): AlongRouteSearchResponse {
        val response = measureNetworkRequest(scenario, "alongRouteSearch") {
            searchService.alongRouteSearch(request).getOrThrow()
        }

        logJson("$scenario-请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        assertEquals(
            "$scenario 预期沿途搜索 status 为 OK，实际=${response.status}",
            ResponseStatus.OK,
            response.status,
        )
        assertFalse(
            "$scenario 预期 alongRouteSearch results 不为空，实际响应=${gson.toJson(response)}",
            response.results.isNullOrEmpty(),
        )
        request.resultLimit?.let { limit ->
            assertTrue(
                "$scenario 预期返回数量不超过 resultLimit=$limit，实际=${response.results.orEmpty().size}",
                response.results.orEmpty().size <= limit,
            )
        }
        response.results.orEmpty().forEach { item ->
            assertFalse("$scenario place.id 不应为空", item.place.id.isBlank())
            assertFalse(
                "$scenario place.displayName 不应为空",
                item.place.displayName?.text.isNullOrBlank(),
            )
            assertTrue(
                "$scenario routeProgressRatio 应在 [0,1]，实际=${item.routeRelation.routeProgressRatio}",
                item.routeRelation.routeProgressRatio in 0.0..1.0,
            )
        }
        return response
    }

    @Test
    fun alongRouteSearch_hotelQuery_returnsOkAndNonEmptyResults() = runTest {
        val request = alongRouteRequest(query = "hotel")
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索hotel基础")
    }

    @Test
    fun alongRouteSearch_typesOnly_returnsOkAndNonEmptyResults() = runTest {
        val request = alongRouteRequest(query = null, types = "Hotels")
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索types=Hotels")
    }

    @Test
    fun alongRouteSearch_resultLimitFive_returnsAtMostFive() = runTest {
        val request = alongRouteRequest(query = "restaurant", resultLimit = 5)
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索resultLimit=5")
    }

    @Test
    fun alongRouteSearch_bufferAndLanguageRegion_returnsPlaces() = runTest {
        val request = alongRouteRequest(
            query = "cafe",
            routeBufferMeters = 3000,
            language = "en",
            region = "SG",
            resultLimit = 5,
        )
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索buffer+language+region")
    }

    @Test
    fun alongRouteSearch_relevanceRank_returnsPlaces() = runTest {
        val request = alongRouteRequest(
            query = "hotel",
            rank = AlongRouteRankType.RELEVANCE,
            resultLimit = 5,
        )
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索RELEVANCE排序")
    }

    @Test
    fun alongRouteSearch_routeOrderRank_returnsPlaces() = runTest {
        val request = alongRouteRequest(
            query = "hotel",
            rank = AlongRouteRankType.ROUTE_ORDER,
            resultLimit = 5,
        )
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索ROUTE_ORDER排序")
    }

    @Test
    fun alongRouteSearch_distanceFromRouteRank_returnsPlaces() = runTest {
        val request = alongRouteRequest(
            query = "restaurant",
            rank = AlongRouteRankType.DISTANCE_FROM_ROUTE,
            resultLimit = 5,
        )
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索DISTANCE_FROM_ROUTE排序")
    }

    @Test
    fun alongRouteSearch_standardEvaluation_returnsRouteEvaluation() = runTest {
        AlongRouteSearchAuth.assumeStandardEvaluationAvailable()
        val request = alongRouteRequest(
            query = "hotel",
            evaluationMode = EvaluationMode.STANDARD,
            rank = AlongRouteRankType.ROUTE_DURATION,
            resultLimit = 3,
        )
        val response = alongRouteSearch_returnsOkAndNonEmptyResults(
            request,
            "沿途搜索STANDARD评估",
        )
        val hasEvaluation = response.results.orEmpty().any { it.routeEvaluation != null }
        assertTrue(
            "STANDARD 模式下至少一条结果应包含 routeEvaluation，实际=${gson.toJson(response.results)}",
            hasEvaluation,
        )
        response.results.orEmpty().mapNotNull { it.routeEvaluation }.forEach { eval ->
            assertTrue(
                "estimatedDurationSeconds 应 >= 0，实际=${eval.estimatedDurationSeconds}",
                eval.estimatedDurationSeconds >= 0,
            )
            assertTrue(
                "estimatedDistanceMeters 应 >= 0，实际=${eval.estimatedDistanceMeters}",
                eval.estimatedDistanceMeters >= 0,
            )
            assertFalse("evaluation.profile 不应为空", eval.profile.isNullOrBlank())
        }
    }

    @Test
    fun alongRouteSearch_standardMode_routeDistanceRank_returnsPlaces() = runTest {
        AlongRouteSearchAuth.assumeStandardEvaluationAvailable()
        val request = alongRouteRequest(
            query = "hotel",
            evaluationMode = EvaluationMode.STANDARD,
            rank = AlongRouteRankType.ROUTE_DISTANCE,
            resultLimit = 3,
        )
        alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索ROUTE_DISTANCE排序")
    }

    @Test
    fun alongRouteSearch_withExtraFields_serializesAndParses() = runTest {
        val extraFields = listOf("openingHours", "phoneNumbers", "aoi")
        val request = alongRouteRequest(
            query = "hotel",
            resultLimit = 5,
            extraFields = extraFields,
        )
        val requestJson = gson.toJson(request)
        assertTrue(
            "请求 JSON 应包含 extraFields：$requestJson",
            requestJson.contains("\"extraFields\"") &&
                extraFields.all { requestJson.contains("\"$it\"") },
        )
        val response = alongRouteSearch_returnsOkAndNonEmptyResults(request, "沿途搜索extraFields")
        response.results.orEmpty().forEach { item ->
            item.place.phoneNumbers?.forEach {
                assertFalse("phoneNumbers 元素不应为 blank", it.isNullOrBlank())
            }
            item.place.aoi?.let { aoi ->
                assertFalse("aoi.id 不应为空", aoi.id.isNullOrBlank())
            }
        }
    }

    @Test
    fun alongRouteSearch_impossibleQuery_returnsZeroResultsOrEmpty() = runTest {
        val request = alongRouteRequest(
            query = "qzxjkvbnonexistentpoi000000000000",
            resultLimit = 5,
        )
        val response = measureNetworkRequest("沿途搜索无结果", "alongRouteSearch") {
            searchService.alongRouteSearch(request).getOrThrow()
        }
        logJson("沿途搜索无结果-请求JSON", request)
        logJson("沿途搜索无结果-实际响应JSON", response)

        val isZeroResults = response.status == ResponseStatus.ZERO_RESULTS
        val isOkWithEmpty =
            response.status == ResponseStatus.OK && response.results.isNullOrEmpty()
        assertTrue(
            """
            无结果沿途搜索应返回 ZERO_RESULTS，或 OK 且 results 为空。
            Actual:
            ${gson.toJson(response)}
            """.trimIndent(),
            isZeroResults || isOkWithEmpty,
        )
    }

    private fun alongRouteRequest(
        query: String? = "hotel",
        types: String? = null,
        routeBufferMeters: Int? = 1000,
        evaluationMode: EvaluationMode? = EvaluationMode.NONE,
        rank: AlongRouteRankType? = null,
        resultLimit: Int? = null,
        language: String = "en",
        region: String? = "SG",
        extraFields: List<String>? = null,
    ): AlongRouteSearchRequest {
        return AlongRouteSearchRequest(
            query = query,
            types = types,
            route = RouteGeometry(encodedPolyline = SAMPLE_ENCODED_POLYLINE),
            routeBufferMeters = routeBufferMeters,
            evaluationMode = evaluationMode,
            rank = rank,
            resultLimit = resultLimit,
            language = language,
            region = region,
            extraFields = extraFields,
        )
    }

    private fun logJson(label: String, value: Any?) {
        println("[$label]")
        println(gson.toJson(value))
    }

    private suspend fun <T> measureNetworkRequest(
        scenario: String,
        apiName: String,
        requestBlock: suspend () -> T,
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

    companion object {
        /**
         * 与 Demo [SearchViewModel] 默认路线一致，保证 UI/API 测试使用同一走廊。
         */
        private const val SAMPLE_ENCODED_POLYLINE =
            "qoyF_k|xRRFbCr@XT??HFBDDDBPUb@Ud@OKBER]?OAGY]YKc@Mw@Uk@OoCe@_CY_CSo@Eo@CiC@cLK{BAgLGu@AiAG_AM}@Qs@Uw@]cAu@{@{@o@_AUe@[{@SaAQaAOiAo@cGYyBqDcYSeAM_AWoEMqF_@sJMs@Qe@W_@WUSMQM[Ke@OcBg@aBi@cBi@eA]_AYsAa@a@Me@Oa@Ua@Ki@OcAWq@Qm@Kg@Ic@EOAQAYC[Aa@A[?Y@]?Q@U@Q@YBYDYDYDYDYFi@Ny@TkAZ}Ab@{Ab@uBl@cAXsAZq@TkBj@??gAXe@Nk@N??UF??MDe@La@J??WF]H]H]Ha@He@JSDc@HG@i@Jg@Hi@Jm@Hg@Hm@Hi@Hs@Hi@Fg@FeAJc@Bg@Dm@Fg@BWBa@Be@Be@Bg@BWBk@BeAHk@BQ@a@Bg@Bc@Bc@@a@@a@?[?i@Ag@Ag@Ca@Cc@Cc@Ee@Gc@Gc@Ia@Ia@Ma@Ma@MSIMEUMIE??GCUM_@USOc@]WWSSWYOWQUU][i@MYKUISO]ISQa@IQMYGMQc@Sc@Ui@Qa@KSGOGQMYO[MYKWIQGMKWQe@IQISUo@Ma@Qm@Qk@GSI]IUIWI[Ma@Ok@K[I[GSSo@EMK[Qc@KWISGKKSIMIKIMGKQQIMMKIKKKKIOMYSMIKGKGOIMGSKMEOGSIMESEMEOEKCOCOCQCSCOAMCI?IAIAQASCQASAQAYAQAOCK?SAQAMAMAG?GAI?OASAG?IAI?MAIAE?O?EAK?MAK?OAM?E?GAI?OAQ?QAS?QAG?O?OAS?Q?WAO?YAQ?MASAIAOAKAMAOCGAMCMCICICMEMEOGWMMG[QSOUSOOKKKMOQKQQYIQIOKWSe@O[M[EKQ_@ISUe@_@w@Yi@Wg@[m@Ye@O[QYKSIOQYQ[ISUa@IQKUO[IQIOEMKUGOKWSg@ACMg@MWEMGQGWGOIWEOOg@K[MYK]MYQe@a@}@Ug@Ue@Q[OUO_@KOGMCEOSQOKSYc@]g@a@k@Ya@W]KQs@y@o@u@Y[QSUUKIKKAC]YSSOMYWOKWS_@[c@[MIWQYS]SQKYQg@YUMc@U[QUKUK[MQIGCEC]MYMe@Oi@O_@Kw@Q??m@QmAY_@ISEQEQCSEQCOCMCOASCUCKAOAMAWCOASAa@AUAU?U?S?_@?Y@Q?YB[@a@Da@DK@M@MBM@OBMBWDQDyAZSBODUDOBMBQ@MBM@QBM@C?UBI@G?O@I?M@K?M@U@_@?_@?m@AS?MAWAUCYCa@EUCSC_@G[GYGMCUGICKEOEmBk@oFcBw@Uy@W{@WaJkC_@MUIe@O]M_@OUI_@Os@[mGwCgMoF_Ac@uBeA{BcAsFeCsFgCuF{By@YcA]eA[gCm@eGoA]Mc@KiAYmCm@yGyAeMuC}A_@_GmAiA[qAc@mAg@{@a@KGOIoB_BqAoAuAkBqA}B}C{Ee@u@m@s@y@o@}@a@a@Og@Ku@K[C_@Ay@Aa@E_BKO@}@I_AMqAYuAUSCcAIs@@o@Bs@Fu@JoA\\k@RULkAl@m@\\o@b@s@f@WNKFOBk@d@yIdHqB`BwAhAwOjMMLaBxAgCzBqCvC{@hAk@n@eAlAaAjAw@dA_D`FuCnFGb@Sn@e@tA_@`ASf@GR@V{@fCqBlHAf@m@lEEb@EZMdAA@UrB?h@B\\P^XXVVrElAD?nBh@bAVdBb@^J^HVHTFVFzBn@tAZb@JfDv@\\H\\HVHIVI^I^_A|DCLA@W`ACJQ|@Gb@Q`BVj@RXjAHfCVFYj@eCLk@b@kBFIJAf@J"
    }
}
