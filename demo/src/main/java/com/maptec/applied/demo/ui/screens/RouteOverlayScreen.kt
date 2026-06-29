package com.maptec.applied.demo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.maptec.applied.maps.overlay.line.LineOptions
import com.maptec.applied.maps.overlay.navigationline.NavigationLine
import com.maptec.applied.maps.overlay.navigationline.NavigationLineOptions
import com.maptec.applied.route.data.*
import com.maptec.applied.style.layers.Property
import com.maptec.applied.utils.BitmapUtils
import com.maptec.applied.demo.map.MapViewLifecycleEffect
import org.maplibre.geojson.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteOverlayScreen(
    modifier: Modifier = Modifier,
    viewModel: RouteOverlayViewModel = viewModel(
        factory = RouteOverlayViewModelFactory(LocalContext.current.applicationContext)
    )
) {

    val context = LocalContext.current
    val viewModel: RouteOverlayViewModel = viewModel(
        factory = remember(context) { RouteOverlayViewModelFactory(context.applicationContext) }
    )

    var mapRef by remember { mutableStateOf<MaptecMap?>(null) }
    var isStyleRendered by remember { mutableStateOf(false) }
    
    var start by remember { mutableStateOf("103.8630,1.2820") }
    var destination by remember { mutableStateOf("103.8920,1.3920") }
    // 途经点相关状态
    var waypointLat by remember { mutableStateOf("1.3500") }
    var waypointLng by remember { mutableStateOf("103.8300") }
    val waypointsList = remember { mutableStateListOf<Pair<String, String>>() }
    var waypoints by remember { mutableStateOf("") }
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

    var avoidTolls by remember { mutableStateOf(false) }
    var avoidHighways by remember { mutableStateOf(false) }
    var avoidFerries by remember { mutableStateOf(false) }
    var avoidCustomArea by remember { mutableStateOf(false) }

    val initialAvoidPolygonJson = """{"type":"Polygon","coordinates":[[[103.82,1.34],[103.84,1.34],[103.84,1.36],[103.82,1.36],[103.82,1.34]]]}"""
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
        STRATEGY_BALANCED to stringResource(R.string.route_strategy_balanced)
    )

    val modes = listOf(
        MODE_DRIVING to stringResource(R.string.route_mode_driving)
    )

    val routeLines = remember { mutableStateListOf<NavigationLine>() }

    val mapView = remember {
        val options = defaultDemoMapOptions(context).apply {
            camera(CameraPosition.Builder().target(LatLng(1.35, 103.83)).zoom(13.0).build())
        }
        MapView(context, options).apply mapViewScope@{
            tag = "mapView"
            onCreate(null)
            getMapAsync { map ->
                mapRef = map
                map.setStyle(StyleOption(Constants.DEFAULT_STYLE_ID),
                    object : StyleStatusCallback {
                        override fun onStyleLoaded(style: Style?) {
                        }

                        override fun onStyleRendered(style: Style?) {
                            isStyleRendered = true
                            if (style == null) return
                            
                            // 注册导航线方向箭头雪碧图（NavigationLine linePattern）
                            ContextCompat.getDrawable(context, R.drawable.line_arrow)?.let {
                                BitmapUtils.getBitmapFromDrawable(it)?.let { bmp ->
                                    mapRef?.getOverlayEngine()?.addImage("line-arrow", bmp, false)
                                }
                            }
                        }

                        override fun onFailed(style: Style?, message: String) {
                        }
                    }
                )
            }
        }
    }
    MapViewLifecycleEffect(mapView)


    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetMaxWidth = Dp.Unspecified,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).verticalScroll(rememberScrollState()).testTag("route_config_sheet")
            ) {
                Text(text = stringResource(R.string.route_config_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp))
                // 起点单独一行
                TextField(value = start, onValueChange = { start = it }, label = { Text(stringResource(R.string.route_origin), style = MaterialTheme.typography.labelSmall) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("origin_input"), textStyle = MaterialTheme.typography.bodySmall, keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
                )
                // 终点单独一行
                TextField(value = destination, onValueChange = { destination = it }, label = { Text(stringResource(R.string.route_destination), style = MaterialTheme.typography.labelSmall) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("destination_input"), textStyle = MaterialTheme.typography.bodySmall, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                // 策略和备选同一行
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).padding(end = 4.dp)) { StrategyOverlayDropdown(strategy, strategies) { strategy = it } }
                    Column(modifier = Modifier.weight(0.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.route_alternatives), style = MaterialTheme.typography.labelSmall)
                        Switch(checked = alternatives, onCheckedChange = { alternatives = it }, modifier = Modifier.scale(0.6f).testTag("alternatives_switch"))
                    }
                }
                
                // 途经点输入区域
                Text(text = stringResource(R.string.route_waypoints), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                // 输入行：纬度、经度、添加按钮
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = waypointLat,
                        onValueChange = { waypointLat = it },
                        label = { Text("纬度", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = waypointLng,
                        onValueChange = { waypointLng = it },
                        label = { Text("经度", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
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
                        modifier = Modifier.padding(start = 4.dp).height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加途经点")
                    }
                }

                // 显示已添加的途经点
                if (waypointsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已添加 ${waypointsList.size} 个途经点",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        waypointsList.clear()
                                        updateWaypointsString()
                                    }
                                ) {
                                    Text("清空", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            waypointsList.forEachIndexed { index, (lat, lng) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "删除", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "格式: $waypoints",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                Text(text = stringResource(R.string.route_avoid_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
//                    AvoidOverlayItem(stringResource(R.string.route_avoid_tolls), avoidTolls, Modifier.weight(1f).testTag("avoid_tolls_item")) { avoidTolls = it }
                    AvoidOverlayItem(stringResource(R.string.route_avoid_highways), avoidHighways, Modifier.weight(1f).testTag("avoid_highways_item")) { avoidHighways = it }
//                    AvoidOverlayItem(stringResource(R.string.route_avoid_ferries), avoidFerries, Modifier.weight(1f).testTag("avoid_ferries_item")) { avoidFerries = it }
                    AvoidOverlayItem(stringResource(R.string.route_avoid_custom_area), avoidCustomArea, Modifier.weight(1f).testTag("avoid_custom_item")) { avoidCustomArea = it }
                }
                if (avoidCustomArea) {
                    TextField(value = avoidPolygonJson, onValueChange = { avoidPolygonJson = it }, label = { Text("GeoJSON Polygon", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("custom_area_input"), maxLines = 1, singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.route_travel_mode), style = MaterialTheme.typography.bodyMedium)
                    Box(modifier = Modifier.width(120.dp)) {
                        StrategyOverlayDropdown(mode, modes, triggerTestTag = "mode_dropdown_trigger") { mode = it }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {

                        val polygons = if (avoidCustomArea) {
                            try {
                                Gson().fromJson(avoidPolygonJson, PolygonArea::class.java)
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.route_geojson_format_error), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        } else null

                        val criteria = mutableListOf<String>().apply { if (avoidTolls) add("tolls"); if (avoidHighways) add("highways"); if (avoidFerries) add("ferries") }
                        val avoid = if (criteria.isNotEmpty() || avoidCustomArea) { Avoid(criteria, polygons = polygons) } else null

                        viewModel.doSearchRoute(
                            start = start,
                            destination = destination,
                            waypointsStr = waypoints,
                            alternatives = alternatives,
                            strategy = strategy,
                            avoid = avoid,
                            mode = mode
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("start_route_button"), shape = RoundedCornerShape(12.dp)
                ) { Text(text = stringResource(R.string.route_start_button), style = MaterialTheme.typography.titleMedium) }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(modifier = Modifier.fillMaxSize().testTag("mapView"), factory = { mapView })
            if (isStyleRendered) {
                Box(modifier = Modifier.size(0.dp).testTag("mapRendered"))
            }
            val summary by viewModel.mainRouteSummary.collectAsState()
            summary?.let {
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(8.dp).testTag("route_summary_card"), horizontalAlignment = Alignment.End) {
                    Text(text = String.format("%.2f km", it.distanceMeters / 1000.0), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    val durationMin = it.durationSeconds / 60
                    Text(text = if (durationMin >= 60) "${durationMin / 60}h ${durationMin % 60}m" else "$durationMin min", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }
        }
    }

    val routes by viewModel.routes.collectAsState()
    LaunchedEffect(routes, avoidCustomArea, avoidPolygonJson, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        val engine = map.getOverlayEngine()

        // 清理导航线并重绘路线
        engine.deleteAllNavigationLines()
        engine.deleteAllLines()
        routeLines.clear()

        // 1. 绘制路线（NavigationLine：线色 + 同色描边 + linePattern 方向箭头）
        if (routes.isNotEmpty()) {
            for (i in routes.indices.reversed()) {
                val isMain = i == 0
                val routeColor = if (isMain) "#00A63E" else "#A5D6A7"

                val opts = NavigationLineOptions()
                    .withLatLngs(routes[i].latLngs)
                    .withLineColor(routeColor)
                    .withLineWidth(10.0f)
                    .withLineJoin(Property.LINE_JOIN_ROUND)
                    .withLineCap(Property.LINE_CAP_ROUND)
                    .withOutlineColor("#000000")
                    .withOutlineWidth(1.0f)
                    .withData(JsonPrimitive(i))
                    .let { builder ->
                        if (isMain) builder.withLinePattern("line-arrow") else builder
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
            viewModel.moveCameraToRoute(map, routes.flatMap { it.latLngs })
        }

        // 2. 绘制避让区（仍用普通 Polyline）
        if (avoidCustomArea) {
            try {
                val polygon = Polygon.fromJson(avoidPolygonJson)
                polygon.coordinates().getOrNull(0)?.let { points ->
                    val opts = LineOptions()
                        .withLatLngs(points.map { LatLng(it.latitude(), it.longitude()) })
                        .withStrokeColor("#FF0000")
                        .withStrokeWeight(2f)
                    engine.addPolyline(opts)
                }
            } catch (e: Exception) { }
        }
    }

    if (routeLines.isNotEmpty()) {
        Box(Modifier.size(1.dp).testTag("route_overlay_has_navigation_lines"))
    }
}

@Composable
fun AvoidOverlayItem(label: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.scale(0.6f).height(24.dp))
    }
}

@Composable
fun StrategyOverlayDropdown(currentStrategy: String, strategies: List<Pair<String, String>>, triggerTestTag: String = "strategy_dropdown_trigger", onStrategySelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart).testTag(triggerTestTag).clickable { expanded = !expanded }) {
        TextField(
            value = strategies.find { it.first == currentStrategy }?.second ?: "",
            onValueChange = { }, readOnly = true, enabled = false,
            label = { Text(if (strategies.any { it.first == MODE_DRIVING }) "" else stringResource(R.string.route_strategy), style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.4f).testTag("strategy_dropdown")) {
            strategies.forEach { (s, l) ->
                DropdownMenuItem(text = { Text(l, style = MaterialTheme.typography.bodySmall) }, onClick = { onStrategySelected(s); expanded = false }, modifier = Modifier.testTag("strategy_item_$s"))
            }
        }
    }
}
