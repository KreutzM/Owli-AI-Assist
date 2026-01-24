package com.owlitech.owli.assist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.owlitech.owli.assist.domain.SceneState
import com.owlitech.owli.assist.pipeline.SnapshotProvider
import com.owlitech.owli.assist.pipeline.VisionPipeline
import com.owlitech.owli.assist.pipeline.VisionPipelineHandle
import com.owlitech.owli.assist.vlm.OpenRouterVlmClient
import com.owlitech.owli.assist.vlm.VlmChatMessage
import com.owlitech.owli.assist.vlm.VlmContentPart
import com.owlitech.owli.assist.vlm.VlmConfig
import com.owlitech.owli.assist.vlm.VlmSceneDescription
import com.owlitech.owli.assist.vlm.VlmSession
import com.owlitech.owli.assist.vlm.VlmUiState
import com.owlitech.owli.assist.vlm.VlmClient
import com.owlitech.owli.assist.vlm.VlmAttachment
import com.owlitech.owli.assist.vlm.VlmAttachmentStore
import com.owlitech.owli.assist.vlm.VlmProfile
import com.owlitech.owli.assist.vlm.VlmProfileLoader
import com.owlitech.owli.assist.vlm.VlmStreamingCallback
import com.owlitech.owli.assist.BuildConfig
import com.owlitech.owli.assist.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(
    detectorInfo: String = "",
    private val vlmClient: VlmClient = OpenRouterVlmClient(
        VlmProfileLoader.fallbackProfiles().first()
    )
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _shouldAutoStart = MutableStateFlow(false)
    val shouldAutoStart: StateFlow<Boolean> = _shouldAutoStart

    private val _sceneState = MutableStateFlow<SceneState?>(null)
    val sceneState: StateFlow<SceneState?> = _sceneState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _status = MutableStateFlow(detectorInfo)
    val status: StateFlow<String> = _status

    private val _vlmUiState = MutableStateFlow<VlmUiState>(VlmUiState.Inactive)
    val vlmUiState: StateFlow<VlmUiState> = _vlmUiState

    private val _isAutoScanRunning = MutableStateFlow(false)
    val isAutoScanRunning: StateFlow<Boolean> = _isAutoScanRunning
    private val attachmentStore = VlmAttachmentStore()
    val vlmAttachments: StateFlow<List<VlmAttachment>> = attachmentStore.attachments

    private var collectJob: Job? = null
    private var pipeline: VisionPipeline? = null
    private var snapshotProvider: SnapshotProvider? = null
    private var detectorInfo: String = detectorInfo
    private var vlmSession: VlmSession? = null
    private var lastVlmDescription: VlmSceneDescription? = null
    private var vlmSystemPrompt: String = VlmConfig.DEFAULT_SYSTEM_PROMPT
    private var vlmOverviewPrompt: String = VlmConfig.DEFAULT_OVERVIEW_PROMPT
    private var vlmProfile: VlmProfile = VlmProfileLoader.fallbackProfiles().first()
    private val useStructuredVlmParsing = false
    private var autoScanJob: Job? = null
    private val vlmRequestInFlight = AtomicBoolean(false)

    fun setPipeline(handle: VisionPipelineHandle) {
        // stop old pipeline if running
        val wasRunning = _isRunning.value
        stopInternal(resetAutoStart = false)
        pipeline?.close()
        pipeline = handle.pipeline
        snapshotProvider = handle.snapshotProvider
        detectorInfo = handle.detectorInfo
        _status.value = handle.detectorInfo
        com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
            isRunning = _isRunning.value,
            detectorInfo = handle.detectorInfo,
            analysisIntervalMs = 0L
        )
        if (wasRunning) {
            start()
        }
    }

    fun requestStart() {
        _shouldAutoStart.value = true
    }

    fun autoStartIfNeeded() {
        if (_shouldAutoStart.value) {
            start()
        }
    }

    fun start() {
        if (_isRunning.value) return
        val current = pipeline ?: return
        runCatching { current.start() }
            .onSuccess {
                collectJob = viewModelScope.launch {
                    current.sceneStates.collect { state ->
                        _sceneState.value = state
                        com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateSceneSnapshot(
                            detections = state.detections.size,
                            topLabels = state.detections.groupBy { it.label }.entries.sortedByDescending { it.value.size }.map { it.key },
                            preview = state.blindViewUtterancePreview
                        )
                    }
                }
                _isRunning.value = true
                com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
                    isRunning = true,
                    detectorInfo = detectorInfo,
                    analysisIntervalMs = 0L
                )
            }
            .onFailure { _lastError.value = it.message }
    }

    fun stopUser() {
        _shouldAutoStart.value = false
        stopInternal(resetAutoStart = false)
    }

    fun stopForLifecycle() {
        stopInternal(resetAutoStart = false)
    }

    private fun stopInternal(resetAutoStart: Boolean = true) {
        if (resetAutoStart) {
            _shouldAutoStart.value = false
        }
        if (!_isRunning.value) return
        collectJob?.cancel()
        collectJob = null
        pipeline?.let { runCatching { it.stop() } }
        _sceneState.value = null
        _isRunning.value = false
        com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
            isRunning = false,
            detectorInfo = detectorInfo,
            analysisIntervalMs = 0L
        )
    }

    override fun onCleared() {
        stopInternal(resetAutoStart = false)
        pipeline?.let { runCatching { it.close() } }
        super.onCleared()
    }

    fun enterVlmMode() {
        requestNewScene()
    }

    fun requestNewScene() {
        if (isVlmBusy()) {
            AppLogger.d("VLM", "Neue Szene uebersprungen: VLM ist beschaeftigt")
            return
        }
        if (!vlmRequestInFlight.compareAndSet(false, true)) {
            AppLogger.d("VLM", "Neue Szene uebersprungen: Request bereits aktiv")
            return
        }
        viewModelScope.launch {
            try {
                performNewSceneRequest()
            } finally {
                vlmRequestInFlight.set(false)
            }
        }
    }

    fun startAutoScan() {
        val autoScan = vlmProfile.autoScan ?: run {
            AppLogger.w("VLM", "Autoscan angefordert, aber Profil hat kein auto_scan")
            return
        }
        if (autoScanJob?.isActive == true) return
        val intervalMs = autoScan.intervalMs.takeIf { it > 0 } ?: 2000L
        _isAutoScanRunning.value = true
        autoScanJob = viewModelScope.launch {
            try {
                while (isActive) {
                    requestNewScene()
                    delay(intervalMs)
                }
            } finally {
                _isAutoScanRunning.value = false
            }
        }
    }

    fun stopAutoScan() {
        autoScanJob?.cancel()
        autoScanJob = null
        _isAutoScanRunning.value = false
    }

    fun addVlmAttachment(jpegBytes: ByteArray): VlmAttachment {
        return attachmentStore.add(jpegBytes)
    }

    fun removeVlmAttachment(id: String): Boolean {
        return attachmentStore.remove(id)
    }

    fun clearVlmAttachments() {
        attachmentStore.clear()
    }

    suspend fun addVlmAttachmentFromSnapshot(): Int? {
        if (vlmSession == null) {
            AppLogger.w("VLM", "Add image requested without active session")
            return null
        }
        val provider = snapshotProvider ?: run {
            AppLogger.w("VLM", "SnapshotProvider not ready - skipping add image")
            return null
        }
        val imageSettings = vlmProfile.imageSettings
        val jpeg = withContext(Dispatchers.Default) {
            provider.requestFreshJpegSnapshot(
                maxSidePx = imageSettings.maxSidePx,
                quality = imageSettings.jpegQuality
            )
        }
        if (jpeg == null) {
            AppLogger.e("VLM", "Kein JPEG-Snapshot verfuegbar fuer Anhang")
            return null
        }
        attachmentStore.add(jpeg)
        return attachmentStore.attachments.value.size
    }

    private fun isVlmBusy(): Boolean {
        return when (_vlmUiState.value) {
            is VlmUiState.LoadingOverview,
            is VlmUiState.Asking,
            is VlmUiState.Streaming -> true
            else -> false
        }
    }

    private suspend fun performNewSceneRequest() {
        AppLogger.i(
            "VLM",
            "enterVlmMode started profile=${vlmProfile.id} model=${vlmProfile.modelId} " +
                "streaming=${vlmProfile.streamingEnabled}"
        )
        if (!vlmClient.isConfigured || BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            AppLogger.e("VLM", "OpenRouter API-Key fehlt")
            _vlmUiState.value = VlmUiState.Error("OpenRouter API-Key fehlt. Bitte OPENROUTER_API_KEY in local.properties setzen.")
            return
        }
        val provider = snapshotProvider ?: run {
            AppLogger.w("VLM", "SnapshotProvider not ready - skipping new scene request")
            return
        }
        _vlmUiState.value = VlmUiState.LoadingOverview("Snapshot vorbereiten...")
        val imageSettings = vlmProfile.imageSettings
        val pipelineRunning = _isRunning.value
        val jpeg = withContext(Dispatchers.Default) {
            if (pipelineRunning) {
                provider.getLatestJpegSnapshot(
                    maxSidePx = imageSettings.maxSidePx,
                    quality = imageSettings.jpegQuality
                )
            } else {
                provider.requestFreshJpegSnapshot(
                    maxSidePx = imageSettings.maxSidePx,
                    quality = imageSettings.jpegQuality
                )
            }
        }
        if (jpeg == null) {
            AppLogger.e("VLM", "Kein JPEG-Snapshot verfuegbar")
            _vlmUiState.value = VlmUiState.Error("Kein Kamerabild verfuegbar.")
            return
        }
        _vlmUiState.value = VlmUiState.LoadingOverview("VLM anfragen...", snapshotBytes = jpeg)
        val session = VlmSession(snapshotBytes = jpeg, messageHistory = mutableListOf())
        val messages = buildOverviewMessages(session)
        try {
            val result = if (vlmProfile.streamingEnabled && !useStructuredVlmParsing) {
                val buffer = StringBuilder()
                val callback = object : VlmStreamingCallback {
                    override fun onDelta(textDelta: String) {
                        if (textDelta.isBlank()) return
                        buffer.append(textDelta)
                        _vlmUiState.value = VlmUiState.Streaming(
                            partialText = buffer.toString(),
                            updatedAt = System.currentTimeMillis(),
                            snapshotBytes = jpeg
                        )
                    }

                    override fun onComplete(
                        finalText: String,
                        usage: com.owlitech.owli.assist.vlm.VlmUsage?,
                        finishReason: String?,
                        nativeFinishReason: String?
                    ) = Unit

                    override fun onError(error: Throwable) {
                        AppLogger.e(error, "VLM: Streaming error (Overview)")
                    }
                }
                withContext(Dispatchers.IO) {
                    vlmClient.chatStreaming(messages, callback)
                }
            } else {
                withContext(Dispatchers.IO) {
                    vlmClient.chat(messages)
                }
            }
            if (result.isReasoningOnly) {
                AppLogger.w("VLM", "VLM: Antwort enthaelt nur Reasoning (Overview)")
                _vlmUiState.value = VlmUiState.Error(
                    "VLM lieferte nur Reasoning ohne Ergebnis.",
                    snapshotBytes = jpeg
                )
                return
            }
            if (result.assistantContent.isBlank()) {
                AppLogger.w("VLM", "VLM: Leere Antwort (Overview)")
                _vlmUiState.value = VlmUiState.Error("VLM-Antwort war leer.", snapshotBytes = jpeg)
                return
            }
            if (useStructuredVlmParsing) {
                if (result.isReasoningOnly) {
                    AppLogger.e("VLM", "VLM lieferte nur Thinking ohne Ergebnis (Overview)")
                    _vlmUiState.value = VlmUiState.Error(
                        "VLM lieferte nur Thinking ohne Ergebnis.",
                        snapshotBytes = jpeg
                    )
                    return
                }
                val parsed = VlmSceneDescription.parse(result.assistantContent)
                if (parsed.isSuccess) {
                    lastVlmDescription = parsed.getOrNull()
                    session.messageHistory.add(
                        VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                    )
                    vlmSession = session
                    _vlmUiState.value = VlmUiState.OverviewReady(
                        lastVlmDescription!!,
                        System.currentTimeMillis(),
                        snapshotBytes = jpeg
                    )
                } else {
                    _vlmUiState.value = VlmUiState.Error(
                        "VLM-Antwort konnte nicht gelesen werden.",
                        snapshotBytes = jpeg
                    )
                }
            } else {
                val raw = result.assistantContent.trim()
                lastVlmDescription = null
                session.messageHistory.add(
                    VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                )
                vlmSession = session
                _vlmUiState.value = VlmUiState.OverviewReadyRaw(
                    raw,
                    System.currentTimeMillis(),
                    snapshotBytes = jpeg
                )
            }
        } catch (ex: Exception) {
            AppLogger.e(ex, "VLM: Fehler bei enterVlmMode (Overview-Request)")
            _vlmUiState.value = VlmUiState.Error(
                ex.message ?: "Unbekannter VLM-Fehler",
                snapshotBytes = jpeg
            )
        }
    }

    fun askVlm(questionText: String) {
        if (!vlmClient.isConfigured || BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            AppLogger.e("VLM", "OpenRouter API-Key fehlt")
            _vlmUiState.value = VlmUiState.Error("OpenRouter API-Key fehlt. Bitte OPENROUTER_API_KEY in local.properties setzen.")
            return
        }
        val session = vlmSession ?: run {
            AppLogger.w("VLM", "VLM: askVlm ohne aktive Session aufgerufen.")
            _vlmUiState.value = VlmUiState.Error("Keine aktive VLM-Session. Bitte zuerst 'Neue Szene' ausfuehren.")
            return
        }
        val snapshotBytes = session.snapshotBytes
        if (questionText.isBlank()) return
        _vlmUiState.value = VlmUiState.Asking(lastVlmDescription, questionText, snapshotBytes = snapshotBytes)
        AppLogger.i("VLM", "askVlm started questionLength=${questionText.length}")
        viewModelScope.launch {
            val messages = buildFollowUpMessages(session, questionText)
            try {
                val result = if (vlmProfile.streamingEnabled && !useStructuredVlmParsing) {
                    val buffer = StringBuilder()
                    val callback = object : VlmStreamingCallback {
                        override fun onDelta(textDelta: String) {
                            if (textDelta.isBlank()) return
                            buffer.append(textDelta)
                            _vlmUiState.value = VlmUiState.Streaming(
                                partialText = buffer.toString(),
                                updatedAt = System.currentTimeMillis(),
                                snapshotBytes = snapshotBytes
                            )
                        }

                        override fun onComplete(
                            finalText: String,
                            usage: com.owlitech.owli.assist.vlm.VlmUsage?,
                            finishReason: String?,
                            nativeFinishReason: String?
                        ) = Unit

                        override fun onError(error: Throwable) {
                            AppLogger.e(error, "VLM: Streaming error (Follow-up)")
                        }
                    }
                    withContext(Dispatchers.IO) {
                        vlmClient.chatStreaming(messages, callback)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        vlmClient.chat(messages)
                    }
                }
                if (result.isReasoningOnly) {
                    AppLogger.w("VLM", "VLM: Antwort enthaelt nur Reasoning (Follow-up)")
                    _vlmUiState.value = VlmUiState.Error(
                        "VLM lieferte nur Reasoning ohne Ergebnis.",
                        snapshotBytes = snapshotBytes
                    )
                    return@launch
                }
                if (result.assistantContent.isBlank()) {
                    AppLogger.w("VLM", "VLM: Leere Antwort (Follow-up)")
                    _vlmUiState.value = VlmUiState.Error("VLM-Antwort war leer.", snapshotBytes = snapshotBytes)
                    return@launch
                }
                if (useStructuredVlmParsing) {
                    if (result.isReasoningOnly) {
                        AppLogger.e("VLM", "VLM lieferte nur Thinking ohne Ergebnis (Follow-up)")
                        _vlmUiState.value = VlmUiState.Error(
                            "VLM lieferte nur Thinking ohne Ergebnis.",
                            snapshotBytes = snapshotBytes
                        )
                        return@launch
                    }
                    val parsed = VlmSceneDescription.parse(result.assistantContent)
                    if (parsed.isSuccess) {
                        lastVlmDescription = parsed.getOrNull()
                        session.messageHistory.add(
                            VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text(questionText)))
                        )
                        session.messageHistory.add(
                            VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                        )
                        _vlmUiState.value = VlmUiState.OverviewReady(
                            lastVlmDescription!!,
                            System.currentTimeMillis(),
                            snapshotBytes = snapshotBytes
                        )
                    } else {
                        _vlmUiState.value = VlmUiState.Error(
                            "VLM-Antwort konnte nicht gelesen werden.",
                            snapshotBytes = snapshotBytes
                        )
                    }
                } else {
                    val raw = result.assistantContent.trim()
                    session.messageHistory.add(
                        VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text(questionText)))
                    )
                    session.messageHistory.add(
                        VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                    )
                    lastVlmDescription = null
                    _vlmUiState.value = VlmUiState.OverviewReadyRaw(
                        raw,
                        System.currentTimeMillis(),
                        snapshotBytes = snapshotBytes
                    )
                }
            } catch (ex: Exception) {
                AppLogger.e(ex, "VLM: Fehler bei askVlm (Follow-up-Request)")
                _vlmUiState.value = VlmUiState.Error(
                    ex.message ?: "Unbekannter VLM-Fehler",
                    snapshotBytes = snapshotBytes
                )
            }
        }
    }

    fun closeVlm() {
        stopAutoScan()
        _vlmUiState.value = VlmUiState.Inactive
        vlmSession = null
        lastVlmDescription = null
    }

    fun applyVlmConfig(config: VlmConfig) {
        vlmSystemPrompt = config.systemPrompt.ifBlank { VlmConfig.DEFAULT_SYSTEM_PROMPT }
        vlmOverviewPrompt = config.overviewPrompt.ifBlank { VlmConfig.DEFAULT_OVERVIEW_PROMPT }
    }

    fun applyVlmProfile(profile: VlmProfile) {
        stopAutoScan()
        vlmProfile = profile
        vlmSystemPrompt = profile.systemPrompt.ifBlank { VlmConfig.DEFAULT_SYSTEM_PROMPT }
        vlmOverviewPrompt = profile.overviewPrompt.ifBlank { VlmConfig.DEFAULT_OVERVIEW_PROMPT }
        (vlmClient as? OpenRouterVlmClient)?.updateProfile(profile)
        AppLogger.i(
            "VLM",
            "VLM profile applied id=${profile.id} model=${profile.modelId} " +
                "streaming=${profile.streamingEnabled}"
        )
    }

    private fun buildOverviewMessages(session: VlmSession): List<VlmChatMessage> {
        val system = VlmChatMessage(
            role = "system",
            content = listOf(VlmContentPart.Text(vlmSystemPrompt))
        )
        val user = VlmChatMessage(
            role = "user",
            content = listOf(
                VlmContentPart.Text(vlmOverviewPrompt),
                VlmContentPart.ImageUrl(jpegToDataUrl(session.snapshotBytes))
            )
        )
        return listOf(system, user)
    }

    private fun buildFollowUpMessages(session: VlmSession, questionText: String): List<VlmChatMessage> {
        val messages = mutableListOf<VlmChatMessage>()
        messages += VlmChatMessage(
            role = "system",
            content = listOf(VlmContentPart.Text(vlmSystemPrompt))
        )
        messages += VlmChatMessage(
            role = "user",
            content = listOf(
                VlmContentPart.Text(vlmOverviewPrompt),
                VlmContentPart.ImageUrl(jpegToDataUrl(session.snapshotBytes))
            )
        )
        messages += session.messageHistory
        messages += VlmChatMessage(
            role = "user",
            content = listOf(VlmContentPart.Text(questionText))
        )
        return messages
    }

    private fun jpegToDataUrl(bytes: ByteArray): String {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private fun enforceSafety(desc: VlmSceneDescription): VlmSceneDescription {
        val obstaclesNotEmpty = desc.obstacles.isNotEmpty()
        val confidenceHigh = desc.overallConfidence?.equals("high", ignoreCase = true) == true
        if (!obstaclesNotEmpty && confidenceHigh) {
            return desc
        }
        val safeOneLiner = sanitizeAllClear(desc.ttsOneLiner)
        val safeAction = sanitizeAllClear(desc.actionSuggestion)
        val safeReadable = sanitizeAllClear(desc.readableText)
        return desc.copy(
            ttsOneLiner = safeOneLiner,
            actionSuggestion = safeAction,
            readableText = safeReadable
        )
    }

    private fun sanitizeAllClear(text: String): String {
        if (text.isBlank()) return text
        val lowered = text.lowercase()
        val hasAllClear = lowered.contains("weg frei") || lowered.contains("freie fahrt") || lowered.contains("alles frei")
        return if (hasAllClear) {
            "Keine Freigabe. Bitte vorsichtig fahren."
        } else {
            text
        }
    }

    class Factory(
        private val vlmClient: VlmClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(vlmClient = vlmClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
