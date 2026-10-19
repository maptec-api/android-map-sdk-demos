package com.maptec.applied.demo.ui.screens.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.ScreenItem
import com.maptec.applied.demo.ui.screens.common.ScreenItemSection
import com.maptec.applied.demo.ui.screens.common.SectionedItems
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_BASIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_DRAGGABLE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_GEODESIC
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_INNER_SHADOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_OUTER_GLOW
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_PULSE
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_RADIUS_BREATH
import com.maptec.applied.demo.ui.screens.overlays.circle.ROUTE_CIRCLE_SCAN

@Composable
fun OverlaysCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionedItems(
        modifier = modifier,
        sections = listOf(
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_marker),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_marker), CatalogRoutes.MARKER_BASIC),
                    ScreenItem(stringResource(R.string.overlay_item_marker_style), CatalogRoutes.MARKER_STYLE),
                    ScreenItem(stringResource(R.string.overlay_item_marker_url), CatalogRoutes.MARKER_URL),
                    ScreenItem(stringResource(R.string.overlay_item_marker_sdf), CatalogRoutes.MARKER_SDF),
                    ScreenItem(stringResource(R.string.overlay_item_marker_animation), CatalogRoutes.MARKER_ANIMATION),
                    ScreenItem(stringResource(R.string.overlay_item_marker_move_along), CatalogRoutes.MARKER_MOVE_ALONG),
                    ScreenItem(stringResource(R.string.overlay_item_marker_collision), CatalogRoutes.MARKER_COLLISION),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_info_window),
                items = listOf(
                    ScreenItem(stringResource(R.string.map_item_markerview), CatalogRoutes.INFO_WINDOW),
                    ScreenItem(stringResource(R.string.overlay_item_info_window_style), CatalogRoutes.INFO_WINDOW_STYLE),
                    ScreenItem(stringResource(R.string.overlay_item_info_window_rich), CatalogRoutes.INFO_WINDOW_RICH),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_polyline),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_line), CatalogRoutes.POLYLINE),
                    ScreenItem(stringResource(R.string.overlay_item_line_glow), CatalogRoutes.POLYLINE_GLOW),
                    ScreenItem(stringResource(R.string.overlay_item_line_arrows), CatalogRoutes.POLYLINE_ARROWS),
                    ScreenItem(stringResource(R.string.overlay_item_line_caps), CatalogRoutes.POLYLINE_CAPS),
                    ScreenItem(stringResource(R.string.overlay_item_line_gradient), CatalogRoutes.POLYLINE_GRADIENT),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_polygon),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_polygon_menu), CatalogRoutes.POLYGON),
                    ScreenItem(stringResource(R.string.overlay_item_polygon_pattern), CatalogRoutes.POLYGON_PATTERN),
                    ScreenItem(stringResource(R.string.overlay_item_polygon_interaction), CatalogRoutes.POLYGON_INTERACTION),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_circle),
                items = listOf(
                    ScreenItem(stringResource(R.string.circle_list_basic), ROUTE_CIRCLE_BASIC),
                    ScreenItem(stringResource(R.string.circle_list_geodesic), ROUTE_CIRCLE_GEODESIC),
                    ScreenItem(stringResource(R.string.circle_list_draggable), ROUTE_CIRCLE_DRAGGABLE),
                    ScreenItem(stringResource(R.string.circle_list_inner_shadow), ROUTE_CIRCLE_INNER_SHADOW),
                    ScreenItem(stringResource(R.string.circle_list_outer_glow), ROUTE_CIRCLE_OUTER_GLOW),
                    ScreenItem(stringResource(R.string.circle_list_scan), ROUTE_CIRCLE_SCAN),
                    ScreenItem(stringResource(R.string.circle_list_pulse), ROUTE_CIRCLE_PULSE),
                    ScreenItem(stringResource(R.string.circle_list_radius_breath), ROUTE_CIRCLE_RADIUS_BREATH),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_geofence),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_geofence_point), CatalogRoutes.GEOFENCE_POINT),
                    ScreenItem(stringResource(R.string.overlay_item_geofence_polygon), CatalogRoutes.GEOFENCE_POLYGON),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_heatmap),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_heatmap_basic), CatalogRoutes.HEATMAP_BASIC),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_point_cluster),
                items = listOf(
                    ScreenItem(
                        stringResource(R.string.overlay_item_point_cluster_basic),
                        CatalogRoutes.POINT_CLUSTER_BASIC,
                    ),
                ),
            ),
            ScreenItemSection(
                title = stringResource(R.string.catalog_section_model),
                items = listOf(
                    ScreenItem(stringResource(R.string.overlay_item_model_basic), CatalogRoutes.MODEL_BASIC),
                ),
            ),
        ),
        onItemClicked = onItemClicked,
    )
}
