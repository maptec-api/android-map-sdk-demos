package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.constants.Constants
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.javabase.log.LoggerFactory
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapZoomCenterModeScreenTest {

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
    val ruleChain: TestRule = RuleChain
        .outerRule(permissionRule)
        .around(composeTestRule)

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToZoomCenterModeScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_zoom_center_mode)
        composeTestRule.waitForMapDemoReady()
        waitForMapZoomCenterModeSynced()
    }

    private fun waitForMapZoomCenterModeSynced() {
        val mapView = composeTestRule.getMapView()
        val latch = CountDownLatch(1)
        mapView.getMapAsync { latch.countDown() }
        check(latch.await(10, TimeUnit.SECONDS)) { "Map async init timed out" }
        composeTestRule.waitForIdle()
    }

    private fun waitForRadioSelected(tag: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            try {
                composeTestRule.onNodeWithTag(tag).assertIsSelected()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /**
     * 测试地图正常渲染
     */
    /**
     * 验证默认选中"手势焦点"模式
     */
    @Test
    fun defaultGestureCenterModeSelected() {
        navigateToZoomCenterModeScreen()
        composeTestRule.waitForIdle()
        val mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithText(getTestString(R.string.zoom_center_mode_title)).assertIsDisplayed()

        composeTestRule.onNodeWithTag("radio_zoom_center_gesture")
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule.onNodeWithTag("radio_zoom_center_screen")
            .assertIsDisplayed()
            .assertIsNotSelected()

        mapView.getMapAsync { map ->
            val mode = map.uiSettings.gestures.zoomCenterMode
            LoggerFactory.getLogger(LOG_MODULE).withTag("MapZoomCenterModeTest").d { "default zoomCenterMode: $mode" }
            assert(mode == Constants.ZOOM_CENTER_GESTURE)
        }
    }

    /**
     * 测试切换缩放中心模式：手势焦点 ↔ 屏幕中心
     */
    @Test
    fun switchZoomCenterModeUpdatesUiSettings() {
        navigateToZoomCenterModeScreen()
        composeTestRule.waitForIdle()
        val mapView = composeTestRule.getMapView()

        // 切换到屏幕中心模式
        composeTestRule.onNodeWithTag("radio_zoom_center_screen")
            .performScrollTo()
            .performClick()
        waitForRadioSelected("radio_zoom_center_screen")
        composeTestRule.onNodeWithTag("radio_zoom_center_gesture").assertIsNotSelected()

        mapView.getMapAsync { map ->
            val mode = map.uiSettings.gestures.zoomCenterMode
            LoggerFactory.getLogger(LOG_MODULE).withTag("MapZoomCenterModeTest").d { "after screen-center: $mode" }
            assert(mode == Constants.ZOOM_CENTER_SCREEN)
        }

        // 切换回手势焦点模式
        composeTestRule.onNodeWithTag("radio_zoom_center_gesture")
            .performScrollTo()
            .performClick()
        waitForRadioSelected("radio_zoom_center_gesture")
        composeTestRule.onNodeWithTag("radio_zoom_center_screen").assertIsNotSelected()

        mapView.getMapAsync { map ->
            val mode = map.uiSettings.gestures.zoomCenterMode
            LoggerFactory.getLogger(LOG_MODULE).withTag("MapZoomCenterModeTest").d { "after gesture-center: $mode" }
            assert(mode == Constants.ZOOM_CENTER_GESTURE)
        }
    }
}
