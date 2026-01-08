package com.owlitech.owli.assist.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.owlitech.owli.assist.domain.HazardLevel
import com.owlitech.owli.assist.domain.SceneState
import com.owlitech.owli.assist.blindview.BlindViewConfig
import com.owlitech.owli.assist.blindview.BlindViewSpeechPlanner
import com.owlitech.owli.assist.pipeline.AppMode
import java.io.Closeable
import java.util.Locale
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Erzeugt Audio-/TTS-Ausgabe basierend auf SceneState.
 */
class AudioFeedbackEngine(
    private val context: Context,
    private val mode: AppMode = AppMode.BLINDVIEW,
    private val blindViewConfig: BlindViewConfig = BlindViewConfig(),
    private val cooldownMillis: Long = blindViewConfig.minSpeakIntervalMs,
    private val speechPlanner: BlindViewSpeechPlanner = BlindViewSpeechPlanner(blindViewConfig)
) : Closeable {

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val utteranceCounter = AtomicLong(0L)
    private val vlmStreamInFlight = AtomicInteger(0)
    private val pendingUtterances = Collections.synchronizedSet(mutableSetOf<String>())
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastSpokenAt: Long = 0L
    private var lastMessage: String? = null
    private var lastLevel: HazardLevel = HazardLevel.NONE
    private var pendingMessage: String? = null
    private var pendingLevel: HazardLevel = HazardLevel.NONE
    private var ttsReady: Boolean = false
    private var ttsState: TtsState = TtsState.INITIALIZING
    private var lastNotReadyLog: Long = 0L
    private val notReadyLogInterval = 1_000L
    private var lastVlmMessage: String? = null
    private var lastVlmSpokenAt: Long = 0L
    private var standardVolume: Float = 1.0f
    private var vlmVolume: Float = 1.0f
    private var desiredSpeechRate: Float =
        blindViewConfig.ttsSpeechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
    private var desiredPitch: Float = DEFAULT_PITCH
    private var sceneSpeechSuppressed: Boolean = false
    private var onVlmStreamIdle: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val rateResult = tts?.setSpeechRate(desiredSpeechRate)
                tts?.setPitch(desiredPitch)
                tts?.setAudioAttributes(audioAttributes)
                tts?.setOnUtteranceProgressListener(ttsProgressListener)
                tts?.playSilentUtterance(PREWARM_SILENCE_MS, TextToSpeech.QUEUE_ADD, "tts-prewarm")
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                ttsState = if (ttsReady) TtsState.READY else TtsState.ERROR
                Log.d(
                    TAG,
                    "TTS init success, languageResult=$result, ready=$ttsReady, speechRate=$desiredSpeechRate pitch=$desiredPitch setResult=$rateResult, pendingMessage=$pendingMessage"
                )
                speakPendingIfPossible()
            } else {
                ttsReady = false
                ttsState = TtsState.ERROR
                Log.d(TAG, "TTS init failed, status=$status")
            }
        }
    }

    fun onSceneUpdated(state: SceneState) {
        if (sceneSpeechSuppressed) {
            pendingMessage = null
            pendingLevel = HazardLevel.NONE
            return
        }
        if (mode == AppMode.BLINDVIEW) {
            val now = System.currentTimeMillis()
            val utterance = speechPlanner.nextUtterance(state.blindViewItems, now)
            if (utterance == null) {
                return
            }
            if (!ttsReady) {
                pendingMessage = utterance
                pendingLevel = state.overallHazardLevel
                if (now - lastNotReadyLog >= notReadyLogInterval) {
                    Log.d(TAG, "TTS not ready (state=$ttsState), pendingMessage=$pendingMessage")
                    lastNotReadyLog = now
                }
                return
            }
            speak(utterance)
            lastMessage = utterance
            lastLevel = state.overallHazardLevel
            lastSpokenAt = now
            return
        }
        val message = state.primaryMessage ?: run {
            // Reset, damit bei erneuter Warnung wieder gesprochen wird.
            lastMessage = null
            lastLevel = state.overallHazardLevel
            return
        }
        val now = System.currentTimeMillis()
        val levelIncreased = state.overallHazardLevel.ordinal > lastLevel.ordinal
        val messageChanged = message != lastMessage
        val cooldownPassed = now - lastSpokenAt >= cooldownMillis

        if (!ttsReady) {
            pendingMessage = message
            pendingLevel = state.overallHazardLevel
            if (now - lastNotReadyLog >= notReadyLogInterval) {
                Log.d(TAG, "TTS not ready (state=$ttsState), pendingMessage=$pendingMessage")
                lastNotReadyLog = now
            }
            return
        }

        if ((levelIncreased || messageChanged) && cooldownPassed) {
            speak(message)
            lastMessage = message
            lastLevel = state.overallHazardLevel
            lastSpokenAt = now
        } else {
            lastLevel = maxHazard(lastLevel, state.overallHazardLevel)
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Log.d(TAG, "speak skipped, ttsReady=false, text=$text")
            return
        }
        speakWithVolume(text, standardVolume, "scene-state", QueueMode.FLUSH)
    }

    fun speakVlmResponse(ttsOneLiner: String?, actionSuggestion: String?) {
        val combined = listOfNotNull(ttsOneLiner?.trim(), actionSuggestion?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(". ")
        if (combined.isBlank()) return
        val now = System.currentTimeMillis()
        val recentlySpoken = combined == lastVlmMessage && now - lastVlmSpokenAt < VLM_REPEAT_SUPPRESS_MS
        if (recentlySpoken) return
        if (!ttsReady) {
            pendingMessage = combined
            pendingLevel = HazardLevel.NONE
            if (now - lastNotReadyLog >= notReadyLogInterval) {
                Log.d(TAG, "TTS not ready (state=$ttsState), pendingMessage=$pendingMessage")
                lastNotReadyLog = now
            }
            return
        }
        speakWithVolume(combined, vlmVolume, "vlm", QueueMode.FLUSH)
        lastVlmMessage = combined
        lastVlmSpokenAt = now
    }

    fun updateSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        desiredSpeechRate = clamped
        tts?.setSpeechRate(clamped)
        Log.d(TAG, "updateSpeechRate to $clamped")
    }

    fun updatePitch(pitch: Float) {
        val clamped = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        desiredPitch = clamped
        tts?.setPitch(clamped)
        Log.d(TAG, "updatePitch to $clamped")
    }

    private fun maxHazard(a: HazardLevel, b: HazardLevel): HazardLevel {
        return if (a.ordinal >= b.ordinal) a else b
    }

    private fun speakPendingIfPossible() {
        if (sceneSpeechSuppressed) {
            pendingMessage = null
            pendingLevel = HazardLevel.NONE
            return
        }
        val message = pendingMessage ?: return
        val now = System.currentTimeMillis()
        val cooldownPassed = now - lastSpokenAt >= cooldownMillis
        if (cooldownPassed) {
            speak(message)
            lastMessage = message
            lastLevel = pendingLevel
            lastSpokenAt = now
            pendingMessage = null
        } else {
            Log.d(TAG, "Pending message cooldown not passed, keeping message=$message")
        }
    }

    override fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        pendingUtterances.clear()
    }

    companion object {
        private const val TAG = "AudioFeedbackEngine"
        private const val VLM_STREAM_PREFIX = "vlm-stream"
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 3.0f
        private const val MIN_PITCH = 0.5f
        private const val MAX_PITCH = 2.0f
        private const val DEFAULT_PITCH = 1.0f
        private const val VLM_REPEAT_SUPPRESS_MS = 8_000L
        private const val PREWARM_SILENCE_MS = 150L
    }

    fun setStandardVolume(volume: Float) {
        standardVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun setVlmVolume(volume: Float) {
        vlmVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun setSceneSpeechSuppressed(suppressed: Boolean) {
        if (sceneSpeechSuppressed == suppressed) return
        sceneSpeechSuppressed = suppressed
        if (suppressed) {
            pendingMessage = null
            pendingLevel = HazardLevel.NONE
        }
        Log.d(TAG, "sceneSpeechSuppressed=$sceneSpeechSuppressed")
    }

    fun setOnVlmStreamIdleListener(listener: (() -> Unit)?) {
        onVlmStreamIdle = listener
    }

    fun isVlmStreamBusy(): Boolean = vlmStreamInFlight.get() > 0

    fun speakVlmStreamingChunk(text: String, queueMode: QueueMode) {
        speakWithVolume(text, vlmVolume, "vlm-stream", queueMode)
    }

    fun stopVlmTts() {
        stopAllTts()
    }

    fun stopAllTts() {
        tts?.stop()
        pendingUtterances.clear()
        vlmStreamInFlight.set(0)
        onVlmStreamIdle?.invoke()
        abandonAudioFocus()
    }

    private fun speakWithVolume(
        text: String,
        volume: Float,
        utterancePrefix: String,
        queueMode: QueueMode
    ) {
        if (!ttsReady) {
            Log.d(TAG, "speak skipped, ttsReady=false, text=$text")
            return
        }
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.0f, 1.0f))
        }
        requestAudioFocus()
        val utteranceId = nextUtteranceId(utterancePrefix)
        pendingUtterances.add(utteranceId)
        if (utterancePrefix == VLM_STREAM_PREFIX) {
            vlmStreamInFlight.incrementAndGet()
        }
        val result = tts?.speak(text, queueMode.toTtsQueueMode(), params, utteranceId) ?: TextToSpeech.ERROR
        if (result == TextToSpeech.ERROR) {
            pendingUtterances.remove(utteranceId)
            if (utterancePrefix == VLM_STREAM_PREFIX) {
                if (vlmStreamInFlight.decrementAndGet() <= 0) {
                    vlmStreamInFlight.set(0)
                    onVlmStreamIdle?.invoke()
                }
            }
        }
        Log.d(TAG, "speak result=$result, text=$text volume=$volume queue=$queueMode")
    }

    private fun requestAudioFocus() {
        val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { audioFocusRequest = it }
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    private fun nextUtteranceId(prefix: String): String {
        return "$prefix-${utteranceCounter.incrementAndGet()}"
    }

    private fun QueueMode.toTtsQueueMode(): Int {
        return when (this) {
            QueueMode.ADD -> TextToSpeech.QUEUE_ADD
            QueueMode.FLUSH -> TextToSpeech.QUEUE_FLUSH
        }
    }

    private val ttsProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            if (utteranceId == null) return
            pendingUtterances.remove(utteranceId)
            if (utteranceId.startsWith("$VLM_STREAM_PREFIX-")) {
                if (vlmStreamInFlight.decrementAndGet() <= 0) {
                    vlmStreamInFlight.set(0)
                    onVlmStreamIdle?.invoke()
                }
            }
            if (pendingUtterances.isEmpty()) {
                abandonAudioFocus()
            }
        }

        override fun onError(utteranceId: String?) {
            if (utteranceId == null) return
            pendingUtterances.remove(utteranceId)
            if (utteranceId.startsWith("$VLM_STREAM_PREFIX-")) {
                if (vlmStreamInFlight.decrementAndGet() <= 0) {
                    vlmStreamInFlight.set(0)
                    onVlmStreamIdle?.invoke()
                }
            }
            if (pendingUtterances.isEmpty()) {
                abandonAudioFocus()
            }
        }
    }

    fun diagnostics(): com.owlitech.owli.assist.diagnostics.TtsDiagnostics {
        return com.owlitech.owli.assist.diagnostics.TtsDiagnostics(
            ready = ttsReady,
            speechRate = desiredSpeechRate
        )
    }

    private enum class TtsState { INITIALIZING, READY, ERROR }
}
