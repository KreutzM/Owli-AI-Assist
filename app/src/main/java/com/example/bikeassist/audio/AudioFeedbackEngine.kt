package com.example.bikeassist.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.bikeassist.domain.HazardLevel
import com.example.bikeassist.domain.SceneState
import com.example.bikeassist.blindview.BlindViewConfig
import com.example.bikeassist.blindview.BlindViewSpeechPlanner
import com.example.bikeassist.pipeline.AppMode
import java.io.Closeable
import java.util.Locale

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

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val rateResult = tts?.setSpeechRate(desiredSpeechRate)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                ttsState = if (ttsReady) TtsState.READY else TtsState.ERROR
                Log.d(
                    TAG,
                    "TTS init success, languageResult=$result, ready=$ttsReady, speechRate=$desiredSpeechRate setResult=$rateResult, pendingMessage=$pendingMessage"
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
        speakWithVolume(text, standardVolume, "scene-state")
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
        speakWithVolume(combined, vlmVolume, "vlm")
        lastVlmMessage = combined
        lastVlmSpokenAt = now
    }

    fun updateSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        desiredSpeechRate = clamped
        tts?.setSpeechRate(clamped)
        Log.d(TAG, "updateSpeechRate to $clamped")
    }

    private fun maxHazard(a: HazardLevel, b: HazardLevel): HazardLevel {
        return if (a.ordinal >= b.ordinal) a else b
    }

    private fun speakPendingIfPossible() {
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
    }

    companion object {
        private const val TAG = "AudioFeedbackEngine"
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 3.0f
        private const val VLM_REPEAT_SUPPRESS_MS = 8_000L
    }

    fun setStandardVolume(volume: Float) {
        standardVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun setVlmVolume(volume: Float) {
        vlmVolume = volume.coerceIn(0.0f, 1.0f)
    }

    private fun speakWithVolume(text: String, volume: Float, utteranceId: String) {
        if (!ttsReady) {
            Log.d(TAG, "speak skipped, ttsReady=false, text=$text")
            return
        }
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.0f, 1.0f))
        }
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId) ?: TextToSpeech.ERROR
        Log.d(TAG, "speak result=$result, text=$text volume=$volume")
    }

    fun diagnostics(): com.example.bikeassist.diagnostics.TtsDiagnostics {
        return com.example.bikeassist.diagnostics.TtsDiagnostics(
            ready = ttsReady,
            speechRate = desiredSpeechRate
        )
    }

    private enum class TtsState { INITIALIZING, READY, ERROR }
}
