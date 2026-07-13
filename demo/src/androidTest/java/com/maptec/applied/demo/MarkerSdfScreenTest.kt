package com.maptec.applied.demo

import android.Manifest
import android.graphics.Color
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
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
 * [MarkerSdfScreen] 功能测试：SDF 彩色标记添加 API 验证。
 */
@RunWith(AndroidJUnit4::class)
class MarkerSdfScreenTest {

    companion object {
        private const val TAG_INPUT_LATLNG = "symbol_input_default_latlng"
        private const val TAG_INPUT_SDF_COLOR = "symbol_input_sdf_icon_color"
        private const val TAG_BTN_ADD_SDF = "symbol_btn_add_sdf"
        private const val TAG_BTN_CLEAR_ALL = "symbol_btn_clear_all"
        private const val TAG_HAS_MARKERS = "symbol_layer_has_markers"
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
        navigateToMarkerSdfScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
        waitForAtLeastOneMarker()
        clickPanelButton(TAG_BTN_CLEAR_ALL)
        waitForMarkerCount(0)
        composeTestRule.waitForIdle()
    }

    private fun waitForAtLeastOneMarker(timeoutMs: Long = 5000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().isNotEmpty()) return
            Thread.sleep(200)
        }
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToMarkerSdfScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_marker_sdf)
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
    fun testAddMarkerBySdf_showsMarkerIndicator() {
        setInput(TAG_INPUT_SDF_COLOR, "#FF0000")
        clickPanelButton(TAG_BTN_ADD_SDF)
        waitForMarkerCount(1)

        // 严谨等待：分步验证节点的存在与显示
        composeTestRule.waitUntil(5000L) {
            composeTestRule.onAllNodes(hasTestTag(TAG_HAS_MARKERS)).fetchSemanticsNodes().isNotEmpty()
        }
        
        // 节点存在后，再给 1 秒缓冲确保重绘完成并验证显示
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
    }

    @Test
    fun testInvalidLatLng_addSdfButtonDisabled() {
        setInput(TAG_INPUT_LATLNG, "invalid")
        composeTestRule.onNodeWithTag(TAG_BTN_ADD_SDF).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testClearAll_removesMarkers() {
        setInput(TAG_INPUT_SDF_COLOR, "#FF0000")
        clickPanelButton(TAG_BTN_ADD_SDF)
        waitForMarkerCount(1)

        clickPanelButton(TAG_BTN_CLEAR_ALL)
        waitForMarkerCount(0)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertDoesNotExist()
    }
}
