package com.maptec.applied.demo.ext


import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.locale.readDemoUiLanguage
import com.maptec.applied.demo.ui.locale.withUiLocale
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.maps.MapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun ComposeContentTestRule.getMapView(): MapView {
    // 在运行时动态检查并获取 Android 相关的 Rule，完美避开编译器的泛型强校验
    val androidRule = this as? AndroidComposeTestRule<*, *>
        ?: error("无法获取 MapView：当前的 composeTestRule 必须是由 createAndroidComposeRule 创建的")

    val mapView = androidRule.activity.findViewById<View>(android.R.id.content)
        ?.findViewWithTag<MapView>("mapView")

    return requireNotNull(mapView) { "MapView 未找到，请检查 Tag 是否正确" }
}


fun getTestString(resId: Int, vararg formatArgs: Any?): String {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val localizedContext = context.withUiLocale(context.readDemoUiLanguage())
    return if (formatArgs.isEmpty()) {
        localizedContext.getString(resId)
    } else {
        localizedContext.getString(resId, *formatArgs)
    }
}


/**
 * 核心测试流程封装
 */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.verifyMapViewSwitchToggle(
    switchTag: String, getMapSetting: (MapView, callback: (Boolean) -> Unit) -> Unit
) {

    val mapView = this.getMapView()

    // 校验地图渲染完成
    this.waitForMapRendered()
    this.expandConfigPanel()

    // 1. 验证初始状态（UI Switch 应为开启，地图内部配置应为 true）
    this.onNodeWithTag(switchTag).assertIsDisplayed().assertIsOn()
    assertMapViewState(mapView, expected = true, getMapSetting)

    // 2. 触发点击操作
    this.onNodeWithTag(switchTag).performClick()

    // 3. 替代 Thread.sleep，智能等待重绘完成
    this.waitForIdle()

    // 4. 验证点击后状态（UI Switch 应为关闭，地图内部配置应为 false）
    this.onNodeWithTag(switchTag).assertIsOff()
    assertMapViewState(mapView, expected = false, getMapSetting)
}

/**
 * 底层异步断言转换逻辑
 */
private fun assertMapViewState(
    mapView: MapView, expected: Boolean, asyncAction: (MapView, callback: (Boolean) -> Unit) -> Unit
) {
    val latch = CountDownLatch(1)
    var actualState = !expected

    asyncAction(mapView) { isEnabled ->
        actualState = isEnabled
        latch.countDown()
    }

    val success = latch.await(3, TimeUnit.SECONDS)
    assertTrue("获取 MapAsync 超时", success)
    assertEquals("地图状态与预期不符", expected, actualState)
}

/**
 * 等待地图渲染完成
 */
fun ComposeContentTestRule.waitForMapRendered(
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(30), tag: String = "mapRendered"
) {
    pollUntil(timeoutMillis) {
        onAllNodesWithTag(tag).existsSafely()
    }
}

/** Expands [WebServiceApiResponseCard] so JSON keys appear in the semantics tree. */
fun ComposeContentTestRule.expandApiResponseCard() {
    if (onAllNodesWithTag("api_response_card", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
        return
    }
    val expandLabels = listOf(
        getTestString(R.string.geocode_expand),
        getTestString(R.string.search_action_expand),
    )
    for (label in expandLabels) {
        if (onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            val expandNode = onAllNodesWithText(label, useUnmergedTree = true).onFirst()
            runCatching { expandNode.performScrollTo() }
            expandNode.performClick()
            waitForIdle()
            return
        }
    }
}

fun ComposeContentTestRule.waitForApiResponseKey(keyword: String, timeoutMs: Long = 15000) {
    expandApiResponseCard()
    this.waitUntil(timeoutMs) {
        this.onAllNodesWithText(keyword, substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }
}