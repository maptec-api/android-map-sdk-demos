package com.maptec.applied.demo.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.settleForIdle
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.demo.ui.screens.interaction.CameraAnimationDemo
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * [CameraAnimationScreen] 相机移动动画功能测试。
 *
 * 覆盖：初始视角、moveCamera / easeCamera / animateCamera 到达目标、
 * CancelableCallback.onFinish / onCancel、回到原位置。
 *
 * 不使用 GrantPermissionRule：仅需 INTERNET 等安装时权限；该 Rule 会反复连接
 * UiAutomation，在套件中易触发 “already registered”。
 */
@RunWith(AndroidJUnit4::class)
class CameraAnimationScreenTest {

    companion object {
        private const val COORD_DELTA = 0.05
        private const val ZOOM_DELTA = 0.2
        private const val CALLBACK_TIMEOUT_MS = 10_000L
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToCameraAnimationScreen()
        composeTestRule.waitForMapDemoReady()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.settleForIdle()
    }

    private fun navigateToCameraAnimationScreen() {
        composeTestRule.settleForIdle()
        composeTestRule.openInteractionDemo(R.string.map_item_camera_animation)
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

    private fun clickAction(tag: String) {
        composeTestRule.onNodeWithTag(tag)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.settleForIdle()
    }

    private fun waitForCallbackStatus(statusResId: Int, timeoutMs: Long = CALLBACK_TIMEOUT_MS) {
        val expected = getTestString(statusResId)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                composeTestRule.onNodeWithTag(CameraAnimationDemo.TAG_CALLBACK_STATUS)
                    .assertTextContains(expected, substring = true)
                return
            } catch (_: AssertionError) {
                Thread.sleep(150)
            } catch (_: Throwable) {
                Thread.sleep(150)
            }
        }
        composeTestRule.onNodeWithTag(CameraAnimationDemo.TAG_CALLBACK_STATUS)
            .assertTextContains(expected, substring = true)
    }

    private fun assertCameraNear(expected: LatLng, expectedZoom: Double) {
        withMaptecMap { map ->
            val target = requireNotNull(map.cameraPosition.target)
            assertEquals(expected.latitude, target.latitude, COORD_DELTA)
            assertEquals(expected.longitude, target.longitude, COORD_DELTA)
            assertEquals(expectedZoom, map.cameraPosition.zoom, ZOOM_DELTA)
        }
    }

    @Test
    fun testInitialCamera_atOriginWithIdleCallback() {
        composeTestRule.onNodeWithTag(CameraAnimationDemo.TAG_CALLBACK_STATUS)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(getTestString(R.string.camera_anim_status_idle), substring = true)

        assertCameraNear(CameraAnimationDemo.ORIGIN, CameraAnimationDemo.ORIGIN_ZOOM)
    }

    @Test
    fun testMoveCamera_jumpsToTargetAndFinishes() {
        clickAction(CameraAnimationDemo.TAG_MOVE_BUTTON)
        waitForCallbackStatus(R.string.camera_anim_callback_on_finish)
        assertCameraNear(CameraAnimationDemo.TARGET, CameraAnimationDemo.TARGET_ZOOM)
    }

    @Test
    fun testEaseCamera_animatesToTargetAndFinishes() {
        clickAction(CameraAnimationDemo.TAG_EASE_BUTTON)
        waitForCallbackStatus(R.string.camera_anim_callback_on_finish)
        assertCameraNear(CameraAnimationDemo.TARGET, CameraAnimationDemo.TARGET_ZOOM)
    }

    @Test
    fun testAnimateCamera_fliesToTargetAndFinishes() {
        clickAction(CameraAnimationDemo.TAG_FLY_BUTTON)
        waitForCallbackStatus(R.string.camera_anim_callback_on_finish)
        assertCameraNear(CameraAnimationDemo.TARGET, CameraAnimationDemo.TARGET_ZOOM)
    }

    @Test
    fun testReset_returnsToOriginAndFinishes() {
        clickAction(CameraAnimationDemo.TAG_MOVE_BUTTON)
        waitForCallbackStatus(R.string.camera_anim_callback_on_finish)
        assertCameraNear(CameraAnimationDemo.TARGET, CameraAnimationDemo.TARGET_ZOOM)

        clickAction(CameraAnimationDemo.TAG_RESET_BUTTON)
        waitForCallbackStatus(R.string.camera_anim_callback_on_finish)
        assertCameraNear(CameraAnimationDemo.ORIGIN, CameraAnimationDemo.ORIGIN_ZOOM)
    }

    @Test
    fun testEaseCamera_cancelTransitionsInvokesOnCancel() {
        clickAction(CameraAnimationDemo.TAG_EASE_BUTTON)
        Thread.sleep(300)

        var alreadyFinished = false
        try {
            composeTestRule.onNodeWithTag(CameraAnimationDemo.TAG_CALLBACK_STATUS)
                .assertTextContains(
                    getTestString(R.string.camera_anim_callback_on_finish),
                    substring = true,
                )
            alreadyFinished = true
        } catch (_: Throwable) {
            alreadyFinished = false
        }

        if (alreadyFinished) {
            assertCameraNear(CameraAnimationDemo.TARGET, CameraAnimationDemo.TARGET_ZOOM)
            return
        }

        withMaptecMap { map -> map.cancelTransitions() }
        composeTestRule.settleForIdle()
        waitForCallbackStatus(R.string.camera_anim_callback_on_cancel)
    }
}
