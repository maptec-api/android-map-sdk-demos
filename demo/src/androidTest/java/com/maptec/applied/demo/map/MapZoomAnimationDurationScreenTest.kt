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
class MapZoomAnimationDurationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToZoomAnimationDurationScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_zoom_animation_duration)
        composeTestRule.waitForIdle()
    }

    /**
     * 测试缩放动画时长开关
     */
    @Test
    fun toggleZoomAnimationEnabledSwitch() {
        navigateToZoomAnimationDurationScreen()
        composeTestRule.waitForMapDemoReady()
        composeTestRule.verifyMapViewSwitchToggle("switch_zoom_animation_enabled") { view, callback ->
            view.getMapAsync { callback(it.uiSettings.gestures.isZoomAnimationDurationEnabled) }
        }
    }

    /**
     * 测试通过滑动条改变缩放动画时长
     */
    @Test
    fun changeZoomAnimationDurationViaSlider() {
        navigateToZoomAnimationDurationScreen()
        composeTestRule.waitForMapDemoReady()
        val mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag("text_zoom_animation_duration").assertIsDisplayed()

        composeTestRule.onNodeWithTag("slider_zoom_animation_duration").performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("text_zoom_animation_duration")
            .assertTextContains("动画时长: ", substring = true)

        mapView.getMapAsync { map ->
            val duration = map.uiSettings.gestures.zoomAnimationDuration
            LoggerFactory.getLogger(LOG_MODULE).withTag("MapZoomAnimationDurationTest").d { "Updated duration: $duration" }
            assert(duration > 1000)
        }
    }
}
