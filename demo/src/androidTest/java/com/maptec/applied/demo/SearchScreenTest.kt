package com.maptec.applied.demo

import android.util.Log
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.waitForApiResponseKey
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.demo.ui.screens.searchScreenViewModel
import com.maptec.applied.demo.viewmodel.SearchViewModel.SearchTab
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SearchScreen API 集成测试：通过 UI 交互触发搜索 API 调用，
 * 利用 ApiResponseDebugCard 中的 JSON 响应进行 API 功能验证。
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun getString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    companion object {
        private const val TAG = "SearchScreenTest"
    }

    /** 打出当前语义树中所有关心的 testTag，用于在平板上诊断渲染差异 */
    private fun logTestTags(context: String) {
        val targetTags = listOf(
            "search_tab_text", "search_tab_nearby", "search_tab_suggest", "search_tab_detail",
            "search_query_input", "search_submit_button",
            "search_nearby_radius", "search_nearby_submit_button", "search_nearby_types_dropdown",
            "search_input_place_id", "search_input_suggest_types",
            "api_response_card", "place_detail_card"
        )
        val found = targetTags.filter { tag ->
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val missing = targetTags - found
        Log.e(TAG, "[$context] Found: $found")
        Log.e(TAG, "[$context] Missing: $missing")
        // 诊断当前哪个 tab 被选中
        val tabNames = listOf("text", "nearby", "suggest", "detail")
        val selectedTabs = tabNames.filter { name ->
            composeTestRule.onAllNodes(
                hasTestTag("search_tab_$name") and isSelected(),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        Log.e(TAG, "[$context] SelectedTab: $selectedTabs")
    }

    /** 从主屏进入搜索服务页面 */
    private fun navigateToSearchScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.screen_item_unified_search))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("search_tab_text", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        logTestTags("after_navigateToSearchScreen")
        Thread.sleep(2000)
    }

    private fun switchToTab(labelResId: Int) {
        val tab = when (labelResId) {
            R.string.search_tab_text -> SearchTab.TEXT
            R.string.search_tab_nearby -> SearchTab.NEARBY
            R.string.search_tab_suggest -> SearchTab.SUGGEST
            R.string.search_tab_detail -> SearchTab.DETAIL
            else -> error("Unknown tab label res $labelResId")
        }
        // 直接通过 ViewModel 切换 tab
        // Material3.Tab.onClick 在平板上不被 performClick() 触发 (Compose issue)
        searchScreenViewModel?.switchTab(tab)
        composeTestRule.waitForIdle()
        logTestTags("switchToTab_${getString(labelResId)}")
    }

    /** 等待 API 响应 JSON 出现在 ApiResponseDebugCard 中 */
    private fun waitForApiResponse(timeoutMs: Long = 60_000) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("api_response_card", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
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
    }

    private fun navigateToNearbyTab() {
        switchToTab(R.string.search_tab_nearby)
        // 诊断：刚切换完 tab 后检查语义树
        logTestTags("after_switchTo_nearby")
        // 等待 nearby tab 的标签订阅指示可见，而非直接等内部内容
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("search_tab_nearby", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        logTestTags("before_wait_nearbyContent")
        // 平板/大屏上可能需要更长时间完成 compositon
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
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
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
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
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
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
    }

    // ==================== 2. 附近搜索 ====================

    @Test
    fun nearbySearch_showsApiResponse() {
        navigateToSearchScreen()
        navigateToNearbyTab()
        clickNearbySearch()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
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
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
    }

    // ==================== 3. 交互式搜索 ====================

    @Test
    fun suggestSearch_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_suggest)
        Thread.sleep(1000)
        replaceField("search_query_input", "hotel")
        composeTestRule.onNodeWithTag("search_submit_button", useUnmergedTree = true).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
          composeTestRule.waitForApiResponseKey("status")
          composeTestRule.waitForApiResponseKey("OK")
          composeTestRule.waitForApiResponseKey("suggestions")
    }

    @Test
    fun suggestSearch_withTypes_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_suggest)
        Thread.sleep(1000)
        replaceField("search_input_suggest_types", "hotel,restaurant")
        replaceField("search_query_input", "hotel")
        composeTestRule.onNodeWithTag("search_submit_button", useUnmergedTree = true).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("suggestions")
    }

    // ==================== 4. 地点详情 ====================

    @Test
    fun placeDetail_fromTab_showsApiResponse() {
        navigateToSearchScreen()
        switchToTab(R.string.search_tab_detail)
        Thread.sleep(1000)
        replaceField("search_input_place_id", "1597034063941321331")
        composeTestRule.onNodeWithText(getString(R.string.search_action_get_detail)).performClick()
        waitForApiResponse()
        composeTestRule.onNodeWithTag("api_response_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("places")
         composeTestRule.waitForApiResponseKey("displayName")
    }

    @Test
    fun placeDetail_fromSearchResult_showsApiResponse() {
        navigateToSearchScreen()
        typeSearchQuery("hotel in Singapore")
        clickSearch()
        waitForApiResponse()
        waitForSearchResults()
        composeTestRule.onAllNodesWithTag("search_place_item", useUnmergedTree = true)[0]
            .performScrollTo()
            .performClick()
        waitForPlaceDetail()
        composeTestRule.onNodeWithTag("place_detail_card").assertIsDisplayed()
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("placeId")
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
         composeTestRule.waitForApiResponseKey("status")
         composeTestRule.waitForApiResponseKey("OK")
         composeTestRule.waitForApiResponseKey("suggestions")
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
        val suggestionIcons = composeTestRule.onAllNodes(hasContentDescription("提示词"))
        if (suggestionIcons.fetchSemanticsNodes().isNotEmpty()) {
            suggestionIcons[0].performClick()
            composeTestRule.waitForIdle()
            // 点击建议后应触发文本搜索
            waitForApiResponse(20000)
             composeTestRule.waitForApiResponseKey("status")
             composeTestRule.waitForApiResponseKey("OK")
        }
    }

    // ==================== Tab 切换 ====================

    @Test
    fun tabSwitching_noCrash() {
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
        composeTestRule.onNodeWithText(getString(R.string.screen_item_unified_search))
            .assertIsDisplayed()
    }
}


