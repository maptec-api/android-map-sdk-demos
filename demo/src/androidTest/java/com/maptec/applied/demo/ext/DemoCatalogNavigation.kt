package com.maptec.applied.demo.ext

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.test.platform.app.InstrumentationRegistry
import android.os.SystemClock
import android.view.MotionEvent
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_BASIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_DRAGGABLE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_GEODESIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_INNER_SHADOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_OUTER_GLOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_PULSE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_RADIUS_BREATH
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_SCAN
import java.util.concurrent.TimeUnit

const val DEMO_CONFIG_PANEL_TOGGLE_TAG = "demo_config_panel_toggle"
const val DEMO_CONFIG_PANEL_CLOSE_TAG = "demo_config_panel_close"
const val DEMO_BACK_BUTTON_TAG = "back_button"
private const val CATALOG_LIST_TAG = "catalog_list"

private fun catalogItemTag(route: String) = "catalog_item_$route"

/**
 * Like [waitForIdle] but tolerant of screens that never reach Compose idle.
 *
 * Map demos embed an Android `MapView` that keeps Compose in perpetual
 * measure/layout, so a strict [waitForIdle] throws [ComposeNotIdleException].
 * Navigation and config-panel helpers only need the UI to settle enough to
 * interact with, so we swallow that timeout and fall back to a short delay.
 */
internal fun Throwable.isComposeBusy(): Boolean =
    this is ComposeNotIdleException || cause is IdlingResourceTimeoutException

/**
 * Polls [condition] without synchronizing on Compose idle between attempts.
 *
 * [waitUntil] waits for idle before every check. Map screens with embedded
 * Android Views never reach idle, so the full timeout is spent waiting rather
 * than polling. A short sleep between attempts lets us observe semantics during
 * brief layout gaps instead.
 */
internal fun ComposeContentTestRule.pollUntil(
    timeoutMillis: Long,
    pollIntervalMillis: Long = 200L,
    condition: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        Thread.sleep(pollIntervalMillis)
    }
    if (condition()) return
    throw AssertionError("Condition not satisfied within ${timeoutMillis}ms")
}

fun ComposeContentTestRule.settleForIdle(settleMillis: Long = 300L) {
    try {
        waitForIdle()
    } catch (e: Throwable) {
        if (e.isComposeBusy()) {
            Thread.sleep(settleMillis)
        } else {
            throw e
        }
    }
}

/**
 * Checks whether any node exists without failing when Compose never reaches idle.
 *
 * [fetchSemanticsNodes] internally waits for idle, so it throws
 * [ComposeNotIdleException] on map screens. We treat "still busy / timed out" the
 * same as "not found yet" so callers can retry (scroll / swipe) instead of crashing.
 */
internal fun SemanticsNodeInteractionCollection.existsSafely(): Boolean =
    runCatching { fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        .getOrDefault(false)

internal fun ComposeContentTestRule.performScrollToSafely(
    interaction: SemanticsNodeInteraction,
    settleMillis: Long = 300L,
) {
    runCatching { interaction.performScrollTo() }
        .onFailure { error ->
            if (error.isComposeBusy()) {
                Thread.sleep(settleMillis)
            } else {
                throw error
            }
        }
}

internal fun ComposeContentTestRule.performClickSafely(
    interaction: SemanticsNodeInteraction,
    settleMillis: Long = 500L,
) {
    val result = runCatching { interaction.performClick() }
    if (result.isSuccess) return
    val error = result.exceptionOrNull()!!
    if (!error.isComposeBusy()) throw error
    Thread.sleep(settleMillis)
    interaction.performClick()
}

/** Waits until a test tag appears without requiring Compose idle on every poll. */
fun ComposeContentTestRule.waitForTag(
    tag: String,
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(10),
) {
    pollUntil(timeoutMillis) {
        onAllNodesWithTag(tag).existsSafely()
    }
}

/**
 * Presses back until the main catalog is reached.
 *
 * Map overlays with Android Views (e.g. InfoWindow) can keep Compose in perpetual
 * measure/layout, which makes any semantics query / [waitForIdle] time out. So this
 * helper drives navigation via the Activity [androidx.activity.OnBackPressedDispatcher]
 * rather than tapping the app-bar back button.
 *
 * Termination is keyed off actually arriving at the root catalog (detected by a
 * main-only top-level entry) instead of [androidx.activity.OnBackPressedDispatcher.hasEnabledCallbacks].
 * While a Navigation-Compose pop animation is still settling, the nav component's
 * back callback can be transiently disabled; trusting that flag alone stops the loop
 * one level short of `main` (e.g. stranded on a sub-catalog).
 */
fun ComposeContentTestRule.resetToMainCatalog(maxSteps: Int = 12) {
    val androidRule = this as? AndroidComposeTestRule<*, *> ?: return
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // Unique to MainCatalogScreen; absent on every sub-catalog and map demo screen.
    val mainCatalogMarker = catalogItemTag(CatalogRoutes.MAPS)
    repeat(maxSteps) {
        val activity = androidRule.activity
        if (activity.isFinishing) return
        // Already at the root catalog once its top-level entries are composed. Checked
        // before pressing back so we never over-pop and finish the Activity.
        if (onAllNodesWithTag(mainCatalogMarker).existsSafely()) return
        val popped = booleanArrayOf(false)
        instrumentation.runOnMainSync {
            val dispatcher = activity.onBackPressedDispatcher
            if (dispatcher.hasEnabledCallbacks()) {
                dispatcher.onBackPressed()
                popped[0] = true
            }
        }
        // A disabled dispatcher usually means we are at the root, but it can also be a
        // transient state mid pop-animation. Poll with short intervals instead of static sleep.
        val waitLimit = if (popped[0]) 400L else 200L
        val deadline = System.currentTimeMillis() + waitLimit
        while (System.currentTimeMillis() < deadline) {
            if (onAllNodesWithTag(mainCatalogMarker).existsSafely()) return
            Thread.sleep(50)
        }
    }
}

private fun catalogRouteFor(textResId: Int): String? = when (textResId) {
    R.string.screen_item_attention -> CatalogRoutes.AUTH
    R.string.catalog_main_maps -> CatalogRoutes.MAPS
    R.string.catalog_main_annotations -> CatalogRoutes.ANNOTATIONS
    R.string.catalog_main_interaction -> CatalogRoutes.INTERACTION
    R.string.catalog_main_web_services -> CatalogRoutes.WEB_SERVICES
    R.string.catalog_ui_controls -> CatalogRoutes.UI_CONTROLS
    R.string.map_item_map_gesture -> CatalogRoutes.GESTURES
    R.string.catalog_user_location -> CatalogRoutes.USER_LOCATION
    R.string.map_item_map_render -> CatalogRoutes.ADD_MAP
    R.string.map_item_map_style -> CatalogRoutes.MAP_STYLE
    R.string.map_item_map_control -> CatalogRoutes.CAMERA
    R.string.map_item_map_traffic -> CatalogRoutes.MAP_TRAFFIC
    R.string.overlay_item_marker -> CatalogRoutes.MARKER_BASIC
    R.string.overlay_item_marker_style -> CatalogRoutes.MARKER_STYLE
    R.string.overlay_item_marker_url -> CatalogRoutes.MARKER_URL
    R.string.overlay_item_marker_sdf -> CatalogRoutes.MARKER_SDF
    R.string.overlay_item_marker_animation -> CatalogRoutes.MARKER_ANIMATION
    R.string.overlay_item_marker_move_along -> CatalogRoutes.MARKER_MOVE_ALONG
    R.string.overlay_item_marker_collision -> CatalogRoutes.MARKER_COLLISION
    R.string.map_item_markerview -> CatalogRoutes.INFO_WINDOW
    R.string.overlay_item_info_window_style -> CatalogRoutes.INFO_WINDOW_STYLE
    R.string.overlay_item_info_window_rich -> CatalogRoutes.INFO_WINDOW_RICH
    R.string.overlay_item_line -> CatalogRoutes.POLYLINE
    R.string.overlay_item_line_glow -> CatalogRoutes.POLYLINE_GLOW
    R.string.overlay_item_line_arrows -> CatalogRoutes.POLYLINE_ARROWS
    R.string.overlay_item_line_caps -> CatalogRoutes.POLYLINE_CAPS
    R.string.overlay_item_line_gradient -> CatalogRoutes.POLYLINE_GRADIENT
    R.string.overlay_item_polygon_menu -> CatalogRoutes.POLYGON
    R.string.overlay_item_polygon_pattern -> CatalogRoutes.POLYGON_PATTERN
    R.string.overlay_item_polygon_interaction -> CatalogRoutes.POLYGON_INTERACTION
    R.string.overlay_item_geofence_point -> CatalogRoutes.GEOFENCE_POINT
    R.string.overlay_item_geofence_polygon -> CatalogRoutes.GEOFENCE_POLYGON
    R.string.map_item_zoom -> CatalogRoutes.ZOOM
    R.string.map_item_compass -> CatalogRoutes.COMPASS
    R.string.map_item_scale_bar -> CatalogRoutes.SCALE_BAR
    R.string.map_item_day_night_mode -> CatalogRoutes.DAY_NIGHT
    R.string.map_item_logo -> CatalogRoutes.LOGO
    R.string.map_item_location_layer -> CatalogRoutes.LOCATION_LAYER
    R.string.map_item_location_button -> CatalogRoutes.LOCATION_BUTTON
    R.string.map_item_location -> CatalogRoutes.LOCATION_LAYER
    R.string.map_item_map_event_listener -> CatalogRoutes.MAP_EVENTS
    R.string.map_item_poi_click_center -> CatalogRoutes.POI_QUERY
    R.string.map_item_overlay_click -> CatalogRoutes.OVERLAY_CLICK
    R.string.map_item_limit_bounds -> CatalogRoutes.LIMIT_BOUNDS
    R.string.map_item_camera_target_bounds -> CatalogRoutes.CAMERA_TARGET_BOUNDS
    R.string.map_item_camera_animation -> CatalogRoutes.CAMERA_ANIMATION
    R.string.map_item_content_padding -> CatalogRoutes.CONTENT_PADDING
    R.string.catalog_text_search -> CatalogRoutes.TEXT_SEARCH
    R.string.catalog_nearby_search -> CatalogRoutes.NEARBY_SEARCH
    R.string.catalog_along_route_search -> CatalogRoutes.ALONG_ROUTE_SEARCH
    R.string.catalog_suggest -> CatalogRoutes.SUGGEST
    R.string.catalog_places -> CatalogRoutes.PLACES
    R.string.catalog_forward_geocode -> CatalogRoutes.GEOCODE
    R.string.catalog_reverse_geocode -> CatalogRoutes.REVERSE_GEOCODE
    R.string.screen_item_route -> CatalogRoutes.ROUTE
    R.string.catalog_tools -> CatalogRoutes.TOOLS
    R.string.catalog_geometry_demo -> CatalogRoutes.GEOMETRY_DEMO
    R.string.map_item_map_gesture_control -> CatalogRoutes.GESTURE_CONTROL
    R.string.map_item_gesture_coordination -> CatalogRoutes.GESTURE_COORDINATION
    R.string.map_item_double_tap_delay -> CatalogRoutes.GESTURE_DOUBLE_TAP_DELAY
    R.string.map_item_zoom_center_mode -> CatalogRoutes.GESTURE_ZOOM_CENTER_MODE
    R.string.map_item_double_tap_zoom_factor -> CatalogRoutes.GESTURE_DOUBLE_TAP_ZOOM_FACTOR
    R.string.map_item_gesture_threshold -> CatalogRoutes.GESTURE_THRESHOLD
    R.string.map_item_rotate_bearing_range -> CatalogRoutes.GESTURE_ROTATE_BEARING_RANGE
    R.string.map_item_fling_duration -> CatalogRoutes.GESTURE_FLING_DURATION
    R.string.map_item_zoom_animation_duration -> CatalogRoutes.GESTURE_ZOOM_ANIMATION_DURATION
    R.string.map_item_two_finger_tap_zoom -> CatalogRoutes.GESTURE_TWO_FINGER_TAP_ZOOM
    R.string.map_item_double_tap_tilt_reset -> CatalogRoutes.GESTURE_DOUBLE_TAP_TILT_RESET
    R.string.circle_list_basic -> ROUTE_CIRCLE_BASIC
    R.string.circle_list_geodesic -> ROUTE_CIRCLE_GEODESIC
    R.string.circle_list_draggable -> ROUTE_CIRCLE_DRAGGABLE
    R.string.circle_list_inner_shadow -> ROUTE_CIRCLE_INNER_SHADOW
    R.string.circle_list_outer_glow -> ROUTE_CIRCLE_OUTER_GLOW
    R.string.circle_list_scan -> ROUTE_CIRCLE_SCAN
    R.string.circle_list_pulse -> ROUTE_CIRCLE_PULSE
    R.string.circle_list_radius_breath -> ROUTE_CIRCLE_RADIUS_BREATH
    R.string.overlay_item_heatmap_basic -> CatalogRoutes.HEATMAP_BASIC
    R.string.overlay_item_model_basic -> CatalogRoutes.MODEL_BASIC
    R.string.overlay_item_point_cluster_basic -> CatalogRoutes.POINT_CLUSTER_BASIC
    R.string.catalog_dev_special -> CatalogRoutes.DEV_SPECIAL
    R.string.catalog_overlay_z_order -> CatalogRoutes.OVERLAY_Z_ORDER
    else -> null
}

/** Scrolls lazy catalog lists and clicks a row by stable route id. */
fun ComposeContentTestRule.clickCatalogItem(
    route: String,
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(30),
) {
    val tag = catalogItemTag(route)
    settleForIdle()
    if (!onAllNodesWithTag(tag).existsSafely()) {
        if (onAllNodesWithTag(CATALOG_LIST_TAG).existsSafely()) {
            runCatching {
                onNodeWithTag(CATALOG_LIST_TAG).performScrollToNode(hasTestTag(tag))
            }
        }
        if (!onAllNodesWithTag(tag).existsSafely()) {
            waitUntil(timeoutMillis) {
                if (onAllNodesWithTag(tag).existsSafely()) {
                    true
                } else {
                    onRoot().performTouchInput {
                        swipeUp(startY = bottom * 0.75f, endY = bottom * 0.25f)
                    }
                    settleForIdle()
                    false
                }
            }
        }
    }
    val item = onNodeWithTag(tag)
    performScrollToSafely(item)
    performClickSafely(item)
    settleForIdle()
}

private fun clickableTextMatcher(label: String) = hasText(label) and hasClickAction()

/** Scrolls lazy catalog lists until a clickable row with [label] is composed. */
private fun ComposeContentTestRule.scrollUntilClickableTextVisible(
    label: String,
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(15),
) {
    val matcher = clickableTextMatcher(label)
    if (!onAllNodes(matcher).existsSafely() &&
        onAllNodesWithTag(CATALOG_LIST_TAG).existsSafely()
    ) {
        onNodeWithTag(CATALOG_LIST_TAG).performScrollToNode(hasText(label) and hasClickAction())
    }
    waitUntil(timeoutMillis) {
        if (onAllNodes(matcher).existsSafely()) {
            true
        } else {
            if (onAllNodesWithTag(CATALOG_LIST_TAG).existsSafely()) {
                onNodeWithTag(CATALOG_LIST_TAG).performTouchInput {
                    swipeUp(startY = bottom * 0.75f, endY = bottom * 0.25f)
                }
            } else {
                onRoot().performTouchInput {
                    swipeUp(startY = bottom * 0.75f, endY = bottom * 0.25f)
                }
            }
            settleForIdle()
            false
        }
    }
}

/** Clicks a list row by string resource, skipping non-clickable section headers with the same text. */
fun ComposeContentTestRule.clickClickableText(textResId: Int) {
    catalogRouteFor(textResId)?.let { route ->
        clickCatalogItem(route)
        return
    }

    val label = getTestString(textResId)
    settleForIdle()
    scrollUntilClickableTextVisible(label)
    val item = onNode(clickableTextMatcher(label))
    performScrollToSafely(item)
    performClickSafely(item)
    settleForIdle()
}

fun ComposeContentTestRule.clickCatalogPath(vararg titleResIds: Int) {
    for (titleResId in titleResIds) {
        clickClickableText(titleResId)
    }
}

fun ComposeContentTestRule.expandConfigPanel() {
    settleForIdle()
    if (onAllNodesWithTag(DEMO_CONFIG_PANEL_TOGGLE_TAG).existsSafely()) {
        performClickSafely(onNodeWithTag(DEMO_CONFIG_PANEL_TOGGLE_TAG))
        settleForIdle()
    }
}

fun ComposeContentTestRule.collapseConfigPanel() {
    settleForIdle()
    if (onAllNodesWithTag(DEMO_CONFIG_PANEL_CLOSE_TAG).existsSafely()) {
        performClickSafely(onNodeWithTag(DEMO_CONFIG_PANEL_CLOSE_TAG))
        settleForIdle()
    }
}

fun ComposeContentTestRule.waitForMapDemoReady(
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(30),
    tag: String = "mapRendered",
) {
    waitForMapRendered(timeoutMillis = timeoutMillis, tag = tag)
    expandConfigPanel()
}

/** 若当前已处于渲染好的 MapRender 页面则直接复用，避免套件高频退出重建导致引擎底层缓存竞争 */
fun ComposeContentTestRule.ensureMapRenderDemoReady(
    timeoutMillis: Long = TimeUnit.SECONDS.toMillis(30),
) {
    if (!onAllNodesWithTag("mapRendered").existsSafely()) {
        resetToMainCatalog()
        openMapsDemo(R.string.map_item_map_render)
        waitForMapDemoReady(timeoutMillis = timeoutMillis)
    }
}

fun ComposeContentTestRule.openMapsDemo(itemResId: Int) {
    clickCatalogPath(R.string.catalog_main_maps, itemResId)
}

fun ComposeContentTestRule.openAnnotationsDemo(vararg pathResIds: Int) {
    clickCatalogPath(R.string.catalog_main_annotations, *pathResIds)
}

fun ComposeContentTestRule.openInteractionDemo(vararg pathResIds: Int) {
    clickCatalogPath(R.string.catalog_main_interaction, *pathResIds)
}

fun ComposeContentTestRule.openUiControlsDemo(itemResId: Int) {
    openInteractionDemo(R.string.catalog_ui_controls, itemResId)
}

fun ComposeContentTestRule.openWebServicesDemo(itemResId: Int) {
    clickCatalogPath(R.string.catalog_main_web_services, itemResId)
}

fun ComposeContentTestRule.openGeometryDemo() {
    clickCatalogPath(
        R.string.catalog_tools,
        R.string.catalog_geometry_demo,
    )
}

/** [com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoCapability.name] */
fun ComposeContentTestRule.selectGeometryChip(capabilityName: String) {
    settleForIdle()
    performClickSafely(
        onNodeWithTag("geometry_chip_$capabilityName", useUnmergedTree = true),
    )
    Thread.sleep(350)
}

/** [com.maptec.applied.demo.ui.screens.tools.geometry.GeometryDemoTab.name] */
fun ComposeContentTestRule.selectGeometryTab(tabName: String) {
    settleForIdle()
    performClickSafely(
        onNodeWithTag("geometry_tab_$tabName", useUnmergedTree = true),
    )
    Thread.sleep(350)
}

fun ComposeContentTestRule.openGeometryDemoReady() {
    openGeometryDemo()
    waitForMapRendered()
}

/** 状态条或配置面板文案（不强制展开面板，减少 idle 等待）。 */
fun ComposeContentTestRule.assertGeometryDemoTextContains(token: String) {
    withShortComposeIdleTimeout {
        pollUntil(15_000) {
            onAllNodesWithText(token, substring = true, useUnmergedTree = true).existsSafely()
        }
    }
}

fun ComposeContentTestRule.assertGeometryDemoTextAbsent(token: String, timeoutMillis: Long = 5_000) {
    withShortComposeIdleTimeout {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (!onAllNodesWithText(token, substring = true, useUnmergedTree = true).existsSafely()) {
                return@withShortComposeIdleTimeout
            }
            Thread.sleep(200)
        }
        throw AssertionError("仍可见文案: $token")
    }
}

/**
 * 几何计算页嵌了 [com.maptec.applied.maps.MapView]，Compose 经常到不了 idle。
 * 这里直接往 MapView 派发点击，避免 [performTouchInput] 卡在 waitForIdle。
 */
fun ComposeContentTestRule.clickGeometryMap(vararg percents: Pair<Float, Float>) {
    waitForMapRendered()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val mapView = getMapView()
    percents.forEach { (xPercent, yPercent) ->
        instrumentation.runOnMainSync {
            val maxX = (mapView.width - 1).toFloat().coerceAtLeast(1f)
            val maxY = (mapView.height - 1).toFloat().coerceAtLeast(1f)
            val x = (mapView.width * xPercent).coerceIn(1f, maxX)
            val y = (mapView.height * yPercent).coerceIn(1f, maxY)
            val downTime = SystemClock.uptimeMillis()
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0),
            )
            mapView.dispatchTouchEvent(
                MotionEvent.obtain(downTime, downTime + 30, MotionEvent.ACTION_UP, x, y, 0),
            )
        }
        Thread.sleep(700)
    }
}

fun ComposeContentTestRule.assertGeometryResultContains(token: String) {
    expandConfigPanel()
    withShortComposeIdleTimeout {
        pollUntil(15_000) {
            onAllNodesWithText(token, substring = true, useUnmergedTree = true).existsSafely()
        }
    }
}

fun ComposeContentTestRule.clickGeometryClearButton() {
    expandConfigPanel()
    performClickSafely(onNodeWithTag("geometry_clear_button"))
    Thread.sleep(400)
}

/**
 * 地图页 [fetchSemanticsNodes] 会先等 idle；默认超时很长，看起来像一直转圈。
 * 断言期间把 idle 超时压短，busy 时快速重试。
 */
private inline fun withShortComposeIdleTimeout(block: () -> Unit) {
    val master = IdlingPolicies.getMasterIdlingPolicy()
    val resource = IdlingPolicies.getDynamicIdlingResourceErrorPolicy()
    IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.SECONDS)
    IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.SECONDS)
    try {
        block()
    } finally {
        IdlingPolicies.setMasterPolicyTimeout(master.idleTimeout, master.idleTimeoutUnit)
        IdlingPolicies.setIdlingResourceTimeout(resource.idleTimeout, resource.idleTimeoutUnit)
    }
}

fun ComposeContentTestRule.openDevSpecialDemo(itemResId: Int) {
    clickCatalogPath(R.string.catalog_dev_special, itemResId)
}
