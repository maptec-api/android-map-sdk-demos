package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
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
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [MapTrafficScreen] 路况展示功能测试。
 *
 * 覆盖：默认开路况、开关同步 [MaptecMap.setTrafficEnabled] / [MaptecMap.isTrafficEnabled]、
 * 浅色/深色样式切换后路况状态保持、路况切换后深浅色保持、当前缩放级别展示。
 *
 * 路况开关内部会切换到底图样式的路况变体，是一次异步样式重载，因此每次点击后
 * 需要等待面板重新可用（[waitForStyleReloaded]）再断言。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class MapTrafficScreenTest {

    companion object {
        private const val TAG_SWITCH_TRAFFIC = "switch_traffic_enabled"
        private const val TAG_SWITCH_MAP_STYLE = "switch_map_style"
        private const val TAG_CURRENT_ZOOM = "text_current_zoom"
        private const val STYLE_TIMEOUT_MS = 20_000L
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToMapTrafficScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToMapTrafficScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openMapsDemo(R.string.map_item_map_traffic)
        composeTestRule.waitForIdle()
    }

    private fun withMaptecMap(action: (MaptecMap) -> Unit) {
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

    private fun waitForStyleLabel(isDark: Boolean) {
        val label = styleLabel(isDark)
        composeTestRule.waitUntil(STYLE_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithTag("mapRendered").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsEnabled()
        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsEnabled()
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    /** 等待路况开关触发的样式重载完成：地图重新渲染且面板恢复可用。 */
    private fun waitForStyleReloaded() {
        composeTestRule.waitUntil(STYLE_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithTag("mapRendered").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsEnabled()
    }

    private fun styleLabel(isDark: Boolean): String {
        val styleName = getTestString(
            if (isDark) R.string.map_style_dark else R.string.map_style_light,
        )
        return getTestString(R.string.map_traffic_map_style_label, styleName)
    }

    @Test
    fun testTrafficSwitch_defaultEnabledAndTogglesApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsDisplayed().assertIsOn()
        withMaptecMap { assertTrue("本页默认应开启路况", it.isTrafficEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performClick()
        waitForStyleReloaded()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOff()
        withMaptecMap { assertEquals(false, it.isTrafficEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performClick()
        waitForStyleReloaded()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOn()
        withMaptecMap { assertEquals(true, it.isTrafficEnabled) }
    }

    @Test
    fun testMapStyleSwitch_preservesTrafficEnabled() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOn()
        withMaptecMap { assertTrue(it.isTrafficEnabled) }
        composeTestRule.onNodeWithText(styleLabel(isDark = false)).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().performClick()
        waitForStyleLabel(isDark = true)

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOn()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOn()
        withMaptecMap { assertTrue("切换深色样式后路况应保持开启", it.isTrafficEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performClick()
        waitForStyleLabel(isDark = false)

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOff()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOn()
        withMaptecMap { assertTrue("切回浅色样式后路况应保持开启", it.isTrafficEnabled) }
    }

    @Test
    fun testMapStyleSwitch_preservesTrafficDisabled() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performClick()
        waitForStyleReloaded()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOff()
        withMaptecMap { assertEquals(false, it.isTrafficEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performClick()
        waitForStyleLabel(isDark = true)

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOn()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOff()
        withMaptecMap { assertEquals(false, it.isTrafficEnabled) }
    }

    /**
     * 路况开关内部走的是样式变体切换，必须验证它不会把当前的深浅色底图带跑。
     */
    @Test
    fun testTrafficSwitch_inDarkStyle_keepsDarkStyle() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().performClick()
        waitForStyleLabel(isDark = true)

        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performClick()
        waitForStyleReloaded()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOff()
        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOn()
        composeTestRule.onNodeWithText(styleLabel(isDark = true)).assertIsDisplayed()
        withMaptecMap { assertEquals(false, it.isTrafficEnabled) }

        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).performClick()
        waitForStyleReloaded()
        composeTestRule.onNodeWithTag(TAG_SWITCH_TRAFFIC).assertIsOn()
        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOn()
        composeTestRule.onNodeWithText(styleLabel(isDark = true)).assertIsDisplayed()
        withMaptecMap { assertTrue("深色底图下重新开启路况后应仍为深色", it.isTrafficEnabled) }
    }

    @Test
    fun testCurrentZoom_displaysInitialValue() {
        composeTestRule.onNodeWithTag(TAG_CURRENT_ZOOM).performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("13.00")

        var apiZoom = -1.0
        withMaptecMap { apiZoom = it.cameraPosition.zoom }
        assertTrue("初始缩放应接近 13", apiZoom in 12.5..13.5)
    }
}
