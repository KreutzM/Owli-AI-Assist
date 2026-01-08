package com.owlitech.owli.assist.processing

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Wandelt Kamera-Frames in Modell-Input um.
 */
interface Preprocessor {
    fun preprocess(image: ImageProxy): Bitmap
}
