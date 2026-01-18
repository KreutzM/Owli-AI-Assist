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
import kotlin.math.roundToInt

class DefaultPreprocessor(
    private val outputSize: Int = 448,
    private val enableImuDerotation: Boolean = false,
    private val stabilizationQualityMin: Float = 0.3f,
    private val logInterval: Int = 30
) : Preprocessor {

    private val converter = YuvToRgbConverter()
    private var frameCount = 0
    private var stableCx = Float.NaN
    private var stableCy = Float.NaN

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
        val targetCx = sourceWidth / 2f
        val targetCy = sourceHeight / 2f
        if (stableCx.isNaN() || stableCy.isNaN()) {
            stableCx = targetCx
            stableCy = targetCy
        } else {
            val alpha = if (motion?.motionLevel == com.owlitech.owli.assist.motion.MotionLevel.HIGH) {
                STABLE_CENTER_ALPHA_HIGH
            } else {
                STABLE_CENTER_ALPHA
            }
            stableCx += (targetCx - stableCx) * alpha
            stableCy += (targetCy - stableCy) * alpha
        }
        val cropSize = min(
            STABILIZED_CROP_SIZE,
            min(sourceWidth, sourceHeight)
        )
        val maxLeft = (sourceWidth - cropSize).coerceAtLeast(0)
        val maxTop = (sourceHeight - cropSize).coerceAtLeast(0)
        val cropLeft = (stableCx - cropSize / 2f)
            .coerceIn(0f, maxLeft.toFloat())
            .roundToInt()
        val cropTop = (stableCy - cropSize / 2f)
            .coerceIn(0f, maxTop.toFloat())
            .roundToInt()
        val cropped = Bitmap.createBitmap(bmp, cropLeft, cropTop, cropSize, cropSize)
        if (ownsBitmap && cropped !== bmp) {
            bmp.recycle()
        }
        ownsBitmap = true

        val output = if (cropSize != outputSize) {
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
            cropSize = cropSize,
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
        cropSize: Int,
        cropLeft: Int,
        cropTop: Int,
        appliedRollDeg: Float
    ): FrameMapping {
        val scale = cropSize.toFloat() / outputSize.toFloat()
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
        private const val STABILIZED_CROP_SIZE = 560
        private const val STABLE_CENTER_ALPHA = 0.2f
        private const val STABLE_CENTER_ALPHA_HIGH = 0.06f
    }
}
