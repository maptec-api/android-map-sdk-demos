package com.maptec.applied.demo.viewmodel

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maptec.applied.camera.CameraUpdateFactory
import com.maptec.applied.geometry.LatLng
import com.maptec.applied.maps.MaptecMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class PoiClickViewModel : ViewModel() {

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

    companion object {
        const val POI_LAYER_ID = "poi"
    }

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
     * 处理地图点击，查询 POI 图层
     * @return true 表示点击被消费
     */
    fun onMapClick(point: LatLng): Boolean {
        if (!_poiCenterEnabled.value) return false
        val map = maptecMapRef ?: return false

        val screenPoint: PointF = map.projection.toScreenLocation(point)
        val features: List<Feature> = map.queryRenderedFeatures(screenPoint, POI_LAYER_ID)

        if (features.isEmpty()) {
            clearSelection()
            return false
        }

        val feature = features[0]
        val name = feature.getStringProperty("name") ?: "未知POI"
        val poiType = feature.getStringProperty("poi_type") ?: ""

        val geometry = feature.geometry()
        val poiLatLng = if (geometry is Point) {
            LatLng(geometry.latitude(), geometry.longitude())
        } else {
            point
        }

        _selectedPoiName.value = name
        _selectedPoiType.value = poiType
        _selectedPoiLatLng.value = poiLatLng

        map.animateCamera(
            CameraUpdateFactory.newLatLng(poiLatLng),
            500
        )

        _toastMessage.value = "POI: $name ($poiType)"
        return true
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
