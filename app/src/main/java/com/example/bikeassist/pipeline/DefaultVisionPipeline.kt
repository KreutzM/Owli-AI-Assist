package com.example.bikeassist.pipeline

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.camera.FrameListener
import com.example.bikeassist.domain.SceneAnalyzer
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.ml.Detector
import com.example.bikeassist.processing.Preprocessor
import com.example.bikeassist.processing.TrafficLightPhaseClassifier
import com.example.bikeassist.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Default-Implementierung der VisionPipeline mit einfachem "latest wins"-Verhalten.
 */
class DefaultVisionPipeline(
    private val cameraFrameSource: CameraFrameSource,
    private val preprocessor: Preprocessor,
    private val detector: Detector,
    private val sceneAnalyzer: SceneAnalyzer,
    private val trafficLightClassifier: TrafficLightPhaseClassifier,
    @Suppress("unused")
    private val scope: CoroutineScope,
    private val minProcessIntervalMs: Long = 250L
) : VisionPipeline, SnapshotProvider {

    private val _sceneStates = MutableSharedFlow<SceneState>(replay = 0, extraBufferCapacity = 1)
    override val sceneStates: Flow<SceneState> = _sceneStates.asSharedFlow()

    @Volatile
    private var processing = false
    @Volatile
    private var running = false
    private var lastProcessedAt: Long = 0L
    private val latestBitmap = AtomicReference<Bitmap?>(null)

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
                latestBitmap.set(bitmap)
                val detections = detector.detect(bitmap)
                val trafficLights = trafficLightClassifier.classify(bitmap, detections)
                val sceneState = sceneAnalyzer.analyze(detections, trafficLights)
                com.example.bikeassist.diagnostics.DiagnosticsCollector.updateFrameProcessed(now)
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

    override fun getLatestJpegSnapshot(maxSidePx: Int, quality: Int): ByteArray? {
        val source = latestBitmap.get() ?: return null
        val safeMaxSide = maxSidePx.coerceAtLeast(256)
        val safeQuality = quality.coerceIn(30, 95)
        val scaled = scaleToMaxSide(source, safeMaxSide)
        return ByteArrayOutputStream().use { out ->
            val ok = scaled.compress(Bitmap.CompressFormat.JPEG, safeQuality, out)
            if (scaled !== source) {
                scaled.recycle()
            }
            if (!ok) return null
            out.toByteArray()
        }
    }

    private fun scaleToMaxSide(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxSidePx) return bitmap
        val scale = maxSidePx.toFloat() / maxSide.toFloat()
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
