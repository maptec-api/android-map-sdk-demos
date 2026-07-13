package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.view.Gravity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
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
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.UiSettings
import com.maptec.applied.maps.ZoomButtonsView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ZoomScreenTest {

    companion object {
        private const val TAG_SWITCH = "switch_zoom_enabled"
        private const val TAG_SWITCH_LEVEL_VISIBLE = "switch_zoom_level_visible"
        private const val TAG_DROPDOWN = "textfield_zoom_gravity"
        private const val TAG_SLIDER_BUTTON_SIZE = "slider_zoom_button_size"
        private const val TAG_DROPDOWN_PRECISION = "textfield_zoom_precision"
        private const val TAG_DROPDOWN_LEVEL_POSITION = "textfield_zoom_level_position"
    }

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

    private lateinit var mapView: MapView

    private val gravityOptions by lazy {
        listOf(
            getTestString(R.string.zoom_gravity_top_end) to (Gravity.TOP or Gravity.END),
            getTestString(R.string.zoom_gravity_top_start) to (Gravity.TOP or Gravity.START),
            getTestString(R.string.zoom_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
            getTestString(R.string.zoom_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
            getTestString(R.string.zoom_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.zoom_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.zoom_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
            getTestString(R.string.zoom_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.zoom_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END)
        )
    }

    @Before
    fun setUp() {
        navigateToZoomScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        //在测试环境移除 FPS 监听，防止不断触发 State 更新导致 waitForIdle() 死锁卡住
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                map.setOnFpsChangedListener(null)
            }
        }
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToZoomScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openUiControlsDemo(R.string.map_item_zoom)
        composeTestRule.waitForIdle()
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
        assert(latch.await(3, TimeUnit.SECONDS)) { "获取 UiSettings 超时" }
        error?.let { throw it }
    }

    /** 精度/位置下拉依赖「显示比例尺」开启；SDK 默认 zoomLevelVisible=false。 */
    private fun ensureZoomLevelVisibleEnabled() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).performScrollTo()
        var visible = false
        withUiSettings { visible = it.isZoomLevelVisible }
        if (!visible) {
            composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).performClick()
            composeTestRule.waitForIdle()
        }
        withUiSettings { assertTrue("应先开启缩放级别显示", it.isZoomLevelVisible) }
    }

    @Test
    fun toggleEnableZoomButtons() {
        composeTestRule.onNodeWithTag(TAG_SWITCH)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsOn()

        withUiSettings { settings ->
            assert(settings.isZoomButtonsEnabled) { "Zoom buttons should be enabled initially" }
        }

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOff()
        withUiSettings { settings ->
            assert(!settings.isZoomButtonsEnabled) { "Zoom buttons should be disabled after toggle" }
        }

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOn()
        withUiSettings { settings ->
            assert(settings.isZoomButtonsEnabled) { "Zoom buttons should be enabled after second toggle" }
        }
    }

    @Test
    fun selectAllGravityOptions() {
        for ((displayName, expectedGravity) in gravityOptions) {
            composeTestRule.onNodeWithTag(TAG_DROPDOWN).performScrollTo()
            composeTestRule.onNodeWithTag(TAG_DROPDOWN).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("zoom_menu_item_$displayName")
                .performScrollTo()
                .performClick()

            composeTestRule.waitForIdle()

            withUiSettings { settings ->
                assertEquals(
                    "选择 $displayName 后 zoomButtonsGravity 未正确更新",
                    expectedGravity,
                    settings.zoomButtonsGravity
                )
            }
        }
    }

    @Test
    fun toggleZoomLevelVisible_updatesApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).performScrollTo()

        var initialVisible = true
        withUiSettings { initialVisible = it.isZoomLevelVisible }
        // UiSettings 默认 zoomLevelVisible=false，LaunchedEffect 会同步到 Switch
        if (initialVisible) {
            composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).assertIsOn()
        } else {
            composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).assertIsOff()
        }

        composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).performClick()
        composeTestRule.waitForIdle()
        withUiSettings {
            assertEquals("第一次切换后 API 应取反", !initialVisible, it.isZoomLevelVisible)
        }

        composeTestRule.onNodeWithTag(TAG_SWITCH_LEVEL_VISIBLE).performClick()
        composeTestRule.waitForIdle()
        withUiSettings {
            assertEquals("第二次切换后 API 应恢复", initialVisible, it.isZoomLevelVisible)
        }
    }

    @Test
    fun sliderZoomButtonSize_updatesApi() {
        var initialSize = 0
        withUiSettings { initialSize = it.zoomButtonsSize }

        composeTestRule.onNodeWithTag(TAG_SLIDER_BUTTON_SIZE).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SLIDER_BUTTON_SIZE).performTouchInput {
            click(percentOffset(0.9f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assert(settings.zoomButtonsSize > initialSize) {
                "调大滑块后 zoomButtonsSize 应增大，initial=$initialSize actual=${settings.zoomButtonsSize}"
            }
        }
    }

    @Test
    fun selectZoomPrecision_updatesApi() {
        ensureZoomLevelVisibleEnabled()

        composeTestRule.onNodeWithTag(TAG_DROPDOWN_PRECISION).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("zoom_precision_item_2")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertEquals("精度应为 2 位小数", "%.2f", settings.zoomLevelFormat)
        }
    }

    @Test
    fun selectZoomLevelPosition_updatesApi() {
        ensureZoomLevelVisibleEnabled()

        val topLabel = getTestString(R.string.zoom_level_position_top)
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_LEVEL_POSITION).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("zoom_level_position_item_$topLabel")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertEquals(
                "缩放级别位置应更新为 TOP",
                ZoomButtonsView.ZOOM_LEVEL_POSITION_TOP,
                settings.zoomLevelPosition,
            )
        }
    }
}
