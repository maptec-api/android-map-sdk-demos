package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.line.Line
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * [PolylineBasicScreen] 功能测试：基础折线绘制与线帽选项。
 */
@RunWith(AndroidJUnit4::class)
class PolylineBasicScreenTest {

    companion object {
        private const val TAG_INPUT_VERTICES = "line_input_vertices"
        private const val TAG_INPUT_STROKE_COLOR = "line_input_stroke_color"
        private const val TAG_INPUT_OPACITY = "line_input_opacity"
        private const val TAG_INPUT_WIDTH = "line_input_width"
        private const val TAG_INPUT_DASH_PATTERN = "line_input_dash_pattern"
        private const val TAG_SWITCH_CLOSED = "line_switch_closed"
        private const val TAG_SWITCH_DRAGGABLE = "line_switch_draggable"
        private const val TAG_INPUT_GLOW_COLOR = "line_input_glow_color"
        private const val TAG_INPUT_GLOW_RADIUS = "line_input_glow_radius"
        private const val TAG_BUTTON_DRAW = "line_button_draw"
        // 起/终点线帽下拉
        private const val TAG_DROPDOWN_START_CAP = "line_dropdown_start_cap"
        private const val TAG_DROPDOWN_END_CAP = "line_dropdown_end_cap"
    }

    private val permissionRule = GrantPermissionRule.grant(
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
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToLineScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
        Thread.sleep(500)
    }

    private fun getString(resId: Int): String = getTestString(resId)

    private fun navigateToLineScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_line)
        composeTestRule.waitForIdle()
    }

    /**
     * 线帽（lineCap）下拉仅在「线帽折线」页（[com.maptec.applied.demo.ui.screens.overlays.polyline.LineMode.CAPS]）
     * 渲染，基础折线页不显示。[setUp] 默认停在基础折线页，因此线帽相关用例先返回主目录、
     * 切换到线帽折线页，并刷新 [mapView] 引用（导航后原 MapView 已失效）。
     */
    private fun navigateToCapsScreen() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_line_caps)
        composeTestRule.waitForIdle()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    /**
     * 发光（glow）开关与发光颜色/半径输入仅在「发光折线」页
     * （[com.maptec.applied.demo.ui.screens.overlays.polyline.LineMode.GLOW]）渲染，
     * 基础折线页不显示。该页默认已启用发光。切换页面后需刷新 [mapView] 引用。
     */
    private fun navigateToGlowScreen() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_line_glow)
        composeTestRule.waitForIdle()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    private fun <T> withMapApi(action: (MaptecMap) -> T): T {
        val latch = CountDownLatch(1)
        var result: T? = null
        var error: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    result = action(map)
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("异步获取 MaptecMap 超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
        return result!!
    }

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performTextClearance()
        composeTestRule.onNodeWithTag(tag).performTextInput(value)
        composeTestRule.waitForIdle()
    }

    private fun toggleSwitch(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickDraw() {
        composeTestRule.onNodeWithTag(TAG_BUTTON_DRAW).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun queryLineAt(latLng: LatLng): Line? {
        var line: Line? = null
        withMapApi { map ->
            val engine = map.getOverlayEngine()
            val screen = map.projection.toScreenLocation(latLng)
            for (hit in engine.queryOverlayHitsFromPoint(screen)) {
                val target = engine.resolveHitTarget(hit)
                if (target is Line) {
                    line = target
                    break
                }
            }
        }
        return line
    }

    private fun waitForLineAt(latLng: LatLng, timeoutMs: Long = 5000): Line {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            queryLineAt(latLng)?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Line not found at $latLng within ${timeoutMs}ms")
    }

    /**
     * 在 lineCap 下拉里选指定 label（"None"/"Round"/"Butt"/"Square"/"Arrow"/"Custom"）。
     *
     * 实现：点开 dropdown → menu 弹出 → 匹配 DropdownMenuItem 上的文本（useUnmergedTree 跨过
     * MenuItem 的合并 semantics）→ 点击第一个匹配（DropdownMenu 文本节点全局唯一）。
     */
    private fun selectLineCap(dropdownTag: String, label: String) {
        composeTestRule.onNodeWithTag(dropdownTag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(label, useUnmergedTree = true).onFirst().performClick()
        composeTestRule.waitForIdle()
    }


    @Test
    fun testDraw_emptyInput_noCrash() {
        val baseline = withMapApi { map ->
            val cam = map.cameraPosition
            Triple(cam.target!!.latitude, cam.target!!.longitude, cam.zoom)
        }

        setInput(TAG_INPUT_VERTICES, "")
        clickDraw()
        composeTestRule.onNodeWithTag(TAG_BUTTON_DRAW).assertIsDisplayed()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("空输入不应移动相机纬度", baseline.first, cam.target!!.latitude, 0.01)
            assertEquals("空输入不应移动相机经度", baseline.second, cam.target!!.longitude, 0.01)
            assertEquals("空输入不应改变缩放", baseline.third, cam.zoom, 0.01)
        }
    }

    @Test
    fun testDraw_validInput_cameraMoves() {
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("相机应移动到城中心纬度", 39.925, cam.target!!.latitude, 0.01)
            assertEquals("相机应移动到城中心经度", 116.35, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull("默认值绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testDraw_customValues_cameraMoves() {
        setInput(TAG_INPUT_VERTICES, "[[35.6,139.7],[35.7,139.8]]")
        setInput(TAG_INPUT_STROKE_COLOR, "#0000FF")
        setInput(TAG_INPUT_OPACITY, "0.9")
        setInput(TAG_INPUT_WIDTH, "15")
        setInput(TAG_INPUT_DASH_PATTERN, "2,4")
        toggleSwitch(TAG_SWITCH_CLOSED)
        toggleSwitch(TAG_SWITCH_DRAGGABLE)
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("纬度应为城中心纬度", 35.65, cam.target!!.latitude, 0.01)
            assertEquals("经度应为城中心经度", 139.75, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(35.6, 139.7))
        assertNotNull("自定义值绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testDraw_glowEnabled_cameraMoves() {
        navigateToGlowScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        setInput(TAG_INPUT_GLOW_COLOR, "#FF00FF")
        setInput(TAG_INPUT_GLOW_RADIUS, "15")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals(39.925, cam.target!!.latitude, 0.01)
            assertEquals(116.35, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull("发光效果绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testDraw_multipleDraws_cameraUpdates() {
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        clickDraw()

        withMapApi { map ->
            assertEquals("第一次绘制相机纬度", 39.925, map.cameraPosition.target!!.latitude, 0.01)
        }

        setInput(TAG_INPUT_VERTICES, "[[40.0,117.0],[40.05,117.1]]")
        clickDraw()

        withMapApi { map ->
            assertEquals("第二次绘制应移动到新坐标", 40.025, map.cameraPosition.target!!.latitude, 0.01)
        }
    }

    // ============================================================
    // 起/终点线帽（lineCap）测试
    //   - "None / Round / Butt / Square" 走引擎 PolylineOverlay.lineCapStart/End
    //   - "Arrow / Custom" 走 MarkLayer marker（业务侧）
    // 引擎渲染结果不在公开 API 暴露，所以测试覆盖：
    //   1. UI dropdown 可点开、切换、文本同步
    //   2. 各种 cap 组合下"绘制"不 crash 且相机正确移动到第一个顶点
    //   3. 重绘切换后相机仍能正常工作（侧面验证 marker 的 add/remove 没漏掉）
    // ============================================================

    @Test
    fun testLineCapDropdowns_displayed_defaultRound() {
        navigateToCapsScreen()
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_START_CAP).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_END_CAP).performScrollTo().assertIsDisplayed()
        // 线帽折线页默认起/终点线帽均为 Round
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_START_CAP).assertTextEquals("起点线帽", "Round")
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_END_CAP).assertTextEquals("终点线帽", "Round")
    }

    @Test
    fun testLineCap_selectSquare_endCapUpdates() {
        navigateToCapsScreen()
        selectLineCap(TAG_DROPDOWN_END_CAP, "Square")
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_END_CAP).assertTextEquals("终点线帽", "Square")
    }

    @Test
    fun testLineCap_roundDraw_cameraMoves() {
        navigateToCapsScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Round")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Round")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("Round 线帽绘制后相机应移动", 39.925, cam.target!!.latitude, 0.01)
            assertEquals(116.35, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull("Round 线帽绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testLineCap_arrowBothEnds_drawSuccess() {
        navigateToCapsScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Arrow")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Arrow")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("Arrow 线帽绘制后相机应移动", 39.925, cam.target!!.latitude, 0.01)
            assertEquals(116.35, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull("Arrow 线帽绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testLineCap_customBothEnds_drawSuccess() {
        navigateToCapsScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Custom")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Custom")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals("Custom 线帽绘制后相机应移动", 39.925, cam.target!!.latitude, 0.01)
            assertEquals(116.35, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull("Custom 线帽绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testLineCap_mixedStartArrowEndCustom_drawSuccess() {
        navigateToCapsScreen()
        setInput(TAG_INPUT_VERTICES, "[[35.6,139.7],[35.7,139.8]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Arrow")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Custom")
        clickDraw()

        withMapApi { map ->
            val cam = map.cameraPosition
            assertEquals(35.65, cam.target!!.latitude, 0.01)
            assertEquals(139.75, cam.target!!.longitude, 0.01)
        }
        val line = waitForLineAt(LatLng(35.6, 139.7))
        assertNotNull("混合线帽绘制后应能查询到 Line 覆盖物", line)
    }

    @Test
    fun testLineCap_arrowThenRoundRedraw_noCrash() {
        navigateToCapsScreen()
        // 第一次：起终点都用 Arrow（业务侧 marker 路径）
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Arrow")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Arrow")
        clickDraw()

        withMapApi { map ->
            assertEquals(39.925, map.cameraPosition.target!!.latitude, 0.01)
        }

        // 第二次：切回 Round（引擎线帽），marker 应被清掉，引擎线帽生效
        selectLineCap(TAG_DROPDOWN_START_CAP, "Round")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Round")
        setInput(TAG_INPUT_VERTICES, "[[40.0,117.0],[40.05,117.1]]")
        clickDraw()

        withMapApi { map ->
            assertEquals("切线帽 + 重绘相机应移动", 40.025, map.cameraPosition.target!!.latitude, 0.01)
            assertEquals(117.05, map.cameraPosition.target!!.longitude, 0.01)
        }
    }

    @Test
    fun testLineCap_customWithColorChange_redrawOk() {
        navigateToCapsScreen()
        // 第一次：绿色线 + Custom 起点（marker 用 stroke 色）
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        setInput(TAG_INPUT_STROKE_COLOR, "#00A63E")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Custom")
        clickDraw()

        // 第二次：换红色重绘，Custom marker 应跟着变色（颜色无法直接断言，验证不 crash + 相机正确）
        setInput(TAG_INPUT_STROKE_COLOR, "#FF0000")
        clickDraw()

        withMapApi { map ->
            assertEquals(39.925, map.cameraPosition.target!!.latitude, 0.01)
        }
    }

    @Test
    fun testLineCap_arrowOnSinglePointInput_noCrash() {
        navigateToCapsScreen()
        // 只输入一个点，pts.size < 2，Arrow/Custom helper 会被 size 检查跳过
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3]]")
        selectLineCap(TAG_DROPDOWN_START_CAP, "Arrow")
        selectLineCap(TAG_DROPDOWN_END_CAP, "Arrow")
        clickDraw()
        // 单点情况下 parseLineLatLngs 仍返回 1 个点，绘制按钮的 pts.isEmpty() 不拦截，但 size<2 时
        // arrow helper 被跳过 → 不 crash 即可
        composeTestRule.onNodeWithTag(TAG_BUTTON_DRAW).assertIsDisplayed()
    }
}
