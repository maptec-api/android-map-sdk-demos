package com.maptec.applied.demo.map

import android.Manifest
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.MainActivity
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ext.getMapView
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ext.waitForMapRendered
import com.maptec.applied.maps.GestureSettings
import com.maptec.applied.maps.MapGestureType
import com.maptec.applied.maps.MapView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 手势协调（互斥 / 同时）功能测试
 */
@RunWith(AndroidJUnit4::class)
class MapGestureCoordinationScreenTest {

    companion object {
        private const val TAG_SWITCH_SIMULTANEOUS = "switch_simultaneous_gestures"
        private const val TAG_BTN_CONFIRM = "button_confirm_custom_mutual"

        private fun tagCheckbox(type: MapGestureType): String = "checkbox_gesture_${type.typeId}"
    }

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

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        navigateMapGestureCoordinationScreen()
        composeTestRule.waitForMapRendered()
        mapView = composeTestRule.getMapView()
    }

    @After
    fun tearDown() {
        composeTestRule.waitForIdle()
    }

    private fun navigateMapGestureCoordinationScreen() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_map)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_map_gesture)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_gesture_coordination)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun withGestureSettings(action: (GestureSettings) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        mapView.getMapAsync { map ->
            try {
                action(map.uiSettings.gestures)
            } catch (e: Throwable) {
                error = e
            } finally {
                latch.countDown()
            }
        }
        assertTrue("异步获取 GestureSettings 超时", latch.await(3, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    private fun toggleGestureCheckbox(type: MapGestureType) {
        composeTestRule.onNodeWithTag(tagCheckbox(type)).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun clickConfirmButton(reExpand: Boolean = false) {
        composeTestRule.onNodeWithTag(TAG_BTN_CONFIRM).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(300)
        composeTestRule.waitForIdle()

        if (reExpand) {
            composeTestRule.onRoot().performTouchInput {
                swipe(
                    start = Offset(centerX, bottom - 10f),
                    end = Offset(centerX, top + 100f),
                    durationMillis = 300
                )
            }
            composeTestRule.waitForIdle()
            Thread.sleep(300)
            composeTestRule.waitForIdle()
        }
    }

    private fun toggleSimultaneousSwitch() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_SIMULTANEOUS).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testSimultaneousSwitch_DefaultOn() {
        composeTestRule.onNodeWithTag(TAG_SWITCH_SIMULTANEOUS).assertIsOn()
        withGestureSettings { gs ->
            assertTrue("默认应允许多手势同时执行", gs.isSimultaneousGesturesAllowed)
        }
    }

    @Test
    fun testSimultaneousSwitch_ToggleOff() {
        toggleSimultaneousSwitch()
        composeTestRule.onNodeWithTag(TAG_SWITCH_SIMULTANEOUS).assertIsOff()

        withGestureSettings { gs ->
            assertFalse("关闭后不应允许多手势同时执行", gs.isSimultaneousGesturesAllowed)
        }
    }

    @Test
    fun testSimultaneousSwitch_CheckboxesAndButtonDisabledWhenOff() {
        toggleSimultaneousSwitch()

        composeTestRule.onNodeWithTag(tagCheckbox(MapGestureType.SCALE)).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(tagCheckbox(MapGestureType.ROTATE)).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_BTN_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun testMutualExclusion_SelectScaleAndRotate() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        toggleGestureCheckbox(MapGestureType.ROTATE)
        clickConfirmButton()

        withGestureSettings { gs ->
            val sets = gs.mutuallyExclusiveGestures
            assertEquals("应有一组互斥集合", 1, sets.size)
            assertTrue("缩放应在互斥集合中", sets[0].contains(MapGestureType.SCALE))
            assertTrue("旋转应在互斥集合中", sets[0].contains(MapGestureType.ROTATE))
        }
    }

    @Test
    fun testMutualExclusion_SelectAllGestures() {
        val allGestures = listOf(
            MapGestureType.SCALE,
            MapGestureType.ROTATE,
            MapGestureType.SHOVE,
            MapGestureType.SIDEWAYS_SHOVE,
            MapGestureType.MOVE,
            MapGestureType.LONG_PRESS,
            MapGestureType.MULTI_FINGER_TAP
        )

        allGestures.forEach { type -> toggleGestureCheckbox(type) }
        clickConfirmButton()

        withGestureSettings { gs ->
            val sets = gs.mutuallyExclusiveGestures
            assertEquals("应有一组互斥集合", 1, sets.size)
            assertEquals("所有7种可选手势应在互斥集合中", 7, sets[0].size)
            assertTrue(sets[0].containsAll(allGestures))
        }
    }

    @Test
    fun testMutualExclusion_ToggleOnThenOffThenOn_PreservesSelection() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        toggleGestureCheckbox(MapGestureType.ROTATE)
        clickConfirmButton(reExpand = true)

        toggleSimultaneousSwitch()
        composeTestRule.onNodeWithTag(TAG_SWITCH_SIMULTANEOUS).assertIsOff()

        withGestureSettings { gs ->
            assertFalse("关闭 simultaneous 后应为 false", gs.isSimultaneousGesturesAllowed)
            assertEquals("关闭 simultaneous 不应清除底层的互斥集合数据", 1, gs.mutuallyExclusiveGestures.size)
        }

        toggleSimultaneousSwitch()
        composeTestRule.onNodeWithTag(TAG_SWITCH_SIMULTANEOUS).assertIsOn()

        withGestureSettings { gs ->
            assertTrue("重新开启后应允许多手势同时执行", gs.isSimultaneousGesturesAllowed)
            val sets = gs.mutuallyExclusiveGestures
            assertEquals("重新开启后互斥集合数量不变", 1, sets.size)
            assertTrue("缩放仍在互斥集合中", sets[0].contains(MapGestureType.SCALE))
            assertTrue("旋转仍在互斥集合中", sets[0].contains(MapGestureType.ROTATE))
        }
    }

    @Test
    fun testMutualExclusion_ClearSelection() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        clickConfirmButton(reExpand = true)

        toggleGestureCheckbox(MapGestureType.SCALE)
        clickConfirmButton(reExpand = false)

        withGestureSettings { gs ->
            assertTrue("清空选择并确定后互斥列表应为空", gs.mutuallyExclusiveGestures.isEmpty())
        }
    }

    @Test
    fun testMutualExclusion_EmptyConfirmClearsMutualExclusion() {
        clickConfirmButton()

        withGestureSettings { gs ->
            assertTrue("未选择任何手势时直接确定，互斥列表应为空", gs.mutuallyExclusiveGestures.isEmpty())
        }
    }

    @Test
    fun testSimultaneousSwitch_ToggleOff_ExcludesMutualExclusionConfirmation() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        toggleGestureCheckbox(MapGestureType.ROTATE)
        clickConfirmButton(reExpand = true)

        toggleSimultaneousSwitch()

        withGestureSettings { gs ->
            assertFalse(gs.isSimultaneousGesturesAllowed)
            assertEquals("关闭 simultaneous 后，数据层互斥配置应保持不变", 1, gs.mutuallyExclusiveGestures.size)
        }
    }

    @Test
    fun testMutualExclusion_MultipleConfirmsReplacesGroup() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        toggleGestureCheckbox(MapGestureType.ROTATE)
        clickConfirmButton(reExpand = true)

        toggleGestureCheckbox(MapGestureType.SHOVE)
        clickConfirmButton(reExpand = false)

        withGestureSettings { gs ->
            val sets = gs.mutuallyExclusiveGestures
            assertEquals("应只有一组互斥集合", 1, sets.size)
            assertEquals("第二次确定应将之前的配置替换为当前选择的 3 项", 3, sets[0].size)
            assertTrue("包含倾斜(Shove)", sets[0].contains(MapGestureType.SHOVE))
        }
    }

    @Test
    fun testMutualExclusion_UncheckAndConfirmReplacesGroup() {
        toggleGestureCheckbox(MapGestureType.SCALE)
        toggleGestureCheckbox(MapGestureType.SHOVE)
        clickConfirmButton(reExpand = true)

        toggleGestureCheckbox(MapGestureType.SCALE)
        clickConfirmButton(reExpand = false)

        withGestureSettings { gs ->
            val sets = gs.mutuallyExclusiveGestures
            assertEquals("应只有一组互斥集合", 1, sets.size)
            assertEquals("取消选中后，互斥组应只剩 1 项", 1, sets[0].size)
            assertTrue("剩下的应为倾斜(Shove)", sets[0].contains(MapGestureType.SHOVE))
        }
    }
}
