package com.maptec.applied.demo.ui.screens.interaction.gestures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.maps.MapGestureType
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

@Composable
fun MapGestureCoordinationScreen(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition.Builder().target(Constants.DEFAULT_MAP_CENTER)
        .zoom(Constants.DEFAULT_ZOOM_LEVEL).build(),
) {
    var isStyleRendered by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(cameraPosition)
        }

        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}
                    override fun onStyleRendered(style: Style?) { isStyleRendered = true }
                    override fun onFailed(style: Style?, message: String) {}
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                GestureCoordinationPanel(
                    mapView = mapView,
                    isStyleRendered = isStyleRendered,
                )
            }
        },
        content = {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mapView"),
                factory = { mapView.apply { tag = "mapView" } },
                update = {},
            )
        },
    )
}

private val customizableGestureChoices: List<Pair<MapGestureType, Int>> = listOf(
    MapGestureType.SCALE to R.string.gesture_type_scale,
    MapGestureType.ROTATE to R.string.gesture_type_rotate,
    MapGestureType.SHOVE to R.string.gesture_type_shove,
    MapGestureType.SIDEWAYS_SHOVE to R.string.gesture_type_sideways_shove,
    MapGestureType.MOVE to R.string.gesture_type_move,
    MapGestureType.LONG_PRESS to R.string.gesture_type_long_press,
    MapGestureType.MULTI_FINGER_TAP to R.string.gesture_type_multi_finger_tap,
)

@Composable
private fun GestureCoordinationPanel(
    mapView: MapView,
    isStyleRendered: Boolean,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    var simultaneousAllowed by remember { mutableStateOf(true) }
    var userChangedSimultaneous by remember { mutableStateOf(false) }
    var appliedMutualSnapshot by remember { mutableStateOf<Set<MapGestureType>?>(null) }
    var draftCustomTypes by remember { mutableStateOf<Set<MapGestureType>>(emptySet()) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            if (!userChangedSimultaneous) {
                simultaneousAllowed = map.uiSettings.gestures.isSimultaneousGesturesAllowed
            }
        }
    }

    LaunchedEffect(simultaneousAllowed, appliedMutualSnapshot) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            gestures.setSimultaneousGesturesAllowed(simultaneousAllowed)
            appliedMutualSnapshot?.let { snapshot ->
                if (simultaneousAllowed) {
                    gestures.setMutuallyExclusiveGestures(
                        if (snapshot.isEmpty()) emptyList() else listOf(HashSet(snapshot)),
                    )
                }
            }
        }
    }

    Text(
        text = stringResource(id = R.string.gesture_coordination_section_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.gesture_coordination_allow_simultaneous),
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = simultaneousAllowed,
            onCheckedChange = {
                userChangedSimultaneous = true
                simultaneousAllowed = it
            },
            modifier = Modifier.testTag("switch_simultaneous_gestures"),
        )
    }
    Text(
        text = stringResource(id = R.string.gesture_coordination_simultaneous_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    Text(
        text = stringResource(id = R.string.gesture_coordination_custom_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Text(
        text = stringResource(id = R.string.gesture_coordination_custom_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    customizableGestureChoices.forEach { (type, labelRes) ->
        val checked = draftCustomTypes.contains(type)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { on ->
                    draftCustomTypes =
                        if (on) draftCustomTypes + type else draftCustomTypes - type
                },
                enabled = simultaneousAllowed,
                modifier = Modifier.testTag("checkbox_gesture_${type.typeId}"),
            )
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    DemoPanelButton(
        onClick = {
            appliedMutualSnapshot = HashSet(draftCustomTypes)
            Toast.makeText(
                context,
                context.getString(R.string.gesture_coordination_applied),
                Toast.LENGTH_SHORT,
            ).show()
            configPanelController.close()
        },
        enabled = simultaneousAllowed,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("button_confirm_custom_mutual"),
    ) {
        Text(text = stringResource(id = R.string.gesture_coordination_confirm))
    }

    if (isStyleRendered) {
        Box(modifier = Modifier.testTag("mapRendered"))
    }
}
