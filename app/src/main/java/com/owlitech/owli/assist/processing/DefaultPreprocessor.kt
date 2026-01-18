package com.owlitech.owli.assist.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.core.graphics.scale
import com.owlitech.owli.assist.motion.MotionSnapshot
import kotlin.math.min

class DefaultPreprocessor(
    private val outputSize: Int = 448,
    private val enableImuDerotation: Boolean = false,
    private val stabilizationQualityMin: Float = 0.3f,
    private val logInterval: Int = 30
) : Preprocessor {

    private val converter = YuvToRgbConverter()
    private var frameCount = 0

    override fun preprocess(image: ImageProxy, motion: MotionSnapshot?): PreprocessResult {
        val start = System.nanoTime()
        val rotation = image.imageInfo.rotationDegrees
        var bmp = converter.yuvToRgb(image)
        var ownsBitmap = false

        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (ownsBitmap && rotated !== bmp) {
                bmp.recycle()
            }
            bmp = rotated
            ownsBitmap = true
        }

        val qualityMin = stabilizationQualityMin.coerceIn(0f, 1f)
        var appliedRollDeg = 0f
        if (enableImuDerotation && motion != null && motion.quality >= qualityMin) {
            appliedRollDeg = -motion.rollRad * RAD_TO_DEG
            val stabilized = rotateKeepingSize(bmp, appliedRollDeg)
            if (ownsBitmap && stabilized !== bmp) {
                bmp.recycle()
            }
            bmp = stabilized
            ownsBitmap = true
        }

        val sourceWidth = bmp.width
        val sourceHeight = bmp.height
        val squareSize = min(sourceWidth, sourceHeight)
        val cropLeft = ((sourceWidth - squareSize) / 2f).toInt()
        val cropTop = ((sourceHeight - squareSize) / 2f).toInt()
        val cropped = Bitmap.createBitmap(bmp, cropLeft, cropTop, squareSize, squareSize)
        if (ownsBitmap && cropped !== bmp) {
            bmp.recycle()
        }
        ownsBitmap = true

        val output = if (squareSize != outputSize) {
            val resized = cropped.scale(outputSize, outputSize, true)
            if (resized !== cropped) {
                cropped.recycle()
            }
            resized
        } else {
            cropped
        }

        val mapping = buildMapping(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            squareSize = squareSize,
            cropLeft = cropLeft,
            cropTop = cropTop,
            appliedRollDeg = appliedRollDeg
        )

        val end = System.nanoTime()
        frameCount++
        if (frameCount % logInterval == 0) {
            val ms = (end - start) / 1_000_000
            Log.d(
                TAG,
                "preprocess: in=${image.width}x${image.height}, out=${output.width}x${output.height}, rotation=$rotation, roll=${"%.1f".format(appliedRollDeg)}, time=${ms}ms"
            )
        }

        return PreprocessResult(
            bitmap448 = output,
            mapping = mapping,
            appliedRollDeg = appliedRollDeg
        )
    }

    private fun rotateKeepingSize(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val output = Bitmap.createBitmap(
            source.width,
            source.height,
            source.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val matrix = Matrix().apply {
            postRotate(degrees, source.width / 2f, source.height / 2f)
        }
        canvas.drawBitmap(source, matrix, bitmapPaint)
        return output
    }

    private fun buildMapping(
        sourceWidth: Int,
        sourceHeight: Int,
        squareSize: Int,
        cropLeft: Int,
        cropTop: Int,
        appliedRollDeg: Float
    ): FrameMapping {
        val scale = squareSize.toFloat() / outputSize.toFloat()
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(cropLeft.toFloat(), cropTop.toFloat())
            if (appliedRollDeg != 0f) {
                postRotate(-appliedRollDeg, sourceWidth / 2f, sourceHeight / 2f)
            }
        }
        return FrameMapping(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            modelSize = outputSize,
            modelToSource = matrix
        )
    }

    companion object {
        private const val TAG = "DefaultPreprocessor"
        private const val RAD_TO_DEG = 180f / kotlin.math.PI.toFloat()
        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    }
}
