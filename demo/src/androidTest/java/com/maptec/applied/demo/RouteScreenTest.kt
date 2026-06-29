package com.maptec.applied.demo

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
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.MapView
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RouteScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        navigateToRouteScreen()
        composeTestRule.waitForMapRendered()
        composeTestRule.waitForIdle()
    }

    private fun navigateToRouteScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_route)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun withMapApi(action: (MapView) -> Unit) {
        val mapView = composeTestRule.getMapView()
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(mapView)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("异步获取 MapView API 超时", latch.await(10, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // ==========================================
    // 基础 UI 加载与状态切换测试
    // ==========================================

    @Test
    fun testNavigationAndMapLoads() {
        composeTestRule.onNodeWithTag("route_config_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mapView").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_route_button").assertIsDisplayed()

        withMapApi { mapView ->
            assertTrue("MapView 应可见且已附加到 Window", mapView.isAttachedToWindow)
        }
    }

    @Test
    fun testCustomAreaToggle() {
        composeTestRule.waitForMapRendered()
        composeTestRule.onNodeWithTag("avoid_custom_item", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("custom_area_input", useUnmergedTree = true).assertIsDisplayed()
    }

    // ==========================================
    // 防御性校验与边界异常测试
    // ==========================================
    @Test
    fun testWaypointsInput_invalidFormat_blocksRouting() {
        composeTestRule.waitForMapRendered()
        composeTestRule.onNodeWithTag("waypoints_input", useUnmergedTree = true).performScrollTo()

        // 注入错误的格式（缺少括号的非法坐标）
        val invalidWaypoints = "1.3600, 103.8200"
        composeTestRule.onNodeWithTag("waypoints_input", useUnmergedTree = true).performTextReplacement(invalidWaypoints)
        composeTestRule.waitForIdle()

        // 点击算路
        composeTestRule.onNodeWithTag("start_route_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 验证由于源头拦截生效，UI 不应渲染出路线汇总卡片
        composeTestRule.onNodeWithTag("route_summary_card").assertDoesNotExist()
    }

    // ==========================================
    // 核心业务流程测试
    // ==========================================

    @Test
    fun testRouteCalculation_verifyApiResult() {
        composeTestRule.waitForMapRendered()

        // 确保输入框内是默认的合法状态，直接发起请求
        composeTestRule.onNodeWithTag("start_route_button").performScrollTo().performClick()

        // 等待异步网络请求返回并渲染 UI（超时设为严格的 15 秒）
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("route_summary_card").fetchSemanticsNodes().isNotEmpty()
        }

        // 验证结果卡片可见
        composeTestRule.onNodeWithTag("route_summary_card").assertIsDisplayed()

        // 在真实的 E2E 环境下，由于路线距离和时间可能微调，我们断言必定包含关键的单位标识符
        composeTestRule.onNodeWithText("km", substring = true).assertExists()
        composeTestRule.onNodeWithText("min", substring = true).assertExists()
    }
}