package com.owlitech.owli.assist.settings

data class OpenRouterCurrentKeyInfo(
    val label: String?,
    val limit: Double?,
    val limitRemaining: Double?,
    val limitReset: String?,
    val usage: Double?,
    val usageDaily: Double?,
    val usageWeekly: Double?,
    val usageMonthly: Double?,
    val isFreeTier: Boolean?,
    val expiresAt: String?
)

data class OpenRouterCurrentKeyInfoResult(
    val sourceMode: VlmTransportMode,
    val info: OpenRouterCurrentKeyInfo
)

sealed interface OpenRouterKeyInfoUiState {
    data object Idle : OpenRouterKeyInfoUiState
    data class Loading(val sourceMode: VlmTransportMode) : OpenRouterKeyInfoUiState
    data class Success(val result: OpenRouterCurrentKeyInfoResult) : OpenRouterKeyInfoUiState
    data class Error(
        val sourceMode: VlmTransportMode?,
        val reason: OpenRouterKeyInfoErrorReason
    ) : OpenRouterKeyInfoUiState
}

enum class OpenRouterKeyInfoErrorReason {
    BACKEND_MODE_ACTIVE,
    NO_ACTIVE_KEY,
    UNAUTHORIZED,
    NETWORK,
    INVALID_RESPONSE,
    UNKNOWN
}
