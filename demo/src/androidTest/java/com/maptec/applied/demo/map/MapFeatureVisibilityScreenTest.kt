package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 地图要素显隐功能测试（MapFeatureVisibilityScreen）。
 *
 * 覆盖：
 * 1. 默认状态下所有底图要素类别（POI、道路、建筑、行政区名称、自然地貌）均默认勾选开启。
 * 2. 点击切换单个要素类别复选框可正确切换 UI 状态。
 * 3. 多个要素类别复选框可以独立切换，互不影响。
 *
 * 参考 Google Android 测试最佳实践与阿里巴巴 Android 编码/测试规范编写。
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered 冲突。
 */
@RunWith(AndroidJUnit4::class)
class MapFeatureVisibilityScreenTest {

    companion object {
        private const val TAG_CHECKBOX_POI = "checkbox_feature_poi"
        private const val TAG_CHECKBOX_ROAD = "checkbox_feature_road"
        private const val TAG_CHECKBOX_BUILDING = "checkbox_feature_building"
        private const val TAG_CHECKBOX_ADMINISTRATIVE = "checkbox_feature_administrative"
        private const val TAG_CHECKBOX_NATURAL = "checkbox_feature_natural_features"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToMapFeatureVisibilityScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateToMapFeatureVisibilityScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openMapsDemo(R.string.map_item_map_feature_visibility)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testDefaultFeatureVisibility_allCategoriesEnabled() {
        listOf(
            TAG_CHECKBOX_POI,
            TAG_CHECKBOX_ROAD,
            TAG_CHECKBOX_BUILDING,
            TAG_CHECKBOX_ADMINISTRATIVE,
            TAG_CHECKBOX_NATURAL,
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsOn()
        }
    }

    @Test
    fun testToggleFeatureCategory_togglesUiState() {
        // 1. 滚动到 POI 复选框并确认初始开启
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI)
            .performScrollTo()
            .assertIsOn()

        // 2. 点击关闭 POI
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI).assertIsOff()

        // 3. 再次点击开启 POI
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI).assertIsOn()
    }

    @Test
    fun testMultipleFeatureToggles_independent() {
        // 1. 关闭道路和建筑
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_ROAD)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_ROAD).assertIsOff()

        composeTestRule.onNodeWithTag(TAG_CHECKBOX_BUILDING)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_BUILDING).assertIsOff()

        // 2. 验证 POI 仍保持开启
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_POI)
            .performScrollTo()
            .assertIsOn()

        // 3. 恢复道路和建筑
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_ROAD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_ROAD).assertIsOn()

        composeTestRule.onNodeWithTag(TAG_CHECKBOX_BUILDING).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_CHECKBOX_BUILDING).assertIsOn()
    }
}
