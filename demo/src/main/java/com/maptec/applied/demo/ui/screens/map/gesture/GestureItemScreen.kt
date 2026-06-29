package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.screens.common.Items
import com.maptec.applied.demo.ui.screens.common.ScreenItem

@Composable
fun GestureItemScreen(
    onItemClicked: (ScreenItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ScreenItem(stringResource(id = R.string.map_item_map_gesture_control), "map_gesture_control"),
        ScreenItem(stringResource(id = R.string.map_item_gesture_coordination), "map_gesture_coordination"),
        ScreenItem(stringResource(id = R.string.map_item_double_tap_delay), "map_double_tap_delay"),
        ScreenItem(stringResource(id = R.string.map_item_zoom_center_mode), "map_zoom_center_mode"),
        ScreenItem(stringResource(id = R.string.map_item_double_tap_zoom_factor), "map_double_tap_zoom_factor"),
        ScreenItem(stringResource(id = R.string.map_item_gesture_threshold), "map_gesture_threshold"),
        ScreenItem(stringResource(id = R.string.map_item_rotate_bearing_range), "map_rotate_bearing_range"),
        ScreenItem(stringResource(id = R.string.map_item_fling_duration), "map_fling_duration"),
        ScreenItem(stringResource(id = R.string.map_item_zoom_animation_duration), "map_zoom_animation_duration"),
        ScreenItem(stringResource(id = R.string.map_item_two_finger_tap_zoom), "map_two_finger_tap_zoom"),
        ScreenItem(stringResource(id = R.string.map_item_double_tap_tilt_reset), "map_double_tap_tilt_reset"),
    )
    Items(items, onItemClicked, modifier)
}
