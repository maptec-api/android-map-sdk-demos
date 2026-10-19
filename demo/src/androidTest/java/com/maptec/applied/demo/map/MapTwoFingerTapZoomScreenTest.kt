package com.maptec.applied.demo.map

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.verifyMapViewSwitchToggle
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered。
 */
@RunWith(AndroidJUnit4::class)
class MapTwoFingerTapZoomScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToTwoFingerTapZoomScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_two_finger_tap_zoom)
        composeTestRule.waitForIdle()
    }

    /**
     * 测试双指点击缩放开关
     */
    @Test
    fun toggleTwoFingerTapZoomSwitch() {
        navigateToTwoFingerTapZoomScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_two_finger_tap_zoom") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isTwoFingerTapZoomEnabled) }
        }
    }
}
