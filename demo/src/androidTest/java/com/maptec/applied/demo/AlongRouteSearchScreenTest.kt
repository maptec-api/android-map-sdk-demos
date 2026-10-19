package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * 沿途搜索 [AlongRouteSearchScreen] UI + API 集成测试。
 *
 * 覆盖面板字段、评估模式、排序、高级参数、结果列表点击与地图切换，
 * 并通过真实 alongRouteSearch 接口校验结果展示。
 */
@RunWith(AndroidJUnit4::class)
class AlongRouteSearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        navigateToAlongRouteSearchScreen()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun getString(resId: Int): String = getTestString(resId)

    private fun navigateToAlongRouteSearchScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openWebServicesDemo(R.string.catalog_along_route_search)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag("search_along_route_query", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun dismissSoftKeyboard() {
        runCatching { Espresso.closeSoftKeyboard() }
        composeTestRule.waitForIdle()
    }

    private fun replaceField(tag: String, value: String) {
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
        runCatching { node.performScrollTo() }
        node.performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clearField(tag: String) {
        val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
        runCatching { node.performScrollTo() }
        node.performTextClearance()
        composeTestRule.waitForIdle()
    }

    private fun clickAlongRouteSearch() {
        val node = composeTestRule.onNodeWithTag(
            "search_along_route_submit_button",
            useUnmergedTree = true,
        )
        runCatching { node.performScrollTo() }
        node.assertIsEnabled()
        node.performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForAlongRouteResults(timeoutMs: Long = 60_000) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("search_along_route_item", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun expandAdvancedParams() {
        val alreadyExpanded = composeTestRule.onAllNodesWithTag(
            "search_along_route_result_limit",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()
        if (!alreadyExpanded) {
            val toggle = composeTestRule.onNodeWithTag(
                "search_advanced_toggle",
                useUnmergedTree = true,
            )
            runCatching { toggle.performScrollTo() }
            toggle.performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithTag(
                "search_along_route_result_limit",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun selectEvaluationMode(modeName: String) {
        dismissSoftKeyboard()
        val modeField = composeTestRule.onNodeWithTag(
            "search_along_route_mode",
            useUnmergedTree = true,
        )
        runCatching { modeField.performScrollTo() }
        modeField.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        val modeTag = "search_along_route_mode_$modeName"
        val appeared = runCatching {
            composeTestRule.waitUntil(8_000) {
                composeTestRule.onAllNodesWithTag(modeTag, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty() ||
                    composeTestRule.onAllNodesWithText(modeName, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .size >= 1
            }
            true
        }.getOrDefault(false)
        if (!appeared) {
            // 部分机型需再点一次才展开 ExposedDropdownMenu
            modeField.performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(300)
            composeTestRule.waitUntil(8_000) {
                composeTestRule.onAllNodesWithTag(modeTag, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty() ||
                    composeTestRule.onAllNodesWithText(modeName, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .size >= 1
            }
        }
        if (composeTestRule.onAllNodesWithTag(modeTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            composeTestRule.onNodeWithTag(modeTag, useUnmergedTree = true).performClick()
        } else {
            val nodes = composeTestRule.onAllNodesWithText(modeName, useUnmergedTree = true)
                .fetchSemanticsNodes()
            composeTestRule.onAllNodesWithText(modeName, useUnmergedTree = true)[
                (nodes.size - 1).coerceAtLeast(0)
            ].performClick()
        }
        composeTestRule.waitForIdle()
    }

    private fun selectRank(rankTagSuffix: String) {
        expandAdvancedParams()
        val rankField = composeTestRule.onNodeWithTag(
            "search_along_route_rank",
            useUnmergedTree = true,
        )
        runCatching { rankField.performScrollTo() }
        rankField.performClick()
        composeTestRule.waitForIdle()
        val tag = "search_along_route_rank_$rankTagSuffix"
        val label = if (rankTagSuffix == "NONE") {
            getString(R.string.search_not_set)
        } else {
            rankTagSuffix
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeTestRule.onAllNodesWithText(label, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        } else {
            composeTestRule.onAllNodesWithText(label, useUnmergedTree = true)[0].performClick()
        }
        composeTestRule.waitForIdle()
    }

    private fun waitForPlaceDetailSheet(timeoutMs: Long = 60_000) {
        composeTestRule.waitUntil(timeoutMs) {
            composeTestRule.onAllNodesWithTag("place_detail_bottom_sheet", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeTestRule.onAllNodesWithText(
                    getString(R.string.search_detail_title),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    // ==================== 基础 UI ====================

    @Test
    fun alongRouteSearch_panelControls_areDisplayed() {
        composeTestRule.onNodeWithText(getString(R.string.search_along_route_tip))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_query", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_polyline", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_buffer", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_mode", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_extra_fields", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_along_route_submit_button", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun alongRouteSearch_emptyQueryAndTypes_submitDisabled() {
        clearField("search_along_route_query")
        // types 默认空；清空 query 后按钮应禁用
        composeTestRule.onNodeWithTag("search_along_route_submit_button", useUnmergedTree = true)
            .assertIsNotEnabled()
    }

    @Test
    fun alongRouteSearch_invalidBuffer_submitDisabled() {
        replaceField("search_along_route_query", "hotel")
        replaceField("search_along_route_buffer", "50")
        composeTestRule.onNodeWithTag("search_along_route_submit_button", useUnmergedTree = true)
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText(getString(R.string.search_error_range_100_50000))
            .assertIsDisplayed()
    }

    @Test
    fun alongRouteSearch_evaluationModeAndRank_canSelect() {
        // 默认 mode=NONE；展开高级参数后应出现 rank / resultLimit 字段
        val modeField = composeTestRule.onNodeWithTag(
            "search_along_route_mode",
            useUnmergedTree = true,
        )
        runCatching { modeField.performScrollTo() }
        modeField.assertIsDisplayed()
        composeTestRule.onNodeWithText("NONE", useUnmergedTree = true).assertIsDisplayed()

        expandAdvancedParams()
        val resultLimit = composeTestRule.onNodeWithTag(
            "search_along_route_result_limit",
            useUnmergedTree = true,
        )
        runCatching { resultLimit.performScrollTo() }
        resultLimit.assertIsDisplayed()

        val rankField = composeTestRule.onNodeWithTag(
            "search_along_route_rank",
            useUnmergedTree = true,
        )
        runCatching { rankField.performScrollTo() }
        rankField.assertIsDisplayed()

        // 打开 mode 菜单并选择 STANDARD（兼容 tag / 文本两种语义）
        selectEvaluationMode("STANDARD")
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("STANDARD", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        selectRank("ROUTE_DURATION")
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("ROUTE_DURATION", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        replaceField("search_along_route_query", "hotel")
        composeTestRule.onNodeWithTag("search_along_route_submit_button", useUnmergedTree = true)
            .assertIsEnabled()
    }

    // ==================== 搜索 API（经 UI） ====================

    @Test
    fun alongRouteSearch_withQuery_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "hotel")
        replaceField("search_along_route_buffer", "2000")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_withTypes_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        clearField("search_along_route_query")
        val dropdown = composeTestRule.onNodeWithTag(
            "search_nearby_types_dropdown",
            useUnmergedTree = true,
        )
        runCatching { dropdown.performScrollTo() }
        dropdown.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drop_index_click_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_withAdvancedParams_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "restaurant")
        expandAdvancedParams()
        replaceField("search_along_route_result_limit", "5")
        replaceField("search_input_language", "en")
        replaceField("search_input_region", "SG")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
        val count = composeTestRule.onAllNodesWithTag(
            "search_along_route_item",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().size
        assertFalse("resultLimit=5 时结果数不应超过 5", count > 5)
    }

    @Test
    fun alongRouteSearch_standardEvaluationMode_showsApiResults() {
        AlongRouteSearchAuth.assumeStandardEvaluationAvailable()
        replaceField("search_along_route_query", "hotel")
        selectEvaluationMode("STANDARD")
        expandAdvancedParams()
        replaceField("search_along_route_result_limit", "3")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_relevanceRank_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "cafe")
        selectRank("RELEVANCE")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_routeOrderRank_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "hotel")
        selectRank("ROUTE_ORDER")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_distanceFromRouteRank_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "restaurant")
        selectRank("DISTANCE_FROM_ROUTE")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_standardMode_routeDurationRank_showsApiResults() {
        AlongRouteSearchAuth.assumeStandardEvaluationAvailable()
        replaceField("search_along_route_query", "hotel")
        selectEvaluationMode("STANDARD")
        selectRank("ROUTE_DURATION")
        replaceField("search_along_route_result_limit", "3")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    @Test
    fun alongRouteSearch_withExtraFields_showsApiResults() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "hotel")
        replaceField(
            "search_along_route_extra_fields",
            "openingHours,phoneNumbers,aoi",
        )
        expandAdvancedParams()
        replaceField("search_along_route_result_limit", "3")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()
    }

    // ==================== 结果交互 ====================

    @Test
    fun alongRouteSearch_resultClick_opensMapPlaceSheet() {
        AlongRouteSearchAuth.assumeAlongRouteAuthorized()
        replaceField("search_along_route_query", "hotel")
        expandAdvancedParams()
        replaceField("search_along_route_result_limit", "3")
        clickAlongRouteSearch()
        dismissSoftKeyboard()
        waitForAlongRouteResults()

        composeTestRule.onAllNodesWithTag("search_along_route_item", useUnmergedTree = true)[0]
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodes(
                hasContentDescription(getString(R.string.search_list_view)),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        waitForPlaceDetailSheet()
        composeTestRule.waitUntil(60_000) {
            composeTestRule.onAllNodesWithText(
                getString(R.string.search_detail_name),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun alongRouteSearch_mapViewToggle_works() {
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

    @Test
    fun alongRouteSearch_backNavigation_returnsToCatalog() {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.catalog_along_route_search))
            .assertIsDisplayed()
    }
}
