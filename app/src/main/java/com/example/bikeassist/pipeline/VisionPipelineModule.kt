package com.example.bikeassist.pipeline

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.DefaultSceneAnalyzer
import com.example.bikeassist.ml.FakeDetector
import com.example.bikeassist.processing.FakePreprocessor
import kotlinx.coroutines.CoroutineScope

object VisionPipelineModule {
    fun create(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        cameraFrameSource: CameraFrameSource = CameraFrameSource(context, lifecycleOwner)
    ): VisionPipeline {
        val preprocessor = FakePreprocessor()
        val detector = FakeDetector()
        val analyzer = DefaultSceneAnalyzer()
        return DefaultVisionPipeline(
            cameraFrameSource = cameraFrameSource,
            preprocessor = preprocessor,
            detector = detector,
            sceneAnalyzer = analyzer,
            scope = scope
        )
    }
}
