package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertEquals
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
 * MapRenderScreen（添加地图）功能测试：地图渲染就绪、标签语言切换 API。
 */
@RunWith(AndroidJUnit4::class)
class MapRenderScreenTest {

    companion object {
        private const val TAG_MAP_VIEW = "mapView"
        private const val TAG_LANGUAGE_DROPDOWN = "map_language_dropdown"
        private const val TAG_LANGUAGE_OPTION_EN = "map_language_option_en"
        private const val TAG_LANGUAGE_OPTION_ZH = "map_language_option_zh"

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
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToMapRenderScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openMapsDemo(R.string.map_item_map_render)
        composeTestRule.waitForIdle()
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

    private fun scrollToLanguageDropdown() {
        composeTestRule.expandConfigPanel()
        composeTestRule.onNodeWithTag(TAG_LANGUAGE_DROPDOWN).performScrollTo()
        composeTestRule.waitForIdle()
    }

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

    @Test
    fun testMapRendered_success() {
        composeTestRule.onNodeWithTag(TAG_MAP_VIEW).assertIsDisplayed()
    }

    @Test
    fun testLanguageDropdown_defaultMatchesLocale() {
        scrollToLanguageDropdown()
        composeTestRule.onNodeWithTag(TAG_LANGUAGE_DROPDOWN).assertIsDisplayed()
        val expected = when (
            InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
                .locales[0].language
        ) {
            "zh" -> LANGUAGE_TAG_ZH
            else -> LANGUAGE_TAG_EN
        }
        withMapApi { map ->
            assertEquals("默认语言应与系统语言一致", expected, map.language)
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
}
