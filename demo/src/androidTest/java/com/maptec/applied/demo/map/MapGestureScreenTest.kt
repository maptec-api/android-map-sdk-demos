package com.maptec.applied.demo.map

import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.maptec.applied.demo.ext.verifyMapViewSwitchToggle
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
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
class MapGestureScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        // 移除 Thread.sleep(2000)，使用智能等待确保所有资源释放和 UI 稳定
        composeTestRule.waitForIdle()
    }

    private fun navigateToMapGestureScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_map_gesture_control)
        composeTestRule.waitForIdle()
    }

    @Test
    fun toggleAllGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_all_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.areAllGesturesEnabled()) }
        }
    }

    @Test
    fun toggleScrollGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_scroll_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isScrollGesturesEnabled) }
        }
    }

    @Test
    fun toggleRotateGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_rotate_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isRotateGesturesEnabled) }
        }
    }

    @Test
    fun toggleTiltGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_tilt_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isTiltGesturesEnabled) }
        }
    }

    @Test
    fun toggleDoubleFingerZoomGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_double_finger_zoom_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isZoomGesturesEnabled) }
        }
    }

    @Test
    fun toggleDoubleClickZoomGestureSwitch() {
        navigateToMapGestureScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_double_click_zoom_gesture") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isDoubleTapGesturesEnabled) }
        }
    }
}