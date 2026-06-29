package com.maptec.applied.demo

import android.Manifest
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.maptec.applied.demo.ext.getTestString
import com.maptec.applied.demo.ui.view.BubbleCalloutView
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.overlay.infowindow.InfoWindow
import com.maptec.applied.maps.overlay.infowindow.InfoWindowOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MarkerViewScreenTest {

    companion object {
        private const val TAG = "MarkerViewTest"
        private const val TAG_SCREEN = "markerview_screen"
        private const val TAG_BTN_ADD = "markerview_btn_add"
        private const val TAG_BTN_REMOVE = "markerview_btn_remove"
        private const val TAG_BTN_AUTO_CLOSE = "markerview_btn_auto_close"
        private const val TAG_INPUT_LINE1 = "markerview_input_line1"
        private const val TAG_INPUT_LINE2 = "markerview_input_line2"
        private const val TAG_INPUT_BG_COLOR = "markerview_input_bg_color"
        private const val TAG_INPUT_BORDER_COLOR = "markerview_input_border_color"
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

    @After
    fun tearDown() {
        Log.d(TAG, "tearDown")
    }

    private fun navigateToTargetScreen() {
        composeTestRule.onNodeWithText(getTestString(R.string.screen_item_overlay)).performClick()
        composeTestRule.onNodeWithText(getTestString(R.string.map_item_markerview))
            .performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(TAG_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun findMapView(): com.maptec.applied.maps.MapView {
        val root = composeTestRule.activity.window.decorView as android.view.ViewGroup
        val found = root.findViewWithTag<com.maptec.applied.maps.MapView>("mapView")
        return requireNotNull(found) { "MapView not found (tag=mapView)" }
    }

    private fun withMapApi(action: (com.maptec.applied.maps.MaptecMap) -> Unit) {
        val mapView = findMapView()
        val latch = CountDownLatch(1)
        val error = AtomicReference<Throwable>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            mapView.getMapAsync { map ->
                try {
                    action(map)
                } catch (e: Throwable) {
                    error.set(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue("getMapAsync timeout", latch.await(5, TimeUnit.SECONDS))
        error.get()?.let { throw it }
    }

    private fun createBubbleView(
        line1: String = "line1",
        line2: String = "line2",
    ): BubbleCalloutView = BubbleCalloutView(composeTestRule.activity).apply {
        tag = "markerview_bubble"
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        this.line1Text = line1
        this.line2Text = line2
    }

    private fun addInfoWindow(
        map: com.maptec.applied.maps.MaptecMap,
        latLng: LatLng = LatLng(1.4, 103.75),
        contentView: View = createBubbleView(),
        anchor: String = "BOTTOM",
        autoClose: Boolean = false,
    ): InfoWindow = map.getOverlayEngine().addInfoWindow(
        InfoWindowOptions()
            .withLatLng(latLng)
            .withContentView(contentView)
            .withAnchor(anchor)
            .withAutoCloseOnMapClick(autoClose),
    )

    // ==================== API 层测试 — 属性回读验证 ====================

    @Test
    fun addInfoWindow_initialProperties_correct() {
        navigateToTargetScreen()
        withMapApi { map ->
            val expectedLatLng = LatLng(1.4, 103.75)
            val contentView = createBubbleView("test a", "test b")
            val infoWindow = addInfoWindow(map, latLng = expectedLatLng, contentView = contentView, anchor = "CENTER", autoClose = true)

            assertEquals("latLng", expectedLatLng, infoWindow.latLng)
            assertEquals("anchor", "CENTER", infoWindow.anchor)
            assertTrue("autoCloseOnMapClick", infoWindow.isAutoCloseOnMapClick)
            assertTrue("visible by default", infoWindow.isVisible)
            assertEquals("contentView", contentView, infoWindow.contentView)
            assertNull("attachedMarker", infoWindow.attachedMarker)
            assertNotNull("id", infoWindow.id)

            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_setLatLng_updates() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map)
            val newLatLng = LatLng(2.0, 104.0)
            infoWindow.setLatLng(newLatLng)
            assertEquals("updated latLng", newLatLng, infoWindow.latLng)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_setAnchor_updates() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map, anchor = "BOTTOM")
            infoWindow.setAnchor("TOP")
            assertEquals("updated anchor", "TOP", infoWindow.anchor)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_toggleAutoClose() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map, autoClose = false)
            assertFalse("initially false", infoWindow.isAutoCloseOnMapClick)
            infoWindow.setAutoCloseOnMapClick(true)
            assertTrue("after set true", infoWindow.isAutoCloseOnMapClick)
            infoWindow.setAutoCloseOnMapClick(false)
            assertFalse("after set false", infoWindow.isAutoCloseOnMapClick)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_toggleVisibility() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map)
            assertTrue("visible after creation", infoWindow.isVisible)
            infoWindow.setVisible(false)
            assertFalse("hidden after setVisible(false)", infoWindow.isVisible)
            infoWindow.setVisible(true)
            assertTrue("visible after setVisible(true)", infoWindow.isVisible)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_setContentView_updates() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map)
            val newView = TextView(composeTestRule.activity).apply { text = "replaced" }
            infoWindow.setContentView(newView)
            assertEquals("updated contentView", newView, infoWindow.contentView)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_setOffsetPx_accepted() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map)
            assertEquals("default offsetX", 0f, infoWindow.offsetXPx, 0.01f)
            assertEquals("default offsetY", 0f, infoWindow.offsetYPx, 0.01f)
            infoWindow.setOffsetPx(10f, 20f)
            assertEquals("updated offsetX", 10f, infoWindow.offsetXPx, 0.01f)
            assertEquals("updated offsetY", 20f, infoWindow.offsetYPx, 0.01f)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_removeFiresCloseListener() {
        navigateToTargetScreen()
        withMapApi { map ->
            val fired = AtomicBoolean(false)
            val infoWindow = addInfoWindow(map)
            infoWindow.addOnCloseListener { fired.set(true) }
            infoWindow.remove()
            assertTrue("onClose should fire after remove", fired.get())
        }
    }

    @Test
    fun addInfoWindow_multipleWindows_independent() {
        navigateToTargetScreen()
        withMapApi { map ->
            val w1 = addInfoWindow(map, latLng = LatLng(1.4, 103.75))
            val w2 = addInfoWindow(map, latLng = LatLng(1.5, 103.8))
            assertNotNull("w1", w1)
            assertNotNull("w2", w2)
            assertEquals("w1 latLng", 1.4, w1.latLng.latitude, 0.001)
            assertEquals("w2 latLng", 1.5, w2.latLng.latitude, 0.001)
            w1.remove()
            w2.remove()
        }
    }

    @Test
    fun addInfoWindow_updatePosition_noCrash() {
        navigateToTargetScreen()
        withMapApi { map ->
            val infoWindow = addInfoWindow(map)
            infoWindow.updatePosition()
            assertTrue("still visible after updatePosition", infoWindow.isVisible)
            infoWindow.remove()
        }
    }

    @Test
    fun addInfoWindow_differentContent_customStrings() {
        navigateToTargetScreen()
        withMapApi { map ->
            val bubbleView = createBubbleView("custom line1", "custom line2")
            val infoWindow = addInfoWindow(map, contentView = bubbleView)
            assertEquals("line1 in view", "custom line1", bubbleView.line1Text)
            assertEquals("line2 in view", "custom line2", bubbleView.line2Text)
            infoWindow.remove()
        }
    }

    // ==================== UI 测试 ====================

    @Test
    fun navigation_reachesScreen() {
        navigateToTargetScreen()
        composeTestRule.onNodeWithTag(TAG_BTN_ADD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BTN_REMOVE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BTN_AUTO_CLOSE).assertIsDisplayed()
    }

    @Test
    fun removeBtn_disabledInitially() {
        navigateToTargetScreen()
        composeTestRule.onNodeWithTag(TAG_BTN_REMOVE).assertIsNotEnabled()
    }

    @Test
    fun addBtn_enabled() {
        navigateToTargetScreen()
        composeTestRule.onNodeWithTag(TAG_BTN_ADD).assertIsEnabled()
    }

    @Test
    fun inputs_disabledInitially() {
        navigateToTargetScreen()
        composeTestRule.onNodeWithTag(TAG_INPUT_LINE1).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_INPUT_LINE2).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_INPUT_BG_COLOR).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_INPUT_BORDER_COLOR).assertIsNotEnabled()
    }

    @Test
    fun toggleAutoClose_noCrash() {
        navigateToTargetScreen()
        composeTestRule.onNodeWithTag(TAG_BTN_AUTO_CLOSE).performClick()
        composeTestRule.onNodeWithTag(TAG_BTN_AUTO_CLOSE).performClick()
    }
}
