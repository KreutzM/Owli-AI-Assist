package com.example.bikeassist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.pipeline.VisionPipeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val visionPipeline: VisionPipeline,
    detectorInfo: String = ""
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _sceneState = MutableStateFlow<SceneState?>(null)
    val sceneState: StateFlow<SceneState?> = _sceneState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _status = MutableStateFlow(detectorInfo)
    val status: StateFlow<String> = _status

    private var collectJob: Job? = null

    fun start() {
        if (_isRunning.value) return
        runCatching { visionPipeline.start() }
            .onSuccess {
                collectJob = viewModelScope.launch {
                    visionPipeline.sceneStates.collect { state ->
                        _sceneState.value = state
                    }
                }
                _isRunning.value = true
            }
            .onFailure { _lastError.value = it.message }
    }

    fun stop() {
        if (!_isRunning.value) return
        collectJob?.cancel()
        collectJob = null
        runCatching { visionPipeline.stop() }
        _sceneState.value = null
        _isRunning.value = false
    }

    override fun onCleared() {
        stop()
        runCatching { visionPipeline.close() }
        super.onCleared()
    }
}
