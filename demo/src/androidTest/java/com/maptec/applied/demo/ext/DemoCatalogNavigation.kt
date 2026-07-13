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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.test.platform.app.InstrumentationRegistry
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

private fun ComposeContentTestRule.performScrollToSafely(
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

private fun ComposeContentTestRule.performClickSafely(
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
        // transient state mid pop-animation. Wait and re-check instead of giving up.
        Thread.sleep(if (popped[0]) 400 else 200)
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
    R.string.overlay_item_marker -> CatalogRoutes.MARKER_BASIC
    R.string.overlay_item_marker_style -> CatalogRoutes.MARKER_STYLE
    R.string.overlay_item_marker_url -> CatalogRoutes.MARKER_URL
    R.string.overlay_item_marker_sdf -> CatalogRoutes.MARKER_SDF
    R.string.overlay_item_marker_animation -> CatalogRoutes.MARKER_ANIMATION
    R.string.map_item_markerview -> CatalogRoutes.INFO_WINDOW
    R.string.overlay_item_info_window_style -> CatalogRoutes.INFO_WINDOW_STYLE
    R.string.overlay_item_info_window_rich -> CatalogRoutes.INFO_WINDOW_RICH
    R.string.overlay_item_line -> CatalogRoutes.POLYLINE
    R.string.overlay_item_line_glow -> CatalogRoutes.POLYLINE_GLOW
    R.string.overlay_item_line_arrows -> CatalogRoutes.POLYLINE_ARROWS
    R.string.overlay_item_line_caps -> CatalogRoutes.POLYLINE_CAPS
    R.string.overlay_item_polygon_menu -> CatalogRoutes.POLYGON
    R.string.overlay_item_polygon_pattern -> CatalogRoutes.POLYGON_PATTERN
    R.string.overlay_item_polygon_translate -> CatalogRoutes.POLYGON_TRANSLATE
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
    R.string.catalog_text_search -> CatalogRoutes.TEXT_SEARCH
    R.string.catalog_nearby_search -> CatalogRoutes.NEARBY_SEARCH
    R.string.catalog_suggest -> CatalogRoutes.SUGGEST
    R.string.catalog_places -> CatalogRoutes.PLACES
    R.string.catalog_forward_geocode -> CatalogRoutes.GEOCODE
    R.string.catalog_reverse_geocode -> CatalogRoutes.REVERSE_GEOCODE
    R.string.screen_item_route -> CatalogRoutes.ROUTE
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
            onNodeWithTag(CATALOG_LIST_TAG).performScrollToNode(hasTestTag(tag))
        } else {
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
