package com.owlitech.owli.assist.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.view.Surface

@Composable
fun VlmCameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(lifecycleOwner, cameraSelector, imageCapture) {
        var provider: ProcessCameraProvider? = null
        var isBound = false

        fun bindIfNeeded() {
            if (isBound) return
            val cameraProvider = provider ?: return
            val preview = Preview.Builder().build()
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            preview.targetRotation = rotation
            imageCapture?.targetRotation = rotation
            preview.setSurfaceProvider(previewView.surfaceProvider)
            runCatching { cameraProvider.unbindAll() }
            val useCases = listOfNotNull(preview, imageCapture)
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
            isBound = true
        }

        fun unbindIfNeeded() {
            if (!isBound) return
            runCatching { provider?.unbindAll() }
            isBound = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> bindIfNeeded()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> unbindIfNeeded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val listener = Runnable {
            provider = cameraProviderFuture.get()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                bindIfNeeded()
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unbindIfNeeded()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}
