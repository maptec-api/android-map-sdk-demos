package com.maptec.applied.demo.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.location.LocationComponent
import com.maptec.applied.gpsprovider.LocationEngine
import com.maptec.applied.gpsprovider.LocationEngineCallback
import com.maptec.applied.gpsprovider.LocationEngineDefault
import com.maptec.applied.gpsprovider.LocationEngineRequest
import com.maptec.applied.gpsprovider.LocationEngineResult
import com.maptec.applied.demo.ui.screens.interaction.controls.MockGpsTrackPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

/**
 * 检查定位权限
 */
data class LocationScreenUiState(
    val permissionGranted: Boolean = false,
    val errorMessage: String? = null,
    val mockPlaybackActive: Boolean = false
)

/**
 * LocationScreen ViewModel
 */
class LocationScreenViewModel(
    private val appContext: Context,
    @get:VisibleForTesting internal val locationEngine: LocationEngine = LocationEngineDefault.getDefaultLocationEngine(appContext)
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationScreenUiState())
    val uiState: StateFlow<LocationScreenUiState> = _uiState.asStateFlow()

    private var locationComponentRef: WeakReference<LocationComponent>? = null
    private var mockGpsTrackPlayer: MockGpsTrackPlayer? = null

    @get:VisibleForTesting
    internal val currentLocationEngineCallback: CurrentLocationEngineCallback by lazy {
        CurrentLocationEngineCallback(null)
    }

    private val locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(1000L)
            .setFastestInterval(1000L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted, errorMessage = null) }
        updateContinuousLocationSubscription()
    }

    /**
     * 设置locationComponent
     */
    fun setLocationComponent(component: LocationComponent?) {
        locationComponentRef = component?.let { WeakReference(it) }
        currentLocationEngineCallback.setComponent(component)
        updateContinuousLocationSubscription()
    }

    @SuppressLint("MissingPermission")
    private fun updateContinuousLocationSubscription() {
        val hasPermission = _uiState.value.permissionGranted
        val hasComponent = locationComponentRef?.get() != null
        val mockActive = _uiState.value.mockPlaybackActive

        if (hasPermission && hasComponent && !mockActive) {
            locationEngine.requestLocationUpdates(
                locationEngineRequest,
                currentLocationEngineCallback,
                Looper.getMainLooper()
            )
        } else {
            locationEngine.removeLocationUpdates(currentLocationEngineCallback)
        }
    }

    fun toggleMockPlayback() {
        if (_uiState.value.mockPlaybackActive) {
            stopMockPlayback()
        } else {
            startMockPlayback()
        }
    }

    fun startMockPlayback() {
        val component = locationComponentRef?.get() ?: return
        mockGpsTrackPlayer?.stop(notify = false)
        locationEngine.removeLocationUpdates(currentLocationEngineCallback)
        mockGpsTrackPlayer = MockGpsTrackPlayer(
            context = appContext,
            locationComponent = component,
            onPlaybackStopped = {
                mockGpsTrackPlayer = null
                _uiState.update { it.copy(mockPlaybackActive = false) }
                updateContinuousLocationSubscription()
            }
        ).also { it.start() }
        _uiState.update { it.copy(mockPlaybackActive = true) }
    }

    fun stopMockPlayback() {
        mockGpsTrackPlayer?.stop()
        mockGpsTrackPlayer = null
        _uiState.update { it.copy(mockPlaybackActive = false) }
        updateContinuousLocationSubscription()
    }

    override fun onCleared() {
        stopMockPlayback()
        locationEngine.removeLocationUpdates(currentLocationEngineCallback)
        locationComponentRef = null
        super.onCleared()
    }
}

/**
 * Callback that forwards [LocationEngineResult] to [LocationComponent.forceLocationUpdate].
 * Holds a [WeakReference] to the component so the engine does not retain the map.
 */
@VisibleForTesting
class CurrentLocationEngineCallback(component: LocationComponent?) : LocationEngineCallback<LocationEngineResult> {

    private var componentWeakReference = WeakReference(component)

    internal fun setComponent(component: LocationComponent?) {
        componentWeakReference = WeakReference(component)
    }

    override fun onSuccess(result: LocationEngineResult) {
        val component = componentWeakReference.get()
        if (component != null) {
            component.updatePosition(result.lastLocation)
        }
    }

    override fun onFailure(exception: Exception) {
        // No-op; optional: report to ViewModel
    }
}

class LocationScreenViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationScreenViewModel::class.java)) {
            return LocationScreenViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
