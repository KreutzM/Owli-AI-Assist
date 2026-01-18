package com.owlitech.owli.assist.processing

import android.graphics.Matrix
import android.graphics.RectF
import com.owlitech.owli.assist.ml.BoundingBox
import kotlin.math.max
import kotlin.math.min

data class FrameMapping(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val modelSize: Int,
    val modelToSource: Matrix
) {
    private val sourceToPreview = Matrix()
    private var lastPreviewWidth = -1f
    private var lastPreviewHeight = -1f

    fun mapToPreviewRect(bbox: BoundingBox, previewWidth: Float, previewHeight: Float): RectF {
        if (previewWidth <= 0f || previewHeight <= 0f) {
            return RectF()
        }
        val left = bbox.xMin * modelSize
        val top = bbox.yMin * modelSize
        val right = bbox.xMax * modelSize
        val bottom = bbox.yMax * modelSize
        val points = floatArrayOf(
            left, top,
            right, top,
            right, bottom,
            left, bottom
        )
        modelToSource.mapPoints(points)
        ensureSourceToPreview(previewWidth, previewHeight)
        sourceToPreview.mapPoints(points)
        var minX = points[0]
        var minY = points[1]
        var maxX = points[0]
        var maxY = points[1]
        var i = 2
        while (i < points.size) {
            val x = points[i]
            val y = points[i + 1]
            minX = min(minX, x)
            minY = min(minY, y)
            maxX = max(maxX, x)
            maxY = max(maxY, y)
            i += 2
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun ensureSourceToPreview(previewWidth: Float, previewHeight: Float) {
        if (previewWidth == lastPreviewWidth && previewHeight == lastPreviewHeight) return
        lastPreviewWidth = previewWidth
        lastPreviewHeight = previewHeight
        val scale = max(previewWidth / sourceWidth.toFloat(), previewHeight / sourceHeight.toFloat())
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val offsetX = (previewWidth - scaledW) * 0.5f
        val offsetY = (previewHeight - scaledH) * 0.5f
        sourceToPreview.reset()
        sourceToPreview.postScale(scale, scale)
        sourceToPreview.postTranslate(offsetX, offsetY)
    }
}
