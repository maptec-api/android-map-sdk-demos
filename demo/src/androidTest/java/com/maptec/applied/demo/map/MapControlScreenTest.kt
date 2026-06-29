package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.demo.ui.screens.map.MapCameraLimits
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapControlScreenTest {

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        *if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            emptyArray()
        }
    )

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

    // ==========================================
    // 测试常量提取 (复用 MapCameraLimits 极限值，并定义测试用的偏好值)
    // ==========================================
    companion object {
        private const val DELTA = 0.01

        // 基于业务常量计算出测试用的偏好值和尝试值
        private val TEST_PREF_MIN_ZOOM = MapCameraLimits.MAX_ZOOM / 2       // 10.0
        private val TEST_ATTEMPT_ZOOM_LOW = TEST_PREF_MIN_ZOOM - 5.0        // 5.0

        private val TEST_PREF_MAX_ZOOM = MapCameraLimits.MAX_ZOOM - 5.0     // 15.0
        private val TEST_ATTEMPT_ZOOM_HIGH = MapCameraLimits.MAX_ZOOM + 5.0 // 25.0 (故意超限)

        private val TEST_PREF_MAX_TILT = MapCameraLimits.MAX_TILT_DEFAULT - 15.0 // 45.0
        private val TEST_ATTEMPT_TILT_HIGH = TEST_PREF_MAX_TILT + 5.0            // 50.0

        private val TEST_PREF_MIN_TILT = MapCameraLimits.MIN_TILT + 20.0    // 20.0
        private val TEST_ATTEMPT_TILT_LOW = TEST_PREF_MIN_TILT - 10.0       // 10.0
    }

    @After
    fun tearDown() {
        Thread.sleep(1000)
    }

    private fun getString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun navigateToMapControlScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.map_item_map_control)).performClick()
        composeTestRule.waitForIdle()
    }

    // ==========================================
    // 辅助方法：同步获取地图的实际底层属性
    // ==========================================

    private fun getActualMapZoom(mapView: MapView): Double {
        var actualZoom = -1.0
        val latch = CountDownLatch(1)
        composeTestRule.runOnUiThread {
            mapView.getMapAsync { map ->
                actualZoom = map.cameraPosition.zoom
                latch.countDown()
            }
        }
        // 等待异步回调，最多等 2 秒
        latch.await(2, TimeUnit.SECONDS)
        return actualZoom
    }

    private fun getActualMapTilt(mapView: MapView): Double {
        var actualTilt = -1.0
        val latch = CountDownLatch(1)
        composeTestRule.runOnUiThread {
            mapView.getMapAsync { map ->
                actualTilt = map.cameraPosition.tilt
                latch.countDown()
            }
        }
        latch.await(2, TimeUnit.SECONDS)
        return actualTilt
    }



    @Test
    fun testAnimateCamera() {
        navigateToMapControlScreen()
        composeTestRule.waitForMapRendered()

        val mapView = composeTestRule.getMapView()

        composeTestRule.onNodeWithTag("tilt").run {
            performTextClearance()
            performTextInput("30")
        }
        composeTestRule.onNodeWithTag("zoom").run {
            performTextClearance()
            performTextInput("16")
        }
        composeTestRule.onNodeWithTag("bearing").run {
            performTextClearance()
            performTextInput("10")
        }

        composeTestRule.onNodeWithText(getString(R.string.change_camera)).performClick()
        composeTestRule.waitForIdle()

        // 断言：检查底层 CameraPosition 属性是否更新
        assertEquals(30.0, getActualMapTilt(mapView), DELTA)
        assertEquals(16.0, getActualMapZoom(mapView), DELTA)
    }

    @Test
    fun testMinZoom() {
        navigateToMapControlScreen()
        composeTestRule.waitForMapRendered()
        val mapView = composeTestRule.getMapView()

        // 设置最小比例尺为 10
        composeTestRule.onNodeWithTag("minZoom").run {
            performTextClearance()
            performTextInput(TEST_PREF_MIN_ZOOM.toString())
        }

        // 尝试调整比例尺到 5
        composeTestRule.onNodeWithTag("zoom").run {
            performTextClearance()
            performTextInput(TEST_ATTEMPT_ZOOM_LOW.toString())
        }

        composeTestRule.onNodeWithText(getString(R.string.change_camera)).performClick()
        composeTestRule.waitForIdle()

        // 断言：直接获取地图属性，判断是否被限制在了 TEST_PREF_MIN_ZOOM (10.0)
        assertEquals(TEST_PREF_MIN_ZOOM, getActualMapZoom(mapView), DELTA)
    }

    @Test
    fun testMaxZoom() {
        navigateToMapControlScreen()
        composeTestRule.waitForMapRendered()
        val mapView = composeTestRule.getMapView()

        // 设置最大比例尺为 15
        composeTestRule.onNodeWithTag("maxZoom").run {
            performTextClearance()
            performTextInput(TEST_PREF_MAX_ZOOM.toString())
        }

        // 尝试调整比例尺到 MAX_ZOOM (20) - 超出当前设置的最大限制
        composeTestRule.onNodeWithTag("zoom").run {
            performTextClearance()
            performTextInput(TEST_ATTEMPT_ZOOM_HIGH.toString())
        }

        composeTestRule.onNodeWithText(getString(R.string.change_camera)).performClick()
        composeTestRule.waitForIdle()

        // 断言：直接获取地图属性，判断是否被限制在了 TEST_PREF_MAX_ZOOM (15.0)
        assertEquals(TEST_PREF_MAX_ZOOM, getActualMapZoom(mapView), DELTA)
    }

    @Test
    fun testMaxTilt() {
        navigateToMapControlScreen()
        composeTestRule.waitForMapRendered()
        val mapView = composeTestRule.getMapView()

        // 设置最大倾斜角为 45
        composeTestRule.onNodeWithTag("maxTilt").run {
            performTextClearance()
            performTextInput(TEST_PREF_MAX_TILT.toString())
        }

        // 尝试调整倾斜角到 50
        composeTestRule.onNodeWithTag("tilt").run {
            performTextClearance()
            performTextInput(TEST_ATTEMPT_TILT_HIGH.toString())
        }

        composeTestRule.onNodeWithText(getString(R.string.change_camera)).performClick()
        composeTestRule.waitForIdle()

        // 断言：实际视角是否被限制在 45.0
        assertEquals(TEST_PREF_MAX_TILT, getActualMapTilt(mapView), DELTA)
    }

    @Test
    fun testMinTilt() {
        navigateToMapControlScreen()
        composeTestRule.waitForMapRendered()
        val mapView = composeTestRule.getMapView()

        // 设置最小倾斜角为 20
        composeTestRule.onNodeWithTag("minTilt").run {
            performTextClearance()
            performTextInput(TEST_PREF_MIN_TILT.toString())
        }

        // 尝试调整倾斜角到 10
        composeTestRule.onNodeWithTag("tilt").run {
            performTextClearance()
            performTextInput(TEST_ATTEMPT_TILT_LOW.toString())
        }

        composeTestRule.onNodeWithText(getString(R.string.change_camera)).performClick()
        composeTestRule.waitForIdle()

        // 断言：实际视角是否被兜底限制在 20.0
        assertEquals(TEST_PREF_MIN_TILT, getActualMapTilt(mapView), DELTA)
    }
}