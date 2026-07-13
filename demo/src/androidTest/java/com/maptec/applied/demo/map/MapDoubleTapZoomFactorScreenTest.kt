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
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertNotEquals
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
class MapDoubleTapZoomFactorScreenTest {

    companion object {
        private const val TAG_ZOOM_FACTOR_TEXT = "text_zoom_factor"
        private const val TAG_SLIDER = "slider_zoom_factor"
        private const val TAG_CLEAR_BUTTON = "button_clear_log"
        private const val TAG_ZOOM_LEVEL_TEXT = "text_zoom_level"
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

    @Before
    fun setUp() {
        navigateToDoubleTapZoomFactorScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航 ====================

    private fun navigateToDoubleTapZoomFactorScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_map_gesture, R.string.map_item_double_tap_zoom_factor)
        composeTestRule.waitForIdle()
    }

    // ==================== 异步 API 断言辅助 ====================

    private fun withMapSettings(action: (MaptecMap) -> Unit) {
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

        assertTrue("获取 Map 异步回调超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // ==================== 测试用例 ====================

    @Test
    fun initialZoomFactorDisplaysDefaultValue() {
        composeTestRule.onNodeWithTag(TAG_ZOOM_FACTOR_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ZOOM_FACTOR_TEXT)
            .assertTextContains(getTestString(R.string.double_tap_zoom_factor_label, 0f)
                .replace("0.0", "").trimEnd(), substring = true)

        composeTestRule.onNodeWithTag(TAG_ZOOM_LEVEL_TEXT).assertIsDisplayed()

        withMapSettings { map ->
            val factor = map.uiSettings.gestures.doubleTapZoomFactor
            assertTrue("默认双击缩放因子应大于 0", factor > 0f)
        }
    }

    @Test
    fun sliderChangesZoomFactorAndSyncsToApi() {
        var initialFactor = -1f
        withMapSettings { map ->
            initialFactor = map.uiSettings.gestures.doubleTapZoomFactor
        }

        composeTestRule.onNodeWithTag(TAG_SLIDER).performTouchInput {
            click(percentOffset(0.8f, 0.5f))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_ZOOM_FACTOR_TEXT)
            .assertTextContains(getTestString(R.string.double_tap_zoom_factor_label, 0f)
                .replace("0.0", "").trimEnd(), substring = true)

        withMapSettings { map ->
            val updatedFactor = map.uiSettings.gestures.doubleTapZoomFactor
            assertNotEquals("双击缩放因子应被修改", initialFactor, updatedFactor)
            assertTrue("在 80% 处点击，缩放因子应增大", updatedFactor > initialFactor)
        }
    }

    @Test
    fun sliderAtMinAndMaxClampsToRange() {
        composeTestRule.onNodeWithTag(TAG_SLIDER).performTouchInput {
            click(percentOffset(0.0f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withMapSettings { map ->
            val minFactor = map.uiSettings.gestures.doubleTapZoomFactor
            assertTrue("缩放因子不应低于最小值", minFactor >= 1.0f)
        }

        composeTestRule.onNodeWithTag(TAG_SLIDER).performTouchInput {
            click(percentOffset(1.0f, 0.5f))
        }
        composeTestRule.waitForIdle()

        withMapSettings { map ->
            val maxFactor = map.uiSettings.gestures.doubleTapZoomFactor
            assertTrue("缩放因子不应超过最大值", maxFactor <= 6.0f)
            assertTrue("最右端因子应大于最左端", maxFactor > 1.0f)
        }
    }
}
