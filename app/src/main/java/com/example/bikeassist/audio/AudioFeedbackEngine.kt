package com.example.bikeassist.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.bikeassist.domain.Direction
import com.example.bikeassist.domain.HazardEvent
import com.example.bikeassist.domain.HazardLevel
import com.example.bikeassist.domain.ProximityZone
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
    private var lastSignature: SpokenSignature? = null
    private var pendingMessage: String? = null
    private var pendingLevel: HazardLevel = HazardLevel.NONE
    private var ttsReady: Boolean = false
    private var ttsState: TtsState = TtsState.INITIALIZING
    private var lastNotReadyLog: Long = 0L
    private val notReadyLogInterval = 1_000L

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.LANG_NOT_SUPPORTED
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                ttsState = if (ttsReady) TtsState.READY else TtsState.ERROR
                Log.d(TAG, "TTS init success, languageResult=$result, ready=$ttsReady, pendingMessage=$pendingMessage")
                speakPendingIfPossible()
            } else {
                ttsReady = false
                ttsState = TtsState.ERROR
                Log.d(TAG, "TTS init failed, status=$status")
            }
        }
    }

    fun onSceneUpdated(state: SceneState) {
        val message = state.primaryMessage
        val now = System.currentTimeMillis()

        val signature = buildSignature(state)
        val cooldown = cooldownForLevel(signature?.level ?: HazardLevel.NONE)
        val levelIncreased = signature != null && lastSignature?.level?.ordinal?.let { signature.level.ordinal > it } == true
        val signatureChanged = signature != null && signature != lastSignature
        val cooldownPassed = now - lastSpokenAt >= cooldown

        if (message == null || signature == null) {
            lastMessage = null
            lastLevel = state.overallHazardLevel
            return
        }

        if (!ttsReady) {
            pendingMessage = message
            pendingLevel = signature.level
            if (now - lastNotReadyLog >= notReadyLogInterval) {
                Log.d(TAG, "TTS not ready (state=$ttsState), pendingMessage=$pendingMessage")
                lastNotReadyLog = now
            }
            return
        }

        if (signature.level == HazardLevel.NONE) {
            lastSignature = signature
            lastMessage = message
            lastLevel = signature.level
            return
        }

        if ((signatureChanged || levelIncreased) && cooldownPassed) {
            speak(message)
            lastMessage = message
            lastLevel = signature.level
            lastSignature = signature
            lastSpokenAt = now
        } else {
            lastLevel = maxHazard(lastLevel, signature.level)
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

    private enum class TtsState { INITIALIZING, READY, ERROR }

    private data class SpokenSignature(
        val kind: String,
        val level: HazardLevel,
        val direction: Direction?,
        val zone: ProximityZone?
    )

    private fun buildSignature(state: SceneState): SpokenSignature? {
        state.primaryTrafficLight?.let {
            return when (it) {
                com.example.bikeassist.domain.TrafficLightPhase.RED -> SpokenSignature("TL_RED", HazardLevel.WARNING, null, null)
                com.example.bikeassist.domain.TrafficLightPhase.GREEN -> SpokenSignature("TL_GREEN", HazardLevel.NONE, null, null)
                com.example.bikeassist.domain.TrafficLightPhase.UNKNOWN -> null
            }
        }
        val hazard = state.primaryHazard ?: return null
        if (hazard.urgency == HazardLevel.NONE) return null
        val kind = when (hazard.type) {
            com.example.bikeassist.domain.HazardType.PERSON_AHEAD -> "PERSON"
            com.example.bikeassist.domain.HazardType.VEHICLE_AHEAD -> "VEHICLE"
            com.example.bikeassist.domain.HazardType.OBSTACLE_AHEAD -> "OBSTACLE"
            else -> "OTHER"
        }
        return SpokenSignature(kind, hazard.urgency, hazard.direction, hazard.zone)
    }

    private fun cooldownForLevel(level: HazardLevel): Long {
        return when (level) {
            HazardLevel.DANGER -> 1500L
            HazardLevel.WARNING -> 2500L
            HazardLevel.NONE -> Long.MAX_VALUE
        }
    }
}
