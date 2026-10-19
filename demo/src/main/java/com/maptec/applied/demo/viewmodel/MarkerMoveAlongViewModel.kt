package com.maptec.applied.demo.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.demo.LOG_MODULE
import com.maptec.applied.demo.R
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.geometry.LatLngBounds
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.javabase.log.LoggerFactory
import com.maptec.applied.maps.MaptecMap
import com.maptec.applied.maps.overlay.marker.Marker
import com.maptec.applied.maps.overlay.marker.MarkerAnchorType
import com.maptec.applied.maps.overlay.marker.MarkerOptions
import com.maptec.applied.maps.overlay.marker.along.BackgroundBehavior
import com.maptec.applied.maps.overlay.marker.along.CancelBehavior
import com.maptec.applied.maps.overlay.marker.along.MoveAlongException
import com.maptec.applied.maps.overlay.marker.along.MoveAlongListener
import com.maptec.applied.maps.overlay.marker.along.MoveAlongMode
import com.maptec.applied.maps.overlay.marker.along.MoveAlongOptions
import com.maptec.applied.maps.overlay.marker.along.MoveAlongProgress
import com.maptec.applied.maps.overlay.marker.along.MoveAlongState
import com.maptec.applied.maps.overlay.marker.along.MoveAlongTask
import com.maptec.applied.maps.overlay.navigationline.NavigationLine
import com.maptec.applied.maps.overlay.navigationline.NavigationLineOptions
import com.maptec.applied.route.RouteService
import com.maptec.applied.style.Property
import com.maptec.applied.utils.BitmapUtils
import com.maptec.applied.route.data.LocationPoint
import com.maptec.applied.route.data.MODE_DRIVING
import com.maptec.applied.route.data.RouteRequest
import com.maptec.applied.route.data.RouteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.geojson.utils.PolylineUtils

data class MarkerMoveAlongUi(
    val bound: Boolean = false,
    val usingFallback: Boolean = true,
    val pointCount: Int = 0,
    val state: String = "NONE",
    val lastEvent: String = "",
    val errorCode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDegrees: Double? = null,
    val segmentIndex: Int? = null,
    val traveledMeters: Double? = null,
    val remainingMeters: Double? = null,
    val mode: MoveAlongMode = MoveAlongMode.DURATION,
    val durationMs: Long = 8_000L,
    val speedMps: Double = 80.0,
    val autoRotate: Boolean = true,
    val headingOffset: Double = 0.0,
    val orientationSmoothingMs: Long = 50L,
    val backgroundBehavior: BackgroundBehavior = BackgroundBehavior.PAUSE,
    val progressCallbackIntervalMs: Long = 100L,
    val seekProgress: Float = 0f,
    val toast: String? = null,
)

private val log = LoggerFactory.getLogger(LOG_MODULE).withTag("MarkerMoveAlongVM")

class MarkerMoveAlongViewModel(
    private val appContext: Context,
    private val calculateRoute: suspend (RouteRequest) -> Result<RouteResponse>,
) : ViewModel() {

    private val _ui = MutableStateFlow(MarkerMoveAlongUi())
    val ui: StateFlow<MarkerMoveAlongUi> = _ui.asStateFlow()

    private val lineArrowBitmap: Bitmap? =
        ContextCompat.getDrawable(appContext, R.drawable.line_arrow)
            ?.let { BitmapUtils.getBitmapFromDrawable(it) }

    private var mapRef: MaptecMap? = null
    private var path: MutableList<LatLng> = mutableListOf()
    private var routeLine: NavigationLine? = null
    private var marker: Marker? = null
    private var task: MoveAlongTask? = null
    private var startedByUser = false

    private val listener = object : MoveAlongListener {
        override fun onStart(taskId: String, markerId: String) {
            _ui.update { it.copy(lastEvent = "onStart", state = MoveAlongState.RUNNING.name) }
        }

        override fun onProgress(progress: MoveAlongProgress) {
            _ui.update {
                it.copy(
                    seekProgress = progress.progress.toFloat(),
                    state = currentStateName(),
                    latitude = progress.position.latitude,
                    longitude = progress.position.longitude,
                    headingDegrees = progress.headingDegrees,
                    segmentIndex = progress.segmentIndex,
                    traveledMeters = progress.traveledMeters,
                    remainingMeters = progress.remainingMeters,
                )
            }
        }

        override fun onPause(taskId: String, position: LatLng, progress: Double) {
            _ui.update {
                it.copy(
                    lastEvent = "onPause",
                    state = MoveAlongState.PAUSED.name,
                    seekProgress = progress.toFloat(),
                )
            }
        }

        override fun onResume(taskId: String, position: LatLng, progress: Double) {
            _ui.update {
                it.copy(
                    lastEvent = "onResume",
                    state = MoveAlongState.RUNNING.name,
                    seekProgress = progress.toFloat(),
                )
            }
        }

        override fun onFinish(
            endPosition: LatLng,
            totalMeters: Double,
            elapsedMs: Long,
            finishReason: com.maptec.applied.maps.overlay.marker.along.FinishReason,
        ) {
            _ui.update {
                it.copy(
                    lastEvent = "onFinish ${finishReason.name} ${elapsedMs}ms",
                    state = MoveAlongState.FINISHED.name,
                    seekProgress = 1f,
                    errorCode = "",
                )
            }
        }

        override fun onCancel(
            cancelBehavior: CancelBehavior,
            cancelReason: com.maptec.applied.maps.overlay.marker.along.CancelReason,
            finalPosition: LatLng,
        ) {
            _ui.update {
                it.copy(
                    lastEvent = "onCancel ${cancelReason.name}/${cancelBehavior.name}",
                    state = MoveAlongState.CANCELED.name,
                )
            }
        }

        override fun onError(errorCode: String, message: String) {
            _ui.update {
                it.copy(
                    lastEvent = "onError $message",
                    state = MoveAlongState.ERROR.name,
                    errorCode = errorCode,
                )
            }
        }
    }

    fun bind(map: MaptecMap) {
        if (mapRef === map) return
        unbind()
        mapRef = map
        map.overlayEngine.setMarkerLayerCollisionEnabled(false)
        _ui.update { it.copy(bound = true) }
        requestRoute()
    }

    fun clearToast() {
        _ui.update { it.copy(toast = null) }
    }

    fun setMode(mode: MoveAlongMode) {
        _ui.update { it.copy(mode = mode) }
        recreateIfReady()
    }

    fun setDurationMs(value: Long) {
        if (value <= 0L) return
        _ui.update { it.copy(durationMs = value) }
        recreateIfReady()
    }

    fun setSpeedMps(value: Double) {
        if (value <= 0.0) return
        _ui.update { it.copy(speedMps = value) }
        val current = task
        if (current != null && !current.isTerminal && current.mode == MoveAlongMode.SPEED) {
            runCatching { current.setSpeed(value) }.onFailure { handleControlError(it) }
        } else {
            recreateIfReady()
        }
    }

    fun setAutoRotate(value: Boolean) {
        _ui.update { it.copy(autoRotate = value) }
        val current = task
        if (current != null && !current.isTerminal) {
            runCatching { current.setAutoRotate(value) }.onFailure { handleControlError(it) }
        } else {
            recreateIfReady()
        }
    }

    fun setHeadingOffset(value: Double) {
        _ui.update { it.copy(headingOffset = value) }
        val current = task
        if (current != null && !current.isTerminal) {
            runCatching { current.setHeadingOffset(value) }.onFailure { handleControlError(it) }
        } else {
            recreateIfReady()
        }
    }

    fun setOrientationSmoothingMs(value: Long) {
        if (value < 0L) return
        _ui.update { it.copy(orientationSmoothingMs = value) }
        val current = task
        if (current != null && !current.isTerminal) {
            runCatching { current.setOrientationSmoothingMs(value) }.onFailure { handleControlError(it) }
        } else {
            recreateIfReady()
        }
    }

    fun setBackgroundBehavior(value: BackgroundBehavior) {
        _ui.update { it.copy(backgroundBehavior = value) }
        recreateIfReady()
    }

    fun setProgressCallbackIntervalMs(value: Long) {
        if (value < 16L) return
        _ui.update { it.copy(progressCallbackIntervalMs = value) }
        val current = task
        if (current != null && !current.isTerminal) {
            runCatching { current.setProgressCallbackIntervalMs(value) }.onFailure { handleControlError(it) }
        } else {
            recreateIfReady()
        }
    }

    fun start() {
        startedByUser = true
        runControl { ensureTask().start() }
    }

    fun pause() = runControl { requireTask().pause() }

    fun resume() = runControl { requireTask().resume() }

    fun finish() = runControl { requireTask().finish() }

    fun seekTo(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        _ui.update { it.copy(seekProgress = clamped) }
        runControl { ensureTask().seekTo(clamped.toDouble()) }
    }

    fun cancel(behavior: CancelBehavior) = runControl { requireTask().cancel(behavior) }

    /** 在 MapView.onDestroy 之前调用，避免沿线任务回调已销毁的 native 引擎。 */
    fun release() {
        unbind()
    }

    override fun onCleared() {
        unbind()
        super.onCleared()
    }

    private fun requestRoute() {
        viewModelScope.launch {
            val result = withTimeoutOrNull(ROUTE_TIMEOUT_MS) {
                calculateRoute(
                    RouteRequest(
                        origin = LocationPoint(GeoCoordinate(longitude = ORIGIN_LNG, latitude = ORIGIN_LAT)),
                        destination = LocationPoint(
                            GeoCoordinate(longitude = DEST_LNG, latitude = DEST_LAT),
                        ),
                        mode = MODE_DRIVING,
                    ),
                )
            }
            if (startedByUser || task?.state == MoveAlongState.RUNNING || task?.state == MoveAlongState.PAUSED) {
                return@launch
            }
            val decoded = result?.getOrNull()
                ?.takeIf { it.status == "OK" }
                ?.routes
                ?.firstOrNull()
                ?.overviewPolyline
                ?.takeUnless { it.isNullOrEmpty() }
                ?.let { decodePolyline(it) }
                ?.takeIf { it.size >= 2 }
            if (decoded != null) {
                applyPath(decoded, usingFallback = false, moveCamera = true)
            } else {
                log.e { "route unavailable, use fallback" }
                applyPath(FALLBACK_PATH, usingFallback = true, moveCamera = true)
            }
        }
    }

    private fun applyPath(points: List<LatLng>, usingFallback: Boolean, moveCamera: Boolean) {
        path = points.toMutableList()
        applyCurrentPath(usingFallback, moveCamera)
    }

    private fun applyCurrentPath(usingFallback: Boolean, moveCamera: Boolean) {
        val map = mapRef ?: return
        if (path.size < 2) return
        val engine = map.overlayEngine
        val existing = task
        if (existing != null && !existing.isTerminal) {
            if (existing.state == MoveAlongState.READY) {
                runCatching { existing.cancel(CancelBehavior.KEEP_CURRENT) }
            } else {
                return
            }
        }
        routeLine?.let { engine.deleteNavigationLine(it) }
        var opts = NavigationLineOptions()
            .withLatLngs(ArrayList(path))
            .withLineColor(ROUTE_LINE_COLOR)
            .withLineWidth(ROUTE_LINE_WIDTH)
            .withLineJoin(Property.LINE_JOIN_ROUND)
            .withLineCap(Property.LINE_CAP_ROUND)
            .withOutlineColor("#000000")
            .withOutlineWidth(1.0f)
        lineArrowBitmap?.let { bitmap ->
            opts = opts.withLinePattern(bitmap, "line-arrow")
        }
        routeLine = engine.addNavigationLine(opts)
        val start = path.first()
        val currentMarker = marker
        if (currentMarker == null) {
            marker = engine.addMarker(
                MarkerOptions()
                    .withLatLng(start)
                    .withIconResource(R.drawable.icon_car, appContext)
                    .withIconAnchor(MarkerAnchorType.CENTER)
                    .withIconSize(CAR_ICON_SIZE)
                    .withClickable(false)
                    .withDraggable(false),
            )
        } else {
            currentMarker.latLng = start
            currentMarker.iconSize = CAR_ICON_SIZE
        }
        createTask()
        _ui.update {
            it.copy(
                usingFallback = usingFallback,
                pointCount = path.size,
                state = currentStateName(),
                errorCode = "",
            )
        }
        if (moveCamera) {
            moveCameraToRoute(map, path)
        }
    }

    private fun recreateIfReady() {
        val current = task ?: return
        if (current.state == MoveAlongState.READY) {
            runCatching { current.cancel(CancelBehavior.KEEP_CURRENT) }
            createTask()
        }
    }

    private fun ensureTask(): MoveAlongTask {
        val current = task
        if (current != null && !current.isTerminal) {
            return current
        }
        return createTask()
    }

    private fun requireTask(): MoveAlongTask {
        return task ?: throw MoveAlongException.invalidState("task not created")
    }

    private fun createTask(): MoveAlongTask {
        val map = mapRef ?: throw MoveAlongException.invalidState("map not bound")
        val car = marker ?: throw MoveAlongException.notFound("car marker missing")
        val snapshot = _ui.value
        val options = MoveAlongOptions()
            .withPath(path)
            .withMode(snapshot.mode)
            .withAutoRotate(snapshot.autoRotate)
            .withHeadingOffset(snapshot.headingOffset)
            .withOrientationSmoothingMs(snapshot.orientationSmoothingMs)
            .withBackgroundBehavior(snapshot.backgroundBehavior)
            .withProgressCallbackIntervalMs(snapshot.progressCallbackIntervalMs)
        if (snapshot.mode == MoveAlongMode.DURATION) {
            options.withDurationMs(snapshot.durationMs)
        } else {
            options.withSpeedMetersPerSecond(snapshot.speedMps)
        }
        val created = map.overlayEngine.createMoveAlong(car, options)
        created.addListener(listener)
        task = created
        _ui.update { it.copy(state = created.state.name, errorCode = "") }
        return created
    }

    private fun runControl(action: () -> Unit) {
        try {
            action()
            _ui.update { it.copy(state = currentStateName(), errorCode = "") }
        } catch (error: Throwable) {
            handleControlError(error)
        }
    }

    private fun handleControlError(error: Throwable) {
        val code = (error as? MoveAlongException)?.errorCode ?: "ERROR"
        _ui.update {
            it.copy(
                toast = code,
                errorCode = code,
                lastEvent = error.message ?: code,
                state = currentStateName(),
            )
        }
    }

    private fun currentStateName(): String = task?.state?.name ?: "NONE"

    private fun unbind() {
        val current = task
        task = null
        if (current != null && !current.isTerminal) {
            runCatching { current.cancel(CancelBehavior.KEEP_CURRENT) }
        }
        val engine = runCatching { mapRef?.overlayEngine }.getOrNull()
        routeLine?.let { line ->
            engine?.let { runCatching { it.deleteNavigationLine(line) } }
        }
        marker?.let { car ->
            engine?.let { runCatching { it.deleteMarker(car) } }
        }
        routeLine = null
        marker = null
        mapRef = null
        startedByUser = false
        path.clear()
    }

    private fun moveCameraToRoute(map: MaptecMap, routePoints: List<LatLng>) {
        if (routePoints.isEmpty()) return
        val boundsBuilder = LatLngBounds.Builder()
        for (point in routePoints) {
            boundsBuilder.include(point)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 200, 200, 200, 200),
            500,
        )
    }

    private fun decodePolyline(polyline: String): List<LatLng> {
        return PolylineUtils.decode(polyline, 5).map { point ->
            LatLng(latitude = point.latitude(), longitude = point.longitude())
        }
    }

    companion object {
        private const val CAR_ICON_SIZE = 2.5f
        private const val ROUTE_LINE_COLOR = "#1677FF"
        private const val ROUTE_LINE_WIDTH = 10.0f
        private const val ROUTE_TIMEOUT_MS = 8_000L
        private const val ORIGIN_LNG = 103.8630
        private const val ORIGIN_LAT = 1.2820
        private const val DEST_LNG = 103.8920
        private const val DEST_LAT = 1.3920

        val FALLBACK_PATH = listOf(
            LatLng(ORIGIN_LAT, ORIGIN_LNG),
            LatLng(1.3100, 103.8700),
            LatLng(1.3500, 103.8800),
            LatLng(DEST_LAT, DEST_LNG),
        )
    }
}

class MarkerMoveAlongViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkerMoveAlongViewModel::class.java)) {
            val routeService = RouteService.getInstance(context.applicationContext)
            return MarkerMoveAlongViewModel(
                context.applicationContext,
                routeService::calculateRoute,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
