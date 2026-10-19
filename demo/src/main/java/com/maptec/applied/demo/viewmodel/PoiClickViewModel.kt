package com.maptec.applied.demo.viewmodel

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MapHitItem.MapQueryType
import com.maptec.applied.maps.MaptecMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PoiClickViewModel : ViewModel() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _poiCenterEnabled = MutableStateFlow(true)
    val poiCenterEnabled: StateFlow<Boolean> = _poiCenterEnabled.asStateFlow()

    private val _selectedPoiName = MutableStateFlow<String?>(null)
    val selectedPoiName: StateFlow<String?> = _selectedPoiName.asStateFlow()

    private val _selectedPoiType = MutableStateFlow<String?>(null)
    val selectedPoiType: StateFlow<String?> = _selectedPoiType.asStateFlow()

    private val _selectedPoiLatLng = MutableStateFlow<LatLng?>(null)
    val selectedPoiLatLng: StateFlow<LatLng?> = _selectedPoiLatLng.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var maptecMapRef: MaptecMap? = null

    fun setPoiCenterEnabled(enabled: Boolean) {
        _poiCenterEnabled.value = enabled
        if (!enabled) {
            clearSelection()
        }
    }

    fun setMap(map: MaptecMap) {
        maptecMapRef = map
    }

    /**
     * 发起 POI 拾取。回调是异步的，不能用命中结果决定本次点击是否消费。
     * @return 始终 false，不拦截后续 listener
     */
    fun onMapClick(point: LatLng): Boolean {
        if (!_poiCenterEnabled.value) return false
        val map = maptecMapRef ?: return false

        val screenPoint: PointF = map.projection.toScreenLocation(point)
        map.queryMapHit(screenPoint, MapQueryType.POI) { hits ->
            // 回调不在主线程；改相机 / Compose State 时自行切回
            mainHandler.post {
                val currentMap = maptecMapRef ?: return@post
                val hit = hits.firstOrNull()
                if (hit == null) {
                    clearSelection()
                    return@post
                }
                val name = hit.featureData?.getValue("name") as? String ?: "未知POI"
                val poiType = hit.featureData?.getValue("poi_type") as? String ?: "Unknown"

                val poiLatLng = hit.featureData?.anchor ?: point

                _selectedPoiName.value = name
                _selectedPoiType.value = poiType
                _selectedPoiLatLng.value = poiLatLng

                currentMap.animateCamera(
                    CameraUpdateFactory.newLatLng(poiLatLng),
                    500
                )

                _toastMessage.value = "POI: $name ($poiType)"
            }
        }
        return false
    }

    fun clearSelection() {
        _selectedPoiName.value = null
        _selectedPoiType.value = null
        _selectedPoiLatLng.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        maptecMapRef = null
        mainHandler.removeCallbacksAndMessages(null)
    }
}

class PoiClickViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoiClickViewModel::class.java)) {
            return PoiClickViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
