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
import com.maptec.applied.demo.ext.waitForApiResponseKey
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * GeocodeScreen API 集成测试：通过 UI 交互触发地理编码 API 调用，
 * 利用 ApiResponseDebugCard 中的 JSON 响应进行 API 功能验证。
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

    private fun getString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun navigateToGeocodeScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.screen_item_geocode)).performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(getString(R.string.geocode_submit))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForApiResponse(timeoutMs: Long = 60_000) {
        val apiTitle = getString(R.string.geocode_api_response_title)
        val loadingText = getString(R.string.geocode_loading)
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("api_response_card", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("geocode_error_message", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("status:", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText(apiTitle, substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(loadingText, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
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



    // ==================== 1. 正向地理编码 ====================

    @Test
    fun forwardGeocode_basic_showsApiResponse() {
        navigateToGeocodeScreen()
        // 默认 address 已是 "Lorong"，直接点击提交
        composeTestRule.onNodeWithText(getString(R.string.geocode_submit)).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("results")
    }

    @Test
    fun forwardGeocode_withAddressAndComponents_showsApiResponse() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_address), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_address), substring = true)
            .performTextInput("Singapore")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextInput("en")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_region), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_region), substring = true)
            .performTextInput("SG")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.geocode_submit)).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("results")
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
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("results")
    }

    // ==================== 2. 反向地理编码 ====================

    @Test
    fun reverseGeocode_basic_showsApiResponse() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithText("反向地理编码").performClick()
        composeTestRule.waitForIdle()
        // 默认 location 已是 "1.46878,103.80373"，直接点击提交
        composeTestRule.onNodeWithText(getString(R.string.geocode_submit)).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("results")
    }

    @Test
    fun reverseGeocode_withAllParams_showsApiResponse() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithText("反向地理编码").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_location), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_location), substring = true)
            .performTextInput("1.35,103.82")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_result_type), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_result_type), substring = true)
            .performTextInput("street_address")
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextClearance()
        composeTestRule.onNodeWithText(getString(R.string.geocode_label_language), substring = true)
            .performTextInput("en")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.geocode_submit)).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("results")
    }

    // ==================== 3. Tab 切换 ====================

    @Test
    fun tabSwitching_noCrash() {
        navigateToGeocodeScreen()
        composeTestRule.onNodeWithText("反向地理编码").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("正向地理编码").performClick()
        composeTestRule.waitForIdle()
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
        composeTestRule.onNodeWithText(getString(R.string.screen_item_geocode)).assertIsDisplayed()
    }
}
