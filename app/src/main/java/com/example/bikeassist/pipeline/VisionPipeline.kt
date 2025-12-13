package com.example.bikeassist.pipeline

import com.example.bikeassist.domain.SceneState
import kotlinx.coroutines.flow.Flow

/**
 * Orchestriert Kamera, ML und Analyse und liefert SceneStates als Flow.
 */
interface VisionPipeline {
    val sceneStates: Flow<SceneState>

    fun start()

    fun stop()
}
