package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.style.layers.Property
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MarkerAnimationScreen 功能测试。
 *
 * 对应页面：业务图层 → Marker动画
 * 覆盖：Marker 添加/清除、进入/消失/点选动画配置、顶部动画控制栏、地图点击添加。
 */
@RunWith(AndroidJUnit4::class)
class MarkerAnimationScreenTest {

    companion object {
        private const val TAG_BTN_ADD_MARKER = "symbol_btn_add_marker"
        private const val TAG_BTN_CLEAR_ALL = "symbol_btn_clear_all"
        private const val TAG_HAS_MARKERS = "symbol_layer_has_markers"
        private const val TAG_TOP_BAR = "anim_top_bar"
        private const val TAG_MODE_ENTER = "anim_mode_enter"
        private const val TAG_MODE_DISAPPEAR = "anim_mode_disappear"
        private const val TAG_BTN_ENTER_START = "anim_btn_enter_start"
        private const val TAG_BTN_ENTER_END = "anim_btn_enter_end"
        private const val TAG_BTN_DISAPPEAR_START = "anim_btn_disappear_start"
        private const val TAG_BTN_DISAPPEAR_END = "anim_btn_disappear_end"
        private const val TAG_DROPDOWN_ENTER = "anim_dropdown_enter"
        private const val TAG_DROPDOWN_SELECT = "anim_dropdown_select"
        private const val TAG_DROPDOWN_DISAPPEAR = "anim_dropdown_disappear"
        private const val TAG_INPUT_ENTER_DURATION = "anim_input_enter_duration"
        private const val TAG_INPUT_DISAPPEAR_DURATION = "anim_input_disappear_duration"

        private const val DEFAULT_LAT = 1.4
        private const val DEFAULT_LNG = 103.75
    }

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        *if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            emptyArray()
        },
    )

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeTestRule)

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateToMarkerAnimationScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
        Thread.sleep(2000)
        resetScreenState()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    // ==================== 导航与辅助 ====================

    /** 用例间若残留 Marker 则清除，不滚动面板避免干扰模式。 */
    private fun resetScreenState() {
        if (getEngineMarkers().isNotEmpty()) {
            clickPanelButton(TAG_BTN_CLEAR_ALL)
            waitForMarkerCount(0, timeoutMs = 3000)
        }
    }

    private fun navigateToMarkerAnimationScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_overlay)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.overlay_item_marker_animation))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun withMapApi(action: (MaptecMap) -> Unit) {
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

    private fun getEngineMarkers(): List<Marker> {
        var markers: Collection<Marker> = emptyList()
        withMapApi { map ->
            markers = map.overlayEngine.markers
        }
        return markers.toList()
    }

    private fun waitForMarkerCount(expected: Int, timeoutMs: Long = 5000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (getEngineMarkers().size == expected) return
            Thread.sleep(200)
        }
        fail("Marker 数量未在 ${timeoutMs}ms 内达到 $expected，实际=${getEngineMarkers().size}")
    }

    /**
     * 点击顶部动画栏按钮。BottomSheet 展开时会挡住 Compose 点击，改用根节点坐标点击。
     */
    private fun clickTopBarButton(tag: String) {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        val bounds = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val x = (bounds.left + bounds.right) / 2f
        val y = (bounds.top + bounds.bottom) / 2f
        composeTestRule.onRoot().performTouchInput {
            click(Offset(x, y))
        }
        composeTestRule.waitForIdle()
    }

    private fun waitForNoMarkersUiState(timeoutMs: Long = 12_000L) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            composeTestRule.onAllNodesWithTag(TAG_HAS_MARKERS).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun clickPanelButton(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun selectDropdownOption(dropdownTag: String, label: String) {
        composeTestRule.onNodeWithTag(dropdownTag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText(label, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(label, useUnmergedTree = true).onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    private fun setInput(tag: String, value: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.onNodeWithTag(tag).performTextReplacement(value)
        composeTestRule.waitForIdle()
    }

    /** 直接向 MapView 派发点击（Compose touch 会被 BottomSheet 拦截）。 */
    private fun clickMapCenter() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val x = mapView.width / 2f
            // 偏上区域，避开顶部动画栏与底部抽屉遮挡
            val y = mapView.height * 0.35f
            val downTime = SystemClock.uptimeMillis()
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0),
            )
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    x,
                    y,
                    0,
                ),
            )
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
    }

    private fun switchToEnterMode() {
        composeTestRule.onNodeWithTag(TAG_MODE_ENTER).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_MODE_ENTER).assertIsSelected()
    }

    private fun switchToDisappearMode() {
        composeTestRule.onNodeWithTag(TAG_MODE_DISAPPEAR).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_MODE_DISAPPEAR).assertIsSelected()
    }

    // ==================== 页面加载 ====================

    @Test
    fun testTopBar_defaultEnterMode_showsEnterButtons() {
        composeTestRule.onNodeWithTag(TAG_BTN_ENTER_START).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BTN_ENTER_END).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_MODE_ENTER).performScrollTo().assertIsSelected()
    }

    // ==================== Marker 添加 / 清除 ====================

    @Test
    fun testAddMarker_apiVerified() {
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(1)

        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()

        val marker = getEngineMarkers().single()
        assertEquals(DEFAULT_LAT, marker.latLng.latitude, 0.01)
        assertEquals(DEFAULT_LNG, marker.latLng.longitude, 0.01)
        assertEquals(2.0f, marker.iconSize, 0.01f)
        assertEquals(1.0f, marker.iconOpacity, 0.01f)
        assertEquals("Marker 1", marker.text)
        assertTrue(marker.isDraggable)
        assertTrue(marker.isClickable)
        assertEquals(Property.ICON_ANCHOR_BOTTOM, marker.iconAnchor)
    }

    @Test
    fun testAddMarker_twice_increasesCount() {
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(1)
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(2)

        val markers = getEngineMarkers()
        assertEquals("Marker 1", markers[0].text)
        assertEquals("Marker 2", markers[1].text)
    }

    @Test
    fun testClearAll_removesMarkers() {
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(1)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()

        clickPanelButton(TAG_BTN_CLEAR_ALL)
        waitForMarkerCount(0)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertDoesNotExist()
    }

    @Test
    fun testMapClick_addsMarker() {
        clickMapCenter()
        waitForMarkerCount(1)
        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
    }

    // ==================== 动画模式切换 ====================

    @Test
    fun testMainAnimationMode_switchToDisappear_changesTopBarButtons() {
        switchToDisappearMode()

        composeTestRule.onNodeWithTag(TAG_BTN_DISAPPEAR_START).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BTN_DISAPPEAR_END).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_MODE_DISAPPEAR).performScrollTo().assertIsSelected()
    }

    @Test
    fun testMainAnimationMode_disappearMode_enablesDisappearSection() {
        switchToDisappearMode()

        composeTestRule.onNodeWithTag(TAG_DROPDOWN_DISAPPEAR).performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag(TAG_INPUT_DISAPPEAR_DURATION).performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag(TAG_DROPDOWN_ENTER).performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_INPUT_ENTER_DURATION).performScrollTo().assertIsNotEnabled()
    }

    // ==================== 进入动画 ====================

    @Test
    fun testEnterAnimation_selectFadeIn_startAndEnd_noCrash() {
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(1)

        selectDropdownOption(TAG_DROPDOWN_ENTER, "淡入 fadeIn")
        switchToEnterMode()
        clickTopBarButton(TAG_BTN_ENTER_START)
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        clickTopBarButton(TAG_BTN_ENTER_END)
        composeTestRule.waitForIdle()

        waitForMarkerCount(1)
    }

    @Test
    fun testEnterAnimation_startWithoutMarkers_noCrash() {
        selectDropdownOption(TAG_DROPDOWN_ENTER, "淡入 fadeIn")
        switchToEnterMode()
        clickTopBarButton(TAG_BTN_ENTER_START)
        composeTestRule.waitForIdle()
        waitForMarkerCount(0)
    }

    @Test
    fun testEnterAnimation_selectDrop_start_noCrash() {
        clickPanelButton(TAG_BTN_ADD_MARKER)
        waitForMarkerCount(1)

        selectDropdownOption(TAG_DROPDOWN_ENTER, "下落 drop")
        switchToEnterMode()
        clickTopBarButton(TAG_BTN_ENTER_START)
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        clickTopBarButton(TAG_BTN_ENTER_END)
        composeTestRule.waitForIdle()

        waitForMarkerCount(1)
    }

    // ==================== 消失动画 ====================

//    @Test
//    fun testDisappearAnimation_none_clearsMarkersImmediately() {
//        clickPanelButton(TAG_BTN_ADD_MARKER)
//        waitForMarkerCount(1)
//
//        switchToDisappearMode()
//        clickTopBarButton(TAG_BTN_DISAPPEAR_START)
//        composeTestRule.waitForIdle()
//
//        waitForMarkerCount(0, timeoutMs = 5000)
//        waitForNoMarkersUiState(timeoutMs = 5000)
//        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertDoesNotExist()
//    }

//    @Test
//    fun testDisappearAnimation_fadeOut_clearsMarkersAfterAnimation() {
//        clickPanelButton(TAG_BTN_ADD_MARKER)
//        waitForMarkerCount(1)
//        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
//
//        switchToDisappearMode()
//        selectDropdownOption(TAG_DROPDOWN_DISAPPEAR, "淡出 fadeOut")
//        setInput(TAG_INPUT_DISAPPEAR_DURATION, "500")
//        switchToDisappearMode()
//
//        clickTopBarButton(TAG_BTN_DISAPPEAR_START)
//        // 默认 600ms + Handler 100ms，留足缓冲
//        Thread.sleep(2500)
//
//        waitForMarkerCount(0, timeoutMs = 8000)
//        waitForNoMarkersUiState(timeoutMs = 5000)
//        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertDoesNotExist()
//    }

//    @Test
//    fun testDisappearAnimation_startAndCancel_keepsMarkers() {
//        clickPanelButton(TAG_BTN_ADD_MARKER)
//        waitForMarkerCount(1)
//        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
//
//        switchToDisappearMode()
//        selectDropdownOption(TAG_DROPDOWN_DISAPPEAR, "淡出 fadeOut")
//        setInput(TAG_INPUT_DISAPPEAR_DURATION, "3000")
//        switchToDisappearMode()
//
//        clickTopBarButton(TAG_BTN_DISAPPEAR_START)
//        switchToDisappearMode()
//        clickTopBarButton(TAG_BTN_DISAPPEAR_END)
//
//        waitForMarkerCount(1)
//        composeTestRule.onNodeWithTag(TAG_HAS_MARKERS).assertIsDisplayed()
//    }

    // ==================== 点选动画 ====================

    @Test
    fun testSelectAnimation_selectBounce_mapClick_noCrash() {
        selectDropdownOption(TAG_DROPDOWN_SELECT, "弹跳 bounce")
        clickMapCenter()
        waitForMarkerCount(1)
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        waitForMarkerCount(1)
    }

    // ==================== 配置输入 ====================

    @Test
    fun testMultipleAddClearAndReadd_succeeds() {
        repeat(2) {
            clickPanelButton(TAG_BTN_ADD_MARKER)
            waitForMarkerCount(1)
            clickPanelButton(TAG_BTN_CLEAR_ALL)
            waitForMarkerCount(0)
        }
        composeTestRule.onNodeWithTag(TAG_BTN_ADD_MARKER).performScrollTo().assertIsDisplayed()
    }
}
