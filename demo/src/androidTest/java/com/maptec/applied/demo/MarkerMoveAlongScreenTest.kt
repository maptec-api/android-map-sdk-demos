package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.ext.waitForTag
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.marker.Marker
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 覆盖物 → 标记 → 沿线移动。
 * 走兜底路径，不依赖网络。
 */
@RunWith(AndroidJUnit4::class)
class MarkerMoveAlongScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        composeTestRule.settleForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_marker_move_along)
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        runCatching {
            composeTestRule.onNodeWithTag("move_along_btn_cancel_keep")
                .performScrollTo()
                .performClick()
        }
        composeTestRule.settleForIdle()
    }

    @Test
    fun testCatalog_opensMoveAlongScreen() {
        composeTestRule.expandConfigPanel()
        composeTestRule.onNodeWithTag("move_along_btn_start").performScrollTo().assertIsDisplayed()
        composeTestRule.waitForTag("move_along_has_marker", timeoutMillis = 10_000L)
        composeTestRule.onNodeWithTag("move_along_has_marker").assertIsDisplayed()
    }

    @Test
    fun testStart_keepsOverlayMarker() {
        waitForOverlayMarker()
        composeTestRule.expandConfigPanel()
        composeTestRule.onNodeWithTag("move_along_btn_start").performScrollTo().performClick()
        composeTestRule.settleForIdle()
        Thread.sleep(800)
        val markers = getEngineMarkers()
        assertTrue("start 后仍应是 Overlay Marker", markers.isNotEmpty())
        assertTrue(markers.all { it.id.isNotEmpty() })
    }

    private fun waitForOverlayMarker(timeoutMs: Long = 8_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().isNotEmpty()) return
            Thread.sleep(200)
        }
        throw AssertionError("兜底路径 Marker 未在 ${timeoutMs}ms 内出现")
    }

    private fun getEngineMarkers(): List<Marker> {
        var markers: Collection<Marker> = emptyList()
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    markers = map.overlayEngine.markers
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("异步获取 MaptecMap 超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
        return markers.toList()
    }
}
