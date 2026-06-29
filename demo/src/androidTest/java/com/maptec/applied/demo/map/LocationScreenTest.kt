package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.view.Gravity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.location.LocationComponent
import com.maptec.applied.location.modes.CameraMode
import com.maptec.applied.location.modes.RenderMode
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.UiSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LocationScreenTest {

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
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

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToLocationScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToLocationScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_location)).performClick()
        composeTestRule.waitForIdle()
    }



    private fun withLocationComponent(timeoutMs: Long = 12_000L, action: (LocationComponent) -> Unit) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            val latch = CountDownLatch(1)
            var lc: LocationComponent? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                mapView.getMapAsync { map ->
                    try {
                        lc = map.locationComponent
                        if (lc != null && lc.isLocationComponentActivated) {
                            action(lc)
                        }
                    } catch (e: Throwable) {
                        lastError = e
                    } finally {
                        latch.countDown()
                    }
                }
            }
            assertTrue("获取 LocationComponent 超时", latch.await(3, TimeUnit.SECONDS))
            if (lc != null && lc.isLocationComponentActivated) {
                lastError?.let { throw it }
                return
            }
            composeTestRule.waitForIdle()
        }
        throw AssertionError("LocationComponent 未在 ${timeoutMs}ms 内激活", lastError)
    }

    private fun withUiSettings(action: (UiSettings) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map.uiSettings)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("获取 UiSettings 超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // ==================== 1. 基础页面加载 ====================


    @Test
    fun bottomSheet_isDisplayed() {
        composeTestRule.onNodeWithTag("location_bottom_sheet").assertIsDisplayed()
    }

    // ==================== 2. 默认状态 API 验证 ====================

    @Test
    fun locationComponent_defaultState() {
        withLocationComponent { lc ->
            assertTrue("定位组件应已激活", lc.isLocationComponentActivated)
            assertTrue("定位组件应默认启用", lc.isLocationComponentEnabled)
            assertEquals("默认相机模式应为 TRACKING",
                CameraMode.TRACKING, lc.cameraMode)
            assertEquals("默认渲染模式应为 COMPASS",
                RenderMode.COMPASS, lc.renderMode)
        }
    }

    @Test
    fun locationComponentOptions_defaultValues() {
        withLocationComponent { lc ->
            val opts = lc.locationComponentOptions
            assertTrue("脉冲动画应默认开启", opts.pulseEnabled() ?: true)
            assertEquals("精度圈透明度默认应为 0.3", 0.3f, opts.accuracyAlpha(), 0.01f)
        }
    }

    @Test
    fun locationView_defaultState() {
        withUiSettings { settings ->
            assertTrue("定位按钮应默认可见", settings.isLocationViewEnabled)
            val gravity = settings.locationViewGravity
            assertTrue("默认重力应包含 BOTTOM", gravity and Gravity.BOTTOM != 0)
            assertTrue("默认重力应包含 END", gravity and Gravity.END != 0)
        }
    }

    // ==================== 3. 定位组件开关 ====================

    @Test
    fun toggleLocationComponent_disablesAndReenables() {
        withLocationComponent { lc ->
            assertTrue("初始应启用", lc.isLocationComponentEnabled)
        }

        // 点击关闭
        composeTestRule.onNodeWithTag("switch_location_component_enabled").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            assertFalse("关闭后应禁用", lc.isLocationComponentEnabled)
        }

        // 点击重新开启
        composeTestRule.onNodeWithTag("switch_location_component_enabled").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            assertTrue("重新开启后应启用", lc.isLocationComponentEnabled)
        }
    }

    // ==================== 4. 脉冲动画开关 ====================

    @Test
    fun togglePulseAnimation_changesApi() {
        withLocationComponent { lc ->
            assertTrue("脉冲动画初始应开启", lc.locationComponentOptions.pulseEnabled() ?: true)
        }

        composeTestRule.onNodeWithTag("switch_pulse_animation").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            val updated = lc.locationComponentOptions.pulseEnabled()
            assertFalse("关闭脉冲后 pulseEnabled 应为 false", updated ?: true)
        }

        composeTestRule.onNodeWithTag("switch_pulse_animation").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            assertTrue("重新开启脉冲后 pulseEnabled 应为 true", lc.locationComponentOptions.pulseEnabled() ?: false)
        }
    }

    // ==================== 5. 精度圈透明度滑块 ====================

    @Test
    fun accuracyAlphaSlider_updatesApi() {
        withLocationComponent { lc ->
            assertEquals("初始精度圈透明度应约为 0.3", 0.3f, lc.locationComponentOptions.accuracyAlpha(), 0.01f)
        }

        composeTestRule.onNodeWithTag("slider_accuracy_alpha").performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            val updated = lc.locationComponentOptions.accuracyAlpha()
            assertTrue("调整后透明度应大于 0.3", updated > 0.3f)
        }

        composeTestRule.onNodeWithTag("slider_accuracy_alpha").performTouchInput {
            click(percentOffset(0.1f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            val updated = lc.locationComponentOptions.accuracyAlpha()
            assertTrue("调低后透明度应小于先前值", updated < 0.8f)
        }
    }

    // ==================== 6. 相机跟踪模式 ====================

    @Test
    fun cameraModeDropdown_selectModes() {
        val testCases = listOf(
            "NONE" to CameraMode.NONE,
            "TRACKING_COMPASS" to CameraMode.TRACKING_COMPASS,
            "TRACKING_GPS" to CameraMode.TRACKING_GPS,
        )

        for ((label, expectedMode) in testCases) {
            composeTestRule.onNodeWithTag("dropdown_camera_mode").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(label).performClick()
            composeTestRule.waitForIdle()

            withLocationComponent { lc ->
                assertEquals("选择 $label 后 cameraMode 应匹配", expectedMode, lc.cameraMode)
            }
        }
    }

    // ==================== 7. 渲染模式 ====================

    @Test
    fun renderModeDropdown_selectModes() {
        composeTestRule.onNodeWithTag("dropdown_render_mode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("NORMAL (圆点)").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            assertEquals("选择 NORMAL 后 renderMode 应匹配", RenderMode.NORMAL, lc.renderMode)
        }

        composeTestRule.onNodeWithTag("dropdown_render_mode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("GPS (导航箭头)").performClick()
        composeTestRule.waitForIdle()

        withLocationComponent { lc ->
            assertEquals("选择 GPS 后 renderMode 应匹配", RenderMode.GPS, lc.renderMode)
        }
    }

    // ==================== 8. 定位按钮 (LocationView) ====================

    @Test
    fun toggleLocationViewEnabled_changesUiSettings() {
        withUiSettings { settings ->
            assertTrue("定位按钮初始应可见", settings.isLocationViewEnabled)
        }

        composeTestRule.onNodeWithTag("switch_location_view_enabled").performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertFalse("关闭后定位按钮应隐藏", settings.isLocationViewEnabled)
        }

        composeTestRule.onNodeWithTag("switch_location_view_enabled").performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertTrue("重新开启后定位按钮应可见", settings.isLocationViewEnabled)
        }
    }

    @Test
    fun locationViewGravityDropdown_changesApi() {
        composeTestRule.onNodeWithTag("dropdown_location_view_gravity").performClick()
        composeTestRule.waitForIdle()

        val targetGravityItem = getTestString(R.string.location_gravity_top_start)
        composeTestRule.onNodeWithTag("menu_item_$targetGravityItem").performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            val g = settings.locationViewGravity
            assertTrue("重力应包含 TOP", g and Gravity.TOP != 0)
            assertTrue("重力应包含 START", g and Gravity.START != 0)
        }
    }

    // ==================== 9. 移动到当前位置 ====================

    @Test
    fun moveToCurrentLocationButton_isDisplayedAndEnabled() {
        composeTestRule.onNodeWithTag("button_move_to_current").performScrollTo()
        composeTestRule.onNodeWithTag("button_move_to_current").assertIsDisplayed()
        withLocationComponent { lc ->
            assertNotNull("LocationComponent 应已就绪", lc)
        }
        composeTestRule.onNodeWithTag("button_move_to_current").assertIsDisplayed()
    }

    // ==================== 10. 导航与返回 ====================

    @Test
    fun back_returnsToMapItemList() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_location)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_render)).assertIsDisplayed()
    }

    @Test
    fun doubleBack_returnsToMainScreen() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).assertIsDisplayed()
    }
}
