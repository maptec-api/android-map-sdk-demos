package com.maptec.applied.demo.ui.screens.interaction.gestures

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun GesturesCatalogScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        ScreenItem(stringResource(R.string.map_item_map_gesture_control), CatalogRoutes.GESTURE_CONTROL),
        ScreenItem(stringResource(R.string.map_item_gesture_coordination), CatalogRoutes.GESTURE_COORDINATION),
        ScreenItem(stringResource(R.string.map_item_double_tap_delay), CatalogRoutes.GESTURE_DOUBLE_TAP_DELAY),
        ScreenItem(stringResource(R.string.map_item_zoom_center_mode), CatalogRoutes.GESTURE_ZOOM_CENTER_MODE),
        ScreenItem(stringResource(R.string.map_item_double_tap_zoom_factor), CatalogRoutes.GESTURE_DOUBLE_TAP_ZOOM_FACTOR),
        ScreenItem(stringResource(R.string.map_item_gesture_threshold), CatalogRoutes.GESTURE_THRESHOLD),
        ScreenItem(stringResource(R.string.map_item_rotate_bearing_range), CatalogRoutes.GESTURE_ROTATE_BEARING_RANGE),
        ScreenItem(stringResource(R.string.map_item_fling_duration), CatalogRoutes.GESTURE_FLING_DURATION),
        ScreenItem(stringResource(R.string.map_item_zoom_animation_duration), CatalogRoutes.GESTURE_ZOOM_ANIMATION_DURATION),
        ScreenItem(stringResource(R.string.map_item_two_finger_tap_zoom), CatalogRoutes.GESTURE_TWO_FINGER_TAP_ZOOM),
        ScreenItem(stringResource(R.string.map_item_double_tap_tilt_reset), CatalogRoutes.GESTURE_DOUBLE_TAP_TILT_RESET),
    )
    Items(items, onItemClicked, modifier)
}
