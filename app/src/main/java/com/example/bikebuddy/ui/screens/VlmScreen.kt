package com.example.bikebuddy.ui.screens

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.bikeassist.vlm.VlmUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VlmScreen(
    state: VlmUiState,
    onNewScene: () -> Unit,
    onAsk: (String) -> Unit
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
            Button(onClick = onNewScene, enabled = !isBusy) { Text("Neue Szene") }
            when (state) {
                is VlmUiState.Inactive -> {
                    Text("Bereit. Tippe auf 'Neue Szene'.")
                }
                is VlmUiState.LoadingOverview -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(state.message ?: "Lade VLM...")
                    }
                }
                is VlmUiState.Asking -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Sende Frage...")
                    }
                }
                is VlmUiState.Streaming -> {
                    Text(text = "Streaming...")
                    Text(text = state.partialText)
                }
                is VlmUiState.Error -> {
                    Text(text = "Fehler: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is VlmUiState.OverviewReadyRaw -> {
                    Text(text = "Antwort:")
                    Text(text = state.rawText)
                }
                is VlmUiState.OverviewReady -> {
                    val desc = state.description
                    val obstaclesText = if (desc.obstacles.isEmpty()) "keine" else desc.obstacles.joinToString()
                    val landmarksText = if (desc.landmarks.isEmpty()) "keine" else desc.landmarks.joinToString()
                    Text(text = "Kurz: ${desc.ttsOneLiner}")
                    Text(text = "Empfehlung: ${desc.actionSuggestion}")
                    Text(text = "Hindernisse: $obstaclesText")
                    Text(text = "Landmarken: $landmarksText")
                    Text(text = "Details: ${desc.readableText}")
                    desc.overallConfidence?.let { Text(text = "Confidence: $it") }
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
