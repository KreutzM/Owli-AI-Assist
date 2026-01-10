package com.owlitech.owli.assist.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.vlm.VlmUiState
import com.owlitech.owli.assist.ui.components.CameraPreview
import com.owlitech.owli.assist.ui.overlay.CameraOverlayDefaults
import com.owlitech.owli.assist.ui.overlay.CameraOverlayLabel
import com.owlitech.owli.assist.ui.overlay.CameraOverlayRow
import com.owlitech.owli.assist.ui.overlay.CameraOverlayScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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
    var isListening by remember { mutableStateOf(false) }
    var isProcessingSpeech by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val dimFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToScale(0.85f, 0.85f, 0.85f, 1f) }
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
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        isProcessingSpeech = true
        val data = result.data
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isNotBlank()) {
            val current = question.trim()
            question = if (current.isBlank()) spoken else "$current $spoken"
            speechError = null
        } else {
            speechError = "Keine Sprache erkannt"
        }
        coroutineScope.launch {
            delay(1200)
            isProcessingSpeech = false
            speechError = null
        }
    }
    val sendQuestion = {
        val text = question.trim()
        if (text.isNotEmpty() && !isBusy) {
            onAsk(text)
            question = ""
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (autoScanAvailable) {
                    val autoLabel = if (isAutoScanRunning) "Auto an" else "Auto aus"
                    FilterChip(
                        selected = isAutoScanRunning,
                        onClick = { if (isAutoScanRunning) onStopAutoScan() else onStartAutoScan() },
                        enabled = autoScanAvailable,
                        label = { Text("Auto") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Autorenew,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = autoLabel }
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isBusy) {
                            if (isAutoScanRunning) {
                                onStopAutoScan()
                            }
                            onNewScene()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                    },
                    text = { Text("Neue Szene") },
                    expanded = true,
                    modifier = Modifier.semantics {
                        contentDescription = "Neue Szene"
                        if (isBusy) {
                            disabled()
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    speechError = null
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spracheingabe")
                                        }
                                        isListening = true
                                        speechLauncher.launch(intent)
                                    } catch (ex: ActivityNotFoundException) {
                                        isListening = false
                                        speechError = "Spracherkennung nicht verfuegbar"
                                    }
                                },
                                enabled = !isBusy && !isListening,
                                modifier = Modifier.semantics {
                                    contentDescription = if (isListening) {
                                        "Spracheingabe laeuft"
                                    } else {
                                        "Spracheingabe starten"
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                            }
                            OutlinedTextField(
                                value = question,
                                onValueChange = { question = it },
                                label = { Text("Frage stellen") },
                                modifier = Modifier.weight(1f),
                                enabled = !isBusy,
                                maxLines = 34,
                                minLines = 1,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = { sendQuestion() },
                                    onDone = { sendQuestion() }
                                )
                            )
                            IconButton(
                                onClick = { sendQuestion() },
                                modifier = Modifier.semantics { contentDescription = "Nachricht senden" },
                                enabled = !isBusy && question.isNotBlank()
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            }
                        }
                        val statusText = when {
                            isListening -> "Hoere zu..."
                            isProcessingSpeech -> "Verarbeite..."
                            else -> speechError
                        }
                        if (statusText != null) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
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
                    .padding(innerPadding)
                    .padding(12.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

            }
        }
    }
}
