package com.maptec.applied.demo.ui.screens.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.viewmodel.PoiClickViewModel
import com.maptec.applied.demo.viewmodel.PoiClickViewModelFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.style.layers.Property
import com.maptec.applied.utils.BitmapUtils

private const val POI_HIGHLIGHT_ICON_SIZE = 2.0f
private const val POI_HIGHLIGHT_FALLBACK_ICON_ID = "poi_highlight_default_pin"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiClickScreen(
    viewModel: PoiClickViewModel = viewModel(factory = PoiClickViewModelFactory())
) {
    val context = LocalContext.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    val poiCenterEnabled by viewModel.poiCenterEnabled.collectAsState()
    val selectedPoiName by viewModel.selectedPoiName.collectAsState()
    val selectedPoiType by viewModel.selectedPoiType.collectAsState()
    val selectedPoiLatLng by viewModel.selectedPoiLatLng.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var highlightMarker by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // 通过 Overlay Marker 放大图标，突出显示命中的 POI
    LaunchedEffect(selectedPoiLatLng, selectedPoiType, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        highlightMarker?.remove()
        highlightMarker = null

        val latLng = selectedPoiLatLng
        val iconId = selectedPoiType?.takeIf { it.isNotBlank() }
        if (latLng == null || iconId == null) return@LaunchedEffect

        map.getStyle { style ->
            val markerOptions = resolvePoiHighlightMarkerOptions(context, style, iconId, latLng)
                ?: return@getStyle
            highlightMarker = map.getOverlayEngine().addMarker(markerOptions)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            highlightMarker?.remove()
            highlightMarker = null
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            PoiClickControlPanel(
                poiCenterEnabled = poiCenterEnabled,
                selectedPoiName = selectedPoiName,
                selectedPoiType = selectedPoiType,
                selectedPoiLatLng = selectedPoiLatLng,
                onPoiCenterEnabledChanged = viewModel::setPoiCenterEnabled,
                onClearSelection = {
                    viewModel.clearSelection()
                    highlightMarker?.remove()
                    highlightMarker = null
                }
            )
        }
    ) { padding ->
        Mapview(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onMapReady = { _, map ->
                mapRef = map
                viewModel.setMap(map)
                map.addOnMapClickListener { point ->
                    viewModel.onMapClick(point)
                }
            }
        )
    }
}

/** 优先使用样式雪碧图中的 poi_type 图标，缺失时回退到 default_pin。 */
private fun resolvePoiHighlightMarkerOptions(
    context: android.content.Context,
    style: Style,
    poiTypeIconId: String,
    latLng: LatLng,
): MarkerOptions? {
    val base = MarkerOptions()
        .withLatLng(latLng)
        .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
        .withIconSize(POI_HIGHLIGHT_ICON_SIZE)
        .withDraggable(false)
        .withClickable(false)
        .withVisible(true)

    if (style.getImage(poiTypeIconId) != null) {
        return base.withStyleIcon(poiTypeIconId)
    }

    val fallback = ContextCompat.getDrawable(context, R.drawable.default_pin)
        ?.let { BitmapUtils.getBitmapFromDrawable(it) }
        ?: return null
    return base.withIcon(POI_HIGHLIGHT_FALLBACK_ICON_ID, fallback)
}

@Composable
private fun PoiClickControlPanel(
    poiCenterEnabled: Boolean,
    selectedPoiName: String?,
    selectedPoiType: String?,
    selectedPoiLatLng: LatLng?,
    onPoiCenterEnabledChanged: (Boolean) -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("点击POI居中")
            Switch(
                checked = poiCenterEnabled,
                onCheckedChange = onPoiCenterEnabledChanged,
                modifier = Modifier.testTag("poi_center_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("点击地图后仅检索样式图层: poi")
        Spacer(modifier = Modifier.height(8.dp))

        Text("当前命中POI名称: ${selectedPoiName ?: "-"}")
        Text("当前命中POI类型: ${selectedPoiType ?: "-"}")
        Text(
            "当前命中POI坐标: ${
                selectedPoiLatLng?.let {
                    "${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}"
                } ?: "-"
            }"
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClearSelection,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("poi_clear_button")
        ) {
            Text("清除高亮")
        }
    }
}
