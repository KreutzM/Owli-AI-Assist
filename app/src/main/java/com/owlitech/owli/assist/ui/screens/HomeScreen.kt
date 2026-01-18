package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.domain.TrafficLightObservation
import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.processing.FrameMapping
import com.owlitech.owli.assist.ui.components.CameraPreview
import com.owlitech.owli.assist.ui.overlay.CameraOverlayLabel
import com.owlitech.owli.assist.ui.overlay.CameraOverlayScope
import com.owlitech.owli.assist.ui.theme.OwliTheme
import androidx.compose.foundation.Image

@Composable
fun HomeScreen(
    isRunning: Boolean,
    sceneMessage: String?,
    detections: List<Detection>,
    lastError: String?,
    statusMessage: String,
    detectionsCount: Int,
    hazardLevel: String,
    trafficLights: List<TrafficLightObservation>,
    blindViewPreview: String?,
    showOverlay: Boolean,
    showLabels: Boolean,
    frameMapping: FrameMapping?,
    detectorDebugBitmap: android.graphics.Bitmap?,
    showDetectorDebugView: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    cameraFrameSource: CameraFrameSource,
    rotationDegrees: Int?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CameraPreview(cameraFrameSource = cameraFrameSource, modifier = Modifier.fillMaxSize())
                if (showOverlay) {
                    CameraOverlayScope {
                        DetectionOverlay(
                            detections = detections,
                            showLabels = showLabels,
                            frameMapping = frameMapping,
                            modifier = Modifier.fillMaxSize()
                        )
                        SceneOverlay(
                            hazardLevel = hazardLevel,
                            detectionsCount = detectionsCount,
                            message = sceneMessage,
                            rotationDegrees = rotationDegrees,
                            trafficLights = trafficLights,
                            blindViewPreview = blindViewPreview,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    }
                }
                if (showDetectorDebugView && detectorDebugBitmap != null) {
                    DetectorDebugView(
                        bitmap = detectorDebugBitmap,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            }
            ControlPanel(
                isRunning = isRunning,
                sceneMessage = sceneMessage,
                lastError = lastError,
                statusMessage = statusMessage,
                blindViewPreview = blindViewPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        val isStart = !isRunning
        val startLabel = stringResource(R.string.action_start)
        val stopLabel = stringResource(R.string.action_stop)
        val actionLabel = if (isStart) startLabel else stopLabel
        ExtendedFloatingActionButton(
            onClick = { if (isStart) onStart() else onStop() },
            icon = {
                Icon(
                    imageVector = if (isStart) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                    contentDescription = null
                )
            },
            text = { Text(actionLabel) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .semantics { contentDescription = actionLabel }
        )
    }
}

@Composable
fun SceneOverlay(
    hazardLevel: String,
    detectionsCount: Int,
    message: String?,
    rotationDegrees: Int?,
    trafficLights: List<TrafficLightObservation>,
    blindViewPreview: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CameraOverlayLabel(text = stringResource(R.string.home_hazard_format, hazardLevel))
        CameraOverlayLabel(text = stringResource(R.string.home_detections_format, detectionsCount))
        rotationDegrees?.let { CameraOverlayLabel(text = stringResource(R.string.home_rotation_format, it)) }
        val primaryTl = trafficLights.maxByOrNull { it.confidence }
        primaryTl?.let {
            CameraOverlayLabel(
                text = stringResource(
                    R.string.home_traffic_light_format,
                    it.phase,
                    it.confidence
                )
            )
        }
        message?.let {
            CameraOverlayLabel(text = it, maxLines = 3)
        }
        blindViewPreview?.let {
            CameraOverlayLabel(text = stringResource(R.string.home_blindview_format, it), maxLines = 2)
        }
    }
}

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    showLabels: Boolean,
    frameMapping: FrameMapping?,
    modifier: Modifier = Modifier
) {
    val maxLabels = 10
    val minLabelConfidence = 0.30f
    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 40f
            style = android.graphics.Paint.Style.FILL
        }
    }
    val bgPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(160, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
    }
    val sorted = remember(detections) {
        detections.filter { it.confidence >= minLabelConfidence }
            .sortedByDescending { it.confidence }
            .take(maxLabels)
    }
    Canvas(modifier = modifier) {
        sorted.forEach { detection ->
            val left: Float
            val top: Float
            val right: Float
            val bottom: Float
            if (frameMapping != null) {
                val mapped = frameMapping.mapToPreviewRect(detection.bbox, size.width, size.height)
                left = mapped.left
                top = mapped.top
                right = mapped.right
                bottom = mapped.bottom
            } else {
                left = detection.bbox.xMin * size.width
                top = detection.bbox.yMin * size.height
                right = detection.bbox.xMax * size.width
                bottom = detection.bbox.yMax * size.height
            }
            drawRect(
                color = Color.Red,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )
            if (showLabels) {
                val text = "${detection.label} ${"%.2f".format(detection.confidence)}"
                bgPaint.color = android.graphics.Color.argb(160, 0, 0, 0)
                labelPaint.color = android.graphics.Color.WHITE
                val fm = labelPaint.fontMetrics
                val textWidth = labelPaint.measureText(text)
                val textHeight = fm.bottom - fm.top
                val padding = 8f
                var textLeft = left
                var textTop = top - padding
                if (textTop < padding) {
                    textTop = top + padding
                }
                if (textLeft + textWidth + padding > size.width) {
                    textLeft = size.width - textWidth - padding
                }
                val rectLeft = textLeft - padding
                val rectTop = textTop - padding
                val rectRight = textLeft + textWidth + padding
                val rectBottom = textTop + textHeight + padding
                drawContext.canvas.nativeCanvas.drawRect(
                    rectLeft,
                    rectTop,
                    rectRight,
                    rectBottom,
                    bgPaint
                )
                val textBaseline = textTop - fm.top
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    textLeft,
                    textBaseline,
                    labelPaint
                )
            }
        }
    }
}

@Composable
fun ControlPanel(
    isRunning: Boolean,
    sceneMessage: String?,
    lastError: String?,
    statusMessage: String,
    blindViewPreview: String?,
    modifier: Modifier = Modifier
) {
    val statusLabel = if (isRunning) {
        stringResource(R.string.home_status_running)
    } else {
        stringResource(R.string.home_status_stopped)
    }
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.home_status_format, statusLabel))
        Text(text = stringResource(R.string.home_detector_format, statusMessage))
        sceneMessage?.let { Text(text = stringResource(R.string.home_last_message_format, it)) }
        lastError?.let {
            Text(
                text = stringResource(R.string.home_error_format, it),
                color = MaterialTheme.colorScheme.error
            )
        }
        blindViewPreview?.let { Text(text = stringResource(R.string.home_blindview_preview_format, it)) }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
fun DetectorDebugView(
    bitmap: android.graphics.Bitmap,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Detector 448x448",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp)
        )
        CameraOverlayLabel(
            text = "Detector 448x448",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )
    }
}

@Composable
fun PreviewControlPanel() {
    OwliTheme {
        ControlPanel(
            isRunning = true,
            sceneMessage = stringResource(R.string.home_preview_scene_message),
            lastError = null,
            statusMessage = stringResource(R.string.home_preview_status_message),
            blindViewPreview = stringResource(R.string.home_preview_blindview)
        )
    }
}
