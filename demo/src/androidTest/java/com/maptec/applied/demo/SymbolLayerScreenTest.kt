package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * [MarkerBasicScreen] UI 冒烟测试：基础标记添加指示器。
 */
@RunWith(AndroidJUnit4::class)
class SymbolLayerScreenTest {

    companion object {
        private const val TAG_INPUT_LATLNG = "symbol_input_default_latlng"
        private const val TAG_BTN_ADD_BY_TYPE = "symbol_btn_add_by_type"
        private const val TAG_HAS_MARKERS = "symbol_layer_has_markers"
    }

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        *if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            emptyArray()
        },
    )

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    @Before
    fun setUp() {
        navigateToSymbolLayerScreen()
        composeTestRule.waitForMapDemoReady()
        Thread.sleep(1500)
    }

    private fun navigateToSymbolLayerScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_marker)
        composeTestRule.waitForIdle()
    }

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickButton(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForHasMarkers(timeoutMs: Long = 5000L) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag(TAG_HAS_MARKERS).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testAddSymbolByType_showsMarkerIndicator() {
        clickButton(TAG_BTN_ADD_BY_TYPE)
        waitForHasMarkers()
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
    }

    @Test
    fun testAddSymbolByType_twice_increasesIndicator() {
        clickButton(TAG_BTN_ADD_BY_TYPE)
        waitForHasMarkers()
        clickButton(TAG_BTN_ADD_BY_TYPE)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
    }

    @Test
    fun testInvalidLatLng_addButtonDisabled() {
        setInput(TAG_INPUT_LATLNG, "invalid")
        composeTestRule.onNodeWithTag(TAG_BTN_ADD_BY_TYPE).performScrollTo().assertIsNotEnabled()
    }
}
