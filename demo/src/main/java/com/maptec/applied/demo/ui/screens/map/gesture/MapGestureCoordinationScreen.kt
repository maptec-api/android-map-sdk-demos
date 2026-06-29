package com.maptec.applied.demo.ui.screens.map.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.maps.MapGestureType
import com.maptec.applied.demo.map.defaultDemoMapOptions
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.map.MapViewLifecycleEffect

@OptIn(ExperimentalMaterial3Api::class)
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
                    override fun onStyleLoaded(style: Style?) {
                    }

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(
                        style: Style?, message: String
                    ) {
                    }
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            GestureCoordinationPanel(
                mapView = mapView,
                isStyleRendered = isStyleRendered,
                sheetState = scaffoldState.bottomSheetState,
            )
        }) { _ ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mapView"),
            factory = { mapView.apply { tag = "mapView" } },
            update = { })
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureCoordinationPanel(
    mapView: MapView,
    isStyleRendered: Boolean,
    sheetState: SheetState,
) {
    var simultaneousAllowed by remember { mutableStateOf(true) }

    /** null: 尚未点确定，不覆盖地图默认互斥配置 */
    var appliedMutualSnapshot by remember { mutableStateOf<Set<MapGestureType>?>(null) }
    var draftCustomTypes by remember { mutableStateOf<Set<MapGestureType>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            simultaneousAllowed = map.uiSettings.gestures.isSimultaneousGesturesAllowed
        }
    }

    LaunchedEffect(simultaneousAllowed, appliedMutualSnapshot) {
        mapView.getMapAsync { map ->
            val gestures = map.uiSettings.gestures
            gestures.setSimultaneousGesturesAllowed(simultaneousAllowed)
            appliedMutualSnapshot?.let { snapshot ->
                if (simultaneousAllowed) {
                    gestures.setMutuallyExclusiveGestures(
                        if (snapshot.isEmpty()) emptyList() else listOf(HashSet(snapshot))
                    )
                }
            }
        }
    }

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.gesture_coordination_section_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.gesture_coordination_allow_simultaneous),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = simultaneousAllowed,
                onCheckedChange = { simultaneousAllowed = it },
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

        Button(
            onClick = {
                appliedMutualSnapshot = HashSet(draftCustomTypes)
                scope.launch { sheetState.partialExpand() }
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
}
