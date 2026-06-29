package com.maptec.applied.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.rememberNestedNavController
import com.maptec.applied.demo.ui.screens.common.ScreenItem
import com.maptec.applied.demo.ui.screens.map.CompassScreen
import com.maptec.applied.demo.ui.screens.map.LocationScreen
import com.maptec.applied.demo.ui.screens.map.LogoScreen
import com.maptec.applied.demo.ui.screens.map.ScaleScreen
import com.maptec.applied.demo.ui.screens.map.MapControlScreen
import com.maptec.applied.demo.ui.screens.map.ZoomScreen
import com.maptec.applied.demo.ui.screens.map.MapEventListenerScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapGestureScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapDoubleTapDelayScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapDoubleTapTiltResetScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapTwoFingerTapZoomScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapZoomCenterModeScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapGestureThresholdScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapFlingDurationScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapGestureCoordinationScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapDoubleTapZoomFactorScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapRotateBearingRangeScreen
import com.maptec.applied.demo.ui.screens.map.gesture.MapZoomAnimationDurationScreen
import com.maptec.applied.demo.ui.screens.map.MapScreen
import com.maptec.applied.demo.ui.screens.map.MapStyleScreen
import com.maptec.applied.demo.ui.screens.map.DayNightScreen
import com.maptec.applied.demo.ui.screens.map.PoiClickScreen
import com.maptec.applied.demo.ui.screens.map.gesture.GestureItemScreen

@Composable
fun MapItemScreen(
    modifier: Modifier = Modifier,
    registerBackHandler: ((() -> Boolean)?) -> Unit = {}
) {
    val navController = rememberNestedNavController(registerBackHandler)
    NavHost(
        navController = navController as NavHostController,
        startDestination = "map",
        modifier = modifier
    ) {
        composable("map") {
            val items = listOf(
                ScreenItem(stringResource(id = R.string.map_item_map_render), "map_render"),
                ScreenItem(stringResource(id = R.string.map_item_map_control), "map_control"),
                ScreenItem(stringResource(id = R.string.map_item_map_gesture), "map_gestures"),
                ScreenItem(stringResource(id = R.string.map_item_location), "location"),
                ScreenItem(stringResource(id = R.string.map_item_compass), "compass"),
                ScreenItem(stringResource(id = R.string.map_item_logo), "logo"),
                ScreenItem(stringResource(id = R.string.map_item_zoom), "zoom"),
                ScreenItem(stringResource(id = R.string.map_item_map_event_listener), "map_event_listener"),
                ScreenItem(stringResource(id = R.string.map_item_map_style), "map_style"),
                ScreenItem(stringResource(id = R.string.map_item_day_night_mode), "day_night_mode"),
                ScreenItem(stringResource(id = R.string.map_item_poi_click_center), "poi_click_center"),
                ScreenItem(stringResource(id = R.string.map_item_scale_bar), "scale_bar"),
            )
            Items(items, { item ->
                navController.navigate(item.route)
            })
        }
        composable("map_render") {
            MapScreen()
        }
        composable("map_control") {
            MapControlScreen()
        }
        composable("map_gestures") {
            GestureItemScreen(onItemClicked = { item ->
                navController.navigate(item.route)
            })
        }
        composable("map_gesture_control") {
            MapGestureScreen()
        }
        composable("map_double_tap_delay") {
            MapDoubleTapDelayScreen()
        }
        composable("map_zoom_center_mode") {
            MapZoomCenterModeScreen()
        }
        composable("map_double_tap_zoom_factor") {
            MapDoubleTapZoomFactorScreen()
        }
        composable("map_gesture_threshold") {
            MapGestureThresholdScreen()
        }
        composable("map_rotate_bearing_range") {
            MapRotateBearingRangeScreen()
        }
        composable("map_fling_duration") {
            MapFlingDurationScreen()
        }
        composable("map_zoom_animation_duration") {
            MapZoomAnimationDurationScreen()
        }
        composable("map_two_finger_tap_zoom") {
            MapTwoFingerTapZoomScreen()
        }
        composable("map_double_tap_tilt_reset") {
            MapDoubleTapTiltResetScreen()
        }
        composable("map_gesture_coordination") {
            MapGestureCoordinationScreen()
        }
        composable("location") {
            LocationScreen()
        }
        composable("compass") {
            CompassScreen()
        }
        composable("logo") {
            LogoScreen()
        }
        composable("scale_bar") {
            ScaleScreen()
        }
        composable("zoom") {
            ZoomScreen()
        }
        composable("map_event_listener") {
            MapEventListenerScreen()
        }
        composable("map_style") {
            MapStyleScreen()
        }
        composable("day_night_mode") {
            DayNightScreen()
        }
        composable("poi_click_center") {
            PoiClickScreen()
        }
    }
}
