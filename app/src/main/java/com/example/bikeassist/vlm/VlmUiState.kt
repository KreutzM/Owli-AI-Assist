package com.example.bikeassist.vlm

sealed class VlmUiState(open val snapshotBytes: ByteArray?) {
    object Inactive : VlmUiState(null)
    data class LoadingOverview(
        val message: String? = null,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
    data class Streaming(
        val partialText: String,
        val updatedAt: Long,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
    data class OverviewReady(
        val description: VlmSceneDescription,
        val updatedAt: Long,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
    data class OverviewReadyRaw(
        val rawText: String,
        val updatedAt: Long,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
    data class Asking(
        val current: VlmSceneDescription?,
        val question: String,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
    data class Error(
        val message: String,
        override val snapshotBytes: ByteArray? = null
    ) : VlmUiState(snapshotBytes)
}
