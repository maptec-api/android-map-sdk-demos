package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.ui.screens.interaction.CameraTargetBoundsDemo
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
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
 * [CameraTargetBoundsScreen] 限制经纬度显示边界功能测试。
 *
 * 覆盖：进入默认开启、get/setCameraTargetBounds、取消后可越界、重新设置后钳制、padding 重载。
 */
@RunWith(AndroidJUnit4::class)
class CameraTargetBoundsScreenTest {

    companion object {
        private const val COORD_DELTA = 0.05
        private const val BOUNDS_DELTA = 1e-6

        /** 明确在园区矩形外（天安门附近）。 */
        private val OUTSIDE_TARGET = LatLng(39.9042, 116.4074)
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToCameraTargetBoundsScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
        waitUntilBoundsEnabled()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    private fun navigateToCameraTargetBoundsScreen() {
        composeTestRule.settleForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_camera_target_bounds)
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

    private fun waitUntilBoundsEnabled(timeoutMs: Long = 12_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var enabled = false
            withMaptecMap { map ->
                enabled = map.cameraTargetBounds != null
            }
            if (enabled) {
                composeTestRule.settleForIdle()
                return
            }
            Thread.sleep(200)
        }
        throw AssertionError("进入页面后未默认开启 cameraTargetBounds（超时 ${timeoutMs}ms）")
    }

    private fun waitUntilTargetInsideBounds(timeoutMs: Long = 12_000L) {
        val bounds = CameraTargetBoundsDemo.bounds
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

    private fun assertBoundsEquals(expected: LatLngBounds, actual: LatLngBounds?) {
        assertNotNull("cameraTargetBounds 不应为 null", actual)
        requireNotNull(actual)
        assertEquals(expected.latitudeNorth, actual.latitudeNorth, BOUNDS_DELTA)
        assertEquals(expected.latitudeSouth, actual.latitudeSouth, BOUNDS_DELTA)
        assertEquals(expected.longitudeEast, actual.longitudeEast, BOUNDS_DELTA)
        assertEquals(expected.longitudeWest, actual.longitudeWest, BOUNDS_DELTA)
    }

    private fun clickEnable() {
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_ENABLE_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.settleForIdle()
    }

    private fun clickDisable() {
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_DISABLE_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.settleForIdle()
    }

    @Test
    fun testDefault_boundsEnabledAndButtonsMatchState() {
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_ENABLE_BUTTON)
            .performScrollTo()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_DISABLE_BUTTON)
            .performScrollTo()
            .assertIsEnabled()

        withMaptecMap { map ->
            assertBoundsEquals(CameraTargetBoundsDemo.bounds, map.cameraTargetBounds)
            val target = requireNotNull(map.cameraPosition.target)
            assertTrue(
                "默认相机应在限制区域内: $target",
                CameraTargetBoundsDemo.bounds.contains(target),
            )
        }
    }

    @Test
    fun testDisableBounds_clearsApiAndAllowsOutsideMove() {
        clickDisable()

        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_ENABLE_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag(CameraTargetBoundsDemo.TAG_DISABLE_BUTTON)
            .performScrollTo()
            .assertIsNotEnabled()

        withMaptecMap { map ->
            assertNull("取消后 getCameraTargetBounds 应为 null", map.cameraTargetBounds)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_TARGET, 12.0))
        }
        composeTestRule.settleForIdle()
        waitUntilTargetNear(OUTSIDE_TARGET)
    }

    @Test
    fun testEnableBounds_setsApiAndClampsOutsideMove() {
        clickDisable()
        withMaptecMap { map ->
            assertNull(map.cameraTargetBounds)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_TARGET, 12.0))
        }
        waitUntilTargetNear(OUTSIDE_TARGET)

        clickEnable()
        withMaptecMap { map ->
            assertBoundsEquals(CameraTargetBoundsDemo.bounds, map.cameraTargetBounds)
            // 开启限制后再次尝试移到区域外，最终中心应被引擎夹紧
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_TARGET, 12.0))
        }
        composeTestRule.settleForIdle()
        Thread.sleep(500)
        waitUntilTargetInsideBounds()

        withMaptecMap { map ->
            val target = requireNotNull(map.cameraPosition.target)
            assertTrue(
                "启用后相机应保持在限制区域内: $target",
                CameraTargetBoundsDemo.bounds.contains(target),
            )
        }
    }

    @Test
    fun testSetCameraTargetBounds_withPadding_returnsOriginalBounds() {
        clickDisable()
        withMaptecMap { map ->
            assertNull(map.cameraTargetBounds)
        }

        val bounds = CameraTargetBoundsDemo.bounds
        withMaptecMap { map ->
            map.setCameraTargetBounds(bounds, 40, 40, 40, 40)
            // getCameraTargetBounds 返回业务设置的原始 bounds（不含 padding 内缩）
            assertBoundsEquals(bounds, map.cameraTargetBounds)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(OUTSIDE_TARGET, 12.0))
        }
        composeTestRule.settleForIdle()
        Thread.sleep(500)
        waitUntilTargetInsideBounds()

        withMaptecMap { map ->
            map.setCameraTargetBounds(null)
            assertNull(map.cameraTargetBounds)
        }
    }
}
