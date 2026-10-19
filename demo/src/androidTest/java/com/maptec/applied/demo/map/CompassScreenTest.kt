package com.maptec.applied.demo.map

import android.view.Gravity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CompassScreen 集成测试：覆盖指南针开关、正北自动隐藏、位置下拉、顺时针旋转与返回导航。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class CompassScreenTest {

    companion object {
        private const val TAG_COMPASS_SWITCH = "compass_switch"
        private const val TAG_FADE_SWITCH = "compass_fade_switch"
        private const val TAG_ROTATE_BUTTON = "compass_rotate_button"
        private const val TAG_GRAVITY_DROPDOWN = "compass_gravity_dropdown"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToCompassScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToCompassScreen() {
        composeTestRule.settleForIdle()
        composeTestRule.openUiControlsDemo(R.string.map_item_compass)
        composeTestRule.settleForIdle()
    }

    // ==================== 异步 API 断言辅助 ====================

    private fun withMaptecMap(action: (com.maptec.applied.maps.MaptecMap) -> Unit) {
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

    // ==================== 测试用例 ====================

    @Test
    fun testCompassSwitch_TogglesApi() {
        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).assertIsDisplayed()

        // 初始状态应为开启
        withMaptecMap { assertTrue("指南针默认应开启", it.uiSettings.isCompassEnabled) }
        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).assertIsOn()

        // 关闭
        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).performClick()
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).assertIsOff()
        withMaptecMap { assertEquals(false, it.uiSettings.isCompassEnabled) }

        // 重新开启
        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).performClick()
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithTag(TAG_COMPASS_SWITCH).assertIsOn()
        withMaptecMap { assertEquals(true, it.uiSettings.isCompassEnabled) }
    }

    @Test
    fun testFadeWhenFacingNorth_TogglesApi() {
        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).assertIsDisplayed()

        // 初始状态应为关闭 (CompassScreen 中 onMapReady 设为 false)
        withMaptecMap {
            assertEquals("正北自动隐藏默认应关闭", false, it.uiSettings.isCompassFadeWhenFacingNorth)
        }
        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).assertIsOff()

        // 开启
        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).performClick()
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).assertIsOn()
        withMaptecMap {
            assertEquals("正北自动隐藏应开启", true, it.uiSettings.isCompassFadeWhenFacingNorth)
        }

        // 再关闭
        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).performClick()
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithTag(TAG_FADE_SWITCH).assertIsOff()
        withMaptecMap {
            assertEquals("正北自动隐藏应关闭", false, it.uiSettings.isCompassFadeWhenFacingNorth)
        }
    }

    @Test
    fun testGravityDropdown_SelectsAndUpdatesApi() {
        composeTestRule.onNodeWithTag(TAG_GRAVITY_DROPDOWN).performScrollTo()

        // 打开下拉
        composeTestRule.onNodeWithTag(TAG_GRAVITY_DROPDOWN).performClick()
        composeTestRule.settleForIdle()

        // 选择左下 (BOTTOM|START)
        val bottomStartText = getTestString(R.string.compass_gravity_bottom_start)
        composeTestRule.onNodeWithText(bottomStartText).assertIsDisplayed()
        composeTestRule.onNodeWithText(bottomStartText).performClick()
        composeTestRule.settleForIdle()

        withMaptecMap { map ->
            val gravity = map.uiSettings.getCompassGravity()
            assertEquals(
                "指南针位置应更新为 BOTTOM|START",
                Gravity.BOTTOM or Gravity.START,
                gravity
            )
        }

        // 重新打开并选择右下 (BOTTOM|END)
        composeTestRule.onNodeWithTag(TAG_GRAVITY_DROPDOWN).performClick()
        composeTestRule.settleForIdle()

        val bottomEndText = getTestString(R.string.compass_gravity_bottom_end)
        composeTestRule.onNodeWithText(bottomEndText).performClick()
        composeTestRule.settleForIdle()

        withMaptecMap { map ->
            assertEquals(
                "指南针位置应更新为 BOTTOM|END",
                Gravity.BOTTOM or Gravity.END,
                map.uiSettings.getCompassGravity()
            )
        }
    }

    @Test
    fun testRotateButton_ChangesBearing() {
        composeTestRule.onNodeWithTag(TAG_ROTATE_BUTTON).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_ROTATE_BUTTON).assertIsDisplayed()

        // 记录初始朝向
        var initialBearing = 0.0
        withMaptecMap { initialBearing = it.cameraPosition.bearing }

        // 点击顺时针旋转
        composeTestRule.onNodeWithTag(TAG_ROTATE_BUTTON).performClick()
        composeTestRule.settleForIdle()
        Thread.sleep(600) // 等待动画完成

        // 验证朝向已改变
        var updatedBearing = 0.0
        withMaptecMap { updatedBearing = it.cameraPosition.bearing }

        assertNotEquals("顺时针旋转后朝向应改变", initialBearing, updatedBearing)
        // 验证变化约为 36°（变化量在 20° 内即可，考虑动画未完全结束）
        val expectedBearing = (initialBearing - 36 + 360) % 360
        val diff = Math.abs(updatedBearing - expectedBearing)
        assertTrue("朝向变化应接近 36°，实际变化: ${diff}°", diff < 15.0 || diff > 345.0)
    }

    @Test
    fun testBack_ReturnsToMapItemList() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.map_item_compass)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_zoom)).assertIsDisplayed()
    }

    @Test
    fun testDoubleBack_ReturnsToMainScreen() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.settleForIdle()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.settleForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.catalog_main_interaction)).assertIsDisplayed()
    }
}
