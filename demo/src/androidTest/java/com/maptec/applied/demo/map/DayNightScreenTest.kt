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
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.DEMO_CONFIG_PANEL_TOGGLE_TAG
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * DayNightScreen 集成测试：覆盖日夜模式开关、位置下拉选择与返回导航。
 */
@RunWith(AndroidJUnit4::class)
class DayNightScreenTest {

    companion object {
        private const val TAG_SWITCH = "switch_day_night_enabled"
        private const val TAG_DROPDOWN = "dropdown_day_night_gravity"
        private const val TAG_TEXT_FIELD = "textfield_day_night_gravity"
        private const val TAG_MENU = "menu_day_night_gravity"
    }

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
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

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToDayNightScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToDayNightScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openUiControlsDemo(R.string.map_item_day_night_mode)
        composeTestRule.waitForIdle()
    }

    // ==================== 异步 API 断言辅助 ====================

    private fun withMaptecMap(action: (com.maptec.applied.maps.MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("异步获取 MaptecMap 超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // ==================== 测试用例 ====================

    @Test
    fun testDayNightSwitch_TogglesApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsDisplayed()

        withMaptecMap { assertTrue("日夜模式默认应开启", it.uiSettings.isDayNightModeEnabled) }
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOn()

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOff()
        withMaptecMap { assertEquals(false, it.uiSettings.isDayNightModeEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOn()
        withMaptecMap { assertEquals(true, it.uiSettings.isDayNightModeEnabled) }
    }

    @Test
    fun testBuiltInDayNightToggle_ChangesMapStyle() {
        val dayNightButton = mapView.findViewWithTag<View>("dayNightModeView")
        assertNotNull("内置日夜切换按钮应存在", dayNightButton)
        assertTrue("内置按钮应可见", dayNightButton.visibility == View.VISIBLE)

        assertFalse("初始应为白天模式", mapView.isDayNightModeNight)

        composeTestRule.activity.runOnUiThread {
            dayNightButton.performClick()
        }
        composeTestRule.waitUntil(10_000) {
            mapView.isDayNightModeNight
        }

        composeTestRule.activity.runOnUiThread {
            dayNightButton.performClick()
        }
        composeTestRule.waitUntil(10_000) {
            !mapView.isDayNightModeNight
        }
    }

    @Test
    fun testGravityDropdown_SelectsAndUpdatesApi() {
        composeTestRule.onNodeWithTag(TAG_TEXT_FIELD).performScrollTo()

        composeTestRule.onNodeWithTag(TAG_TEXT_FIELD).performClick()
        composeTestRule.waitForIdle()

        val bottomStartText = getTestString(R.string.day_night_gravity_bottom_start)
        composeTestRule.onNodeWithText(bottomStartText).assertIsDisplayed()
        composeTestRule.onNodeWithText(bottomStartText).performClick()
        composeTestRule.waitForIdle()

        withMaptecMap { map ->
            val gravity = map.uiSettings.dayNightModeGravity
            assertEquals(
                "按钮位置应更新为 BOTTOM|START",
                Gravity.BOTTOM or Gravity.START,
                gravity
            )
        }

        composeTestRule.onNodeWithTag(TAG_TEXT_FIELD).performClick()
        composeTestRule.waitForIdle()

        val bottomEndText = getTestString(R.string.day_night_gravity_bottom_end)
        composeTestRule.onNodeWithText(bottomEndText).performClick()
        composeTestRule.waitForIdle()

        withMaptecMap { map ->
            assertEquals(
                "按钮位置应更新为 BOTTOM|END",
                Gravity.BOTTOM or Gravity.END,
                map.uiSettings.dayNightModeGravity
            )
        }
    }

    @Test
    fun testConfigPanel_CollapsesAndExpands() {
        composeTestRule.onNodeWithTag(TAG_SWITCH).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEMO_CONFIG_PANEL_TOGGLE_TAG).assertDoesNotExist()

        composeTestRule.collapseConfigPanel()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DEMO_CONFIG_PANEL_TOGGLE_TAG).assertIsDisplayed()

        composeTestRule.expandConfigPanel()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DEMO_CONFIG_PANEL_TOGGLE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SWITCH).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsDisplayed()
    }

    @Test
    fun testBack_ReturnsToMapItemList() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.map_item_day_night_mode)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_zoom)).assertIsDisplayed()
    }

    @Test
    fun testDoubleBack_ReturnsToMainScreen() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.catalog_main_interaction)).assertIsDisplayed()
    }
}
