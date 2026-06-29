package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteOverlayScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        navigateToRouteOverlayScreen()
        composeTestRule.waitForMapRendered()
        composeTestRule.waitForIdle()
    }

    private fun navigateToRouteOverlayScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_route)).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testNavigationAndMapLoads() {
        composeTestRule.onNodeWithTag("route_config_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mapView").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_route_button").assertIsDisplayed()
    }

    @Test
    fun testRouteCalculation_showsSummaryAndNavigationLine() {
        composeTestRule.onNodeWithTag("start_route_button").performScrollTo().performClick()

        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("route_summary_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("km", substring = true).assertExists()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("route_overlay_has_navigation_lines")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_overlay_has_navigation_lines").assertIsDisplayed()
    }
}
