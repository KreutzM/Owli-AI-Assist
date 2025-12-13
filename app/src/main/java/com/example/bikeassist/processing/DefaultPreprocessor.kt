package com.example.bikeassist.processing

import androidx.camera.core.ImageProxy

/**
 * Einfacher Preprocessor-Placeholder. Für das Demo-Setup ist keine echte
 * Bildvorverarbeitung nötig; gibt einen Dummy-Input zurück.
 */
class DefaultPreprocessor : Preprocessor {
    override fun preprocess(image: ImageProxy): FloatArray {
        // TODO: YUV->RGB, Resize, Normalize implementieren, sobald echtes Modell genutzt wird.
        return floatArrayOf(0f)
    }
}
