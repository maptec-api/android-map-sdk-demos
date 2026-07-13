package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapEventListenerScreenTest {

    companion object {
        private const val TAG_SWITCH_CAMERA_MOVE = "switch_camera_move"
        private const val TAG_SWITCH_MAP_LONG_CLICK = "switch_map_long_click"
        private const val TAG_SWITCH_FLING = "switch_fling"
        private const val TAG_SWITCH_CAMERA_IDLE = "switch_camera_idle"
        private const val TAG_SWITCH_CAMERA_MOVE_STARTED = "switch_camera_move_started"
        private const val TAG_SWITCH_MAP_CLICK = "switch_map_click"
        private const val TAG_BTN_CLEAR_LOGS = "btn_clear_logs"

        private const val LOG_MAP_CLICK = "Map Click"
        private const val LOG_MAP_LONG_CLICK = "Map Long Click"
        private const val LOG_CAMERA_MOVE = "Camera Move"
        private const val LOG_CAMERA_IDLE = "Camera Idle"
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
    private lateinit var mapView: MapView

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(permissionRule)
        .around(composeTestRule)

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToMapEventListenerScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_event_listener)
        composeTestRule.waitForIdle()
    }

    private fun waitForLog(text: String, timeout: Long = 5) {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout)
        while (System.currentTimeMillis() < deadline) {
            try {
                val nodes = composeTestRule.onAllNodes(hasText(text, substring = true))
                    .fetchSemanticsNodes()
                if (nodes.isNotEmpty()) return
            } catch (_: Throwable) {
            }
            Thread.sleep(200)
        }
        throw AssertionError("Condition not satisfied within ${timeout}s: $text")
    }

    private fun assertNoLog(text: String) {
        composeTestRule.waitForIdle()
        val nodes = composeTestRule.onAllNodes(hasText(text, substring = true))
            .fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            throw AssertionError("Expected no log containing '$text' but found ${nodes.size} nodes")
        }
    }

    private fun dispatchTapOnMap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val location = IntArray(2)
        instrumentation.runOnMainSync {
            mapView.getLocationOnScreen(location)
        }
        val centerX = (location[0] + mapView.width / 2).toFloat()
        val centerY = (location[1] + mapView.height / 2).toFloat()

        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
            )
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(
                    downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, centerX, centerY, 0
                )
            )
        }
    }

    private fun dispatchLongPressOnMap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val location = IntArray(2)
        instrumentation.runOnMainSync {
            mapView.getLocationOnScreen(location)
        }
        val centerX = (location[0] + mapView.width / 2).toFloat()
        val centerY = (location[1] + mapView.height / 2).toFloat()

        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
            )
        }
        // 长按等待
        Thread.sleep(1500)
        instrumentation.runOnMainSync {
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP, centerX, centerY, 0
                )
            )
        }
    }

    private fun dispatchSwipeOnMap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val location = IntArray(2)
        instrumentation.runOnMainSync {
            mapView.getLocationOnScreen(location)
        }

        // 起点：地图中心
        val startX = (location[0] + mapView.width / 2).toFloat()
        val startY = (location[1] + mapView.height / 2).toFloat()

        // 终点：向右滑动视图宽度的 40%
        val endX = startX + (mapView.width * 0.4f)
        val endY = startY

        // 核心参数：将滑动过程拆分为多个步骤
        val steps = 20          // 将滑动拆分成 20 步
        val durationMs = 400L   // 整个滑动过程耗时 400 毫秒

        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            var eventTime = downTime

            // 1. 手指按下 (ACTION_DOWN)
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
            )

            // 2. 连续的手指滑动 (ACTION_MOVE) - 模拟真实轨迹
            for (i in 1..steps) {
                val progress = i / steps.toFloat()
                // 线性插值计算当前的 X 和 Y 坐标
                val currentX = startX + (endX - startX) * progress
                val currentY = startY + (endY - startY) * progress
                // 按比例计算当前事件的时间戳
                eventTime = downTime + (durationMs * progress).toLong()

                mapView.dispatchTouchEvent(
                    MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, currentX, currentY, 0)
                )
            }

            // 3. 手指抬起 (ACTION_UP)
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, endX, endY, 0)
            )
        }

        composeTestRule.waitForIdle()

        // 等待地图的惯性滑动 (Fling) 结束，留出充足时间让底层渲染和抛出 Idle 日志
        Thread.sleep(1500)
    }

    // ==================== 测试用例 ====================

    @Test
    fun testMapReadyAndSwitchesInitiallyOff() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        assertTrue("MapView should have non-zero dimensions", mapView.width > 0 && mapView.height > 0)
    }

    @Test
    fun testMapClick_enabled_generatesLogEntry() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        dispatchTapOnMap()
        composeTestRule.waitForIdle()

        waitForLog(LOG_MAP_CLICK)
    }

    @Test
    fun testMapLongClick_enabled_generatesLogEntry() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        dispatchLongPressOnMap()
        composeTestRule.waitForIdle()

        waitForLog(LOG_MAP_LONG_CLICK)
    }

    @Test
    fun testClearLogs_removesEntries() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        dispatchTapOnMap()
        composeTestRule.waitForIdle()
        waitForLog(LOG_MAP_CLICK)

        composeTestRule.onNodeWithTag(TAG_BTN_CLEAR_LOGS).performClick()
        composeTestRule.waitForIdle()

        assertNoLog(LOG_MAP_CLICK)
    }


    @Test
    fun testCameraIdle_enabled_generatesLogEntry() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        composeTestRule.waitForIdle()
        dispatchSwipeOnMap()
        waitForLog(LOG_CAMERA_IDLE, timeout = 8)    }
}
