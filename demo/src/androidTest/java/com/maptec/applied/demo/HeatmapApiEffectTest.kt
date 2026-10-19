package com.maptec.applied.demo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.ext.EffectTestHelper
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.resetToMainCatalog
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.overlay.heatmap.HeatmapColorStop
import com.maptec.applied.maps.overlay.heatmap.HeatmapGradient
import com.maptec.applied.maps.overlay.heatmap.HeatmapTileOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Heatmap L1（test-plan MO-H*）：points / gradient / zoom / 参数回读。
 * 须在模拟器跑通。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class HeatmapApiEffectTest {

    companion object {
        private val CENTER = LatLng(1.32, 103.82)
        private val POINTS = listOf(
            LatLng(1.320, 103.810),
            LatLng(1.322, 103.820),
            LatLng(1.318, 103.830),
            LatLng(1.325, 103.815),
            LatLng(1.315, 103.825),
        )
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        composeTestRule.resetToMainCatalog()
        composeTestRule.openMapsDemo(R.string.map_item_map_render)
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        EffectTestHelper.withMapApi(mapView) { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTER, 12.0))
        }
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    @Test
    fun moH01_pointsAndParams_apiVerified() {
        val gradient = HeatmapGradient(
            HeatmapColorStop(0f, 0, 0, 255, 1f),
            HeatmapColorStop(0.5f, 0, 255, 0, 1f),
            HeatmapColorStop(1f, 255, 0, 0, 1f),
        )
        val opts = HeatmapTileOptions()
            .withLatLngs(POINTS)
            .withHeatmapRadius(20f)
            .withHeatmapWeight(0.8f)
            .withHeatmapIntensity(1.2f)
            .withHeatmapOpacity(0.7f)
            .withHeatmapGradient(gradient)
            .withMinZoom(3f)
            .withMaxZoom(18f)

        assertEquals(5, opts.points.size)
        assertEquals(20f, opts.heatmapRadius!!, 0.01f)
        assertEquals(0.8f, opts.heatmapWeight!!, 0.01f)
        assertEquals(3f, opts.minZoom!!, 0.01f)
        assertEquals(18f, opts.maxZoom!!, 0.01f)
        assertEquals(3, opts.heatmapGradient!!.stops.size)

        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllTileOverlays()
            val tile = engine.addTileOverlay(opts)
            assertNotNull(tile)
            assertEquals(5, tile!!.points.size)
            assertEquals(20f, tile.heatmapRadius!!, 0.01f)
            assertEquals(0.8f, tile.heatmapWeight!!, 0.01f)
            assertEquals(1.2f, tile.heatmapIntensity!!, 0.01f)
            assertEquals(0.7f, tile.heatmapOpacity!!, 0.01f)
            assertEquals(3f, tile.minZoom!!, 0.01f)
            assertEquals(18f, tile.maxZoom!!, 0.01f)
        }
    }

    @Test
    fun moH02_gradientRequiresTwoStops() {
        try {
            HeatmapGradient(HeatmapColorStop(0f, 255, 255, 255, 1f))
            fail("少于 2 个色停点应抛 IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun moH03_emptyPoints_rejectedOnAdd() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllTileOverlays()
            try {
                engine.addTileOverlay(HeatmapTileOptions().withLatLngs(emptyList()))
                fail("空点集应被拒绝")
            } catch (_: IllegalArgumentException) {
                // expected
            } catch (_: RuntimeException) {
                // validate 可能包装为 RuntimeException
            }
        }
    }

    @Test
    fun moH_defaultGradient_addSucceeds() {
        EffectTestHelper.withEngine(mapView) { engine ->
            engine.deleteAllTileOverlays()
            val tile = engine.addTileOverlay(
                HeatmapTileOptions()
                    .withLatLngs(POINTS)
                    .withHeatmapGradient(HeatmapGradient.defaultGradient()),
            )
            assertNotNull(tile)
            assertTrue(tile!!.points.isNotEmpty())
        }
    }
}
