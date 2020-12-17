package com.example.facedetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.facedetector.imagehelper.ImageHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UploadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        val filePath = intent.getStringExtra("filePath")

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtId = findViewById<EditText>(R.id.edtId)
        val btnOK = findViewById<Button>(R.id.btnOk)

        val newPath = ImageHelper.compressImage(filePath)
        val newFile = File(newPath)
        val faceService = App.getFaceService()

        btnOK.setOnClickListener {
//            val request = UploadRequest(filePart, edtName.text.toString(), edtId.text.toString().toInt())
            val name = edtName.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val id = edtId.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", edtName.text.toString() + edtId.text.toString()+ ".jpeg", newFile.asRequestBody("image/*".toMediaTypeOrNull()))
            faceService.uploadData(filePart, name, id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError {
                    Toast.makeText(this@UploadActivity, it.message, Toast.LENGTH_LONG)
                        .show()
                }
                .subscribe { res ->
                    Toast.makeText(this@UploadActivity, res.code, Toast.LENGTH_LONG).show()
                    this.finish()
                }
        }
    }
}