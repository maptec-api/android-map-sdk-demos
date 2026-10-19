package com.maptec.applied.demo

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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.performClickSafely
import com.maptec.applied.demo.ext.performScrollToSafely
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.pollUntil
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.runOnBackgroundThread
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.gestures.MoveDistancesObject
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineGradientStop
import com.maptec.applied.maps.overlay.line.LineJoinType
import com.maptec.applied.maps.overlay.line.LineOptions
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 覆盖物折线 Demo（多 [LineMode]）功能效果类测试：基础绘制、Join、渐变、箭头密度、线帽等。
 * 含面板实时预览：可拖拽、渐变关闭、渐变+虚线等（无需点「绘制」）。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class PolylineBasicEffectTest {

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
        private const val TAG_DROPDOWN_LINE_JOIN = "line_dropdown_line_join"
        private const val TAG_INPUT_MITER_LIMIT = "line_input_miter_limit"
        private const val TAG_SWITCH_ARROW_SPACING_CUSTOM = "line_switch_arrow_spacing_custom"
        private const val TAG_INPUT_ARROW_SPACING = "line_input_arrow_spacing"
        private const val TAG_SWITCH_GRADIENT = "line_switch_gradient"
        private const val TAG_INPUT_GRADIENT_START = "line_input_gradient_start_color"
        private const val TAG_INPUT_GRADIENT_MID = "line_input_gradient_mid_color"
        private const val TAG_INPUT_GRADIENT_END = "line_input_gradient_end_color"
        private const val TAG_INPUT_GRADIENT_MID_POSITION = "line_input_gradient_mid_position"
        private const val TAG_BUTTON_REVERSE_PATH = "line_button_reverse_path"

        /** 与 [com.maptec.applied.demo.ui.screens.overlays.polyline] 默认预览折线首顶点一致。 */
        private val DEFAULT_PREVIEW_FIRST_VERTEX = LatLng(1.360879, 103.732578)
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToLineScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun getString(resId: Int): String = getTestString(resId)

    private fun navigateToLineScreen() {
        composeTestRule.resetToMainCatalog()
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

    /** 方向箭头折线页（[LineMode.ARROWS]），含箭头 pattern 与密度控制。 */
    private fun navigateToArrowsScreen() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_line_arrows)
        composeTestRule.waitForIdle()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    /** 折线渐变页（[LineMode.GRADIENT]），默认启用沿线渐变。 */
    private fun navigateToGradientScreen() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_line_gradient)
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
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.settleForIdle()
    }

    private fun toggleSwitch(tag: String) {
        val node = composeTestRule.onNodeWithTag(tag)
        composeTestRule.performScrollToSafely(node)
        composeTestRule.performClickSafely(node)
        composeTestRule.settleForIdle()
    }

    private fun clickDraw() {
        val draw = composeTestRule.onNodeWithTag(TAG_BUTTON_DRAW)
        draw.performScrollTo()
        composeTestRule.performClickSafely(draw)
        composeTestRule.settleForIdle()
        // 绘制在 getMapAsync 里删线/加线/移相机，需留出完成时间再拾取。
        Thread.sleep(800)
    }

    private fun queryLineAt(latLng: LatLng): Line? {
        var line: Line? = null
        withMapApi { map ->
            val engine = map.getOverlayEngine()
            for (hits in listOf(
                { engine.queryOverlayHitsFromPoint(latLng) },
                { engine.queryOverlayHitsFromPoint(map.projection.toScreenLocation(latLng)) },
            )) {
                for (hit in hits()) {
                    when (val target = engine.resolveHitTarget(hit)) {
                        is Line -> {
                            line = target
                            return@withMapApi
                        }
                    }
                }
            }
        }
        return line
    }

    /** 渐变页默认开启；关开关并等预览线 [Line.isGradientEnabled] 读回为 false。 */
    private fun disableGradientPreview(probeAt: LatLng) {
        Espresso.closeSoftKeyboard()
        composeTestRule.expandConfigPanel()
        composeTestRule.settleForIdle()

        val switch = composeTestRule.onNodeWithTag(TAG_SWITCH_GRADIENT)
        composeTestRule.performScrollToSafely(switch)
        if (queryLineAt(probeAt)?.isGradientEnabled == false) return

        composeTestRule.performClickSafely(switch)
        composeTestRule.settleForIdle()

        val deadline = System.currentTimeMillis() + 25_000
        while (System.currentTimeMillis() < deadline) {
            if (queryLineAt(probeAt)?.isGradientEnabled == false) return
            Thread.sleep(300)
        }
        throw AssertionError("预览折线在 $probeAt 仍未关闭渐变（25s 内）")
    }

    private fun waitForLineAt(latLng: LatLng, timeoutMs: Long = 5000): Line {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            queryLineAt(latLng)?.let { return it }
            Thread.sleep(200)
            composeTestRule.settleForIdle()
        }
        throw AssertionError("Line not found at $latLng within ${timeoutMs}ms")
    }

    private fun waitForLineAt(
        latLng: LatLng,
        timeoutMs: Long = 8000,
        predicate: (Line) -> Boolean,
    ): Line {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            queryLineAt(latLng)?.let { line ->
                if (predicate(line)) return line
            }
            Thread.sleep(200)
            composeTestRule.settleForIdle()
        }
        throw AssertionError("Line at $latLng did not match predicate within ${timeoutMs}ms")
    }

    /**
     * 在 lineCap 下拉里选指定 label（"None"/"Round"/"Butt"/"Square"/"Arrow"/"Custom"）。
     *
     * 实现：点开 dropdown → menu 弹出 → 匹配 DropdownMenuItem 上的文本（useUnmergedTree 跨过
     * MenuItem 的合并 semantics）→ 点击第一个匹配（DropdownMenu 文本节点全局唯一）。
     */
    private fun selectLineCap(dropdownTag: String, label: String) {
        selectDropdownOption(dropdownTag, label)
    }

    private fun selectDropdownOption(dropdownTag: String, label: String) {
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
        assertArrayEquals(floatArrayOf(2f, 4f), line.strokeDashArray, 0.01f)
        assertTrue("可拖拽开关应写入 Line", line.isDraggable)
    }

    @Test
    fun testDraggable_togglePreviewWithoutDraw() {
        toggleSwitch(TAG_SWITCH_DRAGGABLE)
        val line = waitForLineAt(DEFAULT_PREVIEW_FIRST_VERTEX) { it.isDraggable }
        assertTrue(line.isDraggable)
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
    // 拐点连接样式（lineJoin）— 基础折线页
    // ============================================================

    @Test
    fun testLineJoin_dropdownDisplayed_defaultRound() {
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_LINE_JOIN)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_LINE_JOIN)
            .assertTextEquals(getString(R.string.line_join), "Round")
    }

    @Test
    fun testLineJoin_selectBevel_drawReadback() {
        val vertices = "[[39.9,116.3],[39.95,116.35],[39.9,116.4]]"
        setInput(TAG_INPUT_VERTICES, vertices)
        selectDropdownOption(TAG_DROPDOWN_LINE_JOIN, "Bevel")
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_LINE_JOIN)
            .assertTextEquals(getString(R.string.line_join), "Bevel")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertEquals(LineJoinType.BEVEL, line.lineJoin)
    }

    @Test
    fun testLineJoin_selectMiter_customLimit_drawReadback() {
        val vertices = "[[39.9,116.3],[39.95,116.35],[39.9,116.4]]"
        setInput(TAG_INPUT_VERTICES, vertices)
        selectDropdownOption(TAG_DROPDOWN_LINE_JOIN, "Miter")
        composeTestRule.onNodeWithTag(TAG_INPUT_MITER_LIMIT).performScrollTo().assertIsDisplayed()
        // 用整数避免 Slider 与右侧输入框在仪器化环境下轻微不同步（如 4.5 vs 4.52）
        setInput(TAG_INPUT_MITER_LIMIT, "5")
        composeTestRule.onNodeWithTag(TAG_INPUT_MITER_LIMIT).assertTextEquals("5")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertEquals(LineJoinType.MITER, line.lineJoin)
        assertNotNull(line.miterLimit)
        assertEquals(5f, line.miterLimit!!, 0.05f)
    }

    // ============================================================
    // 方向箭头密度（arrowSpacing）— 方向箭头折线页
    // ============================================================

    @Test
    fun testArrowSpacing_defaultCustomOff_readbackNull() {
        navigateToArrowsScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertNotNull(line.linePattern)
        assertEquals(null, line.arrowSpacing)
    }

    @Test
    fun testArrowSpacing_customValue_drawReadback() {
        navigateToArrowsScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        toggleSwitch(TAG_SWITCH_ARROW_SPACING_CUSTOM)
        composeTestRule.onNodeWithTag(TAG_INPUT_ARROW_SPACING).performScrollTo().assertIsDisplayed()
        setInput(TAG_INPUT_ARROW_SPACING, "8")
        composeTestRule.onNodeWithTag(TAG_INPUT_ARROW_SPACING).assertTextEquals("8")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertEquals(8f, line.arrowSpacing!!, 0.05f)
    }

    // ============================================================
    // 折线渐变（stroke gradient）— 折线渐变页
    // ============================================================

    @Test
    fun testGradient_controlsDisplayed_defaultEnabled() {
        navigateToGradientScreen()
        composeTestRule.onNodeWithTag(TAG_SWITCH_GRADIENT).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_INPUT_STROKE_COLOR).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_INPUT_GRADIENT_START).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_INPUT_GRADIENT_MID).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_INPUT_GRADIENT_END).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_INPUT_GRADIENT_MID_POSITION).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BUTTON_REVERSE_PATH).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testGradient_defaultDraw_gradientStopsReadback() {
        navigateToGradientScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertTrue(line.isGradientEnabled)
        val stops = line.gradientStops
        assertEquals(3, stops.size)
        assertGradientStop(stops[0], 0f, "#E63946")
        assertGradientStop(stops[1], 0.5f, "#F4A261")
        assertGradientStop(stops[2], 1f, "#2A9D8F")
    }

    @Test
    fun testGradient_customColorsAndMidPosition_drawReadback() {
        navigateToGradientScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        setInput(TAG_INPUT_GRADIENT_START, "#FF0000")
        setInput(TAG_INPUT_GRADIENT_MID, "#00FF00")
        setInput(TAG_INPUT_GRADIENT_END, "#0000FF")
        setInput(TAG_INPUT_GRADIENT_MID_POSITION, "0.3")
        composeTestRule.onNodeWithTag(TAG_INPUT_GRADIENT_MID_POSITION).assertTextEquals("0.3")
        clickDraw()

        val line = waitForLineAt(LatLng(39.9, 116.3))
        assertTrue(line.isGradientEnabled)
        val stops = line.gradientStops
        assertEquals(3, stops.size)
        assertGradientStop(stops[0], 0f, "#FF0000")
        assertGradientStop(stops[1], 0.3f, "#00FF00")
        assertGradientStop(stops[2], 1f, "#0000FF")
    }

    @Test
    fun testGradient_customColors_previewReadbackWithoutDraw() {
        navigateToGradientScreen()
        setInput(TAG_INPUT_GRADIENT_START, "#FF0000")
        setInput(TAG_INPUT_GRADIENT_MID, "#00FF00")
        setInput(TAG_INPUT_GRADIENT_END, "#0000FF")
        setInput(TAG_INPUT_GRADIENT_MID_POSITION, "0.3")

        val line = waitForLineAt(DEFAULT_PREVIEW_FIRST_VERTEX) { ln ->
            ln.isGradientEnabled && ln.gradientStops.size == 3 &&
                ln.gradientStops[1].position in 0.29f..0.31f
        }
        assertGradientStop(line.gradientStops[0], 0f, "#FF0000")
        assertGradientStop(line.gradientStops[1], 0.3f, "#00FF00")
        assertGradientStop(line.gradientStops[2], 1f, "#0000FF")
    }


    @Test
    fun testGradient_dashPattern_previewWithoutDraw() {
        navigateToGradientScreen()
        setInput(TAG_INPUT_DASH_PATTERN, "8,4")

        val line = waitForLineAt(DEFAULT_PREVIEW_FIRST_VERTEX) {
            it.strokeDashArray.size >= 2
        }
        assertArrayEquals(floatArrayOf(8f, 4f), line.strokeDashArray, 0.01f)
        assertTrue(line.isGradientEnabled)
    }

    @Test
    fun testGradient_reversePath_drawReordersVertices() {
        navigateToGradientScreen()
        setInput(TAG_INPUT_VERTICES, "[[39.9,116.3],[39.95,116.4]]")
        composeTestRule.onNodeWithTag(TAG_BUTTON_REVERSE_PATH).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        clickDraw()

        // 绘制后相机走 moveCameraToFitPolyline，target 是包围盒中心而非首顶点，不能用来断言反转。
        val line = waitForLineAt(LatLng(39.95, 116.4))
        val pts = line.latLngs
        assertEquals(2, pts.size)
        assertEquals(39.95, pts[0].latitude, 0.0001)
        assertEquals(116.4, pts[0].longitude, 0.0001)
        assertEquals(39.9, pts[1].latitude, 0.0001)
        assertEquals(116.3, pts[1].longitude, 0.0001)
        assertTrue(line.isGradientEnabled)
    }

    private fun assertGradientStop(stop: LineGradientStop, position: Float, colorHex: String) {
        assertEquals(position, stop.position, 0.001f)
        assertEquals(colorHex.uppercase(), stop.color.uppercase())
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

    // ==================== Overlay 多线程（子线程增删改 / 并发安全） ====================

    private fun obtainMap(): MaptecMap = withMapApi { it }

    /**
     * 子线程可直调 Line 增 / 改 / getOffsetGeometry / 显式拾取 / 删。
     */
    @Test
    fun testWorkerThread_addUpdateQueryDeleteLine_apiVerified() {
        val map = obtainMap()
        val p1 = LatLng(39.90, 116.30)
        val p2 = LatLng(39.95, 116.40)

        runOnBackgroundThread {
            val engine = map.overlayEngine
            val line = engine.addPolyline(
                LineOptions()
                    .withLatLngs(listOf(p1, p2))
                    .withStrokeColor("#FF0000")
                    .withStrokeWeight(6f),
            )
            assertNotNull(line.overlayId)
            line.strokeWeight = 10f
            line.setStrokeColor("#00FF00")
            line.setLatLngs(listOf(p1, LatLng(39.92, 116.35), p2))

            val screen = map.projection.toScreenLocation(p1)
            engine.queryOverlayHitsFromPoint(screen)
            engine.queryOverlayHitsFromPoint(p1)

            val move = MoveDistancesObject(screen.x, screen.y)
            move.addNewPosition(screen.x + 20f, screen.y + 20f)
            line.getOffsetGeometry(map.projection, move, 0f, 0f)

            engine.deleteLine(line)
        }
    }

    /**
     * 多线程各自操作不同 Line：并发 add / update / query / delete 不应闪退。
     */
    @Test
    fun testConcurrentThreads_differentLines_addUpdateDelete_noCrash() {
        val map = obtainMap()
        val threadCount = 4
        val roundsPerThread = 6
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val errors = ConcurrentLinkedQueue<Throwable>()
        val completed = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) { workerIndex ->
            pool.execute {
                try {
                    startLatch.await(5, TimeUnit.SECONDS)
                    val engine = map.overlayEngine
                    repeat(roundsPerThread) { round ->
                        val baseLat = 39.9 + workerIndex * 0.02 + round * 0.0005
                        val baseLng = 116.3 + workerIndex * 0.02 + round * 0.0005
                        val line = engine.addPolyline(
                            LineOptions().withLatLngs(
                                listOf(
                                    LatLng(baseLat, baseLng),
                                    LatLng(baseLat + 0.01, baseLng + 0.01),
                                ),
                            ),
                        )
                        line.strokeWeight = 4f + round
                        val screen = map.projection.toScreenLocation(LatLng(baseLat, baseLng))
                        engine.queryOverlayHitsFromPoint(screen)
                        engine.deleteLine(line)
                    }
                    completed.incrementAndGet()
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue("并发 Line 压测超时", doneLatch.await(60, TimeUnit.SECONDS))
        pool.shutdownNow()
        if (errors.isNotEmpty()) {
            fail("并发 Line 出现异常: ${errors.first()}")
        }
        assertEquals(threadCount, completed.get())
        withMapApi { map.overlayEngine.deleteAllLines() }
    }
}
