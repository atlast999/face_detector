package com.example.facedetector.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.http.Part

data class UploadResponse(
    @SerializedName("code") @Expose val code: String,
)