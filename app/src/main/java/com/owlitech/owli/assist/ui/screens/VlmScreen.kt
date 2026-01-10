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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.owlitech.owli.assist.R
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VlmScreen(
    state: VlmUiState,
    onNewScene: () -> Unit,
    onAsk: (String) -> Unit,
    onVoiceInputActiveChanged: (Boolean) -> Unit,
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
    var autoSendOnVoiceResult by rememberSaveable { mutableStateOf(false) }
    var pendingAutoSendText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var composerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
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
    val sendQuestion = {
        val text = question.trim()
        if (text.isNotEmpty() && !isBusy) {
            onAsk(text)
            question = ""
        }
    }
    val nothingRecognizedText = stringResource(R.string.vlm_snackbar_nothing_recognized)
    val pleaseWaitText = stringResource(R.string.vlm_snackbar_please_wait)
    val sentText = stringResource(R.string.vlm_snackbar_sent)
    val undoText = stringResource(R.string.vlm_snackbar_undo)
    val noSpeechText = stringResource(R.string.vlm_speech_no_speech)
    val speechNotAvailableText = stringResource(R.string.vlm_speech_not_available)
    val voicePrompt = stringResource(R.string.vlm_voice_prompt)
    val newSceneLabel = stringResource(R.string.vlm_action_new_scene)
    val longPressSendLabel = stringResource(R.string.vlm_voice_long_press_send)
    val sendMessageLabel = stringResource(R.string.vlm_send_message)
    val voiceInputLabel = stringResource(R.string.vlm_voice_input)
    val voiceListeningLabel = stringResource(R.string.vlm_voice_listening)
    val voiceAutoSendLabel = stringResource(R.string.vlm_voice_listening_auto_send)
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        isProcessingSpeech = true
        val data = result.data
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        val trimmed = spoken.trim()
        if (autoSendOnVoiceResult) {
            autoSendOnVoiceResult = false
            if (trimmed.length < 2) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = nothingRecognizedText)
                }
            } else if (isBusy) {
                val current = question.trim()
                question = if (current.isBlank()) trimmed else "$current $trimmed"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = pleaseWaitText)
                }
            } else {
                pendingAutoSendText = trimmed
                question = trimmed
                sendQuestion()
                coroutineScope.launch {
                    val resultAction = snackbarHostState.showSnackbar(
                        message = sentText,
                        actionLabel = undoText
                    )
                    if (resultAction == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        question = pendingAutoSendText.orEmpty()
                    }
                    pendingAutoSendText = null
                }
                speechError = null
            }
        } else {
            if (trimmed.isNotBlank()) {
                val current = question.trim()
                question = if (current.isBlank()) trimmed else "$current $trimmed"
                speechError = null
            } else {
                speechError = noSpeechText
            }
        }
        coroutineScope.launch {
            delay(1200)
            isProcessingSpeech = false
            speechError = null
            onVoiceInputActiveChanged(false)
        }
    }
    val startVoiceIntent = { autoSend: Boolean ->
        speechError = null
        autoSendOnVoiceResult = autoSend
        try {
            onVoiceInputActiveChanged(true)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
            }
            isListening = true
            speechLauncher.launch(intent)
        } catch (ex: ActivityNotFoundException) {
            isListening = false
            autoSendOnVoiceResult = false
            onVoiceInputActiveChanged(false)
            speechError = speechNotAvailableText
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
                .padding(bottom = with(density) { composerHeightPx.toDp() })
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CameraOverlayScope {
                when (state) {
                    is VlmUiState.Inactive -> {
                        CameraOverlayLabel(
                            stringResource(R.string.vlm_state_ready_format, newSceneLabel)
                        )
                    }
                    is VlmUiState.LoadingOverview -> {
                        CameraOverlayRow {
                            CircularProgressIndicator(color = CameraOverlayDefaults.textColor)
                            Text(state.message ?: stringResource(R.string.vlm_state_loading))
                        }
                    }
                    is VlmUiState.Asking -> {
                        CameraOverlayRow {
                            CircularProgressIndicator(color = CameraOverlayDefaults.textColor)
                            Text(stringResource(R.string.vlm_state_sending_question))
                        }
                    }
                    is VlmUiState.Streaming -> {
                        CameraOverlayLabel(text = stringResource(R.string.vlm_state_streaming))
                        CameraOverlayLabel(text = state.partialText, maxLines = 6)
                    }
                    is VlmUiState.Error -> {
                        CameraOverlayLabel(
                            text = stringResource(R.string.vlm_state_error_format, state.message)
                        )
                    }
                    is VlmUiState.OverviewReadyRaw -> {
                        CameraOverlayLabel(text = stringResource(R.string.vlm_state_answer))
                        CameraOverlayLabel(text = state.rawText, maxLines = 8)
                    }
                    is VlmUiState.OverviewReady -> {
                        val desc = state.description
                        val noneText = stringResource(R.string.vlm_state_none)
                        val obstaclesText = if (desc.obstacles.isEmpty()) {
                            noneText
                        } else {
                            desc.obstacles.joinToString()
                        }
                        val landmarksText = if (desc.landmarks.isEmpty()) {
                            noneText
                        } else {
                            desc.landmarks.joinToString()
                        }
                        CameraOverlayLabel(
                            text = stringResource(R.string.vlm_state_brief_format, desc.ttsOneLiner),
                            maxLines = 3
                        )
                        CameraOverlayLabel(
                            text = stringResource(
                                R.string.vlm_state_recommendation_format,
                                desc.actionSuggestion
                            ),
                            maxLines = 3
                        )
                        CameraOverlayLabel(
                            text = stringResource(R.string.vlm_state_obstacles_format, obstaclesText),
                            maxLines = 3
                        )
                        CameraOverlayLabel(
                            text = stringResource(R.string.vlm_state_landmarks_format, landmarksText),
                            maxLines = 3
                        )
                        CameraOverlayLabel(
                            text = stringResource(R.string.vlm_state_details_format, desc.readableText),
                            maxLines = 8
                        )
                        desc.overallConfidence?.let {
                            CameraOverlayLabel(
                                text = stringResource(R.string.vlm_state_confidence_format, it)
                            )
                        }
                    }
                }
            }

        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (autoScanAvailable) {
                    val autoLabel = if (isAutoScanRunning) {
                        stringResource(R.string.vlm_auto_on)
                    } else {
                        stringResource(R.string.vlm_auto_off)
                    }
                    FilterChip(
                        selected = isAutoScanRunning,
                        onClick = { if (isAutoScanRunning) onStopAutoScan() else onStartAutoScan() },
                        enabled = autoScanAvailable,
                        label = { Text(stringResource(R.string.vlm_auto_label)) },
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
                    text = { Text(newSceneLabel) },
                    expanded = true,
                    modifier = Modifier.semantics {
                        contentDescription = newSceneLabel
                        if (isBusy) {
                            disabled()
                        }
                    }
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { composerHeightPx = it.height }
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
                        val micEnabled = !isBusy && !isListening
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .combinedClickable(
                                    enabled = micEnabled,
                                    onClick = { startVoiceIntent(false) },
                                    onLongClick = { startVoiceIntent(true) },
                                    onLongClickLabel = longPressSendLabel
                                )
                                .semantics {
                                    contentDescription = when {
                                        isListening -> voiceListeningLabel
                                        autoSendOnVoiceResult -> voiceAutoSendLabel
                                        else -> voiceInputLabel
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                        }
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = { Text(stringResource(R.string.vlm_question_label)) },
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
                            modifier = Modifier.semantics {
                                contentDescription = sendMessageLabel
                            },
                            enabled = !isBusy && question.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        }
                    }
                    if (!isListening) {
                        Text(
                            text = stringResource(R.string.vlm_voice_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
                        )
                    }
                    val statusText = when {
                        isListening -> stringResource(R.string.vlm_voice_status_listening)
                        isProcessingSpeech -> stringResource(R.string.vlm_voice_status_processing)
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
}
