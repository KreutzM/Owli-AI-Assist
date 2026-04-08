package com.owlitech.owli.assist.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.owlitech.owli.assist.util.AppLogger
import java.io.Closeable
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class AudioFeedbackEngine(
    private val context: Context
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
    private var pendingMessage: String? = null
    private var ttsReady: Boolean = false
    private var ttsState: TtsState = TtsState.INITIALIZING
    private var lastNotReadyLog: Long = 0L
    private val notReadyLogInterval = 1_000L
    private var lastVlmMessage: String? = null
    private var lastVlmSpokenAt: Long = 0L
    private var vlmVolume: Float = 1.0f
    private var desiredSpeechRate: Float = DEFAULT_SPEECH_RATE
    private var desiredPitch: Float = DEFAULT_PITCH
    private var ttsEnabled: Boolean = true
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
                AppLogger.d(
                    TAG,
                    "TTS init success, languageResult=$result, ready=$ttsReady, speechRate=$desiredSpeechRate pitch=$desiredPitch setResult=$rateResult, pending=${pendingMessage != null}"
                )
                speakPendingIfPossible()
            } else {
                ttsReady = false
                ttsState = TtsState.ERROR
                AppLogger.d(TAG, "TTS init failed, status=$status")
            }
        }
    }

    fun speakVlmResponse(ttsOneLiner: String?, actionSuggestion: String?) {
        if (!ttsEnabled) {
            return
        }
        val combined = listOfNotNull(ttsOneLiner?.trim(), actionSuggestion?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(". ")
        if (combined.isBlank()) return
        val now = System.currentTimeMillis()
        val recentlySpoken = combined == lastVlmMessage && now - lastVlmSpokenAt < VLM_REPEAT_SUPPRESS_MS
        if (recentlySpoken) return
        if (!ttsReady) {
            pendingMessage = combined
            if (now - lastNotReadyLog >= notReadyLogInterval) {
                AppLogger.d(TAG, "TTS not ready (state=$ttsState), pending=true")
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
        AppLogger.d(TAG, "updateSpeechRate to $clamped")
    }

    fun updatePitch(pitch: Float) {
        val clamped = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        desiredPitch = clamped
        tts?.setPitch(clamped)
        AppLogger.d(TAG, "updatePitch to $clamped")
    }

    fun setTtsEnabled(enabled: Boolean) {
        if (ttsEnabled == enabled) return
        ttsEnabled = enabled
        if (!enabled) {
            pendingMessage = null
            stopAllTts()
        }
        AppLogger.d(TAG, "ttsEnabled=$ttsEnabled")
    }

    private fun speakPendingIfPossible() {
        val message = pendingMessage ?: return
        speakWithVolume(message, vlmVolume, "vlm", QueueMode.FLUSH)
        lastVlmMessage = message
        lastVlmSpokenAt = System.currentTimeMillis()
        pendingMessage = null
    }

    override fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        pendingUtterances.clear()
    }

    fun setVlmVolume(volume: Float) {
        vlmVolume = volume.coerceIn(0.0f, 1.0f)
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
        if (!ttsEnabled) {
            AppLogger.d(TAG, "speak skipped, ttsEnabled=false")
            return
        }
        if (!ttsReady) {
            AppLogger.d(TAG, "speak skipped, ttsReady=false")
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
        AppLogger.d(TAG, "speak result=$result volume=$volume queue=$queueMode")
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
            handleUtteranceFinished(utteranceId)
        }

        override fun onError(utteranceId: String?) {
            handleUtteranceFinished(utteranceId)
        }
    }

    private fun handleUtteranceFinished(utteranceId: String?) {
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

    companion object {
        private const val TAG = "AudioFeedbackEngine"
        private const val VLM_STREAM_PREFIX = "vlm-stream"
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 3.0f
        private const val DEFAULT_SPEECH_RATE = 2.0f
        private const val MIN_PITCH = 0.5f
        private const val MAX_PITCH = 2.0f
        private const val DEFAULT_PITCH = 1.0f
        private const val VLM_REPEAT_SUPPRESS_MS = 8_000L
        private const val PREWARM_SILENCE_MS = 150L
    }

    private enum class TtsState { INITIALIZING, READY, ERROR }
}
