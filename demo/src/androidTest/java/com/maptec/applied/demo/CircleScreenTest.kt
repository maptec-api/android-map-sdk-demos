package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.circle.Circle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 圆形覆盖物（Circle）功能测试。
 *
 * 对应页面：业务图层 → 圆形 → [CircleListScreen] 各子项
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleBasicScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleGeodesicScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleDraggableScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleInnerShadowScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleOuterGlowScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleScanScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CirclePulseScreen]
 * - [com.maptec.applied.demo.ui.screens.overlays.circle.CircleRadiusBreathScreen]
 */
@RunWith(AndroidJUnit4::class)
class CircleScreenTest {

    companion object {
        private const val TAG_LATLNG = "circle_input_latlng"
        private const val TAG_RADIUS = "circle_input_radius"
        private const val TAG_COLOR = "circle_input_color"
        private const val TAG_OPACITY = "circle_input_opacity"
        private const val TAG_STROKE_COLOR = "circle_input_stroke_color"
        private const val TAG_STROKE_WIDTH = "circle_input_stroke_width"
        private const val TAG_STROKE_OPACITY = "circle_input_stroke_opacity"
        private const val TAG_SWITCH_GEODESIC = "circle_switch_geodesic"
        private const val TAG_SWITCH_DRAGGABLE = "circle_switch_draggable"
        private const val TAG_SWITCH_INNER_SHADOW = "circle_switch_inner_shadow"
        private const val TAG_SWITCH_OUTER_GLOW = "circle_switch_outer_glow"
        private const val TAG_INPUT_GLOW_COLOR = "circle_input_glow_color"
        private const val TAG_INPUT_GLOW_RADIUS = "circle_input_glow_radius"
        private const val TAG_INPUT_INNER_SHADOW_BLUR = "circle_input_inner_shadow_blur"
        private const val TAG_INPUT_SCAN_SECTOR_ANGLE = "circle_input_scan_sector_angle"
        private const val TAG_INPUT_SCAN_SECTOR_COLOR = "circle_input_scan_sector_color"
        private const val TAG_INPUT_SCAN_SPEED = "circle_input_scan_speed"
        private const val TAG_INPUT_ANIM_MIN = "circle_input_anim_min"
        private const val TAG_INPUT_ANIM_MAX = "circle_input_anim_max"
        private const val TAG_INPUT_ANIM_DURATION = "circle_input_anim_duration"
        private const val TAG_BTN_ANIMATION_START = "circle_btn_animation_start"
        private const val TAG_DRAW_BUTTON = "circle_draw_button"
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

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航与辅助 ====================

    private fun prepareMap(): MapView {
        composeTestRule.waitForMapRendered()
        return composeTestRule.getMapView()
    }

    private fun navigateToCircleSubScreen(menuTitleRes: Int) {
        navigateToCircleMenu()
        composeTestRule.onNodeWithText(getTestString(menuTitleRes)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun withMapApi(mapView: MapView, action: (MaptecMap) -> Unit) {
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
     * 绘制后 moveCamera 在 getMapAsync 内异步执行，需轮询等待而非单次断言。
     */
    private fun verifyCameraTarget(
        mapView: MapView,
        expectedLat: Double,
        expectedLng: Double,
        expectedZoom: Double = 15.0,
        timeoutMs: Long = 5000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastLat = Double.NaN
        var lastLng = Double.NaN
        var lastZoom = Double.NaN
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            withMapApi(mapView) { map ->
                val cam = map.cameraPosition
                val target = cam?.target
                if (target != null) {
                    lastLat = target.latitude
                    lastLng = target.longitude
                    lastZoom = cam.zoom
                    matched = abs(target.latitude - expectedLat) <= 0.01 &&
                        abs(target.longitude - expectedLng) <= 0.01 &&
                        abs(cam.zoom - expectedZoom) <= 0.01
                }
            }
            if (matched) return
            Thread.sleep(200)
        }
        fail(
            "相机未在 ${timeoutMs}ms 内到达目标：expected=($expectedLat, $expectedLng, zoom=$expectedZoom) " +
                "actual=($lastLat, $lastLng, zoom=$lastZoom)",
        )
    }

    private fun queryCircle(mapView: MapView, center: LatLng): Circle? {
        var circle: Circle? = null
        withMapApi(mapView) { map ->
            val engine = map.getOverlayEngine()
            val screen = map.projection.toScreenLocation(center)
            for (hit in engine.queryOverlayHitsFromPoint(screen)) {
                val target = engine.resolveHitTarget(hit)
                if (target is Circle) {
                    circle = target
                    return@withMapApi
                }
            }
        }
        return circle
    }

    private fun withCircleAtCenter(
        mapView: MapView,
        lat: Double,
        lng: Double,
        action: (Circle) -> Unit,
    ) {
        val circle = queryCircle(mapView, LatLng(lat, lng))
        assertNotNull("Circle not found at ($lat, $lng)", circle)
        action(circle!!)
    }

    private fun waitForCircleAt(
        mapView: MapView,
        lat: Double,
        lng: Double,
        timeoutMs: Long = 5000L,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (queryCircle(mapView, LatLng(lat, lng)) != null) return
            Thread.sleep(200)
        }
        fail("Circle not found at ($lat, $lng) within ${timeoutMs}ms")
    }

    /**
     * 绘制后 [DrawCircleButton] 会 partialExpand 收起面板，再次编辑输入需先 [expandCirclePanel]。
     */
    private fun expandCirclePanel() {
        composeTestRule.onRoot().performTouchInput {
            swipe(
                start = Offset(centerX, bottom - 10f),
                end = Offset(centerX, top + 100f),
                durationMillis = 300,
            )
        }
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        composeTestRule.waitForIdle()
    }

    private fun setInput(tag: String, value: String, expandPanel: Boolean = false) {
        if (expandPanel) {
            expandCirclePanel()
        }
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun toggleSwitch(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickDraw() {
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
    }

    // ==================== 列表导航 ====================

    private fun navigateToCircleMenu() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_overlay)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.overlay_item_circle)).performClick()
        composeTestRule.waitForIdle()
    }

    // ==================== 基础绘制 ====================

    @Test
    fun testBasicScreen_drawDefault_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_basic)
        val mapView = prepareMap()

        clickDraw()
        verifyCameraTarget(mapView, 1.4, 103.75)

        withCircleAtCenter(mapView, 1.4, 103.75) { circle ->
            assertEquals("Radius", 120.0, circle.radius, 0.01)
            assertEquals("Fill opacity", 0.5f, circle.fillOpacity, 0.01f)
            assertEquals("Stroke weight", 2.0f, circle.strokeWeight, 0.01f)
            assertEquals("Stroke opacity", 0.0f, circle.strokeOpacity, 0.01f)
            assertFalse("Geodesic default", circle.geodesic)
            assertFalse("Draggable default", circle.isDraggable)
            assertFalse("Inner shadow default", circle.innerShadow)
            assertFalse("Outer glow default", circle.outerGlow)
        }
    }

    @Test
    fun testBasicScreen_drawCustomValues_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_basic)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_RADIUS, "500")
        setInput(TAG_COLOR, "#FF0000")
        setInput(TAG_OPACITY, "0.8")
        setInput(TAG_STROKE_COLOR, "#000000")
        setInput(TAG_STROKE_WIDTH, "5")
        setInput(TAG_STROKE_OPACITY, "0.9")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertEquals("Radius", 500.0, circle.radius, 0.01)
            assertEquals("Fill color", "#FF0000", circle.fillColor)
            assertEquals("Fill opacity", 0.8f, circle.fillOpacity, 0.01f)
            assertEquals("Stroke weight", 5.0f, circle.strokeWeight, 0.01f)
            assertEquals("Stroke opacity", 0.9f, circle.strokeOpacity, 0.01f)
        }
    }

    @Test
    fun testBasicScreen_invalidLatLng_drawDisabled() {
        navigateToCircleSubScreen(R.string.circle_list_basic)
        prepareMap()

        setInput(TAG_LATLNG, "invalid")
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testBasicScreen_redrawUpdatesCamera() {
        navigateToCircleSubScreen(R.string.circle_list_basic)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        clickDraw()
        waitForCircleAt(mapView, 39.9, 116.4)
        verifyCameraTarget(mapView, 39.9, 116.4)

        setInput(TAG_LATLNG, "40.0,117.0", expandPanel = true)
        clickDraw()
        waitForCircleAt(mapView, 40.0, 117.0)
        verifyCameraTarget(mapView, 40.0, 117.0)
    }

    // ==================== 等距模式 ====================

    @Test
    fun testGeodesicScreen_geodesicEnabled_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_geodesic)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        toggleSwitch(TAG_SWITCH_GEODESIC)
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertTrue("Geodesic should be enabled", circle.geodesic)
        }
    }

    // ==================== 可拖拽 ====================

    @Test
    fun testDraggableScreen_draggableDefault_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_draggable)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertTrue("Draggable should default to enabled", circle.isDraggable)
        }
    }

    @Test
    fun testDraggableScreen_draggableDisabled_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_draggable)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        toggleSwitch(TAG_SWITCH_DRAGGABLE)
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertFalse("Draggable should be disabled", circle.isDraggable)
        }
    }

    // ==================== 内阴影 ====================

    @Test
    fun testInnerShadowScreen_innerShadowEnabled_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_inner_shadow)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertTrue("Inner shadow should be enabled", circle.innerShadow)
            assertEquals("Inner shadow blur", 10.0f, circle.innerShadowBlur, 0.01f)
        }
    }

    @Test
    fun testInnerShadowScreen_customBlur_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_inner_shadow)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_INPUT_INNER_SHADOW_BLUR, "20")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertEquals("Inner shadow blur", 20.0f, circle.innerShadowBlur, 0.01f)
        }
    }

    // ==================== 外发光 ====================

    @Test
    fun testOuterGlowScreen_glowConditionalUI() {
        navigateToCircleSubScreen(R.string.circle_list_outer_glow)
        prepareMap()

        toggleSwitch(TAG_SWITCH_OUTER_GLOW)
        composeTestRule.onNodeWithTag(TAG_INPUT_GLOW_COLOR).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_INPUT_GLOW_RADIUS).assertDoesNotExist()

        toggleSwitch(TAG_SWITCH_OUTER_GLOW)
        composeTestRule.onNodeWithTag(TAG_INPUT_GLOW_COLOR).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_INPUT_GLOW_RADIUS).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testOuterGlowScreen_customGlowValues_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_outer_glow)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_INPUT_GLOW_COLOR, "#FF00FF")
        setInput(TAG_INPUT_GLOW_RADIUS, "15")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertTrue("Outer glow should be enabled", circle.outerGlow)
            assertEquals("Glow color", "#FF00FF", circle.glowColor)
            assertEquals("Glow radius", 15.0f, circle.glowRadius, 0.01f)
        }
    }

    // ==================== 扫描动画 ====================

    @Test
    fun testScanScreen_drawCreatesCircle_apiVerified() {
        navigateToCircleSubScreen(R.string.circle_list_scan)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_INPUT_SCAN_SECTOR_ANGLE, "45")
        setInput(TAG_INPUT_SCAN_SECTOR_COLOR, "#00FF00")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertNotNull("Circle center", circle.center)
            assertEquals("Radius from basic state", 120.0, circle.radius, 0.01)
        }
    }

    @Test
    fun testScanScreen_startAnimation_afterDraw() {
        navigateToCircleSubScreen(R.string.circle_list_scan)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertNotNull("扫描动画启动前 Circle 不应为空", circle.center)
            assertEquals("扫描动画启动前半径应正确", 120.0, circle.radius, 0.01)
        }

        composeTestRule.onNodeWithTag(TAG_BTN_ANIMATION_START).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertNotNull("扫描动画启动后 Circle 应存在", circle.center)
        }
    }

    // ==================== 脉动 / 半径呼吸 ====================

    @Test
    fun testPulseScreen_drawAndStartAnimation_noCrash() {
        navigateToCircleSubScreen(R.string.circle_list_pulse)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_INPUT_ANIM_MIN, "50")
        setInput(TAG_INPUT_ANIM_MAX, "150")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertEquals("Base radius", 120.0, circle.radius, 0.01)
        }

        composeTestRule.onNodeWithTag(TAG_BTN_ANIMATION_START).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertNotNull("脉动动画启动后 Circle 应存在", circle.center)
        }
    }

    @Test
    fun testRadiusBreathScreen_drawAndStartAnimation_noCrash() {
        navigateToCircleSubScreen(R.string.circle_list_radius_breath)
        val mapView = prepareMap()

        setInput(TAG_LATLNG, "39.9,116.4")
        setInput(TAG_INPUT_ANIM_MIN, "80")
        setInput(TAG_INPUT_ANIM_MAX, "200")
        setInput(TAG_INPUT_ANIM_DURATION, "2000")
        clickDraw()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertEquals("Radius", 120.0, circle.radius, 0.01)
        }

        composeTestRule.onNodeWithTag(TAG_BTN_ANIMATION_START).performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag(TAG_BTN_ANIMATION_START).performClick()
        composeTestRule.waitForIdle()

        withCircleAtCenter(mapView, 39.9, 116.4) { circle ->
            assertNotNull("半径呼吸动画启动后 Circle 应存在", circle.center)
        }
    }
}
