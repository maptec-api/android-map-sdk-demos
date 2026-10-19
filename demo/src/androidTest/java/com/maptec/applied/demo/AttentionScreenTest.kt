package com.maptec.applied.demo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.ext.clickClickableText
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapDemoReady
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AttentionScreen 鉴权配置页面集成测试
 *
 * 覆盖范围：
 * - 页面核心元素展示
 * - API Key / SHA1 输入交互
 * - 确认按钮点击
 * - 查看已授权服务
 * - 导航至地图/搜索/路径规划
 * - 使用 maptec.xml 中的真实 key/sha1 后导航至地图并等待渲染
 *
 * 不使用 [androidx.test.rule.GrantPermissionRule]：本页仅需 INTERNET 等安装时权限，
 * GrantPermissionRule 会抢连 UiAutomation，易在套件中触发 already registered。
 */
@RunWith(AndroidJUnit4::class)
class AttentionScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToAttentionScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.clickClickableText(R.string.screen_item_attention)
    }

    @Test
    fun allCoreElements_areDisplayed() {
        navigateToAttentionScreen()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_api_key_label)).assertExists()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_sha1_label)).assertExists()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_api_key)).assertExists()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_sha1)).assertExists()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_view_authorized_services))
            .performScrollTo()
            .assertExists()
    }

    // ==================== 查看已授权服务 ====================

    @Test
    fun viewAuthorizedServices_showsJsonCard() {
        navigateToAttentionScreen()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_view_authorized_services))
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(
            condition = {
                composeTestRule.onAllNodesWithTag("authStatusJson").fetchSemanticsNodes().isNotEmpty()
            },
            timeoutMillis = 30000
        )
    }

    // ==================== 导航按钮 ====================

    // ==================== 真实 Key/SHA1 集成测试 ====================

    @Test
    fun applyMaptecKeyAndSha1_thenNavigateToMap_andVerifyRender() {
        navigateToAttentionScreen()
        val apiKey = getTestString(R.string.maptec_apiKey)
        val sha1 = getTestString(R.string.signature_sha1)

        composeTestRule.onNodeWithText(getTestString(R.string.attention_api_key_hint), substring = true)
            .performTextInput(apiKey)
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_api_key)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_sha1_hint), substring = true)
            .performScrollTo()
            .performTextInput(sha1)
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_sha1))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_verify_map))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitForMapDemoReady()
    }
}
