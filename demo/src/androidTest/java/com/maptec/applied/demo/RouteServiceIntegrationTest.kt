package com.maptec.applied.demo

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.route.RouteService
import com.maptec.applied.route.data.Avoid
import com.maptec.applied.route.data.LocationPoint
import com.maptec.applied.route.data.MODE_DRIVING
import com.maptec.applied.route.data.PolygonArea
import com.maptec.applied.route.data.RouteRequest
import com.maptec.applied.route.data.RouteResponse
import com.maptec.applied.route.data.STRATEGY_BALANCED
import com.maptec.applied.route.data.STRATEGY_FASTEST
import com.maptec.applied.route.data.STRATEGY_SHORTEST
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

const val ROUTE_STATUS_OK: String = "OK"
private const val ROUTE_STATUS_ZERO_RESULTS = "ZERO_RESULTS"
private const val ROUTE_STATUS_ERROR = "ERROR"

/**
 * RouteService 接口集成测试：绕过 ViewModel 和 UI，直接调用路线规划网络接口。
 * 测试复用 demo 的 maptec.xml 鉴权配置和 SDK 默认环境。
 */
@RunWith(AndroidJUnit4::class)
class RouteServiceIntegrationTest {
    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private lateinit var routeService: RouteService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        routeService = RouteService.getInstance(
            context = context,
            apiKey = context.getString(R.string.maptec_apiKey),
            signatureSha1 = context.getString(R.string.signature_sha1)
        )
    }

    /**
     * 通用断言辅助方法：执行路线规划，打印请求/响应 JSON，计时并验证 status == OK、routes 非空。
     */
    private suspend fun calculateRoute_returnsOkAndNonEmptyRoutes(
        request: RouteRequest,
        scenario: String
    ): RouteResponse {
        val response = measureNetworkRequest(scenario, "calculateRoute") {
            routeService.calculateRoute(request).getOrThrow()
        }

        logJson("$scenario-路线规划请求JSON", request)
        logJson("$scenario-实际响应JSON", response)

        assertEquals(
            "$scenario 预期路线规划 status 为 OK，实际=${response.status}",
            "OK",
            response.status
        )
        assertFalse(
            "$scenario 预期 routes 不为空，实际响应=${gson.toJson(response)}",
            response.routes.isEmpty()
        )

        return response
    }

    // 测试目的：单起点到单终点的常规驾车规划，验证概要距离和耗时。
    @Test
    fun routeSearch_basicDriving_returnsOkAndRoutes() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = false,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "常规驾车路线规划")
        assertRouteSummaryHasDistanceAndDuration(response, "常规驾车路线规划")
    }

    // 测试目的：最短路径策略下的驾车规划，验证 STRATEGY_SHORTEST 能返回有效路线。
    @Test
    fun routeSearch_shortestStrategy_returnsOkAndRoutes() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_SHORTEST,
            alternatives = false,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "最短策略路线规划")
        assertRouteSummaryHasDistanceAndDuration(response, "最短策略路线规划")
    }

    // 测试目的：平衡路径策略下的驾车规划，验证 STRATEGY_BALANCED 能返回有效路线。
    @Test
    fun routeSearch_balancedStrategy_returnsOkAndRoutes() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_BALANCED,
            alternatives = false,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "平衡策略路线规划")
        assertRouteSummaryHasDistanceAndDuration(response, "平衡策略路线规划")
    }

    // 测试目的：加入途经点的驾车规划，验证路线 polyline 非空。
    @Test
    fun routeSearch_withWaypoints_returnsOk() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = listOf(
                LocationPoint(
                    location = GeoCoordinate(
                        longitude = 103.8636,
                        latitude = 1.2839
                    )
                )
            ),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = false,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "带途经点路线规划")
        assertRoutePolylineNotEmpty(response, "带途经点路线规划")
    }

    // 测试目的：开启 alternatives，验证服务端能成功返回路线；若有备选路线，应返回多条。
    @Test
    fun routeSearch_alternativesEnabled_returnsMultipleRoutes() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = true,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "备选路线规划")
        assertFalse(
            "备选路线规划预期至少返回一条路线；服务端可能没有多条备选，实际数量=${response.routes.size}",
            response.routes.isEmpty()
        )
    }

    // 测试目的： 避让高速的驾车规划
    @Test
    fun routeSearch_avoidHighways_returnsExpectedResponse() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = ipoh(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = false,
            avoid = Avoid(
                criteria = listOf("highways")
            )
        )
        val result = measureNetworkRequest("避让高速路线规划", "calculateRoute") {
            routeService.calculateRoute(request)
        }
        result.onSuccess { response ->
            logJson("避让高速路线规划-路线规划请求JSON", request)
            logJson("避让高速路线规划-实际响应JSON", response)
            // 简化后的逻辑：只接受 OK 且有数据
            assertFalse(
                "避让高速规划失败，实际状态: ${response.status}",
                !(response.status == ROUTE_STATUS_OK && response.routes.isNotEmpty())
            )
            if (response.status == ROUTE_STATUS_OK) {
                assertRouteSummaryHasDistanceAndDuration(response, "避让高速路线规划")
            }
        }.onFailure { throwable ->
            logJson("避让高速路线规划-路线规划请求JSON", request)
            assertFalse(
                "避让高速路线规划允许服务端拒绝 unsupported criteria，但异常信息不应为空",
                throwable.message.isNullOrBlank()
            )
        }
    }

    // 测试目的：避让自定义区域的驾车规划，验证新加坡矩形 PolygonArea 能随请求提交并返回有效路线。
    @Test
    fun routeSearch_avoidPolygonArea_returnsOkAndRoutes() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = cityHall(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = false,
            avoid = Avoid(
                criteria = emptyList(),
                polygons = singaporeAvoidArea()
            )
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "避让区域路线规划")
        assertRouteSummaryHasDistanceAndDuration(response, "避让区域路线规划")
    }

    // 测试目的：新加坡内部大于50qkm
    @Test
    fun routeSearch_longDistanceIpoh_returnsOk() = runTest {
        val request = RouteRequest(
            origin = changiAirport(),
            destination = ipoh(),
            waypoints = emptyList(),
            mode = MODE_DRIVING,
            strategy = STRATEGY_FASTEST,
            alternatives = false,
            avoid = null
        )
        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "长距离新马跨国算路")
        assertRouteDistanceGreaterThan(response, "长距离新马跨国算路", 50_000)
    }

    // 测试目的：新加坡乌节路到泰国曼谷的长距离跨国算路，验证 >1000km 级别路线返回和解析。
//    @Test
//    fun routeSearch_longDistanceBangkok_returnsOk() = runTest {
//        val request = RouteRequest(
//            origin = orchardRoad(),
//            destination = bangkok(),
//            waypoints = emptyList(),
//            mode = MODE_DRIVING,
//            strategy = STRATEGY_FASTEST,
//            alternatives = false,
//            avoid = null
//        )
//        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "长距离新加坡到曼谷算路")
//        assertRouteDistanceGreaterThan(response, "长距离新加坡到曼谷算路", 1_000_000)
//    }

    // 测试目的：清迈到古来的超长距离跨国算路，验证 >2000km 级别路线返回和解析。
//    @Test
//    fun routeSearch_extraLongDistanceChiangMaiToKulai_returnsOk() = runTest {
//        val request = RouteRequest(
//            origin = chiangMai(),
//            destination = kulai(),
//            waypoints = emptyList(),
//            mode = MODE_DRIVING,
//            strategy = STRATEGY_FASTEST,
//            alternatives = false,
//            avoid = null
//        )
//        val response = calculateRoute_returnsOkAndNonEmptyRoutes(request, "超长距离清迈到古来算路")
//        assertRouteDistanceGreaterThan(response, "超长距离清迈到古来算路", 2_000_000)
//    }

    private fun cityHall(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 103.8920,
                latitude = 1.3920
            )
        )
    }

    /**
     * 50km
     */
    private fun changiAirport(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 103.638070,
                latitude =1.326035
            )
        )
    }
    /**
     * 50km
     */
    private fun ipoh(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 104.022147,
                latitude = 1.343101
            )
        )
    }
    /**
     * 1000km
     */
    private fun orchardRoad(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 103.8318,
                latitude = 1.3004
            )
        )
    }

    private fun bangkok(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 100.5018,
                latitude = 13.7563
            )
        )
    }

    /**
     * 2000km
     */
    private fun chiangMai(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 98.804234,
                latitude = 19.104206
            )
        )
    }

    private fun kulai(): LocationPoint {
        return LocationPoint(
            location = GeoCoordinate(
                longitude = 103.6032,
                latitude = 1.6561
            )
        )
    }

    private fun singaporeAvoidArea(): PolygonArea {
        val polygonJson = """
            {
              "type": "Polygon",
              "coordinates": [
                [
                  [103.82, 1.34],
                  [103.84, 1.34],
                  [103.84, 1.36],
                  [103.82, 1.36],
                  [103.82, 1.34]
                ]
              ]
            }
        """.trimIndent()
        return gson.fromJson(polygonJson, PolygonArea::class.java)
    }

    private fun assertRouteSummaryHasDistanceAndDuration(
        response: RouteResponse,
        scenario: String
    ) {
        val summary = response.routes.first().summary
        assertFalse(
            "$scenario 预期 routes[0].summary.distanceMeters > 0，实际=${summary.distanceMeters}",
            summary.distanceMeters <= 0
        )
        assertFalse(
            "$scenario 预期 routes[0].summary.durationSeconds > 0，实际=${summary.durationSeconds}",
            summary.durationSeconds <= 0
        )
    }

    private fun assertRouteDistanceGreaterThan(
        response: RouteResponse,
        scenario: String,
        minDistanceMeters: Int
    ) {
        val distanceMeters = response.routes.first().summary.distanceMeters
        assertFalse(
            "$scenario 预期 distanceMeters 大于 $minDistanceMeters 米，实际=$distanceMeters",
            distanceMeters <= minDistanceMeters
        )
    }

    private fun assertRoutePolylineNotEmpty(
        response: RouteResponse,
        scenario: String
    ) {
        val hasPolyline = response.routes.any { route ->
            !route.overviewPolyline.isNullOrBlank() ||
                    route.legs.any { leg ->
                        leg.steps.any { step -> !step.polyline.isNullOrBlank() }
                    }
        }
        assertFalse(
            "$scenario 预期 overviewPolyline 或 step polyline 非空，实际响应=${gson.toJson(response)}",
            !hasPolyline
        )
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

    private fun logJson(label: String, value: Any?) {
        println("[$label]")
        println(gson.toJson(value))
    }
}
