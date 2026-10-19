package com.maptec.applied.demo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.Overlay
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.marker.Marker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 地理围栏 Screen 功能测试（PointGeofenceScreen / PolygonGeofenceScreen）。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class GeofenceScreenTest {

    companion object {
        private const val TAG_SCREEN = "geofence_screen"
        private const val TAG_MAP = "mapView"
        private const val TAG_LOAD_POINT = "geofence_load_point"
        private const val TAG_LOAD_POLYGON = "geofence_load_polygon"
        private const val TAG_CLEAR_ALL = "geofence_clear_all"
        private const val TAG_CLEAR_MARKER = "geofence_clear_marker"
        private const val TAG_STATUS = "geofence_status"

        /** 园区多边形围栏内部点 */
        private val INSIDE_POLYGON = LatLng(39.907, 116.392)

        /** 园区多边形围栏外部点 */
        private val OUTSIDE_POLYGON = LatLng(39.900, 116.380)

        /** Point 圆形围栏中心（总部大楼） */
        private val CIRCLE_CENTER = LatLng(39.9087, 116.3975)

        /** Point 圆形围栏名称（与 GeofenceData.POINT_GEOFENCE_JSON 一致） */
        private const val POINT_FENCE_NAME = "总部大楼"

        /** Polygon 围栏名称（与 GeofenceData.POLYGON_GEOFENCE_JSON 一致） */
        private const val POLYGON_FENCE_NAME = "园区围栏"
    }

    private fun getString(resId: Int, vararg args: Any): String = getTestString(resId, *args)

    private fun expectedMarkerInsideStatus(fenceName: String = POINT_FENCE_NAME): String =
        getString(R.string.geofence_marker_inside, fenceName)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        openGeofenceDemo(R.string.overlay_item_geofence_polygon)
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun openGeofenceDemo(itemResId: Int) {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(itemResId)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(TAG_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
    }

    /** 配置面板内按钮需先展开再点击。 */
    private fun clickSheetButton(tag: String) {
        composeTestRule.expandConfigPanel()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    /** 地图交互前收起右侧配置面板，避免遮挡点击区域。 */
    private fun prepareMapForInteraction() {
        composeTestRule.collapseConfigPanel()
        composeTestRule.waitForIdle()
    }

    private fun waitForCircleFenceReady(timeoutMs: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (queryOverlayAt(CIRCLE_CENTER) is Circle) return
            Thread.sleep(200)
        }
        throw AssertionError("Point 围栏 Circle 未在 ${timeoutMs}ms 内加载完成")
    }

    private fun withMapApi(action: (MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("异步获取 MaptecMap 超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    private fun queryOverlayAt(latLng: LatLng): Overlay<*>? {
        var overlay: Overlay<*>? = null
        withMapApi { map ->
            val engine = map.getOverlayEngine()
            val screen = map.projection.toScreenLocation(latLng)
            for (hit in engine.queryOverlayHitsFromPoint(screen)) {
                when (val target = engine.resolveHitTarget(hit)) {
                    is Overlay<*> -> {
                        overlay = target
                        return@withMapApi
                    }
                }
            }
        }
        return overlay
    }

    private fun clickMapAt(latLng: LatLng) {
        prepareMapForInteraction()
        val mapNode = composeTestRule.onNodeWithTag(TAG_MAP)
        var screenX = 0f
        var screenY = 0f
        withMapApi { map ->
            val screen = map.projection.toScreenLocation(latLng)
            screenX = screen.x
            screenY = screen.y
        }
        val bounds = mapNode.fetchSemanticsNode().boundsInRoot
        val offset = Offset(
            screenX.coerceIn(0f, bounds.width),
            screenY.coerceIn(0f, bounds.height),
        )
        mapNode.performTouchInput {
            click(offset)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    private fun clickMapCenter() {
        prepareMapForInteraction()
        composeTestRule.onNodeWithTag(TAG_MAP).performTouchInput {
            click(percentOffset(0.5f, 0.5f))
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    private fun waitForStatusContains(text: String, timeoutMs: Long = 10_000L) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            runCatching {
                composeTestRule.onNodeWithTag(TAG_STATUS).assert(hasText(text, substring = true))
            }.isSuccess
        }
    }

    // ==================== 页面加载 ====================

    // ==================== 围栏绘制 API ====================

    @Test
    fun testDefaultPolygonFence_loaded() {
        val overlay = queryOverlayAt(INSIDE_POLYGON)
        assertNotNull("默认应加载多边形围栏", overlay)
        assertTrue("默认围栏应为 Fill", overlay is Fill)
    }

    @Test
    fun testLoadPointFence_addsCircle() {
        composeTestRule.resetToMainCatalog()
        openGeofenceDemo(R.string.overlay_item_geofence_point)

        waitForCircleFenceReady()
        prepareMapForInteraction()
        // 相机已居中到围栏圆心，点击地图中心即可命中围栏内部
        clickMapCenter()
        val insideStatus = expectedMarkerInsideStatus()
        waitForStatusContains(insideStatus)
        composeTestRule.onNodeWithTag(TAG_STATUS).assert(hasText(insideStatus, substring = true))
    }

    @Test
    fun testLoadPolygonFence_keepsFillAtInsidePoint() {
        clickSheetButton(TAG_LOAD_POLYGON)

        val overlay = queryOverlayAt(INSIDE_POLYGON)
        assertNotNull("Polygon 围栏应覆盖内部点", overlay)
        assertTrue("围栏类型应为 Fill", overlay is Fill)
    }

    @Test
    fun testClearAll_removesFences() {
        clickSheetButton(TAG_CLEAR_ALL)
        Thread.sleep(1000)

        val inside = queryOverlayAt(INSIDE_POLYGON)
        val outside = queryOverlayAt(OUTSIDE_POLYGON)
        assertNull("清除后内部点不应命中围栏", inside)
        assertNull("清除后外部点不应命中围栏", outside)
    }

    // ==================== 命中判定（地图点击） ====================

    @Test
    fun testMapClick_insidePolygon_updatesStatus() {
        prepareMapForInteraction()
        clickMapCenter()
        composeTestRule.onNodeWithTag(TAG_STATUS).assertIsDisplayed()
    }

    // ==================== Marker + Fill.contains API ====================

    private fun getEngineMarkers(): List<Marker> {
        var markers: Collection<Marker> = emptyList()
        withMapApi { map ->
            markers = map.overlayEngine.markers
        }
        return markers.toList()
    }

    private fun waitForMarkerCount(expected: Int, timeoutMs: Long = 8_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().size == expected) return
            Thread.sleep(200)
        }
        fail("Marker 数量未在 ${timeoutMs}ms 内达到 $expected，实际=${getEngineMarkers().size}")
    }

    private fun requirePolygonFill(): Fill {
        val overlay = queryOverlayAt(INSIDE_POLYGON)
        assertNotNull("应存在多边形围栏 Fill", overlay)
        assertTrue("围栏类型应为 Fill", overlay is Fill)
        return overlay as Fill
    }

    @Test
    fun testFillContains_insideOutside_apiVerified() {
        val fill = requirePolygonFill()
        assertTrue("内部点 Fill.contains 应为 true", fill.contains(INSIDE_POLYGON))
        assertFalse("外部点 Fill.contains 应为 false", fill.contains(OUTSIDE_POLYGON))
    }

    @Test
    fun testMapClick_inside_placesMarker_apiVerified() {
        prepareMapForInteraction()
        clickMapCenter()
        waitForMarkerCount(1)
        val marker = getEngineMarkers().single()
        assertTrue(
            "Marker 应落在围栏内",
            requirePolygonFill().contains(marker.latLng),
        )
        waitForStatusContains(expectedMarkerInsideStatus(fenceName = POLYGON_FENCE_NAME))
    }

    @Test
    fun testMapClick_outside_statusOutside_apiVerified() {
        // 先拿到 Fill 引用（后续相机会移到围栏外，拾取可能失败）
        val fill = requirePolygonFill()
        withMapApi { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_POLYGON, 15.0))
        }
        Thread.sleep(1000)
        clickMapAt(OUTSIDE_POLYGON)
        waitForMarkerCount(1)
        val marker = getEngineMarkers().single()
        assertFalse("Marker 应落在围栏外", fill.contains(marker.latLng))
        waitForStatusContains(getString(R.string.geofence_marker_outside))
    }

    @Test
    fun testClearMarker_removesMarker_apiVerified() {
        prepareMapForInteraction()
        clickMapCenter()
        waitForMarkerCount(1)

        composeTestRule.onNodeWithTag(TAG_CLEAR_MARKER).performClick()
        composeTestRule.waitForIdle()
        waitForMarkerCount(0)
        waitForStatusContains(getString(R.string.geofence_status_clear_marker))
    }
}
