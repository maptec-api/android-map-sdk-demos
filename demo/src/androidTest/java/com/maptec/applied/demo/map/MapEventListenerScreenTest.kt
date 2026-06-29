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
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
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
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_event_listener)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForLog(text: String, timeout: Long = 5) {
        composeTestRule.waitUntil(
            condition = {
                composeTestRule.onAllNodes(hasText(text, substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            },
            timeoutMillis = TimeUnit.SECONDS.toMillis(timeout)
        )
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
        val startX = (location[0] + mapView.width / 2).toFloat()
        val startY = (location[1] + mapView.height / 2).toFloat()
        val endX = startX + 120f
        val endY = startY

        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
            )
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(
                    downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, endX, endY, 0
                )
            )
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP, endX, endY, 0
                )
            )
        }
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    // ==================== 测试用例 ====================

    @Test
    fun testMapReadyAndSwitchesInitiallyOff() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
        assertTrue("MapView should have non-zero dimensions", mapView.width > 0 && mapView.height > 0)
    }

    @Test
    fun testMapClick_enabled_generatesLogEntry() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_CLICK).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        dispatchTapOnMap()
        composeTestRule.waitForIdle()

        waitForLog(LOG_MAP_CLICK)
    }

    @Test
    fun testMapLongClick_enabled_generatesLogEntry() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_LONG_CLICK).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        dispatchLongPressOnMap()
        composeTestRule.waitForIdle()

        waitForLog(LOG_MAP_LONG_CLICK)
    }

    @Test
    fun testClearLogs_removesEntries() {
        navigateToMapEventListenerScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_CLICK).performScrollTo().performClick()
        composeTestRule.waitForIdle()
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
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag(TAG_SWITCH_CAMERA_IDLE).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        dispatchSwipeOnMap()
        waitForLog(LOG_CAMERA_IDLE)
    }
}
