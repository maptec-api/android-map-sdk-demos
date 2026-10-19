package com.maptec.applied.demo

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.ensureMapRenderDemoReady
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.OnOverlayClickListener
import com.maptec.applied.maps.overlay.OnOverlayDragListener
import com.maptec.applied.maps.overlay.OnOverlayLongClickListener
import com.maptec.applied.maps.overlay.circle.Circle
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.Fill
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.OnMarkerClickListener
import com.maptec.applied.maps.overlay.marker.OnMarkerDragListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Overlay 通用能力（test-plan MO-G*）：显隐、批量增删、自定义 data、点击 / 长按 / 拖拽监听。
 *
 * 载体：基础地图 → 地图渲染。须在模拟器跑通。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class OverlayInteractionEffectTest {

    companion object {
        private const val TAG_MAP = "mapView"
        private val CENTER = LatLng(1.32, 103.82)
        private val RING = listOf(
            LatLng(1.321, 103.815),
            LatLng(1.321, 103.825),
            LatLng(1.315, 103.825),
            LatLng(1.315, 103.815),
            LatLng(1.321, 103.815),
        )
        private val LINE = listOf(
            LatLng(1.318, 103.810),
            LatLng(1.322, 103.820),
            LatLng(1.318, 103.830),
        )
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        composeTestRule.ensureMapRenderDemoReady()
        mapView = composeTestRule.getMapView()
        EffectTestHelper.withMapApi(mapView) { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTER, 14.0))
        }
        Thread.sleep(200)
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    /** MapView 本地坐标（[MapView.dispatchTouchEvent] 用）。 */
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
        // 点击沿用 Compose 路径（与既有 click 用例一致）
        composeTestRule.onNodeWithTag(TAG_MAP).performTouchInput { click(offset) }
        composeTestRule.settleForIdle()
        Thread.sleep(600)
    }

    /** 直接向 MapView 派发触摸，避免 Compose 手势与地图原生长按/拖拽冲突。 */
    private fun dispatchOnMapView(block: (MapView) -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            block(mapView)
        }
    }

    private fun obtain(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float): MotionEvent =
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0)

    private fun longClickMapAt(latLng: LatLng) {
        composeTestRule.collapseConfigPanel()
        composeTestRule.settleForIdle()
        val p = mapLocalPointOf(latLng)
        dispatchOnMapView { view ->
            val downTime = SystemClock.uptimeMillis()
            view.dispatchTouchEvent(obtain(downTime, downTime, MotionEvent.ACTION_DOWN, p.x, p.y))
        }
        Thread.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 400L)
        dispatchOnMapView { view ->
            val now = SystemClock.uptimeMillis()
            view.dispatchTouchEvent(obtain(now, now, MotionEvent.ACTION_UP, p.x, p.y))
        }
        composeTestRule.settleForIdle()
        Thread.sleep(600)
    }

    /**
     * [holdForLongPress]=true：Marker 长按进入拖拽后再移动。
     * false：Shape 直接滑动触发 move-begin 拖拽。
     */
    private fun dragMapFromTo(from: LatLng, to: LatLng, holdForLongPress: Boolean) {
        composeTestRule.collapseConfigPanel()
        composeTestRule.settleForIdle()
        val start = mapLocalPointOf(from)
        val end = mapLocalPointOf(to)
        val steps = 24
        val moveDurationMs = 500L

        val downTime = SystemClock.uptimeMillis()
        dispatchOnMapView { view ->
            view.dispatchTouchEvent(obtain(downTime, downTime, MotionEvent.ACTION_DOWN, start.x, start.y))
        }
        if (holdForLongPress) {
            Thread.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 400L)
        } else {
            Thread.sleep(50)
        }
        dispatchOnMapView { view ->
            for (i in 1..steps) {
                val progress = i / steps.toFloat()
                val x = start.x + (end.x - start.x) * progress
                val y = start.y + (end.y - start.y) * progress
                val eventTime = downTime + (moveDurationMs * progress).toLong() +
                    if (holdForLongPress) ViewConfiguration.getLongPressTimeout() + 400L else 50L
                view.dispatchTouchEvent(obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y))
            }
            val upTime = SystemClock.uptimeMillis()
            view.dispatchTouchEvent(obtain(downTime, upTime, MotionEvent.ACTION_UP, end.x, end.y))
        }
        composeTestRule.settleForIdle()
        Thread.sleep(800)
    }

    private fun waitUntilAtLeast(counter: AtomicInteger, min: Int, timeoutMs: Long = 8_000L): Int {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && counter.get() < min) {
            Thread.sleep(200)
        }
        return counter.get()
    }

    // ==================== MO-G01 visible ====================

    @Test
    fun moG01_visibleToggle_marker() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            val m = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(2f)
                    .withVisible(true),
            )!!
            assertTrue(m.isVisible)
            m.setVisible(false)
            assertFalse(m.isVisible)
            m.setVisible(true)
            assertTrue(m.isVisible)
        }
    }

    // ==================== MO-G03 data ====================

    @Test
    fun moG03_customData_roundTrip() {
        val payload = JsonParser.parseString("""{"scene":"mo-g03","n":3}""")
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            engine.deleteAllLines()
            engine.deleteAllFills()
            val circle = engine.addCircle(
                CircleOptions().withCenter(CENTER).withRadius(80.0).withData(payload),
            )!!
            assertEquals("mo-g03", circle.data!!.asJsonObject.get("scene").asString)
            assertEquals(3, circle.data!!.asJsonObject.get("n").asInt)

            val line = engine.addPolyline(
                LineOptions().withLatLngs(LINE).withStrokeWeight(4f).withData(payload),
            )!!
            assertEquals("mo-g03", line.data!!.asJsonObject.get("scene").asString)

            val fill = engine.addPolygon(
                FillOptions().withLatLngs(listOf(RING)).withFillOpacity(0.3f).withData(payload),
            )!!
            assertEquals("mo-g03", fill.data!!.asJsonObject.get("scene").asString)
        }
    }

    // ==================== MO-G04 批量 ====================

    @Test
    fun moG04_batchAddAndDelete() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            engine.deleteAllCircles()
            val markers = (0 until 5).map { i ->
                engine.addMarker(
                    MarkerOptions()
                        .withLatLng(LatLng(CENTER.latitude + i * 0.001, CENTER.longitude))
                        .withIconResource(R.drawable.default_pin, context)
                        .withIconSize(1.5f),
                )
            }
            assertEquals(5, markers.size)
            assertEquals(5, engine.markers.size)

            val circles = engine.addCircles(
                (0 until 3).map { i ->
                    CircleOptions()
                        .withCenter(LatLng(CENTER.latitude, CENTER.longitude + i * 0.002))
                        .withRadius(50.0 + i * 10)
                },
            )
            assertEquals(3, circles.size)

            engine.deleteAllMarkers()
            assertEquals(0, engine.markers.size)
            engine.deleteAllCircles()
        }
    }

    // ==================== MO-G02 点击 / 长按 / 拖拽监听 ====================

    @Test
    fun moG02_circleClickListener_fires() {
        val clicks = AtomicInteger(0)
        val clickedId = AtomicReference<Long?>(null)
        var expectedId = -1L
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            engine.deleteAllMarkers()
            val circle = engine.addCircle(
                CircleOptions()
                    .withCenter(CENTER)
                    .withRadius(300.0)
                    .withFillColor("#3388FF")
                    .withFillOpacity(0.55f),
            )!!
            circle.setClickable(true)
            expectedId = circle.id
            circle.addOnMapClickListener(
                OnOverlayClickListener<Circle> { c ->
                    clicks.incrementAndGet()
                    clickedId.set(c.id)
                    true
                },
            )
        }
        Thread.sleep(500)
        clickMapAt(CENTER)
        assertTrue("Circle 点击监听应触发，实际=${clicks.get()}", waitUntilAtLeast(clicks, 1) >= 1)
        assertEquals(expectedId, clickedId.get())
    }

    @Test
    fun moG02_markerClickListener_fires() {
        val clicks = AtomicInteger(0)
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            engine.deleteAllCircles()
            val marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(3.0f)
                    .withClickable(true),
            )!!
            marker.addOnClickListener(
                OnMarkerClickListener {
                    clicks.incrementAndGet()
                    true
                },
            )
            assertNotNull(marker.id)
        }
        Thread.sleep(500)
        clickMapAt(CENTER)
        assertTrue("Marker 点击监听应触发，实际=${clicks.get()}", waitUntilAtLeast(clicks, 1) >= 1)
    }

    @Test
    fun moG02_circleLongClickListener_fires() {
        val longClicks = AtomicInteger(0)
        val hitId = AtomicReference<Long?>(null)
        var expectedId = -1L
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            engine.deleteAllMarkers()
            val circle = engine.addCircle(
                CircleOptions()
                    .withCenter(CENTER)
                    .withRadius(300.0)
                    .withFillColor("#33AA66")
                    .withFillOpacity(0.55f),
            )!!
            circle.setClickable(true)
            expectedId = circle.id
            circle.addOnMapLongClickListener(
                OnOverlayLongClickListener<Circle> { c ->
                    longClicks.incrementAndGet()
                    hitId.set(c.id)
                    true
                },
            )
        }
        Thread.sleep(500)
        longClickMapAt(CENTER)
        assertTrue("Circle 长按监听应触发，实际=${longClicks.get()}", waitUntilAtLeast(longClicks, 1) >= 1)
        assertEquals(expectedId, hitId.get())
    }

    @Test
    fun moG02_markerDragListener_fires() {
        val started = AtomicInteger(0)
        val dragged = AtomicInteger(0)
        val finished = AtomicInteger(0)
        val endLatLng = AtomicReference<LatLng?>(null)
        val dragTarget = LatLng(CENTER.latitude + 0.004, CENTER.longitude + 0.004)
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            engine.deleteAllCircles()
            val marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(3.5f)
                    .withDraggable(true)
                    .withClickable(true),
            )!!
            marker.addOnDragListener(object : OnMarkerDragListener {
                override fun onMarkerDragStarted(marker: Marker) {
                    started.incrementAndGet()
                }

                override fun onMarkerDrag(marker: Marker) {
                    dragged.incrementAndGet()
                }

                override fun onMarkerDragFinished(marker: Marker) {
                    finished.incrementAndGet()
                    endLatLng.set(marker.latLng)
                }
            })
            assertTrue(marker.isDraggable)
        }
        Thread.sleep(500)
        dragMapFromTo(CENTER, dragTarget, holdForLongPress = true)
        assertTrue("Marker dragStarted 应触发，实际=${started.get()}", waitUntilAtLeast(started, 1) >= 1)
        assertTrue("Marker dragFinished 应触发，实际=${finished.get()}", waitUntilAtLeast(finished, 1) >= 1)
        assertTrue("Marker drag 过程应至少回调一次，实际=${dragged.get()}", dragged.get() >= 1)
        val end = endLatLng.get()
        assertNotNull(end)
        assertTrue(
            "拖拽后坐标应离开起点，end=$end",
            kotlin.math.abs(end!!.latitude - CENTER.latitude) > 0.0005 ||
                kotlin.math.abs(end.longitude - CENTER.longitude) > 0.0005,
        )
    }

    @Test
    fun moG02_circleDragListener_fires() {
        val started = AtomicInteger(0)
        val dragged = AtomicInteger(0)
        val finished = AtomicInteger(0)
        val endCenter = AtomicReference<LatLng?>(null)
        val dragTarget = LatLng(CENTER.latitude + 0.005, CENTER.longitude + 0.005)
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            engine.deleteAllMarkers()
            val circle = engine.addCircle(
                CircleOptions()
                    .withCenter(CENTER)
                    .withRadius(280.0)
                    .withFillColor("#3388FF")
                    .withFillOpacity(0.6f)
                    .withDraggable(true),
            )!!
            circle.setClickable(true)
            circle.addOnDragListener(object : OnOverlayDragListener<Circle> {
                override fun onAnnotationDragStarted(t: Circle) {
                    started.incrementAndGet()
                }

                override fun onAnnotationDrag(t: Circle) {
                    dragged.incrementAndGet()
                }

                override fun onAnnotationDragFinished(t: Circle) {
                    finished.incrementAndGet()
                    endCenter.set(t.center)
                }
            })
            assertTrue(circle.isDraggable)
        }
        Thread.sleep(500)
        dragMapFromTo(CENTER, dragTarget, holdForLongPress = false)
        assertTrue("Circle dragStarted 应触发，实际=${started.get()}", waitUntilAtLeast(started, 1) >= 1)
        assertTrue("Circle dragFinished 应触发，实际=${finished.get()}", waitUntilAtLeast(finished, 1) >= 1)
        assertTrue("Circle drag 过程应至少回调一次，实际=${dragged.get()}", dragged.get() >= 1)
        val end = endCenter.get()
        assertNotNull(end)
        assertTrue(
            "拖拽后圆心应离开起点，end=$end",
            kotlin.math.abs(end!!.latitude - CENTER.latitude) > 0.0005 ||
                kotlin.math.abs(end.longitude - CENTER.longitude) > 0.0005,
        )
    }

    @Test
    fun moG02_fillAndLine_clickableListeners_register() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllFills()
            engine.deleteAllLines()
            val fill = engine.addPolygon(
                FillOptions()
                    .withLatLngs(listOf(RING))
                    .withFillOpacity(0.4f)
                    .withData(JsonParser.parseString("""{"t":"fill"}""")),
            )!!
            fill.setClickable(true)
            fill.addOnMapClickListener(OnOverlayClickListener<Fill> { true })
            fill.addOnMapLongClickListener(OnOverlayLongClickListener<Fill> { true })
            fill.addOnDragListener(object : OnOverlayDragListener<Fill> {
                override fun onAnnotationDragStarted(t: Fill) = Unit
                override fun onAnnotationDrag(t: Fill) = Unit
                override fun onAnnotationDragFinished(t: Fill) = Unit
            })
            val line = engine.addPolyline(
                LineOptions()
                    .withLatLngs(LINE)
                    .withStrokeWeight(10f)
                    .withData(JsonParser.parseString("""{"t":"line"}""")),
            )!!
            line.setClickable(true)
            line.addOnMapClickListener(OnOverlayClickListener<Line> { true })
            line.addOnMapLongClickListener(OnOverlayLongClickListener<Line> { true })
            line.addOnDragListener(object : OnOverlayDragListener<Line> {
                override fun onAnnotationDragStarted(t: Line) = Unit
                override fun onAnnotationDrag(t: Line) = Unit
                override fun onAnnotationDragFinished(t: Line) = Unit
            })
            assertEquals("fill", fill.data!!.asJsonObject.get("t").asString)
            assertEquals("line", line.data!!.asJsonObject.get("t").asString)
            assertTrue(fill.isClickable)
            assertTrue(line.isClickable)
            assertTrue(fill.hasMapLongClickListeners())
            assertTrue(line.hasMapLongClickListeners())
        }
    }

    /**
     * MO-G06：多类型属性交叉组合（L1）。
     * Marker / Line / Fill / Circle 同屏叠加丰富属性后回读，并完成显隐与清理，不崩溃。
     */
    @Test
    fun moG06_crossAttributeCombo_roundTrip() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            engine.deleteAllLines()
            engine.deleteAllFills()
            engine.deleteAllCircles()

            val marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(1.8f)
                    .withIconOpacity(0.85f)
                    .withIconRotation(30f)
                    .withIconOffset(2f, -4f)
                    .withText("g06")
                    .withTextSize(12)
                    .withTextColor(0xFF0066FF.toInt())
                    .withVisible(true)
                    .withClickable(true)
                    .withDraggable(false),
            )!!
            assertEquals(1.8f, marker.iconSize, 0.01f)
            assertEquals(0.85f, marker.iconOpacity, 0.01f)
            assertEquals("g06", marker.text)
            assertTrue(marker.isVisible)
            assertTrue(marker.isClickable)

            val line = engine.addPolyline(
                LineOptions()
                    .withLatLngs(LINE)
                    .withStrokeWeight(6f)
                    .withStrokeOpacity(0.9f)
                    .withStrokeColor("#00AA88")
                    .withStrokeDashArray(floatArrayOf(10f, 5f))
                    .withGlowEffect(true)
                    .withGlowRadius(12f)
                    .withGlowColor("#FFAA00")
                    .withMaskColor("#112233")
                    .withLineCapStart(com.maptec.applied.maps.overlay.line.LineCapType.ROUND)
                    .withLineCapEnd(com.maptec.applied.maps.overlay.line.LineCapType.SQUARE)
                    .withData(JsonParser.parseString("""{"combo":"line"}"""))
                    .withOverlayId("g06_l"),
            )!!
            line.setClickable(true)
            assertEquals(6f, line.strokeWeight!!, 0.01f)
            assertEquals(12f, line.glowRadius!!, 0.01f)
            assertEquals(2, line.strokeDashArray.size)
            assertEquals("#112233", line.maskColor)
            assertTrue(line.isClickable)
            assertEquals("line", line.data!!.asJsonObject.get("combo").asString)

            val fill = engine.addPolygon(
                FillOptions()
                    .withLatLngs(listOf(RING))
                    .withFillOpacity(0.35f)
                    .withFillColor("#3344AA")
                    .withStrokeWeight(3f)
                    .withStrokeOpacity(0.8f)
                    .withStrokeColor("#FFFFFF")
                    .withFillTranslate(android.graphics.PointF(4f, -6f))
                    .withBlendMode("normal")
                    .withData(JsonParser.parseString("""{"combo":"fill"}"""))
                    .withOverlayId("g06_f"),
            )!!
            fill.setClickable(true)
            assertEquals(0.35f, fill.fillOpacity!!, 0.01f)
            assertEquals(3f, fill.strokeWeight!!, 0.01f)
            assertTrue(fill.isClickable)
            assertEquals("fill", fill.data!!.asJsonObject.get("combo").asString)

            val circle = engine.addCircle(
                CircleOptions()
                    .withCenter(LatLng(CENTER.latitude - 0.002, CENTER.longitude + 0.002))
                    .withRadius(120.0)
                    .withSegments(64)
                    .withFillColor("#88CCFF")
                    .withFillOpacity(0.4f)
                    .withStrokeColor("#004488")
                    .withStrokeWeight(2f)
                    .withStrokeOpacity(0.9f)
                    .withInnerShadow(true)
                    .withInnerShadowColor("#000000")
                    .withInnerShadowOpacity(0.3f)
                    .withInnerShadowBlur(8f)
                    .withOuterGlow(true)
                    .withGlowColor("#00FFFF")
                    .withGlowRadius(10f)
                    .withScanEnabled(true)
                    .withScanSectorAngle(45f)
                    .withScanSectorColor("#FF00AA")
                    .withData(JsonParser.parseString("""{"combo":"circle"}"""))
                    .withOverlayId("g06_c"),
            )!!
            circle.setClickable(true)
            assertEquals(120.0, circle.radius!!, 0.01)
            assertEquals(64, circle.segments!!)
            assertTrue(circle.innerShadow!!)
            assertTrue(circle.outerGlow!!)
            assertEquals(10f, circle.glowRadius!!, 0.01f)
            assertTrue(circle.isClickable)
            assertEquals("circle", circle.data!!.asJsonObject.get("combo").asString)

            // 交叉态：显隐切换 + 组内 zOrder，确认不崩溃且回读一致
            marker.setVisible(false)
            assertFalse(marker.isVisible)
            marker.setVisible(true)
            assertTrue(marker.isVisible)
            assertTrue(engine.setOverlayZOrder(com.maptec.applied.maps.overlay.OverlayKind.LINE, "g06_l", 2))
            assertTrue(engine.setOverlayZOrder(com.maptec.applied.maps.overlay.OverlayKind.FILL, "g06_f", 1))
            assertTrue(engine.setOverlayZOrder(com.maptec.applied.maps.overlay.OverlayKind.CIRCLE, "g06_c", 4))

            engine.deleteAllMarkers()
            engine.deleteAllLines()
            engine.deleteAllFills()
            engine.deleteAllCircles()
            assertEquals(0, engine.markers.size)
            assertFalse(engine.hasNativeOverlay(com.maptec.applied.maps.overlay.OverlayKind.LINE, "g06_l"))
            assertFalse(engine.hasNativeOverlay(com.maptec.applied.maps.overlay.OverlayKind.FILL, "g06_f"))
            assertFalse(engine.hasNativeOverlay(com.maptec.applied.maps.overlay.OverlayKind.CIRCLE, "g06_c"))
        }
    }
}
