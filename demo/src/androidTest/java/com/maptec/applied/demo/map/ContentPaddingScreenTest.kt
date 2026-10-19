package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.existsSafely
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.pollUntil
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.ui.screens.interaction.ContentPaddingDemo
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [ContentPaddingScreen] 内容内边距功能测试。
 *
 * 覆盖：默认零边距、滑块 + 应用后 API/读回/遮罩、清零恢复。
 *
 * 不使用 GrantPermissionRule：本 Demo 仅依赖普通权限，在部分设备上反复连接
 * UiAutomation 会触发 “already registered” 并拖垮整次 instrumentation。
 */
@RunWith(AndroidJUnit4::class)
class ContentPaddingScreenTest {

    companion object {
        private const val COORD_DELTA = 0.05
        private const val ZOOM_DELTA = 0.2
        private const val PADDING_DELTA = 40.0
        private const val EXPECTED_HALF_SLIDER_PX = 200.0
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToContentPaddingScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    private fun navigateToContentPaddingScreen() {
        composeTestRule.settleForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_content_padding)
        composeTestRule.settleForIdle()
    }

    private fun withMaptecMap(action: (MaptecMap) -> Unit) {
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

    private fun clickSlider(tag: String, percentX: Float) {
        composeTestRule.onNodeWithTag(tag)
            .performScrollTo()
            .assertIsDisplayed()
            .performTouchInput {
                click(percentOffset(percentX, 0.5f))
            }
        composeTestRule.settleForIdle()
    }

    private fun clickAction(tag: String) {
        composeTestRule.onNodeWithTag(tag)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.settleForIdle()
    }

    private fun waitForPaddingNear(
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        timeoutMs: Long = 8_000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            withMaptecMap { map ->
                val pad = map.contentPadding
                matched = kotlin.math.abs(pad[0] - left) <= PADDING_DELTA &&
                    kotlin.math.abs(pad[1] - top) <= PADDING_DELTA &&
                    kotlin.math.abs(pad[2] - right) <= PADDING_DELTA &&
                    kotlin.math.abs(pad[3] - bottom) <= PADDING_DELTA
            }
            if (matched) return
            Thread.sleep(150)
        }
        withMaptecMap { map ->
            val pad = map.contentPadding
            assertEquals("left", left, pad[0], PADDING_DELTA)
            assertEquals("top", top, pad[1], PADDING_DELTA)
            assertEquals("right", right, pad[2], PADDING_DELTA)
            assertEquals("bottom", bottom, pad[3], PADDING_DELTA)
        }
    }

    private fun assertOverlayExists(tag: String, exists: Boolean) {
        if (exists) {
            composeTestRule.pollUntil(timeoutMillis = 5_000L) {
                composeTestRule.onAllNodesWithTag(tag).existsSafely()
            }
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        } else {
            assertFalse(
                "不应存在遮罩 $tag",
                composeTestRule.onAllNodesWithTag(tag).existsSafely(),
            )
        }
    }

    @Test
    fun testDefault_zeroPaddingNoOverlays() {
        composeTestRule.onNodeWithTag(ContentPaddingDemo.TAG_READOUT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("getContentPadding", substring = true)

        withMaptecMap { map ->
            val pad = map.contentPadding
            assertEquals(0.0, pad[0], PADDING_DELTA)
            assertEquals(0.0, pad[1], PADDING_DELTA)
            assertEquals(0.0, pad[2], PADDING_DELTA)
            assertEquals(0.0, pad[3], PADDING_DELTA)

            val target = requireNotNull(map.cameraPosition.target)
            assertEquals(ContentPaddingDemo.TARGET.latitude, target.latitude, COORD_DELTA)
            assertEquals(ContentPaddingDemo.TARGET.longitude, target.longitude, COORD_DELTA)
            assertEquals(ContentPaddingDemo.TARGET_ZOOM, map.cameraPosition.zoom, ZOOM_DELTA)
        }

        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_LEFT, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_TOP, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_RIGHT, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_BOTTOM, exists = false)
    }

    @Test
    fun testApply_leftPaddingUpdatesApiAndOverlay() {
        clickSlider(ContentPaddingDemo.TAG_LEFT_SLIDER, 0.5f)
        clickAction(ContentPaddingDemo.TAG_APPLY_BUTTON)

        waitForPaddingNear(EXPECTED_HALF_SLIDER_PX, 0.0, 0.0, 0.0)

        composeTestRule.onNodeWithTag(ContentPaddingDemo.TAG_READOUT)
            .performScrollTo()
            .assertTextContains("getContentPadding", substring = true)

        composeTestRule.collapseConfigPanel()
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_LEFT, exists = true)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_TOP, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_RIGHT, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_BOTTOM, exists = false)

        withMaptecMap { map ->
            val target = requireNotNull(map.cameraPosition.target)
            assertEquals(ContentPaddingDemo.TARGET.latitude, target.latitude, COORD_DELTA)
            assertEquals(ContentPaddingDemo.TARGET.longitude, target.longitude, COORD_DELTA)
        }
    }

    @Test
    fun testApply_allSidesUpdatesApiAndOverlays() {
        clickSlider(ContentPaddingDemo.TAG_LEFT_SLIDER, 0.5f)
        clickSlider(ContentPaddingDemo.TAG_TOP_SLIDER, 0.5f)
        clickSlider(ContentPaddingDemo.TAG_RIGHT_SLIDER, 0.5f)
        clickSlider(ContentPaddingDemo.TAG_BOTTOM_SLIDER, 0.5f)
        clickAction(ContentPaddingDemo.TAG_APPLY_BUTTON)

        waitForPaddingNear(
            EXPECTED_HALF_SLIDER_PX,
            EXPECTED_HALF_SLIDER_PX,
            EXPECTED_HALF_SLIDER_PX,
            EXPECTED_HALF_SLIDER_PX,
        )

        composeTestRule.collapseConfigPanel()
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_LEFT, exists = true)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_TOP, exists = true)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_RIGHT, exists = true)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_BOTTOM, exists = true)
    }

    @Test
    fun testClear_resetsPaddingAndHidesOverlays() {
        clickSlider(ContentPaddingDemo.TAG_LEFT_SLIDER, 0.5f)
        clickSlider(ContentPaddingDemo.TAG_TOP_SLIDER, 0.5f)
        clickAction(ContentPaddingDemo.TAG_APPLY_BUTTON)
        waitForPaddingNear(EXPECTED_HALF_SLIDER_PX, EXPECTED_HALF_SLIDER_PX, 0.0, 0.0)

        clickAction(ContentPaddingDemo.TAG_CLEAR_BUTTON)
        waitForPaddingNear(0.0, 0.0, 0.0, 0.0)

        composeTestRule.collapseConfigPanel()
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_LEFT, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_TOP, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_RIGHT, exists = false)
        assertOverlayExists(ContentPaddingDemo.TAG_OVERLAY_BOTTOM, exists = false)
    }

    @Test
    fun testApi_setContentPaddingRoundTripsAndKeepsCameraTarget() {
        val left = 120.0
        val top = 80.0
        val right = 60.0
        val bottom = 40.0

        withMaptecMap { map ->
            map.setContentPadding(left, top, right, bottom)
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    ContentPaddingDemo.TARGET,
                    ContentPaddingDemo.TARGET_ZOOM,
                ),
            )
            val pad = map.contentPadding
            assertEquals(left, pad[0], PADDING_DELTA)
            assertEquals(top, pad[1], PADDING_DELTA)
            assertEquals(right, pad[2], PADDING_DELTA)
            assertEquals(bottom, pad[3], PADDING_DELTA)

            val target = requireNotNull(map.cameraPosition.target)
            assertEquals(ContentPaddingDemo.TARGET.latitude, target.latitude, COORD_DELTA)
            assertEquals(ContentPaddingDemo.TARGET.longitude, target.longitude, COORD_DELTA)
        }
    }
}
