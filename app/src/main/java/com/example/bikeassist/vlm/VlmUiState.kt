package com.example.bikeassist.vlm

sealed class VlmUiState {
    object Inactive : VlmUiState()
    data class LoadingOverview(val message: String? = null) : VlmUiState()
    data class OverviewReady(val description: VlmSceneDescription, val updatedAt: Long) : VlmUiState()
    data class Asking(val current: VlmSceneDescription?, val question: String) : VlmUiState()
    data class Error(val message: String) : VlmUiState()
}
