package com.example.bikeassist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.pipeline.VisionPipelineHandle
import com.example.bikeassist.pipeline.VisionPipeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    detectorInfo: String = ""
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _shouldAutoStart = MutableStateFlow(false)
    val shouldAutoStart: StateFlow<Boolean> = _shouldAutoStart

    private val _sceneState = MutableStateFlow<SceneState?>(null)
    val sceneState: StateFlow<SceneState?> = _sceneState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _status = MutableStateFlow(detectorInfo)
    val status: StateFlow<String> = _status

    private var collectJob: Job? = null
    private var pipeline: VisionPipeline? = null
    private var detectorInfo: String = detectorInfo

    fun setPipeline(handle: VisionPipelineHandle) {
        // stop old pipeline if running
        val wasRunning = _isRunning.value
        stopInternal(resetAutoStart = false)
        pipeline?.close()
        pipeline = handle.pipeline
        detectorInfo = handle.detectorInfo
        _status.value = handle.detectorInfo
        com.example.bikeassist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
            isRunning = _isRunning.value,
            detectorInfo = handle.detectorInfo,
            analysisIntervalMs = 0L
        )
        if (wasRunning) {
            start()
        }
    }

    fun requestStart() {
        _shouldAutoStart.value = true
    }

    fun autoStartIfNeeded() {
        if (_shouldAutoStart.value) {
            start()
        }
    }

    fun start() {
        if (_isRunning.value) return
        val current = pipeline ?: return
        runCatching { current.start() }
            .onSuccess {
                collectJob = viewModelScope.launch {
                    current.sceneStates.collect { state ->
                        _sceneState.value = state
                        com.example.bikeassist.diagnostics.DiagnosticsCollector.updateSceneSnapshot(
                            detections = state.detections.size,
                            topLabels = state.detections.groupBy { it.label }.entries.sortedByDescending { it.value.size }.map { it.key },
                            preview = state.blindViewUtterancePreview
                        )
                    }
                }
                _isRunning.value = true
                com.example.bikeassist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
                    isRunning = true,
                    detectorInfo = detectorInfo,
                    analysisIntervalMs = 0L
                )
            }
            .onFailure { _lastError.value = it.message }
    }

    fun stopUser() {
        _shouldAutoStart.value = false
        stopInternal(resetAutoStart = false)
    }

    fun stopForLifecycle() {
        stopInternal(resetAutoStart = false)
    }

    private fun stopInternal(resetAutoStart: Boolean = true) {
        if (resetAutoStart) {
            _shouldAutoStart.value = false
        }
        if (!_isRunning.value) return
        collectJob?.cancel()
        collectJob = null
        pipeline?.let { runCatching { it.stop() } }
        _sceneState.value = null
        _isRunning.value = false
        com.example.bikeassist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
            isRunning = false,
            detectorInfo = detectorInfo,
            analysisIntervalMs = 0L
        )
    }

    override fun onCleared() {
        stopInternal(resetAutoStart = false)
        pipeline?.let { runCatching { it.close() } }
        super.onCleared()
    }
}
