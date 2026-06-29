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
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * AttentionScreen 鉴权配置页面集成测试
 *
 * 覆盖范围：
 * - 页面核心元素展示
 * - API Key / SHA1 输入交互
 * - SDK 复选框勾选状态切换
 * - 确认按钮点击（含空输入、无勾选等边界）
 * - 查看已授权服务
 * - 导航至地图/搜索/路径规划
 * - 使用 maptec.xml 中的真实 key/sha1 后导航至地图并等待渲染
 */
@RunWith(AndroidJUnit4::class)
class AttentionScreenTest {

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
    val ruleChain: TestRule = RuleChain
        .outerRule(permissionRule)
        .around(composeTestRule)

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToAttentionScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_attention)).performClick()
        composeTestRule.waitForIdle()
    }

    // ==================== 页面元素验证 ====================

    @Test
    fun allCoreElements_areDisplayed() {
        navigateToAttentionScreen()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_api_key_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_api_key_hint)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_api_key)).assertIsDisplayed()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_sha1_label)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_sha1_hint)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_confirm_sha1)).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_sdk_checkbox_title)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_sdk_search)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_sdk_route)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_sdk_map)).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_verify_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_verify_map)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_verify_search)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(getTestString(R.string.attention_verify_route)).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText(getTestString(R.string.attention_view_authorized_services)).performScrollTo().assertIsDisplayed()
    }

    // ==================== 输入交互 ====================

    // ==================== 确认按钮边界测试 ====================

    // ==================== SDK 复选框交互 ====================

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

        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_render))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitForMapRendered()
    }
}
