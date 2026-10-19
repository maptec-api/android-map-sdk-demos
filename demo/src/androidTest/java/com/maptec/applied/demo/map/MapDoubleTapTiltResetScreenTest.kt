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
class MapDoubleTapTiltResetScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToDoubleTapTiltResetScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_double_tap_tilt_reset)
        composeTestRule.waitForIdle()
    }

    /**
     * 测试双击倾斜复位开关
     */
    @Test
    fun toggleDoubleTapTiltResetSwitch() {
        navigateToDoubleTapTiltResetScreen()
        composeTestRule.verifyMapViewSwitchToggle("switch_double_tap_tilt_reset") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isDoubleTapTiltResetEnabled) }
        }
    }

}
