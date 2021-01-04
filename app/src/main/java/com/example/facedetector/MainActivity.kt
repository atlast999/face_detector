package com.example.facedetector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.facedetector.dialog.BaseDialog
import com.example.facedetector.dialog.LoadingDialog
import com.example.facedetector.dialog.ResultDialog
import com.example.facedetector.face.FaceDetectorProcessor
import com.example.facedetector.face.VisionImageProcessor
import com.example.facedetector.imagehelper.ImageHelper
import com.example.facedetector.service.Attendee
import com.example.facedetector.service.FaceService
import com.example.facedetector.timehelper.ItervalTimeOutTimer
import com.example.facedetector.timehelper.TimeOutTimer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    private val PERMISSION_REQUESTS = 1
    private val TAG = "MainActivity"

    private var imageView: ImageView? = null

    private var graphicOverlay: GraphicOverlay? = null
    private var previewView: PreviewView? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    private lateinit var actionType: String
    private var faceService: FaceService? = null

    private lateinit var tvTimeOut: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actionType = intent.getStringExtra("type").toString()
        actionBar?.title = actionType

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        previewView = findViewById(R.id.preview_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        tvTimeOut = findViewById(R.id.tvTimeOut)

        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
        facingSwitch.setOnCheckedChangeListener(this)

        val btnCapture = findViewById<Button>(R.id.btnClick)
        btnCapture.setOnClickListener {
            takePicture()
        }

        liveCaptureState.observe(this, {
            btnCapture.visibility = if (it) View.VISIBLE else View.GONE
            if (it) {
                startTimer()
            } else {
                stopTimer()
            }
        })

        imageView = findViewById(R.id.imageView)

        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(this, { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                if (allPermissionsGranted()) {
                    bindAllCameraUseCases()
                }
            })

        if (!allPermissionsGranted()) {
            runtimePermissions
        }

        faceService = App.getFaceService()
    }





    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindImageCaptureUseCase()
            bindAnalysisUseCase()
        }
    }

    private val liveCaptureState = MutableLiveData(false)

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }

        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(1F)
            .build()
        imageProcessor = FaceDetectorProcessor(this, faceDetectorOptions) {
            liveCaptureState.value = it
        }


        val builder = ImageAnalysis.Builder()
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped =
                        lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees =
                        imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.width, imageProxy.height, isImageFlipped
                        )
                    } else {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.height, imageProxy.width, isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(
                        TAG,
                        "Failed to process image. Error: " + e.localizedMessage
                    )
                    Toast.makeText(
                        applicationContext,
                        e.localizedMessage,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        )
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }


    private fun bindImageCaptureUseCase(){
        if (cameraProvider == null) {
            return
        }
        if(imageCaptureUseCase != null) {
            cameraProvider!!.unbind(imageCaptureUseCase)
        }

        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        imageCaptureUseCase = builder.build()
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageCaptureUseCase)
    }

    private fun takePicture(){
        val dir = File(applicationContext.filesDir, "mydir")
        if(!dir.exists()) dir.mkdir()
        val myImage = File(dir, "myImage.jpeg")

        Log.d(TAG, "takePicture: " + myImage.absolutePath)
        val outputFileOption = ImageCapture.OutputFileOptions.Builder(myImage).build()
        imageCaptureUseCase!!.targetRotation = Surface.ROTATION_0
        imageCaptureUseCase!!.takePicture(
            outputFileOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//
                   if (actionType == "upload"){
                       doUpload(myImage)
                   } else {
                       doRecognite(myImage)
                   }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "onError: ")
                }

            })
    }

    private fun doUpload(file: File){
        val intent = Intent(this, UploadActivity::class.java)
        intent.putExtra("filePath", file.absolutePath)
        startActivity(intent)
    }

    private val timer = object: ItervalTimeOutTimer(3, 600, TimeUnit.MILLISECONDS){

        override fun onTimeTick(remain: Long) {
            tvTimeOut.text = remain.toString()
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onTimeoutCompleted(): Int {
            takePicture()
            timerState = false
            return NextAction.DISPOSE
        }
    }

    private var timerState = false
    private fun startTimer(){
        tvTimeOut.visibility = View.VISIBLE
        if(!timerState && !dialog.isShow){
            timerState = true
            timer.start()
        }
    }

    private fun stopTimer(){
        tvTimeOut.text = "3"
        tvTimeOut.visibility = View.GONE
        if (timerState){
            timerState = false
            timer.cancel()
        }
    }
    private val dialog = LoadingDialog.newInstance()

    private fun doRecognite(file: File){
        val newPath = ImageHelper.compressImage(file.absolutePath)
        val newFile = File(newPath)

//                    val filePart = MultipartBody.Part.createFormData("file", myImage.name, myImage.asRequestBody("image/*".toMediaTypeOrNull()))
        val filePart = MultipartBody.Part.createFormData("file", newFile.name, newFile.asRequestBody("image/*".toMediaTypeOrNull()))

        faceService!!.recognite(filePart)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                dialog.show(this@MainActivity)
            }
            .doOnError {
                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG)
                    .show()
                val attendee = Attendee(null, 0)
                BaseDialog.dismissIfShowing(this@MainActivity, LoadingDialog.TAG)
                val resultDialog = ResultDialog.newInstance(attendee)
                resultDialog.show(this)
            }
            .subscribe { attendee, error ->
                if (error != null){
                    Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG)
                        .show()
                    BaseDialog.dismissIfShowing(this@MainActivity, LoadingDialog.TAG)
                } else {
                    BaseDialog.dismissIfShowing(this@MainActivity, LoadingDialog.TAG)
                    val resultDialog = ResultDialog.newInstance(attendee)
                    resultDialog.show(this)
                }
            }
    }

    private fun bindPreviewUseCase() {
//        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
//            return
//        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        builder.setTargetRotation(Surface.ROTATION_0)
//        val targetResolution = null
//        if (targetResolution != null) {
//            builder.setTargetResolution(targetResolution)
//        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }

    override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
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
                bindAllCameraUseCases()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
        Toast.makeText(
            applicationContext, "This device does not have lens with facing: $newLensFacing",
            Toast.LENGTH_SHORT
        ).show()
    }

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

    private fun isPermissionGranted(
        context: Context,
        permission: String?
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }


    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()

        imageProcessor?.run {
            this.stop()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run {
            this.stop()
        }
    }
}