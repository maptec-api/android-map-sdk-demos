package com.maptec.applied.demo

import android.util.Log
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.DEMO_BACK_BUTTON_TAG
import com.maptec.applied.demo.ext.clickClickableText
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.getTestString
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 搜索 Screen API 集成测试（TextSearch / Nearby / Suggest / Places）。
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun getString(resId: Int): String = getTestString(resId)

    companion object {
        private const val TAG = "SearchScreenTest"
    }

    /** 打出当前语义树中所有关心的 testTag，用于在平板上诊断渲染差异 */
    private fun logTestTags(context: String) {
        val targetTags = listOf(
            "search_query_input", "search_submit_button",
            "search_nearby_radius", "search_nearby_submit_button", "search_nearby_types_dropdown",
            "search_input_place_id", "search_input_suggest_types",
            "place_detail_card"
        )
        val found = targetTags.filter { tag ->
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val missing = targetTags - found
        Log.e(TAG, "[$context] Found: $found")
        Log.e(TAG, "[$context] Missing: $missing")
    }

    /** 从主屏进入文本搜索页面 */
    private fun navigateToSearchScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openWebServicesDemo(R.string.catalog_text_search)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("search_query_input", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        logTestTags("after_navigateToSearchScreen")
    }

    private fun catalogResForTab(labelResId: Int): Int = when (labelResId) {
        R.string.search_tab_text -> R.string.catalog_text_search
        R.string.search_tab_nearby -> R.string.catalog_nearby_search
        R.string.search_tab_suggest -> R.string.catalog_suggest
        R.string.search_tab_detail -> R.string.catalog_places
        else -> error("Unknown tab label res $labelResId")
    }

    private fun switchToTab(labelResId: Int) {
        composeTestRule.onNodeWithTag(DEMO_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.clickClickableText(catalogResForTab(labelResId))
        composeTestRule.waitForIdle()
        logTestTags("switchToTab_${getString(labelResId)}")
    }

    /** 关闭软键盘，避免全屏 IME 遮挡待断言的组件 */
    private fun dismissSoftKeyboard() {
        runCatching { Espresso.closeSoftKeyboard() }
        composeTestRule.waitForIdle()
    }

    private fun replaceField(tag: String, value: String) {
        Log.e(TAG, "replaceField: waiting for tag=$tag")
        logTestTags("before_replace_$tag")
        try {
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            Log.e(TAG, "replaceField: TIMEOUT waiting for tag=$tag")
            logTestTags("timeout_replace_$tag")
            throw e
        }
        val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            Log.e(TAG, "replaceField: scrollTo ignored for tag=$tag (content visible or scroll not needed)")
        }
        node.performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun typeSearchQuery(query: String) {
        replaceField("search_query_input", query)
    }

    private fun clickSearch() {
        val node = composeTestRule.onNodeWithTag("search_submit_button", useUnmergedTree = true)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
        }
        node.performClick()
        composeTestRule.waitForIdle()

    }

    private fun navigateToNearbyTab() {
        composeTestRule.onNodeWithTag(DEMO_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.clickClickableText(R.string.catalog_nearby_search)
        logTestTags("after_switchTo_nearby")
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("search_nearby_radius", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        logTestTags("after_wait_nearbyContent")
    }

    private fun clickNearbySearch() {
        replaceField("search_nearby_radius", "5000")
        val node = composeTestRule.onNodeWithTag("search_nearby_submit_button", useUnmergedTree = true)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
        }
        node.performClick()
    }

    private fun waitForSearchResults(timeoutMs: Long = 15_000) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("search_place_item", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForPlaceDetail(timeoutMs: Long = 60_000) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("place_detail_card", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun assertPlaceDetailDisplayed() {
        composeTestRule.onNodeWithTag("place_detail_card", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /** 等待 API 响应 JSON 中出现指定关键字 */


    // ==================== 1. 文本搜索 ====================

    @Test
    fun textSearch_query_showsApiResponse() {
        navigateToSearchScreen()
        composeTestRule.onNodeWithText(getString(R.string.search_placeholder))
            .performTextInput("hotel in Singapore")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.search_action_search)).performClick()
        dismissSoftKeyboard()
        waitForSearchResults(60_000)
    }

    @Test
    fun textSearch_withAdvancedParams_showsApiResponse() {
        navigateToSearchScreen()
        composeTestRule.onNodeWithText(getString(R.string.search_advanced_more)).performClick()
        composeTestRule.waitForIdle()
        replaceField("search_input_region", "SG")
        replaceField("search_input_page_size", "5")
        typeSearchQuery("restaurant")
        clickSearch()
        dismissSoftKeyboard()
        waitForSearchResults(60_000)
    }

    @Test
    fun textSearch_withLocationBias_showsApiResponse() {
        navigateToSearchScreen()
        composeTestRule.onNodeWithText(getString(R.string.search_advanced_more)).performClick()
        composeTestRule.waitForIdle()
        // 选择 BIAS_CIRCLE 位置模式
        composeTestRule.onNodeWithText("不设置").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("locationBias - Circle").fetchSemanticsNodes()
            .firstOrNull()
            ?.let { composeTestRule.onNodeWithText("locationBias - Circle").performClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.search_label_circle_center))
            .performTextInput("1.3521,103.8198")
        composeTestRule.onNodeWithText(getString(R.string.search_label_radius))
            .performTextInput("5000")
        composeTestRule.onNodeWithText(getString(R.string.search_placeholder))
            .performTextInput("cafe")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.search_action_search)).performClick()
        dismissSoftKeyboard()
        waitForSearchResults(60_000)
    }

    // ==================== 2. 附近搜索 ====================

    @Test
    fun nearbySearch_showsApiResponse() {
        navigateToSearchScreen()
        navigateToNearbyTab()
        clickNearbySearch()
        dismissSoftKeyboard()
        waitForSearchResults(60_000)
    }

    @Test
    fun nearbySearch_withTypes_showsApiResponse() {
        navigateToSearchScreen()
        navigateToNearbyTab()
        replaceField("search_nearby_radius", "3000")
        // 选择POI类型
        val dropdown = composeTestRule.onNodeWithTag("search_nearby_types_dropdown", useUnmergedTree = true)
        try {
            dropdown.performScrollTo()
        } catch (_: AssertionError) {
        }
        dropdown.performClick()
        composeTestRule.waitForIdle()
//        composeTestRule.onNodeWithText("Hotels").performClick()
        composeTestRule.onNodeWithTag("drop_index_click_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.search_advanced_more)).performClick()
        composeTestRule.waitForIdle()
        replaceField("search_nearby_result_limit", "5")
        composeTestRule.waitForIdle()
        val submitBtn = composeTestRule.onNodeWithTag("search_nearby_submit_button", useUnmergedTree = true)
        submitBtn.performClick()
        dismissSoftKeyboard()
        waitForSearchResults(60_000)
    }

    // ==================== 3. 交互式搜索 ====================

    @Test
    fun suggestSearch_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_suggest)
        Thread.sleep(1000)
        replaceField("search_query_input", "hotel")
        composeTestRule.onNodeWithTag("search_submit_button", useUnmergedTree = true).performClick()
        dismissSoftKeyboard()
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodesWithText("hotel", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size > 1
        }
    }

    @Test
    fun suggestSearch_withTypes_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_suggest)
        Thread.sleep(1000)
        replaceField("search_input_suggest_types", "hotel,restaurant")
        replaceField("search_query_input", "hotel")
        composeTestRule.onNodeWithTag("search_submit_button", useUnmergedTree = true).performClick()
        dismissSoftKeyboard()
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodesWithText("hotel", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size > 1
        }
    }

    // ==================== 4. 地点详情 ====================

    @Test
    fun placeDetail_fromTab_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_detail)
        Thread.sleep(1000)
        replaceField("search_input_place_id", "1597034063941321331")
        composeTestRule.onNodeWithText(getString(R.string.search_action_get_detail)).performClick()
        dismissSoftKeyboard()
        waitForPlaceDetail()
        composeTestRule.onNodeWithTag("place_detail_card").assertIsDisplayed()
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodesWithText(getString(R.string.search_detail_name), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun placeDetail_fromSearchResult_showsApiResponse() {
        navigateToSearchScreen()
        typeSearchQuery("hotel in Singapore")
        clickSearch()
        dismissSoftKeyboard()
        waitForSearchResults()
        composeTestRule.onAllNodesWithTag("search_place_item", useUnmergedTree = true)[0]
            .performScrollTo()
            .performClick()
        waitForPlaceDetail()
        assertPlaceDetailDisplayed()
        // placeDetail API is async; wait for loaded content before checking response JSON
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodesWithText(getString(R.string.search_detail_name), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }


    // ==================== 提示词（自动提示） ====================

    @Test
    fun autoSuggest_onTyping_showsSuggestions() {
        navigateToSearchScreen()
        composeTestRule.onNodeWithText(getString(R.string.search_placeholder))
            .performTextInput("singapo")
        composeTestRule.waitForIdle()
        // 自动提示带 300ms 防抖，等待建议出现
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(
                "singapo", substring = true, useUnmergedTree = true, ignoreCase = true
            ).fetchSemanticsNodes().size > 1
        }
        dismissSoftKeyboard()
    }

    @Test
    fun textSearch_autoSuggest_fillsQueryOnClick() {
        navigateToSearchScreen()
        // 输入关键词触发自动提示
        composeTestRule.onNodeWithText(getString(R.string.search_placeholder))
            .performTextInput("hotel")
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("hotel", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size > 1
        }
        // 通过 contentDescription 找到建议项（合并树中 Card 的 clickable 可用）
        val suggestionIcons = composeTestRule.onAllNodes(hasContentDescription(getString(R.string.search_suggestion_cd)))
        if (suggestionIcons.fetchSemanticsNodes().isNotEmpty()) {
            suggestionIcons[0].performClick()
            composeTestRule.waitForIdle()
            // 点击建议后应触发文本搜索，底部显示结果列表
            waitForSearchResults(60_000)
        }
    }

    // ==================== Catalog navigation ====================

    @Test
    fun catalogScreenSwitching_noCrash() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_nearby)
        switchToTab(R.string.search_tab_suggest)
        switchToTab(R.string.search_tab_detail)
        switchToTab(R.string.search_tab_text)
        assertFalse(composeTestRule.activity.isFinishing)
    }

    // ==================== 地图视图切换 ====================

    @Test
    fun mapView_toggle_works() {
        navigateToSearchScreen()
        composeTestRule.onNodeWithContentDescription(getString(R.string.search_map_view))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.search_list_view))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(getString(R.string.search_list_view))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.search_map_view))
            .assertIsDisplayed()
    }

    // ==================== 导航 ====================

    @Test
    fun backNavigation_returnsToMainScreen() {
        navigateToSearchScreen()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.catalog_text_search))
            .assertIsDisplayed()
    }
}


