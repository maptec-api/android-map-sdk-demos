package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
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
import androidx.test.rule.GrantPermissionRule
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
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@RunWith(AndroidJUnit4::class)
class MapZoomAnimationDurationScreenTest {

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

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

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
