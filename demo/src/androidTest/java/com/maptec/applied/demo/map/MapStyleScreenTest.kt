package com.maptec.applied.demo.map

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.R
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered。
 */
@RunWith(AndroidJUnit4::class)
class MapStyleScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
    }

    private companion object {
        private const val STYLE_WITH_DATA = "dark"
        private const val API_TIMEOUT_MS = 15_000L
    }

    private fun navigateToMapStyleScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openMapsDemo(R.string.map_item_map_style)
        composeTestRule.waitForMapDemoReady()
    }

    private fun getStyleAsync(mapView: MapView, timeoutMs: Long = API_TIMEOUT_MS): Style? {
        val latch = CountDownLatch(1)
        var result: Style? = null

        composeTestRule.runOnUiThread {
            mapView.getMapAsync { map ->
                map.getStyle { style ->
                    result = style
                    latch.countDown()
                }
            }
        }

        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return result
    }

    @Test
    fun testSwitchStyleTriggersRendered() {
        navigateToMapStyleScreen()
        val mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag("style_${STYLE_WITH_DATA}_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(API_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithTag("mapRendered").fetchSemanticsNodes().isNotEmpty()
        }

        val style = getStyleAsync(mapView)
        assertTrue("Style should not be null", style != null)
        assertTrue("Style [$STYLE_WITH_DATA] should be fully loaded", style!!.isFullyLoaded)
    }

    @Test
    fun testSameIdResetAfterSuccessStaysFullyLoaded() {
        navigateToMapStyleScreen()
        val mapView = composeTestRule.getMapView()

        composeTestRule.waitUntil(API_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithTag("mapRendered").fetchSemanticsNodes().isNotEmpty()
        }

        // mapRendered 只代表页面收到 onStyleRendered，SDK 会在回调返回后再 markFullyLoaded。
        val initialStyle = getStyleAsync(mapView)
        assertTrue("Style should be fully loaded before same-id reset", initialStyle != null)
        assertTrue("Style should be fully loaded before same-id reset", initialStyle!!.isFullyLoaded)

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        composeTestRule.runOnUiThread {
            mapView.getMapAsync { map ->
                val currentId = map.style?.id ?: STYLE_WITH_DATA
                map.setStyle(StyleOption(currentId), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) = Unit

                    override fun onStyleRendered(style: Style?) {
                        callbackInvoked = true
                        latch.countDown()
                    }

                    override fun onFailed(style: Style?, message: String) {
                        latch.countDown()
                    }
                })
            }
        }

        assertTrue(
            "Same-id setStyle should invoke onStyleRendered",
            latch.await(API_TIMEOUT_MS, TimeUnit.MILLISECONDS),
        )
        assertTrue("Same-id setStyle should callback success, not fail", callbackInvoked)

        val style = getStyleAsync(mapView)
        assertTrue("Style should not be null after same-id reset", style != null)
        assertTrue("Style should stay fully loaded after same-id reset", style!!.isFullyLoaded)
    }
}
