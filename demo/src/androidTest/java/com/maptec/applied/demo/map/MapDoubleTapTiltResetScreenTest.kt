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
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@RunWith(AndroidJUnit4::class)
class MapDoubleTapTiltResetScreenTest {

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
