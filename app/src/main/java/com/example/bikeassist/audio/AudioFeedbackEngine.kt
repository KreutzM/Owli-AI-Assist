package com.example.bikeassist.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.bikeassist.domain.HazardLevel
import com.example.bikeassist.domain.SceneState
import java.io.Closeable
import java.util.Locale

/**
 * Erzeugt Audio-/TTS-Ausgabe basierend auf SceneState.
 */
class AudioFeedbackEngine(
    private val context: Context,
    private val cooldownMillis: Long = 2500L
) : Closeable {

    private var tts: TextToSpeech? = null
    private var lastSpokenAt: Long = 0L
    private var lastMessage: String? = null
    private var lastLevel: HazardLevel = HazardLevel.NONE
    private var pendingMessage: String? = null
    private var pendingLevel: HazardLevel = HazardLevel.NONE
    private var ttsReady: Boolean = false

    init {
        tts = TextToSpeech(context) { status ->
            Log.d(TAG, "TTS init status=$status")
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
                Log.d(TAG, "TTS ready=true, pendingMessage=$pendingMessage")
                speakPendingIfPossible()
            } else {
                ttsReady = false
                Log.d(TAG, "TTS init failed, ready=false")
            }
        }
    }

    fun onSceneUpdated(state: SceneState) {
        val message = state.primaryMessage ?: return
        val now = System.currentTimeMillis()
        val levelIncreased = state.overallHazardLevel.ordinal > lastLevel.ordinal
        val messageChanged = message != lastMessage
        val cooldownPassed = now - lastSpokenAt >= cooldownMillis

        if (!ttsReady) {
            pendingMessage = message
            pendingLevel = state.overallHazardLevel
            Log.d(TAG, "TTS not ready, pendingMessage=$pendingMessage")
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
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scene-state") ?: TextToSpeech.ERROR
        Log.d(TAG, "speak result=$result, text=$text")
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
    }
}
