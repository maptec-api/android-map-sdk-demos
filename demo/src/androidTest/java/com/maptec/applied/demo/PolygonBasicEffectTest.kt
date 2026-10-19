package com.maptec.applied.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.runOnBackgroundThread
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.gestures.MoveDistancesObject
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * [PolygonBasicScreen] 功能效果类测试（API 验证）：基础多边形绘制与导航。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class PolygonBasicEffectTest {

    companion object {
        private const val TAG_COORDINATES = "fill_input_coordinates"
        private const val TAG_COLOR = "fill_input_color"
        private const val TAG_OUTLINE_COLOR = "fill_input_outline_color"
        private const val TAG_DRAW_BUTTON = "fill_draw_button"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @After
    fun tearDown() {
        composeTestRule.resetToMainCatalog()
        Thread.sleep(500)
    }

    private fun navigateToFillScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.openAnnotationsDemo(R.string.overlay_item_polygon_menu)
        composeTestRule.waitForIdle()
    }

    private fun replaceField(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.mainClock.advanceTimeBy(300)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    private fun withMap(action: (MaptecMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
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
        assertTrue("获取 Map 异步超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    private fun getCameraTarget(): Pair<Double, Double>? {
        val latch = CountDownLatch(1)
        var result: Pair<Double, Double>? = null
        var error: Throwable? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    val pos = map.cameraPosition
                    result = if (pos?.target != null) {
                        Pair(pos.target!!.latitude, pos.target!!.longitude)
                    } else null
                } catch (e: Throwable) {
                    error = e
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("获取相机位置超时", latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
        return result
    }

    private fun drawAndVerifyCameraMoved(): Pair<Double, Double> {
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo()
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).assertIsEnabled()
        val initialTarget = getCameraTarget()

        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performClick()
        composeTestRule.waitUntil(10_000) {
            val current = getCameraTarget()
            current != null && initialTarget != null &&
                (current.first != initialTarget.first || current.second != initialTarget.second)
        }

        val newTarget = getCameraTarget()
        assertNotNull("初始相机位置不应为空", initialTarget)
        assertNotNull("绘制后相机位置不应为空", newTarget)
        return newTarget!!
    }

    private fun queryFillAt(latLng: LatLng): Fill? {
        var fill: Fill? = null
        withMap { map ->
            val engine = map.getOverlayEngine()
            val screen = map.projection.toScreenLocation(latLng)
            for (hit in engine.queryOverlayHitsFromPoint(screen)) {
                val target = engine.resolveHitTarget(hit)
                if (target is Fill) {
                    fill = target
                    return@withMap
                }
            }
        }
        return fill
    }

    private fun waitForFillAt(latLng: LatLng, timeoutMs: Long = 5000): Fill {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            queryFillAt(latLng)?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Fill not found at $latLng within ${timeoutMs}ms")
    }

    @Test
    fun drawWithDefaultValues_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("默认参数绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithCustomColors_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        replaceField(TAG_COLOR, "#FF5733")
        replaceField(TAG_OUTLINE_COLOR, "#000000")
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().assertIsEnabled()
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("自定义颜色绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithSimpleCoords_movesCamera() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()

        replaceField(
            TAG_COORDINATES,
            "[[39.9,116.3;39.95,116.4;39.9,116.5;39.9,116.3]]"
        )
        val target = drawAndVerifyCameraMoved()
        val fill = waitForFillAt(LatLng(target.first, target.second))
        assertNotNull("简单坐标绘制后应能查询到 Fill 覆盖物", fill)
    }

    @Test
    fun drawWithInvalidInput_drawButtonDisabled() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()

        replaceField(TAG_COLOR, "not-a-color")
        composeTestRule.onNodeWithTag(TAG_DRAW_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun back_returnsToOverlayList() {
        navigateToFillScreen()
        composeTestRule.waitForIdle()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(getTestString(R.string.overlay_item_polygon_menu))
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun doubleBack_returnsToMainScreen() {
        navigateToFillScreen()
        composeTestRule.waitForIdle()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(getTestString(R.string.catalog_main_annotations)).assertIsDisplayed()
    }

    // ==================== Overlay 多线程（子线程增删改 / 并发安全） ====================

    private fun obtainMap(): MaptecMap {
        var mapRef: MaptecMap? = null
        withMap { mapRef = it }
        return requireNotNull(mapRef)
    }

    private fun ensureMapReady() {
        navigateToFillScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    /**
     * 子线程可直调 Fill 增 / 改 / getOffsetGeometry / 显式拾取 / 删。
     */
    @Test
    fun testWorkerThread_addUpdateQueryDeleteFill_apiVerified() {
        ensureMapReady()
        val map = obtainMap()
        val ring = listOf(
            LatLng(39.90, 116.30),
            LatLng(39.95, 116.40),
            LatLng(39.90, 116.50),
            LatLng(39.90, 116.30),
        )

        runOnBackgroundThread {
            val engine = map.overlayEngine
            val fill = engine.addPolygon(
                FillOptions()
                    .withLatLngs(listOf(ring))
                    .withFillColor("#3366FF")
                    .withFillOpacity(0.4f),
            )
            assertNotNull(fill.overlayId)
            fill.setFillColor("#FF6633")
            fill.fillOpacity = 0.7f

            val center = LatLng(39.92, 116.40)
            val screen = map.projection.toScreenLocation(center)
            engine.queryOverlayHitsFromPoint(screen)
            engine.queryOverlayHitsFromPoint(center)

            val move = MoveDistancesObject(screen.x, screen.y)
            move.addNewPosition(screen.x + 15f, screen.y + 15f)
            fill.getOffsetGeometry(map.projection, move, 0f, 0f)

            engine.deleteFill(fill)
        }
    }

    /**
     * 多线程各自操作不同 Fill：并发 add / update / query / delete 不应闪退。
     */
    @Test
    fun testConcurrentThreads_differentFills_addUpdateDelete_noCrash() {
        ensureMapReady()
        val map = obtainMap()
        val threadCount = 4
        val roundsPerThread = 5
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
                        val baseLat = 39.8 + workerIndex * 0.03 + round * 0.001
                        val baseLng = 116.2 + workerIndex * 0.03 + round * 0.001
                        val ring = listOf(
                            LatLng(baseLat, baseLng),
                            LatLng(baseLat + 0.01, baseLng),
                            LatLng(baseLat + 0.01, baseLng + 0.01),
                            LatLng(baseLat, baseLng + 0.01),
                            LatLng(baseLat, baseLng),
                        )
                        val fill = engine.addPolygon(FillOptions().withLatLngs(listOf(ring)))
                        fill.fillOpacity = 0.3f + round * 0.05f
                        engine.queryOverlayHitsFromPoint(LatLng(baseLat + 0.005, baseLng + 0.005))
                        engine.deleteFill(fill)
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
        assertTrue("并发 Fill 压测超时", doneLatch.await(60, TimeUnit.SECONDS))
        pool.shutdownNow()
        if (errors.isNotEmpty()) {
            fail("并发 Fill 出现异常: ${errors.first()}")
        }
        assertEquals(threadCount, completed.get())
        withMap { map.overlayEngine.deleteAllFills() }
    }
}
