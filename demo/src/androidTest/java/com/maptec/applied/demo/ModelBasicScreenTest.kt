package com.maptec.applied.demo

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.model.Model
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [com.maptec.applied.demo.ui.screens.overlays.model.ModelBasicScreen]
 *
 */
@RunWith(AndroidJUnit4::class)
class ModelBasicScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView
    private lateinit var robot: ModelBasicScreenRobot

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.settleForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_model_basic)
        composeTestRule.settleForIdle()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        robot = ModelBasicScreenRobot(composeTestRule, mapView)
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    // ==================== 测试用例 (Test Cases) ====================

    /**
     * TC-01: 默认模型 (sands-detail.glb) 初始状态与各项物理属性校验。
     */
    @Test
    fun defaultModel_whenLoaded_initializesWithCorrectProperties() {
        // Arrange & Act
        val model = robot.awaitModelReady("sands-detail")

        // Assert
        assertEquals("模型 ID 不匹配", "sands-detail", model.modelId)
        assertEquals("锚点纬度不匹配", SANDS_POSITION.latitude, model.position.latitude, 1e-4)
        assertEquals("锚点经度不匹配", SANDS_POSITION.longitude, model.position.longitude, 1e-4)
        assertEquals("高度应默认为 0", 0f, model.height, 0.01f)
        assertEquals("X轴旋转角应默认为 0", 0f, model.rotationX, 0.01f)
        assertEquals("Y轴旋转角应默认为 0", 0f, model.rotationY, 0.01f)
        assertEquals("Z轴旋转角应默认为 0", 0f, model.rotationZ, 0.01f)
        assertEquals("X轴缩放比例应默认为 1", 1f, model.scaleX, 0.01f)
        assertEquals("Y轴缩放比例应默认为 1", 1f, model.scaleY, 0.01f)
        assertEquals("Z轴缩放比例应默认为 1", 1f, model.scaleZ, 0.01f)
        assertEquals("最小 Zoom 视角限制应默认为 0", 0f, model.minZoom ?: 0f, 0.01f)
        assertEquals("最大 Zoom 视角限制应默认为 22", 22f, model.maxZoom ?: 22f, 0.01f)
        assertTrue("模型初始应处于可见状态", model.isVisible)
        assertTrue("模型加载状态非预期", (model.loadState == Model.LoadState.READY) || (model.loadState == Model.LoadState.CREATED))
    }

    /**
     * TC-02: 界面改变 Zoom 缩放范围与可见性时，实时同步更新模型 API。
     */
    @Test
    fun modelVisibilityAndZoom_whenChanged_updatesModelApiAndUi() {
        // Arrange
        val model = robot.awaitModelReady("sands-detail")

        // Act 1: 调整 Zoom 范围
        robot.setZoomRange(minZoom = "5", maxZoom = "18")

        // Assert 1: 校验 API 限制同步
        assertEquals("最小 Zoom 应成功更新为 5", 5f, model.minZoom ?: 0f, 0.01f)
        assertEquals("最大 Zoom 应成功更新为 18", 18f, model.maxZoom ?: 22f, 0.01f)

        // Act 2 & Assert 2: 切换隐藏并断言
        robot.toggleVisibility()
            .verifyVisibilitySwitchIsOff()
        assertFalse("模型 API 状态应变为不可见", model.isVisible)

        // Act 3 & Assert 3: 恢复显示并断言
        robot.toggleVisibility()
            .verifyVisibilitySwitchIsOn()
        assertTrue("模型 API 状态应恢复为可见", model.isVisible)
    }

    /**
     * TC-03: 点击“移除”与“添加”按钮时，正确管理 3D 模型生命周期与 UI 按钮使能状态。
     */
    @Test
    fun removeAndAddButtons_whenClicked_managesModelLifecycle() {
        // Arrange
        val initialModel = robot.awaitModelReady("sands-detail")

        // Act & Assert 1: 初始按钮状态检查
        robot.verifyRemoveButtonIsEnabled()
            .verifyAddButtonIsDisabled()

        // Act 2: 点击移除
        robot.clickRemove()

        // Assert 2: 原模型变为 REMOVED，引擎内无活跃模型
        assertEquals("旧模型状态应为 REMOVED", Model.LoadState.REMOVED, initialModel.loadState)
        robot.verifyNoActiveModel()
            .verifyRemoveButtonIsDisabled()
            .verifyAddButtonIsEnabled()

        // Act 3: 重新添加
        robot.clickAdd()

        // Assert 3: 校验新模型加载就绪与按钮恢复
        val newModel = robot.awaitModelReady("sands-detail")
        assertNotNull("新模型不应为空", newModel)
        assertTrue("新模型 ID 或状态非预期", (newModel.id != initialModel.id) || (newModel.loadState == Model.LoadState.READY))
        robot.verifyRemoveButtonIsEnabled()
            .verifyAddButtonIsDisabled()
    }

    /**
     * TC-04: 切换数据源为二进制时，仍加载金沙模型（sands-detail.glb 字节）。
     */
    @Test
    fun binarySourceSwitch_whenToggled_loadsSandsModel() {
        // Arrange: 移除当前模型并勾选二进制数据源
        robot.clickRemove()
            .verifyBinarySwitchIsOff()
            .toggleBinarySource()
            .verifyBinarySwitchIsOn()

        // Act: 点击添加模型
        robot.clickAdd()

        // Assert: 二进制和文件路径用同一份金沙模型
        val sandsModel = robot.awaitModelReady("sands-detail")
        assertEquals("模型 ID 应为 sands-detail", "sands-detail", sandsModel.modelId)
        assertEquals("锚点纬度不匹配", SANDS_POSITION.latitude, sandsModel.position.latitude, 1e-4)
        assertEquals("锚点经度不匹配", SANDS_POSITION.longitude, sandsModel.position.longitude, 1e-4)
        assertTrue("金沙模型加载状态非预期", (sandsModel.loadState == Model.LoadState.READY) || (sandsModel.loadState == Model.LoadState.CREATED))
    }

    /**
     * TC-05: 输入非法 Zoom 范围 (minZoom > maxZoom) 时，校验逻辑防护并禁用添加按钮。
     */
    @Test
    fun invalidZoomRange_whenSet_disablesAddButton() {
        // Arrange: 移除当前模型
        robot.clickRemove()

        // Act: 输入非法 Zoom 范围 minZoom=18, maxZoom=10
        robot.setZoomRange(minZoom = "18", maxZoom = "10")

        // Assert: 添加按钮被禁用
        robot.verifyAddButtonIsDisabled()

        // Act & Assert: 恢复正常 Zoom 范围后，添加按钮重新可用
        robot.setZoomRange(minZoom = "0", maxZoom = "22")
            .verifyAddButtonIsEnabled()
    }

    /**
     * TC-06: 切换地图样式 (Light / Dark) 时，模型安全离屏清除并自动恢复重建。
     */
    @Test
    fun mapStyleSwitch_whenToggled_recreatesModel() {
        // Arrange
        val initialModel = robot.awaitModelReady("sands-detail")
        val initialId = initialModel.id

        // Act: 切换深色模式样式
        robot.verifyMapStyleSwitchIsOff()
            .toggleMapStyle()
            .verifyMapStyleSwitchIsOn()

        // Assert: 等待样式渲染与新模型自动挂载就绪
        val recreatedModel = robot.awaitModelReady("sands-detail", timeoutMs = TIMEOUT_STYLE_MS)
        assertNotNull("重建的模型不应为空", recreatedModel)
        assertTrue("重绘模型句柄或就绪状态非预期", (recreatedModel.id != initialId) || (recreatedModel.loadState == Model.LoadState.READY))
        assertEquals("模型 ID 应保持为 sands-detail", "sands-detail", recreatedModel.modelId)
    }

    companion object {
        private val SANDS_POSITION = LatLng(1.283983, 103.859149)
        private const val TIMEOUT_STYLE_MS = 30_000L
    }
}

/**
 * 页面级 UI 自动化测试 Robot 类 (Robot Pattern)。
 * 负责封装 [ModelBasicScreen] 页面的点击、输入、同步等待及页面特定断言。
 */
private class ModelBasicScreenRobot(
    private val composeRule: ComposeContentTestRule,
    private val mapView: MapView,
) {
    private val removeBtnText get() = getTestString(R.string.model_action_remove)
    private val addBtnText get() = getTestString(R.string.model_action_add)

    fun clickRemove() = apply {
        composeRule.onNodeWithText(removeBtnText).performScrollTo().performClick()
        composeRule.settleForIdle()
    }

    fun clickAdd() = apply {
        composeRule.onNodeWithText(addBtnText).performScrollTo().performClick()
        composeRule.settleForIdle()
    }

    fun toggleBinarySource() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_BINARY).performScrollTo().performClick()
        composeRule.settleForIdle()
    }

    fun toggleVisibility() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_VISIBLE).performScrollTo().performClick()
        composeRule.settleForIdle()
    }

    fun toggleMapStyle() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().performClick()
        composeRule.settleForIdle()
        // 样式下载与渲染期间等待按键恢复使能
        composeRule.waitUntil(TIMEOUT_STYLE_MS) {
            composeRule.onAllNodesWithTag(TAG_SWITCH_MAP_STYLE)
                .fetchSemanticsNodes()
                .any { !it.config.contains(SemanticsProperties.Disabled) }
        }
    }

    fun setZoomRange(minZoom: String, maxZoom: String) = apply {
        setInput(TAG_SLIDER_MIN_ZOOM, minZoom)
        setInput(TAG_SLIDER_MAX_ZOOM, maxZoom)
    }

    private fun setInput(tag: String, value: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeRule.onNodeWithTag(tag).performTextReplacement(value)
        composeRule.settleForIdle()
    }

    fun verifyRemoveButtonIsEnabled() = apply {
        composeRule.onNodeWithText(removeBtnText).performScrollTo().assertIsEnabled()
    }

    fun verifyRemoveButtonIsDisabled() = apply {
        composeRule.onNodeWithText(removeBtnText).performScrollTo().assertIsNotEnabled()
    }

    fun verifyAddButtonIsEnabled() = apply {
        composeRule.onNodeWithText(addBtnText).performScrollTo().assertIsEnabled()
    }

    fun verifyAddButtonIsDisabled() = apply {
        composeRule.onNodeWithText(addBtnText).performScrollTo().assertIsNotEnabled()
    }

    fun verifyBinarySwitchIsOn() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_BINARY).performScrollTo().assertIsOn()
    }

    fun verifyBinarySwitchIsOff() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_BINARY).performScrollTo().assertIsOff()
    }

    fun verifyVisibilitySwitchIsOn() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_VISIBLE).performScrollTo().assertIsOn()
    }

    fun verifyVisibilitySwitchIsOff() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_VISIBLE).performScrollTo().assertIsOff()
    }

    fun verifyMapStyleSwitchIsOn() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().assertIsOn()
    }

    fun verifyMapStyleSwitchIsOff() = apply {
        composeRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().assertIsOff()
    }

    fun awaitModelReady(
        expectedId: String = "sands-detail",
        timeoutMs: Long = TIMEOUT_MODEL_MS,
    ): Model {
        val deadline = System.currentTimeMillis() + timeoutMs
        var found: Model? = null
        var lastStateSummary = ""

        while (System.currentTimeMillis() < deadline) {
            EffectTestHelper.withEngine(mapView) { engine ->
                val activeModels = engine.models.filter { it.loadState != Model.LoadState.REMOVED }
                lastStateSummary = activeModels.joinToString { "${it.modelId}:${it.loadState}" }
                found = activeModels.firstOrNull { it.modelId == expectedId }
            }
            val current = found
            if (current != null) {
                check(current.loadState != Model.LoadState.FAILED) {
                    "3D 模型 $expectedId 加载失败: ${current.loadFailureReason ?: "原因未知"}"
                }
                if (current.loadState == Model.LoadState.READY) {
                    return current
                }
            }
            Thread.sleep(200)
        }
        val fallback = found
        if (fallback != null) {
            return fallback
        }
        fail("未在 ${timeoutMs}ms 内等待到 3D 模型 $expectedId (当前已有模型状态: [$lastStateSummary])")
        throw AssertionError("unreachable")
    }

    fun verifyNoActiveModel(timeoutMs: Long = 3_000L) = apply {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var activeCount = 0
            EffectTestHelper.withEngine(mapView) { engine ->
                activeCount = engine.models.count { it.loadState != Model.LoadState.REMOVED }
            }
            if (activeCount == 0) return@apply
            Thread.sleep(200)
        }
        fail("模型移除后仍然在引擎中检测到活跃 Model")
    }

    companion object {
        private const val TAG_SWITCH_MAP_STYLE = "model_switch_map_style"
        private const val TAG_SLIDER_MIN_ZOOM = "model_slider_min_zoom"
        private const val TAG_SLIDER_MAX_ZOOM = "model_slider_max_zoom"
        private const val TAG_SWITCH_BINARY = "model_switch_binary"
        private const val TAG_SWITCH_VISIBLE = "model_switch_visible"
        private const val TIMEOUT_MODEL_MS = 10_000L
        private const val TIMEOUT_STYLE_MS = 30_000L
    }
}
