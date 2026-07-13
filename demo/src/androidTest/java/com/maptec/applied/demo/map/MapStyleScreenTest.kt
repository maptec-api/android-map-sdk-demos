package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.R
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapStyleScreenTest {

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        *if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            emptyArray()
        }
    )

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

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
}
