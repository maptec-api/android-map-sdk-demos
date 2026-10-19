package com.maptec.applied.demo

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.gson.JsonParser
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.ensureMapRenderDemoReady
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.circle.CircleOptions
import com.maptec.applied.maps.overlay.fill.FillOptions
import com.maptec.applied.maps.overlay.line.LineCapType
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.MarkerLayerOptions
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Overlay 属性 L1（test-plan MO-*）：Options 写入 / 越界截断后 getter 回读。
 *
 * 载体：基础地图 → 地图渲染。须在模拟器跑通。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class OverlayAttributeClampEffectTest {

    companion object {
        private val CENTER = LatLng(1.32, 103.82)
        private val LINE = listOf(
            LatLng(1.320, 103.810),
            LatLng(1.325, 103.820),
            LatLng(1.320, 103.830),
        )
        private val RING = listOf(
            LatLng(1.321, 103.815),
            LatLng(1.321, 103.825),
            LatLng(1.315, 103.825),
            LatLng(1.315, 103.815),
            LatLng(1.321, 103.815),
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
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    // ==================== Line MO-L* ====================

    @Test
    fun moL_strokeWeight_clampedAndApplied() {
        val opts = LineOptions()
            .withLatLngs(LINE)
            .withStrokeWeight(100f) // → 50
            .withStrokeOpacity(-0.5f) // → 0
            .withGlowEffect(true)
            .withGlowRadius(99f) // → 30
        assertEquals(50f, opts.strokeWeight!!, 0.01f)
        assertEquals(0f, opts.strokeOpacity!!, 0.01f)
        assertEquals(30f, opts.glowRadius!!, 0.01f)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(opts)
            assertNotNull(line)
            assertEquals(50f, line!!.strokeWeight!!, 0.01f)
            assertEquals(0f, line.strokeOpacity!!, 0.01f)
            assertEquals(30f, line.glowRadius!!, 0.01f)
        }
    }

    @Test
    fun moL_strokeWeight_lowClamp_andClosed() {
        val opts = LineOptions()
            .withLatLngs(LINE)
            .withStrokeWeight(0.01f) // → 0.1
            .withStrokeOpacity(2f) // → 1
            .withGlowRadius(0.1f) // → 1
            .withIsClosed(true)
        assertEquals(0.1f, opts.strokeWeight!!, 0.01f)
        assertEquals(1f, opts.strokeOpacity!!, 0.01f)
        assertEquals(1f, opts.glowRadius!!, 0.01f)
        assertTrue(opts.isClosed!!)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(opts)!!
            assertEquals(0.1f, line.strokeWeight!!, 0.01f)
            assertTrue(line.isClosed)
        }
    }

    /** MO-L03：虚线 dash 写入 Options / Line 后 L1 回读，并支持运行时 set。 */
    @Test
    fun moL03_strokeDashArray_roundTrip() {
        val dash = floatArrayOf(8f, 4f, 2f, 4f)
        val opts = LineOptions()
            .withLatLngs(LINE)
            .withStrokeWeight(4f)
            .withStrokeDashArray(dash)
        assertArrayEquals(dash, opts.strokeDashArray, 0.01f)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(opts)!!
            assertArrayEquals(dash, line.strokeDashArray, 0.01f)

            val updated = floatArrayOf(12f, 6f)
            line.setStrokeDashArray(updated)
            assertArrayEquals(updated, line.strokeDashArray, 0.01f)

            line.setStrokeDashArray(floatArrayOf())
            assertEquals(0, line.strokeDashArray.size)
        }
    }

    /** MO-L03：linePattern id + bitmap 经 Options 灌入后可回读。 */
    @Test
    fun moL03_linePattern_bitmapAndId_roundTrip() {
        val patternId = "mo_l03_pattern"
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.RED)
        }
        val opts = LineOptions()
            .withLatLngs(LINE)
            .withStrokeWeight(6f)
            .withLinePattern(bmp, patternId)
        assertEquals(patternId, opts.linePattern)
        assertNotNull(opts.linePatternBitmap)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(opts)!!
            assertEquals(patternId, line.linePattern)
            assertNotNull(line.linePatternBitmap)
            assertEquals(bmp.width, line.linePatternBitmap!!.width)
            assertEquals(bmp.height, line.linePatternBitmap!!.height)

            val bmp2 = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(Color.BLUE)
            }
            line.setLinePattern(bmp2, "mo_l03_pattern_v2")
            assertEquals("mo_l03_pattern_v2", line.linePattern)
            assertEquals(4, line.linePatternBitmap!!.width)
        }
    }

    /** MO-L06：maskColor 字符串 / ARGB 回读。 */
    @Test
    fun moL06_maskColor_roundTrip() {
        val opts = LineOptions()
            .withLatLngs(LINE)
            .withStrokeWeight(5f)
            .withMaskColor("#FF3366")
        assertEquals("#FF3366", opts.maskColor)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(opts)!!
            assertEquals("#FF3366", line.maskColor)
            assertEquals(Color.parseColor("#FF3366"), line.maskColorAsInt!!.toInt())

            line.setMaskColor("#00AABB")
            assertEquals("#00AABB", line.maskColor)
            assertEquals(Color.parseColor("#00AABB"), line.maskColorAsInt!!.toInt())

            // int 重载写入 rgba(...) 字符串（非 #hex）；L1 校验字符串回读
            val argb = Color.argb(255, 16, 32, 48)
            line.setMaskColor(argb)
            assertEquals("rgba(16, 32, 48, 1)", line.maskColor)
        }
    }

    /** MO-L06：lineCapStart / lineCapEnd 全量常量 + 运行时切换。 */
    @Test
    fun moL06_lineCapStartEnd_matrix() {
        val caps = listOf(
            LineCapType.NONE,
            LineCapType.ROUND,
            LineCapType.BUTT,
            LineCapType.SQUARE,
        )
        for (start in caps) {
            for (end in caps) {
                val opts = LineOptions()
                    .withLatLngs(LINE)
                    .withStrokeWeight(4f)
                    .withLineCapStart(start)
                    .withLineCapEnd(end)
                assertEquals(start, opts.lineCapStart)
                assertEquals(end, opts.lineCapEnd)

                EffectTestHelper.withEngine(mapView) { engine ->
                    engine.deleteAllLines()
                    val line = engine.addPolyline(opts)!!
                    assertEquals(start, line.lineCapStart)
                    assertEquals(end, line.lineCapEnd)
                }
            }
        }

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            val line = engine.addPolyline(
                LineOptions()
                    .withLatLngs(LINE)
                    .withStrokeWeight(4f)
                    .withLineCapStart(LineCapType.ROUND)
                    .withLineCapEnd(LineCapType.BUTT),
            )!!
            line.setLineCapStart(LineCapType.SQUARE)
            line.setLineCapEnd(LineCapType.NONE)
            assertEquals(LineCapType.SQUARE, line.lineCapStart)
            assertEquals(LineCapType.NONE, line.lineCapEnd)
        }
    }

    // ==================== Fill MO-F* ====================

    @Test
    fun moF_fillOpacityAndStrokeWeight_clamped() {
        val opts = FillOptions()
            .withLatLngs(listOf(RING))
            .withFillOpacity(1.5f) // → 1
            .withStrokeWeight(99f) // → 20
            .withBlendMode("not-a-mode") // → normal
            .withFillPatternMode("bogus") // → solid
        assertEquals(1f, opts.fillOpacity!!, 0.01f)
        assertEquals(20f, opts.strokeWeight!!, 0.01f)
        assertEquals(FillOptions.BLEND_MODE_NORMAL, opts.blendMode)
        assertEquals(FillOptions.PATTERN_SOLID, opts.fillPatternMode)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllFills()
            val fill = engine.addPolygon(opts)!!
            assertEquals(1f, fill.fillOpacity!!, 0.01f)
            assertEquals(20f, fill.strokeWeight!!, 0.01f)
            assertEquals(FillOptions.BLEND_MODE_NORMAL, fill.blendMode)
            assertEquals(FillOptions.PATTERN_SOLID, fill.fillPatternMode)
        }
    }

    @Test
    fun moF_fillOpacity_lowClamp() {
        val opts = FillOptions()
            .withLatLngs(listOf(RING))
            .withFillOpacity(-1f)
            .withStrokeWeight(-5f) // → 0
            .withBlendMode(FillOptions.BLEND_MODE_MULTIPLY)
        assertEquals(0f, opts.fillOpacity!!, 0.01f)
        assertEquals(0f, opts.strokeWeight!!, 0.01f)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllFills()
            val fill = engine.addPolygon(opts)!!
            assertEquals(0f, fill.fillOpacity!!, 0.01f)
            assertEquals(FillOptions.BLEND_MODE_MULTIPLY, fill.blendMode)
        }
    }

    // ==================== Circle MO-C* ====================

    @Test
    fun moC_segmentsAndGlow_clamped() {
        val opts = CircleOptions()
            .withCenter(CENTER)
            .withRadius(200.0)
            .withSegments(2) // → 8
            .withFillOpacity(3f) // → 1
            .withStrokeWeight(100f) // → 20
            .withOuterGlow(true)
            .withGlowRadius(100f) // → 30
        assertEquals(8, opts.segments!!.toInt())
        assertEquals(1f, opts.fillOpacity!!, 0.01f)
        assertEquals(20f, opts.strokeWeight!!, 0.01f)
        assertEquals(30f, opts.glowRadius!!, 0.01f)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            val circle = engine.addCircle(opts)!!
            assertEquals(8, circle.segments!!.toInt())
            assertEquals(1f, circle.fillOpacity!!, 0.01f)
            assertEquals(30f, circle.glowRadius!!, 0.01f)
        }
    }

    @Test
    fun moC_segments_highClamp() {
        val opts = CircleOptions()
            .withCenter(CENTER)
            .withRadius(150.0)
            .withSegments(9999) // → 1024
            .withFillOpacity(-1f)
            .withStrokeOpacity(2f)
        assertEquals(1024, opts.segments!!.toInt())
        assertEquals(0f, opts.fillOpacity!!, 0.01f)
        assertEquals(1f, opts.strokeOpacity!!, 0.01f)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            val circle = engine.addCircle(opts)!!
            assertEquals(1024, circle.segments!!.toInt())
        }
    }

    // ==================== Marker MO-M*（SDK 无 Math 截断，验回读） ====================

    /**
     * MO-M02：图标来源矩阵 L1。
     * - drawable 资源（withIconResource）
     * - 显式 bitmap + id（withIcon）
     * - SDF bitmap（withIcon sdf=true + iconColor）
     * - Style 雪碧图引用（withStyleIcon，无 bitmap）
     */
    @Test
    fun moM02_iconSourceMatrix_optionsAndMarker() {
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.BLUE)
        }
        val sdfBmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.WHITE)
        }

        val resourceOpts = MarkerOptions()
            .withLatLng(CENTER)
            .withIconResource(R.drawable.default_pin, context)
        assertNotNull(resourceOpts.iconId)
        assertTrue(resourceOpts.iconId!!.startsWith("drawable_"))
        assertNotNull(resourceOpts.iconBitmap)
        assertFalse(resourceOpts.isStyleIconReference)
        assertFalse(resourceOpts.isIconSdf)

        val bitmapOpts = MarkerOptions()
            .withLatLng(LatLng(CENTER.latitude + 0.001, CENTER.longitude))
            .withIcon("mo_m02_bitmap", bmp, false)
        assertEquals("mo_m02_bitmap", bitmapOpts.iconId)
        assertNotNull(bitmapOpts.iconBitmap)
        assertFalse(bitmapOpts.isStyleIconReference)
        assertFalse(bitmapOpts.isIconSdf)

        val sdfOpts = MarkerOptions()
            .withLatLng(LatLng(CENTER.latitude + 0.002, CENTER.longitude))
            .withIcon("mo_m02_sdf", sdfBmp, true)
            .withIconColor(Color.GREEN)
        assertEquals("mo_m02_sdf", sdfOpts.iconId)
        assertTrue(sdfOpts.isIconSdf)
        assertFalse(sdfOpts.isStyleIconReference)
        assertEquals(Color.GREEN, sdfOpts.iconColor!!.toInt())

        val styleOpts = MarkerOptions()
            .withLatLng(LatLng(CENTER.latitude + 0.003, CENTER.longitude))
            .withStyleIcon("mo_m02_style_sprite")
        assertEquals("mo_m02_style_sprite", styleOpts.iconId)
        assertNull(styleOpts.iconBitmap)
        assertTrue(styleOpts.isStyleIconReference)
        assertFalse(styleOpts.isIconSdf)

        val styleSdfOpts = MarkerOptions()
            .withLatLng(LatLng(CENTER.latitude + 0.004, CENTER.longitude))
            .withStyleIcon("mo_m02_style_sdf", true)
        assertTrue(styleSdfOpts.isStyleIconReference)
        assertTrue(styleSdfOpts.isIconSdf)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            val fromResource = engine.addMarker(resourceOpts)!!
            assertEquals(resourceOpts.iconId, fromResource.iconId)

            val fromBitmap = engine.addMarker(bitmapOpts)!!
            assertEquals("mo_m02_bitmap", fromBitmap.iconId)

            val fromSdf = engine.addMarker(sdfOpts)!!
            assertEquals("mo_m02_sdf", fromSdf.iconId)
            assertEquals(Color.GREEN, fromSdf.iconColor!!.toInt())

            val fromStyle = engine.addMarker(styleOpts)!!
            assertEquals("mo_m02_style_sprite", fromStyle.iconId)

            val fromStyleSdf = engine.addMarker(styleSdfOpts)!!
            assertEquals("mo_m02_style_sdf", fromStyleSdf.iconId)

            assertEquals(5, engine.markers.size)
        }
    }

    @Test
    fun moM_markerProperties_roundTrip() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            val marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(2.5f)
                    .withIconOpacity(0.7f)
                    .withIconRotation(45f)
                    .withIconOffset(4f, -8f)
                    .withText("clamp-m")
                    .withTextSize(14)
                    .withTextColor(0xFFFF0000.toInt())
                    .withVisible(true)
                    .withDraggable(true)
                    .withClickable(true),
            )
            assertNotNull(marker)
            assertEquals(CENTER.latitude, marker!!.latLng.latitude, 1e-6)
            assertEquals(CENTER.longitude, marker.latLng.longitude, 1e-6)
            assertEquals(2.5f, marker.iconSize, 0.01f)
            assertEquals(0.7f, marker.iconOpacity, 0.01f)
            assertEquals("clamp-m", marker.text)
            assertTrue(marker.isVisible)
            assertTrue(marker.isDraggable)
            assertTrue(marker.isClickable)

            marker.setVisible(false)
            assertFalse(marker.isVisible)
            marker.setVisible(true)
            assertTrue(marker.isVisible)
        }
    }

    // ==================== MZ-07 collision L1 ====================

    @Test
    fun mz07_markerLayerCollision_optionsAndConfigure() {
        val opts = MarkerLayerOptions()
            .withCollisionEnabled(false)
            .withVisible(true)
        assertEquals(false, opts.collisionEnabled)
        assertEquals(true, opts.visible)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.setMarkerLayerCollisionEnabled(false)
            engine.configureMarkerLayer(
                MarkerLayerOptions().withCollisionEnabled(true).withVisible(true),
            )
            engine.deleteAllMarkers()
            val m = engine.addMarker(
                MarkerOptions()
                    .withLatLng(CENTER)
                    .withIconResource(R.drawable.default_pin, context)
                    .withIconSize(2f),
            )
            assertNotNull(m)
        }
    }

    // ==================== MO-G07 容错 ====================

    @Test
    fun moG07_invalidBlendAndPattern_safeFallback() {
        val opts = FillOptions()
            .withLatLngs(listOf(RING))
            .withBlendMode("")
            .withFillPatternMode("")
            .withData(JsonParser.parseString("""{"k":"v"}"""))
        assertEquals(FillOptions.BLEND_MODE_NORMAL, opts.blendMode)
        assertEquals(FillOptions.PATTERN_SOLID, opts.fillPatternMode)
        assertNotNull(opts.data)
    }
}
