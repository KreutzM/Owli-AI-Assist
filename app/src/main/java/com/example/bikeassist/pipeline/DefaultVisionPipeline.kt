package com.example.bikeassist.pipeline

import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.domain.SceneAnalyzer
import com.example.bikeassist.ml.Detector
import com.example.bikeassist.processing.Preprocessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Default-Implementierung der VisionPipeline. Enthält nur ein Skelett, Logik folgt später.
 */
class DefaultVisionPipeline(
    private val cameraFrameSource: CameraFrameSource,
    private val preprocessor: Preprocessor,
    private val detector: Detector,
    private val sceneAnalyzer: SceneAnalyzer,
    private val scope: CoroutineScope
) : VisionPipeline {

    private val _sceneStates = MutableSharedFlow<SceneState>(replay = 0, extraBufferCapacity = 1)
    override val sceneStates: Flow<SceneState> = _sceneStates

    override fun start() {
        // TODO: CameraFrameSource registrieren, Frames drosseln und sequentiell verarbeiten.
    }

    override fun stop() {
        // TODO: Ressourcen freigeben und CameraFrameSource stoppen.
    }
}
