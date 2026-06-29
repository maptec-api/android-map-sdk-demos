package com.maptec.applied.demo

import android.Manifest
import android.os.Build
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
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.Overlay
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.fill.Fill
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 地理围栏（GeofenceScreen）功能测试。
 *
 * 对应页面：业务图层 → 地理围栏
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
    }

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        *if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            emptyArray()
        },
    )

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToGeofenceScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToGeofenceScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_overlay)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.overlay_item_geofence))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(TAG_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun expandBottomSheet() {
        composeTestRule.onNodeWithTag(TAG_SCREEN).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        composeTestRule.waitForIdle()
    }

    /** 底部抽屉内按钮无 Scroll 父节点，需先展开再直接点击。 */
    private fun clickSheetButton(tag: String) {
        expandBottomSheet()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
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
        var offset = Offset.Zero
        withMapApi { map ->
            val screen = map.projection.toScreenLocation(latLng)
            offset = Offset(screen.x, screen.y)
        }
        composeTestRule.onNodeWithTag(TAG_MAP).performTouchInput {
            click(offset)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    private fun clickMapCenter() {
        composeTestRule.onNodeWithTag(TAG_MAP).performTouchInput {
            click(percentOffset(0.5f, 0.5f))
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    private fun waitForStatusContains(text: String, timeoutMs: Long = 5000L) {
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
        clickSheetButton(TAG_LOAD_POINT)
        waitForStatusContains("已加载 Point 围栏")

        // 页面内 Circle 命中走 distanceTo，与 Fill 的 overlay pick 不同
        clickMapAt(CIRCLE_CENTER)
        waitForStatusContains("Marker 在围栏")
        composeTestRule.onNodeWithTag(TAG_STATUS).assert(hasText("内", substring = true))
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
        clickMapCenter()
        composeTestRule.onNodeWithTag(TAG_STATUS).assertIsDisplayed()
    }
}
