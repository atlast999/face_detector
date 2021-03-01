package com.example.facedetector.face.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.toRectF
import com.example.facedetector.ai.RecognitionAPI
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

/** Face Detector Demo.  */
class FaceDetectorProcessor(
    detectorOptions: FaceDetectorOptions,
    private val recognitionAPI: RecognitionAPI
) :
    VisionProcessorBase<List<Face>>() {

    private val detector: FaceDetector = FaceDetection.getClient(detectorOptions)
    private var imgBitmap: Bitmap? = null
    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage, bitmap: Bitmap?): Task<List<Face>> {
        this.imgBitmap = bitmap
        return detector.process(image)
    }

    override fun onSuccess(results: List<Face>, graphicOverlay: GraphicOverlay) {
        results.forEach {
            graphicOverlay.add(FaceGraphic(graphicOverlay, it, ""))
            Single.just(recognise(it))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { res ->
                    graphicOverlay.add(FaceGraphic(graphicOverlay, it, res))
                }
        }
    }

    private fun recognise(face: Face): String {
        val frame = Bitmap.createBitmap(imgBitmap!!)
        val cropFace = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val faceBB = RectF(face.boundingBox)
        val sx = 120.toFloat()/faceBB.width()
        val sy = 120.toFloat()/faceBB.height()
        val matrix = Matrix()
        matrix.postTranslate(-faceBB.left, -faceBB.top)
        matrix.postScale(sx, sy)
        val canvas = Canvas(cropFace)
        canvas.drawBitmap(frame!!, matrix, null)
        return recognitionAPI.recognizeImage(cropFace)
    }

    override fun onFailure(e: Exception) {
        Log.e("TAG", "onFailure: ",e )
    }

}