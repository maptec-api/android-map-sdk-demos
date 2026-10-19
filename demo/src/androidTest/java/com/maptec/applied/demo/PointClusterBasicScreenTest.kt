package com.maptec.applied.demo

import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.expandConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.cluster.Cluster
import com.maptec.applied.maps.overlay.cluster.ClusterClickBehavior
import com.maptec.applied.maps.overlay.cluster.ClusterOptions
import com.maptec.applied.maps.overlay.cluster.util.ClusterGeoJson
import com.maptec.applied.utils.BitmapUtils
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [PointClusterBasicScreen] 基础点聚合功能测试。
 *
 * 覆盖：默认自动绘制、核心参数滑块/开关 → Cluster API、样式区实时下发、
 * 颜色选择、簇点击放大、外部数据、清除、无效输入禁用绘制按钮。
 *
 * 不使用 GrantPermissionRule：仅需安装时权限；该 Rule 抢连 UiAutomation 易 already registered。
 */
@RunWith(AndroidJUnit4::class)
class PointClusterBasicScreenTest {

    companion object {
        private const val TAG_RADIUS = "point_cluster_slider_radius"
        private const val TAG_MAX_ZOOM = "point_cluster_slider_max_zoom"
        private const val TAG_MIN_POINTS = "point_cluster_slider_min_points"
        private const val TAG_SWITCH_ENABLE = "point_cluster_switch_enable"
        private const val TAG_SWITCH_ABBREVIATE = "point_cluster_switch_abbreviate"
        private const val TAG_SWITCH_VISIBLE = "point_cluster_switch_visible"
        private const val TAG_SWITCH_MAP_STYLE = "point_cluster_switch_map_style"
        private const val TAG_CLUSTER_STYLE_TOGGLE = "point_cluster_cluster_style_toggle"
        private const val TAG_LEAF_STYLE_TOGGLE = "point_cluster_leaf_style_toggle"
        private const val TAG_SWITCH_CUSTOM_ICON = "point_cluster_switch_cluster_custom_icon"
        private const val TAG_SWITCH_ADAPTIVE = "point_cluster_switch_cluster_adaptive_radius"
        private const val TAG_SWITCH_ICON_STEPS = "point_cluster_switch_cluster_icon_steps"
        private const val TAG_FILL_COLOR = "point_cluster_cluster_fill_color"
        private const val TAG_STROKE_COLOR = "point_cluster_cluster_stroke_color"
        private const val TAG_TEXT_COLOR = "point_cluster_cluster_text_color"
        private const val TAG_SLIDER_FILL_OPACITY = "point_cluster_slider_cluster_fill_opacity"
        private const val TAG_SLIDER_STROKE_WIDTH = "point_cluster_slider_cluster_stroke_width"
        private const val TAG_SLIDER_VISUAL_RADIUS = "point_cluster_slider_cluster_visual_radius"
        private const val TAG_SLIDER_MIN_RADIUS = "point_cluster_slider_cluster_min_radius"
        private const val TAG_SLIDER_MAX_RADIUS = "point_cluster_slider_cluster_max_radius"
        private const val TAG_SLIDER_MAX_REFERENCE = "point_cluster_slider_cluster_max_reference"
        private const val TAG_SLIDER_CLUSTER_TEXT_SIZE = "point_cluster_slider_cluster_text_size"
        private const val TAG_SLIDER_CLUSTER_LAYER_MIN =
            "point_cluster_slider_cluster_layer_min_zoom"
        private const val TAG_SLIDER_CLUSTER_LAYER_MAX =
            "point_cluster_slider_cluster_layer_max_zoom"
        private const val TAG_SWITCH_LEAF_ICON_MODE = "point_cluster_switch_leaf_icon_mode"
        private const val TAG_SLIDER_LEAF_ICON_SIZE = "point_cluster_slider_leaf_icon_size"
        private const val TAG_SLIDER_LEAF_ICON_ROTATION = "point_cluster_slider_leaf_icon_rotation"
        private const val TAG_SLIDER_LEAF_ICON_OPACITY = "point_cluster_slider_leaf_icon_opacity"
        private const val TAG_SWITCH_LEAF_SHOW_TEXT = "point_cluster_switch_leaf_show_text"
        private const val TAG_SLIDER_LEAF_TEXT_SIZE = "point_cluster_slider_leaf_text_size"
        private const val TAG_SLIDER_POINT_MIN_ZOOM = "point_cluster_slider_point_min_zoom"
        private const val TAG_SLIDER_POINT_MAX_ZOOM = "point_cluster_slider_point_max_zoom"
        private const val TAG_DRAW = "point_cluster_draw_button"
        private const val TAG_LOAD_EXTERNAL = "point_cluster_load_external_button"
        private const val TAG_REMOVE = "point_cluster_remove_button"
        private const val TAG_MAP = "mapView"

        private const val DEMO_LAT = 1.315
        private const val DEMO_LNG = 103.82
        private const val DEMO_ZOOM = 11.15

        /** 演示数据第一簇锚点，zoom≈11 时应为聚合簇。 */
        private val CLUSTER_QUERY = LatLng(1.2903, 103.8519)

        private const val DEFAULT_POINT_COUNT = 80
        private const val EXTERNAL_POINT_COUNT = 120
        private const val TIMEOUT_CAMERA_MS = 8_000L
        private const val TIMEOUT_CLUSTER_MS = 10_000L
        /** 深色样式首次下载 + onStyleRendered 重绘，测试机/CI 常超过 15s。 */
        private const val TIMEOUT_STYLE_MS = 30_000L
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
        navigateToScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    // ==================== 导航与辅助 ====================

    private fun navigateToScreen() {
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_point_cluster_basic)
        composeTestRule.waitForIdle()
    }

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickTagged(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickDraw() {
        clickTagged(TAG_DRAW)
        Thread.sleep(1_200)
        composeTestRule.expandConfigPanel()
    }

    private fun clickLoadExternal() {
        clickTagged(TAG_LOAD_EXTERNAL)
        Thread.sleep(1_500)
        composeTestRule.expandConfigPanel()
    }

    private fun clickRemove() {
        clickTagged(TAG_REMOVE)
        Thread.sleep(800)
        composeTestRule.expandConfigPanel()
    }

    private fun verifyCameraTarget(
        expectedLat: Double = DEMO_LAT,
        expectedLng: Double = DEMO_LNG,
        expectedZoom: Double = DEMO_ZOOM,
        timeoutMs: Long = TIMEOUT_CAMERA_MS,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastLat = Double.NaN
        var lastLng = Double.NaN
        var lastZoom = Double.NaN
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            EffectTestHelper.withMapApi(mapView) { map ->
                val cam = map.cameraPosition
                val target = cam?.target
                if (target != null) {
                    lastLat = target.latitude
                    lastLng = target.longitude
                    lastZoom = cam.zoom
                    matched =
                        abs(target.latitude - expectedLat) <= 0.02 && abs(target.longitude - expectedLng) <= 0.02 && abs(
                            cam.zoom - expectedZoom
                        ) <= 0.05
                }
            }
            if (matched) return
            Thread.sleep(200)
        }
        fail(
            "相机未到位 expected=($expectedLat,$expectedLng,z=$expectedZoom) " + "actual=($lastLat,$lastLng,z=$lastZoom)",
        )
    }

    private fun awaitCluster(
        at: LatLng = CLUSTER_QUERY,
        timeoutMs: Long = TIMEOUT_CLUSTER_MS,
    ): Cluster {
        // 不依赖 platform 私有列表 API：多点位拾取 + resolveHitTarget
        val probePoints = listOf(
            at,
            LatLng(DEMO_LAT, DEMO_LNG),
            LatLng(1.3048, 103.8318),
            LatLng(1.3023, 103.8519),
        )
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: String? = null
        while (System.currentTimeMillis() < deadline) {
            var found: Cluster? = null
            EffectTestHelper.withEngine(mapView) { engine ->
                val hitSummaries = mutableListOf<String>()
                for (point in probePoints) {
                    val hits = engine.queryOverlayHitsFromPoint(point)
                    hitSummaries += "${point.latitude},${point.longitude}->${hits.map { it.overlayId }}"
                    for (hit in hits) {
                        val target = engine.resolveHitTarget(hit)
                        if (target is Cluster && !target.isRemoved) {
                            found = target
                            return@withEngine
                        }
                    }
                }
                lastError = hitSummaries.joinToString("; ")
            }
            found?.let { return it }
            Thread.sleep(250)
        }
        fail("未在 ${timeoutMs}ms 内拾取到 Cluster：$lastError")
        throw AssertionError("unreachable")
    }

    /** 确认当前视野内没有活跃 Cluster（用于 remove 后断言）。 */
    private fun assertNoActiveClusterHit(timeoutMs: Long = 3_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var found = false
            EffectTestHelper.withEngine(mapView) { engine ->
                for (point in listOf(CLUSTER_QUERY, LatLng(DEMO_LAT, DEMO_LNG))) {
                    found = engine.queryOverlayHitsFromPoint(point).any {
                        val t = engine.resolveHitTarget(it)
                        t is Cluster && !t.isRemoved
                    }
                    if (found) break
                }
            }
            if (!found) return
            Thread.sleep(200)
        }
        fail("移除后仍能拾取到活跃 Cluster")
    }

    private fun awaitClusterReady(cluster: Cluster, timeoutMs: Long = TIMEOUT_CLUSTER_MS) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (cluster.lifecycleState == Cluster.LifecycleState.READY || cluster.lifecycleState == Cluster.LifecycleState.EMPTY) {
                return
            }
            Thread.sleep(150)
        }
        fail("Cluster 未就绪，state=${cluster.lifecycleState} error=${cluster.lifecycleError}")
    }

    private fun pickColor(fieldTag: String, colorHex: String) {
        val optionTag = "${fieldTag}_option_${colorHex.uppercase()}"
        composeTestRule.onNodeWithTag(fieldTag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(optionTag).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)
    }

    private fun mapLocalPointOf(latLng: LatLng): Offset {
        var sx = 0f
        var sy = 0f
        EffectTestHelper.withMapApi(mapView) { map ->
            val screen = map.projection.toScreenLocation(latLng)
            sx = screen.x
            sy = screen.y
        }
        return Offset(
            sx.coerceIn(1f, (mapView.width - 1).toFloat().coerceAtLeast(1f)),
            sy.coerceIn(1f, (mapView.height - 1).toFloat().coerceAtLeast(1f)),
        )
    }

    private fun clickMapAt(latLng: LatLng) {
        composeTestRule.collapseConfigPanel()
        composeTestRule.settleForIdle()
        val offset = mapLocalPointOf(latLng)
        composeTestRule.onNodeWithTag(TAG_MAP).performTouchInput { click(offset) }
        composeTestRule.settleForIdle()
        Thread.sleep(800)
    }

    private fun currentZoom(): Double {
        var zoom = Double.NaN
        EffectTestHelper.withMapApi(mapView) { map ->
            zoom = map.cameraPosition?.zoom ?: Double.NaN
        }
        return zoom
    }

    // ==================== 默认自动绘制 ====================

    @Test
    fun testDefaultAutoDrawn_cameraAndApiDefaults() {
        verifyCameraTarget()
        val cluster = awaitCluster()
        awaitClusterReady(cluster)

        assertEquals(DEFAULT_POINT_COUNT, cluster.points.size)
        assertEquals(true, cluster.clusterEnabled)
        assertEquals(50, cluster.clusterRadius)
        assertEquals(16.0, cluster.clusterMaxZoom!!, 0.01)
        assertEquals(2L, cluster.clusterMinPoints)
        assertEquals(true, cluster.clusterCountAbbreviate)
        assertEquals(true, cluster.visible)
        assertEquals(0f, cluster.clusterLayerMinZoom!!, 0.01f)
        assertEquals(16f, cluster.clusterLayerMaxZoom!!, 0.01f)
        assertEquals(0f, cluster.pointLayerMinZoom!!, 0.01f)
        assertEquals(22f, cluster.pointLayerMaxZoom!!, 0.01f)
        assertEquals(Cluster.LifecycleState.READY, cluster.lifecycleState)
    }

    // ==================== 核心参数 → API ====================

    @Test
    fun testCoreSliders_updateClusterApi() {
        setInput(TAG_RADIUS, "80")
        setInput(TAG_MAX_ZOOM, "14")
        setInput(TAG_MIN_POINTS, "5")
        composeTestRule.waitForIdle()
        Thread.sleep(400)

        val cluster = awaitCluster()
        assertEquals(80, cluster.clusterRadius)
        assertEquals(14.0, cluster.clusterMaxZoom!!, 0.01)
        assertEquals(5L, cluster.clusterMinPoints)
    }

    @Test
    fun testEnableAndAbbreviateSwitches_updateClusterApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_ENABLE).performScrollTo().assertIsOn()
        composeTestRule.onNodeWithTag(TAG_SWITCH_ABBREVIATE).performScrollTo().assertIsOn()

        clickTagged(TAG_SWITCH_ENABLE)
        clickTagged(TAG_SWITCH_ABBREVIATE)

        val cluster = awaitCluster()
        assertEquals(false, cluster.clusterEnabled)
        assertEquals(false, cluster.clusterCountAbbreviate)

        clickTagged(TAG_SWITCH_ENABLE)
        clickTagged(TAG_SWITCH_ABBREVIATE)
        assertEquals(true, cluster.clusterEnabled)
        assertEquals(true, cluster.clusterCountAbbreviate)
    }

    @Test
    fun testVisibleSwitch_updatesClusterApi() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_VISIBLE).performScrollTo().assertIsOn()
        val cluster = awaitCluster()
        assertEquals(true, cluster.visible)

        clickTagged(TAG_SWITCH_VISIBLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_VISIBLE).assertIsOff()
        assertEquals(false, cluster.visible)

        clickTagged(TAG_SWITCH_VISIBLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_VISIBLE).assertIsOn()
        assertEquals(true, cluster.visible)
    }

    @Test
    fun testDrawButton_redrawsWithCustomCoreParams() {
        setInput(TAG_RADIUS, "60")
        setInput(TAG_MAX_ZOOM, "12")
        setInput(TAG_MIN_POINTS, "3")
        clickDraw()
        verifyCameraTarget()

        val cluster = awaitCluster()
        awaitClusterReady(cluster)
        assertEquals(DEFAULT_POINT_COUNT, cluster.points.size)
        assertEquals(60, cluster.clusterRadius)
        assertEquals(12.0, cluster.clusterMaxZoom!!, 0.01)
        assertEquals(3L, cluster.clusterMinPoints)
    }

    // ==================== 无效输入 ====================

    @Test
    fun testInvalidRadius_drawButtonDisabled() {
        setInput(TAG_RADIUS, "600")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_RADIUS, "50")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidMaxZoom_drawButtonDisabled() {
        setInput(TAG_MAX_ZOOM, "30")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_MAX_ZOOM, "16")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidMinPoints_drawButtonDisabled() {
        setInput(TAG_MIN_POINTS, "1")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_MIN_POINTS, "2")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidLeafOpacity_drawButtonDisabled() {
        clickTagged(TAG_LEAF_STYLE_TOGGLE)
        setInput(TAG_SLIDER_LEAF_ICON_OPACITY, "1.5")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_SLIDER_LEAF_ICON_OPACITY, "1")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidClusterLayerZoom_drawButtonDisabled() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        setInput(TAG_SLIDER_CLUSTER_LAYER_MAX, "25")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_SLIDER_CLUSTER_LAYER_MAX, "16")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidAdaptiveMaxReference_drawButtonDisabled() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_ADAPTIVE).performScrollTo().assertIsOn()
        // maxReference 必须 > minPoints(默认 2)
        setInput(TAG_SLIDER_MAX_REFERENCE, "2")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_SLIDER_MAX_REFERENCE, "1000")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testInvalidAdaptiveMaxRadiusLtMin_drawButtonDisabled() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_ADAPTIVE).performScrollTo().assertIsOn()
        setInput(TAG_SLIDER_MIN_RADIUS, "30")
        setInput(TAG_SLIDER_MAX_RADIUS, "20")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_SLIDER_MAX_RADIUS, "36")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    // ==================== 聚合点样式 ====================

    @Test
    fun testClusterStyle_colorPickers_applySimpleStyle() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        pickColor(TAG_FILL_COLOR, "#EB5757")
        pickColor(TAG_STROKE_COLOR, "#000000")
        pickColor(TAG_TEXT_COLOR, "#FFC107")
        Thread.sleep(500)

        val cluster = awaitCluster()
        assertNotNull(cluster.clusterSimpleStyle)
        assertNotNull(cluster.clusterAdvancedStyles)
        assertEquals(3, cluster.clusterAdvancedStyles!!.size)
        assertEquals(Cluster.LifecycleState.READY, cluster.lifecycleState)
    }

    @Test
    fun testClusterStyleSection_simpleStyleLiveApply() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        composeTestRule.onNodeWithTag(TAG_SLIDER_FILL_OPACITY).assertIsDisplayed()

        setInput(TAG_SLIDER_FILL_OPACITY, "0.55")
        setInput(TAG_SLIDER_STROKE_WIDTH, "2.5")
        setInput(TAG_SLIDER_CLUSTER_TEXT_SIZE, "16")
        setInput(TAG_SLIDER_CLUSTER_LAYER_MIN, "2")
        setInput(TAG_SLIDER_CLUSTER_LAYER_MAX, "15")
        Thread.sleep(500)

        val cluster = awaitCluster()
        assertNotNull(cluster.clusterSimpleStyle)
        assertEquals(2f, cluster.clusterLayerMinZoom!!, 0.01f)
        assertEquals(15f, cluster.clusterLayerMaxZoom!!, 0.01f)
        assertNotNull(cluster.clusterIconRadius)
        assertEquals(ClusterOptions.IconRadius.MODE_ADAPTIVE, cluster.clusterIconRadius!!.mode)
    }

    @Test
    fun testClusterStyle_adaptiveRadiusParams_updateIconRadiusApi() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_ADAPTIVE).performScrollTo().assertIsOn()

        val before = awaitCluster()
        // 先触发一次 SimpleStyle，拿到默认自适应半径
        setInput(TAG_SLIDER_MIN_RADIUS, "18")
        Thread.sleep(400)
        val baseline = before.clusterIconRadius
        assertNotNull(baseline)
        assertEquals(ClusterOptions.IconRadius.MODE_ADAPTIVE, baseline!!.mode)

        setInput(TAG_SLIDER_MIN_RADIUS, "20")
        setInput(TAG_SLIDER_MAX_RADIUS, "36")
        setInput(TAG_SLIDER_MAX_REFERENCE, "500")
        Thread.sleep(500)

        val radius = before.clusterIconRadius
        assertNotNull(radius)
        assertEquals(ClusterOptions.IconRadius.MODE_ADAPTIVE, radius!!.mode)
        // IconRadius 存的是 dp→iconSize 比例，不是原始 dp；校验参考数量与相对大小
        assertEquals(500L, radius.maxReference)
        assertTrue(radius.minRadius > 0f)
        assertTrue(radius.maxRadius > radius.minRadius)
        assertTrue(
            "增大视觉半径后 max 比例应上升",
            radius.maxRadius > baseline.maxRadius || radius.minRadius != baseline.minRadius,
        )
    }

    @Test
    fun testClusterStyle_adaptiveToFixedRadius() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_ADAPTIVE).performScrollTo().assertIsOn()

        clickTagged(TAG_SWITCH_ADAPTIVE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_ADAPTIVE).assertIsOff()
        setInput(TAG_SLIDER_VISUAL_RADIUS, "32")
        Thread.sleep(500)

        val cluster = awaitCluster()
        val radius = cluster.clusterIconRadius
        assertNotNull(radius)
        assertEquals(ClusterOptions.IconRadius.MODE_FIXED, radius!!.mode)
        // fixedDp 会换算为 icon-size 比例（约 radiusDp / 自然半径）
        assertTrue(radius.radius > 1f)
        assertEquals(32f / 20f, radius.radius, 0.3f)
    }

    @Test
    fun testClusterStyle_customIconAdvancedStyles() {
        clickTagged(TAG_CLUSTER_STYLE_TOGGLE)
        clickTagged(TAG_SWITCH_CUSTOM_ICON)
        Thread.sleep(600)

        val cluster = awaitCluster()
        val styles = cluster.clusterAdvancedStyles
        assertNotNull(styles)
        assertEquals(3, styles!!.size)

        clickTagged(TAG_SWITCH_ICON_STEPS)
        Thread.sleep(600)
        val stepped = cluster.clusterAdvancedStyles
        assertNotNull(stepped)
        assertEquals(3, stepped!!.size)
    }

    // ==================== 叶子点样式 ====================

    @Test
    fun testLeafStyleSection_liveApplyApi() {
        clickTagged(TAG_LEAF_STYLE_TOGGLE)
        setInput(TAG_SLIDER_LEAF_ICON_SIZE, "1.8")
        setInput(TAG_SLIDER_LEAF_ICON_ROTATION, "45")
        setInput(TAG_SLIDER_LEAF_ICON_OPACITY, "0.7")
        setInput(TAG_SLIDER_POINT_MIN_ZOOM, "3")
        setInput(TAG_SLIDER_POINT_MAX_ZOOM, "18")
        Thread.sleep(800)

        val cluster = awaitCluster()
        awaitClusterReady(cluster)
        assertEquals(3f, cluster.pointLayerMinZoom!!, 0.01f)
        assertEquals(18f, cluster.pointLayerMaxZoom!!, 0.01f)
        val sample = cluster.points.first()
        assertEquals(1.8f, sample.iconSize!!, 0.05f)
        assertEquals(45f, sample.iconRotation!!, 0.5f)
        assertEquals(0.7f, sample.iconOpacity!!, 0.05f)
    }

    @Test
    fun testLeafStyle_fixedIconModeAndShowText() {
        clickTagged(TAG_LEAF_STYLE_TOGGLE)
        clickTagged(TAG_SWITCH_LEAF_ICON_MODE)
        clickTagged(TAG_SWITCH_LEAF_SHOW_TEXT)
        setInput(TAG_SLIDER_LEAF_TEXT_SIZE, "18")
        Thread.sleep(800)

        val cluster = awaitCluster()
        awaitClusterReady(cluster)
        val mode = cluster.pointIconImage?.mode
        assertEquals(ClusterOptions.PointIconImage.MODE_FIXED, mode)
        val withText = cluster.points.firstOrNull { !it.text.isNullOrBlank() }
        assertNotNull(withText)
        assertEquals(18f, withText!!.textSize!!, 0.5f)
    }

    // ==================== 外部数据 / 清除 ====================

    /**
     * 安全路径：不点「使用外部数据」按钮（该按钮已还原为会崩溃的旧并发写法）。
     * 在测试内解析 assets 后 deleteAll + addClusterOverlay 全量重建，验证外部点可加载。
     */
    @Test
    fun testLoadExternal_replacesPoints() {
        val before = awaitCluster()
        assertEquals(DEFAULT_POINT_COUNT, before.points.size)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json =
            context.assets.open("cluster_sample_points.json").bufferedReader().use { it.readText() }
        val leafIcon = ContextCompat.getDrawable(context, R.drawable.default_pin)
            ?.let(BitmapUtils::getBitmapFromDrawable)
        val clusterIcon = ContextCompat.getDrawable(context, R.drawable.blue_dot)
            ?.let(BitmapUtils::getBitmapFromDrawable)
        assertNotNull(leafIcon)
        assertNotNull(clusterIcon)
        val leafBmp = leafIcon!!
        val clusterBmp = clusterIcon!!

        val parse = ClusterGeoJson.parse(json, leafBmp, ClusterGeoJson.DEFAULT_MAX_FEATURES)
        assertTrue("外部 GeoJSON 应解析成功", parse is ClusterGeoJson.Result.Success)
        val points = (parse as ClusterGeoJson.Result.Success).points
        assertEquals(EXTERNAL_POINT_COUNT, points.size)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllClusterOverlays()
            val overlay = engine.addClusterOverlay(
                ClusterOptions().withPoints(points).withDefaultIcons(leafBmp, clusterBmp)
                    .withClusterEnabled(true).withClusterRadius(50).withClusterMaxZoom(16.0)
                    .withClusterMinPoints(2L).withVisible(true),
            )
            assertNotNull(overlay.overlayId)
        }
        Thread.sleep(1_200)

        val after = awaitCluster()
        awaitClusterReady(after)
        assertEquals(EXTERNAL_POINT_COUNT, after.points.size)
    }

    /**
     * 缺陷复现：点击「使用外部数据」走旧写法
     * （同一 Cluster 上 setPoints 后立刻 setCluster* → 并发 registerImages → SIGSEGV）。
     *
     * 期望：进程 native crash（Instrumentation 记为 Process crashed / FAILED）。
     * 请单独跑本方法，勿并入全量套件（会中断后续用例）。
     *
     * 单次：
     * `./gradlew :demo:connectedOpenglReleaseAndroidTest -PuseSource=true -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.PointClusterBasicScreenTest#testError_loadExternal_setPointsThenSetCluster_nativeCrash`
     *
     * 重复 10 次：
     * `for i in {1..10}; do ./gradlew :demo:connectedOpenglReleaseAndroidTest -PuseSource=true -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.PointClusterBasicScreenTest#testError_loadExternal_setPointsThenSetCluster_nativeCrash; done`
     */
    @Test
    fun testError_loadExternal_setPointsThenSetCluster_nativeCrash() {

        val cluster = awaitCluster()
        awaitClusterReady(cluster)
        assertEquals(DEFAULT_POINT_COUNT, cluster.points.size)

        // 触发 Demo 旧并发路径（LoadExternalPointClusterButton）
        clickLoadExternal()
        Thread.sleep(3_000)

    }

    @Test
    fun testRemoveButton_marksClusterRemoved() {
        val cluster = awaitCluster()
        assertFalse(cluster.isRemoved)
        assertEquals(Cluster.LifecycleState.READY, cluster.lifecycleState)

        clickRemove()
        assertTrue(cluster.isRemoved)
        assertEquals(Cluster.LifecycleState.REMOVED, cluster.lifecycleState)
        assertNoActiveClusterHit()
    }

    @Test
    fun testMapStyleSwitch_redrawsCluster() {
        val before = awaitCluster()
        val beforeId = before.id
        val beforeOverlayId = before.overlayId
        awaitClusterReady(before)

        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).performScrollTo().assertIsOff()
        clickTagged(TAG_SWITCH_MAP_STYLE)
        composeTestRule.onNodeWithTag(TAG_SWITCH_MAP_STYLE).assertIsOn()

        // 开关在 setStyle 期间 disabled；等 onStyleRendered / onFailed 后再查句柄。
        // 深色样式首次下载在 CI 上常超过 15s，不能只靠地理拾取。
        composeTestRule.waitUntil(TIMEOUT_STYLE_MS) {
            composeTestRule.onAllNodesWithTag(TAG_SWITCH_MAP_STYLE)
                .fetchSemanticsNodes()
                .any { !it.config.contains(SemanticsProperties.Disabled) }
        }

        val deadline = System.currentTimeMillis() + TIMEOUT_STYLE_MS
        var redrawn: Cluster? = null
        var lastSnapshot = "[]"
        while (System.currentTimeMillis() < deadline) {
            EffectTestHelper.withEngine(mapView) { engine ->
                val active = engine.clusterOverlays.filter { !it.isRemoved }
                lastSnapshot = active.joinToString {
                    "id=${it.id} oid=${it.overlayId} state=${it.lifecycleState}"
                }
                redrawn = active.firstOrNull { candidate ->
                    candidate.id != beforeId || candidate.overlayId != beforeOverlayId
                }
            }
            val current = redrawn
            if (current != null &&
                (current.lifecycleState == Cluster.LifecycleState.READY ||
                    current.lifecycleState == Cluster.LifecycleState.EMPTY)
            ) {
                break
            }
            Thread.sleep(300)
        }
        assertNotNull(
            "深色样式切换后应重建点聚合；beforeId=$beforeId beforeOid=$beforeOverlayId " +
                "beforeRemoved=${before.isRemoved} overlays=[$lastSnapshot]",
            redrawn,
        )
        awaitClusterReady(redrawn!!)
        assertEquals(DEFAULT_POINT_COUNT, redrawn!!.points.size)
        verifyCameraTarget()
    }

    @Test
    fun testDrawAfterRemove_createsNewCluster() {
        val first = awaitCluster()
        clickRemove()
        assertTrue(first.isRemoved)

        clickDraw()
        val second = awaitCluster()
        awaitClusterReady(second)
        assertFalse(second.isRemoved)
        assertEquals(DEFAULT_POINT_COUNT, second.points.size)
        assertTrue(second.overlayId != first.overlayId || second.id != first.id)
    }

    // ==================== 簇点击放大 ====================

    @Test
    fun testClusterClick_zoomsInTowardCluster() {
        val cluster = awaitCluster()
        awaitClusterReady(cluster)
        verifyCameraTarget()

        // Demo 默认 CALLBACK_ONLY；本用例验证 ZOOM_TO_EXPANSION（约 +1 zoom）
        EffectTestHelper.withEngine(mapView) {
            cluster.setClusterClickBehavior(ClusterClickBehavior.ZOOM_TO_EXPANSION)
        }

        val zoomBefore = currentZoom()
        assertTrue("初始 zoom 应有效", !zoomBefore.isNaN())

        // 多试几次：聚合图标像素位置可能略偏锚点
        var zoomAfter = zoomBefore
        repeat(3) {
            clickMapAt(CLUSTER_QUERY)
            // animateCameraTo 需要时间落到目标 zoom
            val deadline = System.currentTimeMillis() + 3_000L
            while (System.currentTimeMillis() < deadline) {
                zoomAfter = currentZoom()
                if (zoomAfter > zoomBefore + 0.3) break
                Thread.sleep(150)
            }
            if (zoomAfter > zoomBefore + 0.3) return@repeat
        }

        assertTrue(
            "簇点击应触发放大：before=$zoomBefore after=$zoomAfter",
            zoomAfter > zoomBefore + 0.3,
        )
        assertTrue(
            "单次点击放大约 +1（允许动画误差）",
            zoomAfter <= zoomBefore + 1.5,
        )
        composeTestRule.expandConfigPanel()
    }
}
