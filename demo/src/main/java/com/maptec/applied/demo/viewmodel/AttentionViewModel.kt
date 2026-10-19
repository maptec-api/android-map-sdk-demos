package com.maptec.applied.demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.maptec.applied.MaptecInitializer
import com.maptec.applied.demo.R
import com.maptec.applied.search.service.SearchService
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

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun applyApiKey() {
        val formatKey = _apiKeyInput.value.trim()
        if (formatKey.isBlank()) {
            _toastMessage.value = context.getString(R.string.attention_toast_enter_api_key)
            return
        }
        try {
            MaptecInitializer.setApiKey(formatKey)
            _toastMessage.value = context.getString(R.string.attention_toast_api_key_updated)
        } catch (e: Exception) {
            _toastMessage.value = context.getString(
                R.string.attention_toast_all_failed_format,
                e.message ?: "",
            )
        }
    }

    fun applySignatureSha1() {
        val formatSha1 = _sha1Input.value.trim()
        if (formatSha1.isBlank()) {
            _toastMessage.value = context.getString(R.string.attention_toast_enter_sha1)
            return
        }
        try {
            MaptecInitializer.setSignatureSha1(formatSha1)
            _toastMessage.value = context.getString(R.string.attention_toast_sha1_updated)
        } catch (e: Exception) {
            _toastMessage.value = context.getString(
                R.string.attention_toast_all_failed_format,
                e.message ?: "",
            )
        }
    }

    fun loadAuthStatus() {
        viewModelScope.launch {
            _authStatusLoading.value = true
            _authStatusJson.value = null
            try {
                val searchService = SearchService.getInstance(context)
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
