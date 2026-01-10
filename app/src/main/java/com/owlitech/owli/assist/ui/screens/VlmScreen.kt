package com.owlitech.owli.assist.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.vlm.VlmUiState
import com.owlitech.owli.assist.ui.components.CameraPreview
import com.owlitech.owli.assist.ui.overlay.CameraOverlayDefaults
import com.owlitech.owli.assist.ui.overlay.CameraOverlayLabel
import com.owlitech.owli.assist.ui.overlay.CameraOverlayRow
import com.owlitech.owli.assist.ui.overlay.CameraOverlayScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VlmScreen(
    state: VlmUiState,
    onNewScene: () -> Unit,
    onAsk: (String) -> Unit,
    cameraFrameSource: CameraFrameSource,
    autoScanAvailable: Boolean,
    isAutoScanRunning: Boolean,
    onStartAutoScan: () -> Unit,
    onStopAutoScan: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isBusy = state is VlmUiState.LoadingOverview || state is VlmUiState.Asking
    var question by remember { mutableStateOf("") }
    var backgroundBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val dimFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToScale(0.5f, 0.5f, 0.5f, 1f) }
        )
    }
    LaunchedEffect(state.snapshotBytes) {
        backgroundBitmap = null
        val bytes = state.snapshotBytes
        if (bytes != null) {
            val decoded = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            backgroundBitmap = decoded?.asImageBitmap()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            cameraFrameSource = cameraFrameSource,
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
        )
        backgroundBitmap?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = dimFilter
            )
        }
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onNewScene, enabled = !isBusy) { Text("Neue Szene") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartAutoScan,
                        enabled = autoScanAvailable && !isAutoScanRunning
                    ) {
                        Text("Start")
                    }
                    Button(
                        onClick = onStopAutoScan,
                        enabled = isAutoScanRunning
                    ) {
                        Text("Stop")
                    }
                }
            }
            CameraOverlayScope {
                when (state) {
                    is VlmUiState.Inactive -> {
                        CameraOverlayLabel("Bereit. Tippe auf 'Neue Szene'.")
                    }
                    is VlmUiState.LoadingOverview -> {
                        CameraOverlayRow {
                            CircularProgressIndicator(color = CameraOverlayDefaults.textColor)
                            Text(state.message ?: "Lade VLM...")
                        }
                    }
                    is VlmUiState.Asking -> {
                        CameraOverlayRow {
                            CircularProgressIndicator(color = CameraOverlayDefaults.textColor)
                            Text("Sende Frage...")
                        }
                    }
                    is VlmUiState.Streaming -> {
                        CameraOverlayLabel(text = "Streaming...")
                        CameraOverlayLabel(text = state.partialText, maxLines = 6)
                    }
                    is VlmUiState.Error -> {
                        CameraOverlayLabel(text = "Fehler: ${state.message}")
                    }
                    is VlmUiState.OverviewReadyRaw -> {
                        CameraOverlayLabel(text = "Antwort:")
                        CameraOverlayLabel(text = state.rawText, maxLines = 8)
                    }
                    is VlmUiState.OverviewReady -> {
                        val desc = state.description
                        val obstaclesText = if (desc.obstacles.isEmpty()) "keine" else desc.obstacles.joinToString()
                        val landmarksText = if (desc.landmarks.isEmpty()) "keine" else desc.landmarks.joinToString()
                        CameraOverlayLabel(text = "Kurz: ${desc.ttsOneLiner}", maxLines = 3)
                        CameraOverlayLabel(text = "Empfehlung: ${desc.actionSuggestion}", maxLines = 3)
                        CameraOverlayLabel(text = "Hindernisse: $obstaclesText", maxLines = 3)
                        CameraOverlayLabel(text = "Landmarken: $landmarksText", maxLines = 3)
                        CameraOverlayLabel(text = "Details: ${desc.readableText}", maxLines = 8)
                        desc.overallConfidence?.let { CameraOverlayLabel(text = "Confidence: $it") }
                    }
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Frage stellen") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            )
            Button(
                onClick = {
                    val text = question.trim()
                    if (text.isNotEmpty()) {
                        onAsk(text)
                        question = ""
                    }
                },
                enabled = !isBusy && question.isNotBlank()
            ) {
                Text("Senden")
            }
        }
    }
}
