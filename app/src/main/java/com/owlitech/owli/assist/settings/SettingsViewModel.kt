package com.owlitech.owli.assist.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val openRouterUserKeyStore: OpenRouterUserKeyStore
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hasOpenRouterUserKey = MutableStateFlow(false)
    val hasOpenRouterUserKey: StateFlow<Boolean> = _hasOpenRouterUserKey.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collectLatest { _settings.value = it }
        }
        refreshOpenRouterUserKeyState()
    }

    fun update(update: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.update(update)
        }
    }

    fun saveOpenRouterUserKey(apiKey: String) {
        val normalized = apiKey.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            openRouterUserKeyStore.saveKey(normalized)
            _hasOpenRouterUserKey.value = true
            repository.update { it.copy(openRouterKeyMode = OpenRouterKeyMode.USER_PROVIDED_KEY) }
        }
    }

    fun clearOpenRouterUserKey() {
        viewModelScope.launch(Dispatchers.IO) {
            openRouterUserKeyStore.clearKey()
            _hasOpenRouterUserKey.value = false
            repository.update { it.copy(openRouterKeyMode = OpenRouterKeyMode.EMBEDDED_APP_KEY) }
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            openRouterUserKeyStore.clearKey()
            _hasOpenRouterUserKey.value = false
            repository.resetToDefaults()
        }
    }

    private fun refreshOpenRouterUserKeyState() {
        viewModelScope.launch(Dispatchers.IO) {
            _hasOpenRouterUserKey.value = openRouterUserKeyStore.hasKey()
        }
    }

    class Factory(
        private val repository: SettingsRepository,
        private val openRouterUserKeyStore: OpenRouterUserKeyStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(repository, openRouterUserKeyStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
