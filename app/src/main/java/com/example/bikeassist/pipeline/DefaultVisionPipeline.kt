package com.example.bikeassist.pipeline

import androidx.camera.core.ImageProxy
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.camera.FrameListener
import com.example.bikeassist.domain.SceneAnalyzer
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.ml.Detector
import com.example.bikeassist.processing.Preprocessor
import com.example.bikeassist.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Default-Implementierung der VisionPipeline mit einfachem "latest wins"-Verhalten.
 */
class DefaultVisionPipeline(
    private val cameraFrameSource: CameraFrameSource,
    private val preprocessor: Preprocessor,
    private val detector: Detector,
    private val sceneAnalyzer: SceneAnalyzer,
    @Suppress("unused")
    private val scope: CoroutineScope
) : VisionPipeline {

    private val _sceneStates = MutableSharedFlow<SceneState>(replay = 0, extraBufferCapacity = 1)
    override val sceneStates: Flow<SceneState> = _sceneStates.asSharedFlow()

    @Volatile
    private var processing = false
    @Volatile
    private var running = false
    private var lastProcessedAt: Long = 0L
    private val minProcessIntervalMs: Long = 250L

    private val frameListener = object : FrameListener {
        override fun onFrame(image: ImageProxy) {
            val now = System.currentTimeMillis()
            if (processing || now - lastProcessedAt < minProcessIntervalMs) {
                image.close()
                return
            }
            processing = true
            try {
                cameraFrameSource.lastRotationDegrees = image.imageInfo.rotationDegrees
                val bitmap = preprocessor.preprocess(image)
                val detections = detector.detect(bitmap)
                val sceneState = sceneAnalyzer.analyze(detections)
                _sceneStates.tryEmit(sceneState)
                lastProcessedAt = now
            } finally {
                image.close()
                processing = false
            }
        }
    }

    override fun start() {
        if (running) return
        AppLogger.d("Pipeline start")
        runCatching { detector.warmup() }
            .onFailure { return }
        cameraFrameSource.frameListener = frameListener
        cameraFrameSource.start()
        running = true
    }

    override fun stop() {
        AppLogger.d("Pipeline stop")
        cameraFrameSource.frameListener = null
        cameraFrameSource.stop()
        running = false
        processing = false
    }

    override fun close() {
        runCatching { detector.close() }
    }
}
