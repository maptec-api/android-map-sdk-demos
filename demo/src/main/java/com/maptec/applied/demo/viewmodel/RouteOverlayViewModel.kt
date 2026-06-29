package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.javabase.error.CommonError
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.route.RouteService
import com.maptec.applied.route.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.maptec.applied.geometry.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.maplibre.geojson.utils.PolylineUtils
import com.maptec.applied.javabase.error.Error
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.route.RouteError

data class RouteOverlayUIData(
    val latLngs: List<LatLng>,
    val summary: Summary
)

private val log = LoggerFactory.getLogger(LOG_MODULE).withTag("RouteOverlayViewModel")

class RouteOverlayViewModel(private val routeService: RouteService): ViewModel() {

    private val _routes = MutableStateFlow<List<RouteOverlayUIData>>(emptyList())
    val routes: StateFlow<List<RouteOverlayUIData>> = _routes.asStateFlow()

    private val _mainRouteSummary = MutableStateFlow<Summary?>(null)
    val mainRouteSummary: StateFlow<Summary?> = _mainRouteSummary.asStateFlow()

    private val _error = MutableStateFlow<Error?>(null)
    val error: StateFlow<Error?> = _error.asStateFlow()

    private var job: Job? = null

     fun doSearchRoute(
         start: String,
         destination: String,
         waypointsStr: String = "",
         alternatives: Boolean = true,
         strategy: String = STRATEGY_FASTEST,
         avoid: Avoid? = null,
         mode: String = MODE_DRIVING
     ) {
        job?.cancel()
        // 执行本次算路前清除上一次的路线
        _routes.value = emptyList()
        _mainRouteSummary.value = null
         _error.value = null

        val startCoordinates = toCoordinates(start)
        val destinationCoordinates = toCoordinates(destination)
        if(startCoordinates == null || destinationCoordinates == null)  {
            _error.value = RouteError(RouteError.INVALID_PARAMETER.code, "起终点坐标格式错误")
            return
        }

        val waypoints = parseWaypoints(waypointsStr)
        if (waypoints == null) {
            _error.value = RouteError(RouteError.INVALID_PARAMETER.code, "途经点格式错误，请使用 [(lat,lng),...] 格式")
            return
        }

        job = viewModelScope.launch {
            log.d { "doSearchRoute: start: $start, destination: $destination, waypoints: $waypointsStr, alternatives: $alternatives, strategy: $strategy, avoid: $avoid" }
            routeService.calculateRoute(
                RouteRequest(
                    origin = LocationPoint(startCoordinates),
                    destination = LocationPoint(destinationCoordinates),
                    waypoints = waypoints,
                    alternatives = alternatives,
                    strategy = strategy,
                    avoid = avoid,
                    mode = mode
                )
            ).onSuccess {
                if(it.status != "OK"){
                    log.e { "doSearchRoute: status: ${it.status}" }
                    _error.value = RouteError.fromServer(it.error)
                    return@launch
                }
                
                val uiRoutes = it.routes.mapNotNull { route ->
                    val polyline = route.overviewPolyline
                    if (!polyline.isNullOrEmpty()) {
                        RouteOverlayUIData(toLatLngs(polyline), route.summary)
                    } else null
                }
                _routes.value = uiRoutes

                if (uiRoutes.isNotEmpty()) {
                    _mainRouteSummary.value = uiRoutes[0].summary
                }
                
            }.onFailure { it ->
                log.e { "doSearchRoute: error: ${it.message}" }
                _error.value = CommonError.NETWORK_ERROR(it.toString())
            }
        }
    }

    fun switchRoute(index: Int) {
        val current = _routes.value.toMutableList()
        if (index > 0 && index < current.size) {
            val selected = current.removeAt(index)
            current.add(0, selected)
            _routes.value = current
            _mainRouteSummary.value = selected.summary
        }
    }

    private fun toCoordinates(str: String): GeoCoordinate? {
        return try {
            val parts = str.split(",")
            if (parts.size == 2) {
                GeoCoordinate(longitude = parts[0].trim().toDouble(), latitude = parts[1].trim().toDouble())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWaypoints(str: String): List<LocationPoint>? {
        if (str.isBlank() || str.trim() == "[]") return emptyList()
        
        val result = mutableListOf<LocationPoint>()
        try {
            // 匹配 (lat,lng) 格式
            val regex = Regex("\\(([^)]+)\\)")
            val matches = regex.findAll(str)
            
            val matchesList = matches.toList()
            if (matchesList.isEmpty()) return null
            
            for (match in matchesList) {
                val pair = match.groupValues[1].split(",")
                if (pair.size == 2) {
                    val lat = pair[0].trim().toDouble()
                    val lng = pair[1].trim().toDouble()
                    result.add(LocationPoint(GeoCoordinate(longitude = lng, latitude = lat)))
                } else {
                    return null
                }
            }
        } catch (e: Exception) {
            log.e { "parseWaypoints error: ${e.message}" }
            return null
        }
        return result
    }

    private fun toLatLngs(polyline: String): List<LatLng> {
        val points = PolylineUtils.decode(polyline,5)
        return points.map {
            LatLng(latitude = it.latitude(), longitude = it.longitude())
        }
    }

     fun moveCameraToRoute(map: MaptecMap, routePoints: List<LatLng>) {
        if (routePoints.isEmpty()) return

        // 1. 创建 LatLngBounds.Builder 并包含所有路线点
        val boundsBuilder = LatLngBounds.Builder()
        for (point in routePoints) {
            boundsBuilder.include(point)
        }

        // 2. 构建最终的边界范围
        val bounds = boundsBuilder.build()

        // 3. 使用 CameraUpdateFactory 创建更新对象
        // 参数 2、3、4、5 分别为左、上、右、下的内边距（单位：像素），防止路线贴边
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100, 100, 100, 100)

        // 4. 执行相机动画
        map.animateCamera(cameraUpdate, 500) // 500ms平滑过渡
    }
}

class RouteOverlayViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteOverlayViewModel::class.java)) {
            val routeService = RouteService.getInstance(context.applicationContext, null)
            return RouteOverlayViewModel(routeService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
