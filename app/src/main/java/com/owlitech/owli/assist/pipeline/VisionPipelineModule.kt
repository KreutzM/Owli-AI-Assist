package com.owlitech.owli.assist.pipeline

import android.content.Context
import android.content.res.AssetManager
import androidx.lifecycle.LifecycleOwner
import com.owlitech.owli.assist.blindview.BlindViewConfig
import com.owlitech.owli.assist.blindview.CocoLabelTranslator
import com.owlitech.owli.assist.blindview.LabelRepository
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.domain.DefaultSceneAnalyzer
import com.owlitech.owli.assist.ml.FakeDetector
import com.owlitech.owli.assist.ml.TfliteDetectorOptions
import com.owlitech.owli.assist.ml.TfliteTaskDetector
import com.owlitech.owli.assist.motion.MotionEstimator
import com.owlitech.owli.assist.processing.DefaultPreprocessor
import com.owlitech.owli.assist.processing.HsvTrafficLightPhaseClassifier
import com.owlitech.owli.assist.util.AppLogger
import kotlinx.coroutines.CoroutineScope

data class VisionPipelineHandle(
    val pipeline: VisionPipeline,
    val detectorInfo: String,
    val mode: AppMode,
    val snapshotProvider: SnapshotProvider? = null
)

object VisionPipelineModule {
    fun create(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        cameraFrameSource: CameraFrameSource = CameraFrameSource(context, lifecycleOwner),
        useFake: Boolean = false,
        detectorOptions: TfliteDetectorOptions = TfliteDetectorOptions(),
        mode: AppMode = AppMode.BLINDVIEW,
        blindViewConfig: BlindViewConfig = BlindViewConfig(),
        analysisIntervalMs: Long = 250L,
        motionEstimator: MotionEstimator? = null,
        motionGatingEnabled: Boolean = true,
        motionSpeakIntervalMultiplierHigh: Float = 1.35f,
        enableImuDerotation: Boolean = false,
        stabilizationQualityMin: Float = 0.3f
    ): VisionPipelineHandle {
        val labels = LabelRepository().loadLabels(context)
        val translator = CocoLabelTranslator().also { it.validateAgainst(labels) }
        val preprocessor = DefaultPreprocessor(
            outputSize = 448,
            enableImuDerotation = enableImuDerotation,
            stabilizationQualityMin = stabilizationQualityMin
        )
        val trafficLightClassifier = HsvTrafficLightPhaseClassifier()
        val (detector, info) = if (!useFake && modelExists(context.assets, detectorOptions.modelPath)) {
            runCatching {
                TfliteTaskDetector(context, detectorOptions) to "RealDetector: ${detectorOptions.modelPath}"
            }.getOrElse { ex ->
                AppLogger.e(ex, "Falling back to FakeDetector")
                FakeDetector() to "Fallback: FakeDetector (detector init failed)"
            }
        } else {
            val reason = if (useFake) "Forced fake" else "Model missing: ${detectorOptions.modelPath}"
            FakeDetector() to "Fallback: FakeDetector ($reason)"
        }
        val analyzer = DefaultSceneAnalyzer(
            blindViewConfig = blindViewConfig,
            translator = translator,
            motionGatingEnabled = motionGatingEnabled,
            motionSpeakIntervalMultiplierHigh = motionSpeakIntervalMultiplierHigh
        )
        val pipeline = DefaultVisionPipeline(
            cameraFrameSource = cameraFrameSource,
            preprocessor = preprocessor,
            detector = detector,
            sceneAnalyzer = analyzer,
            trafficLightClassifier = trafficLightClassifier,
            motionEstimator = motionEstimator,
            scope = scope,
            minProcessIntervalMs = analysisIntervalMs
        )
        AppLogger.d("VisionPipelineModule created: $info mode=$mode")
        return VisionPipelineHandle(pipeline, info, mode, pipeline as? SnapshotProvider)
    }

    private fun modelExists(assets: AssetManager, path: String): Boolean {
        return try {
            val segments = path.split("/").toMutableList()
            if (segments.isEmpty()) return false
            val fileName = segments.removeAt(segments.lastIndex)
            val dir = segments.joinToString("/")
            assets.list(dir)?.contains(fileName) == true
        } catch (_: Exception) {
            false
        }
    }
}
