package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.DEMO_BACK_BUTTON_TAG
import com.maptec.applied.demo.ext.clickClickableText
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForApiResponseKey
import com.maptec.applied.demo.viewmodel.GeocodeViewModel.Mode
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * 地理编码 Screen API 集成测试（ForwardGeocodeScreen / ReverseGeocodeScreen）。
 */
@RunWith(AndroidJUnit4::class)
class GeocodeScreenTest {

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

    private fun getString(resId: Int): String = getTestString(resId)

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
    }

    private fun navigateToGeocodeScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openWebServicesDemo(R.string.catalog_forward_geocode)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(getString(R.string.geocode_submit))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun catalogResForMode(mode: Mode): Int = when (mode) {
        Mode.FORWARD -> R.string.catalog_forward_geocode
        Mode.REVERSE -> R.string.catalog_reverse_geocode
    }

    private fun switchToGeocodeMode(mode: Mode) {
        composeTestRule.onNodeWithTag(DEMO_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.clickClickableText(catalogResForMode(mode))
        composeTestRule.waitForIdle()
    }

    private fun waitForApiResponse(timeoutMs: Long = 60_000) {
        val loadingText = getString(R.string.geocode_loading)
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("geocode_error_message", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("status:", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(loadingText, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun expandGeocodeAdvanced() {
        composeTestRule.onNodeWithText(getString(R.string.search_advanced_more)).performScrollTo()
        composeTestRule.onNodeWithText(getString(R.string.search_advanced_more)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun replaceLatLngField(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickSubmit() {
        composeTestRule.onNodeWithTag("geocode_submit_button").performScrollTo()
        composeTestRule.onNodeWithTag("geocode_submit_button").performClick()
    }

    private fun verifyApiResponse() {
        composeTestRule.waitUntil(30_000) {
            composeTestRule.onAllNodesWithText("status:", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("geocode_error_message", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val hasError = composeTestRule.onAllNodesWithTag("geocode_error_message", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (hasError) {
            composeTestRule.onNodeWithTag("geocode_error_message").assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("status:", substring = true).assertIsDisplayed()
        }
    }


    // ==================== 1. 正向地理编码 ====================

    @Test
    fun forwardGeocode_basic_showsApiResponse() {
        navigateToGeocodeScreen()
        clickSubmit()
        waitForApiResponse()
        verifyApiResponse()
    }

    @Test
    fun forwardGeocode_withAddressAndComponents_showsApiResponse() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_address), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_address), substring = true)
            .performTextInput("Singapore")
        expandGeocodeAdvanced()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextInput("en")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_region), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_region), substring = true)
            .performTextInput("SG")
        composeTestRule.waitForIdle()
        clickSubmit()
        waitForApiResponse()
        verifyApiResponse()
    }

    @Test
    fun forwardGeocode_withBiasRectangle_showsApiResponse() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithTag("locationBiasSwitch").performScrollTo()
        composeTestRule.onNodeWithTag("locationBiasSwitch").performClick()
        composeTestRule.waitForIdle()
        replaceLatLngField("geocode_input_rect_ne", "1.4,103.8")
        replaceLatLngField("geocode_input_rect_sw", "1.2,103.6")
        composeTestRule.onNodeWithTag("geocode_submit_button").performScrollTo()
        composeTestRule.onNodeWithTag("geocode_submit_button").assertIsEnabled()
        clickSubmit()
        waitForApiResponse()
        verifyApiResponse()
    }

    // ==================== 2. 反向地理编码 ====================

    @Test
    fun reverseGeocode_basic_showsApiResponse() {
        navigateToGeocodeScreen()
        switchToGeocodeMode(Mode.REVERSE)
        clickSubmit()
        waitForApiResponse()
        verifyApiResponse()
    }

    @Test
    fun reverseGeocode_withAllParams_showsApiResponse() {
        navigateToGeocodeScreen()
        switchToGeocodeMode(Mode.REVERSE)
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_location), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_location), substring = true)
            .performTextInput("1.35,103.82")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_result_type), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_result_type), substring = true)
            .performTextInput("street_address")
        expandGeocodeAdvanced()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextInput("en")
        composeTestRule.waitForIdle()
        clickSubmit()
        waitForApiResponse()
        verifyApiResponse()
    }

    // ==================== 3. Tab 切换 ====================

    @Test
    fun tabSwitching_noCrash() {
        navigateToGeocodeScreen()
        switchToGeocodeMode(Mode.REVERSE)
        switchToGeocodeMode(Mode.FORWARD)
        assertFalse(composeTestRule.activity.isFinishing)
    }

    // ==================== 4. 导航 ====================

    @Test
    fun backNavigation_returnsToMainScreen() {
        navigateToGeocodeScreen()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.catalog_forward_geocode)).assertIsDisplayed()
    }
}
