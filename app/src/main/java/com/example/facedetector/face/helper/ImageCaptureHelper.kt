package com.example.facedetector.face.helper

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.facedetector.ai.RecognitionAPI
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutionException


class ImageCaptureHelper(
    private val context: Context,
    private val recognitionAPI: RecognitionAPI
) : ICaptureHelper {

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    private fun bindPreviewUseCase(lifecycle: LifecycleOwner, previewView: PreviewView?) {
        cameraProvider?.run {
            previewUseCase?.let { unbind(it) }
            Preview.Builder().build().apply {
                previewView?.let {
                    setSurfaceProvider(it.surfaceProvider)
                }
            }.also {
                previewUseCase = it
                bindToLifecycle(lifecycle, cameraSelector, it)
            }
        }
    }

    private fun bindAnalysisUseCase(lifecycle: LifecycleOwner, graphicOverlay: GraphicOverlay?) {
        cameraProvider?.run {
            analysisUseCase?.let {
                unbind(it)
            }
            imageProcessor?.stop()
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.1F)
                .build()
            imageProcessor = FaceDetectorProcessor(faceDetectorOptions, recognitionAPI)
            needUpdateGraphicOverlayImageSourceInfo = true
            analysisUseCase = ImageAnalysis.Builder().build().apply {
                setAnalyzer(
                    // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                    // thus we can just runs the analyzer itself on main thread.
                    ContextCompat.getMainExecutor(context),
                    { imageProxy: ImageProxy ->
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            val isImageFlipped =
                                lensFacing == CameraSelector.LENS_FACING_FRONT
                            val rotationDegrees =
                                imageProxy.imageInfo.rotationDegrees
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay?.setImageSourceInfo(
                                    imageProxy.width, imageProxy.height, isImageFlipped
                                )
                            } else {
                                graphicOverlay?.setImageSourceInfo(
                                    imageProxy.height, imageProxy.width, isImageFlipped
                                )
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                        try {
                            imageProcessor?.processImageProxy(imageProxy, graphicOverlay)
                        } catch (e: MlKitException) {
                           Log.d("TAG", "Failed to process image. Error: %s" + e.localizedMessage)
                        }
                    }
                )
            }
            bindToLifecycle(
                lifecycle,
                cameraSelector,
                analysisUseCase
            )
        }
    }

    private fun bindImageCaptureUseCase(lifecycle: LifecycleOwner) {
        cameraProvider?.run {
            imageCaptureUseCase?.let {
                unbind(it)
            }
            imageCaptureUseCase = ImageCapture.Builder().build()
            bindToLifecycle(lifecycle, cameraSelector, imageCaptureUseCase)
        }
    }

    fun bindAllCameraUseCases(
        lifecycle: LifecycleOwner,
        previewView: PreviewView?,
        graphicOverlay: GraphicOverlay?
    ) {
        cameraProvider?.run {
            unbindAll()
            bindPreviewUseCase(lifecycle, previewView)
            bindImageCaptureUseCase(lifecycle)
            bindAnalysisUseCase(lifecycle, graphicOverlay)
        }
    }

    private val cameraProviderLiveData: MutableLiveData<ProcessCameraProvider> by lazy {
        MutableLiveData<ProcessCameraProvider>().also {
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        it.setValue(cameraProviderFuture.get())
                    } catch (e: ExecutionException) {
                        Log.e("TAG", ": ", e)
                    } catch (e: InterruptedException) {
                        Log.e("TAG", ": ", e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    }

    private fun getProcessCameraProvider(): LiveData<ProcessCameraProvider?> {
        return cameraProviderLiveData
    }

    fun changeLens(lifecycle: LifecycleOwner,
                   previewView: PreviewView?,
                   graphicOverlay: GraphicOverlay?){
        if (cameraProvider == null) {
            return
        }
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val newCameraSelector =
            CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                bindAllCameraUseCases(lifecycle, previewView, graphicOverlay)
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
    }

    fun startObservingCamera(
        lifecycle: LifecycleOwner,
        previewView: PreviewView?,
        graphicOverlay: GraphicOverlay?
    ) {
        getProcessCameraProvider()
            .observe(lifecycle, { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                bindAllCameraUseCases(lifecycle, previewView, graphicOverlay)
            })
    }


}