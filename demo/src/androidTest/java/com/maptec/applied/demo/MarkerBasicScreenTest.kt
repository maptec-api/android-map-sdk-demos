package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.style.Property
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
/**
 * [MarkerBasicScreen] 功能测试：基础标记添加与样式参数 API 验证。
 *
 * 对应页面：覆盖物 → 基础标记
 */
@RunWith(AndroidJUnit4::class)
class MarkerBasicScreenTest {

    companion object {
        private const val TAG_INPUT_LATLNG = "symbol_input_default_latlng"
        private const val TAG_INPUT_ICON_SIZE = "symbol_input_icon_size"
        private const val TAG_BTN_ADD_BY_TYPE = "symbol_btn_add_by_type"
        private const val TAG_BTN_CLEAR_ALL = "symbol_btn_clear_all"
        private const val TAG_HAS_MARKERS = "symbol_layer_has_markers"

        private const val DEFAULT_LAT = 1.4
        private const val DEFAULT_LNG = 103.75
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
        navigateToMarkerBasicScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
        // 页面进入时会在 onMapReady 中自动添加一个默认示例 Marker，
        // 清空以保证每个用例从 0 个 Marker 的干净状态开始。
        resetToEmptyMarkers()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航与辅助 ====================

    private fun navigateToMarkerBasicScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_marker)
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

    private fun getEngineMarkers(): List<Marker> {
        var markers: Collection<Marker> = emptyList()
        withMapApi { map ->
            markers = map.overlayEngine.markers
        }
        return markers.toList()
    }

    private fun waitForMarkerCount(expected: Int, timeoutMs: Long = 5000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().size == expected) return
            Thread.sleep(200)
        }
        fail("Marker 数量未在 ${timeoutMs}ms 内达到 $expected，实际=${getEngineMarkers().size}")
    }

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickPanelButton(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * 页面在 onMapReady 中会异步添加一个默认示例 Marker。
     * 先等待该默认 Marker 生效，再清空，确保每个用例从 0 个 Marker 开始。
     */
    private fun resetToEmptyMarkers() {
        waitForAtLeastOneMarker()
        clickPanelButton(TAG_BTN_CLEAR_ALL)
        waitForMarkerCount(0)
    }

    private fun waitForAtLeastOneMarker(timeoutMs: Long = 5000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().isNotEmpty()) return
            Thread.sleep(200)
        }
        fail("默认 Marker 未在 ${timeoutMs}ms 内生效")
    }

    // ==================== 页面加载 ====================

    // ==================== 添加 Marker（API 验证） ====================

    @Test
    fun testAddMarkerByType_apiVerified() {
        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(1)

        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()

        val marker = getEngineMarkers().single()
        assertEquals(DEFAULT_LAT, marker.latLng.latitude, 0.01)
        assertEquals(DEFAULT_LNG, marker.latLng.longitude, 0.01)
        assertEquals(2.0f, marker.iconSize, 0.01f)
        assertEquals(1.0f, marker.iconOpacity, 0.01f)
        assertEquals("Marker 1", marker.text)
        assertTrue(marker.isDraggable)
        assertTrue(marker.isClickable)
        assertEquals(Property.ICON_ANCHOR_BOTTOM, marker.iconAnchor)
    }

    @Test
    fun testAddMarkerByType_twice_increasesCount() {
        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(1)

        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(2)

        val markers = getEngineMarkers()
        assertEquals(2, markers.size)
    }

    @Test
    fun testInvalidLatLng_addButtonDisabled() {
        setInput(TAG_INPUT_LATLNG, "invalid")

        composeTestRule.onNodeWithTag(TAG_BTN_ADD_BY_TYPE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testClearAll_removesMarkers() {
        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(1)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()

        clickPanelButton(TAG_BTN_CLEAR_ALL)
        waitForMarkerCount(0)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertDoesNotExist()
    }

    // ==================== 输入校验 ====================

    @Test
    fun testCustomLatLng_markerPlacedAtPosition() {
        setInput(TAG_INPUT_LATLNG, "1.45,103.80")
        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(1)

        val target = getEngineMarkers().single().latLng
        assertEquals(1.45, target.latitude, 0.01)
        assertEquals(103.80, target.longitude, 0.01)
    }

    @Test
    fun testCustomIconSize_appliedToMarker() {
        setInput(TAG_INPUT_ICON_SIZE, "3.5")
        clickPanelButton(TAG_BTN_ADD_BY_TYPE)
        waitForMarkerCount(1)

        assertEquals(3.5f, getEngineMarkers().single().iconSize, 0.01f)
    }


}
