package com.owlitech.owli.assist.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.owlitech.owli.assist.settings.OpenRouterCurrentKeyInfoException.InvalidResponse
import com.owlitech.owli.assist.settings.OpenRouterCurrentKeyInfoException.Network
import com.owlitech.owli.assist.settings.OpenRouterCurrentKeyInfoException.NoActiveKey
import com.owlitech.owli.assist.settings.OpenRouterCurrentKeyInfoException.Unauthorized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val openRouterUserKeyStore: OpenRouterUserKeyStore,
    private val embeddedOpenRouterApiKey: String,
    private val openRouterCurrentKeyInfoService: OpenRouterCurrentKeyInfoService
) : ViewModel() {
    val hasEmbeddedOpenRouterAppKey: Boolean = embeddedOpenRouterApiKey.isNotBlank()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hasOpenRouterUserKey = MutableStateFlow(false)
    val hasOpenRouterUserKey: StateFlow<Boolean> = _hasOpenRouterUserKey.asStateFlow()

    private val _openRouterKeyInfoState = MutableStateFlow<OpenRouterKeyInfoUiState>(OpenRouterKeyInfoUiState.Idle)
    val openRouterKeyInfoState: StateFlow<OpenRouterKeyInfoUiState> = _openRouterKeyInfoState.asStateFlow()

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
            repository.update {
                it.copy(
                    vlmTransportMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK,
                    openRouterKeyMode = OpenRouterKeyMode.USER_PROVIDED_KEY
                )
            }
        }
    }

    fun clearOpenRouterUserKey() {
        viewModelScope.launch(Dispatchers.IO) {
            openRouterUserKeyStore.clearKey()
            _hasOpenRouterUserKey.value = false
            repository.update {
                it.copy(
                    vlmTransportMode = VlmTransportMode.BACKEND_MANAGED,
                    openRouterKeyMode = OpenRouterKeyMode.EMBEDDED_APP_KEY
                )
            }
        }
    }

    fun selectBackendManagedTransport() {
        viewModelScope.launch {
            repository.update { it.copy(vlmTransportMode = VlmTransportMode.BACKEND_MANAGED) }
        }
    }

    fun selectDirectByokTransport() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!openRouterUserKeyStore.hasKey()) return@launch
            repository.update {
                it.copy(
                    vlmTransportMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK,
                    openRouterKeyMode = OpenRouterKeyMode.USER_PROVIDED_KEY
                )
            }
        }
    }

    fun selectEmbeddedDebugTransport() {
        if (!hasEmbeddedOpenRouterAppKey) return
        viewModelScope.launch {
            repository.update {
                it.copy(
                    vlmTransportMode = VlmTransportMode.EMBEDDED_DEBUG,
                    openRouterKeyMode = OpenRouterKeyMode.EMBEDDED_APP_KEY
                )
            }
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            openRouterUserKeyStore.clearKey()
            _hasOpenRouterUserKey.value = false
            repository.resetToDefaults()
        }
    }

    fun loadOpenRouterKeyInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val selection = resolveVlmTransportSelection(
                settings = _settings.value,
                embeddedAppKey = embeddedOpenRouterApiKey,
                userProvidedKey = openRouterUserKeyStore.loadKey()
            )
            if (selection.activeMode == VlmTransportMode.BACKEND_MANAGED) {
                _openRouterKeyInfoState.value = OpenRouterKeyInfoUiState.Error(
                    sourceMode = selection.activeMode,
                    reason = OpenRouterKeyInfoErrorReason.BACKEND_MODE_ACTIVE
                )
                return@launch
            }
            if (!selection.hasUsableTransport) {
                _openRouterKeyInfoState.value = OpenRouterKeyInfoUiState.Error(
                    sourceMode = selection.activeMode,
                    reason = OpenRouterKeyInfoErrorReason.NO_ACTIVE_KEY
                )
                return@launch
            }

            _openRouterKeyInfoState.value = OpenRouterKeyInfoUiState.Loading(selection.activeMode)
            _openRouterKeyInfoState.value = try {
                val info = openRouterCurrentKeyInfoService.fetchCurrentKeyInfo(selection.apiKey.orEmpty())
                OpenRouterKeyInfoUiState.Success(
                    OpenRouterCurrentKeyInfoResult(
                        sourceMode = selection.activeMode,
                        info = info
                    )
                )
            } catch (exception: Exception) {
                OpenRouterKeyInfoUiState.Error(
                    sourceMode = selection.activeMode,
                    reason = when (exception) {
                        NoActiveKey -> OpenRouterKeyInfoErrorReason.NO_ACTIVE_KEY
                        Unauthorized -> OpenRouterKeyInfoErrorReason.UNAUTHORIZED
                        Network -> OpenRouterKeyInfoErrorReason.NETWORK
                        InvalidResponse -> OpenRouterKeyInfoErrorReason.INVALID_RESPONSE
                        else -> OpenRouterKeyInfoErrorReason.UNKNOWN
                    }
                )
            }
        }
    }

    private fun refreshOpenRouterUserKeyState() {
        viewModelScope.launch(Dispatchers.IO) {
            _hasOpenRouterUserKey.value = openRouterUserKeyStore.hasKey()
        }
    }

    class Factory(
        private val repository: SettingsRepository,
        private val openRouterUserKeyStore: OpenRouterUserKeyStore,
        private val embeddedOpenRouterApiKey: String,
        private val openRouterCurrentKeyInfoService: OpenRouterCurrentKeyInfoService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    repository = repository,
                    openRouterUserKeyStore = openRouterUserKeyStore,
                    embeddedOpenRouterApiKey = embeddedOpenRouterApiKey,
                    openRouterCurrentKeyInfoService = openRouterCurrentKeyInfoService
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
