package com.maptec.applied.demo

import android.Manifest
import android.graphics.Color
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.style.layers.Property
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Marker(Overlay)（[MarkerLayerScreen]）功能测试。
 *
 * 对应页面：业务图层 → Marker(Overlay)
 */
@RunWith(AndroidJUnit4::class)
class MarkerLayerScreenTest {

    companion object {
        private const val TAG_INPUT_LATLNG = "symbol_input_default_latlng"
        private const val TAG_INPUT_ICON_SIZE = "symbol_input_icon_size"
        private const val TAG_INPUT_SDF_COLOR = "symbol_input_sdf_icon_color"
        private const val TAG_BTN_ADD_BY_TYPE = "symbol_btn_add_by_type"
        private const val TAG_BTN_ADD_BY_URL = "symbol_btn_add_by_url"
        private const val TAG_BTN_ADD_SDF = "symbol_btn_add_sdf"
        private const val TAG_BTN_CLEAR_ALL = "symbol_btn_clear_all"
        private const val TAG_SWITCH_SCALE_WITH_ZOOM = "symbol_switch_icon_scale_with_zoom"
        private const val TAG_INPUT_MIN_ZOOM = "symbol_input_min_zoom"
        private const val TAG_INPUT_MAX_ZOOM = "symbol_input_max_zoom"
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
        navigateToMarkerLayerScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航与辅助 ====================

    private fun navigateToMarkerLayerScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_overlay)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.overlay_item_marker))
            .performScrollTo()
            .performClick()
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

    private fun toggleSwitch(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
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
    fun testAddMarkerBySdf_iconColorVerified() {
        setInput(TAG_INPUT_SDF_COLOR, "#FF0000")
        clickPanelButton(TAG_BTN_ADD_SDF)
        waitForMarkerCount(1)

        val marker = getEngineMarkers().single()
        assertNotNull(marker.iconColor)
        assertEquals(Color.RED, marker.iconColor!!)
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
    fun testInvalidLatLng_addButtonsDisabled() {
        setInput(TAG_INPUT_LATLNG, "invalid")

        composeTestRule.onNodeWithTag(TAG_BTN_ADD_BY_TYPE).performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_BTN_ADD_BY_URL).performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_BTN_ADD_SDF).performScrollTo().assertIsNotEnabled()
    }

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
