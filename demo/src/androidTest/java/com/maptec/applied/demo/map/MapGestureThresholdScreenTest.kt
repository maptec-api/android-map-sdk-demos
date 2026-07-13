package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
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
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
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
 * 手势阻力与阈值配置 (Gesture Threshold & Resistance) 功能测试
 *
 * 测试目标：
 * - 验证平移 (Move)、旋转 (Rotate)、倾斜 (Shove) 的阈值与阻力 Slider 能否正确滑动
 * - 验证 Slider 滑动后，能否将数值双向绑定并同步到 GestureSettings API
 */
@RunWith(AndroidJUnit4::class)
class MapGestureThresholdScreenTest {

    companion object {
        private const val TAG_SLIDER_MOVE_THRESHOLD = "slider_move_threshold"
        private const val TAG_SLIDER_ROTATE_THRESHOLD = "slider_rotate_threshold"
        private const val TAG_SLIDER_ROTATE_RESISTANCE = "slider_rotate_resistance"
        private const val TAG_SLIDER_SHOVE_THRESHOLD = "slider_shove_threshold"
        private const val TAG_SLIDER_SHOVE_RESISTANCE = "slider_shove_resistance"
    }

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
        navigateMapGestureThresholdScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateMapGestureThresholdScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(
            R.string.map_item_map_gesture,
            R.string.map_item_gesture_threshold,
        )
        composeTestRule.waitForIdle()
    }

    // ==================== 异步 API 断言辅助 ====================

    private fun withGestureSettings(action: (com.maptec.applied.maps.GestureSettings) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        
        // 确保在主线程访问视图
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

    // ==================== 测试用例 ====================

    @Test
    fun testMoveGesture_ThresholdSliderUpdatesApi() {
        // 1. 确保节点可见
        composeTestRule.onNodeWithTag(TAG_SLIDER_MOVE_THRESHOLD).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SLIDER_MOVE_THRESHOLD).assertIsDisplayed()

        // 2. 记录初始状态
        var initialThreshold = -1f
        withGestureSettings { gs -> initialThreshold = gs.scrollGestureThreshold }

        // 3. 模拟用户拖动滑块至 90% 宽度处
        composeTestRule.onNodeWithTag(TAG_SLIDER_MOVE_THRESHOLD).performTouchInput {
            click(percentOffset(0.9f, 0.5f))
        }
        composeTestRule.waitForIdle()

        // 4. 断言底层 API 已更新
        withGestureSettings { gs ->
            val updatedThreshold = gs.scrollGestureThreshold
            assertNotEquals("平移触发阈值应当被修改", initialThreshold, updatedThreshold)
            assertTrue("在 90% 处点击，值应当大于初始默认值", updatedThreshold > initialThreshold)
        }
    }

    @Test
    fun testRotateGesture_SlidersUpdateApi() {
        // --- 旋转阈值测试 ---
        composeTestRule.onNodeWithTag(TAG_SLIDER_ROTATE_THRESHOLD).performScrollTo()
        
        var initialThreshold = -1f
        withGestureSettings { gs -> initialThreshold = gs.rotateGestureThreshold }

        // 点击 80% 处
        composeTestRule.onNodeWithTag(TAG_SLIDER_ROTATE_THRESHOLD).performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withGestureSettings { gs ->
            assertNotEquals("旋转触发阈值应当被修改", initialThreshold, gs.rotateGestureThreshold)
        }

        // --- 旋转阻力测试 ---
        composeTestRule.onNodeWithTag(TAG_SLIDER_ROTATE_RESISTANCE).performScrollTo()

        var initialResistance = -1f
        withGestureSettings { gs -> initialResistance = gs.rotateGestureResistance }

        // 点击 10% 处 (降低阻力)
        composeTestRule.onNodeWithTag(TAG_SLIDER_ROTATE_RESISTANCE).performTouchInput {
            click(percentOffset(0.1f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withGestureSettings { gs ->
            assertNotEquals("旋转阻力应当被修改", initialResistance, gs.rotateGestureResistance)
        }
    }

    @Test
    fun testShoveGesture_SlidersUpdateApi() {
        // --- 倾斜阈值测试 ---
        composeTestRule.onNodeWithTag(TAG_SLIDER_SHOVE_THRESHOLD).performScrollTo()
        
        var initialThreshold = -1f
        withGestureSettings { gs -> initialThreshold = gs.tiltGestureThreshold }

        composeTestRule.onNodeWithTag(TAG_SLIDER_SHOVE_THRESHOLD).performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withGestureSettings { gs ->
            assertNotEquals("倾斜触发阈值应当被修改", initialThreshold, gs.tiltGestureThreshold)
        }

        // --- 倾斜阻力测试 ---
        // 这是最底部的元素，验证 performScrollTo 的效果
        composeTestRule.onNodeWithTag(TAG_SLIDER_SHOVE_RESISTANCE).performScrollTo()

        var initialResistance = -1f
        withGestureSettings { gs -> initialResistance = gs.tiltGestureResistance }

        composeTestRule.onNodeWithTag(TAG_SLIDER_SHOVE_RESISTANCE).performTouchInput {
            click(percentOffset(0.9f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withGestureSettings { gs ->
            assertNotEquals("倾斜阻力应当被修改", initialResistance, gs.tiltGestureResistance)
        }
    }
}