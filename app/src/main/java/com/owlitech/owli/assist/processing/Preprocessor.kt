package com.owlitech.owli.assist.processing

import androidx.camera.core.ImageProxy
import com.owlitech.owli.assist.motion.MotionSnapshot

/**
 * Wandelt Kamera-Frames in Modell-Input um.
 */
interface Preprocessor {
    fun preprocess(image: ImageProxy, motion: MotionSnapshot? = null): PreprocessResult
}
