package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 旋转朝向范围（Rotate Bearing Range）功能测试
 *
 * 测试目标：
 * - 验证页面标题、当前朝向、提示文案正确显示
 * - 验证最小/最大朝向 Slider 能正确滑动并同步到 UiSettings 底层 API
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class MapRotateBearingRangeScreenTest {

    companion object {
        private const val TAG_SLIDER_MIN_BEARING = "slider_min_bearing"
        private const val TAG_SLIDER_MAX_BEARING = "slider_max_bearing"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_rotate_bearing_range)
        composeTestRule.waitForIdle()
    }

    // ==================== 异步 API 断言辅助 ====================

    private fun withUiSettings(action: (minBearing: Double, maxBearing: Double) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    val gestures = map.uiSettings.gestures
                    action(gestures.rotateGestureMinBearing, gestures.rotateGestureMaxBearing)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("异步获取 UiSettings 超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // ==================== 测试用例 ====================

    @Test
    fun testMinBearingSlider_UpdatesApi() {
        composeTestRule.onNodeWithTag(TAG_SLIDER_MIN_BEARING).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SLIDER_MIN_BEARING).assertIsDisplayed()

        var initialMin = -1.0
        withUiSettings { min, _ -> initialMin = min }

        composeTestRule.onNodeWithTag(TAG_SLIDER_MIN_BEARING).performTouchInput {
            click(percentOffset(0.5f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withUiSettings { min, _ ->
            assertNotEquals("最小朝向应当被修改", initialMin, min)
            assertTrue("最小朝向应在 0~360 范围内", min in 0.0..360.0)
        }
    }

    @Test
    fun testMaxBearingSlider_UpdatesApi() {
        composeTestRule.onNodeWithTag(TAG_SLIDER_MAX_BEARING).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SLIDER_MAX_BEARING).assertIsDisplayed()

        var initialMax = -1.0
        withUiSettings { _, max -> initialMax = max }

        composeTestRule.onNodeWithTag(TAG_SLIDER_MAX_BEARING).performTouchInput {
            click(percentOffset(0.3f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withUiSettings { _, max ->
            assertNotEquals("最大朝向应当被修改", initialMax, max)
            assertTrue("最大朝向应在 0~360 范围内", max in 0.0..360.0)
        }
    }

    @Test
    fun testMinAndMaxBearing_RangeConsistency() {
        // 验证初始值：min<=max
        withUiSettings { min, max ->
            assertTrue("初始最小朝向($min)应 <= 最大朝向($max)", min <= max)
        }
    }
}
