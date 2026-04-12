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
    val sourceMode: OpenRouterKeyMode,
    val info: OpenRouterCurrentKeyInfo
)

sealed interface OpenRouterKeyInfoUiState {
    data object Idle : OpenRouterKeyInfoUiState
    data class Loading(val sourceMode: OpenRouterKeyMode) : OpenRouterKeyInfoUiState
    data class Success(val result: OpenRouterCurrentKeyInfoResult) : OpenRouterKeyInfoUiState
    data class Error(
        val sourceMode: OpenRouterKeyMode?,
        val reason: OpenRouterKeyInfoErrorReason
    ) : OpenRouterKeyInfoUiState
}

enum class OpenRouterKeyInfoErrorReason {
    NO_ACTIVE_KEY,
    UNAUTHORIZED,
    NETWORK,
    INVALID_RESPONSE,
    UNKNOWN
}
