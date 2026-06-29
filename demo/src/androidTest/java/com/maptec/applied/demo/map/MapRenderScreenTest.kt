package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MapScreen（地图渲染）功能测试：地图渲染就绪、天空盒子开关 API。
 */
@RunWith(AndroidJUnit4::class)
class MapRenderScreenTest {

    companion object {
        private const val TAG_MAP_VIEW = "mapView"
        private const val TAG_SKY_SWITCH = "map_switch_sky_enabled"
        private const val TAG_DEBUG_TOGGLE = "map_btn_debug_toggle"
        private const val TAG_LANGUAGE_DROPDOWN = "map_language_dropdown"
        private const val TAG_LANGUAGE_OPTION_DEFAULT = "map_language_option_default"
        private const val TAG_LANGUAGE_OPTION_EN = "map_language_option_en"
        private const val TAG_LANGUAGE_OPTION_ZH = "map_language_option_zh"

        private const val LANGUAGE_TAG_DEFAULT = ""
        private const val LANGUAGE_TAG_EN = "en"
        private const val LANGUAGE_TAG_ZH = "zh"
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

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToMapRenderScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
        disableFpsListener()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToMapRenderScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_render)).performClick()
        composeTestRule.waitForIdle()
    }

    /** 移除 FPS 监听，避免持续 State 更新导致 waitForIdle 死锁。 */
    private fun disableFpsListener() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map -> map.setOnFpsChangedListener(null) }
        }
    }

    private fun withMapApi(action: (MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("异步获取 MaptecMap 超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    /**
     * 展开底部 BottomSheet。
     *
     * Why：面板默认折叠到 peek 48dp，里面的控件大多在屏幕外且无可滚动空间，
     * 此时 performScrollTo 是 no-op，节点会被判定为 not displayed。
     * 这里用 Material3 给 sheet 设置的 Expand 无障碍语义动作把面板展开（比 swipe 手势更确定）。
     * 用 onAllNodes 判空保证幂等：已展开时（没有 Expand 动作）直接跳过。
     */
    private fun expandSheet() {
        val expandable = composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
        if (expandable.fetchSemanticsNodes().isNotEmpty()) {
            expandable.onFirst().performSemanticsAction(SemanticsActions.Expand)
            composeTestRule.waitForIdle()
        }
    }

    private fun expandBottomSheet() {
        expandSheet()
        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).performScrollTo()
        composeTestRule.waitForIdle()
    }

    /** 展开面板并滚动到「标签语言」下拉框，确保其在可视区域内。 */
    private fun scrollToLanguageDropdown() {
        expandSheet()
        composeTestRule.onNodeWithTag(TAG_LANGUAGE_DROPDOWN).performScrollTo()
        composeTestRule.waitForIdle()
    }

    /**
     * 展开下拉框并选择指定语言项。
     *
     * Why 暂停时钟：ExposedDropdownMenuBox 在菜单展开期间会持续请求帧（跟踪锚点位置），
     * 自动推进模式下 Compose 测试时钟永不进入 idle，导致 waitForIdle 以及节点交互内部的
     * idle 同步一直阻塞（表现为「一直转圈」）。这里在交互期间关闭 autoAdvance，
     * 手动推进帧来驱动菜单的展开 / 选择 / 收起，菜单收起后再恢复自动推进。
     */
    private fun selectLanguage(optionTag: String) {
        scrollToLanguageDropdown()
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag(TAG_LANGUAGE_DROPDOWN).performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.onNodeWithTag(optionTag).performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    // ==================== 地图渲染 ====================

    @Test
    fun testMapRendered_success() {
        composeTestRule.onNodeWithTag(TAG_MAP_VIEW).assertIsDisplayed()
    }

    // ==================== 天空盒子 ====================

    @Test
    fun testSkySwitch_defaultOff_apiVerified() {
        expandBottomSheet()
        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).assertIsOff()
        withMapApi { map ->
            assertFalse("天空盒默认应关闭", map.isSkyEnabled)
        }
    }

    @Test
    fun testSkySwitch_toggleOn_updatesApi() {
        expandBottomSheet()
        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).assertIsOn()
        withMapApi { map ->
            assertTrue("开启后 isSkyEnabled 应为 true", map.isSkyEnabled)
        }
    }

    @Test
    fun testSkySwitch_toggleOff_updatesApi() {
        expandBottomSheet()
        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SKY_SWITCH).assertIsOff()
        withMapApi { map ->
            assertFalse("关闭后 isSkyEnabled 应为 false", map.isSkyEnabled)
        }
    }

    // ==================== 标签语言 ====================

    @Test
    fun testLanguageDropdown_defaultIsEngineDefault() {
        scrollToLanguageDropdown()
        composeTestRule.onNodeWithTag(TAG_LANGUAGE_DROPDOWN).assertIsDisplayed()
        withMapApi { map ->
            assertEquals("默认语言应为 zh（系统默认或瓦片默认）", LANGUAGE_TAG_ZH, map.language)
        }
    }

    @Test
    fun testLanguageSelect_english_updatesApi() {
        selectLanguage(TAG_LANGUAGE_OPTION_EN)
        withMapApi { map ->
            assertEquals("切换英语后引擎语言应为 en", LANGUAGE_TAG_EN, map.language)
        }
    }

    @Test
    fun testLanguageSelect_chinese_updatesApi() {
        selectLanguage(TAG_LANGUAGE_OPTION_ZH)
        withMapApi { map ->
            assertEquals("切换中文后引擎语言应为 zh", LANGUAGE_TAG_ZH, map.language)
        }
    }

    @Test
    fun testLanguageSelect_backToDefault_updatesApi() {
        selectLanguage(TAG_LANGUAGE_OPTION_EN)
        withMapApi { map ->
            assertEquals("切换英语后引擎语言应为 en", LANGUAGE_TAG_EN, map.language)
        }

        selectLanguage(TAG_LANGUAGE_OPTION_DEFAULT)
        withMapApi { map ->
            assertEquals("切回默认后引擎语言应为空标签（引擎默认）", LANGUAGE_TAG_DEFAULT, map.language)
        }
    }

    // ==================== 调试模式 ====================

    @Test
    fun testDebugToggle_changesApi() {
        var initial = false
        withMapApi { initial = it.isDebugActive }

        composeTestRule.onNodeWithTag(TAG_DEBUG_TOGGLE).performClick()
        composeTestRule.waitForIdle()

        withMapApi { map ->
            assertEquals("调试模式应切换", !initial, map.isDebugActive)
        }
    }
}
