package com.example.facedetector.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") @Expose val username: String,
    @SerializedName("password") @Expose val password: String
)

data class LoginResponse(
    @SerializedName("code") @Expose val code: String,
    @SerializedName("message") @Expose val message: String,
    @SerializedName("token") @Expose val token: String,
)