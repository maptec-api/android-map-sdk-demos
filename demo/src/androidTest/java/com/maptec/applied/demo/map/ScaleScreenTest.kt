package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.view.Gravity
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
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.widgets.ScaleView
import org.junit.After
import org.junit.Assert.assertEquals
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
class ScaleScreenTest {

    companion object {
        private const val TAG_SWITCH = "switch_scale_enabled"
        private const val TAG_DROPDOWN = "dropdown_scale_gravity"
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
    val ruleChain: TestRule = RuleChain
        .outerRule(permissionRule)
        .around(composeTestRule)

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToScaleScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
    }

    private fun navigateToScaleScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openUiControlsDemo(R.string.map_item_scale_bar)
        composeTestRule.waitForIdle()
    }

    private fun withUiSettings(action: (com.maptec.applied.maps.UiSettings) -> Unit) {
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
        assertTrue("获取 UiSettings 超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    @Test
    fun toggleScaleBarEnabled_updatesApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsDisplayed().assertIsOn()
        withUiSettings { assertTrue("比例尺默认应开启", it.isScaleBarEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOff()
        withUiSettings { assertEquals(false, it.isScaleBarEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_SWITCH).assertIsOn()
        withUiSettings { assertEquals(true, it.isScaleBarEnabled) }
    }

    @Test
    fun changeScaleBarGravity_updatesApi() {
        composeTestRule.onNodeWithTag(TAG_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.zoom_gravity_top_start)).performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertEquals("比例尺位置应更新为 TOP|START", Gravity.TOP or Gravity.START, settings.scaleBarGravity)
        }

        composeTestRule.onNodeWithTag(TAG_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.zoom_gravity_center)).performClick()
        composeTestRule.waitForIdle()

        withUiSettings { settings ->
            assertEquals("比例尺位置应更新为 CENTER", Gravity.CENTER, settings.scaleBarGravity)
        }
    }

    @Test
    fun defaultScaleBarMaxWidth_matchesSdkDefault() {
        var maxWidth = -1
        withUiSettings { settings ->
            val sv = settings.scaleView
            maxWidth = sv?.scaleBarMaxWidthPx ?: -1
        }
        assertEquals("默认最大宽度应匹配 SDK 默认值", ScaleView.SCALE_BAR_MAX_WIDTH_DEFAULT_PX, maxWidth)
    }
}
