package com.example.facedetector

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.facedetector.ai.RecognitionAPI
import com.example.facedetector.face.helper.GraphicOverlay
import com.example.facedetector.face.helper.ImageCaptureHelper
import java.util.*

class MainActivity : AppCompatActivity(){

    private val PERMISSION_REQUESTS = 1
    private val TAG = "MainActivity"

    private var graphicOverlay: GraphicOverlay? = null
    private var previewView: PreviewView? = null
    private var imageHelper: ImageCaptureHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.preview_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        val recognitionAPI = RecognitionAPI.create(assets)
        imageHelper = ImageCaptureHelper(this, recognitionAPI)
        if (!allPermissionsGranted()) {
            runtimePermissions
        } else{
            bindAllCameraUseCases()
        }

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            imageHelper?.changeLens(this, previewView, graphicOverlay)
        }
    }

    private fun bindAllCameraUseCases() {
        imageHelper?.startObservingCamera(this, previewView, graphicOverlay)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUESTS && allPermissionsGranted()){
            bindAllCameraUseCases()
        }
    }


    public override fun onResume() {
        super.onResume()
    }

}