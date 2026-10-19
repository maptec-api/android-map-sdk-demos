package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.ui.screens.interaction.LimitBoundsDemo
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [LimitBoundsScreen] 限制显示区域功能测试。
 *
 * 覆盖：默认未限制、开启后 minZoom + 视野适配、越界钳制、取消后恢复。
 */
@RunWith(AndroidJUnit4::class)
class LimitBoundsScreenTest {

    companion object {
        private const val COORD_DELTA = 0.05
        private const val ZOOM_DELTA = 0.2

        /** 明确在限制区域外（北京）。 */
        private val OUTSIDE_TARGET = LatLng(39.9042, 116.4074)
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToLimitBoundsScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    private fun navigateToLimitBoundsScreen() {
        composeTestRule.settleForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_limit_bounds)
        composeTestRule.settleForIdle()
    }

    private fun withMaptecMap(action: (MaptecMap) -> Unit) {
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

    private fun clickToggle() {
        composeTestRule.onNodeWithTag(LimitBoundsDemo.TAG_TOGGLE_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.settleForIdle()
    }

    private fun assertStatus(enabled: Boolean) {
        val expected = getTestString(
            if (enabled) R.string.limit_bounds_status_enabled
            else R.string.limit_bounds_status_disabled,
        )
        composeTestRule.onNodeWithTag(LimitBoundsDemo.TAG_STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(expected)
    }

    private fun waitUntilTargetInsideBounds(timeoutMs: Long = 12_000L) {
        val bounds = LimitBoundsDemo.bounds
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var inside = false
            withMaptecMap { map ->
                val target = map.cameraPosition.target
                inside = target != null && bounds.contains(target)
            }
            if (inside) return
            Thread.sleep(200)
        }
        throw AssertionError("相机中心未在限制区域内（超时 ${timeoutMs}ms）")
    }

    private fun waitUntilTargetNear(expected: LatLng, timeoutMs: Long = 8_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var near = false
            withMaptecMap { map ->
                val target = map.cameraPosition.target
                near = target != null &&
                    kotlin.math.abs(target.latitude - expected.latitude) < COORD_DELTA &&
                    kotlin.math.abs(target.longitude - expected.longitude) < COORD_DELTA
            }
            if (near) return
            Thread.sleep(200)
        }
        throw AssertionError("相机未到达目标 $expected（超时 ${timeoutMs}ms）")
    }

    @Test
    fun testDefault_unlimitedMinZoomAndStatusDisabled() {
        assertStatus(enabled = false)
        withMaptecMap { map ->
            // 原生层会按视口把 minZoom 抬到可显示全球的下限（约 0.3x）
            assertTrue(
                "默认 minZoom 应明显低于限制值 10",
                map.minZoomLevel < LimitBoundsDemo.LIMITED_MIN_ZOOM - 1.0,
            )
            val target = requireNotNull(map.cameraPosition.target)
            assertEquals(LimitBoundsDemo.INITIAL_TARGET.latitude, target.latitude, COORD_DELTA)
            assertEquals(LimitBoundsDemo.INITIAL_TARGET.longitude, target.longitude, COORD_DELTA)
        }
    }

    @Test
    fun testEnableBounds_setsMinZoomAndFitsCameraInsideBounds() {
        clickToggle()
        assertStatus(enabled = true)

        withMaptecMap { map ->
            assertNotNull(map.cameraTargetBounds)
            assertEquals(
                LimitBoundsDemo.LIMITED_MIN_ZOOM,
                map.minZoomLevel,
                ZOOM_DELTA,
            )
            assertTrue(
                "启用后 zoom 不应低于最小限制",
                map.cameraPosition.zoom + ZOOM_DELTA >= LimitBoundsDemo.LIMITED_MIN_ZOOM,
            )
        }
        waitUntilTargetInsideBounds()
    }

    @Test
    fun testEnableBounds_moveOutside_staysInside() {
        clickToggle()
        assertStatus(enabled = true)
        composeTestRule.settleForIdle()
        waitUntilTargetInsideBounds()

        withMaptecMap { map ->
            assertNotNull(map.cameraTargetBounds)
            assertEquals(
                LimitBoundsDemo.LIMITED_MIN_ZOOM,
                map.minZoomLevel,
                ZOOM_DELTA,
            )
            // 尝试移到区域外 / 降到限制以下，最终中心应被引擎夹紧在 bounds 内
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    OUTSIDE_TARGET,
                    LimitBoundsDemo.LIMITED_MIN_ZOOM - 3.0,
                ),
            )
        }
        composeTestRule.settleForIdle()
        Thread.sleep(500)
        waitUntilTargetInsideBounds()

        withMaptecMap { map ->
            assertTrue(
                "启用区域限制后 zoom 不应明显低于 minZoom",
                map.cameraPosition.zoom + ZOOM_DELTA >= LimitBoundsDemo.LIMITED_MIN_ZOOM,
            )
            val target = requireNotNull(map.cameraPosition.target)
            assertTrue(
                "启用后相机应保持在限制区域内: $target",
                LimitBoundsDemo.bounds.contains(target),
            )
        }
    }

    @Test
    fun testDisableBounds_restoresMinZoomAndAllowsOutsideMove() {
        clickToggle()
        assertStatus(enabled = true)
        waitUntilTargetInsideBounds()

        clickToggle()
        assertStatus(enabled = false)

        withMaptecMap { map ->
            assertNull(map.cameraTargetBounds)
            assertTrue(
                "取消限制后 minZoom 应恢复为低门槛",
                map.minZoomLevel < LimitBoundsDemo.LIMITED_MIN_ZOOM - 1.0,
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_TARGET, 10.0))
        }
        composeTestRule.settleForIdle()
        waitUntilTargetNear(OUTSIDE_TARGET)
    }
}
