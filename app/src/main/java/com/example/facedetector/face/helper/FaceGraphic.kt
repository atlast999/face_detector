/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.facedetector.face.helper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.face.Face

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
class FaceGraphic constructor(
    overlay: GraphicOverlay?,
    private val face: Face,
    private val result: String
) : GraphicOverlay.Graphic(overlay) {
    private val facePositionPaint: Paint
    private val boxPaints: Paint
    private val labelPaint: Paint

    init {
        val selectedColor = Color.WHITE
        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
        boxPaints = Paint()
        boxPaints.color = Color.GREEN
        boxPaints.style = Paint.Style.STROKE
        boxPaints.strokeWidth = BOX_STROKE_WIDTH
        labelPaint = Paint()
        labelPaint.color = Color.BLACK
        labelPaint.style = Paint.Style.FILL
        labelPaint.textSize = 30f
    }

    /** Draws the face annotations for position on the supplied canvas.  */
    override fun draw(canvas: Canvas) {
        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.centerY().toFloat())
        canvas.drawCircle(
            x,
            y,
            FACE_POSITION_RADIUS,
            facePositionPaint
        )
        val width = scale(face.boundingBox.width() shr 1)
        val height = scale(face.boundingBox.height() shr 1)
        val left = x - width
        val top = y - height
        val right = x + width
        val bottom = y + height

        canvas.drawRect(left, top, right, bottom, boxPaints)

        canvas.drawText(
            result,
            left,
            top - BOX_STROKE_WIDTH,
            labelPaint)


    }

    companion object {
        private const val FACE_POSITION_RADIUS = 8.0f
        private const val BOX_STROKE_WIDTH = 5.0f
    }
}
