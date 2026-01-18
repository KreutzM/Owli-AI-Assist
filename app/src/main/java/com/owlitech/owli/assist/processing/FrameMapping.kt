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
    fun mapToPreviewRect(bbox: BoundingBox, previewWidth: Float, previewHeight: Float): RectF {
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
        val scaleX = previewWidth / sourceWidth.toFloat()
        val scaleY = previewHeight / sourceHeight.toFloat()
        return RectF(
            minX * scaleX,
            minY * scaleY,
            maxX * scaleX,
            maxY * scaleY
        )
    }
}
