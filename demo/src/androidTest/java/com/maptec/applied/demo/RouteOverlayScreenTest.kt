package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.route.RouteService
import com.maptec.applied.route.data.STRATEGY_SHORTEST
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteOverlayScreenTest {

    companion object {
        private const val TAG_ORIGIN_INPUT = "origin_input"
        private const val TAG_DESTINATION_INPUT = "destination_input"
        private const val TAG_STRATEGY_DROPDOWN = "strategy_dropdown_trigger"
        private const val TAG_WAYPOINT_LAT = "waypoint_lat_input"
        private const val TAG_WAYPOINT_LNG = "waypoint_lng_input"
        private const val TAG_WAYPOINT_ADD = "waypoint_add_button"
        private const val TAG_AVOID_TOLLS = "avoid_tolls_item"
        private const val TAG_START_ROUTE = "start_route_button"
        private const val TAG_ROUTE_SUMMARY = "route_summary_card"
        private const val TAG_NAV_LINES = "route_overlay_has_navigation_lines"
    }

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
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

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToRouteScreen() {
        composeTestRule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        RouteService.getInstance(
            context = context,
            apiKey = context.getString(R.string.maptec_apiKey),
            signatureSha1 = context.getString(R.string.signature_sha1)
        )
        composeTestRule.openWebServicesDemo(R.string.screen_item_route)
        composeTestRule.waitForMapDemoReady()
    }

    // ==================== 元素辅助 ====================

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
    }

    private fun replaceField(tag: String, value: String) {
        scrollTo(tag)
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickStartRoute() {
        scrollTo(TAG_START_ROUTE)
        composeTestRule.onNodeWithTag(TAG_START_ROUTE, useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForRouteSummary(timeoutMs: Long = 30000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            composeTestRule.onAllNodesWithTag(TAG_ROUTE_SUMMARY, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ==================== 测试用例 ====================

    @Test
    fun defaultRoute_returnsValidDistanceAndDuration() {
        navigateToRouteScreen()
        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routeWithWaypoints_returnsValidSummary() {
        navigateToRouteScreen()

        replaceField(TAG_ORIGIN_INPUT, "103.8630,1.2820")
        replaceField(TAG_DESTINATION_INPUT, "103.8920,1.3920")

        replaceField(TAG_WAYPOINT_LAT, "1.3500")
        replaceField(TAG_WAYPOINT_LNG, "103.8300")
        scrollTo(TAG_WAYPOINT_ADD)
        composeTestRule.onNodeWithTag(TAG_WAYPOINT_ADD).performClick()
        composeTestRule.waitForIdle()

        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routeWithAvoidTolls_returnsValidSummary() {
        navigateToRouteScreen()

        scrollTo(TAG_AVOID_TOLLS)
        composeTestRule.onNodeWithTag(TAG_AVOID_TOLLS).performClick()
        composeTestRule.waitForIdle()

        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routeShortestStrategy_returnsValidSummary() {
        navigateToRouteScreen()

        scrollTo(TAG_STRATEGY_DROPDOWN)
        composeTestRule.onNodeWithTag(TAG_STRATEGY_DROPDOWN).performClick()
        composeTestRule.waitForIdle()

        val strategyItemTag = "strategy_item_$STRATEGY_SHORTEST"
        scrollTo(strategyItemTag)
        composeTestRule.onNodeWithTag(strategyItemTag).performClick()
        composeTestRule.waitForIdle()

        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun invalidDestination_doesNotShowSummary() {
        navigateToRouteScreen()

        replaceField(TAG_DESTINATION_INPUT, "invalid")

        clickStartRoute()

        Thread.sleep(5000)

        val summaryExists = composeTestRule
            .onAllNodesWithTag(TAG_ROUTE_SUMMARY, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        assert(!summaryExists) { "无效坐标算路不应展示概要卡片" }
    }
}
