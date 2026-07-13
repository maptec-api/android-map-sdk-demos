package com.maptec.applied.demo.ui.screens.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.interaction.controls.CompassScreen
import com.maptec.applied.demo.ui.screens.interaction.controls.DayNightScreen
import com.maptec.applied.demo.ui.screens.interaction.controls.LogoScreen
import com.maptec.applied.demo.ui.screens.interaction.InteractionCatalogScreen
import com.maptec.applied.demo.ui.screens.interaction.controls.ControlsCatalogScreen
import com.maptec.applied.demo.ui.screens.interaction.location.LocationLayerScreen
import com.maptec.applied.demo.ui.screens.interaction.location.LocationCatalogScreen
import com.maptec.applied.demo.ui.screens.maps.MapsCatalogScreen
import com.maptec.applied.demo.ui.screens.overlays.OverlaysCatalogScreen
import com.maptec.applied.demo.ui.screens.maps.MapControlScreen
import com.maptec.applied.demo.ui.screens.interaction.MapEventListenerScreen
import com.maptec.applied.demo.ui.screens.maps.MapRenderScreen
import com.maptec.applied.demo.ui.screens.maps.MapStyleScreen
import com.maptec.applied.demo.ui.screens.interaction.PoiClickScreen
import com.maptec.applied.demo.ui.screens.interaction.controls.ScaleScreen
import com.maptec.applied.demo.ui.screens.interaction.controls.ZoomScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.GesturesCatalogScreen
import com.maptec.applied.demo.ui.screens.interaction.location.LocationButtonScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapDoubleTapTiltResetScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapDoubleTapZoomFactorScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapFlingDurationScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapGestureCoordinationScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapGestureScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapGestureThresholdScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapRotateBearingRangeScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapTwoFingerTapZoomScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapZoomAnimationDurationScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapZoomCenterModeScreen
import com.maptec.applied.demo.ui.screens.overlays.geofence.GeofencePointScreen
import com.maptec.applied.demo.ui.screens.overlays.geofence.GeofencePolygonScreen
import com.maptec.applied.demo.ui.screens.overlays.polyline.PolylineArrowsScreen
import com.maptec.applied.demo.ui.screens.overlays.polyline.PolylineBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.polyline.PolylineCapsScreen
import com.maptec.applied.demo.ui.screens.overlays.polyline.PolylineGlowScreen
import com.maptec.applied.demo.ui.screens.overlays.marker.MarkerAnimationScreen
import com.maptec.applied.demo.ui.screens.overlays.marker.MarkerBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.marker.MarkerSdfScreen
import com.maptec.applied.demo.ui.screens.overlays.marker.MarkerStyleScreen
import com.maptec.applied.demo.ui.screens.overlays.marker.MarkerUrlScreen
import com.maptec.applied.demo.ui.screens.overlays.info_window.InfoWindowBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.info_window.InfoWindowRichScreen
import com.maptec.applied.demo.ui.screens.overlays.info_window.InfoWindowStyleScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleDraggableScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleGeodesicScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleInnerShadowScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleOuterGlowScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CirclePulseScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleRadiusBreathScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleScanScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_BASIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_DRAGGABLE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_GEODESIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_INNER_SHADOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_OUTER_GLOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_PULSE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_RADIUS_BREATH
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_SCAN
import com.maptec.applied.demo.ui.screens.overlays.polygon.PolygonBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.polygon.PolygonInteractionScreen
import com.maptec.applied.demo.ui.screens.overlays.polygon.PolygonPatternScreen
import com.maptec.applied.demo.ui.screens.overlays.polygon.PolygonTranslateScreen
import com.maptec.applied.demo.ui.screens.web_services.geocode.ForwardGeocodeScreen
import com.maptec.applied.demo.ui.screens.web_services.geocode.ReverseGeocodeScreen
import com.maptec.applied.demo.ui.screens.web_services.search.NearbySearchScreen
import com.maptec.applied.demo.ui.screens.web_services.search.PlaceDetailScreen
import com.maptec.applied.demo.ui.screens.web_services.search.SuggestSearchScreen
import com.maptec.applied.demo.ui.screens.web_services.search.TextSearchScreen
import com.maptec.applied.demo.ui.screens.web_services.RouteOverlayScreen
import com.maptec.applied.demo.ui.screens.web_services.WebServicesCatalogScreen
import com.maptec.applied.demo.ui.screens.interaction.gestures.MapDoubleTapDelayScreen

object CatalogRoutes {
    const val AUTH = "catalog_auth"
    const val MAPS = "catalog_maps"
    const val ANNOTATIONS = "catalog_annotations"
    const val INTERACTION = "catalog_interaction"
    const val UI_CONTROLS = "catalog_ui_controls"
    const val GESTURES = "catalog_gestures"
    const val USER_LOCATION = "catalog_user_location"
    const val WEB_SERVICES = "catalog_web_services"

    const val ADD_MAP = "catalog_add_map"
    const val MAP_STYLE = "catalog_map_style"
    const val CAMERA = "catalog_camera"
    const val MARKER_BASIC = "catalog_marker_basic"
    const val MARKER_STYLE = "catalog_marker_style"
    const val MARKER_URL = "catalog_marker_url"
    const val MARKER_SDF = "catalog_marker_sdf"
    const val MARKER_ANIMATION = "catalog_marker_animation"
    const val INFO_WINDOW = "catalog_info_window"
    const val INFO_WINDOW_STYLE = "catalog_info_window_style"
    const val INFO_WINDOW_RICH = "catalog_info_window_rich"
    const val POLYLINE = "catalog_polyline"
    const val POLYLINE_GLOW = "catalog_polyline_glow"
    const val POLYLINE_ARROWS = "catalog_polyline_arrows"
    const val POLYLINE_CAPS = "catalog_polyline_caps"
    const val POLYGON = "catalog_polygon"
    const val POLYGON_PATTERN = "catalog_polygon_pattern"
    const val POLYGON_TRANSLATE = "catalog_polygon_translate"
    const val POLYGON_INTERACTION = "catalog_polygon_interaction"
    const val ZOOM = "catalog_zoom"
    const val COMPASS = "catalog_compass"
    const val SCALE_BAR = "catalog_scale_bar"
    const val DAY_NIGHT = "catalog_day_night"
    const val LOGO = "catalog_logo"
    const val LOCATION_LAYER = "catalog_location_layer"
    const val LOCATION_BUTTON = "catalog_location_button"
    const val MAP_EVENTS = "catalog_map_events"
    const val POI_QUERY = "catalog_poi_query"
    const val TEXT_SEARCH = "catalog_text_search"
    const val NEARBY_SEARCH = "catalog_nearby_search"
    const val SUGGEST = "catalog_suggest"
    const val PLACES = "catalog_places"
    const val GEOCODE = "catalog_geocode"
    const val REVERSE_GEOCODE = "catalog_reverse_geocode"
    const val ROUTE = "catalog_route"
    const val GEOFENCE_POINT = "catalog_geofence_point"
    const val GEOFENCE_POLYGON = "catalog_geofence_polygon"

    const val GESTURE_CONTROL = "map_gesture_control"
    const val GESTURE_COORDINATION = "map_gesture_coordination"
    const val GESTURE_DOUBLE_TAP_DELAY = "map_double_tap_delay"
    const val GESTURE_ZOOM_CENTER_MODE = "map_zoom_center_mode"
    const val GESTURE_DOUBLE_TAP_ZOOM_FACTOR = "map_double_tap_zoom_factor"
    const val GESTURE_THRESHOLD = "map_gesture_threshold"
    const val GESTURE_ROTATE_BEARING_RANGE = "map_rotate_bearing_range"
    const val GESTURE_FLING_DURATION = "map_fling_duration"
    const val GESTURE_ZOOM_ANIMATION_DURATION = "map_zoom_animation_duration"
    const val GESTURE_TWO_FINGER_TAP_ZOOM = "map_two_finger_tap_zoom"
    const val GESTURE_DOUBLE_TAP_TILT_RESET = "map_double_tap_tilt_reset"
}

fun NavGraphBuilder.catalogRoutes(
    navController: NavHostController,
    registerNestedBackHandler: ((() -> Boolean)?) -> Unit = {},
) {
    val onCatalogItemClicked: (com.maptec.applied.demo.ui.screens.common.ScreenItem) -> Unit =
        { item -> navController.navigate(item.route) }

    composable(CatalogRoutes.MAPS) {
        registerNestedBackHandler(null)
        MapsCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.ANNOTATIONS) {
        registerNestedBackHandler(null)
        OverlaysCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.INTERACTION) {
        registerNestedBackHandler(null)
        InteractionCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.UI_CONTROLS) {
        registerNestedBackHandler(null)
        ControlsCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.GESTURES) {
        registerNestedBackHandler(null)
        GesturesCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.USER_LOCATION) {
        registerNestedBackHandler(null)
        LocationCatalogScreen(onItemClicked = onCatalogItemClicked)
    }
    composable(CatalogRoutes.WEB_SERVICES) {
        registerNestedBackHandler(null)
        WebServicesCatalogScreen(onItemClicked = onCatalogItemClicked)
    }

    composable(CatalogRoutes.ADD_MAP) { registerNestedBackHandler(null); MapRenderScreen() }
    composable(CatalogRoutes.MAP_STYLE) { registerNestedBackHandler(null); MapStyleScreen() }
    composable(CatalogRoutes.CAMERA) { registerNestedBackHandler(null); MapControlScreen() }
    composable(CatalogRoutes.MARKER_BASIC) { registerNestedBackHandler(null); MarkerBasicScreen() }
    composable(CatalogRoutes.MARKER_STYLE) { registerNestedBackHandler(null); MarkerStyleScreen() }
    composable(CatalogRoutes.MARKER_URL) { registerNestedBackHandler(null); MarkerUrlScreen() }
    composable(CatalogRoutes.MARKER_SDF) { registerNestedBackHandler(null); MarkerSdfScreen() }
    composable(CatalogRoutes.MARKER_ANIMATION) { registerNestedBackHandler(null); MarkerAnimationScreen() }
    composable(CatalogRoutes.INFO_WINDOW) { registerNestedBackHandler(null); InfoWindowBasicScreen() }
    composable(CatalogRoutes.INFO_WINDOW_STYLE) { registerNestedBackHandler(null); InfoWindowStyleScreen() }
    composable(CatalogRoutes.INFO_WINDOW_RICH) { registerNestedBackHandler(null); InfoWindowRichScreen() }
    composable(CatalogRoutes.POLYLINE) { registerNestedBackHandler(null); PolylineBasicScreen() }
    composable(CatalogRoutes.POLYLINE_GLOW) { registerNestedBackHandler(null); PolylineGlowScreen() }
    composable(CatalogRoutes.POLYLINE_ARROWS) { registerNestedBackHandler(null); PolylineArrowsScreen() }
    composable(CatalogRoutes.POLYLINE_CAPS) { registerNestedBackHandler(null); PolylineCapsScreen() }
    composable(CatalogRoutes.POLYGON) { registerNestedBackHandler(null); PolygonBasicScreen() }
    composable(CatalogRoutes.POLYGON_PATTERN) { registerNestedBackHandler(null); PolygonPatternScreen() }
    composable(CatalogRoutes.POLYGON_TRANSLATE) { registerNestedBackHandler(null); PolygonTranslateScreen() }
    composable(CatalogRoutes.POLYGON_INTERACTION) { registerNestedBackHandler(null); PolygonInteractionScreen() }
    composable(CatalogRoutes.GEOFENCE_POINT) { registerNestedBackHandler(null); GeofencePointScreen() }
    composable(CatalogRoutes.GEOFENCE_POLYGON) { registerNestedBackHandler(null); GeofencePolygonScreen() }
    composable(CatalogRoutes.ZOOM) { registerNestedBackHandler(null); ZoomScreen() }
    composable(CatalogRoutes.COMPASS) { registerNestedBackHandler(null); CompassScreen() }
    composable(CatalogRoutes.SCALE_BAR) { registerNestedBackHandler(null); ScaleScreen() }
    composable(CatalogRoutes.DAY_NIGHT) { registerNestedBackHandler(null); DayNightScreen() }
    composable(CatalogRoutes.LOGO) { registerNestedBackHandler(null); LogoScreen() }
    composable(CatalogRoutes.LOCATION_LAYER) { registerNestedBackHandler(null); LocationLayerScreen() }
    composable(CatalogRoutes.LOCATION_BUTTON) { registerNestedBackHandler(null); LocationButtonScreen() }
    composable(CatalogRoutes.MAP_EVENTS) { registerNestedBackHandler(null); MapEventListenerScreen() }
    composable(CatalogRoutes.POI_QUERY) { registerNestedBackHandler(null); PoiClickScreen() }
    composable(CatalogRoutes.TEXT_SEARCH) { registerNestedBackHandler(null); TextSearchScreen() }
    composable(CatalogRoutes.NEARBY_SEARCH) { registerNestedBackHandler(null); NearbySearchScreen() }
    composable(CatalogRoutes.SUGGEST) { registerNestedBackHandler(null); SuggestSearchScreen() }
    composable(CatalogRoutes.PLACES) { registerNestedBackHandler(null); PlaceDetailScreen() }
    composable(CatalogRoutes.GEOCODE) { registerNestedBackHandler(null); ForwardGeocodeScreen() }
    composable(CatalogRoutes.REVERSE_GEOCODE) { registerNestedBackHandler(null); ReverseGeocodeScreen() }
    composable(CatalogRoutes.ROUTE) { registerNestedBackHandler(null); RouteOverlayScreen() }

    composable(CatalogRoutes.GESTURE_CONTROL) { registerNestedBackHandler(null); MapGestureScreen() }
    composable(CatalogRoutes.GESTURE_DOUBLE_TAP_DELAY) { registerNestedBackHandler(null); MapDoubleTapDelayScreen() }
    composable(CatalogRoutes.GESTURE_ZOOM_CENTER_MODE) { registerNestedBackHandler(null); MapZoomCenterModeScreen() }
    composable(CatalogRoutes.GESTURE_DOUBLE_TAP_ZOOM_FACTOR) { registerNestedBackHandler(null); MapDoubleTapZoomFactorScreen() }
    composable(CatalogRoutes.GESTURE_THRESHOLD) { registerNestedBackHandler(null); MapGestureThresholdScreen() }
    composable(CatalogRoutes.GESTURE_ROTATE_BEARING_RANGE) { registerNestedBackHandler(null); MapRotateBearingRangeScreen() }
    composable(CatalogRoutes.GESTURE_FLING_DURATION) { registerNestedBackHandler(null); MapFlingDurationScreen() }
    composable(CatalogRoutes.GESTURE_ZOOM_ANIMATION_DURATION) { registerNestedBackHandler(null); MapZoomAnimationDurationScreen() }
    composable(CatalogRoutes.GESTURE_TWO_FINGER_TAP_ZOOM) { registerNestedBackHandler(null); MapTwoFingerTapZoomScreen() }
    composable(CatalogRoutes.GESTURE_DOUBLE_TAP_TILT_RESET) { registerNestedBackHandler(null); MapDoubleTapTiltResetScreen() }
    composable(CatalogRoutes.GESTURE_COORDINATION) { registerNestedBackHandler(null); MapGestureCoordinationScreen() }

    composable(ROUTE_CIRCLE_BASIC) { registerNestedBackHandler(null); CircleBasicScreen() }
    composable(ROUTE_CIRCLE_GEODESIC) { registerNestedBackHandler(null); CircleGeodesicScreen() }
    composable(ROUTE_CIRCLE_DRAGGABLE) { registerNestedBackHandler(null); CircleDraggableScreen() }
    composable(ROUTE_CIRCLE_INNER_SHADOW) { registerNestedBackHandler(null); CircleInnerShadowScreen() }
    composable(ROUTE_CIRCLE_OUTER_GLOW) { registerNestedBackHandler(null); CircleOuterGlowScreen() }
    composable(ROUTE_CIRCLE_SCAN) { registerNestedBackHandler(null); CircleScanScreen() }
    composable(ROUTE_CIRCLE_PULSE) { registerNestedBackHandler(null); CirclePulseScreen() }
    composable(ROUTE_CIRCLE_RADIUS_BREATH) { registerNestedBackHandler(null); CircleRadiusBreathScreen() }
}

@Composable
fun catalogTopBarTitle(route: String?, appTitle: String): String {
    return when (route) {
        "main" -> appTitle
        CatalogRoutes.AUTH -> stringResource(R.string.screen_item_attention)
        CatalogRoutes.MAPS -> stringResource(R.string.catalog_main_maps)
        CatalogRoutes.ANNOTATIONS -> stringResource(R.string.catalog_main_annotations)
        CatalogRoutes.INTERACTION -> stringResource(R.string.catalog_main_interaction)
        CatalogRoutes.UI_CONTROLS -> stringResource(R.string.catalog_ui_controls)
        CatalogRoutes.GESTURES -> stringResource(R.string.map_item_map_gesture)
        CatalogRoutes.USER_LOCATION -> stringResource(R.string.catalog_user_location)
        CatalogRoutes.WEB_SERVICES -> stringResource(R.string.catalog_main_web_services)
        CatalogRoutes.ADD_MAP -> stringResource(R.string.map_item_map_render)
        CatalogRoutes.MAP_STYLE -> stringResource(R.string.map_item_map_style)
        CatalogRoutes.CAMERA -> stringResource(R.string.map_item_map_control)
        CatalogRoutes.MARKER_BASIC -> stringResource(R.string.overlay_item_marker)
        CatalogRoutes.MARKER_STYLE -> stringResource(R.string.overlay_item_marker_style)
        CatalogRoutes.MARKER_URL -> stringResource(R.string.overlay_item_marker_url)
        CatalogRoutes.MARKER_SDF -> stringResource(R.string.overlay_item_marker_sdf)
        CatalogRoutes.MARKER_ANIMATION -> stringResource(R.string.overlay_item_marker_animation)
        CatalogRoutes.INFO_WINDOW -> stringResource(R.string.map_item_markerview)
        CatalogRoutes.INFO_WINDOW_STYLE -> stringResource(R.string.overlay_item_info_window_style)
        CatalogRoutes.INFO_WINDOW_RICH -> stringResource(R.string.overlay_item_info_window_rich)
        CatalogRoutes.POLYLINE -> stringResource(R.string.overlay_item_line)
        CatalogRoutes.POLYLINE_GLOW -> stringResource(R.string.overlay_item_line_glow)
        CatalogRoutes.POLYLINE_ARROWS -> stringResource(R.string.overlay_item_line_arrows)
        CatalogRoutes.POLYLINE_CAPS -> stringResource(R.string.overlay_item_line_caps)
        CatalogRoutes.POLYGON -> stringResource(R.string.overlay_item_polygon_menu)
        CatalogRoutes.POLYGON_PATTERN -> stringResource(R.string.overlay_item_polygon_pattern)
        CatalogRoutes.POLYGON_TRANSLATE -> stringResource(R.string.overlay_item_polygon_translate)
        CatalogRoutes.POLYGON_INTERACTION -> stringResource(R.string.overlay_item_polygon_interaction)
        CatalogRoutes.GEOFENCE_POINT -> stringResource(R.string.overlay_item_geofence_point)
        CatalogRoutes.GEOFENCE_POLYGON -> stringResource(R.string.overlay_item_geofence_polygon)
        CatalogRoutes.ZOOM -> stringResource(R.string.map_item_zoom)
        CatalogRoutes.COMPASS -> stringResource(R.string.map_item_compass)
        CatalogRoutes.SCALE_BAR -> stringResource(R.string.map_item_scale_bar)
        CatalogRoutes.DAY_NIGHT -> stringResource(R.string.map_item_day_night_mode)
        CatalogRoutes.LOGO -> stringResource(R.string.map_item_logo)
        CatalogRoutes.LOCATION_LAYER -> stringResource(R.string.map_item_location_layer)
        CatalogRoutes.LOCATION_BUTTON -> stringResource(R.string.map_item_location_button)
        CatalogRoutes.MAP_EVENTS -> stringResource(R.string.map_item_map_event_listener)
        CatalogRoutes.POI_QUERY -> stringResource(R.string.map_item_poi_click_center)
        CatalogRoutes.TEXT_SEARCH -> stringResource(R.string.catalog_text_search)
        CatalogRoutes.NEARBY_SEARCH -> stringResource(R.string.catalog_nearby_search)
        CatalogRoutes.SUGGEST -> stringResource(R.string.catalog_suggest)
        CatalogRoutes.PLACES -> stringResource(R.string.catalog_places)
        CatalogRoutes.GEOCODE -> stringResource(R.string.catalog_forward_geocode)
        CatalogRoutes.REVERSE_GEOCODE -> stringResource(R.string.catalog_reverse_geocode)
        CatalogRoutes.ROUTE -> stringResource(R.string.screen_item_route)
        CatalogRoutes.GESTURE_CONTROL -> stringResource(R.string.map_item_map_gesture_control)
        CatalogRoutes.GESTURE_COORDINATION -> stringResource(R.string.map_item_gesture_coordination)
        CatalogRoutes.GESTURE_DOUBLE_TAP_DELAY -> stringResource(R.string.map_item_double_tap_delay)
        CatalogRoutes.GESTURE_ZOOM_CENTER_MODE -> stringResource(R.string.map_item_zoom_center_mode)
        CatalogRoutes.GESTURE_DOUBLE_TAP_ZOOM_FACTOR -> stringResource(R.string.map_item_double_tap_zoom_factor)
        CatalogRoutes.GESTURE_THRESHOLD -> stringResource(R.string.map_item_gesture_threshold)
        CatalogRoutes.GESTURE_ROTATE_BEARING_RANGE -> stringResource(R.string.map_item_rotate_bearing_range)
        CatalogRoutes.GESTURE_FLING_DURATION -> stringResource(R.string.map_item_fling_duration)
        CatalogRoutes.GESTURE_ZOOM_ANIMATION_DURATION -> stringResource(R.string.map_item_zoom_animation_duration)
        CatalogRoutes.GESTURE_TWO_FINGER_TAP_ZOOM -> stringResource(R.string.map_item_two_finger_tap_zoom)
        CatalogRoutes.GESTURE_DOUBLE_TAP_TILT_RESET -> stringResource(R.string.map_item_double_tap_tilt_reset)
        ROUTE_CIRCLE_BASIC -> stringResource(R.string.circle_list_basic)
        ROUTE_CIRCLE_GEODESIC -> stringResource(R.string.circle_list_geodesic)
        ROUTE_CIRCLE_DRAGGABLE -> stringResource(R.string.circle_list_draggable)
        ROUTE_CIRCLE_INNER_SHADOW -> stringResource(R.string.circle_list_inner_shadow)
        ROUTE_CIRCLE_OUTER_GLOW -> stringResource(R.string.circle_list_outer_glow)
        ROUTE_CIRCLE_SCAN -> stringResource(R.string.circle_list_scan)
        ROUTE_CIRCLE_PULSE -> stringResource(R.string.circle_list_pulse)
        ROUTE_CIRCLE_RADIUS_BREATH -> stringResource(R.string.circle_list_radius_breath)
        else -> appTitle
    }
}
