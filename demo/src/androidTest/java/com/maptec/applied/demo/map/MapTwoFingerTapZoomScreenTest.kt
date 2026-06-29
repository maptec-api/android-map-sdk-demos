package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.verifyMapViewSwitchToggle
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@RunWith(AndroidJUnit4::class)
class MapTwoFingerTapZoomScreenTest {

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

    private fun navigateToTwoFingerTapZoomScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_gesture)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_two_finger_tap_zoom))
            .performClick()
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
