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
import com.maptec.applied.demo.ui.screens.overlays.GeofenceMarkerScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleBasicScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleDraggableScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleGeodesicScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleInnerShadowScreen
import com.maptec.applied.demo.ui.screens.overlays.circle.CircleListScreen
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
import com.maptec.applied.demo.ui.screens.overlays.polygon.FillScreen
import com.maptec.applied.demo.ui.screens.overlays.LineScreen
import com.maptec.applied.demo.ui.screens.overlays.MarkerAnimationScreen
import com.maptec.applied.demo.ui.screens.overlays.MarkerLayerScreen
import com.maptec.applied.demo.ui.screens.overlays.MarkerViewScreen
import com.maptec.applied.demo.ui.screens.overlays.OverlayPickScreen

@Composable
fun OverlayItemScreen(
    modifier: Modifier = Modifier,
    registerBackHandler: ((() -> Boolean)?) -> Unit = {}
) {
    val navController = rememberNestedNavController(registerBackHandler)
    NavHost(
        navController = navController as NavHostController,
        startDestination = "overlays",
        modifier = modifier
    ) {
        composable("overlays") {
            val items = listOf(
                ScreenItem(stringResource(id = R.string.overlay_item_marker), "marker"),
                ScreenItem(stringResource(id = R.string.overlay_item_marker_animation), "marker_animation"),
                ScreenItem(stringResource(id = R.string.overlay_item_line), "line"),
                ScreenItem(stringResource(id = R.string.overlay_item_polygon_menu), "polygon_full"),
                ScreenItem(stringResource(id = R.string.overlay_item_circle), "circle"),
                ScreenItem(stringResource(id = R.string.map_item_markerview), "markerview"),
                ScreenItem(stringResource(id = R.string.overlay_item_geofence), "geofence"),
                ScreenItem(stringResource(id = R.string.overlay_item_pick), "overlay_pick"),
            )
            Items(items, { item ->
                navController.navigate(item.route)
            })
        }
        composable("marker") {
            MarkerLayerScreen()
        }
        composable("marker_animation") {
            MarkerAnimationScreen()
        }
        composable("line") {
            LineScreen()
        }
        composable("polygon_menu") {
            val items = listOf(
                ScreenItem(stringResource(id = R.string.overlay_item_polygon_full_example), "polygon_full"),
            )
            Items(items, { item ->
                navController.navigate(item.route)
            })
        }
        composable("polygon_full") {
            FillScreen()
        }
        composable("circle") {
            CircleListScreen(navController as NavHostController)
        }
        composable(ROUTE_CIRCLE_BASIC) { CircleBasicScreen() }
        composable(ROUTE_CIRCLE_GEODESIC) { CircleGeodesicScreen() }
        composable(ROUTE_CIRCLE_DRAGGABLE) { CircleDraggableScreen() }
        composable(ROUTE_CIRCLE_INNER_SHADOW) { CircleInnerShadowScreen() }
        composable(ROUTE_CIRCLE_OUTER_GLOW) { CircleOuterGlowScreen() }
        composable(ROUTE_CIRCLE_SCAN) { CircleScanScreen() }
        composable(ROUTE_CIRCLE_PULSE) { CirclePulseScreen() }
        composable(ROUTE_CIRCLE_RADIUS_BREATH) { CircleRadiusBreathScreen() }
        composable("markerview") {
            MarkerViewScreen()
        }
        composable("geofence") {
            GeofenceMarkerScreen()
        }
        composable("overlay_pick") {
            OverlayPickScreen()
        }
    }
}
