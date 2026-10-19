package com.maptec.applied.demo.ui.screens.maps

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
import com.maptec.applied.demo.map.showMapStyleLoadError
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapFeatureCategory
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback

private val featureCategoryChoices: List<Pair<MapFeatureCategory, Int>> = listOf(
    MapFeatureCategory.POI to R.string.map_feature_category_poi,
    MapFeatureCategory.ROAD to R.string.map_feature_category_road,
    MapFeatureCategory.BUILDING to R.string.map_feature_category_building,
    MapFeatureCategory.ADMINISTRATIVE to R.string.map_feature_category_administrative,
    MapFeatureCategory.NATURAL_FEATURES to R.string.map_feature_category_natural,
)

@Composable
fun MapFeatureVisibilityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var maptecMap by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }
    var visibleCategories by remember {
        mutableStateOf(MapFeatureCategory.entries.toSet())
    }

    val cameraPosition = remember {
        CameraPosition.Builder()
            .target(LatLng(1.360879, 103.732578))
            .zoom(16.0)
            .tilt(40.0)
            .build()
    }

    val mapView = remember(cameraPosition) {
        val options = defaultDemoMapOptions(context).apply {
            zoomButtonsEnabled(false)
            camera(cameraPosition)
        }
        MapView(context, options).apply {
            onCreate(null)
            getMapAsync { map ->
                maptecMap = map
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                    override fun onStyleLoaded(style: Style?) {}

                    override fun onStyleRendered(style: Style?) {
                        isStyleRendered = true
                    }

                    override fun onFailed(style: Style?, message: String) {
                        showMapStyleLoadError(context, message)
                    }
                })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    LaunchedEffect(maptecMap, isStyleRendered, visibleCategories) {
        val map = maptecMap ?: return@LaunchedEffect
        if (!isStyleRendered) return@LaunchedEffect
        MapFeatureCategory.entries.forEach { category ->
            map.setMapFeatureVisibility(category, visibleCategories.contains(category))
        }
    }

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                Text(
                    text = stringResource(R.string.map_feature_visibility_panel_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.map_feature_visibility_panel_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                featureCategoryChoices.forEach { (category, labelRes) ->
                    val checked = visibleCategories.contains(category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                visibleCategories = if (on) {
                                    visibleCategories + category
                                } else {
                                    visibleCategories - category
                                }
                            },
                            enabled = isStyleRendered,
                            modifier = Modifier.testTag(featureCategoryTestTag(category)),
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mapView"),
                    factory = { mapView.apply { tag = "mapView" } },
                    update = {},
                )
                if (isStyleRendered) {
                    Box(modifier = Modifier.testTag("mapRendered"))
                }
            }
        },
    )
}

internal fun featureCategoryTestTag(category: MapFeatureCategory): String =
    "checkbox_feature_${category.name.lowercase()}"
