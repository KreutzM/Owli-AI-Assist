package com.owlitech.owli.assist.processing

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.min

data class TranslationEstimate(
    val dx: Int,
    val dy: Int,
    val quality: Float,
    val patchCenterX: Int? = null,
    val patchCenterY: Int? = null
)

class GlobalMotionEstimator(
    val lowWidth: Int = DEFAULT_LOW_WIDTH,
    val lowHeight: Int = DEFAULT_LOW_HEIGHT,
    val patchSize: Int = DEFAULT_PATCH_SIZE,
    val searchRadius: Int = DEFAULT_SEARCH_RADIUS,
    val patchOffset: Int = DEFAULT_PATCH_OFFSET,
    val qualityMin: Float = DEFAULT_QUALITY_MIN,
    val textureScoreMin: Float = DEFAULT_TEXTURE_SCORE_MIN
) {
    private var prevLuma: IntArray? = null

    fun estimate(source: Bitmap): TranslationEstimate {
        val lowRes = downsampleLuma(source)
        val prev = prevLuma
        prevLuma = lowRes
        if (prev == null) return TranslationEstimate(0, 0, 0f, null, null)

        val safePatch = patchSize.coerceIn(8, min(lowWidth, lowHeight))
        val halfPatch = safePatch / 2
        val centerX = lowWidth / 2
        val centerY = lowHeight / 2
        val offset = patchOffset.coerceAtLeast(0)
        val candidates = arrayOf(
            centerX to centerY,
            (centerX - offset) to centerY,
            (centerX + offset) to centerY,
            centerX to (centerY - offset),
            centerX to (centerY + offset)
        )

        var bestTexture = Long.MIN_VALUE
        var bestLeft = 0
        var bestTop = 0
        var bestCenterX = centerX
        var bestCenterY = centerY
        for ((cx, cy) in candidates) {
            val left = (cx - halfPatch).coerceIn(0, lowWidth - safePatch)
            val top = (cy - halfPatch).coerceIn(0, lowHeight - safePatch)
            val score = textureScore(prev, left, top, safePatch)
            if (score > bestTexture) {
                bestTexture = score
                bestLeft = left
                bestTop = top
                bestCenterX = left + halfPatch
                bestCenterY = top + halfPatch
            }
        }

        val avgTexture = if (safePatch > 1) {
            bestTexture.toFloat() / (2f * safePatch * (safePatch - 1))
        } else {
            0f
        }
        if (avgTexture < textureScoreMin) {
            return TranslationEstimate(0, 0, 0f, null, null)
        }

        var bestSad = Long.MAX_VALUE
        var secondBest = Long.MAX_VALUE
        var bestDx = 0
        var bestDy = 0
        val radius = searchRadius.coerceAtLeast(0)
        for (dy in -radius..radius) {
            val curTop = bestTop + dy
            if (curTop < 0 || curTop > lowHeight - safePatch) continue
            for (dx in -radius..radius) {
                val curLeft = bestLeft + dx
                if (curLeft < 0 || curLeft > lowWidth - safePatch) continue
                val sad = sadPatch(prev, bestLeft, bestTop, lowRes, curLeft, curTop, safePatch)
                if (sad < bestSad) {
                    secondBest = bestSad
                    bestSad = sad
                    bestDx = dx
                    bestDy = dy
                } else if (sad < secondBest) {
                    secondBest = sad
                }
            }
        }

        val quality = if (bestSad == Long.MAX_VALUE || secondBest == Long.MAX_VALUE || secondBest == 0L) {
            0f
        } else {
            ((secondBest - bestSad).toFloat() / secondBest.toFloat()).coerceIn(0f, 1f)
        }
        val clampedQualityMin = qualityMin.coerceIn(0f, 1f)
        return if (quality < clampedQualityMin) {
            TranslationEstimate(0, 0, 0f, bestCenterX, bestCenterY)
        } else {
            TranslationEstimate(bestDx, bestDy, quality, bestCenterX, bestCenterY)
        }
    }

    private fun sadPatch(
        prev: IntArray,
        prevLeft: Int,
        prevTop: Int,
        curr: IntArray,
        currLeft: Int,
        currTop: Int,
        size: Int
    ): Long {
        var sad = 0L
        var row = 0
        while (row < size) {
            val prevIdx = (prevTop + row) * lowWidth + prevLeft
            val currIdx = (currTop + row) * lowWidth + currLeft
            var col = 0
            while (col < size) {
                sad += abs(prev[prevIdx + col] - curr[currIdx + col])
                col++
            }
            row++
        }
        return sad
    }

    private fun textureScore(
        luma: IntArray,
        left: Int,
        top: Int,
        size: Int
    ): Long {
        var score = 0L
        var row = 0
        while (row < size) {
            val rowStart = (top + row) * lowWidth + left
            var col = 0
            while (col < size) {
                val idx = rowStart + col
                val v = luma[idx]
                if (col < size - 1) {
                    score += abs(luma[idx + 1] - v)
                }
                if (row < size - 1) {
                    score += abs(luma[idx + lowWidth] - v)
                }
                col++
            }
            row++
        }
        return score
    }

    private fun downsampleLuma(source: Bitmap): IntArray {
        val scaled = Bitmap.createScaledBitmap(source, lowWidth, lowHeight, false)
        val pixels = IntArray(lowWidth * lowHeight)
        scaled.getPixels(pixels, 0, lowWidth, 0, 0, lowWidth, lowHeight)
        if (scaled !== source) {
            scaled.recycle()
        }
        val luma = IntArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            luma[i] = (r * 30 + g * 59 + b * 11) / 100
        }
        return luma
    }

    companion object {
        private const val DEFAULT_LOW_WIDTH = 160
        private const val DEFAULT_LOW_HEIGHT = 90
        private const val DEFAULT_PATCH_SIZE = 48
        private const val DEFAULT_SEARCH_RADIUS = 12
        private const val DEFAULT_PATCH_OFFSET = 16
        private const val DEFAULT_QUALITY_MIN = 0.15f
        private const val DEFAULT_TEXTURE_SCORE_MIN = 2.0f
    }
}