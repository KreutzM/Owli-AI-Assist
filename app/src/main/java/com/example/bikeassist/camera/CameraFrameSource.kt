package com.example.bikeassist.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Verantwortlich für CameraX-Setup und liefert Frames an einen FrameListener.
 */
class CameraFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    var frameListener: FrameListener? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService? = null
    private var pendingSurfaceProvider: Preview.SurfaceProvider? = null

    fun start(cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                if (analysisExecutor == null || analysisExecutor?.isShutdown == true) {
                    analysisExecutor = Executors.newSingleThreadExecutor()
                }

                preview = Preview.Builder().build()
                pendingSurfaceProvider?.let { surfaceProvider ->
                    preview?.setSurfaceProvider(surfaceProvider)
                }

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        val executor = analysisExecutor
                        if (executor != null) {
                            setAnalyzer(executor) { image ->
                                try {
                                    frameListener?.onFrame(image)
                                } finally {
                                    image.close()
                                }
                            }
                        }
                    }

                runCatching { provider.unbindAll() }
                val useCases = listOfNotNull(preview, imageAnalysis)
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun stop() {
        cameraProvider?.unbindAll()
        preview = null
        imageAnalysis = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }

    fun bindPreviewSurface(surfaceProvider: Preview.SurfaceProvider) {
        pendingSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }
}
