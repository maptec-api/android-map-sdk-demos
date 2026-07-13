package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.openAnnotationsDemo
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.openInteractionDemo
import com.maptec.applied.demo.ext.openMapsDemo
import com.maptec.applied.demo.ext.openUiControlsDemo
import com.maptec.applied.demo.ext.openWebServicesDemo
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.waitForMapDemoReady
import com.maptec.applied.geometry.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PoiClickScreenTest {

    // 自动授予地图所需的权限
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

    private fun getString(resId: Int): String = getTestString(resId)

    /**
     * 导航逻辑：从主页跳转到“点击POI居中”示例页面
     */
    private fun navigateToPoiClickScreen() {
        composeTestRule.openInteractionDemo(R.string.map_item_poi_click_center)
        composeTestRule.waitForIdle()
        // 等待地图基础渲染完成
        composeTestRule.waitForMapDemoReady()

        // 等待地图非空且坐标不再是默认的 0,0 (如果是以 0,0 为起始点)

        // 额外等待 2 秒，确保 POI 数据图层请求并加载到本地
        Thread.sleep(2000)

        // 3. 打印一下初始坐标，看看是不是 (0,0)
        val initial = getMapCenter()
        println("DEBUG: Initial coordinate after navigation: $initial")
    }

    /**
     * 反射辅助函数：获取当前地图的中心点经纬度
     * 用于验证地图是否发生了位移（居中）
     */
    private fun getMapCenter(): LatLng {
        val mapView = composeTestRule.getMapView()
        val latch = CountDownLatch(1)
        var center = LatLng(0.0, 0.0)

        // 在 UI 线程执行
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                // 注意：不同 SDK 的获取方式略有不同，这里以 Mapbox/Google 风格为例
                val target = map.cameraPosition.target
                if (target != null) {
                    center = LatLng(target.latitude, target.longitude)
                }
                latch.countDown() // 释放锁
            }
        }

        // 等待最多 5 秒，如果超时则继续，避免测试无限阻塞
        latch.await(5, TimeUnit.SECONDS)
        return center
    }

    /**
     * 核心算法：遍历点击地图区域。
     * 因为地图上不是每一处都有 POI，该方法按 8x8 网格扫描屏幕，直到命中一个 POI（UI显示了名称）。
     */
    private fun searchAndClickUntilPoiHit() {
        val mapNode = composeTestRule.onNodeWithTag("mapView")
        val bounds = mapNode.fetchSemanticsNode().boundsInRoot

        val steps = 32 // 增加扫描密度到 31x31
        val stepX = bounds.width / steps
        val stepY = bounds.height / steps

        for (i in 1 until steps) {
            for (j in 1 until steps) {
                val x = i * stepX
                val y = j * stepY

                mapNode.performTouchInput {
                    click(androidx.compose.ui.geometry.Offset(x, y))
                }

                // 给地图检索数据预留一小段异步时间
                Thread.sleep(500)
                composeTestRule.waitForIdle()

                // 验证 UI 中的 POI 名称是否已经不再是初始的 "-"
                val isHit = try {
                    composeTestRule.onNodeWithText("当前命中POI 名称: -").assertDoesNotExist()
                    true
                } catch (e: AssertionError) {
                    false
                }

                if (isHit) return // 命中成功，停止扫描
            }
        }
        throw AssertionError("在当前区域扫描了 ${steps * steps} 个点仍未找到 POI。请检查网络或地图数据。")
    }

    /**
     * 测试用例 1：验证开启居中开关时，点击 POI 会移动地图且更新 UI
     */
    @Test
    fun testPoiClick_WhenEnabled_UpdatesUIAndMovesCamera() {
        navigateToPoiClickScreen()

        // 确保拿到非 0 的初始坐标
        val beforeCenter = getMapCenter()

        searchAndClickUntilPoiHit()

        // 轮询检查位移
        composeTestRule.waitUntil(5000) {
            val current = getMapCenter()
            val diff = Math.abs(current.latitude - beforeCenter.latitude)
            println("DEBUG: Moving diff = $diff")
            diff > 0.00001
        }

        // 最后的断言确保 UI 也更新了
        composeTestRule.onNodeWithText("当前命中POI 名称: ", substring = true).assertIsDisplayed()
    }

    /**
     * 测试用例 2：验证关闭居中开关时，点击 POI 只更新 UI 但不移动地图
     * 明天问下产品是否要满足这样的测试功能
     */
//    @Test
//    fun testPoiClick_WhenDisabled_UpdatesUIButKeepsCamera() {
//        navigateToPoiClickScreen()
//
//        // 1. 操作：手动关闭居中开关
//        composeTestRule.onNodeWithTag("poi_center_switch").performClick()
//        composeTestRule.onNodeWithTag("poi_center_switch").assertIsOff()
//
//        // 2. 记录当前中心坐标
//        val beforeCenter = getMapCenter()
//        println("testPoiClick_WhenDisabled_UpdatesUIButKeepsCamera->beforeCenter{$beforeCenter}")
//
//        // 3. 寻找并点击 POI
//        searchAndClickUntilPoiHit()
//
//        // 4. 预期结果 A：中心点坐标应该保持完全一致（验证居中逻辑被拦截）
//        val afterCenter = getMapCenter()
//        println("testPoiClick_WhenDisabled_UpdatesUIButKeepsCamera->afterCenter{$afterCenter}")
//
//        assertEquals(
//            "关闭居中时，点击POI地图中心不应发生位移",
//            beforeCenter.latitude,
//            afterCenter.latitude,
//            0.000001
//        )
//
//        // 5. 预期结果 B：UI 仍然需要显示点击到的 POI 信息
//        composeTestRule.onNodeWithText("当前命中POI 名称: ", substring = true).assertIsDisplayed()
//    }

    /**
     * 测试用例 3：验证“清除高亮”按钮的功能
     */
    @Test
    fun testClearButton_ResetsPoiInformation() {
        navigateToPoiClickScreen()

        // 1. 先触发一次 POI 选中
        searchAndClickUntilPoiHit()

        // 2. 点击“清除高亮”按钮
        composeTestRule.onNodeWithTag("poi_clear_button").performClick()
        composeTestRule.waitForIdle()

        // 3. 预期结果：UI 上的文本应该恢复成初始的横杠 "-"
        composeTestRule.onNodeWithText("当前命中POI 名称: -").assertIsDisplayed()
        composeTestRule.onNodeWithText("当前命中POI 坐标: -").assertIsDisplayed()
    }

    /**
     * 测试用例 4：验证切换开关状态时，会自动清理当前已选中的 POI
     */
    @Test
    fun testToggleSwitch_ClearsCurrentSelection() {
        navigateToPoiClickScreen()

        // 1. 选中一个 POI
        searchAndClickUntilPoiHit()

        // 2. 切换开关（从 ON 到 OFF）
        composeTestRule.onNodeWithTag("poi_center_switch").performClick()

        // 3. 预期结果：根据业务代码 logic，切换开关会调用 clearSelection()，信息应重置
        composeTestRule.onNodeWithText("当前命中POI 名称: -").assertIsDisplayed()
    }
}