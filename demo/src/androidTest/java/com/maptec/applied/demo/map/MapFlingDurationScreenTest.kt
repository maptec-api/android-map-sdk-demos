package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 惯性滑动时长控制（Fling Duration）功能测试
 *
 * 测试目标：
 * - Switch 控制 isInertiaScrollEnabled 的开关
 * - Slider 控制 inertiaScrollDuration 的时长数值
 * - 验证与 GesturesManager 底层 API 的双向绑定是否正确
 */
@RunWith(AndroidJUnit4::class)
class MapFlingDurationScreenTest {

    companion object {
        private const val TAG_SWITCH_INERTIA = "switch_inertia_scroll_enabled"
        private const val TAG_SLIDER_DURATION = "slider_fling_duration"
        private const val TAG_TEXT_DURATION = "text_fling_duration"
    }

    // 修复点 2：移除了 INTERNET 和 ACCESS_NETWORK_STATE，只保留需要动态申请的危险权限
    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
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

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateMapFlingDurationScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateMapFlingDurationScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_gesture)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_fling_duration)).performClick()
        composeTestRule.waitForIdle()
    }


    // ==================== 异步 API 断言辅助 ====================

    private fun withGestureSettings(action: (com.maptec.applied.maps.GestureSettings) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        // 修复点 1：将 getMapAsync 包裹在主线程中执行，防止视图层跨线程访问崩溃
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    val gs = map.uiSettings.gestures
                    action(gs)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("异步获取 GestureSettings 超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    private fun getActualMapZoom(): Double {
        var actualZoom = -1.0
        val latch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                actualZoom = map.cameraPosition.zoom
                latch.countDown()
            }
        }
        latch.await(2, TimeUnit.SECONDS)
        return actualZoom
    }

    // ==================== 测试用例 ====================

    @Test
    fun testFlingDuration_DefaultState() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_INERTIA).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SLIDER_DURATION).assertIsDisplayed()

        withGestureSettings { gs ->
            assertTrue("默认情况下，SDK 应该允许惯性滑动", gs.isInertiaScrollEnabled)
        }
    }

    @Test
    fun testFlingDuration_ToggleInertiaSwitch() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_INERTIA).performScrollTo()

        var initialState = false
        withGestureSettings { gs -> initialState = gs.isInertiaScrollEnabled }

        composeTestRule.onNodeWithTag(TAG_SWITCH_INERTIA).performClick()
        composeTestRule.waitForIdle()

        if (initialState) {
            composeTestRule.onNodeWithTag(TAG_SWITCH_INERTIA).assertIsOff()
        } else {
            composeTestRule.onNodeWithTag(TAG_SWITCH_INERTIA).assertIsOn()
        }

        withGestureSettings { gs ->
            assertEquals("切换 Switch 后，API 的状态应该相反", !initialState, gs.isInertiaScrollEnabled)
        }
    }

    @Test
    fun testFlingDuration_SliderChangesValue() {
        composeTestRule.onNodeWithTag(TAG_SLIDER_DURATION).performScrollTo()

        var initialDuration = -1L
        withGestureSettings { gs -> initialDuration = gs.inertiaScrollDuration }

        composeTestRule.onNodeWithTag(TAG_SLIDER_DURATION).performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withGestureSettings { gs ->
            val updatedDuration = gs.inertiaScrollDuration
            assertNotEquals("拖动 Slider 后，底层 inertiaScrollDuration 应该改变", initialDuration, updatedDuration)
            assertTrue("在 80% 处点击，值应当大于0（预期约为 4000 左右）", updatedDuration > 0L)
        }
    }
}