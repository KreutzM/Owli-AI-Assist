package com.owlitech.owli.assist.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.speech.RecognizerIntent
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.media.ExifInterface
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.vlm.VlmAttachment
import com.owlitech.owli.assist.vlm.VlmUiState
import com.owlitech.owli.assist.ui.components.VlmCameraPreview
import com.owlitech.owli.assist.ui.overlay.CameraOverlayDefaults
import com.owlitech.owli.assist.ui.overlay.CameraOverlayLabel
import com.owlitech.owli.assist.ui.overlay.CameraOverlayRow
import com.owlitech.owli.assist.ui.overlay.CameraOverlayScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.ByteArrayInputStream

private enum class CaptureUiMode { Preview, Frozen }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VlmScreen(
    state: VlmUiState,
    onNewScene: (ByteArray) -> Unit,
    onAsk: (String) -> Unit,
    onRepeatLastResponse: (String?, String?) -> Unit,
    onAddImage: (ByteArray) -> Int?,
    attachments: List<VlmAttachment>,
    onRemoveAttachment: (String) -> Unit,
    lastImageBytes: ByteArray?,
    onReset: () -> Unit,
    onVoiceInputActiveChanged: (Boolean) -> Unit,
    autoScanAvailable: Boolean,
    isAutoScanRunning: Boolean,
    autoScanIntervalMs: Long,
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
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var lastSpeakable by remember { mutableStateOf<Pair<String?, String?>?>(null) }
    var attachmentsDialogVisible by remember { mutableStateOf(false) }
    var captureMode by rememberSaveable { mutableStateOf(CaptureUiMode.Preview) }
    var captureInFlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val dimFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToScale(0.85f, 0.85f, 0.85f, 1f) }
        )
    }
    val previewBytes = lastImageBytes ?: state.snapshotBytes
    LaunchedEffect(previewBytes) {
        backgroundBitmap = null
        val bytes = previewBytes
        if (bytes != null) {
            val decoded = withContext(Dispatchers.Default) {
                decodeJpegWithExif(bytes)
            }
            backgroundBitmap = decoded
        }
    }
    LaunchedEffect(state) {
        when (state) {
            is VlmUiState.OverviewReady -> {
                lastSpeakable = state.description.ttsOneLiner to state.description.actionSuggestion
            }
            is VlmUiState.OverviewReadyRaw -> {
                lastSpeakable = state.rawText to null
            }
            is VlmUiState.Asking -> {
                state.current?.let {
                    lastSpeakable = it.ttsOneLiner to it.actionSuggestion
                }
            }
            is VlmUiState.Inactive -> {
                lastSpeakable = null
                captureMode = CaptureUiMode.Preview
            }
            else -> Unit
        }
    }
    LaunchedEffect(isAutoScanRunning) {
        if (isAutoScanRunning && captureMode != CaptureUiMode.Preview) {
            captureMode = CaptureUiMode.Preview
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
    val addImageAddedText = stringResource(R.string.vlm_attachment_added)
    val addImageFailedText = stringResource(R.string.vlm_attachment_add_failed)
    val attachmentCountSingle = stringResource(R.string.vlm_attachment_count_single)
    val attachmentCountFormat = stringResource(R.string.vlm_attachment_count_format)
    val attachmentsManageLabel = stringResource(
        R.string.vlm_attachments_manage_format,
        attachments.size
    )
    val attachmentsTitle = stringResource(R.string.vlm_attachments_title)
    val attachmentsClose = stringResource(R.string.vlm_attachments_close)
    val attachmentsRemoveLabel = stringResource(R.string.vlm_attachment_remove)
    val attachmentsRemoveContentDescription = stringResource(R.string.vlm_attachment_remove_cd)
    val cameraPreviewLabel = stringResource(R.string.vlm_camera_preview)
    val voicePrompt = stringResource(R.string.vlm_voice_prompt)
    val newSceneLabel = stringResource(R.string.vlm_action_new_scene)
    val resetLabel = stringResource(R.string.vlm_action_reset)
    val longPressSendLabel = stringResource(R.string.vlm_voice_long_press_send)
    val sendMessageLabel = stringResource(R.string.vlm_send_message)
    val voiceInputLabel = stringResource(R.string.vlm_voice_input)
    val voiceListeningLabel = stringResource(R.string.vlm_voice_listening)
    val voiceAutoSendLabel = stringResource(R.string.vlm_voice_listening_auto_send)
    val moreActionsLabel = stringResource(R.string.vlm_more_actions)
    val repeatLastAnswerLabel = stringResource(R.string.vlm_repeat_last_answer)
    val addImageLabel = stringResource(R.string.vlm_add_image)
    val captureFailedText = stringResource(R.string.vlm_capture_failed)
    val canRepeatLastAnswer = lastSpeakable != null
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    val latestState by rememberUpdatedState(state)
    val latestCaptureMode by rememberUpdatedState(captureMode)
    val latestAutoScanRunning by rememberUpdatedState(isAutoScanRunning)
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
    val captureNewScene: (Boolean) -> Unit = newScene@{ freezeOnSuccess ->
        if (captureInFlight) return@newScene
        captureInFlight = true
        val file = File(context.cacheDir, "vlm_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    captureInFlight = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = captureFailedText)
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    coroutineScope.launch {
                        val bytes = runCatching {
                            withContext(Dispatchers.IO) { file.readBytes() }
                        }.getOrNull()
                        runCatching { file.delete() }
                        captureInFlight = false
                        if (bytes == null) {
                            snackbarHostState.showSnackbar(message = captureFailedText)
                            return@launch
                        }
                        onNewScene(bytes)
                        if (freezeOnSuccess) {
                            captureMode = CaptureUiMode.Frozen
                        }
                    }
                }
            }
        )
    }
    val captureAttachment = attachment@{
        if (captureInFlight) return@attachment
        captureInFlight = true
        val file = File(context.cacheDir, "vlm_attachment_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    captureInFlight = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = addImageFailedText)
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    coroutineScope.launch {
                        val bytes = runCatching {
                            withContext(Dispatchers.IO) { file.readBytes() }
                        }.getOrNull()
                        runCatching { file.delete() }
                        captureInFlight = false
                        if (bytes == null) {
                            snackbarHostState.showSnackbar(message = addImageFailedText)
                            return@launch
                        }
                        val count = onAddImage(bytes)
                        if (count == null) {
                            snackbarHostState.showSnackbar(message = addImageFailedText)
                            return@launch
                        }
                        val countLabel = if (count == 1) {
                            attachmentCountSingle
                        } else {
                            attachmentCountFormat.format(count)
                        }
                        snackbarHostState.showSnackbar(
                            message = "$addImageAddedText. $countLabel"
                        )
                    }
                }
            }
        )
    }
    LaunchedEffect(isAutoScanRunning, autoScanIntervalMs) {
        if (!isAutoScanRunning) return@LaunchedEffect
        while (isActive) {
            val busy = when (latestState) {
                is VlmUiState.LoadingOverview,
                is VlmUiState.Asking,
                is VlmUiState.Streaming -> true
                else -> false
            }
            if (latestAutoScanRunning && latestCaptureMode == CaptureUiMode.Preview && !busy) {
                captureNewScene(false)
            }
            delay(autoScanIntervalMs)
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
        val previewSemantics = if (captureMode == CaptureUiMode.Preview) {
            Modifier.semantics { contentDescription = cameraPreviewLabel }
        } else {
            Modifier
        }
        VlmCameraPreview(
            modifier = Modifier
                .matchParentSize()
                .then(previewSemantics),
            imageCapture = imageCapture
        )
        if (captureMode == CaptureUiMode.Frozen) {
            backgroundBitmap?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = cameraPreviewLabel,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = dimFilter
                )
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
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
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { actionsMenuExpanded = true },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = moreActionsLabel
                            )
                        }
                        DropdownMenu(
                            expanded = actionsMenuExpanded,
                            onDismissRequest = { actionsMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(repeatLastAnswerLabel) },
                                onClick = {
                                    actionsMenuExpanded = false
                                    lastSpeakable?.let { (primary, secondary) ->
                                        onRepeatLastResponse(primary, secondary)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Replay,
                                        contentDescription = null
                                    )
                                },
                                enabled = canRepeatLastAnswer
                            )
                            DropdownMenuItem(
                                text = { Text(addImageLabel) },
                                onClick = {
                                    actionsMenuExpanded = false
                                    captureAttachment()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (!isBusy) {
                                if (captureMode == CaptureUiMode.Preview) {
                                    if (isAutoScanRunning) {
                                        onStopAutoScan()
                                    }
                                    captureNewScene(true)
                                } else {
                                    onReset()
                                    captureMode = CaptureUiMode.Preview
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .sizeIn(minHeight = 48.dp)
                            .semantics {
                                contentDescription = if (captureMode == CaptureUiMode.Preview) {
                                    newSceneLabel
                                } else {
                                    resetLabel
                                }
                            }
                    ) {
                        Text(if (captureMode == CaptureUiMode.Preview) newSceneLabel else resetLabel)
                    }
                }
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
                    if (attachments.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { attachmentsDialogVisible = true },
                            modifier = Modifier
                                .sizeIn(minHeight = 48.dp)
                                .semantics { contentDescription = attachmentsManageLabel }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = attachments.size.toString())
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val micEnabled = !isBusy && !isListening
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
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = sendMessageLabel },
                            enabled = !isBusy && question.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        }
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .combinedClickable(
                                    enabled = micEnabled,
                                    onClick = { startVoiceIntent(false) },
                                    onLongClick = { startVoiceIntent(true) },
                                    onLongClickLabel = longPressSendLabel
                                )
                                .semantics {
                                    contentDescription = voiceInputLabel
                                    when {
                                        isListening -> stateDescription = voiceListeningLabel
                                        autoSendOnVoiceResult -> stateDescription = voiceAutoSendLabel
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                        }
                    }
                    if (!isListening) {
                        Text(
                            text = stringResource(R.string.vlm_voice_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp)
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
        if (attachmentsDialogVisible) {
            AlertDialog(
                onDismissRequest = { attachmentsDialogVisible = false },
                title = { Text(attachmentsTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        attachments.forEachIndexed { index, attachment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.vlm_attachment_item_format,
                                        index + 1
                                    )
                                )
                                TextButton(
                                    onClick = { onRemoveAttachment(attachment.id) },
                                    modifier = Modifier.semantics {
                                        contentDescription = attachmentsRemoveContentDescription
                                    }
                                ) {
                                    Text(attachmentsRemoveLabel)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { attachmentsDialogVisible = false }) {
                        Text(attachmentsClose)
                    }
                }
            )
        }
    }
}

private fun decodeJpegWithExif(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap? {
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = readExifRotation(bytes)
    if (rotation == 0) {
        return decoded.asImageBitmap()
    }
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val rotated = runCatching {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }.getOrNull()
    return if (rotated != null && rotated != decoded) {
        decoded.recycle()
        rotated.asImageBitmap()
    } else {
        decoded.asImageBitmap()
    }
}

private fun readExifRotation(bytes: ByteArray): Int {
    val orientation = runCatching {
        ByteArrayInputStream(bytes).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_UNDEFINED)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSPOSE -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSVERSE -> 270
        else -> 0
    }
}
