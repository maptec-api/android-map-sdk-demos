package com.maptec.applied.demo.ui.screens.web_services

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maptec.applied.demo.ui.screens.common.DemoPanelColumn
import com.maptec.applied.demo.ui.screens.common.DemoPanelScaffold
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.camera.CameraPosition
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.demo.map.defaultDemoMapOptions
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.maptec.applied.demo.Constants
import com.maptec.applied.demo.R
import com.maptec.applied.demo.viewmodel.RouteOverlayViewModel
import com.maptec.applied.demo.viewmodel.RouteOverlayViewModelFactory
import com.maptec.applied.maps.MapView
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.Style
import com.maptec.applied.maps.StyleOption
import com.maptec.applied.maps.StyleStatusCallback
import com.maptec.applied.demo.viewmodel.RouteOverlayUIData
import com.maptec.applied.maps.overlay.MapOverlayEngine
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.navigationline.NavigationLine
import com.maptec.applied.maps.overlay.navigationline.NavigationLineOptions
import com.maptec.applied.style.Property
import com.maptec.applied.route.data.*
import com.maptec.applied.utils.BitmapUtils
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import org.maplibre.geojson.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteOverlayScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configPanelController = LocalDemoConfigPanelController.current
    val viewModel: RouteOverlayViewModel = viewModel(
        factory = remember(context) { RouteOverlayViewModelFactory(context.applicationContext) })

    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }

    var start by remember { mutableStateOf("103.8630,1.2820") }
    var destination by remember { mutableStateOf("103.8920,1.3920") }
    var waypointLat by remember { mutableStateOf("1.3500") }
    var waypointLng by remember { mutableStateOf("103.8300") }
    val waypointsList = remember { mutableStateListOf("1.3500" to "103.8300") }
    var waypoints by remember { mutableStateOf("[(1.3500,103.8300)]") }
    var alternatives by remember { mutableStateOf(true) }
    var strategy by remember { mutableStateOf(STRATEGY_FASTEST) }
    var mode by remember { mutableStateOf(MODE_DRIVING) }

    // 更新 waypoints 字符串格式
    fun updateWaypointsString() {
        waypoints = if (waypointsList.isEmpty()) {
            ""
        } else {
            waypointsList.joinToString(prefix = "[", postfix = "]") { (lat, lng) ->
                "($lat,$lng)"
            }
        }
    }

    var avoidHighways by remember { mutableStateOf(false) }
    var avoidCustomArea by remember { mutableStateOf(false) }

    val initialAvoidPolygonJson =
        """{"type":"Polygon","coordinates":[[[103.82,1.34],[103.84,1.34],[103.84,1.36],[103.82,1.36],[103.82,1.34]]]}"""
    var avoidPolygonJson by remember { mutableStateOf(initialAvoidPolygonJson) }

    LaunchedEffect(viewModel.error) {
        viewModel.error.collect { error ->
            error?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val strategies = listOf(
        STRATEGY_FASTEST to stringResource(R.string.route_strategy_fastest),
        STRATEGY_SHORTEST to stringResource(R.string.route_strategy_shortest),
        STRATEGY_BALANCED to stringResource(R.string.route_strategy_balanced),
    )

    val routeLines = remember { mutableStateListOf<NavigationLine>() }

    val lineArrowBitmap = remember(context) {
        ContextCompat.getDrawable(context, R.drawable.line_arrow)?.let {
            BitmapUtils.getBitmapFromDrawable(it)
        }
    }

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(CameraPosition.Builder().target(LatLng(1.35, 103.83)).zoom(13.0).build())
        }
        MapView(context, options).apply mapViewScope@{
            tag = "mapView"
            onCreate(null)
            getMapAsync { map ->
                mapRef = map
                map.setStyle(
                    StyleOption(Constants.DEFAULT_STYLE_ID), object : StyleStatusCallback {
                        override fun onStyleLoaded(style: Style?) {
                        }

                        override fun onStyleRendered(style: Style?) {
                            isStyleRendered = true
                        }

                        override fun onFailed(style: Style?, message: String) {
                        }
                    })
            }
        }
    }
    MapViewLifecycleEffect(mapView)

    DemoPanelScaffold(
        modifier = modifier,
        sheetContent = {
            DemoPanelColumn {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("route_config_sheet"),
                ) {
                    Text(
                        text = stringResource(R.string.route_config_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                    )
                    // 起点单独一行
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = {
                            Text(
                                stringResource(R.string.route_origin),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("origin_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        )
                    )
                    // 终点单独一行
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = {
                            Text(
                                stringResource(R.string.route_destination),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("destination_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    // 策略、出行方式、备选
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        StrategyOverlayDropdown(strategy, strategies) { strategy = it }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.route_alternatives),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = alternatives,
                            onCheckedChange = { alternatives = it },
                            modifier = Modifier
                                .scale(0.75f)
                                .testTag("alternatives_switch")
                        )
                    }

                    // 途经点输入区域
                    Text(
                        text = stringResource(R.string.route_waypoints),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // 输入行：纬度、经度、添加按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = waypointLat,
                            onValueChange = { waypointLat = it },
                            label = {
                                Text(
                                    stringResource(R.string.route_waypoint_lat),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .testTag("waypoint_lat_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = waypointLng,
                            onValueChange = { waypointLng = it },
                            label = {
                                Text(
                                    stringResource(R.string.route_waypoint_lng),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("waypoint_lng_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Button(
                            onClick = {
                                if (waypointLat.isNotBlank() && waypointLng.isNotBlank()) {
                                    waypointsList.add(waypointLat to waypointLng)
                                    updateWaypointsString()
                                    waypointLat = ""
                                    waypointLng = ""
                                }
                            },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .height(56.dp)
                                .testTag("waypoint_add_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.route_waypoint_add_desc)
                            )
                        }
                    }

                    // 直接途经点格式输入（支持直接粘贴 [(lat,lng),...] 格式）
                    OutlinedTextField(
                        value = waypoints,
                        onValueChange = { waypoints = it },
                        label = {
                            Text(
                                stringResource(R.string.route_waypoint_placeholder),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("waypoints_input"),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                        ),
                    )

                    // 显示已添加的途经点列表
                    if (waypointsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("waypoints_list_card"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.3f
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.route_waypoints_count_format,
                                            waypointsList.size
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            waypointsList.clear()
                                            updateWaypointsString()
                                        }, modifier = Modifier.testTag("waypoint_clear_button")
                                    ) {
                                        Text(
                                            stringResource(R.string.route_waypoints_clear),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                waypointsList.forEachIndexed { index, (lat, lng) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ($lat, $lng)",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                waypointsList.removeAt(index)
                                                updateWaypointsString()
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("waypoint_remove_btn_$index")
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.route_waypoint_remove_desc),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp
                    )
                    Text(
                        text = stringResource(R.string.route_avoid_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AvoidOverlayItem(
                        stringResource(R.string.route_avoid_highways),
                        avoidHighways,
                        Modifier
                            .fillMaxWidth()
                            .testTag("avoid_highways_item")
                    ) { avoidHighways = it }
                    AvoidOverlayItem(
                        stringResource(R.string.route_avoid_custom_area),
                        avoidCustomArea,
                        Modifier
                            .fillMaxWidth()
                            .testTag("avoid_custom_item")
                    ) { avoidCustomArea = it }
                    if (avoidCustomArea) {
                        TextField(
                            value = avoidPolygonJson,
                            onValueChange = { avoidPolygonJson = it },
                            label = {
                                Text(
                                    stringResource(R.string.route_custom_area_polygon_label),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("custom_area_input"),
                            maxLines = 1,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (waypoints.isNotEmpty()) {
                                val regex = Regex("\\(([^)]+)\\)")
                                if (waypoints != "[]" && !regex.containsMatchIn(waypoints)) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.route_waypoint_format_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                            }

                            val polygons = if (avoidCustomArea) {
                                try {
                                    Gson().fromJson(avoidPolygonJson, PolygonArea::class.java)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.route_geojson_format_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                            } else null

                            val criteria = mutableListOf<String>().apply {
                                if (avoidHighways) add("highways")
                            }
                            val avoid = if (criteria.isNotEmpty() || avoidCustomArea) {
                                Avoid(criteria, polygons = polygons)
                            } else null

                            viewModel.doSearchRoute(
                                start = start,
                                destination = destination,
                                waypointsStr = waypoints,
                                alternatives = alternatives,
                                strategy = strategy,
                                avoid = avoid,
                                mode = mode
                            )
                            configPanelController.close()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_route_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.route_start_button),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mapView"), factory = { mapView })
                if (isStyleRendered) {
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .testTag("mapRendered")
                    )
                }
                val summary by viewModel.mainRouteSummary.collectAsState()
                summary?.let {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .testTag("route_summary_card"),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format("%.2f km", it.distanceMeters / 1000.0),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        val durationMin = it.durationSeconds / 60
                        Text(
                            text = if (durationMin >= 60) "${durationMin / 60}h ${durationMin % 60}m" else "$durationMin min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        },
    )

    val routes by viewModel.routes.collectAsState()
    LaunchedEffect(routes, destination, avoidCustomArea, avoidPolygonJson, mapRef, isStyleRendered) {
        val map = mapRef ?: return@LaunchedEffect
        if (!isStyleRendered) return@LaunchedEffect
        val engine = map.getOverlayEngine()

        // 清理导航线、Marker 并重绘路线
        engine.deleteAllNavigationLines()
        engine.deleteAllLines()
        engine.deleteAllMarkers()
        routeLines.clear()

        // 1. 绘制路线（NavigationLine：线色 + 同色描边 + linePattern 方向箭头）
        if (routes.isNotEmpty()) {
            for (i in routes.indices.reversed()) {
                val isMain = i == 0
                val routeColor = if (isMain) "#1677FF" else "#8FB3D9"
                val routeWidth =  10.0f
                val outlineColor = "#000000"

                val opts =
                    NavigationLineOptions()
                        .withLatLngs(routes[i].latLngs)
                        .withLineColor(routeColor)
                        .withLineWidth(routeWidth)
                        .withLineJoin(Property.LINE_JOIN_ROUND)
                        .withLineCap(Property.LINE_CAP_ROUND)
                        .withOutlineColor(outlineColor)
                        .withOutlineWidth(1.0f)
                        .withData(JsonPrimitive(i)).let { builder ->
                            if (isMain && lineArrowBitmap != null) {
                                builder.withLinePattern(lineArrowBitmap, "line-arrow")
                            } else {
                                builder
                            }
                        }

                val navLine = engine.addNavigationLine(opts)
                navLine.addOnMapClickListener { ln ->
                    val index = ln.data?.asInt ?: -1
                    if (index > 0) {
                        viewModel.switchRoute(index)
                        true
                    } else false
                }
                routeLines.add(navLine)
            }
            drawRouteEndpointMarkers(
                engine = engine,
                context = context,
                routes = routes,
                destinationStr = destination,
            )
            viewModel.moveCameraToRoute(map, routes.flatMap { it.latLngs })
        }

        // 2. 绘制避让区（仍用普通 Polyline）
        if (avoidCustomArea) {
            try {
                val polygon = Polygon.fromJson(avoidPolygonJson)
                polygon.coordinates().getOrNull(0)?.let { points ->
                    val opts = LineOptions().withLatLngs(points.map {
                            LatLng(
                                it.latitude(),
                                it.longitude()
                            )
                        }).withStrokeColor("#FF0000").withStrokeWeight(2f)
                    engine.addPolyline(opts)
                }
            } catch (e: Exception) {
            }
        }
    }

    if (routeLines.isNotEmpty()) {
        Box(
            Modifier
                .size(1.dp)
                .testTag("route_overlay_has_navigation_lines")
        )
    }
}

private fun parseRouteCoordinate(str: String): LatLng? {
    return try {
        val parts = str.split(",")
        if (parts.size == 2) {
            LatLng(
                latitude = parts[1].trim().toDouble(),
                longitude = parts[0].trim().toDouble(),
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * 与 NaviProject 路线 overlay 对齐：每条路线起终点吸附小圆点 + 用户输入终点大 pin。
 * 小圆点锚点 Center（贴在路上），终点 pin 锚点 Bottom（针尖对准坐标）。
 */
private fun drawRouteEndpointMarkers(
    engine: MapOverlayEngine,
    context: android.content.Context,
    routes: List<RouteOverlayUIData>,
    destinationStr: String,
) {
    for (route in routes) {
        val points = route.latLngs
        if (points.isEmpty()) continue

        engine.addMarker(
            MarkerOptions()
                .withLatLng(points.first())
                .withIconResource(R.drawable.route_matched_start_point_day, context)
                .withIconAnchor(Property.ICON_ANCHOR_CENTER)
                .withClickable(false),
        )
        engine.addMarker(
            MarkerOptions()
                .withLatLng(points.last())
                .withIconResource(R.drawable.route_matched_destination_point_day, context)
                .withIconAnchor(Property.ICON_ANCHOR_CENTER)
                .withClickable(false),
        )
    }

    parseRouteCoordinate(destinationStr)?.let { dest ->
        engine.addMarker(
            MarkerOptions()
                .withLatLng(dest)
                .withIconResource(R.drawable.original_destination_point_day, context)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withClickable(false),
        )
    }
}

@Composable
fun AvoidOverlayItem(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier.toggleable(
            value = checked, onValueChange = onCheckedChange, role = Role.Switch
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked, onCheckedChange = null, modifier = Modifier
                .scale(0.6f)
                .height(24.dp)
        )
    }
}

@Composable
fun StrategyOverlayDropdown(
    currentStrategy: String,
    strategies: List<Pair<String, String>>,
    triggerTestTag: String = "strategy_dropdown_trigger",
    onStrategySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
            .testTag(triggerTestTag)
            .clickable { expanded = !expanded }) {
        OutlinedTextField(
            value = strategies.find { it.first == currentStrategy }?.second ?: "",
            onValueChange = { },
            readOnly = true,
            enabled = false,
            label = {
                Text(
                    stringResource(R.string.route_strategy),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledContainerColor = Color.White,
                disabledBorderColor = Color(0xFFB0B8C4),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .testTag("strategy_dropdown")
        ) {
            strategies.forEach { (s, l) ->
                DropdownMenuItem(
                    text = { Text(l, style = MaterialTheme.typography.bodySmall) },
                    onClick = { onStrategySelected(s); expanded = false },
                    modifier = Modifier.testTag("strategy_item_$s")
                )
            }
        }
    }
}
