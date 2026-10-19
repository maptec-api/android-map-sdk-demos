package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.existsSafely
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.pollUntil
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.route.RouteService
import com.maptec.applied.route.data.STRATEGY_SHORTEST
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered。
 */
@RunWith(AndroidJUnit4::class)
class RouteOverlayScreenTest {

    companion object {
        private const val TAG_ORIGIN_INPUT = "origin_input"
        private const val TAG_DESTINATION_INPUT = "destination_input"
        private const val TAG_STRATEGY_DROPDOWN = "strategy_dropdown_trigger"
        private const val TAG_WAYPOINT_ADD = "waypoint_add_button"
        private const val TAG_WAYPOINTS_LIST = "waypoints_list_card"
        private const val TAG_AVOID_HIGHWAYS = "avoid_highways_item"
        private const val TAG_ALTERNATIVES = "alternatives_switch"
        private const val TAG_WAYPOINT_CLEAR = "waypoint_clear_button"
        private const val TAG_START_ROUTE = "start_route_button"
        private const val TAG_ROUTE_SUMMARY = "route_summary_card"
        private const val TAG_NAV_LINES = "route_overlay_has_navigation_lines"

        // 与 RouteServiceApiTest 常规驾车用例对齐的已知可算路 OD（lng,lat）。
        private const val KNOWN_GOOD_ORIGIN = "103.638070,1.326035"
        private const val KNOWN_GOOD_DESTINATION = "103.8920,1.3920"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    // ==================== 导航辅助 ====================

    private fun navigateToRouteScreen() {
        composeTestRule.settleForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        RouteService.getInstance(context)
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
        composeTestRule.settleForIdle()
    }

    private fun clickStartRoute() {
        scrollTo(TAG_START_ROUTE)
        composeTestRule.onNodeWithTag(TAG_START_ROUTE, useUnmergedTree = true).performClick()
        composeTestRule.settleForIdle()
    }

    private fun clearWaypointsIfPresent() {
        val hasClear = composeTestRule
            .onAllNodesWithTag(TAG_WAYPOINT_CLEAR, useUnmergedTree = true)
            .existsSafely()
        if (hasClear) {
            scrollTo(TAG_WAYPOINT_CLEAR)
            composeTestRule.onNodeWithTag(TAG_WAYPOINT_CLEAR, useUnmergedTree = true).performClick()
            composeTestRule.settleForIdle()
        }
    }

    private fun setAlternatives(enabled: Boolean) {
        scrollTo(TAG_ALTERNATIVES)
        val switch = composeTestRule.onNodeWithTag(TAG_ALTERNATIVES, useUnmergedTree = true)
        if (enabled) {
            try {
                switch.assertIsOn()
            } catch (_: AssertionError) {
                switch.performClick()
                composeTestRule.settleForIdle()
                composeTestRule.onNodeWithTag(TAG_ALTERNATIVES, useUnmergedTree = true).assertIsOn()
            }
        } else {
            try {
                switch.assertIsOff()
            } catch (_: AssertionError) {
                switch.performClick()
                composeTestRule.settleForIdle()
                composeTestRule.onNodeWithTag(TAG_ALTERNATIVES, useUnmergedTree = true).assertIsOff()
            }
        }
    }

    private fun enableAvoidHighways() {
        scrollTo(TAG_AVOID_HIGHWAYS)
        val item = composeTestRule.onNodeWithTag(TAG_AVOID_HIGHWAYS, useUnmergedTree = true)
        try {
            item.assertIsOn()
        } catch (_: AssertionError) {
            item.performClick()
            composeTestRule.settleForIdle()
            composeTestRule.onNodeWithTag(TAG_AVOID_HIGHWAYS, useUnmergedTree = true).assertIsOn()
        }
    }

    private fun waitForRouteSummary(timeoutMs: Long = 30000) {
        // 地图页几乎到不了 Compose idle，waitUntil 会先卡在 idle 上把进程拖死。
        // fetchSemanticsNodes 也会先等 idle；默认超时太长，30s 窗口里可能只问到一次。
        withShortComposeIdleTimeout {
            composeTestRule.pollUntil(timeoutMillis = timeoutMs) {
                composeTestRule.onAllNodesWithTag(TAG_ROUTE_SUMMARY, useUnmergedTree = true)
                    .existsSafely()
            }
        }
    }

    /**
     * 地图页 [androidx.compose.ui.test.SemanticsNodeInteractionCollection.fetchSemanticsNodes]
     * 会先等 idle；默认超时很长。断言期间压短，busy 时快速重试。
     */
    private inline fun withShortComposeIdleTimeout(block: () -> Unit) {
        val master = IdlingPolicies.getMasterIdlingPolicy()
        val resource = IdlingPolicies.getDynamicIdlingResourceErrorPolicy()
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.SECONDS)
        try {
            block()
        } finally {
            IdlingPolicies.setMasterPolicyTimeout(master.idleTimeout, master.idleTimeoutUnit)
            IdlingPolicies.setIdlingResourceTimeout(resource.idleTimeout, resource.idleTimeoutUnit)
        }
    }

    // ==================== 测试用例 ====================

    @Test
    fun defaultRoute_returnsValidDistanceAndDuration() {
        navigateToRouteScreen()
        replaceField(TAG_ORIGIN_INPUT, KNOWN_GOOD_ORIGIN)
        replaceField(TAG_DESTINATION_INPUT, KNOWN_GOOD_DESTINATION)
        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routeWithWaypoints_returnsValidSummary() {
        navigateToRouteScreen()
        // 与 RouteServiceApiTest 带途经点用例对齐：已知可算路 OD + 预填途经点。
        replaceField(TAG_ORIGIN_INPUT, KNOWN_GOOD_ORIGIN)
        replaceField(TAG_DESTINATION_INPUT, KNOWN_GOOD_DESTINATION)

        // 输入框已预填可用途经点示例，直接点添加。
        scrollTo(TAG_WAYPOINT_ADD)
        composeTestRule.onNodeWithTag(TAG_WAYPOINT_ADD, useUnmergedTree = true).performClick()
        composeTestRule.settleForIdle()
        composeTestRule.onNodeWithTag(TAG_WAYPOINTS_LIST, useUnmergedTree = true).assertIsDisplayed()

        clickStartRoute()
        waitForRouteSummary()

        composeTestRule.onNodeWithText("km", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_NAV_LINES, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routeWithAvoidHighways_returnsValidSummary() {
        navigateToRouteScreen()

        // 与 RouteServiceApiTest 避让高速用例对齐：无途经点、关闭备选，避免请求条件干扰结果。
        clearWaypointsIfPresent()
        setAlternatives(enabled = false)
        enableAvoidHighways()

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
        composeTestRule.settleForIdle()

        val strategyItemTag = "strategy_item_$STRATEGY_SHORTEST"
        scrollTo(strategyItemTag)
        composeTestRule.onNodeWithTag(strategyItemTag).performClick()
        composeTestRule.settleForIdle()

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

        Thread.sleep(1000)

        val summaryExists = composeTestRule
            .onAllNodesWithTag(TAG_ROUTE_SUMMARY, useUnmergedTree = true)
            .existsSafely()
        assert(!summaryExists) { "无效坐标算路不应展示概要卡片" }
    }
}
