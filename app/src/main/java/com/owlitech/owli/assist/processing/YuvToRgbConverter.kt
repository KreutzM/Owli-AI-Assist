package com.owlitech.owli.assist.processing

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

/**
 * Konvertiert YUV_420_888 nach ARGB_8888 ohne JPEG-Roundtrip.
 * Basierend auf rowStride/pixelStride Handling.
 */
class YuvToRgbConverter : AutoCloseable {

    private val TAG = "YuvToRgbConverter"

    private var buffer: Bitmap? = null
    private var bufferWidth = 0
    private var bufferHeight = 0

    fun yuvToRgb(image: ImageProxy): Bitmap {
        ensureBuffer(image.width, image.height)
        val out = buffer ?: throw IllegalStateException("Bitmap buffer not available")

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val argbPixels = IntArray(out.width * out.height)
        var outputIndex = 0

        for (row in 0 until out.height) {
            val yRowStart = row * yRowStride
            val uvRowStart = (row shr 1) * uvRowStride
            for (col in 0 until out.width) {
                val y = (yBuffer.get(yRowStart + col).toInt() and 0xFF)
                val uvIndex = uvRowStart + (col shr 1) * uvPixelStride
                val u = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val v = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                val yVal = (y - 16).coerceAtLeast(0)
                val c = 1192 * yVal
                val d = u
                val e = v

                var r = (c + 1634 * e)
                var g = (c - 833 * e - 400 * d)
                var b = (c + 2066 * d)

                r = (r shr 10).coerceIn(0, 255)
                g = (g shr 10).coerceIn(0, 255)
                b = (b shr 10).coerceIn(0, 255)

                argbPixels[outputIndex++] =
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        out.setPixels(argbPixels, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    private fun ensureBuffer(width: Int, height: Int) {
        if (buffer == null || bufferWidth != width || bufferHeight != height) {
            buffer?.recycle()
            buffer = createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bufferWidth = width
            bufferHeight = height
        }
    }

    override fun close() {
        buffer?.recycle()
        buffer = null
    }
}
