package com.example.bikeassist.processing

import androidx.camera.core.ImageProxy

/**
 * Fake-Preprocessor für Demo-Zwecke; liefert einen Dummy-Input.
 */
class FakePreprocessor : Preprocessor {
    override fun preprocess(image: ImageProxy): FloatArray {
        // Kein echtes Preprocessing nötig für Demo-Detector.
        return floatArrayOf(0f)
    }
}
