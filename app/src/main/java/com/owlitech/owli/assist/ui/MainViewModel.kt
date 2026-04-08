package com.owlitech.owli.assist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.owlitech.owli.assist.BuildConfig
import com.owlitech.owli.assist.util.AppLogger
import com.owlitech.owli.assist.vlm.OpenRouterVlmClient
import com.owlitech.owli.assist.vlm.VlmAttachment
import com.owlitech.owli.assist.vlm.VlmAttachmentStore
import com.owlitech.owli.assist.vlm.VlmChatMessage
import com.owlitech.owli.assist.vlm.VlmClient
import com.owlitech.owli.assist.vlm.VlmConfig
import com.owlitech.owli.assist.vlm.VlmContentPart
import com.owlitech.owli.assist.vlm.VlmProfile
import com.owlitech.owli.assist.vlm.VlmProfileLoader
import com.owlitech.owli.assist.vlm.VlmSceneDescription
import com.owlitech.owli.assist.vlm.VlmSession
import com.owlitech.owli.assist.vlm.VlmStreamingCallback
import com.owlitech.owli.assist.vlm.VlmUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val MISSING_OPENROUTER_CLIENT_KEY_LOG = "OpenRouter client key fehlt"
private const val MISSING_OPENROUTER_CLIENT_KEY_UI =
    "OpenRouter client key fehlt. Lokale Builds lesen OPENROUTER_API_KEY aus local.properties; " +
        "Release-Builds liefern diesen Wert aktuell mit der App aus. Das ist eine Zwischenloesung, keine sichere Secret-Speicherung."

class MainViewModel(
    private val vlmClient: VlmClient = OpenRouterVlmClient(
        VlmProfileLoader.fallbackProfiles().first()
    )
) : ViewModel() {

    private val _vlmUiState = MutableStateFlow<VlmUiState>(VlmUiState.Inactive)
    val vlmUiState: StateFlow<VlmUiState> = _vlmUiState

    private val _isAutoScanRunning = MutableStateFlow(false)
    val isAutoScanRunning: StateFlow<Boolean> = _isAutoScanRunning

    private val attachmentStore = VlmAttachmentStore()
    val vlmAttachments: StateFlow<List<VlmAttachment>> = attachmentStore.attachments

    private val _lastVlmImageBytes = MutableStateFlow<ByteArray?>(null)
    val lastVlmImageBytes: StateFlow<ByteArray?> = _lastVlmImageBytes

    private var vlmSession: VlmSession? = null
    private var lastVlmDescription: VlmSceneDescription? = null
    private var vlmSystemPrompt: String = VlmConfig.DEFAULT_SYSTEM_PROMPT
    private var vlmOverviewPrompt: String = VlmConfig.DEFAULT_OVERVIEW_PROMPT
    private var vlmProfile: VlmProfile = VlmProfileLoader.fallbackProfiles().first()
    private val useStructuredVlmParsing = false
    private val vlmRequestInFlight = AtomicBoolean(false)

    fun startAutoScan() {
        val autoScan = vlmProfile.autoScan ?: run {
            AppLogger.w("VLM", "Autoscan angefordert, aber Profil hat kein auto_scan")
            return
        }
        if (_isAutoScanRunning.value) return
        _isAutoScanRunning.value = true
    }

    fun stopAutoScan() {
        _isAutoScanRunning.value = false
    }

    fun addVlmAttachment(jpegBytes: ByteArray): VlmAttachment {
        val attachment = attachmentStore.add(jpegBytes)
        _lastVlmImageBytes.value = attachment.jpegBytes
        return attachment
    }

    fun addVlmAttachmentForActiveSession(jpegBytes: ByteArray): Int? {
        if (vlmSession == null) {
            AppLogger.w("VLM", "Add image requested without active session")
            return null
        }
        addVlmAttachment(jpegBytes)
        return attachmentStore.attachments.value.size
    }

    fun removeVlmAttachment(id: String): Boolean {
        val removed = attachmentStore.remove(id)
        if (removed) {
            val remaining = attachmentStore.attachments.value
            _lastVlmImageBytes.value = remaining.lastOrNull()?.jpegBytes ?: vlmSession?.snapshotBytes
        }
        return removed
    }

    fun clearVlmAttachments() {
        attachmentStore.clear()
    }

    private fun isVlmBusy(): Boolean {
        return when (_vlmUiState.value) {
            is VlmUiState.LoadingOverview,
            is VlmUiState.Asking,
            is VlmUiState.Streaming -> true
            else -> false
        }
    }

    fun requestNewSceneWithSnapshot(jpegBytes: ByteArray) {
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
                performNewSceneWithSnapshot(jpegBytes)
            } finally {
                vlmRequestInFlight.set(false)
            }
        }
    }

    private suspend fun performNewSceneWithSnapshot(jpeg: ByteArray) {
        if (!vlmClient.isConfigured || BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            AppLogger.e("VLM", MISSING_OPENROUTER_CLIENT_KEY_LOG)
            _vlmUiState.value = VlmUiState.Error(MISSING_OPENROUTER_CLIENT_KEY_UI)
            return
        }
        clearVlmAttachments()
        _lastVlmImageBytes.value = jpeg
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
            AppLogger.e("VLM", MISSING_OPENROUTER_CLIENT_KEY_LOG)
            _vlmUiState.value = VlmUiState.Error(MISSING_OPENROUTER_CLIENT_KEY_UI)
            return
        }
        val session = vlmSession ?: run {
            AppLogger.w("VLM", "VLM: askVlm ohne aktive Session aufgerufen.")
            _vlmUiState.value = VlmUiState.Error("Keine aktive VLM-Session. Bitte zuerst 'Neue Szene' ausfuehren.")
            return
        }
        val attachments = attachmentStore.attachments.value
        val snapshotBytes = session.snapshotBytes
        if (questionText.isBlank() && attachments.isEmpty()) return
        _vlmUiState.value = VlmUiState.Asking(lastVlmDescription, questionText, snapshotBytes = snapshotBytes)
        AppLogger.i("VLM", "askVlm started questionLength=${questionText.length}")
        viewModelScope.launch {
            val messages = buildFollowUpMessages(session, questionText, attachments)
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
                        if (questionText.isNotBlank()) {
                            session.messageHistory.add(
                                VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text(questionText)))
                            )
                        }
                        session.messageHistory.add(
                            VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                        )
                        clearVlmAttachments()
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
                    if (questionText.isNotBlank()) {
                        session.messageHistory.add(
                            VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text(questionText)))
                        )
                    }
                    session.messageHistory.add(
                        VlmChatMessage(role = "assistant", content = listOf(VlmContentPart.Text(result.assistantContent)))
                    )
                    lastVlmDescription = null
                    clearVlmAttachments()
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
        clearVlmAttachments()
        _lastVlmImageBytes.value = null
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

    private fun buildFollowUpMessages(
        session: VlmSession,
        questionText: String,
        attachments: List<VlmAttachment>
    ): List<VlmChatMessage> {
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
        val userParts = mutableListOf<VlmContentPart>()
        if (questionText.isNotBlank()) {
            userParts += VlmContentPart.Text(questionText)
        }
        attachments.forEach { attachment ->
            userParts += VlmContentPart.ImageUrl(jpegToDataUrl(attachment.jpegBytes))
        }
        messages += VlmChatMessage(
            role = "user",
            content = userParts
        )
        return messages
    }

    private fun jpegToDataUrl(bytes: ByteArray): String {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
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
