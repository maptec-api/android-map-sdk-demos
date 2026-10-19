// DEBUG: remove after Overlay verification
package com.maptec.applied.demo.ui.screens.interaction.controls

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.maptec.applied.location.LocationComponent
import org.json.JSONArray
import org.json.JSONObject

internal data class MockGpsPoint(
    val lat: Double,
    val lng: Double,
    val bearing: Float,
    val accuracy: Float,
    val intervalMs: Long
)

/**
 * Reads a mock GPS track from assets and replays it via [LocationComponent.updatePosition].
 */
internal class MockGpsTrackPlayer(
    context: Context,
    private val locationComponent: LocationComponent,
    private val assetPath: String = "mock_gps/default_track.json",
    private val onPlaybackStopped: () -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var points: List<MockGpsPoint> = emptyList()
    private var index = 0
    private var stopped = true

    fun start() {
        handler.removeCallbacksAndMessages(null)
        stopped = false
        points = loadTrack(appContext, assetPath)
        if (points.isEmpty()) {
            stopped = true
            onPlaybackStopped()
            return
        }
        index = 0
        playNext()
    }

    fun stop(notify: Boolean = true) {
        if (stopped) {
            return
        }
        stopped = true
        handler.removeCallbacksAndMessages(null)
        if (notify) {
            onPlaybackStopped()
        }
    }

    val isPlaying: Boolean
        get() = !stopped

    private fun playNext() {
        if (stopped || index >= points.size) {
            stop()
            return
        }
        val point = points[index++]
        val location = Location("mock_gps").apply {
            latitude = point.lat
            longitude = point.lng
            bearing = point.bearing
            accuracy = point.accuracy
            time = System.currentTimeMillis()
        }
        locationComponent.updatePosition(location)
        if (index < points.size) {
            handler.postDelayed({ playNext() }, point.intervalMs)
        } else {
            handler.postDelayed({ stop() }, point.intervalMs)
        }
    }

    companion object {
        fun loadTrack(context: Context, assetPath: String): List<MockGpsPoint> {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(obj.toMockGpsPoint())
                }
            }
        }

        private fun JSONObject.toMockGpsPoint(): MockGpsPoint {
            return MockGpsPoint(
                lat = getDouble("lat"),
                lng = getDouble("lng"),
                bearing = optDouble("bearing", 0.0).toFloat(),
                accuracy = optDouble("accuracy", 10.0).toFloat(),
                intervalMs = optLong("intervalMs", 1000L)
            )
        }
    }
}
