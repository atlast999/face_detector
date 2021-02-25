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

import android.graphics.Bitmap
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.facedetector.face.BitmapUtils
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T> :
  VisionImageProcessor {

  private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

  // Whether this processor is already shut down
  private var isShutdown = false

/*
  // -----------------Code for processing single still image----------------------------------------
  override fun processBitmap(bitmap: Bitmap?, graphicOverlay: vn.com.vti.smartta.ui.photocapture.helper.graphic.GraphicOverlay) {
    requestDetectInImage(
      InputImage.fromBitmap(bitmap!!, 0),
      graphicOverlay, /* originalCameraImage= */
      null, /* shouldShowFps= */
      false
    )
  }
*/

  // -----------------Code for processing live preview frame from CameraX API-----------------------
  @RequiresApi(VERSION_CODES.KITKAT)
  @ExperimentalGetImage
  override fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
    if (isShutdown) {
      return
    }
    requestDetectInImage(
      InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
      graphicOverlay,
      BitmapUtils.getBitmap(image)
    )
      // When the image is from CameraX analysis use case, must call image.close() on received
      // images when finished using them. Otherwise, new images may not be received or the camera
      // may stall.
      .addOnCompleteListener { image.close() }
  }

  // -----------------Common processing logic-------------------------------------------------------
  private fun requestDetectInImage(
    image: InputImage,
    graphicOverlay: GraphicOverlay,
    bimap: Bitmap?
  ): Task<T> {
    return detectInImage(image, bimap).addOnSuccessListener(executor) { results: T ->
      graphicOverlay.clear()
      this@VisionProcessorBase.onSuccess(results, graphicOverlay)
      graphicOverlay.postInvalidate()
    }
      .addOnFailureListener(executor) { e: Exception ->
        graphicOverlay.clear()
        graphicOverlay.postInvalidate()
        this@VisionProcessorBase.onFailure(e)
      }
  }

  override fun stop() {
    executor.shutdown()
    isShutdown = true
  }

  protected abstract fun detectInImage(image: InputImage, bitmap: Bitmap?): Task<T>

  protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

  protected abstract fun onFailure(e: Exception)
}
