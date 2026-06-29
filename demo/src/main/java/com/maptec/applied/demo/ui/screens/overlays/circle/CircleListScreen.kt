package com.maptec.applied.demo.ui.screens.overlays.circle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

const val ROUTE_CIRCLE_BASIC = "circle_basic"
const val ROUTE_CIRCLE_GEODESIC = "circle_geodesic"
const val ROUTE_CIRCLE_DRAGGABLE = "circle_draggable"
const val ROUTE_CIRCLE_INNER_SHADOW = "circle_inner_shadow"
const val ROUTE_CIRCLE_OUTER_GLOW = "circle_outer_glow"
const val ROUTE_CIRCLE_SCAN = "circle_scan"
const val ROUTE_CIRCLE_PULSE = "circle_pulse"
const val ROUTE_CIRCLE_RADIUS_BREATH = "circle_radius_breath"

@Composable
fun CircleListScreen(navController: NavController) {
    val items = listOf(
        ScreenItem(stringResource(R.string.circle_list_basic), ROUTE_CIRCLE_BASIC),
        ScreenItem(stringResource(R.string.circle_list_geodesic), ROUTE_CIRCLE_GEODESIC),
        ScreenItem(stringResource(R.string.circle_list_draggable), ROUTE_CIRCLE_DRAGGABLE),
        ScreenItem(stringResource(R.string.circle_list_inner_shadow), ROUTE_CIRCLE_INNER_SHADOW),
        ScreenItem(stringResource(R.string.circle_list_outer_glow), ROUTE_CIRCLE_OUTER_GLOW),
        ScreenItem(stringResource(R.string.circle_list_scan), ROUTE_CIRCLE_SCAN),
        ScreenItem(stringResource(R.string.circle_list_pulse), ROUTE_CIRCLE_PULSE),
        ScreenItem(stringResource(R.string.circle_list_radius_breath), ROUTE_CIRCLE_RADIUS_BREATH),
    )
    Items(items, { item -> navController.navigate(item.route) })
}
