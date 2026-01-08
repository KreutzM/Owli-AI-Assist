package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.domain.TrafficLightObservation
import com.owlitech.owli.assist.ml.Detection
import com.owlitech.owli.assist.ui.components.CameraPreview
import com.owlitech.owli.assist.ui.theme.OwliTheme

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
    onStart: () -> Unit,
    onStop: () -> Unit,
    cameraFrameSource: CameraFrameSource,
    rotationDegrees: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CameraPreview(cameraFrameSource = cameraFrameSource, modifier = Modifier.fillMaxSize())
            if (showOverlay) {
                DetectionOverlay(
                    detections = detections,
                    showLabels = showLabels,
                    modifier = Modifier.fillMaxSize()
                )
                SceneOverlay(
                    hazardLevel = hazardLevel,
                    detectionsCount = detectionsCount,
                    message = sceneMessage,
                    rotationText = rotationDegrees?.let { "${it}deg" },
                    trafficLights = trafficLights,
                    blindViewPreview = blindViewPreview,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color(0x66000000))
                        .padding(8.dp)
                )
            }
        }
        ControlPanel(
            isRunning = isRunning,
            sceneMessage = sceneMessage,
            lastError = lastError,
            statusMessage = statusMessage,
            onStart = onStart,
            onStop = onStop,
            blindViewPreview = blindViewPreview,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun SceneOverlay(
    hazardLevel: String,
    detectionsCount: Int,
    message: String?,
    rotationText: String?,
    trafficLights: List<TrafficLightObservation>,
    blindViewPreview: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Hazard: $hazardLevel", color = Color.White)
        Text(text = "Detections: $detectionsCount", color = Color.White)
        rotationText?.let { Text(text = "Rot: $it", color = Color.White) }
        val primaryTl = trafficLights.maxByOrNull { it.confidence }
        primaryTl?.let {
            Text(text = "TL: ${it.phase} (conf=${"%.2f".format(it.confidence)})", color = Color.White)
        }
        message?.let {
            Text(text = it, color = Color.White)
        }
        blindViewPreview?.let {
            Text(text = "BV: $it", color = Color.White)
        }
    }
}

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    showLabels: Boolean,
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
            val left = detection.bbox.xMin * size.width
            val top = detection.bbox.yMin * size.height
            val right = detection.bbox.xMax * size.width
            val bottom = detection.bbox.yMax * size.height
            drawRect(
                color = Color.Red,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )
            if (showLabels) {
                val text = "${detection.label} ${"%.2f".format(detection.confidence)}"
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
    onStart: () -> Unit,
    onStop: () -> Unit,
    blindViewPreview: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart, enabled = !isRunning, modifier = Modifier.weight(1f)) {
                Text(text = "Start")
            }
            Button(onClick = onStop, enabled = isRunning, modifier = Modifier.weight(1f)) {
                Text(text = "Stop")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Status: ${if (isRunning) "Laeuft" else "Gestoppt"}")
        Text(text = "Detector: $statusMessage")
        sceneMessage?.let { Text(text = "Letzte Meldung: $it") }
        lastError?.let { Text(text = "Fehler: $it", color = MaterialTheme.colorScheme.error) }
        blindViewPreview?.let { Text(text = "BlindView: $it") }
    }
}

@Composable
fun PreviewControlPanel() {
    OwliTheme {
        ControlPanel(
            isRunning = true,
            sceneMessage = "Achtung, Person voraus",
            lastError = null,
            statusMessage = "Preview (FakeDetector)",
            onStart = {},
            onStop = {},
            blindViewPreview = "2 Personen, 11 Uhr."
        )
    }
}
