package com.example.facedetector.service

import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface DeviceService {

    @GET("api/sensors")
    fun sensors(): Single<List<Device>>

    @POST("api/devices")
    fun sendCommand(@Body request: CommandRequest): Single<CommandResponse>

    @POST("user/login")
    fun login(@Body request: LoginRequest): Single<LoginResponse>
}