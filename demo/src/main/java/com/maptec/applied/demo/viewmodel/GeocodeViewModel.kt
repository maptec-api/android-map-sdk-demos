package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.maptec.applied.javabase.bean.GeoCoordinate
import com.maptec.applied.search.model.common.LocationBias
import com.maptec.applied.search.model.request.GeocodeRequest
import com.maptec.applied.search.model.request.ReverseGeocodeRequest
import com.maptec.applied.search.model.response.GeocodeResponse
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.service.GeocodeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeocodeViewModel(
    private val geocodeService: GeocodeService
) : ViewModel() {

    private val prettyGson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    data class ApiSnapshot(
        val apiName: String,
        val responseJson: String
    )

    private val _apiSnapshot = MutableStateFlow<ApiSnapshot?>(null)
    val apiSnapshot: StateFlow<ApiSnapshot?> = _apiSnapshot.asStateFlow()

    enum class Mode(val label: String) {
        FORWARD("正向地理编码"),
        REVERSE("反向地理编码")
    }

    private val _mode = MutableStateFlow(Mode.FORWARD)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _response = MutableStateFlow<GeocodeResponse?>(null)
    val response: StateFlow<GeocodeResponse?> = _response.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private val _address = MutableStateFlow("Lorong")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _components = MutableStateFlow("")
    val components: StateFlow<String> = _components.asStateFlow()

    private val _language = MutableStateFlow("zh")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _region = MutableStateFlow("")
    val region: StateFlow<String> = _region.asStateFlow()

    private val _enableBiasRectangle = MutableStateFlow(false)
    val enableBiasRectangle: StateFlow<Boolean> = _enableBiasRectangle.asStateFlow()

    private val _rectNorthEast = MutableStateFlow("")
    val rectNorthEast: StateFlow<String> = _rectNorthEast.asStateFlow()

    private val _rectSouthWest = MutableStateFlow("")
    val rectSouthWest: StateFlow<String> = _rectSouthWest.asStateFlow()

    private val _reverseLocation = MutableStateFlow("1.360879,103.732578")
    val reverseLocation: StateFlow<String> = _reverseLocation.asStateFlow()

    private val _reverseResultType = MutableStateFlow("premise")
    val reverseResultType: StateFlow<String> = _reverseResultType.asStateFlow()

    private val _reverseLocationType = MutableStateFlow("")
    val reverseLocationType: StateFlow<String> = _reverseLocationType.asStateFlow()

    fun switchMode(mode: Mode) {
        _mode.value = mode
        _errorMessage.value = null
        _response.value = null
        _apiSnapshot.value = null
    }

    fun updateAddress(value: String) {
        _address.value = value
    }

    fun updateComponents(value: String) {
        _components.value = value
    }

    fun updateLanguage(value: String) {
        _language.value = value
    }

    fun updateRegion(value: String) {
        _region.value = value
    }

    fun updateEnableBiasRectangle(value: Boolean) {
        _enableBiasRectangle.value = value
    }

    fun updateRectNorthEast(value: String) {
        _rectNorthEast.value = value
    }

    fun updateRectSouthWest(value: String) {
        _rectSouthWest.value = value
    }

    fun updateReverseLocation(value: String) {
        _reverseLocation.value = value
    }

    fun updateReverseResultType(value: String) {
        _reverseResultType.value = value
    }

    fun updateReverseLocationType(value: String) {
        _reverseLocationType.value = value
    }

    fun submit() {
        when (_mode.value) {
            Mode.FORWARD -> submitForward()
            Mode.REVERSE -> submitReverse()
        }
    }

    private fun submitForward() {
        _errorMessage.value = null
        _response.value = null
        _apiSnapshot.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val bias = if (_enableBiasRectangle.value) {
                val ne = parseCoordinates(_rectNorthEast.value)
                val sw = parseCoordinates(_rectSouthWest.value)
                if (ne != null && sw != null) LocationBias.Rectangle(ne, sw) else null
            } else {
                null
            }

            val req = GeocodeRequest(
                address = _address.value.trim().takeIf { it.isNotEmpty() },
                components = _components.value.trim().takeIf { it.isNotEmpty() },
                language = _language.value.trim().takeIf { it.isNotEmpty() },
                region = _region.value.trim().takeIf { it.isNotEmpty() },
                locationBias = bias
            )

            val result = geocodeService.geocode(req)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _response.value = it
                    _apiSnapshot.value = ApiSnapshot(apiName = "geocode", responseJson = prettyGson.toJson(it))
                    if (it.status != ResponseStatus.OK) {
                        it.error?.let { err ->
                            _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                        }
                    }
                },
                onFailure = {
                    val message = it.message ?: it.toString()
                    _errorMessage.value = message
                    _apiSnapshot.value = ApiSnapshot(
                        apiName = "geocode",
                        responseJson = """{"status":"ERROR","error":"$message"}""",
                    )
                }
            )
        }
    }

    private fun submitReverse() {
        _errorMessage.value = null
        _response.value = null
        _apiSnapshot.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val loc = parseCoordinates(_reverseLocation.value)
            if (loc == null) {
                _isLoading.value = false
                _errorMessage.value = "location 格式错误，应为: lat,lng"
                return@launch
            }

            val req = ReverseGeocodeRequest(
                location = loc,
                resultType = _reverseResultType.value.trim(),
                locationType = _reverseLocationType.value.trim().takeIf { it.isNotEmpty() },
                language = _language.value.trim().takeIf { it.isNotEmpty() },
                region = _region.value.trim().takeIf { it.isNotEmpty() }
            )

            val result = geocodeService.reverseGeocode(req)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _response.value = it
                    _apiSnapshot.value = ApiSnapshot(apiName = "reverseGeocode", responseJson = prettyGson.toJson(it))
                    if (it.status != ResponseStatus.OK) {
                        it.error?.let { err ->
                            _toastMessage.value = "服务器返回错误${err.code}：${err.message}"
                        }
                    }
                },
                onFailure = {
                    val message = it.message ?: it.toString()
                    _errorMessage.value = message
                    _apiSnapshot.value = ApiSnapshot(
                        apiName = "reverseGeocode",
                        responseJson = """{"status":"ERROR","error":"$message"}""",
                    )
                }
            )
        }
    }

    private fun parseCoordinates(raw: String): GeoCoordinate? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        return GeoCoordinate(longitude = lng, latitude = lat)
    }
}

class GeocodeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeocodeViewModel::class.java)) {
            val service = GeocodeService.getInstance(context.applicationContext, null)
            return GeocodeViewModel(service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
