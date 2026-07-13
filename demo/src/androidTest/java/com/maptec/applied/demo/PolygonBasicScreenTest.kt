package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.fill.Fill
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [PolygonBasicScreen] 功能测试：基础多边形绘制与导航。
 */
@RunWith(AndroidJUnit4::class)
class PolygonBasicScreenTest {

    companion object {
        private const val TAG_COORDINATES = "fill_input_coordinates"
        private const val TAG_COLOR = "fill_input_color"
        private const val TAG_OUTLINE_COLOR = "fill_input_outline_color"
        private const val TAG_DRAW_BUTTON = "fill_draw_button"
    }

    private val permissionRule = GrantPermissionRule.grant(
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
    val ruleChain: TestRule = RuleChain
        .outerRule(permissionRule)
        .around(composeTestRule)

    private lateinit var mapView: MapView

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
        Thread.sleep(500)
    }

    private fun navigateToFillScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_polygon_menu)
        composeTestRule.waitForIdle()
    }

    private fun replaceField(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.mainClock.advanceTimeBy(300)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    private fun withMap(action: (MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
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
        assertTrue("获取 Map 异步超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    private fun getCameraTarget(): Pair<Double, Double>? {
        val latch = CountDownLatch(1)
        var result: Pair<Double, Double>? = null
        var error: Throwable? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    val pos = map.cameraPosition
                    result = if (pos?.target != null) {
                        Pair(pos.target!!.latitude, pos.target!!.longitude)
                    } else null
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("获取相机位置超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
        return result
    }

    private fun drawAndVerifyCameraMoved(): Pair<Double, Double> {
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).assertIsEnabled()
        val initialTarget = getCameraTarget()

        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performClick()
        composeTestRule.waitUntil(10_000) {
            val current = getCameraTarget()
            current != null && initialTarget != null &&
                (current.first != initialTarget.first || current.second != initialTarget.second)
        }

        val newTarget = getCameraTarget()
        assertNotNull("初始相机位置不应为空", initialTarget)
        assertNotNull("绘制后相机位置不应为空", newTarget)
        return newTarget!!
    }

    private fun queryFillAt(latLng: LatLng): Fill? {
        var fill: Fill? = null
        withMap { map ->
            val engine = map.getOverlayEngine()
            val screen = map.projection.toScreenLocation(latLng)
            for (hit in engine.queryOverlayHitsFromPoint(screen)) {
                val target = engine.resolveHitTarget(hit)
                if (target is Fill) {
                    fill = target
                    return@withMap
                }
            }
        }
        return fill
    }

    private fun waitForFillAt(latLng: LatLng, timeoutMs: Long = 5000): Fill {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            queryFillAt(latLng)?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Fill not found at $latLng within ${timeoutMs}ms")
    }

    @Test
    fun drawWithDefaultValues_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("默认参数绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithCustomColors_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        replaceField(TAG_COLOR, "#FF5733")
        replaceField(TAG_OUTLINE_COLOR, "#000000")
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().assertIsEnabled()
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("自定义颜色绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithSimpleCoords_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        replaceField(
            TAG_COORDINATES,
            "[[39.9,116.3;39.95,116.4;39.9,116.5;39.9,116.3]]"
        )
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("简单坐标绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithInvalidInput_drawButtonDisabled() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()

        replaceField(TAG_COLOR, "not-a-color")
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun back_returnsToOverlayList() {
        navigateToFillScreen()
        composeTestRule.waitForIdle()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(getTestString(R.string.overlay_item_polygon_menu))
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun doubleBack_returnsToMainScreen() {
        navigateToFillScreen()
        composeTestRule.waitForIdle()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.catalog_main_annotations)).assertIsDisplayed()
    }
}
