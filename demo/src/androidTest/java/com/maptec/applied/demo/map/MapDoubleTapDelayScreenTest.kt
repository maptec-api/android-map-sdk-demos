package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.percentOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.LOG_MODULE
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
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.javabase.log.LoggerFactory
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered。
 */
@RunWith(AndroidJUnit4::class)
class MapDoubleTapDelayScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToDoubleTapDelayScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_double_tap_delay)
        composeTestRule.waitForIdle()
    }

    /**
     * 测试地图正常渲染
     */
    /**
     * 测试自定义双击检测开关
     */
    @Test
    fun toggleCustomDoubleTapSwitch() {
        navigateToDoubleTapDelayScreen()
        composeTestRule.waitForMapDemoReady()
        composeTestRule.verifyMapViewSwitchToggle("switch_custom_double_tap") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isCustomDoubleTapDetectionEnabled) }
        }
    }

    /**
     * 测试通过滑动条改变双击延时
     */
    @Test
    fun changeDoubleTapDelayViaSlider() {
        navigateToDoubleTapDelayScreen()
        composeTestRule.waitForMapDemoReady()
        val mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag("text_double_tap_delay").assertIsDisplayed()

        composeTestRule.onNodeWithTag("slider_double_tap_delay").performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("text_double_tap_delay").assertTextContains("双击延时: ", substring = true)

        mapView.getMapAsync { map ->
            val doubleTapDelay = map.uiSettings.gestures.doubleTapDelay
            LoggerFactory.getLogger(LOG_MODULE).withTag("MapDoubleTapDelayTest").d { "Updated doubleTapDelay: $doubleTapDelay" }
            assert(doubleTapDelay > 300)
        }
    }
}
