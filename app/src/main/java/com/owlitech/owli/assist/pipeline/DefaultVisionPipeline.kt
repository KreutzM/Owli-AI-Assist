package com.owlitech.owli.assist.pipeline

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.scale
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.camera.FrameListener
import com.owlitech.owli.assist.domain.SceneAnalyzer
import com.owlitech.owli.assist.domain.SceneState
import com.owlitech.owli.assist.ml.Detector
import com.owlitech.owli.assist.motion.MotionEstimator
import com.owlitech.owli.assist.processing.Preprocessor
import com.owlitech.owli.assist.processing.TrafficLightPhaseClassifier
import com.owlitech.owli.assist.util.AppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
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
    private val motionEstimator: MotionEstimator? = null,
    private val debugDetectorViewEnabled: Boolean = false,
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
    private val latestBitmapUpdatedAt = AtomicLong(0L)
    private val snapshotMutex = Mutex()
    private var lastDebugDetectorAt: Long = 0L
    private var lastDebugDetectorBitmap: Bitmap? = null

    private val frameListener = object : FrameListener {
        override fun onFrame(image: ImageProxy) {
            val now = System.currentTimeMillis()
            if (processing || now - lastProcessedAt < minProcessIntervalMs) {
                return
            }
            processing = true
            try {
                val tsNs = image.imageInfo.timestamp
                val motion = motionEstimator?.getSnapshot(tsNs)
                com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateMotion(motion)
                cameraFrameSource.lastRotationDegrees = image.imageInfo.rotationDegrees
                val preprocessResult = preprocessor.preprocess(image, motion)
                val bitmap = preprocessResult.bitmap448
                latestBitmap.set(bitmap)
                latestBitmapUpdatedAt.set(now)
                val detections = detector.detect(bitmap)
                val trafficLights = trafficLightClassifier.classify(bitmap, detections)
                com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateStabilization(
                    appliedRollDeg = preprocessResult.appliedRollDeg,
                    mappingActive = preprocessResult.mapping != null
                )
                com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateTranslation(
                    dxLowRes = preprocessResult.translationDxLowRes,
                    dyLowRes = preprocessResult.translationDyLowRes,
                    quality = preprocessResult.translationQuality,
                    cropLeftPx = preprocessResult.cropLeftPx,
                    cropTopPx = preprocessResult.cropTopPx
                )
                val debugBitmap = updateDebugDetectorBitmap(preprocessResult.bitmap448, now)
                val sceneState = sceneAnalyzer.analyze(detections, trafficLights, motion)
                    .copy(
                        frameMapping = preprocessResult.mapping,
                        detectorDebugBitmap = debugBitmap
                    )
                com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateFrameProcessed(now)
                _sceneStates.tryEmit(sceneState)
                lastProcessedAt = now
            } finally {
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
        val source = latestBitmap.get()
        val updatedAt = latestBitmapUpdatedAt.get()
        val ageMs = if (updatedAt > 0L) System.currentTimeMillis() - updatedAt else -1L
        if (source == null) {
            AppLogger.d("VLM", "Snapshot requested but no bitmap available (ageMs=$ageMs)")
            return null
        }
        AppLogger.d("VLM", "Snapshot requested (ageMs=$ageMs, size=${source.width}x${source.height})")
        return compressJpeg(source, maxSidePx, quality)
    }

    override suspend fun requestFreshJpegSnapshot(
        maxSidePx: Int,
        quality: Int,
        timeoutMs: Long
    ): ByteArray? = snapshotMutex.withLock {
        if (running) {
            return@withLock getLatestJpegSnapshot(maxSidePx, quality)
        }
        val deferred = CompletableDeferred<ByteArray?>()
        val previousListener = cameraFrameSource.frameListener
        val oneShotListener = object : FrameListener {
            override fun onFrame(image: ImageProxy) {
                if (deferred.isCompleted) return
                try {
                    val now = System.currentTimeMillis()
                    cameraFrameSource.lastRotationDegrees = image.imageInfo.rotationDegrees
                    val preprocessResult = preprocessor.preprocess(image, null)
                    val bitmap = preprocessResult.bitmap448
                    latestBitmap.set(bitmap)
                    latestBitmapUpdatedAt.set(now)
                    deferred.complete(compressJpeg(bitmap, maxSidePx, quality))
                } catch (ex: Exception) {
                    deferred.complete(null)
                }
            }
        }
        withContext(Dispatchers.Main) {
            cameraFrameSource.frameListener = oneShotListener
            cameraFrameSource.start()
        }
        val result = withTimeoutOrNull(timeoutMs) { deferred.await() }
        withContext(Dispatchers.Main) {
            cameraFrameSource.frameListener = previousListener
            cameraFrameSource.stop()
        }
        return@withLock result
    }

    private fun compressJpeg(source: Bitmap, maxSidePx: Int, quality: Int): ByteArray? {
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
        return bitmap.scale(targetW, targetH, true)
    }

    private fun updateDebugDetectorBitmap(source: Bitmap, nowMs: Long): Bitmap? {
        if (!debugDetectorViewEnabled) {
            lastDebugDetectorBitmap = null
            lastDebugDetectorAt = 0L
            return null
        }
        if (nowMs - lastDebugDetectorAt >= DEBUG_DETECTOR_INTERVAL_MS || lastDebugDetectorBitmap == null) {
            lastDebugDetectorAt = nowMs
            lastDebugDetectorBitmap = createDetectorDebugBitmap(source)
        }
        return lastDebugDetectorBitmap
    }

    private fun createDetectorDebugBitmap(source: Bitmap): Bitmap {
        return if (source.width == DEBUG_DETECTOR_SIZE && source.height == DEBUG_DETECTOR_SIZE) {
            source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        } else {
            source.scale(DEBUG_DETECTOR_SIZE, DEBUG_DETECTOR_SIZE, true)
        }
    }

    companion object {
        private const val DEBUG_DETECTOR_INTERVAL_MS = 300L
        private const val DEBUG_DETECTOR_SIZE = 224
    }
}
