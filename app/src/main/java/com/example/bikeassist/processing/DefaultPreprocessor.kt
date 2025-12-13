package com.example.bikeassist.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy

class DefaultPreprocessor(
    private val targetWidth: Int? = null,
    private val targetHeight: Int? = null,
    private val logInterval: Int = 30
) : Preprocessor {

    private val converter = YuvToRgbConverter()
    private var frameCount = 0

    override fun preprocess(image: ImageProxy): Bitmap {
        val start = System.nanoTime()
        val rotation = image.imageInfo.rotationDegrees
        var bmp = converter.yuvToRgb(image)

        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        if (targetWidth != null && targetHeight != null && (bmp.width != targetWidth || bmp.height != targetHeight)) {
            bmp = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
        }

        val end = System.nanoTime()
        frameCount++
        if (frameCount % logInterval == 0) {
            val ms = (end - start) / 1_000_000
            Log.d(TAG, "preprocess: in=${image.width}x${image.height}, out=${bmp.width}x${bmp.height}, rotation=$rotation, time=${ms}ms")
        }

        return bmp
    }

    companion object {
        private const val TAG = "DefaultPreprocessor"
    }
}
