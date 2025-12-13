package com.example.bikeassist.pipeline

import android.content.Context
import android.content.res.AssetManager
import androidx.lifecycle.LifecycleOwner
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.DefaultSceneAnalyzer
import com.example.bikeassist.ml.FakeDetector
import com.example.bikeassist.ml.TfliteDetectorOptions
import com.example.bikeassist.ml.TfliteTaskDetector
import com.example.bikeassist.processing.DefaultPreprocessor
import com.example.bikeassist.util.AppLogger
import kotlinx.coroutines.CoroutineScope

data class VisionPipelineHandle(
    val pipeline: VisionPipeline,
    val detectorInfo: String
)

object VisionPipelineModule {
    fun create(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        cameraFrameSource: CameraFrameSource = CameraFrameSource(context, lifecycleOwner),
        useFake: Boolean = false,
        detectorOptions: TfliteDetectorOptions = TfliteDetectorOptions()
    ): VisionPipelineHandle {
        val preprocessor = DefaultPreprocessor()
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
        val analyzer = DefaultSceneAnalyzer()
        val pipeline = DefaultVisionPipeline(
            cameraFrameSource = cameraFrameSource,
            preprocessor = preprocessor,
            detector = detector,
            sceneAnalyzer = analyzer,
            scope = scope
        )
        AppLogger.d("VisionPipelineModule created: $info")
        return VisionPipelineHandle(pipeline, info)
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
