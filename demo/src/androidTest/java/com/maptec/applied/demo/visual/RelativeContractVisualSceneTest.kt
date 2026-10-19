package com.maptec.applied.demo.visual

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.collapseConfigPanel
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.takeDeviceScreenshot
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.OverlayKind
import com.maptec.applied.maps.overlay.line.Line
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.MarkerLayerOptions
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * L5 终态场景准备器（跨构建人审主路径）。
 *
 * 命名死规则：必须以 `*VisualSceneTest` 结尾（满足 Gradle `*Test` 扫描；禁止 `*EffectTest`）。
 *
 * 模型：一个 `@Test` = 一个 checklist_id = 一张终态图。
 * Runner 用 Python 与对照构建做相似度：高分 → contract_passed；否则 pending_review 交 Vue 人审。
 *
 * UI：基础地图 → 地图渲染、收起配置面板。
 * 相机：开阔南海 + 最大缩放，底图近似均匀水色，降低跨构建像素对比噪声。
 *
 * 不使用 GrantPermissionRule：getExternalFilesDir 无需运行时存储权限，且该 Rule
 * 会反复连接 UiAutomation，与截屏冲突（already registered / Not connected）。
 */
@RunWith(AndroidJUnit4::class)
class RelativeContractVisualSceneTest {

    companion object {
        /** 开阔南海（远离陆地/道路），消除底图要素对相似度对比的干扰。 */
        private val CENTER = LatLng(4.0, 112.0)
        /** [com.maptec.applied.demo.ui.screens.maps.MapCameraLimits.MAX_ZOOM] */
        private const val ZOOM = 20.0
        /**
         * 相对原 zoom=14 新加坡几何缩小约 64×（2^(20-14)），保持屏幕占位大致不变。
         */
        private val LINE = listOf(
            LatLng(4.0, 112.0 - 0.000156),
            LatLng(4.0 + 0.000078, 112.0),
            LatLng(4.0, 112.0 + 0.000156),
        )
        private val LINE_HIT = LatLng(4.0 + 0.000039, 112.0 - 0.000078)
        private const val MARKER_STEP = 0.0000055
        private const val CLASS_NAME =
            "com.maptec.applied.demo.visual.RelativeContractVisualSceneTest"

        private var outDirReady = false
        private lateinit var sharedOutDir: File
        private val sharedChecks = JSONArray()
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        if (!outDirReady) {
            sharedOutDir = File(context.getExternalFilesDir(null), "visual_checks").apply {
                deleteRecursively()
                mkdirs()
            }
            outDirReady = true
        }
        composeTestRule.resetToMainCatalog()
        composeTestRule.openMapsDemo(R.string.map_item_map_render)
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        EffectTestHelper.withMapApi(mapView) { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTER, ZOOM))
        }
        composeTestRule.collapseConfigPanel()
        composeTestRule.settleForIdle()
        // 远海瓦片 + 最大缩放：多等一会，避免截到未渲染完的底图
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        writeManifest()
        composeTestRule.settleForIdle()
        composeTestRule.resetToMainCatalog()
    }

    private fun awaitFrames(ms: Long = 1000L) {
        composeTestRule.settleForIdle()
        Thread.sleep(ms)
        composeTestRule.settleForIdle()
    }

    private fun lineOverlayId(line: Line): String =
        line.overlayId ?: "line_${line.id}"

    private fun awaitLineReady(line: Line) {
        val oid = lineOverlayId(line)
        EffectTestHelper.waitUntilAllNative(mapView, OverlayKind.LINE, listOf(oid))
        EffectTestHelper.waitUntilTopHit(
            mapView,
            LINE_HIT,
            expectedTop = oid,
            knownIds = setOf(oid),
        )
        awaitFrames()
    }

    private fun awaitMarkerLayerReady(markerIds: List<String>, at: LatLng) {
        EffectTestHelper.waitUntilAllNative(
            mapView,
            OverlayKind.MARKER,
            listOf("marker_overlay_main"),
        )
        val deadline = System.currentTimeMillis() + 10_000L
        var hits = emptyList<String>()
        while (System.currentTimeMillis() < deadline) {
            EffectTestHelper.withEngine(mapView) { engine ->
                hits = engine.queryOverlayHitsFromPoint(at).map { hit ->
                    hit.subId ?: hit.overlayId
                }
            }
            if (hits.any { it in markerIds }) {
                awaitFrames(1200)
                return
            }
            Thread.sleep(200)
        }
        org.junit.Assert.fail("Marker 拾取未命中: expectedAny=$markerIds actual=$hits")
    }

    private fun shot(name: String): String {
        val file = File(sharedOutDir, name)
        val bmp = takeDeviceScreenshot(composeTestRule.activity)
        FileOutputStream(file).use { out ->
            assertTrue("压缩失败: $name", bmp.compress(Bitmap.CompressFormat.PNG, 90, out))
        }
        bmp.recycle()
        awaitFrames(300)
        return name
    }

    private fun addCheck(
        checklistId: String,
        name: String,
        image: String,
        track: String = "quality",
    ) {
        sharedChecks.put(
            JSONObject()
                .put("checklist_id", checklistId)
                .put("track", track)
                .put("name", name)
                .put("image", image)
                .put("class_name", CLASS_NAME)
                .put("test_name", name),
        )
    }

    private fun writeManifest() {
        val root = JSONObject()
            .put("class_name", CLASS_NAME)
            .put("mode", "canonical_state")
            .put("device_dir", sharedOutDir.absolutePath)
            .put("checks", sharedChecks)
        File(sharedOutDir, "manifest.json").writeText(root.toString(2))
    }

    /** MO-L05：glowRadius=30 终态。 */
    @Test
    fun visual_glowRadius_r30() {
        var line: Line? = null
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            line = engine.addPolyline(
                LineOptions()
                    .withOverlayId("visual_glow_line")
                    .withLatLngs(LINE)
                    .withStrokeWeight(8f)
                    .withStrokeColor("#3388FF")
                    .withGlowEffect(true)
                    .withGlowColor("#66CCFF")
                    .withGlowRadius(30f),
            )
        }
        assertNotNull("addPolyline 失败", line)
        awaitLineReady(line!!)
        val image = shot("glow_r30.png")
        addCheck("MO-L05", "glow_radius_30", image)
    }

    /** MO-L03：虚线 on 终态。 */
    @Test
    fun visual_strokeDash_on() {
        var line: Line? = null
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllLines()
            line = engine.addPolyline(
                LineOptions()
                    .withOverlayId("visual_dash_line")
                    .withLatLngs(LINE)
                    .withStrokeWeight(10f)
                    .withStrokeColor("#FF6633")
                    .withStrokeDashArray(floatArrayOf(12f, 8f, 4f, 8f)),
            )
        }
        assertNotNull("addPolyline 失败", line)
        awaitLineReady(line!!)
        val image = shot("dash_on.png")
        addCheck("MO-L03", "stroke_dash_on", image)
    }

    /** MZ-07-off：Marker 碰撞关终态。 */
    @Test
    fun visual_markerCollision_off() {
        val markerIds = addDenseMarkers(collisionEnabled = false)
        awaitMarkerLayerReady(markerIds, CENTER)
        val image = shot("collision_off.png")
        addCheck("MZ-07-off", "marker_collision_off", image)
    }

    /** MZ-07-on：Marker 碰撞开终态（先就绪再开碰撞，避免拾取被压掉）。 */
    @Test
    fun visual_markerCollision_on() {
        val markerIds = addDenseMarkers(collisionEnabled = false)
        awaitMarkerLayerReady(markerIds, CENTER)
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.configureMarkerLayer(MarkerLayerOptions().withCollisionEnabled(true))
        }
        awaitFrames(1200)
        val image = shot("collision_on.png")
        addCheck("MZ-07-on", "marker_collision_on", image)
    }

    /** MO-C04：圆形外发光终态。 */
    @Test
    fun visual_circleOuterGlow_on() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllCircles()
            val circle = engine.addCircle(
                com.maptec.applied.maps.overlay.circle.CircleOptions()
                    .withOverlayId("visual_circle_glow")
                    .withCenter(CENTER)
                    .withRadius(40.0)
                    .withFillColor("#3388FF")
                    .withFillOpacity(0.45f)
                    .withStrokeColor("#0044AA")
                    .withStrokeWeight(2f)
                    .withOuterGlow(true)
                    .withGlowColor("#66FFFF")
                    .withGlowRadius(24f),
            )
            assertNotNull("addCircle 失败", circle)
        }
        // waitUntilAllNative 内部会 withEngine/runOnMainSync，不能嵌在 withEngine 回调里
        EffectTestHelper.waitUntilAllNative(
            mapView,
            OverlayKind.CIRCLE,
            listOf("visual_circle_glow"),
        )
        awaitFrames(1500)
        val image = shot("circle_glow_on.png")
        addCheck("MO-C04", "circle_outer_glow_on", image)
    }

    /** MO-H02：热力图渐变终态。 */
    @Test
    fun visual_heatmapGradient_on() {
        val heatPts = (0 until 80).map { i ->
            val row = i / 10
            val col = i % 10
            LatLng(
                CENTER.latitude + (row - 4) * MARKER_STEP * 2,
                CENTER.longitude + (col - 5) * MARKER_STEP * 2,
            )
        }
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllTileOverlays()
            val heat = engine.addTileOverlay(
                com.maptec.applied.maps.overlay.heatmap.HeatmapTileOptions()
                    .withOverlayId("visual_heatmap")
                    .withLatLngs(heatPts)
                    .withHeatmapGradient(
                        com.maptec.applied.maps.overlay.heatmap.HeatmapGradient.defaultGradient(),
                    ),
            )
            assertNotNull("addTileOverlay 失败", heat)
        }
        EffectTestHelper.waitUntilAllNative(
            mapView,
            OverlayKind.HEATMAP,
            listOf("visual_heatmap"),
        )
        awaitFrames(2000)
        val image = shot("heatmap_on.png")
        addCheck("MO-H02", "heatmap_gradient_on", image)
    }

    private fun addDenseMarkers(collisionEnabled: Boolean): List<String> {
        val markerIds = mutableListOf<String>()
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllMarkers()
            engine.configureMarkerLayer(MarkerLayerOptions().withCollisionEnabled(collisionEnabled))
            repeat(40) { i ->
                val row = i / 8
                val col = i % 8
                val m = engine.addMarker(
                    MarkerOptions()
                        .withLatLng(
                            LatLng(
                                CENTER.latitude + row * MARKER_STEP,
                                CENTER.longitude + col * MARKER_STEP,
                            ),
                        )
                        .withIconResource(R.drawable.default_pin, context)
                        .withIconSize(2.2f),
                )
                assertNotNull("addMarker[$i] 失败", m)
                markerIds += m!!.id
            }
        }
        return markerIds
    }
}
