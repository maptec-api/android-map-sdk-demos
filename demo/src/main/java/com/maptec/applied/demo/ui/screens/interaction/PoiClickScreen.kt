package com.maptec.applied.demo.ui.screens.interaction

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.R
import com.maptec.applied.demo.map.Mapview
import com.maptec.applied.demo.ui.screens.common.DemoPanelButton
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.DemoPanelSwitch
import com.maptec.applied.demo.viewmodel.PoiClickViewModel
import com.maptec.applied.demo.viewmodel.PoiClickViewModelFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.style.Property
import com.maptec.applied.utils.BitmapUtils

private const val POI_HIGHLIGHT_ICON_SIZE = 2.0f
private const val POI_HIGHLIGHT_FALLBACK_ICON_ID = "poi_highlight_default_pin"

@Composable
fun PoiClickScreen(
    viewModel: PoiClickViewModel = viewModel(factory = PoiClickViewModelFactory()),
) {
    val context = LocalContext.current
    val poiCenterEnabled by viewModel.poiCenterEnabled.collectAsState()
    val selectedPoiName by viewModel.selectedPoiName.collectAsState()
    val selectedPoiType by viewModel.selectedPoiType.collectAsState()
    val selectedPoiLatLng by viewModel.selectedPoiLatLng.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var highlightMarker by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    LaunchedEffect(selectedPoiLatLng, selectedPoiType, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        highlightMarker?.let { it.remove() }
        highlightMarker = null

        val latLng = selectedPoiLatLng
        if (latLng == null) return@LaunchedEffect

        map.getStyle { style ->
            val iconId = selectedPoiType?.takeIf { it.isNotBlank() }
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

    DemoPanelScaffold(
        sheetContent = {
            DemoPanelColumn {
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
                    },
                )
            }
        },
        content = {
            Mapview(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { _, map ->
                    mapRef = map
                    viewModel.setMap(map)
                    map.addOnMapClickListener { point ->
                        viewModel.onMapClick(point)
                    }
                },
            )
        },
    )
}

private fun resolvePoiHighlightMarkerOptions(
    context: android.content.Context,
    style: Style,
    poiTypeIconId: String?,
    latLng: LatLng,
): MarkerOptions? {
    val base = MarkerOptions()
        .withLatLng(latLng)
        .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
        .withIconSize(POI_HIGHLIGHT_ICON_SIZE)
        .withDraggable(false)
        .withClickable(false)
        .withVisible(true)

    if (!poiTypeIconId.isNullOrBlank() && style.getImage(poiTypeIconId) != null) {
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
    onClearSelection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.poi_center_on_click),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        DemoPanelSwitch(
            checked = poiCenterEnabled,
            onCheckedChange = onPoiCenterEnabledChanged,
            modifier = Modifier.testTag("poi_center_switch"),
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.poi_query_layer_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(16.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.poi_hit_name, selectedPoiName ?: "-"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("poi_hit_name"),
        )
        Text(
            text = stringResource(R.string.poi_hit_type, selectedPoiType ?: "-"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("poi_hit_type"),
        )
        Text(
            text = stringResource(
                R.string.poi_hit_coordinates,
                selectedPoiLatLng?.let {
                    "${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}"
                } ?: "-",
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("poi_hit_coordinates"),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    DemoPanelButton(
        onClick = onClearSelection,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("poi_clear_button"),
    ) {
        Text(stringResource(R.string.poi_clear_highlight))
    }
}
