package com.example.bikeassist.audio

import android.content.Context
import com.example.bikeassist.domain.SceneState
import java.io.Closeable

/**
 * Erzeugt Audio-/TTS-Ausgabe basierend auf SceneState.
 */
class AudioFeedbackEngine(
    private val context: Context
) : Closeable {

    fun onSceneUpdated(state: SceneState) {
        // TODO: TTS-Ausgabe mit Cooldown implementieren.
    }

    override fun close() {
        // TODO: Ressourcen freigeben (TTS shutdown etc.).
    }
}
