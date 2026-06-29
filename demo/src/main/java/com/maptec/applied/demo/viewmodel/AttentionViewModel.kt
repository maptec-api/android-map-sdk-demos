package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.maptec.applied.MapSDK
import com.maptec.applied.demo.R
import com.maptec.applied.search.service.GeocodeService
import com.maptec.applied.search.service.SearchService
import com.maptec.applied.route.RouteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttentionViewModel(
    private val context: Context
) : ViewModel() {

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _sha1Input = MutableStateFlow("")
    val sha1Input: StateFlow<String> = _sha1Input.asStateFlow()

    private val _searchChecked = MutableStateFlow(true)
    val searchChecked: StateFlow<Boolean> = _searchChecked.asStateFlow()

    private val _routeChecked = MutableStateFlow(true)
    val routeChecked: StateFlow<Boolean> = _routeChecked.asStateFlow()

    private val _mapChecked = MutableStateFlow(true)
    val mapChecked: StateFlow<Boolean> = _mapChecked.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _authStatusJson = MutableStateFlow<String?>(null)
    val authStatusJson: StateFlow<String?> = _authStatusJson.asStateFlow()

    private val _authStatusLoading = MutableStateFlow(false)
    val authStatusLoading: StateFlow<Boolean> = _authStatusLoading.asStateFlow()

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    fun updateApiKeyInput(value: String) {
        _apiKeyInput.value = value
    }

    fun updateSha1Input(value: String) {
        _sha1Input.value = value
    }

    fun setSearchChecked(checked: Boolean) {
        _searchChecked.value = checked
    }

    fun setRouteChecked(checked: Boolean) {
        _routeChecked.value = checked
    }

    fun setMapChecked(checked: Boolean) {
        _mapChecked.value = checked
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun applyApiKey() {
        val formatKey = _apiKeyInput.value.trim()
        if (formatKey.isBlank()) {
            _toastMessage.value = context.getString(R.string.attention_toast_enter_api_key)
            return
        }
        if (!_searchChecked.value && !_routeChecked.value && !_mapChecked.value) {
            _toastMessage.value = context.getString(R.string.attention_toast_select_sdk_for_api_key)
            return
        }

        val applied = mutableListOf<String>()
        val errors = mutableListOf<String>()

        if (_mapChecked.value) {
            try {
                MapSDK.getInstance(context, null)
                MapSDK.setApiKey(formatKey)
                applied.add(context.getString(R.string.attention_sdk_name_map))
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_map)}: ${e.message}")
            }
        }

        if (_searchChecked.value) {
            try {
                SearchService.getInstance(context, null)
                SearchService.setApiKey(formatKey)
                GeocodeService.getInstance(context, null)
                GeocodeService.setApiKey(formatKey)
                applied.add(context.getString(R.string.attention_sdk_name_search_geocode))
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_search_geocode)}: ${e.message}")
            }
        }
        if (_routeChecked.value) {
            try {
                RouteService.getInstance(context, null)
                RouteService.setApiKey(formatKey)
                applied.add(context.getString(R.string.attention_sdk_name_route))
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_route)}: ${e.message}")
            }
        }

        when {
            applied.isEmpty() && errors.isEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_no_change)
            applied.isEmpty() && errors.isNotEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_all_failed_format, errors.joinToString())
            errors.isNotEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_updated_with_errors_format, applied.joinToString(), errors.joinToString())
            else ->
                _toastMessage.value = context.getString(R.string.attention_toast_updated_format, applied.joinToString())
        }
    }

    fun applySignatureSha1() {
        if (!_searchChecked.value && !_routeChecked.value && !_mapChecked.value) {
            _toastMessage.value = context.getString(R.string.attention_toast_select_sdk_for_sha1)
            return
        }

        val formatSha1 = _sha1Input.value.trim()
        if (formatSha1.isBlank()) {
            _toastMessage.value = context.getString(R.string.attention_toast_enter_sha1)
            return
        }
        val sha1Arg = formatSha1.takeIf { it.isNotEmpty() }

        val applied = mutableListOf<String>()
        val errors = mutableListOf<String>()

        if (_mapChecked.value) {
            try {
                MapSDK.getInstance(context, null)
                MapSDK.setSignatureSha1(sha1Arg)
                if (formatSha1.isNotEmpty()) {
                    applied.add(context.getString(R.string.attention_sdk_name_sha1_map))
                } else {
                    applied.add(context.getString(R.string.attention_toast_signature_default_format, context.getString(R.string.attention_sdk_name_map)))
                }
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_map)}: ${e.message}")
            }
        }

        if (_searchChecked.value) {
            try {
                SearchService.getInstance(context, null)
                SearchService.setSignatureSha1(sha1Arg)
                GeocodeService.getInstance(context, null)
                GeocodeService.setSignatureSha1(sha1Arg)
                if (formatSha1.isNotEmpty()) {
                    applied.add(context.getString(R.string.attention_sdk_name_sha1_search_geocode))
                } else {
                    applied.add(context.getString(R.string.attention_toast_signature_default_format, context.getString(R.string.attention_sdk_name_search_geocode)))
                }
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_search_geocode)}: ${e.message}")
            }
        }
        if (_routeChecked.value) {
            try {
                RouteService.getInstance(context, null)
                RouteService.setSignatureSha1(sha1Arg)
                if (formatSha1.isNotEmpty()) {
                    applied.add(context.getString(R.string.attention_sdk_name_sha1_route))
                } else {
                    applied.add(context.getString(R.string.attention_toast_signature_default_format, context.getString(R.string.attention_sdk_name_route)))
                }
            } catch (e: Exception) {
                errors.add("${context.getString(R.string.attention_sdk_name_route)}: ${e.message}")
            }
        }

        when {
            applied.isEmpty() && errors.isEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_no_change)
            applied.isEmpty() && errors.isNotEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_all_failed_format, errors.joinToString())
            errors.isNotEmpty() ->
                _toastMessage.value = context.getString(R.string.attention_toast_updated_with_errors_format, applied.joinToString(), errors.joinToString())
            else ->
                _toastMessage.value = context.getString(R.string.attention_toast_updated_format, applied.joinToString())
        }
    }

    fun loadAuthStatus() {
        viewModelScope.launch {
            _authStatusLoading.value = true
            _authStatusJson.value = null
            try {
                val searchService = SearchService.getInstance(context, null)
                val result = searchService.getAuthStatus()
                result.onSuccess { response ->
                    _authStatusJson.value = prettyGson.toJson(response)
                }.onFailure { t ->
                    _authStatusJson.value = "{\n  \"error\": \"请求失败\",\n  \"message\": \"${(t.message ?: "未知错误").replace("\"", "\\\"")}\"\n}"
                    _toastMessage.value = context.getString(R.string.attention_toast_auth_status_error_format, t.message)
                }
            } catch (e: Exception) {
                _authStatusJson.value = "{\n  \"error\": \"请求失败\",\n  \"message\": \"${(e.message ?: "未知错误").replace("\"", "\\\"")}\"\n}"
                _toastMessage.value = context.getString(R.string.attention_toast_auth_status_error_format, e.message)
            } finally {
                _authStatusLoading.value = false
            }
        }
    }
}

class AttentionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != AttentionViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
        return AttentionViewModel(context.applicationContext) as T
    }
}
