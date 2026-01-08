package com.owlitech.owli.assist.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.owlitech.owli.assist.camera.CameraFrameSource

@Composable
fun CameraPreview(
    cameraFrameSource: CameraFrameSource,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).also { previewView ->
                cameraFrameSource.bindPreviewSurface(previewView.surfaceProvider)
            }
        },
        update = { previewView ->
            cameraFrameSource.bindPreviewSurface(previewView.surfaceProvider)
        }
    )
}
