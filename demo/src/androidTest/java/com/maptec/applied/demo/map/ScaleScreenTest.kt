package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.view.Gravity
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.widgets.ScaleView
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/**
 * ScaleScreen 集成测试：从「基础地图能力」进入比例尺演示页，校验地图展示、
 * 比例尺开关、位置与最大宽度滑块等控件。
 */
@RunWith(AndroidJUnit4::class)
class ScaleScreenTest {

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
        Thread.sleep(2000)
    }

    private fun getString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun navigateToScaleScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.map_item_scale_bar)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun findMapView(): MapView? =
        composeTestRule.activity.findViewById<View>(android.R.id.content)
            ?.findViewWithTag("mapView")

    @Test
    fun scaleScreen_toggleScaleBarEnabled() {
        navigateToScaleScreen()
        Thread.sleep(1000)

        val mapView = findMapView()
        assert(mapView != null) { "MapView not found" }

        composeTestRule.onNodeWithTag("switch_scale_enabled")
            .assertIsDisplayed()
            .assertIsOn()

        composeTestRule.onNodeWithTag("switch_scale_enabled").performClick()
        Thread.sleep(500)
        composeTestRule.onNodeWithTag("switch_scale_enabled").assertIsOff()

        mapView?.getMapAsync {
            assert(!it.uiSettings.isScaleBarEnabled) { "Scale bar should be disabled" }
        }
        Thread.sleep(500)

        composeTestRule.onNodeWithTag("switch_scale_enabled").performClick()
        Thread.sleep(500)
        composeTestRule.onNodeWithTag("switch_scale_enabled").assertIsOn()

        mapView?.getMapAsync {
            assert(it.uiSettings.isScaleBarEnabled) { "Scale bar should be enabled" }
        }
        Thread.sleep(500)
    }

    @Test
    fun scaleScreen_changeScaleBarGravity() {
        navigateToScaleScreen()
        Thread.sleep(1000)

        val mapView = findMapView()
        assert(mapView != null) { "MapView not found" }

        composeTestRule.onNodeWithTag("dropdown_scale_gravity").performClick()
        Thread.sleep(500)
        composeTestRule.onNodeWithText("左上 (TOP|START)").performClick()
        Thread.sleep(500)

        mapView?.getMapAsync {
            val expected = Gravity.TOP or Gravity.START
            assert(it.uiSettings.scaleBarGravity == expected) {
                "Scale bar gravity should be TOP|START, got ${it.uiSettings.scaleBarGravity}"
            }
        }
        Thread.sleep(500)

        composeTestRule.onNodeWithTag("dropdown_scale_gravity").performClick()
        Thread.sleep(500)
        composeTestRule.onNodeWithText("居中 (CENTER)").performClick()
        Thread.sleep(500)

        mapView?.getMapAsync {
            val expected = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            assert(it.uiSettings.scaleBarGravity == expected) {
                "Scale bar gravity should be CENTER, got ${it.uiSettings.scaleBarGravity}"
            }
        }
        Thread.sleep(500)

        composeTestRule.onNodeWithTag("dropdown_scale_gravity").performClick()
        Thread.sleep(500)
        composeTestRule.onNodeWithText("左下 (BOTTOM|START)").performClick()
        Thread.sleep(500)
    }


    @Test
    fun scaleScreen_defaultScaleBarMaxWidth_matchesSdkDefault() {
        navigateToScaleScreen()
        Thread.sleep(2000)

        val mapView = findMapView()
        assert(mapView != null) { "MapView not found" }

        mapView?.getMapAsync {
            val sv = requireNotNull(it.uiSettings.scaleView) {
                "ScaleView should exist when scale bar is enabled"
            }
            assert(sv.scaleBarMaxWidthPx == ScaleView.SCALE_BAR_MAX_WIDTH_DEFAULT_PX) {
                "Default max width should be ${ScaleView.SCALE_BAR_MAX_WIDTH_DEFAULT_PX}, got ${sv.scaleBarMaxWidthPx}"
            }
        }
        Thread.sleep(500)
    }
}
