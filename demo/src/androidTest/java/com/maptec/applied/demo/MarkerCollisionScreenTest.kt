package com.maptec.applied.demo

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 功能效果类测试（API 验证）：验证标记碰撞避让设置及增删功能。
 *
 * 对应页面：覆盖物 → 标记 → 标记碰撞避让
 */
@RunWith(AndroidJUnit4::class)
class MarkerCollisionScreenTest {

    companion object {
        private const val TAG_DROPDOWN_STRATEGY = "marker_collision_strategy_dropdown"
        private const val TAG_STRATEGY_REQUIRED = "marker_collision_strategy_REQUIRED"
        private const val TAG_STRATEGY_OPTIONAL = "marker_collision_strategy_OPTIONAL_AND_HIDES_LOWER_PRIORITY"
        private const val TAG_STRATEGY_REQUIRED_HIDES_OPTIONAL = "marker_collision_strategy_REQUIRED_AND_HIDES_OPTIONAL"
        private const val TAG_INPUT_Z_ORDER = "marker_collision_z_order_input"
        private const val TAG_BTN_ADD = "marker_collision_add_button"
        private const val TAG_BTN_CLEAR = "marker_collision_clear_button"
        
        // 与 ViewModel 中定义保持一致
        private const val POI_HUB_LAT = 1.3048
        private const val POI_HUB_LNG = 103.8319
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        // 等待地图及底图加载
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_marker_collision)
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

    private fun clickPanelButton(tag: String) {
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun setZOrder(value: String) {
        composeTestRule.onNodeWithTag(TAG_INPUT_Z_ORDER, useUnmergedTree = true).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_INPUT_Z_ORDER, useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag(TAG_INPUT_Z_ORDER, useUnmergedTree = true).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun selectStrategy(strategyTag: String) {
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_STRATEGY, useUnmergedTree = true).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(strategyTag, useUnmergedTree = true).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * 验证默认状态下添加 Marker，其碰撞策略应为 REQUIRED，且文本标签正确生成（REQ-1）。
     */
    @Test
    fun testAddMarker_defaultProperties_verifiesRequiredLabel() {
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)
        
        val markers = getEngineMarkers()
        val marker = markers.first()
        
        // 默认是 REQUIRED，文本为 REQ-1
        assertEquals("REQ-1", marker.text)
    }
    
    /**
     * 验证选择 OPTIONAL_AND_HIDES_LOWER_PRIORITY 策略后添加 Marker，生成的文本标签是否匹配（OPT-1）。
     */
    @Test
    fun testAddMarker_optionalBehavior_verifiesOptionalLabel() {
        selectStrategy(TAG_STRATEGY_OPTIONAL)
        setZOrder("10")
        
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)
        
        val marker = getEngineMarkers().first()
        // 选择 OPTIONAL 后，文本为 OPT-1
        assertEquals("OPT-1", marker.text)
    }

    /**
     * 验证选择 REQUIRED_AND_HIDES_OPTIONAL 策略后添加 Marker，生成的文本标签是否匹配（RHO-1）。
     */
    @Test
    fun testAddMarker_requiredHidesOptionalBehavior_verifiesLabel() {
        selectStrategy(TAG_STRATEGY_REQUIRED_HIDES_OPTIONAL)
        setZOrder("20")
        
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)
        
        val marker = getEngineMarkers().first()
        // 选择 REQUIRED_AND_HIDES_OPTIONAL 后，文本为 RHO-1
        assertEquals("RHO-1", marker.text)
    }
    
    /**
     * 验证连续添加多个具备不同碰撞策略的 Marker 时，引擎中 Marker 的总数和各自的标签是否都能独立且正确地维护。
     */
    @Test
    fun testAddMultipleMarkers_verifiesCountAndLabels() {
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)
        
        selectStrategy(TAG_STRATEGY_OPTIONAL)
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(2)
        
        val markers = getEngineMarkers()
        assertTrue(markers.any { it.text == "REQ-1" })
        assertTrue(markers.any { it.text == "OPT-2" })
    }
    
    /**
     * 验证点击“清除”按钮后，地图引擎中的所有 Marker 能否被正确移除（数量归零）。
     */
    @Test
    fun testClearAll_removesMarkers() {
        clickPanelButton(TAG_BTN_ADD)
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(2)
        
        clickPanelButton(TAG_BTN_CLEAR)
        waitForMarkerCount(0)
    }

    /**
     * 验证清除全部 Marker 后，内部的添加计数器（addCount）是否会重置，使得下次添加时编号重新从 1 开始。
     */
    @Test
    fun testClearMarkers_resetsCounter() {
        clickPanelButton(TAG_BTN_ADD) // REQ-1
        clickPanelButton(TAG_BTN_ADD) // REQ-2
        waitForMarkerCount(2)

        clickPanelButton(TAG_BTN_CLEAR)
        waitForMarkerCount(0)

        // 重新添加，验证计数器是否已被重置，后缀应为 1
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)

        val marker = getEngineMarkers().first()
        assertEquals("计数器未重置", "REQ-1", marker.text)
    }

    /**
     * 验证添加的第一个 Marker 坐标严格等于 POI_HUB 的坐标位置。
     */
    @Test
    fun testFirstMarker_isPlacedAtPoiHub() {
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(1)

        val marker = getEngineMarkers().first()
        assertEquals(POI_HUB_LAT, marker.latLng.latitude, 0.0001)
        assertEquals(POI_HUB_LNG, marker.latLng.longitude, 0.0001)
    }

    /**
     * 验证添加的第二个及后续 Marker 会基于随机偏移生成，不会与 POI_HUB 中心点完全重合。
     */
    @Test
    fun testSubsequentMarkers_arePlacedAroundHub() {
        clickPanelButton(TAG_BTN_ADD)
        clickPanelButton(TAG_BTN_ADD)
        waitForMarkerCount(2)

        val markers = getEngineMarkers()
        val first = markers.first { it.text == "REQ-1" }
        val second = markers.first { it.text == "REQ-2" }

        // 第一个坐标应严格等于 Hub
        assertEquals(POI_HUB_LAT, first.latLng.latitude, 0.0001)
        assertEquals(POI_HUB_LNG, first.latLng.longitude, 0.0001)

        // 第二个坐标为随机生成，与 Hub 不应完全一致
        assertTrue(
            "后续添加的 Marker 不应该与 Hub 坐标完全一致",
            first.latLng.latitude != second.latLng.latitude ||
            first.latLng.longitude != second.latLng.longitude
        )
    }

    /**
     * 验证 Z-Order 输入框的输入限制逻辑，确保输入超过 3 位数字时会被 UI 层逻辑正确截断。
     */
    @Test
    fun testZOrderInput_limitsToThreeDigits() {
        // ViewModel UI 层限制了最多输入 3 位数字，输入五位时应截断
        setZOrder("12345")
        
        composeTestRule.onNodeWithTag(TAG_INPUT_Z_ORDER, useUnmergedTree = true)
            .assertTextContains("123")
    }
}
