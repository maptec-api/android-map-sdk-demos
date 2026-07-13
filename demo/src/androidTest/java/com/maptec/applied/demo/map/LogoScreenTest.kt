package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import android.view.Gravity
import androidx.compose.ui.test.assertIsDisplayed
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
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LogoScreenTest {

    companion object {
        private const val TAG_DROPDOWN = "dropdown_logo_gravity"
    }

    private val gravityOptions by lazy {
        listOf(
            getTestString(R.string.logo_gravity_top_end) to (Gravity.TOP or Gravity.END),
            getTestString(R.string.logo_gravity_top_start) to (Gravity.TOP or Gravity.START),
            getTestString(R.string.logo_gravity_bottom_end) to (Gravity.BOTTOM or Gravity.END),
            getTestString(R.string.logo_gravity_bottom_start) to (Gravity.BOTTOM or Gravity.START),
            getTestString(R.string.logo_gravity_top_center) to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.logo_gravity_bottom_center) to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.logo_gravity_middle_start) to (Gravity.CENTER_VERTICAL or Gravity.START),
            getTestString(R.string.logo_gravity_center) to (Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL),
            getTestString(R.string.logo_gravity_middle_end) to (Gravity.CENTER_VERTICAL or Gravity.END)
        )
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
        navigateToLogoScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToLogoScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openUiControlsDemo(R.string.map_item_logo)
        composeTestRule.waitForIdle()
    }

    private fun withLogoGravity(action: (Int) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map.uiSettings.logoGravity)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        val success = latch.await(3, TimeUnit.SECONDS)
        assert(success) { "获取 logoGravity 超时" }
        error?.let { throw it }
    }


    @Test
    fun selectAllGravityOptions() {
        composeTestRule.waitForMapDemoReady()
        for ((displayName, expectedGravity) in gravityOptions) {
            composeTestRule.onNodeWithTag(TAG_DROPDOWN).performScrollTo()
            composeTestRule.onNodeWithTag(TAG_DROPDOWN).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(displayName).performClick()
            composeTestRule.waitForIdle()

            withLogoGravity { gravity ->
                assertEquals(
                    "选择 $displayName 后 logoGravity 未正确更新",
                    expectedGravity,
                    gravity
                )
            }
        }
    }
}
