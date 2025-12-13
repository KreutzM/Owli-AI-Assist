package com.example.bikeassist.pipeline

import com.example.bikeassist.domain.SceneState
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

/**
 * Orchestriert Kamera, ML und Analyse und liefert SceneStates als Flow.
 */
interface VisionPipeline : Closeable {
    val sceneStates: Flow<SceneState>

    fun start()

    fun stop()
}
