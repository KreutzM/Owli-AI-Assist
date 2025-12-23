package com.example.bikeassist.pipeline

import android.content.Context
import android.content.res.AssetManager
import androidx.lifecycle.LifecycleOwner
import com.example.bikeassist.blindview.BlindViewConfig
import com.example.bikeassist.blindview.CocoLabelTranslator
import com.example.bikeassist.blindview.LabelRepository
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.DefaultSceneAnalyzer
import com.example.bikeassist.ml.FakeDetector
import com.example.bikeassist.ml.TfliteDetectorOptions
import com.example.bikeassist.ml.TfliteTaskDetector
import com.example.bikeassist.processing.DefaultPreprocessor
import com.example.bikeassist.processing.HsvTrafficLightPhaseClassifier
import com.example.bikeassist.util.AppLogger
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
        analysisIntervalMs: Long = 250L
    ): VisionPipelineHandle {
        val labels = LabelRepository().loadLabels(context)
        val translator = CocoLabelTranslator().also { it.validateAgainst(labels) }
        val preprocessor = DefaultPreprocessor()
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
            translator = translator
        )
        val pipeline = DefaultVisionPipeline(
            cameraFrameSource = cameraFrameSource,
            preprocessor = preprocessor,
            detector = detector,
            sceneAnalyzer = analyzer,
            trafficLightClassifier = trafficLightClassifier,
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
