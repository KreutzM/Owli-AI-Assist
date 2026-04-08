package com.owlitech.owli.assist.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.owlitech.owli.assist.util.AppLogger
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
    @Volatile
    var lastRotationDegrees: Int = 0

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService? = null
    private var pendingSurfaceProvider: Preview.SurfaceProvider? = null

    fun start(cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
        AppLogger.d("CameraFrameSource start")
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
                                    // CameraFrameSource owns analyzer frames end-to-end and closes
                                    // them exactly once after the listener has finished reading them.
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
        AppLogger.d("CameraFrameSource stop")
        cameraProvider?.unbindAll()
        preview = null
        imageAnalysis = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }

    fun bindPreviewSurface(surfaceProvider: Preview.SurfaceProvider) {
        AppLogger.d("CameraFrameSource bindPreviewSurface")
        pendingSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }
}
