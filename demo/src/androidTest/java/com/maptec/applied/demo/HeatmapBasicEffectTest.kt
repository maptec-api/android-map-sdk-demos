package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.runOnBackgroundThread
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.heatmap.HeatmapPoint
import com.maptec.applied.maps.overlay.heatmap.HeatmapTileOptions
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 不使用 GrantPermissionRule：避免 UiAutomation already registered。
 */
@RunWith(AndroidJUnit4::class)
class HeatmapBasicEffectTest {

    companion object {
        private const val TAG_RADIUS = "heatmap_slider_radius"
        private const val TAG_WEIGHT = "heatmap_slider_weight"
        private const val TAG_INTENSITY = "heatmap_slider_intensity"
        private const val TAG_OPACITY = "heatmap_slider_opacity"
        private const val TAG_MIN_ZOOM = "heatmap_slider_min_zoom"
        private const val TAG_MAX_ZOOM = "heatmap_slider_max_zoom"
        private const val TAG_DRAW = "heatmap_draw_button"
        private const val TAG_LOAD_EXTERNAL = "heatmap_load_external_button"

        private const val DEMO_LAT = 1.315
        private const val DEMO_LNG = 103.82
        private const val DEMO_ZOOM = 11.15

        private const val TIMEOUT_CAMERA_MS = 8000L
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航与辅助 ====================

    private fun navigateToScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_heatmap_basic)
        composeTestRule.waitForIdle()
    }

    private fun prepareMap(): MapView {
        composeTestRule.waitForMapDemoReady()
        return composeTestRule.getMapView()
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

    private fun verifyCameraTarget(
        mapView: MapView,
        expectedLat: Double,
        expectedLng: Double,
        expectedZoom: Double,
        timeoutMs: Long = TIMEOUT_CAMERA_MS,
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

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    private fun clickDraw() {
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
    }

    // ==================== 默认自动绘制 ====================

    @Test
    fun testDefaultHeatmapAutoDrawn_cameraPositionCorrect() {
        navigateToScreen()
        val mapView = prepareMap()
        verifyCameraTarget(mapView, DEMO_LAT, DEMO_LNG, DEMO_ZOOM)
    }

    @Test
    fun testDrawButton_defaultValues_cameraPositionCorrect() {
        navigateToScreen()
        val mapView = prepareMap()
        clickDraw()
        verifyCameraTarget(mapView, DEMO_LAT, DEMO_LNG, DEMO_ZOOM)
    }

    // ==================== 输入有效值 ====================

    @Test
    fun testSliderRadius_validValue_drawButtonEnabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_RADIUS, "40")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testSliderWeight_validValue_drawButtonEnabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_WEIGHT, "0.5")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testSliderIntensity_validValue_drawButtonEnabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_INTENSITY, "5")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    @Test
    fun testSliderOpacity_validValue_drawButtonEnabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_OPACITY, "0.5")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    // ==================== 输入无效值 ====================

    @Test
    fun testInvalidRadius_drawButtonDisabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_RADIUS, "100")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testInvalidWeight_drawButtonDisabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_WEIGHT, "2")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testInvalidOpacity_drawButtonDisabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_OPACITY, "-1")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testInvalidIntensity_drawButtonDisabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_INTENSITY, "-1")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testMinZoomGreaterThanMaxZoom_drawButtonDisabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_MIN_ZOOM, "15")
        setInput(TAG_MAX_ZOOM, "10")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun testRecoverFromInvalidInput_drawButtonEnabled() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_RADIUS, "100")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsNotEnabled()
        setInput(TAG_RADIUS, "25")
        composeTestRule.onNodeWithTag(TAG_DRAW).performScrollTo().assertIsEnabled()
    }

    // ==================== 绘制与行为 ====================

    @Test
    fun testDrawButton_drawsWithCustomSettings_noCrash() {
        navigateToScreen()
        val mapView = prepareMap()
        setInput(TAG_RADIUS, "20")
        setInput(TAG_WEIGHT, "0.8")
        setInput(TAG_INTENSITY, "3")
        setInput(TAG_OPACITY, "0.6")
        setInput(TAG_MIN_ZOOM, "5")
        setInput(TAG_MAX_ZOOM, "18")
        clickDraw()
        verifyCameraTarget(mapView, DEMO_LAT, DEMO_LNG, DEMO_ZOOM)
    }

    @Test
    fun testLoadExternalButton_drawsFromAssets_noCrash() {
        navigateToScreen()
        val mapView = prepareMap()
        composeTestRule.onNodeWithTag(TAG_LOAD_EXTERNAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        verifyCameraTarget(mapView, DEMO_LAT, DEMO_LNG, DEMO_ZOOM)
    }

    @Test
    fun testLivePreviewChangeRadius_noCrash() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_RADIUS, "45")
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    @Test
    fun testLivePreviewChangeAllParams_noCrash() {
        navigateToScreen()
        prepareMap()
        setInput(TAG_RADIUS, "30")
        setInput(TAG_WEIGHT, "0.6")
        setInput(TAG_INTENSITY, "4")
        setInput(TAG_OPACITY, "0.7")
        setInput(TAG_MIN_ZOOM, "3")
        setInput(TAG_MAX_ZOOM, "20")
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    // ==================== Overlay 多线程（子线程增删改 / 并发安全） ====================

    private fun obtainMap(mapView: MapView): MaptecMap {
        var mapRef: MaptecMap? = null
        withMapApi(mapView) { mapRef = it }
        return requireNotNull(mapRef)
    }

    /**
     * 子线程可直调 HeatmapTile 增 / 改 / 删。
     */
    @Test
    fun testWorkerThread_addUpdateDeleteHeatmap_apiVerified() {
        navigateToScreen()
        val mapView = prepareMap()
        val map = obtainMap(mapView)

        runOnBackgroundThread {
            val engine = map.overlayEngine
            engine.deleteAllTileOverlays()
            val tile = engine.addTileOverlay(
                HeatmapTileOptions()
                    .withPoints(
                        listOf(
                            HeatmapPoint(LatLng(DEMO_LAT, DEMO_LNG), 1f),
                            HeatmapPoint(LatLng(DEMO_LAT + 0.01, DEMO_LNG + 0.01), 0.6f),
                        ),
                    )
                    .withHeatmapRadius(30f)
                    .withHeatmapIntensity(1f)
                    .withHeatmapOpacity(0.8f),
            )
            assertNotNull(tile.overlayId)
            tile.setHeatmapRadius(40f)
            tile.setHeatmapIntensity(2f)
            engine.deleteTileOverlay(tile)
        }
    }

    /**
     * 多线程串行化各自创建不同 Heatmap（每轮先删再建）：并发下不应闪退。
     * 注：同一时刻多 tile 叠加热力源允许弱一致，但不得崩溃。
     */
    @Test
    fun testConcurrentThreads_heatmapAddUpdateDelete_noCrash() {
        navigateToScreen()
        val mapView = prepareMap()
        val map = obtainMap(mapView)
        val threadCount = 3
        val roundsPerThread = 4
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
                        val lat = DEMO_LAT + workerIndex * 0.02 + round * 0.001
                        val lng = DEMO_LNG + workerIndex * 0.02 + round * 0.001
                        val tile = engine.addTileOverlay(
                            HeatmapTileOptions()
                                .withPoints(listOf(HeatmapPoint(LatLng(lat, lng), 0.8f)))
                                .withHeatmapRadius(20f + round),
                        )
                        tile.setHeatmapOpacity(0.5f)
                        engine.deleteTileOverlay(tile)
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
        assertTrue("并发 Heatmap 压测超时", doneLatch.await(60, TimeUnit.SECONDS))
        pool.shutdownNow()
        if (errors.isNotEmpty()) {
            fail("并发 Heatmap 出现异常: ${errors.first()}")
        }
        assertEquals(threadCount, completed.get())
        withMapApi(mapView) { map.overlayEngine.deleteAllTileOverlays() }
    }
}
